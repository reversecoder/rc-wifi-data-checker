package com.reversecoder.wifidatachecker.util;

import android.app.ActivityManager;
import android.content.Context;

/**
 * @author Md. Rashadul Alam
 */
public class WifiDataCheckerUtil {

    private boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
