package com.technicolor.homecamservice;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static com.technicolor.homecamservice.App.*;

public class HomeCamService extends Service {
    static final int REQUEST_CAMERA = 1;
    private static final String TAG = "CAMERA MANAGER";
    private Context mContext;
    private Preview preview;

    @Override
    public void onCreate() {
        super.onCreate();
        preview = new Preview(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Home Cam Service")
                .setContentText("service...")
                .setTicker("Camera On")
                .setContentIntent(pendingIntent)
                .build();

        preview.openCamera();
        startForeground(1, notification);

        //do heavy work on a background thread
        //stopSelf();

        //return START_STICKY;
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        preview.release();;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}