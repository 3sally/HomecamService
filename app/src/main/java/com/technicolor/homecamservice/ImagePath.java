package com.technicolor.homecamservice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class ImagePath {

    private String imageUrl;
    private Context mContext;
    @SuppressLint("SdCardPath")
//    private String path = "/data/user/0/com.technicolor.homecamservice/files/Captured/";

    private File pathFile;
    private File[] files;

    ImagePath(Context context){
        this.mContext = context;
    }

    public ArrayList imagePath() {
        String path = mContext.getFilesDir().getPath() +"/Captured";
        pathFile= new File(path);
        files = pathFile.listFiles();
        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));
        fileList.addAll(Arrays.asList(files));
        Log.e("Length", String.valueOf(fileList.size()));
        for(int i=0; i<files.length; i++){
            setImageUrl(fileList.get(i).getName());
        }
        return fileList;
    }
    public String[] textList(){
        String[] tempText = new String[files.length];
        Log.e("length", ""+files.length);
        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));
        fileList.addAll(Arrays.asList(files));
        for(int i=0; i<files.length; i++){
            tempText[i] = (fileList.get(i).getName());
        }

        return tempText;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl){
        this.imageUrl =imageUrl;
    }
}
