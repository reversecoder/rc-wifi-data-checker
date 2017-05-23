package com.reversecoder.wifidatachecker.listener;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.OrientationEventListener;

import static com.reversecoder.wifidatachecker.util.AllConstants.MINTIMETOREFRESH_MOTION_TO_NOMOTION;

public class OrientationListener extends OrientationEventListener {

    private boolean isMotionOn = false;

    private long lastTime = 0;

    WifiDataController wifiControllerMotion;

    final Handler handler = new Handler();

    private static OrientationListener self = null;

    public OrientationListener(Context context, int rate) {
        super(context, rate);
        wifiControllerMotion = WifiDataController.getInstance(context);
    }

    public static OrientationListener getInstance(Context context, int rate) {
        if (self == null) {
            self = new OrientationListener(context, rate);
        }
        return self;

    }

    @Override
    public void onOrientationChanged(int orientation) {
        Log.d(getClass().getName(),
                "Orientation changed, angle = " + orientation);
        if ((orientation >= 0 && orientation <= 40) || (orientation >= 50 && orientation <= 130)
                || (orientation >= 230 && orientation <= 310)
                || (orientation >= 320 && orientation <= 359)) {
            if (isMotionOn) {

                wifiControllerMotion.stopAutoMode();
                if ((System.currentTimeMillis() - lastTime) > MINTIMETOREFRESH_MOTION_TO_NOMOTION) {

                }
                isMotionOn = false;
            }
        } else {
            if (isMotionOn == false) {
                lastTime = System.currentTimeMillis();
                wifiControllerMotion.isWifiOnNoMotion = true;
                wifiControllerMotion.startAutoMode();
                isMotionOn = true;
            }
        }
    }

    @Override
    public void enable() {
        try {
            super.enable();
        } catch (Exception e) {
        }
        wifiControllerMotion.startAutoMode();
        isMotionOn = true;
    }

    @Override
    public void disable() {
        try {
            super.disable();
        } catch (Exception e) {
            e.printStackTrace();
        }
        wifiControllerMotion.stopAutoMode();
    }
}
