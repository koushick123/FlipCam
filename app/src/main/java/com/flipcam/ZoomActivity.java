package com.flipcam;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.flipcam.media.FileMedia;
import com.flipcam.util.MediaUtil;

import java.io.File;

public class ZoomActivity extends AppCompatActivity {

    ImageView zoomPic;
    RelativeLayout parentPic;
    static String TAG = "ZoomActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zoom);
        String path="";
        FileMedia[] medias = MediaUtil.getMediaList(getApplicationContext());
        for(int i=0;i<medias.length;i++){
            if(medias[i].getPath().endsWith(getResources().getString(R.string.IMG_EXT)) ||
                    medias[i].getPath().endsWith(getResources().getString(R.string.ANOTHER_IMG_EXT))){
                Log.d("ZoomActivity","Path = "+medias[i].getPath());
                path = medias[i].getPath();
                break;
            }
        }
        zoomPic = (ImageView)findViewById(R.id.zoompicture);
        parentPic = (RelativeLayout)findViewById(R.id.parentPic);
        Uri uri = Uri.fromFile(new File(path));
        GlideApp.with(this).load(uri).into(zoomPic);
        scaleGestureDetector = new ScaleGestureDetector(this,new ZoomActivity.ScaleListener());
    }

    float xCoOrdinate = -1;
    float yCoOrdinate = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        onTouch(zoomPic,event);
        return true;
    }

    int pointers = 1;
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG,"ACTION_DOWN");
                xCoOrdinate = view.getX() - event.getRawX();
                yCoOrdinate = view.getY() - event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                if(pointers == 1) {
                    if (scaleFactor > 1 && scaleFactor <= 4) {
                        view.animate().x(event.getRawX() + xCoOrdinate).y(event.getRawY() + yCoOrdinate).setDuration(0).start();
                    } else {
                        view.animate().x(0).y(0).setDuration(500).start();
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                pointers++;
                Log.d(TAG,"ACTION_POINTER_DOWN = "+pointers);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                Log.d(TAG,"ACTION_POINTER_UP = "+pointers);
                break;
            case MotionEvent.ACTION_UP:
                pointers = 1;
                Log.d(TAG,"ACTION_UP");
                break;
            default:
                return false;
        }
        scaleGestureDetector.onTouchEvent(event);
        return true;
    }

    private ScaleGestureDetector scaleGestureDetector;
    float scaleFactor;
    float prevScaleFactor = -1;
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if(prevScaleFactor  != -1){
                if(Math.abs(prevScaleFactor - scaleFactor) >= 0.2){
                    Log.d("ZoomActivity","onScale scaleFactor = "+scaleFactor);
                    prevScaleFactor = scaleFactor;
                }
            }
            else{
                prevScaleFactor = scaleFactor;
            }
            scaleFactor += detector.getScaleFactor() - 1;
            scaleFactor = (float) (Math.floor(scaleFactor * 100) / 100);
            scaleFactor = scaleFactor < 1 ? 1 : (scaleFactor > 5 ? 5 : scaleFactor);
            if(scaleFactor == 1){
                zoomPic.setScaleX(scaleFactor);
                zoomPic.setScaleY(scaleFactor);
            }
            else {
                zoomPic.setScaleX(scaleFactor * 1.5f);
                zoomPic.setScaleY(scaleFactor * 1.5f);
            }
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
        }
    }
}
