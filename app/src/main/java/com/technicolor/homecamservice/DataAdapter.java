package com.technicolor.homecamservice;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {
    ArrayList<ImagePath> imageUrls;
    private Context context;
    private String[] textSet;

    public DataAdapter(Context context, ArrayList<ImagePath> imageUrls, String[] textSet) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.textSet = textSet;

    }

    @Override
    public DataAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.image_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    /**
     * gets the image url from adapter and passes to Glide API to load the image
     *
     * @param viewHolder
     * @param i
     */
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        Log.e("imageUrls",""+imageUrls.get(i));
        Glide.with(context).load(imageUrls.get(i)).into(viewHolder.imageView);
        viewHolder.textView.setText(this.textSet[i]);

    }

    @Override
    public int getItemCount() {
        //??????
        return imageUrls.size()/2;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView imageView;

        public ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.imageView);
            textView =view.findViewById(R.id.gallView);
        }
    }

}