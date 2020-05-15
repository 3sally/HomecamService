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
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import java.util.concurrent.Semaphore;
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
    private CameraCharacteristics characteristics;
    private HandlerThread jobThread;
    private  Handler backgroundHandler;
    private ImageReader imageReader;

    private String timeStamp;
    private String path;
    FirebaseVisionFaceDetector detector;
    private PowerManager pm;
    private Bitmap myBitmap;



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
        imageReader = ImageReader.newInstance(mPreviewSize.getWidth()/2, mPreviewSize.getHeight()/2, ImageFormat.JPEG, 1);


        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @SuppressLint("SimpleDateFormat")
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireNextImage();
                Log.d(TAG, "onImageAvailable: " + image.getWidth() + "x" + image.getHeight());
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                myBitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);

                path = Environment.getExternalStorageDirectory() + "/Pictures/";
                timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                File file = new File(path, timeStamp + ".jpg");
                File pathFile = new File(path);
                File[] files = pathFile.listFiles();
                int fileLength = files.length;
                Log.v("Files :", String.valueOf(fileLength));
                del(files);
                try {
                    if (!file.exists() && Integer.parseInt(timeStamp.substring(9, 13)) % 10 == 0){
                        save(bytes);

                    Log.e("ImageReader : ", "save Image : " + timeStamp);
                    }
                } catch (IOException e) {
                }
                image.close();
            }

            private void save(byte[] bytes) throws IOException {
                OutputStream output = null;
                try {
                    output = new FileOutputStream(path + timeStamp + ".jpg");
                    output.write(bytes);
                } finally {
                    if (null != output) {
                        output.close();
                    }
                }
            }

            private void del(File[] files) {
                long todayMil = System.currentTimeMillis();
                Calendar fileCal = Calendar.getInstance();
                Date fileDate = null;
                for (int j = 0; j < files.length; j++) {
                    fileDate = new Date(files[j].lastModified());
                    fileCal.setTime(fileDate);
                    long diffMil = todayMil - fileCal.getTimeInMillis();
                    int diffTime = (int) (diffMil / (6 * 60 * 60 * 1000));
                    if (diffTime >= 1 && files[j].exists()) {
                        Log.e("Files : ", "DelFile" + timeStamp + ".jpg");
                        files[j].delete();
                    }
                }
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
                    takePicture();

                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    // TODO Auto-generated method stub
                    Toast.makeText(mContext, "onConfigureFailed", Toast.LENGTH_LONG).show();
                }
                //null -> backgoundHandler
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void takePicture(){
        try {
            CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            mPreviewSession.capture(captureBuilder.build(), null, backgroundHandler);
        } catch (Exception e){
            e.printStackTrace();
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
//                    Log.d(TAG, "onCaptureCompleted, faceDetectMode: " + result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE)
//                            + ", faces: " + result.get(CaptureResult.STATISTICS_FACES).length);
                    try {
                        detectFaces(myBitmap);
                    } catch (IOException e) {

                    }
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private void detectFaces(Bitmap bitmap) throws IOException {
        // [START set_detector_options]
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .setMinFaceSize(0.15f)
                        .enableTracking()
                        .build();
        // [END set_detector_options]

        // [START get_detector]
        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);
        // [END get_detector]

        // [START run_detector]
//        detector.detectInImage(FirebaseVisionImage.fromFilePath(mContext, Uri.fromFile(new File(path + timeStamp+ ".jpg"))))
        detector.detectInImage(FirebaseVisionImage.fromBitmap(bitmap))
            .addOnSuccessListener(
                    new OnSuccessListener<List<FirebaseVisionFace>>() {
                        @Override
                        public void onSuccess(List<FirebaseVisionFace> faces) {
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




                                // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                                // nose available):
//                                FirebaseVisionFaceLandmark leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);
//                                if (leftEar != null) {
//                                    FirebaseVisionPoint leftEarPos = leftEar.getPosition();
//                                }
//
//                                // If classification was enabled:
//                                if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
//                                    float smileProb = face.getSmilingProbability();
//                                }
//                                if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
//                                    float rightEyeOpenProb = face.getRightEyeOpenProbability();
//                                }

                                // If face tracking was enabled:
//                                if (face.getTrackingId() != FirebaseVisionFace.INVALID_ID) {
//                                    int id = face.getTrackingId();
//                                }

                            }

                            // [END get_face_info]
                            // [END_EXCLUDE]
                        }
                    })
            .addOnFailureListener(
                    new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Task failed with an exception
                            Log.e("Face Detector", "Task failed");

                            // ...
                        }
                    });

// [END run_detector]
    }



}
