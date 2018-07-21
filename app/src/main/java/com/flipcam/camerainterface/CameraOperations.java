package com.flipcam.camerainterface;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import com.flipcam.PhotoFragment;
import com.flipcam.VideoFragment;
import com.flipcam.view.CameraView;

import java.util.List;

/**
 * Created by Koushick on 02-08-2017.
 * Contains a list of camera related operations.
 * This Interface will be the only way to use a camera in this app. This will have two implementations. One for Camera 1 API and another for Camera 2 API.
 * This will promote loose coupling, since only minimal code changes will be necessary in the Photo or Video fragments.
 *
 * This will contain methods which are Camera 1 and Camera 2 API specific. Those methods that do not have a purpose for a specific API, will need an empty
 * implementation.
 */

 public interface CameraOperations {
    //To open camera
     void openCamera(boolean backCam, Context context);
    //To release camera
     void releaseCamera();
    //To set FPS
     void setFPS();
    //To set Resolution
     void setResolution();
    //To set Resolution with width and height
     void setResolution(int width,int height);
    //To set zoom
     boolean zoomInOrOut(int zoomInOrOut);
    //To check if smooth zoom is supported
     boolean isSmoothZoomSupported();
    //To zoom in/out smoothly
     void smoothZoomInOrOut(int zoomInOrOut);
    //To check if normal zoom is supported
     boolean isZoomSupported();
    //To get max zoom
     int getMaxZoom();
    //To stop preview
     void stopPreview();
    //To set auto focus
     void setAutoFocus();
    //To set recording hint for certain optimization done by camera
     void setRecordingHint();
    //To disable recording hint
     void disableRecordingHint();
    //To cancel auto focus
     void cancelAutoFocus();
    //To set a particular focus mode
     void setFocusMode(String focusMode);
    //To start preview
     void startPreview(SurfaceTexture surfaceTexture);
    //To set auto flash
     void setAutoFlash();
    //To set flash on/off
     void setFlashOnOff(boolean flashOn);
    //To get current flash mode
     String getFlashMode();
    //To get focus mode
     String getFocusMode();
    //To check if flash mode is supported
     boolean isFlashModeSupported(String flashMode);
    //To check if focus mode is supported
     boolean isFocusModeSupported(String focusMode);
    //To set flash as torchlight
     void setTorchLight();
    //Fetch camera instance
     int getCameraId();
    //Check if camera instance is ready to be used
     boolean isCameraReady();
    //To set display orientation to match that of the activity/frame
     void setDisplayOrientation(int result);
    //To get supported picture sizes
     List<Camera.Size> getSupportedPictureSizes();
    //To set picture size
     void setPictureSize(int width, int height);
     //To set Video Size for Recording
      void setVideoSize();
      //To capture picture
     void capturePicture();
    //Fetch AF mode
    String getFocusModeAuto();
    //Fetch flash mode off
    String getFlashModeOff();
    //Fetch focus mode video
    String getFocusModeVideo();
    //Fetch focus mode picture
    String getFocusModePicture();
    //Fetch flash mode torch
    String getFlashModeTorch();
    //Get camera info (For Camera 1 API)
    Camera.CameraInfo getCameraInfo();
    //Get display sizes for recording
    int[] getDisplaySizes();
    //Set picture orientation (For Camera 1 API)
    void setRotation(int rotation);
    //The below methods are not necessary for camera, but are included for the sake of maintaining loose coupling between CameraView and Camera APIs
    void setPhotoFragmentInstance(PhotoFragment photoFragment);
    void setPhotoPath(String mediaPath);
    void setRotation(float rot);
    void removePreviewCallback();
    void setSurfaceView(CameraView surfaceView);
    //For video fragment instance
    void setVideoFragmentInstance(VideoFragment videoFragment);
}
