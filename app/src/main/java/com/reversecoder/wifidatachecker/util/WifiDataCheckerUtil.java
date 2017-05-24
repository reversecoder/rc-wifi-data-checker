package com.reversecoder.wifidatachecker.util;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

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

    public static String checkNetworkStatus(final Context context) {
        String networkStatus = "None";
        final ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                networkStatus = "Wifi";
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                networkStatus = "Mobile Data";
            }
        } else {
            networkStatus = "None";
        }
        return networkStatus;
    }
}
