package com.matchhub.football;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebResourceError;
import android.os.Message;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;
import android.os.Build;
import android.app.PictureInPictureParams;
import android.content.pm.PackageManager;
import android.util.Rational;
import org.json.*;
import java.util.*;

/** A simple source browser. No stream scraping, hosting or automatic audio language conversion. */
public class MainActivity extends Activity {
    private static final String FOOTY = "https://footy100.st/";
    private static final String YALLA = "https://yallashoot.eu/home_/";
    private WebView browser;
    private FrameLayout root, fullScreenContainer;
    private LinearLayout content;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private Button footyBtn, yallaBtn;
    private String source = FOOTY;
    private FrameLayout featureLayer;
    private LinearLayout featureList;
    private boolean videoFullscreen = false;
    private static final Set<String> BLOCKED_HOSTS = new HashSet<>(Arrays.asList("doubleclick.net","googlesyndication.com","googleadservices.com","popads.net","popcash.net","propellerads.com","adsterra.com","exoclick.com","trafficjunky.net"));
    private boolean isAdHost(String host) { if(host==null)return false;host=host.toLowerCase(Locale.ROOT);for(String h:BLOCKED_HOSTS)if(host.equals(h)||host.endsWith("."+h))return true;return false; }
    private boolean permittedNavigation(Uri uri) { String scheme=uri.getScheme();return "https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(11, 20, 38));
        getWindow().setNavigationBarColor(Color.rgb(11, 20, 38));
        root = new FrameLayout(this);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setBackgroundColor(Color.rgb(10, 18, 34));
        root.addView(content, new FrameLayout.LayoutParams(-1,-1)); setContentView(root);

        TextView title = new TextView(this); title.setText("⚽  MATCHHUB   ·   ENGLISH PREFERRED");
        title.setTextColor(Color.WHITE); title.setTextSize(17); title.setGravity(Gravity.CENTER_VERTICAL); title.setPadding(dp(16),dp(12),dp(8),dp(10));
        content.addView(title, new LinearLayout.LayoutParams(-1,dp(55)));
        LinearLayout tabs = new LinearLayout(this); tabs.setPadding(dp(9),0,dp(9),dp(8));
        footyBtn = makeButton("Footy100"); yallaBtn = makeButton("Yalla Shoot");
        tabs.addView(footyBtn,new LinearLayout.LayoutParams(0,dp(45),1));
        tabs.addView(yallaBtn,new LinearLayout.LayoutParams(0,dp(45),1));
        content.addView(tabs);
        footyBtn.setOnClickListener(v -> switchSource(FOOTY)); yallaBtn.setOnClickListener(v -> switchSource(YALLA));

        LinearLayout controls = new LinearLayout(this); controls.setPadding(dp(9),0,dp(9),dp(4));
        Button back = makeButton("‹ Back"), refresh = makeButton("↻ Refresh"), external = makeButton("↗ Browser");
        controls.addView(back,new LinearLayout.LayoutParams(0,dp(43),1));
        controls.addView(refresh,new LinearLayout.LayoutParams(0,dp(43),1));
        controls.addView(external,new LinearLayout.LayoutParams(0,dp(43),1));
        content.addView(controls);
        back.setOnClickListener(v -> { if(browser.canGoBack()) browser.goBack(); else Toast.makeText(this,"No previous page",Toast.LENGTH_SHORT).show(); });
        refresh.setOnClickListener(v -> browser.reload());
        external.setOnClickListener(v -> { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(browser.getUrl()==null?source:browser.getUrl()))); } catch(Exception ignored) { Toast.makeText(this,"No browser found",Toast.LENGTH_SHORT).show(); } });

        TextView hint = new TextView(this);
        hint.setText("Choose a match on the site. Streams usually appear near kickoff. Select English commentary when offered; availability is not guaranteed.");
        hint.setTextColor(0xffa8bed0); hint.setTextSize(12); hint.setPadding(dp(12),dp(4),dp(12),dp(8));
        content.addView(hint);

        browser = new WebView(this);
        browser.setBackgroundColor(Color.rgb(16, 26, 45)); browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setDomStorageEnabled(true); browser.getSettings().setMediaPlaybackRequiresUserGesture(true);
        browser.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        browser.getSettings().setAllowFileAccess(false); browser.getSettings().setAllowContentAccess(false);
        browser.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        browser.getSettings().setSupportMultipleWindows(true);
        browser.getSettings().setAllowFileAccessFromFileURLs(false);
        browser.getSettings().setAllowUniversalAccessFromFileURLs(false);
        CookieManager.getInstance().setAcceptThirdPartyCookies(browser,false);
        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri uri=req.getUrl();
                if(!req.isForMainFrame())return false;
                if(!permittedNavigation(uri))return true;
                if(isAdHost(uri.getHost()))return true;
                // Keep normal HTTP(S) navigation in MatchHub instead of launching Chrome.
                return false;
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if(isAdHost(request.getUrl().getHost()))
                    return new WebResourceResponse("text/plain","UTF-8",new java.io.ByteArrayInputStream(new byte[0]));
                return null;
            }
        });
        browser.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                // Block popup windows and new tabs, frequently used for intrusive ads.
                return false;
            }
            @Override public void onCloseWindow(WebView window) { if(window!=null && window!=browser)window.destroy(); }
            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullscreenView != null) { callback.onCustomViewHidden(); return; }
                fullscreenView = view; fullscreenCallback = callback; videoFullscreen=true;
                fullScreenContainer = new FrameLayout(MainActivity.this);
                fullScreenContainer.setBackgroundColor(Color.BLACK);
                fullScreenContainer.addView(view,new FrameLayout.LayoutParams(-1,-1));
                root.addView(fullScreenContainer,new FrameLayout.LayoutParams(-1,-1)); content.setVisibility(View.GONE);
                Button pip=makeButton("▣ PiP");FrameLayout.LayoutParams pipParam=new FrameLayout.LayoutParams(dp(82),dp(44),Gravity.TOP|Gravity.RIGHT);fullScreenContainer.addView(pip,pipParam);pip.setOnClickListener(w->startPip());
            }
            @Override public void onHideCustomView() { hideFullscreen(); }
        });
        content.addView(browser,new LinearLayout.LayoutParams(-1,0,1));
        if (state != null) { browser.restoreState(state); source = state.getString("source",FOOTY); } else browser.loadUrl(FOOTY);
        updateTabs();
        FixtureJob.schedule(this);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},21);
        LinearLayout nav = new LinearLayout(this); nav.setBackgroundColor(0xff11243f);
        Button streams=makeButton("Watch"), favorites=makeButton("★ Teams"), matches=makeButton("My Matches"), live=makeButton("Today");
        nav.addView(live,new LinearLayout.LayoutParams(0,dp(48),1));
        nav.addView(streams,new LinearLayout.LayoutParams(0,dp(48),1));
        nav.addView(favorites,new LinearLayout.LayoutParams(0,dp(48),1));
        nav.addView(matches,new LinearLayout.LayoutParams(0,dp(48),1));
        content.addView(nav,0);
        live.setOnClickListener(v->showToday("all"));streams.setOnClickListener(v->hideFeature());favorites.setOnClickListener(v->showTeams());matches.setOnClickListener(v->showMatches());
        if(getIntent().getBooleanExtra("showMatches",false))showMatches();
    }
    private TextView line(String label,int size){TextView t=new TextView(this);t.setText(label);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setPadding(dp(14),dp(13),dp(14),dp(13));return t;}
    private void showFeature(String heading){
        if(featureLayer!=null)root.removeView(featureLayer);
        featureLayer=new FrameLayout(this);featureLayer.setBackgroundColor(0xff0a1222);
        ScrollView scroll=new ScrollView(this);featureList=new LinearLayout(this);featureList.setOrientation(LinearLayout.VERTICAL);
        featureList.addView(line(heading,22));scroll.addView(featureList);featureLayer.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        root.addView(featureLayer,new FrameLayout.LayoutParams(-1,-1));
        Button close=makeButton("← Watch");FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(108),dp(44),Gravity.TOP|Gravity.RIGHT);featureLayer.addView(close,p);close.setOnClickListener(v->hideFeature());
    }
    private void hideFeature(){if(featureLayer!=null){root.removeView(featureLayer);featureLayer=null;}}
    private void showTeams(){showFeature("★ Favorite Teams");
        featureList.addView(line("Star clubs in Premier League, La Liga or MLS. Your selections are saved on this phone. Match alerts are scheduled from fixture times, not from streaming sites.",13));
        for(int i=0;i<Fixtures.CODES.length;i++){
            final String code=Fixtures.CODES[i];
            Button load=makeButton("Browse " +Fixtures.LEAGUES[i]+ " teams");featureList.addView(load,new LinearLayout.LayoutParams(-1,dp(56)));
            load.setOnClickListener(v->{load.setEnabled(false);load.setText("Loading teams…");
              new Thread(()->{try{JSONArray all=Fixtures.teams(code);runOnUiThread(()->{
                if(featureLayer==null)return;load.setText(Fixtures.LEAGUES[Arrays.asList(Fixtures.CODES).indexOf(code)]);load.setEnabled(true);
                LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);featureList.addView(box);
                ArrayList<JSONObject> clubs=new ArrayList<>();for(int j=0;j<all.length();j++){JSONObject obj=all.optJSONObject(j);if(obj!=null)clubs.add(obj);}
                clubs.sort(Comparator.comparing(Fixtures::name));
                for(JSONObject t:clubs){String k=Fixtures.key(code,Fixtures.id(t));String name=Fixtures.name(t);
                    Button b=makeButton((Fixtures.favorite(this,k)?"★ ":"☆ ")+name);
                    box.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));
                    b.setOnClickListener(w->{boolean enable=!Fixtures.favorite(this,k);Fixtures.setFavorite(this,k,enable);b.setText((enable?"★ ":"☆ ")+name);
                        new Thread(()->{try{Fixtures.updateMatches(getApplicationContext());}catch(Exception ignored){} }).start();});
                }
              });}catch(Exception ex){runOnUiThread(()->{load.setEnabled(true);load.setText("Retry "+code+" (network error)");});}}).start();
            });
        }
    }
    private void showMatches(){showFeature("My Matches");
        Button refresh=makeButton("↻ Sync schedules and reminders");featureList.addView(refresh,new LinearLayout.LayoutParams(-1,dp(54)));
        featureList.addView(line("Notifications: 15 minutes before kickoff and at kickoff, when Android permits. Android may delay alarms to save battery. Times use your phone's timezone.",13));
        LinearLayout entries=new LinearLayout(this);entries.setOrientation(LinearLayout.VERTICAL);featureList.addView(entries);
        drawMatches(entries,Fixtures.storedMatches(this));
        refresh.setOnClickListener(v->{refresh.setText("Refreshing…");refresh.setEnabled(false);new Thread(()->{
          try{JSONArray result=Fixtures.updateMatches(getApplicationContext());runOnUiThread(()->{if(featureLayer!=null){drawMatches(entries,result);refresh.setText("✓ Updated — tap to refresh");refresh.setEnabled(true);}});}
          catch(Exception e){runOnUiThread(()->{refresh.setText("Network unavailable — retry");refresh.setEnabled(true);});}
        }).start();});
        refresh.performClick();
    }
    private void drawMatches(LinearLayout list,JSONArray matches){list.removeAllViews();
        if(matches.length()==0){list.addView(line("No upcoming fixtures saved. Open ★ Teams, select your clubs, then sync schedules.",15));return;}
        for(int i=0;i<matches.length();i++){JSONObject m=matches.optJSONObject(i);if(m==null||m.optLong("kick")<System.currentTimeMillis())continue;
            list.addView(line("⚽ "+m.optString("match")+"\n"+Fixtures.time(m.optLong("kick"))+" · "+m.optString("league"),16));
        }
    }
    private Button makeButton(String name){ Button b=new Button(this); b.setAllCaps(false); b.setText(name); b.setTextColor(Color.WHITE); b.setTextSize(13); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff1b3151)); return b; }
    private int dp(int px){return Math.round(getResources().getDisplayMetrics().density*px);}
    private void switchSource(String url){ source=url; browser.loadUrl(url); updateTabs(); }
    private void updateTabs(){footyBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(source.equals(FOOTY)?0xff168a9e:0xff1b3151)); yallaBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(source.equals(YALLA)?0xff168a9e:0xff1b3151));}
    private void hideFullscreen(){ if(fullscreenView==null)return; root.removeView(fullScreenContainer); fullScreenContainer.removeAllViews(); content.setVisibility(View.VISIBLE); fullscreenView=null; videoFullscreen=false; if(fullscreenCallback!=null)fullscreenCallback.onCustomViewHidden(); fullscreenCallback=null; }
    private void showToday(String filter){
        showFeature("⚽ Today's Matches · Live scores");
        LinearLayout filters=new LinearLayout(this); featureList.addView(filters);
        for(String f:new String[]{"all","live","favorites"}){
            Button b=makeButton(f.equals("all")?"All":f.equals("live")?"Live":"★ Teams");
            filters.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));
            b.setOnClickListener(v->showToday(f));
        }
        TextView note=line("Live scores are refreshed when this page opens. Tap ↻ Refresh for updates. Source: ESPN unofficial public feed. Match data may be delayed.",12);
        featureList.addView(note);
        Button refresh=makeButton("↻ Refresh scores");featureList.addView(refresh,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout entries=new LinearLayout(this);entries.setOrientation(LinearLayout.VERTICAL);featureList.addView(entries);
        refresh.setOnClickListener(v->{refresh.setEnabled(false);refresh.setText("Loading…");new Thread(()->{
            try{JSONArray events=Fixtures.todayEvents();runOnUiThread(()->{
                if(featureLayer==null)return;
                entries.removeAllViews(); int visible=0;
                for(int i=0;i<events.length();i++){
                    JSONObject m=events.optJSONObject(i);if(m==null)continue;
                    String status=m.optString("status");
                    if(filter.equals("live")&&!status.equals("in"))continue;
                    if(filter.equals("favorites")&&!Fixtures.isFavoriteEvent(this,m))continue;
                    visible++;
                    String id=m.optString("id");String league=m.optString("league");
                    String display=m.optString("match")+"\n"+m.optString("score")+" · "+m.optString("statusText")+" · "+league;
                    Button item=makeButton(display);item.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
                    entries.addView(item,new LinearLayout.LayoutParams(-1,dp(74)));
                    item.setOnClickListener(w->showMatchDetail(league,id,m));
                }
                if(visible==0)entries.addView(line("No matches found for this filter. Try All or refresh later.",14));
                refresh.setEnabled(true);refresh.setText("↻ Refresh scores");
            });}catch(Exception ex){runOnUiThread(()->{refresh.setText("Couldn't load — retry");refresh.setEnabled(true);entries.addView(line("Unable to load live data. Check your connection.",14));});}
        }).start();});refresh.performClick();
    }
    private void showMatchDetail(String league,String id,JSONObject event){
        showFeature(event.optString("match"));
        featureList.addView(line(event.optString("score")+" · "+event.optString("statusText"),19));
        featureList.addView(line("League: "+league+"\nKickoff: "+Fixtures.time(event.optLong("kick")),14));
        Button sourceButton=makeButton("Watch on Footy100 (browse matches)");featureList.addView(sourceButton);
        sourceButton.setOnClickListener(v->{hideFeature();switchSource(FOOTY);});
        Button yalla=makeButton("Browse on Yalla Shoot");featureList.addView(yalla);
        yalla.setOnClickListener(v->{hideFeature();switchSource(YALLA);});
        featureList.addView(line("These buttons open the source match listings, not a verified direct stream. Select an English feed if available.",12));
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.VERTICAL);featureList.addView(stats);
        stats.addView(line("Loading match statistics…",13));
        new Thread(()->{try{JSONObject detail=Fixtures.summary(league,id);runOnUiThread(()->{
            if(featureLayer==null)return;stats.removeAllViews();
            JSONObject header=detail.optJSONObject("header");
            JSONObject box=detail.optJSONObject("boxscore");
            if(box!=null){JSONArray players=box.optJSONArray("players");if(players!=null){
              for(int i=0;i<players.length();i++){
                JSONObject p=players.optJSONObject(i);if(p==null)continue;
                stats.addView(line(p.optJSONObject("team")==null?"Team":p.optJSONObject("team").optString("displayName"),16));
                JSONArray values=p.optJSONArray("statistics");if(values!=null)for(int j=0;j<Math.min(values.length(),2);j++){
                    JSONObject st=values.optJSONObject(j);if(st!=null)stats.addView(line(st.optString("name","Statistics")+" available",12));
                }
              }
            }}
            JSONArray details=detail.optJSONArray("details");if(details!=null){stats.addView(line("Match events",18));for(int i=0;i<Math.min(details.length(),15);i++){
                JSONObject x=details.optJSONObject(i);if(x!=null)stats.addView(line(x.optString("clock")+" "+x.optString("text"),13));
            }}
            JSONObject game=detail.optJSONObject("gameInfo");
            if(stats.getChildCount()==0)stats.addView(line("Detailed statistics unavailable for this match. Scores and schedule remain available.",13));
        });}catch(Exception e){runOnUiThread(()->{stats.removeAllViews();stats.addView(line("Statistics are currently unavailable for this fixture.",13));});}}).start();
    }
    private void startPip(){
        if(Build.VERSION.SDK_INT<26 || !videoFullscreen || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)){
            Toast.makeText(this,"Open a supported fullscreen video first (Android 8+)",Toast.LENGTH_SHORT).show();return;
        }
        try {enterPictureInPictureMode(new PictureInPictureParams.Builder().setAspectRatio(new Rational(16,9)).build());}
        catch(Exception ex){Toast.makeText(this,"PiP unavailable for this player",Toast.LENGTH_SHORT).show();}
    }
    @Override public void onPictureInPictureModeChanged(boolean inPip,android.content.res.Configuration config){
        super.onPictureInPictureModeChanged(inPip,config);
        if(fullscreenView!=null){ if(inPip)content.setVisibility(View.GONE); }
    }
    @Override public void onBackPressed(){ if(featureLayer!=null){hideFeature();return;} if(fullscreenView!=null)hideFullscreen();else if(browser.canGoBack())browser.goBack();else super.onBackPressed(); }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);browser.saveState(out);out.putString("source",source);}
    @Override protected void onDestroy(){browser.destroy();super.onDestroy();}
}
