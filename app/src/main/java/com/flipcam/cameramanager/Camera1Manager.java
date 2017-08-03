package com.flipcam.cameramanager;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.flipcam.camerainterface.CameraOperations;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Koushick on 02-08-2017.
 */

public class Camera1Manager implements CameraOperations {

    private Camera mCamera;
    public final String TAG = "Camera1Manager";
    private static int VIDEO_WIDTH = 640;  // default dimensions.
    private static int VIDEO_HEIGHT = 480;
    //Safe to assume every camera would support 15 fps.
    int MIN_FPS = 15;
    int MAX_FPS = 15;
    int cameraId;
    Camera.Parameters parameters;
    private static Camera1Manager camera1Manager;
    public static Camera1Manager getInstance()
    {
        if(camera1Manager == null){
            camera1Manager = new Camera1Manager();
        }
        return camera1Manager;
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
        parameters = mCamera.getParameters();
        parameters.setFlashMode(null);
    }

    @Override
    public void releaseCamera() {
        mCamera.release();
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
    public void setResolution(WindowManager windowManager) {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        Log.d(TAG,"Width = "+metrics.widthPixels);
        Log.d(TAG,"Height = "+metrics.heightPixels);

        //Aspect ratio needs to be reversed, if orientation is portrait.
        double screenAspectRatio = 1.0f / ((double)metrics.widthPixels/(double)metrics.heightPixels);
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

    @Override
    public void setResolution() {
        //This can be used for Camera 2 API implementation if necessary.
    }

    @Override
    public boolean zoomInOrOut(boolean zoomInOrOut) {
        if(parameters.isZoomSupported())
        {
            int currentZoom = parameters.getZoom();
            int MAX_ZOOM = parameters.getMaxZoom();
            if(zoomInOrOut && (currentZoom < MAX_ZOOM && currentZoom >= 0)){ //Zoom in
                parameters.setZoom(++currentZoom);
                Log.d(TAG,"New zoom in set to ="+currentZoom);
            }
            else if(!zoomInOrOut && (currentZoom <= MAX_ZOOM && currentZoom > 0)){ //Zoom out
                parameters.setZoom(--currentZoom);
                Log.d(TAG,"New zoom out set to ="+currentZoom);
            }
            mCamera.setParameters(parameters);
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public void stopPreview() {
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

    @Override
    public boolean setAutoFocus(){

        List<String> focusModes = parameters.getSupportedFocusModes();
        if(focusModes != null && focusModes.size() > 0){
            Iterator<String> iterator = focusModes.iterator();
            while(iterator.hasNext()){
                String focus = iterator.next();
                if(focus.equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_AUTO)){
                    parameters.setFocusMode(focus);
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean b, Camera camera) {
                            Log.d(TAG,"auto focus set successfully");
                        }
                    });
                    return true;
                }
            }
            return false;
        }
        else{
            return false;
        }
    }

    @Override
    public boolean setAutoFlash() {

        List<String> flashModes = parameters.getSupportedFlashModes();
        if(flashModes != null && flashModes.size() > 0){
            Iterator<String> iterator = flashModes.iterator();
            while(iterator.hasNext()){
                String flash = iterator.next();
                if(flash.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_AUTO)){
                    parameters.setFlashMode(flash);
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean setFlashOnOff() {
        if(parameters.getFlashMode() != null && parameters.getFlashMode().equalsIgnoreCase(Camera.Parameters.FLASH_MODE_ON)){
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            return true;
        }
        else if(parameters.getFlashMode() != null && parameters.getFlashMode().equalsIgnoreCase(Camera.Parameters.FLASH_MODE_OFF)){
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            return true;
        }
        List<String> flashModes = parameters.getSupportedFlashModes();
        if(flashModes != null && flashModes.size() > 0){
            Iterator<String> iterator = flashModes.iterator();
            while(iterator.hasNext()){
                String flash = iterator.next();
                if(flash.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_ON)){
                    parameters.setFlashMode(flash);
                    return true;
                }
            }
            return false;
        }
        return false;
    }
}
