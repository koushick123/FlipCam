package com.flipcam.cameramanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import com.flipcam.PermissionActivity;
import com.flipcam.PhotoFragment;
import com.flipcam.camerainterface.CameraOperations;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Koushick on 02-08-2017.
 */

public class Camera1Manager implements CameraOperations, Camera.OnZoomChangeListener, Camera.ShutterCallback,Camera.PictureCallback {

    private Camera mCamera;
    public final String TAG = "Camera1Manager";
    private static int VIDEO_WIDTH = 640;  // default dimensions.
    private static int VIDEO_HEIGHT = 480;
    //Safe to assume every camera would support 15 fps.
    int MIN_FPS = 15;
    int MAX_FPS = 15;
    int cameraId;
    Camera.Parameters parameters;
    Camera.CameraInfo info = new Camera.CameraInfo();
    private PhotoFragment photoFrag;
    String photoPath;
    float rotation;
    private static Camera1Manager camera1Manager;
    Bitmap photo;
    public static Camera1Manager getInstance()
    {
        if(camera1Manager == null){
            camera1Manager = new Camera1Manager();
        }
        return camera1Manager;
    }

    public int[] getPreviewSizes()
    {
        int[] sizes = new int[2];
        sizes[0] = VIDEO_WIDTH;
        sizes[1] = VIDEO_HEIGHT;
        return sizes;
    }

    public Camera.CameraInfo getCameraInfo()
    {
        return info;
    }

    @Override
    public List<Camera.Size> getSupportedPictureSizes() {
        return mCamera.getParameters().getSupportedPictureSizes();
    }

    @Override
    public void openCamera(boolean backCamera) {
        int cameraId = -1;
        for(int i=0;i<Camera.getNumberOfCameras();i++)
        {
            Camera.getCameraInfo(i, info);
            if(backCamera) {
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    mCamera = Camera.open(i);
                    Log.d(TAG,"Open back facing camera");
                    cameraId = i;
                    break;
                }
            }
            else{
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    mCamera = Camera.open(i);
                    Log.d(TAG,"Open front facing camera");
                    cameraId = i;
                    break;
                }
            }
        }
        if(cameraId != -1) {
            parameters = mCamera.getParameters();
            parameters.setExposureCompensation((parameters.getMaxExposureCompensation()-3) > 0 ? parameters.getMaxExposureCompensation()-3 : 0);
            mCamera.setParameters(parameters);
            Log.d(TAG,"exp comp set = "+parameters.getExposureCompensation());
        }
        else{
            mCamera=null;
        }
    }

    @Override
    public void releaseCamera() {
        mCamera.release();
        mCamera = null;
        zoomChangeListener = false;
    }

    public void removePreviewCallback()
    {
        mCamera.setPreviewCallback(null);
    }

    @Override
    public void setFPS() {
        List<int[]> fps = parameters.getSupportedPreviewFpsRange();
        Iterator<int[]> iter = fps.iterator();

        while(iter.hasNext())
        {
            int[] frames = iter.next();
            if(!iter.hasNext())
            {
                MIN_FPS = frames[0];
                MAX_FPS = frames[1];
            }
        }
        Log.d(TAG,"Setting min and max Fps  == "+MIN_FPS+" , "+MAX_FPS);
        parameters.setPreviewFpsRange(MIN_FPS,MAX_FPS);
        mCamera.setParameters(parameters);
    }

    @Override
    public void setDisplayOrientation(int result) {
        mCamera.setDisplayOrientation(result);
    }

    @Override
    public void setResolution(int width, int height) {
        Log.d(TAG,"Set Width = "+width);
        Log.d(TAG,"Set Height = "+height);

        //Aspect ratio needs to be reversed, if orientation is portrait.
        double screenAspectRatio = 1.0f / ((double)width/(double)height);
        Log.d(TAG,"SCREEN Aspect Ratio = "+screenAspectRatio);
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();

        //If none of the camera preview size will (closely) match with screen resolution, default it to take the first preview size value.
        VIDEO_HEIGHT = previewSizes.get(0).height;
        VIDEO_WIDTH = previewSizes.get(0).width;
        for(int i = 0;i<previewSizes.size();i++)
        {
            double ar = (double)previewSizes.get(i).width/(double)previewSizes.get(i).height;
            Log.d(TAG,"Aspect ratio for "+previewSizes.get(i).width+" / "+previewSizes.get(i).height+" is = "+ar);
            if(Math.abs(screenAspectRatio - ar) <= 0.2){
                //Best match for camera preview!!
                VIDEO_HEIGHT = previewSizes.get(i).height;
                VIDEO_WIDTH = previewSizes.get(i).width;
                break;
            }
        }
        Log.d(TAG,"HEIGTH == "+VIDEO_HEIGHT+", WIDTH == "+VIDEO_WIDTH);
        parameters.setPreviewSize(VIDEO_WIDTH, VIDEO_HEIGHT);
        mCamera.setParameters(parameters);
    }

    public void setRotation(int rotation)
    {
        parameters.setRotation(rotation);
        mCamera.setParameters(parameters);
    }

    @Override
    public void setResolution() {
        //This can be used for Camera 2 API implementation if necessary.
    }

    @Override
    public boolean isZoomSupported() {
        return parameters.isZoomSupported();
    }

    @Override
    public boolean zoomInOrOut(int zoomInOrOut) {
        Log.d(TAG,"Current zoom = "+zoomInOrOut);
        if(isZoomSupported() && zoomInOrOut >= 0 && zoomInOrOut <= parameters.getMaxZoom())
        {
            Log.d(TAG,"Set Current zoom = "+zoomInOrOut);
            parameters.setZoom(zoomInOrOut);
            mCamera.setParameters(parameters);
            return true;
        }
        else
        {
            return false;
        }
    }

    boolean zoomChangeListener = false;
    public boolean isSmoothZoomSupported()
    {
        Log.d(TAG,"smooth zoom = "+parameters.isSmoothZoomSupported());
        //Add a zoomchangelistener flag so that it is not set every time this is called
        if(parameters.isSmoothZoomSupported() && !zoomChangeListener)
        {
            mCamera.setZoomChangeListener(this);
            zoomChangeListener = true;
        }
        return parameters.isSmoothZoomSupported();
    }

    public void smoothZoomInOrOut(int zoomInOrOut)
    {
        mCamera.startSmoothZoom(zoomInOrOut);
    }

    @Override
    public int getMaxZoom() {
        return parameters.getMaxZoom();
    }

    @Override
    public void stopPreview() {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
    }

    @Override
    public void startPreview(SurfaceTexture surfaceTexture) {
        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mCamera.startPreview();
    }
    FileOutputStream picture = null;
    @Override
    public void capturePicture() {
        photo=null;
        int zoomedVal = photoFrag.getZoomBar().getProgress();
        //Focus only if no focus and zoomed out. AF zooms out completely.
        /*if(isFocusModeSupported(Camera.Parameters.FOCUS_MODE_AUTO) && !isAutoFocus() && zoomedVal == 0){
            Log.d(TAG,"AF and take pic");
            takePic=true;
            setAutoFocus();
        }*/
        //else {
            Log.d(TAG,"take pic");
//            capture=true;
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setZoom(zoomedVal);
            mCamera.takePicture(this, null, null, this);
            mCamera.setParameters(parameters);
        //}
    }

    public void setPhotoFragmentInstance(PhotoFragment photoFragment){
        photoFrag = photoFragment;
    }

    public void setPhotoPath(String mediaPath)
    {
        photoPath = mediaPath;
    }

    public void setRotation(float rot){
        rotation = rot;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.d(TAG, "Picture wil be saved at loc = " + photoPath);
        try {
            picture = new FileOutputStream(photoPath);
            picture.write(data);
            picture.close();
            SharedPreferences.Editor editor = photoFrag.getActivity().getSharedPreferences(PermissionActivity.FC_SHARED_PREFERENCE, Context.MODE_PRIVATE).edit();
            editor.putBoolean("videoCapture",false);
            editor.commit();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if(photoFrag!=null && photoFrag.isFlashOn()){
                setFlashOnOff(false);
            }
            //Start the preview no matter if photo is saved or not.
            Log.d(TAG, "photo is ready");
            camera.startPreview();
            photoFrag.showImageSaved();
            photoFrag.showCapturedImagePreview(photoPath);
            photoFrag.getCapturePic().setClickable(true);
            photoFrag.hideImagePreview();
        }
    }

    @Override
    public void onShutter() {
        Log.d(TAG,"Photo captured");
    }

    @Override
    public void setPictureSize(int width, int height) {
        List<Integer> formats = mCamera.getParameters().getSupportedPictureFormats();
        for(int i=0;i<formats.size();i++)
        {
            if(formats.get(i) == ImageFormat.NV21){
                mCamera.getParameters().setPreviewFormat(formats.get(i));
                break;
            }
        }
        mCamera.getParameters().setPictureSize(width,height);
    }

    @Override
    public boolean isFocusModeSupported(String focusMode) {
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes != null && focusModes.size() > 0) {
            Iterator<String> iterator = focusModes.iterator();
            while (iterator.hasNext()) {
                String focus = iterator.next();
                if(focus.equalsIgnoreCase(focusMode)){
                    return true;
                }
            }
            return false;
        }
        else{
            return false;
        }
    }

    public void setTakePic(boolean takePic1)
    {
        takePic = takePic1;
    }

    boolean focused=false;
    boolean takePic=false;
    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if(success) {
                Log.d(TAG,"auto focus set successfully");
                focused = success;
                /*if(takePic)
                {
                    capture=true;
                    takePic=false;
                    camera.takePicture(camera1Manager, null, null, camera1Manager);
                }*/
            }
        }
    };

    //Use this method to keep checking until auto focus is set
    public boolean isAutoFocus(){
        return focused;
    }

    @Override
    public void cancelAutoFocus() {
        mCamera.cancelAutoFocus();
        focused = false;
    }

    @Override
    public void setAutoFocus(){
        mCamera.autoFocus(autoFocusCallback);
    }

    @Override
    public void disableRecordingHint() {
        mCamera.getParameters().setRecordingHint(false);
    }

    @Override
    public void setRecordingHint() {
        mCamera.getParameters().setRecordingHint(true);
    }

    @Override
    public void setFocusMode(String focusMode) {
        parameters.setFocusMode(focusMode);
        mCamera.setParameters(parameters);
    }

    @Override
    public void setAutoFlash() {
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
    }

    @Override
    public boolean isFlashModeSupported(String flashMode) {
        List<String> flashModes = parameters.getSupportedFlashModes();
        if(flashModes != null && flashModes.size() > 0){
            Iterator<String> iterator = flashModes.iterator();
            while(iterator.hasNext()){
                String flash = iterator.next();
                if(flash.equalsIgnoreCase(flashMode)){
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    //Flash On for photo mode
    @Override
    public void setFlashOnOff(boolean flashOn) {
        if(!flashOn){
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }
        else{
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
        }
        mCamera.setParameters(parameters);
    }

    @Override
    public String getFlashMode() {
        return mCamera.getParameters().getFlashMode();
    }

    @Override
    public String getFocusMode() {
        return mCamera.getParameters().getFocusMode();
    }

    //For video mode
    @Override
    public void setTorchLight() {
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(parameters);
    }

    @Override
    public int getCameraId() {
        return cameraId;
    }

    @Override
    public boolean isCameraReady() {
        return (mCamera != null);
    }

    @Override
    public void onZoomChange(int zoomvalue, boolean stopped, Camera camera) {
        if(!stopped) {
            camera.getParameters().setZoom(zoomvalue);
        }
    }
    boolean capture=false;

    @Override
    public String getFocusModeAuto() {
        return Camera.Parameters.FOCUS_MODE_AUTO;
    }

    @Override
    public String getFlashModeOff() {
        return Camera.Parameters.FLASH_MODE_OFF;
    }

    @Override
    public String getFocusModeVideo() {
        return Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
    }

    @Override
    public String getFocusModePicture() {
        return Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
    }

    @Override
    public String getFlashModeTorch() {
        return Camera.Parameters.FLASH_MODE_TORCH;
    }
}