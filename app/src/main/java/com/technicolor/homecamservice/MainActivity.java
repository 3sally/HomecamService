package com.technicolor.homecamservice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseApp;

/***
 * Add base foreground service (sticky service)
 * start the service on boot completed
 ***/

public class MainActivity extends AppCompatActivity {
    private Preview preview;
    private PowerManager pm;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        preview =new Preview(getApplicationContext());
        pm = (PowerManager)getSystemService(POWER_SERVICE);

        preview =new Preview(getApplicationContext());

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.SYSTEM_ALERT_WINDOW) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없으면 권한을 요청한다.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.SYSTEM_ALERT_WINDOW},
                    200);
        }
    }
    public void sleep(View v) throws InterruptedException {
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
    public void imageList(View v)  {
        Intent imageIntent = new Intent(this, GalleryActivity.class);
        startActivity(imageIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}