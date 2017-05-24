package com.reversecoder.wifidatachecker.activity;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.reversecoder.wifidatachecker.R;
import com.reversecoder.wifidatachecker.service.WifiDataCheckerService;
import com.reversecoder.wifidatachecker.util.AllConstants;
import com.reversecoder.wifidatachecker.util.SessionManager;
import com.reversecoder.wifidatachecker.util.WifiDataCheckerUtil;

/**
 * @author Md. Rashadul Alam
 */
public class MainActivity extends AppCompatActivity {

    private SensorManager sensorManager;
    private Sensor sensor;
    Button btnStart, btnStop;
    TextView tvNetworkType, tvSendingSpeed, tvActiveApp, tvReceivingSpeed, tvMotionPosition;

    public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI(intent);
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setApplicationBasicSettings();

        initUI();

        initActions();

    }

    private void updateUI(Intent intent) {
        String strActiveApp = intent.getStringExtra(AllConstants.KEY_INTENT_ACTIVE_APP);
        String strByteSent = intent.getStringExtra(AllConstants.KEY_INTENT_BYTE_SENT_PER_SECOND);
        String strByteReceived = intent.getStringExtra(AllConstants.KEY_INTENT_BYTE_RECEIVED_PER_SECOND);
        String strMotion = intent.getStringExtra(AllConstants.KEY_INTENT_MOTION);

        tvActiveApp.setText(strActiveApp);
        tvSendingSpeed.setText(strByteSent);
        tvReceivingSpeed.setText(strByteReceived);
        tvSendingSpeed.setText(strByteSent);
        tvMotionPosition.setText(strMotion);
        tvNetworkType.setText(WifiDataCheckerUtil.checkNetworkStatus(MainActivity.this));

//        tvWifiData.setText("Active app: " + strActiveApp + "\n" + "Byte sent: " + strByteSent + "\n" + "Byte received: " + strByteReceived);
    }

    private void setApplicationBasicSettings() {
        SessionManager.setStringSetting(MainActivity.this, AllConstants.SESSION_KEY_MEASUREMENT_UNIT, "Kbps");
        SessionManager.setBooleanSetting(MainActivity.this, AllConstants.SESSION_KEY_SHOW_TOTAL_VALUE, true);
        SessionManager.setStringSetting(MainActivity.this, AllConstants.SESSION_KEY_POLL_RATE, "5");
        if (Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            SessionManager.setBooleanSetting(MainActivity.this, AllConstants.SESSION_KEY_DISPLAY_ACTIVE_APP, false);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            SessionManager.setBooleanSetting(MainActivity.this, AllConstants.SESSION_KEY_DISPLAY_ACTIVE_APP, false);
        } else {
            SessionManager.setBooleanSetting(MainActivity.this, AllConstants.SESSION_KEY_DISPLAY_ACTIVE_APP, true);
        }
        SessionManager.setBooleanSetting(MainActivity.this, AllConstants.SESSION_KEY_HIDE_NOTIFICATION, true);
        SessionManager.setBooleanSetting(MainActivity.this, AllConstants.SESSION_KEY_AUTO_START, false);
    }

    private void initUI() {
        tvNetworkType = (TextView) findViewById(R.id.tv_network_type);
        tvActiveApp = (TextView) findViewById(R.id.tv_active_app);
        tvSendingSpeed = (TextView) findViewById(R.id.tv_sending_speed);
        tvReceivingSpeed = (TextView) findViewById(R.id.tv_receiving_speed);
        tvMotionPosition = (TextView) findViewById(R.id.tv_motion_position);

        btnStart = (Button) findViewById(R.id.btn_start);
        btnStop = (Button) findViewById(R.id.btn_stop);

        tvNetworkType.setText(WifiDataCheckerUtil.checkNetworkStatus(MainActivity.this));
    }

    private void initActions() {
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isServiceRunning(WifiDataCheckerService.class)) {
                    Intent intent = new Intent(getApplicationContext(), WifiDataCheckerService.class);
                    startService(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Service is running", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceRunning(WifiDataCheckerService.class)) {
                    Intent intent = new Intent(getApplicationContext(), WifiDataCheckerService.class);
                    stopService(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Please start the service first", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void restartService() {
        stopService(new Intent(MainActivity.this, WifiDataCheckerService.class));
        startService(new Intent(MainActivity.this, WifiDataCheckerService.class));
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager =
                (ActivityManager) MainActivity.this.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            registerReceiver(broadcastReceiver, new IntentFilter(AllConstants.INTENT_FILTER_ACTIVITY_UPDATE));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
