package com.matchhub.football;
import android.app.job.*;
import android.content.*;
public class FixtureJob extends JobService {
 public static void schedule(Context c){JobScheduler js=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(js==null)return;
  JobInfo info=new JobInfo.Builder(42,new ComponentName(c,FixtureJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(6L*60*60*1000).setPersisted(true).build();js.schedule(info);
 }
 @Override public boolean onStartJob(JobParameters params){new Thread(()->{try{Fixtures.updateMatches(getApplicationContext());}catch(Exception ignored){}jobFinished(params,false);}).start();return true;}
 @Override public boolean onStopJob(JobParameters params){return true;}
}
