package com.technicolor.homecamservice;

import android.animation.Animator;
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
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
    private RecyclerView recyclerCapturedList;

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


        overlayView = LayoutInflater.from(new ContextThemeWrapper(this, R.style.AppTheme)).inflate(R.layout.overlay_homecamservice, null);


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

        recyclerCapturedList = overlayView.findViewById(R.id.capturedList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        recyclerCapturedList.setLayoutManager(linearLayoutManager);
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
                loadImages();
            }
        }, 500);
    }

    private void project(Rect rect, int srcWidth, int srcHeight, int destWidth, int destHeight){
        rect.right = rect.right * destWidth / srcWidth;
        rect.left = rect.left * destWidth / srcWidth;
        rect.top = rect.top * destHeight / srcHeight;
        rect.bottom = rect.bottom * destHeight / srcHeight;
    }


    @Override
    public void onFaceDetectedFiltered(boolean detected) {
        showOverlay(detected);
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
    DataAdapter dataAdapter = null;
    private void loadImages(){
        String path = getFilesDir().getPath() +"/Captured";
        File imageRoot = new File(path);
        List<File> files = new ArrayList<>();
        if(imageRoot.exists() && imageRoot.listFiles()!=null && imageRoot.listFiles().length>0){
            files = new ArrayList<>(Arrays.asList(imageRoot.listFiles()));
        }
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return -o1.getName().compareTo(o2.getName());
            }
        });

        if(dataAdapter==null) {
            dataAdapter = new DataAdapter(getApplicationContext(), files, R.layout.item_overlay_captured_list);
            recyclerCapturedList.setAdapter(dataAdapter);
            recyclerCapturedList.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.list_layout_animation));
            recyclerCapturedList.scheduleLayoutAnimation();
        } else {
            dataAdapter.setData(files);
            recyclerCapturedList.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.list_layout_animation));
            dataAdapter.notifyDataSetChanged();
            recyclerCapturedList.scheduleLayoutAnimation();
        }
    }

    private Runnable captureTask = new Runnable() {
        @Override
        public void run() {
            Log.d("capture_task", "try capture");
            preview.capture();
            mainHandler.postDelayed(captureTask, 10000);
        }
    };
    boolean overlayIsShowing = false;

    private void showOverlay(boolean show){
        if(show && !overlayIsShowing){
            overlayIsShowing = true;
            //overlayView.setVisibility(View.VISIBLE);
            startCaptureTask();

            overlayView.setVisibility(View.VISIBLE);
            overlayView.animate().alpha(1).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    overlayView.setAlpha(1);
                    loadImages();
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        } else if(!show && overlayIsShowing){
            overlayIsShowing = false;
            stopCaptureTask();
            overlayView.animate().alpha(0).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    overlayView.setAlpha(0);
                    overlayView.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
    }

    private void startCaptureTask(){
        Log.d("capture_task", "start");
        mainHandler.postDelayed(captureTask, 10000);
    }
    private void stopCaptureTask(){
        Log.d("capture_task", "stop");
        mainHandler.removeCallbacks(captureTask);
    }
}