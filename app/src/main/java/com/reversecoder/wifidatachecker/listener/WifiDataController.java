package com.reversecoder.wifidatachecker.listener;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;

import com.reversecoder.wifidatachecker.interfaces.MotionChangeListener;
import com.reversecoder.wifidatachecker.interfaces.OnTiltCallback;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.reversecoder.wifidatachecker.util.AllConstants.MINTIMETOREFRESH_MOTION_TO_NOMOTION;
import static com.reversecoder.wifidatachecker.util.AllConstants.MINTIMETOREFRESH_TILT_TO_NOMOTION;

/**
 * @author Md. Rashadul Alam
 */
public class WifiDataController {

    private final SensorManager mSensorManager;

    private final MotionEventListener mMotionEventListener;

    private final Context context;

    public boolean isWifiOnNoMotion = true;

    private boolean isWifiOnMotion = false;

    public boolean wasNoMotion = false;

    private long lastTime = 0;

    private long registerLastTime;

    private static WifiDataController self;

    private static AtomicBoolean mAutoSet;

    private AtomicBoolean mIsNoTilt;

    WifiManager wifiManager = null;

    OnTiltCallback mOnTiltCallback;

    public WifiDataController(Context ctx) {
        // TODO Auto-generated constructor stub
        context = ctx;
        mMotionEventListener = MotionEventListener.getInstance();
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (mIsNoTilt == null) {
            mIsNoTilt = new AtomicBoolean();
        }
    }

    public static WifiDataController getInstance(Context context) {
        if (self == null) {
            self = new WifiDataController(context);
        }
        if (mAutoSet == null) {
            mAutoSet = new AtomicBoolean();
        }
        return self;
    }

    public void startAutoMode() {
        mAutoSet.set(Boolean.TRUE);
        lastTime = 0;
        registerLastTime = System.currentTimeMillis();
        mSensorManager.registerListener(mMotionEventListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
        mMotionEventListener.setMotionListener(motionChangeListener);

    }

    public void stopAutoMode() {
        mAutoSet.set(Boolean.FALSE);
        try {
            mSensorManager.unregisterListener(mMotionEventListener);
        } catch (Exception e) {

        }

        lastTime = 0;
    }

    public void setOnTiltCallback(OnTiltCallback onTiltCallback) {
        mOnTiltCallback = onTiltCallback;
    }

    MotionChangeListener motionChangeListener = new MotionChangeListener() {
        @Override
        public void onMotionChange() {
            lastTime = System.currentTimeMillis();
            if (isWifiOnMotion && wasNoMotion) {
                isWifiOnMotion = false;
                wasNoMotion = false;
                isWifiOnNoMotion = true;

                mIsNoTilt.set(Boolean.FALSE);
                if (mOnTiltCallback != null) {
                    mOnTiltCallback.getTiltCallback(mIsNoTilt.get());
                }
            }
        }

        @Override
        public void noMotion() {
            if (!mAutoSet.get()) {
                return;
            }

            if (isWifiOnNoMotion
                    && (System.currentTimeMillis() - registerLastTime) > MINTIMETOREFRESH_TILT_TO_NOMOTION) {

//                Toast.makeText(context, "Tilt to no motion", Toast.LENGTH_SHORT).show();

                isWifiOnNoMotion = false;
                if (lastTime != 0
                        && (System.currentTimeMillis() - lastTime) > MINTIMETOREFRESH_MOTION_TO_NOMOTION) {
                    isWifiOnMotion = true;
                    wasNoMotion = true;

                    mIsNoTilt.set(Boolean.TRUE);
                    if (mOnTiltCallback != null) {
                        mOnTiltCallback.getTiltCallback(mIsNoTilt.get());
                    }
                }
            }

        }
    };

    public boolean isNoTilt() {
        return mIsNoTilt.get();
    }

    public void enableWifi() {
        if (wifiManager == null) {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        }
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            wifiManager.setWifiEnabled(true);
        }
    }

    public void disableWifi() {
        if (wifiManager == null) {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        }
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            wifiManager.setWifiEnabled(false);
        }
    }

    public boolean isWifiEnable() {
        if (wifiManager == null) {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        }
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            return true;
        }
        return false;
    }

}
