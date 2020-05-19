package com.technicolor.homecamservice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.RecommendedStreamConfigurationMap;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.text.Line;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import kotlin.coroutines.EmptyCoroutineContext;

/**
 * Open Camera on Service
 */
public class Preview{
    private final static String TAG = "Preview : ";

    private Size mPreviewSize;
    private Context mContext;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mPreviewSession;
    private TextureView mTextureView;
    private SurfaceTexture mPreviewSurfaceTexture;
    private TextureView mPreview;
    private CameraCharacteristics characteristics;
    private HandlerThread jobThread;
    private  Handler backgroundHandler;
    private ImageReader imageReader;

    private String timeStamp;
    private String path;
    FirebaseVisionFaceDetector detector;
    private Bitmap myBitmap;
    byte[] bytes;


    PowerManager powerManager;

    Preview(Context context) {
        this(context, new SurfaceTexture(0));
    }

    Preview(Context context, TextureView textureView) {
        this(context, textureView.getSurfaceTexture());
        this.mContext =context;
        this.mTextureView = textureView;
    }

    Preview(Context context, SurfaceTexture surfaceTexture) {
        mContext = context;
        mPreviewSurfaceTexture = surfaceTexture;

        jobThread = new HandlerThread("CameraPreview");
        jobThread.start();
        backgroundHandler = new Handler(jobThread.getLooper());

        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);



        // [START set_detector_options]
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.FAST)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
                        .setMinFaceSize(0.15f)
                        .enableTracking()
                        .build();
        // [END set_detector_options]

        // [START get_detector]
        detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);
        // [END get_detector]
    }


    void release() {
        Log.d(TAG, "release");
        jobThread.quit();

        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
                Log.d(TAG, "CameraDevice Close");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private String getCameraId(CameraManager cManager) {
        try {
                for (final String cameraId : cManager.getCameraIdList()) {
                characteristics = cManager.getCameraCharacteristics(cameraId);
                List<CaptureResult.Key<?>> resultKeys = characteristics.getAvailableCaptureResultKeys();
                for(CaptureResult.Key<?> key:resultKeys){
                    Log.d(TAG, "key: " + key + ", name: " + key.getName());
                }
                return cameraId;
            }


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    void openCamera() {
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "openCamera E");
        try {
            String cameraId = getCameraId(manager);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
            Size maxSize = null;
            for(Size size:sizes){
                Log.d(TAG, "available size: " + size.toString());
                if(maxSize==null || maxSize.getHeight()*maxSize.getWidth() < size.getHeight()*size.getWidth()){
                    maxSize = size;
                }
            }
            Log.d(TAG, "max size: " + maxSize);
            mPreviewSize = maxSize;
            Log.e(TAG, String.valueOf(manager));

            int permissionCamera = ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
            if(permissionCamera == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.CAMERA},  HomeCamService.REQUEST_CAMERA);
            } else {
                manager.openCamera(cameraId, mStateCallback, null);
            }
        } catch (CameraAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onOpened");
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onDisconnected");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onError");
        }

    };

    @SuppressLint("SimpleDateFormat")
    private void startPreview() {
        mPreviewSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(mPreviewSurfaceTexture);
        List<Surface> outputSurfaces = new ArrayList<>(1);
        outputSurfaces.add(surface);
        imageReader = ImageReader.newInstance(mPreviewSize.getWidth()/2, mPreviewSize.getHeight()/2, ImageFormat.JPEG, 10);


        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @SuppressLint("SimpleDateFormat")
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireNextImage();
                if((System.currentTimeMillis()-lastFaceDetectRequest)>TimeUnit.SECONDS.toMillis(1) && myBitmap==null) {
                    lastFaceDetectRequest = System.currentTimeMillis();
                } else {
                    image.close();
                    return;
                }
                Log.d(TAG, "onImageAvailable: " + image.getWidth() + "x" + image.getHeight());
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                myBitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                try {
                    detectFaces(myBitmap);
                } catch (IOException e) {

                }
                image.close();
            }
        }, backgroundHandler);
        outputSurfaces.add(imageReader.getSurface());

        try {
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mPreviewBuilder.addTarget(surface);
        try {
            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    // TODO Auto-generated method stub
                    mPreviewSession = session;
                    updatePreview();
                    //takePicture();

                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    // TODO Auto-generated method stub
                }
                //null -> backgoundHandler
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private File file;
    private String fileName;
    @SuppressLint("SimpleDateFormat")
    private void initSave(byte[] SaveBytes) throws IOException {
        path = mContext.getFilesDir() + "/Captured/";

        Log.d("DIR", "" + path);
        File pathFile = new File(path);
        if (!pathFile.exists()) {
            pathFile.mkdir();
        }
//        int count = 0;
        timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        fileName = path +""+timeStamp+".jpg";
        file = new File(fileName);
        File[] files = pathFile.listFiles();
        if (files != null) {
            int fileLength = files.length;
            Log.v("Files :", String.valueOf(fileLength));
        }
//        if(file.exists()){
//            count ++;
//        }
        save(SaveBytes);
        del(files);

    }
    private void save(byte[] SaveBytes) throws IOException {
        try (OutputStream output = new FileOutputStream(file)) {
            output.write(SaveBytes);
        }
    }

    private void del(File[] files) {
        long todayMil = System.currentTimeMillis();
        Calendar fileCal = Calendar.getInstance();
        Date fileDate = null;
        for (File value : files) {
            fileDate = new Date(value.lastModified());
            fileCal.setTime(fileDate);
            long diffMil = todayMil - fileCal.getTimeInMillis();
            int diffTime = (int) (diffMil / (24 * 60 * 60 * 1000));
            if (diffTime >= 1 && value.exists()) {
                value.delete();
            }
        }
    }

//    private void takePicture(){
//        try {
//            CaptureRequest.Builder captureBuilder =
//                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            captureBuilder.addTarget(imageReader.getSurface());
//            mPreviewSession.capture(captureBuilder.build(), null, backgroundHandler);
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//    }

    private void updatePreview() {
        if(null == mCameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        mPreviewBuilder.addTarget(imageReader.getSurface());

        try {
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);


                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private OnSuccessListener successListener =new OnSuccessListener<List<FirebaseVisionFace>>() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onSuccess(List<FirebaseVisionFace> faces) {
            if(myBitmap !=null){
                myBitmap.recycle();
                myBitmap = null;
            }

            // Task completed successfully
            Log.e("Face Detector", "Task completed successfully");
            // [START_EXCLUDE]
            // [START get_face_info]
//                            processFaceList(faces);
            for (FirebaseVisionFace face : faces) {
                int Id=face.getTrackingId();
                Rect bounds = face.getBoundingBox();
                float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                Log.e("test", ""+bounds+ ""+rotY+"" + rotZ);
            }

            try {
                onFaceDetected(faces!=null && faces.size()>0);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // [END get_face_info]
            // [END_EXCLUDE]
        }
    };
    private OnFailureListener failureListener = new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
            // Task failed with an exception
            Log.e("Face Detector", "Task failed");
            // ...
            if(myBitmap !=null){
                myBitmap.recycle();
                myBitmap = null;
            }
        }
    };
    private void detectFaces(Bitmap bitmap) throws IOException {



        // [START run_detector]
//        detector.detectInImage(FirebaseVisionImage.fromFilePath(mContext, Uri.fromFile(new File(path + timeStamp+ ".jpg"))))

        detector.detectInImage(FirebaseVisionImage.fromBitmap(bitmap))
            .addOnSuccessListener(successListener)
            .addOnFailureListener(failureListener);

// [END run_detector]
    }


    private long lastFaceDetectRequest = 0;
    private Long lastChanged = null;
    private Boolean lastFaceDetected = null;
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void onFaceDetected(boolean detected) throws IOException {
        Log.d("power", "face detected: " + detected);
        if(!Objects.equals(detected, lastFaceDetected)){
            Log.d("power", "diff");
            lastChanged = System.currentTimeMillis();
            lastFaceDetected = detected;
        } else {
            Log.d("power", "same");
        }
        if(lastChanged!=null && (System.currentTimeMillis() - lastChanged)> TimeUnit.SECONDS.toMillis(1)) {
            Log.d("power", "trigger..");

            if (lastFaceDetected && !powerManager.isInteractive()) {
                Log.d("power", "wake up");
                PowerManagerHelper.wakeUp(powerManager, SystemClock.uptimeMillis());

                initSave(bytes);
                initImageShow();
                backgroundHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        initOverlay();

                    }
                },5000);
            }
            else if(!lastFaceDetected && powerManager.isInteractive()) {
                Log.d("power", "goto sleep");
                PowerManagerHelper.goToSleep(powerManager, SystemClock.uptimeMillis());
            }
        }
    }
    private WindowManager sideWm;
    private void initOverlay(){
        LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        sideWm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        View sideView = li.inflate(R.layout.overlay, null);
        mPreview = sideView.findViewById(R.id.texPreview);
        Log.e("is with Preview", "Open");


        int type = 0;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        mPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Preview sidePreview = new Preview(mContext, mPreview);
                sidePreview.openCamera();
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

        sideWm.addView(sideView, params);


        Log.e("is with Preview", "Open");
    }
    private WindowManager imageWm;
    private void initImageShow(){
        LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imageWm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        final View imageView = li.inflate(R.layout.imageshow, null);
        ImageView image = (ImageView)imageView.findViewById(R.id.imageview);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap myImage = BitmapFactory.decodeFile(fileName, options);
        Log.e("filepath", path);
        image.setImageBitmap(myImage);
        Log.e("is with Preview", "Open");


        int type = 0;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        imageWm.addView(imageView, params);
        Timer tm = new Timer();
        tm.schedule(new TimerTask() {
            @Override
            public void run() {
                imageWm.removeView(imageView);
            }
        }, 5000);
        Log.e("is with Preview", "Open");
    }
}
