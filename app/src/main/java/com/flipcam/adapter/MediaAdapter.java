package com.flipcam.adapter;

import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    }

    static class ViewHolderImage{
        GridView mediaGrid;
        FrameLayout mediaPlaceHolder;
        ImageView recordedMedia;
        ImageView playVideo;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Log.d(TAG, "getView = "+position);
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
        }
        else
        {
            viewHolderImage = (ViewHolderImage)listItem.getTag();
        }
        int orientation = getContext().getResources().getConfiguration().orientation;
        thumbnailParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        if(orientation == Configuration.ORIENTATION_PORTRAIT){
            thumbnailParams.width = (int)getContext().getResources().getDimension(R.dimen.gridThumbnailWidth);
            thumbnailParams.height = (int)getContext().getResources().getDimension(R.dimen.gridThumbnailHeight);
            viewHolderImage.recordedMedia.setLayoutParams(thumbnailParams);
            viewHolderImage.mediaGrid.setNumColumns(4);
        }
        else{
            thumbnailParams.width = (int)getContext().getResources().getDimension(R.dimen.gridThumbnailWidthLandscape);
            thumbnailParams.height = (int)getContext().getResources().getDimension(R.dimen.gridThumbnailHeightLandscape);
            viewHolderImage.recordedMedia.setLayoutParams(thumbnailParams);
            viewHolderImage.mediaGrid.setNumColumns(4);
        }
        viewHolderImage.mediaGrid.setVerticalSpacing(1);
        FileMedia media = (FileMedia)getItem(position);
        Log.d(TAG, "Path = "+media.getPath());
        if(!isImage(media.getPath())){
            Uri uri = Uri.fromFile(new File(media.getPath()));
            Glide
                    .with(getContext())
                    .load(uri)
                    .thumbnail(0.2f)
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

    public boolean isImage(String path)
    {
        if(path.endsWith(getContext().getResources().getString(R.string.IMG_EXT)) || path.endsWith(getContext().getResources().getString(R.string.ANOTHER_IMG_EXT))){
            return true;
        }
        return false;
    }
}
