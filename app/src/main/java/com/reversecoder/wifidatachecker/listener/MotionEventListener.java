package com.reversecoder.wifidatachecker.listener;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * @author Md. Rashadul Alam
 */
public class MotionEventListener implements SensorEventListener {

    private static final float MIN_FORCE = (float) 0.25;

    private float oldX = 0;

    private float oldY = 0;

    private float oldZ = 0;

    private long lastTime = 0;

    private MotionChangeListener mMotionListener;

    private static MotionEventListener self = null;

    public static MotionEventListener getInstance() {
        if (self == null) {
            self = new MotionEventListener();
        }
        return self;

    }

    public void setMotionListener(MotionChangeListener listener) {
        mMotionListener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent se) {
        if (System.currentTimeMillis() - lastTime < 1000) {
            return;
        }
        // get sensor data
        float x = se.values[SensorManager.DATA_X];
        float y = se.values[SensorManager.DATA_Y];
        float z = se.values[SensorManager.DATA_Z];

        // calculate movement
        float totalMovement = Math.abs(x + y + z - oldX - oldY - oldZ);

        if (totalMovement > MIN_FORCE) {
            mMotionListener.onMotionChange();
            lastTime = System.currentTimeMillis();
            oldX = x;
            oldY = y;
            oldZ = z;

        } else {
            mMotionListener.noMotion();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
