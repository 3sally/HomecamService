package com.technicolor.homecamservice;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.technicolor.homecamservice.App.CHANNEL_ID;

public class HomeCamService extends Service implements Preview.OnEventListener {
    static final int REQUEST_CAMERA = 1;
    private static final String TAG = "CAMERA MANAGER";
    private Context mContext;
    private Preview preview;
    private TextureView texturePreview;
    private ImageView imageCaptured;
    private TextView textDatetime;
    private SurfaceView surfaceOSD;

    private Handler mainHandler;
    private View overlayView;

    private FirebaseAuth mAuth;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler();
        showOverlayView();

        mAuth = FirebaseAuth.getInstance();
        mAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference ref = database.getReference("capture_request");
                ref.setValue(null);
                ref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.getValue()!=null){
                            preview.capture();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });
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

        surfaceOSD = overlayView.findViewById(R.id.surfaceOSD);
        surfaceOSD.getHolder().setFormat(PixelFormat.TRANSPARENT);
        surfaceOSD.setZOrderOnTop(true);
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
                surfaceOSD.setVisibility(View.INVISIBLE);
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
                surfaceOSD.setVisibility(View.VISIBLE);
                imageCaptured.setVisibility(View.GONE);
                updateTime();
            }
        }, 5000);
    }

    private void project(Rect rect, int srcWidth, int srcHeight, int destWidth, int destHeight){
        rect.right = rect.right * destWidth / srcWidth;
        rect.left = rect.left * destWidth / srcWidth;
        rect.top = rect.top * destHeight / srcHeight;
        rect.bottom = rect.bottom * destHeight / srcHeight;
    }


    @Override
    public void onFaceDetected(List<FirebaseVisionFace> faces, int width, int height){
        int screenHeight = surfaceOSD.getMeasuredHeight();
        int screenWidth = surfaceOSD.getMeasuredWidth();
        if(faces!=null && faces.size()>0){
            Canvas canvas = surfaceOSD.getHolder().lockCanvas();
            if(canvas==null) return;
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            for(FirebaseVisionFace face:faces){
                Paint paint = new Paint();
                paint.setColor(Color.DKGRAY);
                paint.setStrokeWidth(1);
                paint.setStyle(Paint.Style.STROKE);
                Rect rect = face.getBoundingBox();
                project(rect, width, height, screenWidth, screenHeight);
                canvas.drawRect(rect, paint);
            }

            surfaceOSD.getHolder().unlockCanvasAndPost(canvas);
        } else {
            Canvas canvas = surfaceOSD.getHolder().lockCanvas();
            if(canvas==null) return;
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            surfaceOSD.getHolder().unlockCanvasAndPost(canvas);
        }
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