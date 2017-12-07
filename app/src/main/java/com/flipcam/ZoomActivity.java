package com.flipcam;

import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import com.flipcam.media.FileMedia;
import com.flipcam.util.MediaUtil;

import java.io.File;

public class ZoomActivity extends AppCompatActivity {

    ImageView zoomPic;
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
        zoomPic= (ImageView)findViewById(R.id.zoompicture);
        Uri uri = Uri.fromFile(new File(path));
        GlideApp.with(this).load(uri).into(zoomPic);
        zoomPic.setImageMatrix(matrix);
        scaleGestureDetector = new ScaleGestureDetector(this,new ZoomActivity.ScaleListener());
    }

    float factor;
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        //Log.d("ZoomActivity","onTouchEvent = "+ev.getAction());
        scaleGestureDetector.onTouchEvent(ev);
        return true;
    }

    private ScaleGestureDetector scaleGestureDetector;
    private Matrix matrix = new Matrix();
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor() - 1;
            factor += scaleFactor;
            Log.d("ZoomActivity","onScale = "+factor);
            factor = factor < 1 ? 1 : factor;
            zoomPic.setScaleX(factor);
            zoomPic.setScaleY(factor);
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            factor = 1.0f;
            return true;
            //return super.onScaleBegin(detector);
        }
    }
}
