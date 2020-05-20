package com.technicolor.homecamservice;

import android.content.Context;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class GalleryActivity extends AppCompatActivity {
    private ImageView imageView;
    RecyclerView recyclerView;
    GridLayoutManager gridLayoutManager;
    ImagePath mImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImagePath  = new ImagePath(getApplicationContext());
        setContentView(R.layout.activity_gallery);
        imageView = (ImageView) findViewById(R.id.imageView);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        gridLayoutManager = new GridLayoutManager(getApplicationContext(), 1);
        recyclerView.setLayoutManager(gridLayoutManager);

        ArrayList imageUrlList = prepareData();
        String[] textList = getTextlist();
        DataAdapter dataAdapter = new DataAdapter(getApplicationContext(), imageUrlList, textList);
        recyclerView.setAdapter(dataAdapter);
    }

    private ArrayList prepareData() {
        return mImagePath.imagePath();
    }
    private String[] getTextlist(){
        return mImagePath.textList();
    }
}