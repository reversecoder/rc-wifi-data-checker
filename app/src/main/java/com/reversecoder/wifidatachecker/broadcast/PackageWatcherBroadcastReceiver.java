package com.reversecoder.wifidatachecker.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.reversecoder.wifidatachecker.service.GaugeService;
import com.reversecoder.wifidatachecker.util.AllConstants;
import com.reversecoder.wifidatachecker.util.SessionManager;

public class PackageWatcherBroadcastReceiver extends BroadcastReceiver {

  @Override public void onReceive(Context context, Intent intent) {
    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
//    boolean notificationEnabled = !(sharedPref.getBoolean("pref_key_auto_start", false));
    boolean notificationEnabled = !SessionManager.getBooleanSetting(context, AllConstants.SESSION_KEY_DISABLE_REPORTING_TO_NOTIFICATION,false);
//    boolean widgetExist = sharedPref.getBoolean("widget_exists", false);
//    if (!notificationEnabled && !widgetExist) {
      if (!notificationEnabled) {
      return;
    }
    Intent startServiceIntent = new Intent(context, GaugeService.class);
    Bundle extras = intent.getExtras();
    String uid = null;

    if (extras != null) {
      uid = extras.getString("EXTRA_UID", null);
    }
    if (uid != null && !uid.isEmpty()) {
      startServiceIntent.putExtra("EXTRA_UID", Integer.parseInt(uid));
    }
    //TODO make sure this doesn't trigger even when NetLive is disabled

    startServiceIntent.putExtra("PACKAGE_ADDED", true);
    context.startService(startServiceIntent);
  }
}
