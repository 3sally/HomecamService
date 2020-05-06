package com.technicolor.homecamservice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import static com.technicolor.homecamservice.ExampleService.*;

public class ViewActivity extends AppCompatActivity {
    private Preview preview;
    private TextureView mCameraTextureView;
    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        mCameraTextureView = (TextureView) findViewById(R.id.cameraTextureView);
        preview = new Preview(getApplicationContext(), mCameraTextureView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        preview.openCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void CamView(View v) {
        preview.openCamera();
    }

        public void Back(View v) {
        Intent backIntent = new Intent(this, MainActivity.class);
        startActivity(backIntent);

    }
}
