package com.technicolor.homecamservice;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.TextureView;

import androidx.appcompat.app.AppCompatActivity;

public class ViewActivity extends AppCompatActivity {
    private Preview preview;
    private TextureView mCameraTextureView;
    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        mCameraTextureView = findViewById(R.id.cameraTextureView);
        mCameraTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                preview = new Preview(getApplicationContext(), mCameraTextureView);
                preview.openCamera();
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

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if(preview!=null) {
            preview.release();
        }
    }
}
