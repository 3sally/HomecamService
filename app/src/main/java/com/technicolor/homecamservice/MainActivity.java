package com.technicolor.homecamservice;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraDevice;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

/***
 * Add base foreground service (sticky service)
 * start the service on boot completed
 ***/

public class MainActivity extends AppCompatActivity {
    private PowerManager.WakeLock wakeLock;
    private Preview preview;
    private SensorManager sensorManager;
    private Sensor sensor;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        preview =new Preview(getApplicationContext());

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        assert pm != null;
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock Test");

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없으면 권한을 요청한다.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    200);
        }

        if ((wakeLock != null) && (!wakeLock.isHeld())) {
            Log.e("IsAcquire", "Acquire");
            wakeLock.acquire(10*60*1000000000000000L /*10 minutes*/);
        }
        Intent serviceIntent = new Intent(this, ExampleService.class);
    }

    public void startService(View v) {
        Intent serviceIntent = new Intent(this, ExampleService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }
    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, ExampleService.class);
        stopService(serviceIntent);
        preview.onPause();
    }

    public void viewPreview(View v) {
        Intent viewIntent = new Intent(this, ViewActivity.class);
        startActivity(viewIntent);
    }

    @Override
    protected void onDestroy() {
        wakeLock.release();
        super.onDestroy();
    }
}