package com.reversecoder.wifidatachecker.listener;

import android.content.Context;
import android.util.Log;
import android.view.OrientationEventListener;

import com.reversecoder.wifidatachecker.interfaces.DeviceOrientationChangedCallback;

/**
 * @author Md. Rashadul Alam
 */
public class DeviceOrientationListener extends OrientationEventListener {

    private static DeviceOrientationListener self = null;
    DeviceOrientationChangedCallback deviceOrientationChangedCallback = null;
    Context mContext;

    public DeviceOrientationListener(Context context, int rate) {
        super(context, rate);
        mContext = context;
    }

    public static DeviceOrientationListener getInstance(Context context, int rate) {
        if (self == null) {
            self = new DeviceOrientationListener(context, rate);
        }
        return self;
    }

    public void setDeviceOrientationChangedCallback(DeviceOrientationChangedCallback callback) {
        deviceOrientationChangedCallback = callback;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        Log.d(getClass().getName(),
                "Orientation changed, angle = " + orientation);
        if (deviceOrientationChangedCallback != null) {
            deviceOrientationChangedCallback.onDeviceOrientationChanged(orientation);
        }
    }

    @Override
    public void enable() {
        try {
            super.enable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disable() {
        try {
            super.disable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
