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
    SurfaceViewVideoFragment surfaceViewVideoFragment;

    public MediaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG,"start mediaView");
        getHolder().addCallback(this);
    }

    public void setData(MediaPlayer mediaPlayer, String path, SurfaceViewVideoFragment fragment){
        mPlayer = mediaPlayer;
        mediaPath = path;
        surfaceViewVideoFragment = fragment;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG,"surfaceCreated = "+mediaPath);
        if(!isImage()){
            mPlayer.setDisplay(surfaceHolder);
            try {
                mPlayer.setDataSource("file://"+mediaPath);
                mPlayer.prepare();
                Log.d(TAG,"MP prepared");
                if(surfaceViewVideoFragment.getUserVisibleHint()) {
                    Log.d(TAG, "SAVED VIDEO for min = " + surfaceViewVideoFragment.savedVideo);
                    if (surfaceViewVideoFragment.savedVideo != null) {
                        surfaceViewVideoFragment.reConstructVideo(surfaceViewVideoFragment.savedVideo);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(surfaceViewVideoFragment.getUserVisibleHint()){
                surfaceViewVideoFragment.startTrackerThread();
            }
        }
        surfaceViewVideoFragment.fitVideoToScreen();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG,"surfaceDestroyed = "+mediaPath);
        if(!isImage()){
            if(surfaceViewVideoFragment.getUserVisibleHint()) {
                Log.d(TAG, "Reset");
                surfaceViewVideoFragment.stopTrackerThread();
                try {
                    if(surfaceViewVideoFragment.videoTracker!=null) {
                        surfaceViewVideoFragment.videoTracker.join();
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
