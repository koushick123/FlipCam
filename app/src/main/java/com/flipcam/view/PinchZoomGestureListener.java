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
    int progress = 0;
    public PinchZoomGestureListener(Context context, VideoFragment vFrag, PhotoFragment pFrag){
        appContext = context;
        videoFragment = vFrag;
        photoFragment = pFrag;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if(detector.getCurrentSpan() - detector.getPreviousSpan() > 0){
            Log.d(TAG, "Zoom IN = "+progress++);
            /*if(progress < videoFragment.getCameraMaxZoom()) {
                progress += 3;
            }*/
        }
        else if(detector.getCurrentSpan() - detector.getPreviousSpan() < 0){
            Log.d(TAG, "Zoom OUT = "+progress--);
            /*if(progress > 0) {
                progress -= 3;
            }
            else{
                progress = 0;
            }*/
        }
//        performZoomOperation(progress);
        return true;
    }

    private void performZoomOperation(int progressValue){
        if(videoFragment != null){
            cameraView = videoFragment.getCameraView();
            if(cameraView.isCameraReady()) {
                if (cameraView.isSmoothZoomSupported()) {
                    //if(VERBOSE)Log.d(TAG, "Smooth zoom supported");
                    cameraView.smoothZoomInOrOut(progressValue);
                } else if (cameraView.isZoomSupported()) {
                    cameraView.zoomInAndOut(progressValue);
                }
            }
        }
    }
}
