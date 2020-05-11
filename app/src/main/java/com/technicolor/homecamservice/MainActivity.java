package com.technicolor.homecamservice;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

/***
 * Add base foreground service (sticky service)
 * start the service on boot completed
 ***/

public class MainActivity extends AppCompatActivity {
    private Preview preview;
    private SensorManager sensorManager;
    private Sensor sensor;
    private PowerManager pm;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        preview =new Preview(getApplicationContext());
        pm = (PowerManager)getSystemService(POWER_SERVICE);

        preview =new Preview(getApplicationContext());

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없으면 권한을 요청한다.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    200);
        }
    }
    public void Sleep(View v) throws InterruptedException {
        PowerManagerHelper.goToSleep(pm, SystemClock.uptimeMillis());
        Thread.sleep(10000);
        PowerManagerHelper.wakeUp(pm, SystemClock.uptimeMillis());
    }

    public void startService(View v) throws InterruptedException {
        Intent serviceIntent = new Intent(this, HomeCamService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }
    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, HomeCamService.class);
        stopService(serviceIntent);
    }

    public void viewPreview(View v) {
        Intent viewIntent = new Intent(this, ViewActivity.class);
        startActivity(viewIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}