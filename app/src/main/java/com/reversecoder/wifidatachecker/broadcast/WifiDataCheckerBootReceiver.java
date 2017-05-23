package com.reversecoder.wifidatachecker.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.reversecoder.wifidatachecker.service.WifiDataCheckerService;
import com.reversecoder.wifidatachecker.util.AllConstants;
import com.reversecoder.wifidatachecker.util.SessionManager;

/**
 * @author Md. Rashadul Alam
 */
public class WifiDataCheckerBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //this will always autostart and at least check if notification enabled, if not, it destroys
        boolean autoStart = !(SessionManager.getBooleanSetting(context, AllConstants.SESSION_KEY_AUTO_START, false));
        if (autoStart) {
            Intent startServiceIntent = new Intent(context, WifiDataCheckerService.class);
            context.startService(startServiceIntent);
        }
    }
}
