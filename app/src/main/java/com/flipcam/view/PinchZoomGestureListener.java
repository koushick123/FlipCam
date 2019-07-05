package com.flipcam.view;

import android.content.Context;
import android.util.Log;
import android.view.ScaleGestureDetector;

public class PinchZoomGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

    public static final String TAG = "PinchZoomGestureListner";
    Context appContext;
    public PinchZoomGestureListener(Context context){
        appContext = context;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        Log.d(TAG, "scale factor = "+detector.getScaleFactor());
        return true;
    }
}
