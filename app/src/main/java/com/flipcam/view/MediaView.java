package com.flipcam.view;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.flipcam.R;

import java.io.IOException;

import static android.R.attr.path;

/**
 * Created by Koushick on 29-11-2017.
 */

public class MediaView extends SurfaceView implements SurfaceHolder.Callback {

    public static final String TAG = "MediaView";
    MediaPlayer mPlayer;
    String mediaPath;
    SurfaceViewVideoFragment surfaceViewVideoFragment;

    public MediaView(Context context, AttributeSet attrs, MediaPlayer mediaPlayer, String path, SurfaceViewVideoFragment fragment) {
        super(context, attrs);
        Log.d(TAG,"start mediaView");
        mPlayer = mediaPlayer;
        mediaPath = path;
        surfaceViewVideoFragment = fragment;
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG,"surfaceCreated");
        mPlayer.setDisplay(surfaceHolder);
        try {
            mPlayer.setDataSource("file://"+path);
            mPlayer.prepare();
            Log.d(TAG,"MP prepared");
            if(surfaceViewVideoFragment.getUserVisibleHint()) {
                /*Log.d(TAG, "SAVED VIDEO for min = " + savedVideo);
                if (savedVideo != null) {
                    reConstructVideo(savedVideo);
                }*/
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    public boolean isImage()
    {
        if(mediaPath.endsWith(getResources().getString(R.string.IMG_EXT)) || mediaPath.endsWith(getResources().getString(R.string.ANOTHER_IMG_EXT))){
            return true;
        }
        return false;
    }
}
