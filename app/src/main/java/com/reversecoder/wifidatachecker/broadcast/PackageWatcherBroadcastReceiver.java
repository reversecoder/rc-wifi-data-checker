package com.reversecoder.wifidatachecker.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.reversecoder.wifidatachecker.service.WifiDataCheckerService;
import com.reversecoder.wifidatachecker.util.AllConstants;
import com.reversecoder.wifidatachecker.util.SessionManager;

/**
 * @author Md. Rashadul Alam
 */
public class PackageWatcherBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean notificationEnabled = !SessionManager.getBooleanSetting(context, AllConstants.SESSION_KEY_AUTO_START, false);
        if (!notificationEnabled) {
            return;
        }
        Intent startServiceIntent = new Intent(context, WifiDataCheckerService.class);
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
