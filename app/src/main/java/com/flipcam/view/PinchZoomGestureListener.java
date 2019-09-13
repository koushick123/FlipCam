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
    float zoomDiff = 0.5f;
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
            if(cameraMaxZoom <= 10){
                progressZoomInStep = 0.25f;
                progressZoomOutStep = 0.5f;
                zoomDiff = 0.25f;
            }
            else if(cameraMaxZoom > 10 && cameraMaxZoom <= 20){
                progressZoomInStep = 0.35f;
                progressZoomOutStep = 0.70f;
                zoomDiff = 0.2f;
            }
            else if(cameraMaxZoom > 20 && cameraMaxZoom <= 30){
                progressZoomInStep = 0.4f;
                progressZoomOutStep = 0.8f;
                zoomDiff = 0.2f;
            }
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
//            Log.d(TAG, "currentSpan = "+currentSpan+" , previousSpan = "+previousSpan);
            if (currentSpan - previousSpan > zoomInSensitivity)
            {
                if (progress < cameraMaxZoom) {
                    Log.d(TAG, "Zoom IN = " + progress);
                    progress += progressZoomInStep;
                    performZoomOperation();
                    return true;
                }
            }
            else if (currentSpan - previousSpan < zoomOutSensitivity)
            {
                if (progress > 0) {
                    Log.d(TAG, "Zoom OUT = " + progress);
                    progress -= progressZoomOutStep;
                    performZoomOperation();
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

    public void setProgress(int progressValue){
        progress = progressValue;
    }

    private void performZoomOperation(){
        if(isSmoothZoom) {
            cameraView.smoothZoomInOrOut((int)progress);
        }
        else{
            cameraView.zoomInAndOut((int)progress);
        }
        double closestWholeNum = Math.ceil(progress);
        Log.d(TAG, "closestWholeNum = "+closestWholeNum+", for progress = "+progress);
        if(Math.abs(closestWholeNum - progress) <= zoomDiff) {
            incrementProgressBar();
        }
    }

    private void incrementProgressBar(){
        if(videoFragment!=null){
            videoFragment.getZoomBar().setProgress((int)progress);
        }
        else{
            photoFragment.getZoomBar().setProgress((int)progress);
        }
    }
}
