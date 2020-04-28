package com.technicolor.homecamservice;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

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

    @Override
    protected void onDestroy() {
        wakeLock.release();
        super.onDestroy();

    }
}