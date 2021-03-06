package com.reversecoder.wifidatachecker.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;

import com.reversecoder.wifidatachecker.R;
import com.reversecoder.wifidatachecker.interfaces.DeviceOrientationChangedCallback;
import com.reversecoder.wifidatachecker.interfaces.UnitConverter;
import com.reversecoder.wifidatachecker.listener.DeviceOrientationListener;
import com.reversecoder.wifidatachecker.model.AppDataUsage;
import com.reversecoder.wifidatachecker.util.AllConstants;
import com.reversecoder.wifidatachecker.util.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.reversecoder.wifidatachecker.util.AllConstants.INTENT_FILTER_ACTIVITY_UPDATE;
import static com.reversecoder.wifidatachecker.util.AllConstants.KEY_INTENT_ACTIVE_APP;
import static com.reversecoder.wifidatachecker.util.AllConstants.KEY_INTENT_BYTE_RECEIVED_PER_SECOND;
import static com.reversecoder.wifidatachecker.util.AllConstants.KEY_INTENT_BYTE_SENT_PER_SECOND;
import static com.reversecoder.wifidatachecker.util.AllConstants.KEY_INTENT_ORIENTATION_ANGLE;

/**
 * @author Md. Rashadul Alam
 */
public class WifiDataCheckerService extends Service implements DeviceOrientationChangedCallback {

    Intent broadcastIntentActivityUpdate;

    private long previousBytesSentSinceBoot;
    private long previousBytesReceivedSinceBoot;

    private List<AppDataUsage> appDataUsageList;

    private Notification.Builder mBuilder;
    private NotificationManager mNotifyMgr;

    private UnitConverter converter;

    private String unitMeasurement;
    private boolean showActiveApp;

    private PowerManager pm;
    private boolean notificationEnabled;

    private boolean eitherNotificationOrRequestsActiveApp;
    private boolean showTotalValueNotification;
    private boolean hideNotification;

    private boolean firstUpdate;
    private PackageManager packageManager;

    private ScheduledFuture wifiDataUpdateHandler;

    private ScheduledFuture uiUpdateHandler;

    private long start = 0l;

    private double totalSecondsSinceLastPackageRefresh = 0d;
    private double totalSecondsSinceNotificaitonTimeUpdated = 0d;

    DeviceOrientationListener deviceOrientationListener;

    double mBytesReceivedPerSecond = 0.0;
    double mBytesSentPerSecond = 0.0;
    String mActiveApp = "";
    int mOrientationAngle = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createService(this);
    }

    private void createService(final Service service) {
        firstUpdate = true;

        deviceOrientationListener = DeviceOrientationListener.getInstance(this, SensorManager.SENSOR_DELAY_UI);

        broadcastIntentActivityUpdate = new Intent(INTENT_FILTER_ACTIVITY_UPDATE);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        notificationEnabled = !SessionManager.getBooleanSetting(getApplicationContext(), AllConstants.SESSION_KEY_AUTO_START, false);

        if (!notificationEnabled) {
            this.stopSelf();
            return;
        }

        unitMeasurement = SessionManager.getStringSetting(getApplicationContext(), AllConstants.SESSION_KEY_MEASUREMENT_UNIT, "Kbps");
        showTotalValueNotification = SessionManager.getBooleanSetting(getApplicationContext(), AllConstants.SESSION_KEY_SHOW_TOTAL_VALUE, false);
        long pollRate = Long.parseLong(SessionManager.getStringSetting(getApplicationContext(), AllConstants.SESSION_KEY_POLL_RATE, "5"));
        showActiveApp = SessionManager.getBooleanSetting(getApplicationContext(), AllConstants.SESSION_KEY_DISPLAY_ACTIVE_APP, true);
        hideNotification = SessionManager.getBooleanSetting(getApplicationContext(), AllConstants.SESSION_KEY_HIDE_NOTIFICATION, false);

        converter = getUnitConverter(unitMeasurement);

        if (showActiveApp) {
            eitherNotificationOrRequestsActiveApp = true;
            packageManager = this.getPackageManager();
        }

        if (notificationEnabled) {
            mNotifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            int mId = 1;
            mBuilder = new Notification.Builder(service).setSmallIcon(R.drawable.idle)
                    .setContentTitle("")
                    .setContentText("")
                    .setOngoing(true);

            if (hideNotification) {
                mBuilder.setPriority(Notification.PRIORITY_MIN);
            } else {
                mBuilder.setPriority(Notification.PRIORITY_HIGH);
            }

            Notification notification = mBuilder.build();

            mNotifyMgr.notify(mId, notification);

            startForeground(mId, notification);
        }
        startUpdateService(pollRate);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        try {
            wifiDataUpdateHandler.cancel(true);
            uiUpdateHandler.cancel(true);
            deviceOrientationListener.disable();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = null;
        if (intent != null) {
            extras = intent.getExtras();
        }
        boolean wasPackageAdded = false;
        //int newAppUid = 0;
        if (extras != null) {
            wasPackageAdded = extras.getBoolean("PACKAGE_ADDED");
            //newAppUid = extras.getInt("EXTRA_UID");
        }
        if (wasPackageAdded && eitherNotificationOrRequestsActiveApp) {
            loadAllAppsIntoAppDataUsageList();
            //the uid in the EXTRA_UID from the packagewatcher broadcast receiver is always blank, to be fair, API says only that is "may" include it.
            //so for now just leave this disabled and just reload the entire list of apps
            //            if(newAppUid!=0) {
            //                addSpecificPackageWithUID(newAppUid);
            //            } else {
            //                loadAllAppsIntoAppDataUsageList();
            //            }
        }

        deviceOrientationListener.enable();
        deviceOrientationListener.setDeviceOrientationChangedCallback(this);

        return START_STICKY;
    }

    private synchronized String getActiveAppWithTrafficApi() {
        long maxDelta = 0L;
        long delta;
        String appLabel = "";

        for (AppDataUsage currentApp : appDataUsageList) {
            delta = currentApp.getTransferRate();
            if (delta > maxDelta) {
                appLabel = currentApp.getAppName();
                maxDelta = delta;
            }
        }

        if (appLabel.equals("")) {
            return "(" + "..." + ")";
        }
        return "(" + appLabel + ")";
    }

    private UnitConverter getUnitConverter(String unitMeasurement) {

        if (unitMeasurement.equals("b/s")) {
            return (new UnitConverter() {
                @Override
                public double convert(double bytesPerSecond) {
                    return (bytesPerSecond * 8.0);
                }
            });
        }
        if (unitMeasurement.equals("Kb/s")) {
            return (new UnitConverter() {
                @Override
                public double convert(double bytesPerSecond) {
                    return (bytesPerSecond * 8.0) / 1000.0;
                }
            });
        }
        if (unitMeasurement.equals("Mb/s")) {
            return (new UnitConverter() {
                @Override
                public double convert(double bytesPerSecond) {
                    return (bytesPerSecond * 8.0) / 1000000.0;
                }
            });
        }
        if (unitMeasurement.equals("Gb/s")) {
            return (new UnitConverter() {
                @Override
                public double convert(double bytesPerSecond) {
                    return (bytesPerSecond * 8.0) / 1000000000.0;
                }
            });
        }
        if (unitMeasurement.equals("B/s")) {
            return (new UnitConverter() {
                @Override
                public double convert(double bytesPerSecond) {
                    return bytesPerSecond;
                }
            });
        }
        if (unitMeasurement.equals("KB/s")) {
            return (new UnitConverter() {
                @Override
                public double convert(double bytesPerSecond) {
                    return bytesPerSecond / 1000.0;
                }
            });
        }
        if (unitMeasurement.equals("MB/s")) {
            return (new UnitConverter() {
                @Override
                public double convert(double bytesPerSecond) {
                    return bytesPerSecond / 1000000.0;
                }
            });
        }
        if (unitMeasurement.equals("GB/s")) {
            return (new UnitConverter() {
                @Override
                public double convert(double bytesPerSecond) {
                    return bytesPerSecond / 1000000000.0;
                }
            });
        }

        return (new UnitConverter() {
            @Override
            public double convert(double bytesPerSecond) {
                return (bytesPerSecond * 8.0) / 1000000.0;
            }
        });
    }

    private void startUpdateService(long pollRate) {
        final Runnable wifiDataUpdater = new Runnable() {
            public void run() {
                wifiDataUpdate();
            }
        };

        final Runnable uiUpdater = new Runnable() {
            public void run() {
                //send wifiDataUpdate to ui
                sendUpdateToActivity();
            }
        };

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        wifiDataUpdateHandler = scheduler.scheduleAtFixedRate(wifiDataUpdater, 0, pollRate, TimeUnit.SECONDS);
        uiUpdateHandler = scheduler.scheduleAtFixedRate(uiUpdater, 0, 1, TimeUnit.SECONDS);
    }

    @SuppressWarnings("deprecation")
    private void wifiDataUpdate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!pm.isInteractive()) {
                return;
            }
        } else if (!pm.isScreenOn()) {
            return;
        }

        initiateUpdate();
    }

    private synchronized void initiateUpdate() {

        if (firstUpdate) {
            previousBytesSentSinceBoot =
                    TrafficStats.getTotalTxBytes();//i dont initialize these to 0, because if i do, when app first reports, the rate will be crazy high
            previousBytesReceivedSinceBoot = TrafficStats.getTotalRxBytes();
            if (eitherNotificationOrRequestsActiveApp) {
                appDataUsageList = new ArrayList<>();
                loadAllAppsIntoAppDataUsageList();
            }
            firstUpdate = false;
        }

        if (firstUpdate
                && eitherNotificationOrRequestsActiveApp) {  //lazy initiazation, do it here so it is not done on the main thread, thus freezing the UI
            appDataUsageList = new ArrayList<>();
            loadAllAppsIntoAppDataUsageList();  //
            firstUpdate = false;
        }

        long end = System.nanoTime();
        long totalElapsed = end - start;
        long bytesSentSinceBoot = TrafficStats.getTotalTxBytes();
        long bytesReceivedSinceBoot = TrafficStats.getTotalRxBytes();
        start = System.nanoTime();

        double totalElapsedInSeconds = (double) totalElapsed / 1000000000.0;
        totalSecondsSinceLastPackageRefresh += totalElapsedInSeconds;
        totalSecondsSinceNotificaitonTimeUpdated += totalElapsedInSeconds;
        long bytesSentOverPollPeriod = bytesSentSinceBoot - previousBytesSentSinceBoot;
        long bytesReceivedOverPollPeriod = bytesReceivedSinceBoot - previousBytesReceivedSinceBoot;

        double bytesSentPerSecond = bytesSentOverPollPeriod / totalElapsedInSeconds;
        double bytesReceivedPerSecond = bytesReceivedOverPollPeriod / totalElapsedInSeconds;

        previousBytesSentSinceBoot = bytesSentSinceBoot;
        previousBytesReceivedSinceBoot = bytesReceivedSinceBoot;

        String activeApp = "";
        if (eitherNotificationOrRequestsActiveApp) {

            activeApp = getActiveAppWithTrafficApi();

            if (totalSecondsSinceLastPackageRefresh
                    >= 86400) { //86400 once a day, just reload all the apps.

                loadAllAppsIntoAppDataUsageList();
                totalSecondsSinceLastPackageRefresh = 0;
            }
        }

        //send wifiDataUpdate to the notification bar
        if (notificationEnabled) {
            updateNotification(bytesSentPerSecond, bytesReceivedPerSecond, activeApp);
        }

        //assigned converted global variable
        String sentString = String.format("%.3f", (converter.convert(bytesSentPerSecond)));
        String receivedString = String.format("%.3f", (converter.convert(bytesReceivedPerSecond)));
        try {
            mBytesReceivedPerSecond = Double.parseDouble(receivedString);
            mBytesSentPerSecond = Double.parseDouble(sentString);
            mActiveApp = activeApp;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendUpdateToActivity() {

        broadcastIntentActivityUpdate.putExtra(KEY_INTENT_BYTE_SENT_PER_SECOND, mBytesSentPerSecond + " " + unitMeasurement);
        broadcastIntentActivityUpdate.putExtra(KEY_INTENT_BYTE_RECEIVED_PER_SECOND, mBytesReceivedPerSecond + " " + unitMeasurement);
        broadcastIntentActivityUpdate.putExtra(KEY_INTENT_ACTIVE_APP, mActiveApp);
        broadcastIntentActivityUpdate.putExtra(KEY_INTENT_ORIENTATION_ANGLE, mOrientationAngle + "");

        sendBroadcast(broadcastIntentActivityUpdate);
    }

    @Override
    public void onDeviceOrientationChanged(int orientation) {
        mOrientationAngle = orientation;
    }

    private void updateNotification(double bytesSentPerSecond, double bytesReceivedPerSecond, String activeApp) {

        String sentString = String.format("%.3f", (converter.convert(bytesSentPerSecond)));
        String receivedString = String.format("%.3f", (converter.convert(bytesReceivedPerSecond)));

        String displayValuesText = "";
        if (showTotalValueNotification) {
            double total =
                    (converter.convert(bytesSentPerSecond) + converter.convert(bytesReceivedPerSecond));
            String totalString = String.format("%.3f", total);
            displayValuesText = "Total: " + totalString;
        }

        displayValuesText += " Up: " + sentString + " Down: " + receivedString;
        String contentTitleText = unitMeasurement;

        if (showActiveApp) {
            contentTitleText += " " + activeApp;
        }

        mBuilder.setContentText(displayValuesText);
        mBuilder.setContentTitle(contentTitleText);

        if (totalSecondsSinceNotificaitonTimeUpdated > 10800) { //10800 seconds is three hours
            mBuilder.setWhen(System.currentTimeMillis());
            totalSecondsSinceNotificaitonTimeUpdated = 0;
        }

        int mId = 1;
        if (!hideNotification) {

            if (bytesSentPerSecond < 13107 && bytesReceivedPerSecond < 13107) {
                mBuilder.setSmallIcon(R.drawable.idle);
                mNotifyMgr.notify(mId, mBuilder.build());
                return;
            }

            if (!(bytesSentPerSecond > 13107) && bytesReceivedPerSecond > 13107) {
                mBuilder.setSmallIcon(R.drawable.download);
                mNotifyMgr.notify(mId, mBuilder.build());
                return;
            }

            if (bytesSentPerSecond > 13107 && bytesReceivedPerSecond < 13107) {
                mBuilder.setSmallIcon(R.drawable.upload);
                mNotifyMgr.notify(mId, mBuilder.build());
                return;
            }

            if (bytesSentPerSecond > 13107
                    && bytesReceivedPerSecond > 13107) {//1307 bytes is equal to .1Mbit
                mBuilder.setSmallIcon(R.drawable.both);
                mNotifyMgr.notify(mId, mBuilder.build());
            }
        }
        mNotifyMgr.notify(mId, mBuilder.build());
    }

    private synchronized void loadAllAppsIntoAppDataUsageList() {
        if (appDataUsageList != null) {
            appDataUsageList.clear(); // clear before adding all the apps so we don't add duplicates
        } else {
            appDataUsageList = new ArrayList<>();
        }
        List<ApplicationInfo> appList = packageManager.getInstalledApplications(0);

        for (ApplicationInfo appInfo : appList) {
            addAppToAppDataUsageList(appInfo);
        }
    }

    private synchronized void addAppToAppDataUsageList(
            ApplicationInfo appInfo) {  //synchronized because both addSpecificPackageUID and loadAllAppsIntoAppDataUsageList may be changing the app list at the same time.
        String appLabel = (String) packageManager.getApplicationLabel(appInfo);
        int uid = appInfo.uid;
        AppDataUsage app = new AppDataUsage(appLabel, uid);
        appDataUsageList.add(app);
    }
}
