//package com.bjit.wifidatachecker.widget;
//
//import android.appwidget.AppWidgetProvider;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.preference.PreferenceManager;
//
//import com.bjit.wifidatachecker.application.WifiDataCheckerApplication;
//import com.bjit.wifidatachecker.service.GaugeService;
//
//public class NetworkSpeedWidget extends AppWidgetProvider {
//
//    /*This onUpdate is called when the widget is first configured, and also updates over
//    the interval defined in the updatePeriodMillis in my widget_details.xml file.
//    Though, remember, this can only work every 15 minutes, which is why I must
//    start a service, to update every second.
//    */
//
//  @Override public void onDeleted(Context context, int[] appWidgetIds) {
//
//    WifiDataCheckerApplication.getInstance()
//        .stopService(new Intent(WifiDataCheckerApplication.getInstance(), GaugeService.class));
//    WifiDataCheckerApplication.getInstance()
//        .startService(new Intent(WifiDataCheckerApplication.getInstance(), GaugeService.class));
//    super.onDeleted(context, appWidgetIds);
//  }
//
//  @Override public void onDisabled(Context context) {
//
//    SharedPreferences sharedPref =
//        PreferenceManager.getDefaultSharedPreferences(WifiDataCheckerApplication.getInstance());
//    SharedPreferences.Editor edit = sharedPref.edit();
//    edit.putBoolean("widget_exists", false);
//    edit.apply();
//    WifiDataCheckerApplication.getInstance()
//        .stopService(new Intent(WifiDataCheckerApplication.getInstance(), GaugeService.class));
//    WifiDataCheckerApplication.getInstance()
//        .startService(new Intent(WifiDataCheckerApplication.getInstance(), GaugeService.class));
//    super.onDisabled(context);
//  }
//}