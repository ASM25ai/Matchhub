package com.matchhub.football;
import android.content.*;
import android.app.*;
import android.os.*;

public class ReminderReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){
  if("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())){Fixtures.scheduleAlarms(context,Fixtures.storedMatches(context));FixtureJob.schedule(context);return;}
  String match=intent.getStringExtra("match");if(match==null)return;
  NotificationManager mgr=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=26)mgr.createNotificationChannel(new NotificationChannel("kickoffs","Match kickoff alerts",NotificationManager.IMPORTANCE_HIGH));
  Intent activity=new Intent(context,MainActivity.class);activity.putExtra("showMatches",true);activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  PendingIntent pi=PendingIntent.getActivity(context,0,activity,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  int delta=intent.getIntExtra("delta",0);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,"kickoffs"):new Notification.Builder(context);
  b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(delta==15?"Match starts in 15 minutes":"Match starting now")
   .setContentText(match).setStyle(new Notification.BigTextStyle().bigText(match)).setAutoCancel(true).setContentIntent(pi);
  mgr.notify((match+delta).hashCode(),b.build());
 }
}
