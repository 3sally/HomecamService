package com.technicolor.homecamservice;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.IBinder;
import android.print.PrinterId;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.w3c.dom.Text;

import static com.technicolor.homecamservice.App.*;

public class ExampleService extends Service {
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
                .setSmallIcon(R.drawable.ic_camera)
                .setContentIntent(pendingIntent)
                .build();

        preview.openCamera();
        startForeground(1, notification);

        //do heavy work on a background thread
        //stopSelf();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}