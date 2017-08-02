package com.flipcam.cameramanager;

import android.hardware.Camera;

import com.flipcam.camerainterface.CameraOperations;

/**
 * Created by Koushick on 02-08-2017.
 */

public class Camera1Manager implements CameraOperations {

    private Camera mCamera;
    private static int VIDEO_WIDTH = 640;  // default dimensions.
    private static int VIDEO_HEIGHT = 480;
    //Safe to assume every camera would support 15 fps.
    int MIN_FPS = 15;
    int MAX_FPS = 15;
    int cameraId;
    private Camera1Manager()
    {

    }
    @Override
    public void openCamera(boolean backCamera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        for(int i=0;i<Camera.getNumberOfCameras();i++)
        {
            Camera.getCameraInfo(i, info);
            if(backCamera) {
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    mCamera = Camera.open(i);
                    cameraId = i;
                    break;
                }
            }
            else{
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    mCamera = Camera.open(i);
                    cameraId = i;
                    break;
                }
            }
        }
    }

    @Override
    public void releaseCamera() {

    }

    @Override
    public void setFPS() {

    }

    @Override
    public void setResolution() {

    }

    @Override
    public void zoomInOrOut() {

    }

    @Override
    public void stopPreview() {

    }

    @Override
    public void setAutoFocus(){

    }
}
