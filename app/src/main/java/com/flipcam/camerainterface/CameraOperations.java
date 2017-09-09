package com.flipcam.camerainterface;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import java.util.List;

/**
 * Created by Koushick on 02-08-2017.
 * Contains a lit of camera related operations.
 * This Interface will be the only way to use a camera in this app. This will have two implementations. One for Camera 1 API and another for Camera 2 API.
 * This is to promote loose coupling, since only the implementation methods will be called in the Photo and Video fragments.
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
    //To set Resolution with width and height
    public void setResolution(int width,int height);
    //To set zoom
    public boolean zoomInOrOut(int zoomInOrOut);
    //To check if smooth zoom is supported
    public boolean isSmoothZoomSupported();
    //To zoom in/out smoothly
    public void smoothZoomInOrOut(int zoomInOrOut);
    //To check if normal zoom is supported
    public boolean isZoomSupported();
    //To get max zoom
    public int getMaxZoom();
    //To stop preview
    public void stopPreview();
    //To set auto focus
    public void setAutoFocus();
    //To set recording hint for certain optimization done by camera
    public void setRecordingHint();
    //To disable recording hint
    public void disableRecordingHint();
    //To cancel auto focus
    public void cancelAutoFocus();
    //To set a particular focus mode
    public void setFocusMode(String focusMode);
    //To start preview
    public void startPreview(SurfaceTexture surfaceTexture);
    //To set auto flash
    public void setAutoFlash();
    //To set flash on/off
    public void setFlashOnOff(boolean flashOn);
    //To get current flash mode
    public String getFlashMode();
    //To get focus mode
    public String getFocusMode();
    //To check if flash mode is supported
    public boolean isFlashModeSupported(String flashMode);
    //To check if focus mode is supported
    public boolean isFocusModeSupported(String focusMode);
    //To set flash as torchlight
    public void setTorchLight();
    //Fetch camera instance
    public int getCameraId();
    //Check if camera instance is ready to be used
    public boolean isCameraReady();
    //To set display orientation to match that of the activity/frame
    public void setDisplayOrientation(int result);
    //To get supported picture sizes
    public List<Camera.Size> getSupportedPictureSizes();
    //To set picture size
    public void setPictureSize(int width, int height);
    //To capture picture
    public boolean capturePicture();
}
