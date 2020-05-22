package com.technicolor.homecamservice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {
    private ImageView imageView;
    RecyclerView recyclerView;
    GridLayoutManager gridLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        imageView = (ImageView) findViewById(R.id.imageView);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        gridLayoutManager = new GridLayoutManager(getApplicationContext(), 3);
        recyclerView.setLayoutManager(gridLayoutManager);



        loadImages();


        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("last_captured");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                loadImages();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

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
        DataAdapter dataAdapter = new DataAdapter(getApplicationContext(), files, R.layout.item_gallery);
        recyclerView.setAdapter(dataAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent serviceIntent = new Intent(this, HomeCamService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }


    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_0){
            Preview.enableUpload = !Preview.enableUpload;
            Toast.makeText(this, "enable upload: " + Preview.enableUpload, Toast.LENGTH_LONG).show();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }
}