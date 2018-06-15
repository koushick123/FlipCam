package com.flipcam.view;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.flipcam.R;

import java.io.IOException;

/**
 * Created by Koushick on 29-11-2017.
 */

public class MediaView extends SurfaceView implements SurfaceHolder.Callback {

    public static final String TAG = "MediaView";
    MediaPlayer mPlayer;
    String mediaPath;
    MediaFragment mediaFragment;
    boolean VERBOSE = false;

    public MediaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if(VERBOSE)Log.d(TAG,"start mediaView");
        getHolder().addCallback(this);
    }

    public void setData(MediaPlayer mediaPlayer, String path, MediaFragment fragment){
        mPlayer = mediaPlayer;
        mediaPath = path;
        mediaFragment = fragment;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if(VERBOSE)Log.d(TAG,"surfaceCreated = "+mediaPath);
        if(!isImage()){
            mPlayer.setDisplay(surfaceHolder);
            try {
                mPlayer.setDataSource("file://"+mediaPath);
                mPlayer.prepare();
                if(VERBOSE)Log.d(TAG,"MP prepared");
                if(mediaFragment.getUserVisibleHint()) {
                    if(VERBOSE)Log.d(TAG, "SAVED VIDEO for min = " + mediaFragment.savedVideo);
                    if (mediaFragment.savedVideo != null) {
                        mediaFragment.reConstructVideo(mediaFragment.savedVideo);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(mediaFragment.getUserVisibleHint() && !mediaFragment.isStartTracker()){
                mediaFragment.startTrackerThread();
            }
        }
        mediaFragment.fitVideoToScreen();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if(VERBOSE)Log.d(TAG,"surfaceDestroyed = "+mediaPath);
        if(!isImage()){
            if(mediaFragment.getUserVisibleHint()) {
                if(VERBOSE)Log.d(TAG, "Reset");
                mediaFragment.stopTrackerThread();
                try {
                    if(mediaFragment.videoTracker!=null) {
                        mediaFragment.videoTracker.join();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mPlayer.reset();
        }
    }

    public boolean isImage()
    {
        if(mediaPath.endsWith(getResources().getString(R.string.IMG_EXT)) || mediaPath.endsWith(getResources().getString(R.string.ANOTHER_IMG_EXT))){
            return true;
        }
        return false;
    }
}
