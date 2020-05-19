package com.technicolor.homecamservice;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.technicolor.homecamservice.App.CHANNEL_ID;

public class HomeCamService extends Service implements Preview.OnImageCapturedListener {
    static final int REQUEST_CAMERA = 1;
    private static final String TAG = "CAMERA MANAGER";
    private Context mContext;
    private Preview preview;
    private TextureView texturePreview;
    private ImageView imageCaptured;
    private TextView textDatetime;
    private Handler mainHandler;
    private View overlayView;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler();
        showOverlayView();
    }

    private void showOverlayView(){
        LayoutInflater inflate = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        |WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        |WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.LEFT | Gravity.TOP;
        overlayView = inflate.inflate(R.layout.overlay_homecamservice, null);

        texturePreview = overlayView.findViewById(R.id.preview);
        texturePreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                preview = new Preview(getApplicationContext(), texturePreview);
                preview.setOnImageCapturedListener(HomeCamService.this);
                preview.openCamera();
                updateTime();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

        imageCaptured = overlayView.findViewById(R.id.imageCaptured);
        imageCaptured.setVisibility(View.GONE);

        textDatetime = overlayView.findViewById(R.id.textDatetime);

        wm.addView(overlayView, params);
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

    Bitmap bitmapCaptured = null;
    @Override
    public void onImageCaptured(final File file) {
        mainHandler.removeCallbacksAndMessages(null);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                clearCapturedImage();
                imageCaptured.setVisibility(View.VISIBLE);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                bitmapCaptured = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                imageCaptured.setImageBitmap(bitmapCaptured);
                textDatetime.setText(file.getName());
                textDatetime.setTextColor(Color.RED);
            }
        });
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                clearCapturedImage();
                imageCaptured.setVisibility(View.GONE);
                updateTime();
            }
        }, 5000);
    }
    public void clearCapturedImage(){
        if(bitmapCaptured!=null){
            bitmapCaptured.recycle();
            bitmapCaptured = null;
        }
    }
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    public void updateTime(){
        textDatetime.setText(DATE_FORMAT.format(new Date()));
        textDatetime.setTextColor(Color.DKGRAY);
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateTime();
            }
        }, 1000);
    }
}