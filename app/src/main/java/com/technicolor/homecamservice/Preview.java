package com.technicolor.homecamservice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
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
            Log.e(TAG, "found: " + cameraId);
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
        imageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 10);


        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @SuppressLint("SimpleDateFormat")
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireNextImage();

                if(!detectingInProgress) {
                //if((System.currentTimeMillis()-lastFaceDetectRequest)>TimeUnit.SECONDS.toMillis(1) && myBitmap==null) {
                    lastFaceDetectRequest = System.currentTimeMillis();
                } else {
                    image.close();
                    return;
                }
                Log.d(TAG, "onImageAvailable: " + image.getWidth() + "x" + image.getHeight());
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);

                myBitmap = Bitmap.createScaledBitmap(bitmap,  mPreviewSize.getWidth()/4, mPreviewSize.getHeight()/4, false);
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
    private File capturedFile;
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
        timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
        fileName = path +""+timeStamp;
        capturedFile = new File(fileName);
        File[] files = pathFile.listFiles();
        if (files != null) {
            int fileLength = files.length;
            Log.v("Files :", String.valueOf(fileLength));
        }
//        if(file.exists()){
//            count ++;
//        }
        save(SaveBytes);
        deleteOldCapturedImages();

    }
    private void save(byte[] SaveBytes) throws IOException {
        try (OutputStream output = new FileOutputStream(capturedFile)) {
            output.write(SaveBytes);
        }
    }

    private static final int MAX_CAPTURE_IMAGE = 30;
    private void deleteOldCapturedImages() {
        File pathFile = new File(path);
        File[] files = pathFile.listFiles();
        if(files!=null && files.length>MAX_CAPTURE_IMAGE){
            List<File> fileList = new ArrayList<>(Arrays.asList(files));
            Collections.sort(fileList);

            for(int i=0; i<MAX_CAPTURE_IMAGE; i++){
                Log.d(TAG, "delete:" + fileList.get(i).getName());
                fileList.get(i).delete();
            }
        }
    }

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
            if(listener!=null){
                listener.onFaceDetected(faces);
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

            onFaceDetected(faces!=null && faces.size()>0);
            // [END get_face_info]
            // [END_EXCLUDE]
            detectingInProgress = false;
        }
    };
    private OnFailureListener failureListener = new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
            // Task failed with an exception
            Log.e("Face Detector", "Task failed");
            if(listener!=null){
                listener.onFaceDetected(null);
            }
            // ...
            if(myBitmap !=null){
                myBitmap.recycle();
                myBitmap = null;
            }
            detectingInProgress = false;
        }
    };
    private boolean detectingInProgress = false;
    private void detectFaces(Bitmap bitmap) throws IOException {
        // [START run_detector]
//        detector.detectInImage(FirebaseVisionImage.fromFilePath(mContext, Uri.fromFile(new File(path + timeStamp+ ".jpg"))))
        detectingInProgress = true;
        Log.d(TAG, "request detect face");
        detector.detectInImage(FirebaseVisionImage.fromBitmap(bitmap))
            .addOnSuccessListener(successListener)
            .addOnFailureListener(failureListener);

// [END run_detector]
    }



    private long lastFaceDetectRequest = 0;
    private Long lastChanged = null;
    private Boolean lastFaceDetected = null;

    private static final int MAX_FILTER_COUNT = 10;
    private int filterCount = 0;

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void onFaceDetected(boolean detected) {
        Log.d("face", "onFaceDetected: " + detected);
        if(!Objects.equals(detected, lastFaceDetected)){
            filterCount++;
            if(filterCount>MAX_FILTER_COUNT){
                Log.d("face", "face detected: " + detected);
                lastChanged = System.currentTimeMillis();
                lastFaceDetected = detected;
                if(detected){
                    capture();
                }
                filterCount = 0;
            }
        } else {
            Log.d("power", "same");
            filterCount = 0;
        }
        if(lastChanged!=null && (System.currentTimeMillis() - lastChanged)> TimeUnit.SECONDS.toMillis(1)
            && lastFaceDetected && !powerManager.isInteractive()) {
                Log.d("power", "wake up");
                PowerManagerHelper.wakeUp(powerManager, SystemClock.uptimeMillis());
        }

        if(lastChanged!=null && (System.currentTimeMillis() - lastChanged)> TimeUnit.MINUTES.toMillis(1)
            && !lastFaceDetected && powerManager.isInteractive()) {
                Log.d("power", "goto sleep");
                PowerManagerHelper.goToSleep(powerManager, SystemClock.uptimeMillis());
        }
    }
    public interface OnEventListener {
        void onImageCaptured(File file);
        void onFaceDetected(List<FirebaseVisionFace> faces);
    }
    OnEventListener listener;
    public void setOnImageCapturedListener(OnEventListener listener){
        this.listener = listener;
    }

    private void capture(){
        try {
            initSave(bytes);
            if (listener != null) {
                listener.onImageCaptured(capturedFile);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
