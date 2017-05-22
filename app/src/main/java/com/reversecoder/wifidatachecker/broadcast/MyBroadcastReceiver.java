package com.reversecoder.wifidatachecker.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.reversecoder.wifidatachecker.service.GaugeService;

public class MyBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
//    //this will always autostart and at least check if notification or widget enabled, if not, it destroys
//    SharedPreferences sharedPref =
//        PreferenceManager.getDefaultSharedPreferences(WifiDataCheckerApplication.getInstance());
//    boolean widgetExist = sharedPref.getBoolean("widget_exists", false);
//    boolean autoStart = !(sharedPref.getBoolean("pref_key_auto_start", false));
//
//    if (widgetExist || autoStart) {

        Intent startServiceIntent = new Intent(context, GaugeService.class);
        context.startService(startServiceIntent);
//    }
    }
}
