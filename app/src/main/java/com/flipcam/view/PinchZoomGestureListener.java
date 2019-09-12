package com.flipcam.view;

import android.content.Context;
import android.util.Log;
import android.view.ScaleGestureDetector;

import com.flipcam.PhotoFragment;
import com.flipcam.VideoFragment;

public class PinchZoomGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener{

    public static final String TAG = "PinchZoomGestureListner";
    VideoFragment videoFragment;
    PhotoFragment photoFragment;
    CameraView cameraView;
    Context appContext;
    float progress = 0.0f;
    float progressZoomInStep = 1f;
    float progressZoomOutStep = 1.5f;
    Boolean isSmoothZoom = null;
    int cameraMaxZoom = -1;
    float zoomInSensitivity = 1f;
    float zoomOutSensitivity = -13f;
    int zoomLevel = -1;
    public PinchZoomGestureListener(Context context, VideoFragment vFrag, PhotoFragment pFrag){
        appContext = context;
        videoFragment = vFrag;
        photoFragment = pFrag;
    }

    private void checkSmoothZoom(){
        if(videoFragment != null) {
            cameraView = videoFragment.getCameraView();
            cameraMaxZoom = videoFragment.getCameraMaxZoom();
        }
        else{
            cameraView = photoFragment.getCameraView();
            cameraMaxZoom = photoFragment.getCameraMaxZoom();
        }

        Log.d(TAG, "cameraMaxZoom = "+cameraMaxZoom);
        zoomLevel = (int)Math.ceil(cameraMaxZoom / 10);
        Log.d(TAG, "zoom level = "+zoomLevel);

        if (cameraView.isSmoothZoomSupported()) {
            isSmoothZoom = true;
        } else if (cameraView.isZoomSupported()) {
            isSmoothZoom = false;
        }
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        Log.d(TAG, "onScaleBegin");
        if(isSmoothZoom == null){
            checkSmoothZoom();
        }
        return super.onScaleBegin(detector);
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        Log.d(TAG, "onScaleEnd");
        super.onScaleEnd(detector);
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if(detector.isInProgress()) {
            float currentSpan = detector.getCurrentSpan();
            float previousSpan = detector.getPreviousSpan();
            Log.d(TAG, "currentSpan = "+currentSpan+" , previousSpan = "+previousSpan);
            if (currentSpan - previousSpan > zoomInSensitivity)
            {
                if (progress < cameraMaxZoom) {
                    Log.d(TAG, "Zoom IN = " + progress);
                    progress += progressZoomInStep;
                    performZoomOperation((int)progress);
                    return true;
                }
            }
            else if (currentSpan - previousSpan < zoomOutSensitivity)
            {
                if (progress > 0) {
                    Log.d(TAG, "Zoom OUT = " + progress);
                    progress -= progressZoomOutStep;
                    performZoomOperation((int)progress);
                    return true;
                } else {
                    progress = 0;
                }
            }
            return false;
        }
        else{
            return false;
        }
    }

    private void performZoomOperation(int progressValue){
        if(isSmoothZoom) {
            cameraView.smoothZoomInOrOut(progressValue);
        }
        else{
            cameraView.zoomInAndOut(progressValue);
        }
    }
}
