package com.samp.mobile.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;

import com.samp.mobile.R;
import com.samp.mobile.game.SAMP;

import org.ini4j.Wini;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private static final String SERVER_IP = "51.222.193.109";
    private static final int SERVER_PORT = 7777;
    private static final String DATA_PAGE_URL = "https://www.mediafire.com/file/462u64oylkt5eqz/Data_Sem_Mods_Samp_Alyn_Todas_Gpus.zip/file";
    private static final int DATA_VERSION = 2;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private TextView serverStatusText, playersText, gameStatusText, detailText, progressText;
    private ProgressBar progressBar;
    private EditText nickInput;
    private Button actionButton;
    private volatile boolean preparing;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        prefs = getSharedPreferences("sgnt_launcher", MODE_PRIVATE);
        setContentView(buildUi());
        ensureSettings();
        refreshServer();
        refreshGameState();
    }

    @Override protected void onResume() { super.onResume(); if (!preparing && gameStatusText != null) { refreshServer(); refreshGameState(); } }

    private View buildUi() {
        FrameLayout root = new FrameLayout(this); root.setBackgroundColor(Color.BLACK);
        ImageView bg = new ImageView(this); bg.setImageResource(R.drawable.sgnt_background); bg.setScaleType(ImageView.ScaleType.CENTER_CROP); root.addView(bg,new FrameLayout.LayoutParams(-1,-1));
        View shade = new View(this); shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x18000000,0x70000000,0xF9000000})); root.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setVerticalScrollBarEnabled(false); root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setGravity(Gravity.CENTER_HORIZONTAL); content.setPadding(dp(22),dp(42),dp(22),dp(24)); scroll.addView(content,new ScrollView.LayoutParams(-1,-1));
        Space hero = new Space(this); content.addView(hero,new LinearLayout.LayoutParams(1,0,1f));
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(18),dp(18),dp(18),dp(18)); panel.setBackground(roundGradient(0xE0131313,0xF8080808,22,0x66E10600)); content.addView(panel,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = new LinearLayout(this); left.setOrientation(LinearLayout.VERTICAL); left.addView(text("SERVIDOR OFICIAL",11,0xFFBDBDBD,true)); serverStatusText=text("● VERIFICANDO...",15,0xFFFFC107,true); left.addView(serverStatusText);
        LinearLayout right = new LinearLayout(this); right.setOrientation(LinearLayout.VERTICAL); right.setGravity(Gravity.END); playersText=text("--/--",22,Color.WHITE,true); playersText.setGravity(Gravity.END); TextView pl=text("JOGADORES ONLINE",9,0xFF9B9B9B,true); pl.setGravity(Gravity.END); right.addView(playersText); right.addView(pl);
        row.addView(left,new LinearLayout.LayoutParams(0,-2,1f)); row.addView(right,new LinearLayout.LayoutParams(-2,-2)); row.setOnClickListener(v->refreshServer()); panel.addView(row);
        gap(panel,16); View d=new View(this); d.setBackgroundColor(0x33FFFFFF); panel.addView(d,new LinearLayout.LayoutParams(-1,dp(1))); gap(panel,16);
        panel.addView(text("STATUS DO JOGO",10,0xFFBDBDBD,true)); gameStatusText=text("VERIFICANDO...",16,0xFFFFC107,true); panel.addView(gameStatusText); detailText=text("",11,0xFFAAAAAA,false); panel.addView(detailText);
        gap(panel,8); progressBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); progressBar.setMax(100); progressBar.setVisibility(View.GONE); panel.addView(progressBar,new LinearLayout.LayoutParams(-1,dp(8))); progressText=text("",10,0xFFBDBDBD,false); progressText.setVisibility(View.GONE); panel.addView(progressText);
        gap(panel,14); panel.addView(text("SEU NICK",10,0xFFBDBDBD,true)); gap(panel,6);
        nickInput=new EditText(this); nickInput.setSingleLine(true); nickInput.setTextColor(Color.WHITE); nickInput.setHintTextColor(0xFF686868); nickInput.setHint("Ex.: Junior_SGNT"); nickInput.setTextSize(16); nickInput.setTypeface(Typeface.DEFAULT,Typeface.BOLD); nickInput.setPadding(dp(16),0,dp(16),0); nickInput.setBackground(rounded(0xE6181818,0xFF555555,14)); nickInput.setText(prefs.getString("nickname","")); panel.addView(nickInput,new LinearLayout.LayoutParams(-1,dp(56)));
        gap(panel,14); actionButton=new Button(this); actionButton.setText("VERIFICANDO..."); actionButton.setTextColor(Color.WHITE); actionButton.setTextSize(16); actionButton.setTypeface(Typeface.DEFAULT_BOLD); actionButton.setAllCaps(false); actionButton.setBackground(roundGradient(0xFFE10600,0xFF9D0000,16,0xFFFF4A45)); actionButton.setEnabled(false); actionButton.setOnClickListener(v->mainAction()); panel.addView(actionButton,new LinearLayout.LayoutParams(-1,dp(60)));
        gap(panel,10); TextView hint=text("Cliente universal 32/64 bits • DATA automática • servidor fixo",10,0xFF777777,false); hint.setGravity(Gravity.CENTER); panel.addView(hint);
        gap(content,14); TextView foot=text("GTA SGNT RJ  •  v1.0.0  •  @gtasaogoncalo",10,0xFF777777,false); foot.setGravity(Gravity.CENTER); content.addView(foot);
        return root;
    }

    private void mainAction() {
        if(preparing) return; String nick=validNick(); if(nick==null)return; prefs.edit().putString("nickname",nick).apply(); writeSettings(nick);
        if(isDataReady()) play(); else prepareData();
    }
    private String validNick(){String n=nickInput.getText().toString().trim(); if(!n.matches("[A-Za-z0-9_]{3,24}")){Toast.makeText(this,"Use um nick de 3 a 24 caracteres (letras, números e _).",Toast.LENGTH_LONG).show();return null;}return n;}
    private void play(){String n=validNick();if(n==null)return;writeSettings(n); startActivity(new Intent(this,SAMP.class));}

    private void ensureSettings(){ File f=new File(getExternalFilesDir(null),"SAMP/settings.ini"); if(!f.exists()){f.getParentFile().mkdirs(); writeSettings(prefs.getString("nickname","Junior_SGNT"));}}
    private void writeSettings(String nick){ try{File f=new File(getExternalFilesDir(null),"SAMP/settings.ini"); f.getParentFile().mkdirs(); if(!f.exists()){try(InputStream in=getAssets().open("settings.ini");OutputStream out=new FileOutputStream(f)){byte[]b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);}} Wini w=new Wini(f); w.put("client","host",SERVER_IP);w.put("client","port",SERVER_PORT);w.put("client","name",nick);w.put("debug","online",true);w.store();}catch(Exception e){Toast.makeText(this,"Falha ao salvar configuração: "+e.getMessage(),Toast.LENGTH_LONG).show();}}

    private boolean isDataReady(){ if(prefs.getInt("data_version",0)!=DATA_VERSION)return false; File root=getExternalFilesDir(null); return new File(root,"texdb/gta3").exists() || new File(root,"files/texdb/gta3").exists() || dirSize(root)>150L*1024*1024; }
    private long dirSize(File f){if(f==null||!f.exists())return 0;if(f.isFile())return f.length();long s=0;File[]a=f.listFiles();if(a!=null)for(File x:a)s+=dirSize(x);return s;}
    private void refreshGameState(){boolean ok=isDataReady(); if(ok){gameStatusText.setText("PRONTO PARA JOGAR");gameStatusText.setTextColor(0xFF4CAF50);detailText.setText("✓ DATA INSTALADA COM SUCESSO\n✓ CLIENTE ARM32 + ARM64 INTEGRADO");detailText.setTextColor(0xFF4CAF50);actionButton.setText("JOGAR");actionButton.setEnabled(true);}else{gameStatusText.setText("NÃO PRONTO");gameStatusText.setTextColor(0xFFFFC107);detailText.setText("DATA ainda não instalada. Toque em PREPARAR JOGO.");detailText.setTextColor(0xFFAAAAAA);actionButton.setText("PREPARAR JOGO");actionButton.setEnabled(true);} hideProgress();}

    private void prepareData(){preparing=true; actionButton.setEnabled(false);gameStatusText.setText("BAIXANDO DATA...");gameStatusText.setTextColor(0xFFFFC107);detailText.setText("Aguarde. O download e a extração são automáticos.");showProgress("Conectando...",0); executor.execute(()->{File zip=new File(getCacheDir(),"sgnt_data.zip");try{String direct=resolveMediafire(DATA_PAGE_URL);download(direct,zip);runOnUiThread(()->{gameStatusText.setText("EXTRAINDO DATA...");showProgress("Extraindo arquivos...",0);});extractSmart(zip,getExternalFilesDir(null));zip.delete();prefs.edit().putInt("data_version",DATA_VERSION).apply();runOnUiThread(()->{preparing=false;refreshGameState();Toast.makeText(this,"DATA instalada com sucesso!",Toast.LENGTH_LONG).show();});}catch(Exception e){runOnUiThread(()->{preparing=false;gameStatusText.setText("FALHA NA PREPARAÇÃO");gameStatusText.setTextColor(0xFFFF5252);detailText.setText(e.getMessage()==null?"Erro desconhecido":e.getMessage());actionButton.setText("TENTAR NOVAMENTE");actionButton.setEnabled(true);hideProgress();});}});}
    private String resolveMediafire(String page)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(page).openConnection();c.setRequestProperty("User-Agent","Mozilla/5.0");c.setConnectTimeout(15000);c.setReadTimeout(15000);String html;try(InputStream in=c.getInputStream()){html=new String(readAll(in),StandardCharsets.UTF_8);}Matcher m=Pattern.compile("href=\\\"(https?://download[^\\\"]+)\\\"").matcher(html);if(!m.find())m=Pattern.compile("href=\\\"(https?://[^\\\"]*mediafire[^\\\"]*/[^\\\"]+)\\\"[^>]*id=\\\"downloadButton\\\"").matcher(html);if(!m.find())throw new IOException("Não foi possível obter o link da DATA.");return m.group(1).replace("&amp;","&");}
    private byte[] readAll(InputStream in)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[]b=new byte[16384];int n;while((n=in.read(b))>0)o.write(b,0,n);return o.toByteArray();}
    private void download(String u,File out)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setRequestProperty("User-Agent","Mozilla/5.0");c.setConnectTimeout(20000);c.setReadTimeout(30000);long total=c.getContentLengthLong(),done=0;try(InputStream in=new BufferedInputStream(c.getInputStream());OutputStream os=new BufferedOutputStream(new FileOutputStream(out))){byte[]b=new byte[1024*128];int n;while((n=in.read(b))>0){os.write(b,0,n);done+=n;final long d=done,t=total;runOnUiThread(()->{int p=t>0?(int)(d*100/t):0;showProgress(String.format(Locale.US,"Baixando DATA... %d%%",p),p);});}}}
    private void extractSmart(File zip,File dest)throws Exception{String canonical=dest.getCanonicalPath()+File.separator;try(ZipInputStream zin=new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)))){ZipEntry e;int count=0;while((e=zin.getNextEntry())!=null){String name=e.getName().replace('\\','/');name=stripKnownRoot(name);if(name.length()==0){zin.closeEntry();continue;}File out=new File(dest,name);if(!out.getCanonicalPath().startsWith(canonical))throw new IOException("ZIP inválido.");if(e.isDirectory())out.mkdirs();else{File p=out.getParentFile();if(p!=null)p.mkdirs();try(OutputStream os=new BufferedOutputStream(new FileOutputStream(out))){byte[]b=new byte[65536];int n;while((n=zin.read(b))>0)os.write(b,0,n);}}count++;final int cc=count;if(cc%100==0)runOnUiThread(()->showProgress("Extraindo arquivos... "+cc,Math.min(99,cc/20)));zin.closeEntry();}}}
    private String stripKnownRoot(String n){String[] marks={"Android/data/com.rockstargames.gtasa/files/","com.rockstargames.gtasa/files/","files/"};for(String m:marks){int i=n.indexOf(m);if(i>=0)return n.substring(i+m.length());}while(n.startsWith("/"))n=n.substring(1);return n;}

    private void refreshServer(){executor.execute(()->{try{ServerInfo s=query();runOnUiThread(()->{serverStatusText.setText("● ONLINE");serverStatusText.setTextColor(0xFF4CAF50);playersText.setText(s.players+"/"+s.maxPlayers);});}catch(Exception e){runOnUiThread(()->{serverStatusText.setText("● OFFLINE / SEM RESPOSTA");serverStatusText.setTextColor(0xFFFF5252);playersText.setText("--/--");});}});}
    private ServerInfo query()throws Exception{InetAddress a=InetAddress.getByName(SERVER_IP);byte[]ip=a.getAddress();ByteArrayOutputStream q=new ByteArrayOutputStream();q.write(new byte[]{'S','A','M','P'});q.write(ip);q.write(SERVER_PORT&255);q.write((SERVER_PORT>>8)&255);q.write('i');byte[]data=q.toByteArray();DatagramSocket s=new DatagramSocket();s.setSoTimeout(2500);s.send(new DatagramPacket(data,data.length,a,SERVER_PORT));byte[]buf=new byte[2048];DatagramPacket p=new DatagramPacket(buf,buf.length);s.receive(p);s.close();if(p.getLength()<15)throw new IOException("Resposta inválida");ByteBuffer bb=ByteBuffer.wrap(buf,11,p.getLength()-11).order(ByteOrder.LITTLE_ENDIAN);bb.get();int players=bb.getShort()&0xffff,max=bb.getShort()&0xffff;return new ServerInfo(players,max);}
    static class ServerInfo{int players,maxPlayers;ServerInfo(int p,int m){players=p;maxPlayers=m;}}

    private void showProgress(String t,int p){progressBar.setVisibility(View.VISIBLE);progressText.setVisibility(View.VISIBLE);progressBar.setProgress(p);progressText.setText(t);} private void hideProgress(){progressBar.setVisibility(View.GONE);progressText.setVisibility(View.GONE);} private TextView text(String s,int z,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);if(b)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;} private void gap(LinearLayout l,int h){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(1,dp(h)));} private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);} private GradientDrawable rounded(int fill,int stroke,int r){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(r));g.setStroke(dp(1),stroke);return g;} private GradientDrawable roundGradient(int a,int b,int r,int stroke){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{a,b});g.setCornerRadius(dp(r));g.setStroke(dp(1),stroke);return g;}
}
