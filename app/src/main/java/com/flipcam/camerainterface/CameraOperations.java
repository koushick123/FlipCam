package com.flipcam.camerainterface;

import android.graphics.SurfaceTexture;
import android.view.WindowManager;

/**
 * Created by Koushick on 02-08-2017.
 * Contains a lit of camera related operations.
 * This Interface will be the only way to use a camera in this app. This will have two implementations. One for Camera 1 API and another for Camera 2 API.
 * This is to promote loose coupling.
 */

public interface CameraOperations {
    //To open camera
    public void openCamera(boolean backCam);
    //To release camera
    public void releaseCamera();
    //To set FPS
    public void setFPS();
    //To set Resolution
    public void setResolution();
    //To set Resolution with windowmanager
    public void setResolution(WindowManager windowManager);
    //To set zoom
    public boolean zoomInOrOut(boolean zoomInOrOut);
    //To stop preview
    public void stopPreview();
    //To set auto focus
    public boolean setAutoFocus();
    //To start preview
    public void startPreview(SurfaceTexture surfaceTexture);
    //To set auto flash
    public boolean setAutoFlash();
    //To set flashlight on
    public boolean setFlashOnOff();
}
