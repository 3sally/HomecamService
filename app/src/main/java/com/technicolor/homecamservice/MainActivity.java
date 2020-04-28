package com.technicolor.homecamservice;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
/***
 * Add base foreground service (sticky service)
 * start the service on boot completed
 ***/

public class MainActivity extends AppCompatActivity {

    private PowerManager.WakeLock wakeLock;


    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock Test");

        if ((wakeLock != null) && (!wakeLock.isHeld())) {
            Log.i("IsAcquire", "Acquire");
            wakeLock.acquire();
        }
    }

    public void startService(View v) {
        Intent serviceIntent = new Intent(this, ExampleService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }
    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, ExampleService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        wakeLock.release();
        super.onDestroy();

    }
}