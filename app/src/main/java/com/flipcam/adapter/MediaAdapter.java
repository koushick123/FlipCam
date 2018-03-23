package com.flipcam.adapter;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.flipcam.R;
import com.flipcam.media.FileMedia;

import java.io.File;

/**
 * Created by koushick on 21-Mar-18.
 */

public class MediaAdapter extends ArrayAdapter {

    public static final String TAG = "MediaAdapter";
    public MediaAdapter(Context context, FileMedia[] medias){
        super(context, 0, medias);
        mediaList = medias;
    }
    Display display;
    FileMedia[] mediaList;
    WindowManager windowManager = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
    Point screenSize=new Point();
    //A 180px X 180px size for a thumbnail would look ideal on most devices.
    int thumbnailResolution = 180;
    int numOfCols = 3;
    int thumbnailWidthAndHeight = 180;

    static class ViewHolderImage{
        GridView mediaGrid;
        FrameLayout mediaPlaceHolder;
        ImageView recordedMedia;
        ImageView playVideo;
    }

    @Override
    public int getCount() {
        return mediaList.length;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        ViewHolderImage viewHolderImage;
        FrameLayout.LayoutParams thumbnailParams;
        if(listItem == null)
        {
            listItem = LayoutInflater.from(getContext()).inflate(R.layout.media_item,parent,false);
            viewHolderImage = new ViewHolderImage();
            viewHolderImage.mediaGrid = (GridView)parent.findViewById(R.id.mediaGrid);
            viewHolderImage.mediaPlaceHolder = (FrameLayout)listItem.findViewById(R.id.mediaPlaceholder);
            viewHolderImage.recordedMedia = (ImageView)listItem.findViewById(R.id.recordedMedia);
            viewHolderImage.playVideo = (ImageView)listItem.findViewById(R.id.playVideo);
            listItem.setTag(viewHolderImage);
            display = windowManager.getDefaultDisplay();
        }
        else
        {
            viewHolderImage = (ViewHolderImage)listItem.getTag();
        }
        int orientation = getContext().getResources().getConfiguration().orientation;
        thumbnailParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        if(orientation == Configuration.ORIENTATION_PORTRAIT){
            calculateThumbnailSizeAndCols();
            viewHolderImage.mediaGrid.setNumColumns(numOfCols);
        }
        else{
            calculateThumbnailSizeAndCols();
            viewHolderImage.mediaGrid.setNumColumns(numOfCols);
        }
        thumbnailParams.width = thumbnailParams.height = thumbnailWidthAndHeight;
        viewHolderImage.recordedMedia.setLayoutParams(thumbnailParams);
        FileMedia media = mediaList[position];
        if(!isImage(media.getPath())){
            Uri uri = Uri.fromFile(new File(media.getPath()));
            Glide
                    .with(getContext())
                    .load(uri)
                    .thumbnail(0.1f)
                    .into(viewHolderImage.recordedMedia);
            viewHolderImage.playVideo.setVisibility(View.VISIBLE);
            viewHolderImage.playVideo.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_play_circle_outline));
        }
        else{
            Uri uri = Uri.fromFile(new File(media.getPath()));
            Glide.with(getContext()).load(uri).into(viewHolderImage.recordedMedia);
            viewHolderImage.playVideo.setVisibility(View.INVISIBLE);
        }
        return listItem;
    }

    boolean isImage(String path)
    {
        if(path.endsWith(getContext().getResources().getString(R.string.IMG_EXT)) || path.endsWith(getContext().getResources().getString(R.string.ANOTHER_IMG_EXT))){
            return true;
        }
        return false;
    }

    void calculateThumbnailSizeAndCols(){
        display.getRealSize(screenSize);
        numOfCols = screenSize.x / thumbnailResolution;
//        Log.d(TAG, "numofCols = "+numOfCols);
        int verticalSpace = (int)getContext().getResources().getDimension(R.dimen.verticalSpace);
        int totalVerticalSpace = numOfCols * verticalSpace;
        thumbnailWidthAndHeight = (screenSize.x - totalVerticalSpace) / numOfCols;
//        Log.d(TAG, "thumbnailWidthAndHeight = "+thumbnailWidthAndHeight);
    }
}
