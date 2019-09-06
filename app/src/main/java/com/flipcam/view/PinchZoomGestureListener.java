package com.flipcam.view;

import android.content.Context;
import android.util.Log;
import android.view.ScaleGestureDetector;

import com.flipcam.PhotoFragment;
import com.flipcam.VideoFragment;

public class PinchZoomGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

    public static final String TAG = "PinchZoomGestureListner";
    VideoFragment videoFragment;
    PhotoFragment photoFragment;
    CameraView cameraView;
    Context appContext;
    float progress = 0.0f;
    float progressStep = 1.5f;
    Boolean isSmoothZoom = null;
    int cameraMaxZoom = -1;
    int zoomLevel = -1;
    //Set max zoom in restriction to 5.0x level
    int restrictedMaxZoom = -1;
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

        restrictedMaxZoom = (int)(cameraMaxZoom * 0.5);
        Log.d(TAG, "cameraMaxZoom = "+cameraMaxZoom);
        Log.d(TAG, "restrictedMaxZoom = "+restrictedMaxZoom);
        zoomLevel = (int)Math.ceil(restrictedMaxZoom / 10);
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
            if (detector.getCurrentSpan() - detector.getPreviousSpan() > 0) {
                if (progress < restrictedMaxZoom) {
                    Log.d(TAG, "Zoom IN = " + progress);
                    progress += progressStep;
                }
            } else if (detector.getCurrentSpan() - detector.getPreviousSpan() < 0) {
                if (progress > 0) {
                    Log.d(TAG, "Zoom OUT = " + progress);
                    progress -= progressStep;
                } else {
                    progress = 0;
                }
            }
            performZoomOperation((int)progress);
            return true;
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
