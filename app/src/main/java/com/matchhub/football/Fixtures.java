package com.matchhub.football;

import android.content.Context;
import android.content.SharedPreferences;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import org.json.*;
import java.net.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class Fixtures {
    public static final String[] LEAGUES={"Premier League","La Liga","MLS"};
    public static final String[] CODES={"eng.1","esp.1","usa.1"};
    static final String API="https://site.api.espn.com/apis/site/v2/sports/soccer/";
    static SharedPreferences pref(Context c){return c.getSharedPreferences("favorites_v2",0);}
    static String key(String league,String team){return league+":"+team;}
    public static Set<String> favorites(Context c){return new HashSet<>(pref(c).getStringSet("teams",Collections.emptySet()));}
    public static boolean favorite(Context c,String key){return favorites(c).contains(key);}
    public static void setFavorite(Context c,String key,boolean value){Set<String> s=favorites(c);if(value)s.add(key);else s.remove(key);pref(c).edit().putStringSet("teams",s).apply();}
    static String get(String url) throws Exception {
        HttpURLConnection conn=(HttpURLConnection)new URL(url).openConnection();conn.setConnectTimeout(9000);conn.setReadTimeout(11000);conn.setRequestProperty("User-Agent","MatchHub/2.0 (Android)");
        try(InputStream stream=conn.getInputStream();ByteArrayOutputStream b=new ByteArrayOutputStream()){
            byte[] buf=new byte[4096];int n;while((n=stream.read(buf))!=-1)b.write(buf,0,n);return b.toString("UTF-8");
        } finally {conn.disconnect();}
    }
    public static JSONArray teams(String code) throws Exception {
        JSONObject obj=new JSONObject(get(API+code+"/teams?limit=100"));
        return obj.getJSONArray("sports").getJSONObject(0).getJSONArray("leagues").getJSONObject(0).getJSONArray("teams");
    }
    public static JSONObject schedule(String code,String id) throws Exception {return new JSONObject(get(API+code+"/teams/"+id+"/schedule"));}
    public static String name(JSONObject t){JSONObject x=t.optJSONObject("team");if(x==null)x=t;return x.optString("displayName",x.optString("name","Unknown"));}
    public static String id(JSONObject t){JSONObject x=t.optJSONObject("team");if(x==null)x=t;return x.optString("id");}
    public static long dateMs(String iso){try {SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.parse(iso).getTime();}catch(Exception e){return 0;}}
    public static String time(long when){return new SimpleDateFormat("EEE, d MMM · h:mm a",Locale.getDefault()).format(new Date(when));}
    public static JSONArray storedMatches(Context c){try{return new JSONArray(pref(c).getString("matches","[]"));}catch(Exception e){return new JSONArray();}}
    static void storeMatches(Context c,JSONArray a){pref(c).edit().putString("matches",a.toString()).apply();}
    public static JSONArray updateMatches(Context c) throws Exception {
        Set<String> selected=favorites(c);JSONArray all=new JSONArray();Set<String> dedup=new HashSet<>();int success=0;
        for(String entry:selected){String[] p=entry.split(":",2);if(p.length!=2)continue;
            try {JSONObject result=schedule(p[0],p[1]);success++;
                JSONArray events=result.optJSONArray("events");if(events==null)continue;
                for(int i=0;i<events.length();i++){
                    JSONObject ev=events.getJSONObject(i);long kick=dateMs(ev.optString("date"));
                    if(kick<System.currentTimeMillis()-60000 || kick>System.currentTimeMillis()+1000L*60*60*24*75)continue;
                    String eid=ev.optString("id");if(!dedup.add(p[0]+":"+eid))continue;
                    JSONObject out=new JSONObject();out.put("id",p[0]+":"+eid);out.put("match",ev.optString("name"));out.put("league",p[0]);out.put("kick",kick);all.put(out);
                }
            }catch(Exception ignored){}
        }
        if(selected.size()>0 && success==0)throw new IOException("Unable to refresh team schedules");
        ArrayList<JSONObject> ordered=new ArrayList<>();for(int i=0;i<all.length();i++)ordered.add(all.getJSONObject(i));
        ordered.sort(Comparator.comparingLong(x->x.optLong("kick")));JSONArray ret=new JSONArray();for(JSONObject x:ordered)ret.put(x);
        cancelAlarms(c,storedMatches(c));storeMatches(c,ret);scheduleAlarms(c,ret);return ret;
    }
    public static JSONArray todayEvents() throws Exception {
        JSONArray result=new JSONArray();int successes=0;
        java.text.SimpleDateFormat fmt=new java.text.SimpleDateFormat("yyyyMMdd",Locale.US);
        String today=fmt.format(new Date());
        for(String code:new String[]{"eng.1","esp.1","usa.1","uefa.champions"}){
            try{
                JSONObject response=new JSONObject(get(API+code+"/scoreboard?dates="+today+"&limit=100"));
                JSONArray events=response.optJSONArray("events");successes++;
                if(events==null)continue;
                for(int i=0;i<events.length();i++){
                    JSONObject e=events.optJSONObject(i);if(e==null)continue;
                    JSONObject comp=e.optJSONArray("competitions")==null?null:e.optJSONArray("competitions").optJSONObject(0);
                    JSONArray contestants=comp==null?null:comp.optJSONArray("competitors");
                    String score="";String keys="";
                    if(contestants!=null){for(int j=0;j<contestants.length();j++){
                        JSONObject t=contestants.optJSONObject(j);if(t==null)continue;
                        JSONObject team=t.optJSONObject("team");
                        if(j>0){score+=" - ";keys+=",";}
                        score+=t.optString("score","0");keys+=code+":"+(team==null?"":team.optString("id"));
                    }}
                    JSONObject state=e.optJSONObject("status");JSONObject type=state==null?null:state.optJSONObject("type");
                    JSONObject item=new JSONObject();item.put("id",e.optString("id"));item.put("league",code);
                    item.put("match",e.optString("shortName",e.optString("name")));
                    item.put("score",score);item.put("teams",keys);item.put("kick",dateMs(e.optString("date")));
                    item.put("status",type==null?"pre":type.optString("state","pre"));
                    item.put("statusText",type==null?"Upcoming":type.optString("shortDetail","Upcoming"));
                    result.put(item);
                }
            }catch(Exception ignored){}
        }
        if(successes==0)throw new IOException("No league feeds reachable");
        return result;
    }
    public static boolean isFavoriteEvent(Context c,JSONObject event){
        String[] teams=event.optString("teams").split(",");Set<String> selected=favorites(c);
        for(String t:teams)if(selected.contains(t))return true;
        return false;
    }
    public static JSONObject summary(String code,String id) throws Exception {
        return new JSONObject(get(API+code+"/summary?event="+java.net.URLEncoder.encode(id,"UTF-8")));
    }
    public static void cancelAlarms(Context c,JSONArray matches){
        AlarmManager mgr=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(mgr==null)return;
        for(int i=0;i<matches.length();i++){JSONObject m=matches.optJSONObject(i);if(m==null)continue;
            for(int delta:new int[]{15,0}){
                Intent intent=new Intent(c,ReminderReceiver.class);intent.setAction("MATCH_REMINDER");
                int request=(m.optString("id")+":"+delta).hashCode();
                PendingIntent pi=PendingIntent.getBroadcast(c,request,intent,PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
                if(pi!=null){mgr.cancel(pi);pi.cancel();}
            }
        }
    }
    public static void scheduleAlarms(Context c,JSONArray matches){
        AlarmManager mgr=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(mgr==null)return;
        for(int i=0;i<matches.length();i++){
            JSONObject m=matches.optJSONObject(i);if(m==null)continue;long when=m.optLong("kick");
            for(int delta:new int[]{15,0}){long target=when-delta*60000L;if(target<=System.currentTimeMillis()||target>System.currentTimeMillis()+75L*86400000)continue;
                Intent intent=new Intent(c,ReminderReceiver.class);intent.setAction("MATCH_REMINDER");intent.putExtra("match",m.optString("match"));intent.putExtra("delta",delta);
                int request=(m.optString("id")+":"+delta).hashCode();
                PendingIntent pi=PendingIntent.getBroadcast(c,request,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                if(Build.VERSION.SDK_INT>=23)mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,target,pi);
                else mgr.set(AlarmManager.RTC_WAKEUP,target,pi);
            }
        }
    }
}
