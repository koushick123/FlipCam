package com.flipcam.cameramanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.flipcam.VideoFragment;
import com.flipcam.camerainterface.CameraOperations;
import com.flipcam.constants.Constants;
import com.flipcam.model.Dimension;
import com.flipcam.view.CameraView;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by Koushick on 02-08-2017.
 */
/*
This class controls all Old Camera1 API operations for the camera. The CameraView only uses CameraOperations interface and this is the implementation for
Camera1.
 */
public class Camera1Manager implements CameraOperations, Camera.OnZoomChangeListener {

    private Camera mCamera = null;
    public final String TAG = "Camera1Manager";
    private static int VIDEO_WIDTH = 640;  // default dimensions.
    private static int VIDEO_HEIGHT = 480;
    private int DISPLAY_WIDTH;
    private int DISPLAY_HEIGHT;
    //Safe to assume every camera would support 15 fps.
    int MIN_FPS = 15;
    int MAX_FPS = 15;
    int cameraId = -1;
    Camera.Parameters parameters;
    Camera.CameraInfo info = new Camera.CameraInfo();
    private VideoFragment videoFrag;
    private static Camera1Manager camera1Manager;
    boolean VERBOSE = false;
    CameraView cameraView;
    Context appContext;
    WindowManager windowManager;
    Point screenSize = new Point();
    Display display;
    String targetWidth, targetHeight;
    List<Camera.Size> previewSizes;
    double targetVideoRatio;
    int screenWidth;
    boolean zoomChangeListener = false;

    public static Camera1Manager getInstance() {
        if (camera1Manager == null) {
            camera1Manager = new Camera1Manager();
        }
        return camera1Manager;
    }

    @Override
    public int[] getDisplaySizes() {
        int[] sizes = new int[2];
        sizes[0] = DISPLAY_WIDTH;
        sizes[1] = DISPLAY_HEIGHT;
        return sizes;
    }

    @Override
    public Camera.CameraInfo getCameraInfo() {
        return info;
    }

    private void setCameraInfo(Camera.CameraInfo cameraInfo){
        info = cameraInfo;
    }

    public void getSupportedVideoSizes(){
        Set<String> supportedVids=null;
        SharedPreferences sharedPreferences = obtainSettingsPrefs();
        //For camera get all supported video resolutions.
        supportedVids = sharedPreferences.getStringSet(Constants.SUPPORT_VIDEO_RESOLUTIONS, null);
        Log.d(TAG, "SupportedVids = " + supportedVids);
        fetchSupportedVideoSizesForCamera(supportedVids, sharedPreferences, mCamera, cameraId);
    }

    public List<String> getSupportedVideoResolutions() {
        List<String> resolutions = new java.util.ArrayList<>();
        if (mCamera == null) return resolutions;

        List<Camera.Size> videoSizes = mCamera.getParameters().getSupportedVideoSizes();
        if (videoSizes == null || videoSizes.isEmpty()) {
            videoSizes = mCamera.getParameters().getSupportedPreviewSizes();
        }
        if (videoSizes == null || videoSizes.isEmpty()) return resolutions;
        Set<String> resolutionsSupportedByBothCameras = obtainSettingsPrefs()
                .getStringSet(Constants.SUPPORT_VIDEO_RESOLUTIONS, null);
        TreeSet<Dimension> sortedSizes = new TreeSet<>();
        for (Camera.Size size : videoSizes) {
            String resolution = size.width + " X " + size.height;
            Integer quality = camcorderVideoRes.get(size.width + "x" + size.height);
            if (quality != null
                    && CamcorderProfile.hasProfile(cameraId, quality)
                    && (resolutionsSupportedByBothCameras == null
                    || resolutionsSupportedByBothCameras.contains(resolution))) {
                sortedSizes.add(new Dimension(size.width, size.height));
            }
        }
        for (Dimension size : sortedSizes) {
            resolutions.add(size.toString());
        }
        return resolutions;
    }

    private boolean isResolutionPresentInCamcorder(String camResolution){
        Set<String> resolKeys = camcorderVideoRes.keySet();
        return resolKeys.contains(camResolution);
    }

    public HashMap<String, Integer> camcorderVideoRes =  new HashMap<String, Integer>(){
        {
            put("1920x1080", CamcorderProfile.QUALITY_1080P);
            put("3840x2160", CamcorderProfile.QUALITY_2160P);
            put("720x480", CamcorderProfile.QUALITY_480P);
            put("1280x720", CamcorderProfile.QUALITY_720P);
            put("352x288", CamcorderProfile.QUALITY_CIF);
            put("176x144", CamcorderProfile.QUALITY_QCIF);
            put("320x240", CamcorderProfile.QUALITY_QVGA);
        }
    };

    private static Set<String> supportedVideoResolutions = new HashSet<>();
    private void fetchSupportedVideoSizesForCamera(Set<String> supportedVids, SharedPreferences sharedPreferences, Camera selectedCam, int camId){
        if (supportedVids == null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            List<Camera.Size> videoSizes = selectedCam.getParameters().getSupportedVideoSizes();
            StringBuilder resolSize = new StringBuilder();
            for (Camera.Size size : videoSizes) {
                resolSize.append(size.width);
                resolSize.append("x");
                resolSize.append(size.height);
                if(VERBOSE)Log.d(TAG, "resolSize ==== "+resolSize);
                if(!cameraView.isBackCamera()) {
                    if (isResolutionPresentInCamcorder(resolSize.toString()) && CamcorderProfile.hasProfile(camId, camcorderVideoRes.get(resolSize.toString()))) {
                        supportedVideoResolutions.add(size.width + " X " + size.height);
                    }
                }
                else{
                    Set<String> removeResolutions = new HashSet<>();
                    //Exclude resolutions not supported by front camera.
                    Iterator<String> iter = supportedVideoResolutions.iterator();
                    while (iter.hasNext()){
                        String resolution = iter.next();
                        String[] tempRes = resolution.split(" X ");
                        if(!CamcorderProfile.hasProfile(camId, camcorderVideoRes.get(tempRes[0]+"x"+tempRes[1]))){
                            if(VERBOSE)Log.d(TAG, "Removing "+tempRes[0]+" X "+tempRes[1]+" for rear camera");
                            removeResolutions.add(resolution);
                        }
                    }
                    //Remove those resolutions.
                    for(String temp : removeResolutions){
                        supportedVideoResolutions.remove(temp);
                    }
                }
                resolSize.delete(0, resolSize.length());
            }

            if(cameraView.isBackCamera()) {
                //Sort by descending order and take the largest value as default video resolution.
                TreeSet<Dimension> sortedVidSizes = new TreeSet<>();
                if(VERBOSE)Log.d(TAG, "videoRES SIZE = " + supportedVideoResolutions.size());
                int width = 0, height = 0;
                for (String resol : supportedVideoResolutions) {
                    width = Integer.parseInt(resol.split(" X ")[0]);
                    if(VERBOSE)Log.d(TAG, "WIDTH ==== " + width);
                    height = Integer.parseInt(resol.split(" X ")[1]);
                    if(VERBOSE)Log.d(TAG, "HEIGHT ==== " + height);
                    sortedVidSizes.add(new Dimension(width, height));
                }
                Dimension firstEle = sortedVidSizes.first();
                width = firstEle.getWidth();
                height = firstEle.getHeight();
                editor.putStringSet(Constants.SUPPORT_VIDEO_RESOLUTIONS, supportedVideoResolutions);
                editor.putString(Constants.SELECT_VIDEO_RESOLUTION, width + " X " + height);
                editor.commit();
            }
        }
    }

    @Override
    public void openCamera(boolean backCamera, Context context) {
        appContext = context;
        SharedPreferences sharedPreferences = obtainSettingsPrefs();
        Set<String> frontCamResolutions = sharedPreferences.getStringSet(Constants.SUPPORT_VIDEO_RESOLUTIONS, null);
        if(frontCamResolutions != null) {
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, info);
                if (backCamera) {
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        mCamera = Camera.open(i);
                        setCameraInfo(info);
                        if (VERBOSE) Log.d(TAG, "Open back facing camera");
                        cameraId = i;
                        break;
                    }
                } else {
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        mCamera = Camera.open(i);
                        setCameraInfo(info);
                        if (VERBOSE) Log.d(TAG, "Open front facing camera");
                        cameraId = i;
                        break;
                    }
                }
            }
        }
        else{
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    if (VERBOSE) Log.d(TAG, "Open back facing camera FIRST TIME");
                    setCameraInfo(info);
                    cameraId = i;
                }
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    if (VERBOSE) Log.d(TAG, "Open front facing camera FIRST TIME");
                    //This is for front camera, when the app is opening the camera for the very first time.
                    Camera frontCam = Camera.open(i);
                    //Fetch all video resolutions for front camera and check the rear camera resolutions against this list to ensure recordings for both cameras work.
                    cameraView.setBackCamera(false);
                    fetchSupportedVideoSizesForCamera(null, sharedPreferences,frontCam, i);
                    //Reset to rear camera
                    cameraView.setBackCamera(true);
                    frontCam.release();
                    frontCam = null;
                    //Now, open the back facing camera.
                    if(cameraId != -1) {
                        mCamera = Camera.open(cameraId);
                        break;
                    }
                }
            }
            if(mCamera == null){
                mCamera = Camera.open(cameraId);
            }
        }
        if (cameraId != -1) {
            parameters = mCamera.getParameters();
        } else {
            mCamera = null;
            return;
        }
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
        display.getSize(screenSize);
        screenWidth = screenSize.x;
    }

    @Override
    public void releaseCamera() {
        mCamera.release();
        mCamera = null;
        zoomChangeListener = false;
    }

    @Override
    public void setFPS() {
        List<int[]> fps = parameters.getSupportedPreviewFpsRange();
        Iterator<int[]> iter = fps.iterator();

        while (iter.hasNext()) {
            int[] frames = iter.next();
            if (!iter.hasNext()) {
                MIN_FPS = frames[0];
                MAX_FPS = frames[1];
                if (VERBOSE) Log.d(TAG, "Setting min and max Fps  == " + MIN_FPS + " , " + MAX_FPS);
                break;
            }
        }
        parameters.setPreviewFpsRange(MIN_FPS, MAX_FPS);
        mCamera.setParameters(parameters);
    }

    @Override
    public void setDisplayOrientation(int result) {
        mCamera.setDisplayOrientation(result);
    }

    @Override
    public void setResolution(int width, int height) {

    }

    @Override
    public void setAutoExposureAndLock() {
        if(parameters.isAutoExposureLockSupported()) {
            if(VERBOSE)Log.d(TAG, "setAutoExposureLock false");
            parameters.setAutoExposureLock(false);
            mCamera.setParameters(parameters);
        }
    }

    private void setPreviewSizeForTargetRatio() {
        // Match the camera preview to the selected video resolution.
        double targetRatio = targetVideoRatio;
        double arDiff = Double.MAX_VALUE;
        previewSizes = parameters.getSupportedPreviewSizes();
        Collections.sort(previewSizes, new CameraSizeComparator());
        for (Camera.Size previews : previewSizes) {
            double previewAR = (double) previews.width / (double) previews.height;
            if(VERBOSE)Log.d(TAG, "PREVIEW res = " + previews.width + " / " + previews.height);
            if(VERBOSE)Log.d(TAG, "PREVIEWAR = " + previewAR);
            if (Math.abs(previewAR - targetRatio) < arDiff) {
                arDiff = Math.abs(previewAR - targetRatio);
                if(VERBOSE)Log.d(TAG, "arDiff = " + arDiff);
                VIDEO_WIDTH = previews.width;
                VIDEO_HEIGHT = previews.height;
                if(VERBOSE)Log.d(TAG, "Video width = " + VIDEO_WIDTH + ", Video height = " + VIDEO_HEIGHT);
            }
        }
        parameters.setPreviewSize(VIDEO_WIDTH, VIDEO_HEIGHT);
        //Scale display preview to make the camera window look not too small.
        int scaleWidth = (int)(targetRatio * (double)screenWidth);
        DISPLAY_WIDTH = scaleWidth;
        DISPLAY_HEIGHT = screenWidth;
        if(VERBOSE)Log.d(TAG, "SCALED Video width = " + DISPLAY_WIDTH + ", Video height = " + DISPLAY_HEIGHT);
    }

    @Override
    public void setRotation(int rotation) {
        parameters.setRotation(rotation);
        mCamera.setParameters(parameters);
    }

    @Override
    public void setResolution() {
        setVideoSize();
        setPreviewSizeForTargetRatio();
        mCamera.setParameters(parameters);
    }

    @Override
    public boolean isZoomSupported() {
        return parameters.isZoomSupported();
    }

    @Override
    public boolean zoomInOrOut(int zoomInOrOut) {
        if (VERBOSE) Log.d(TAG, "Current zoom = " + zoomInOrOut);
        if (zoomInOrOut >= 0 && zoomInOrOut <= parameters.getMaxZoom()) {
            if (VERBOSE) Log.d(TAG, "Set Current zoom = " + zoomInOrOut);
            parameters.setZoom(zoomInOrOut);
            mCamera.setParameters(parameters);
            return true;
        }
        return false;
    }

    public boolean isSmoothZoomSupported() {
        if (VERBOSE) Log.d(TAG, "smooth zoom = " + parameters.isSmoothZoomSupported());
        //Add a zoomchangelistener flag so that it is not set every time this is called
        if (parameters.isSmoothZoomSupported() && !zoomChangeListener) {
            mCamera.setZoomChangeListener(this);
            zoomChangeListener = true;
        }
        return parameters.isSmoothZoomSupported();
    }

    public void smoothZoomInOrOut(int zoomInOrOut) {
        try {
            mCamera.startSmoothZoom(zoomInOrOut);
        }
        catch(RuntimeException runtime){
            //This catch block is necessary to prevent application crashes when user is trying to zoom in/out.
            if(VERBOSE)Log.d(TAG, "ZOOM EXCEPTION");
        }
    }

    @Override
    public int getMaxZoom() {
        return parameters.getMaxZoom();
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
    public void setVideoFragmentInstance(VideoFragment videoFragment) {
        this.videoFrag = videoFragment;
    }

    private SharedPreferences obtainSettingsPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    /*
    setVideoSize() sets the video resolution as per selection in Settings.
    When this is triggered for a camera switch, need to pick a video ratio that matches the first camera's video ratio.
    The preview will be set based on the same ratio. The preview size may or may not be the same depending on the camera's supported resolutions.
    If the preview size for the second camera is different from the first camera, we are still picking the best possible resolution for preview,
    so that the mediarecorder is still able to record with the same resolution without too much scaling.
    The recording dimensions will not be changed.*/

    @Override
    public void setVideoSize() {
        SharedPreferences sharedPreferences = obtainSettingsPrefs();
        if(VERBOSE)Log.d(TAG, "cameraView.isRecord() = "+cameraView.isRecord());
        String selectedRes;
        selectedRes = sharedPreferences.getString(Constants.SELECT_VIDEO_RESOLUTION, null);
        if(selectedRes!=null) {
            targetWidth = selectedRes.split(" X ")[0];
            targetHeight = selectedRes.split(" X ")[1];
            Log.d(TAG, "targetWidth === " + targetWidth);
            Log.d(TAG, "targetHeight === " + targetHeight);
        }
        targetVideoRatio = Double.parseDouble(targetWidth) / Double.parseDouble(targetHeight);
        if(!cameraView.isRecord()) {
            cameraView.setCamProfileForRecord(camcorderVideoRes.get(targetWidth+"x"+targetHeight));
            cameraView.setRecordVideoWidth(Integer.parseInt(targetWidth));
            cameraView.setRecordVideoHeight(Integer.parseInt(targetHeight));
            if(this.videoFrag != null){
                Log.d(TAG, "videoFrag ==== "+targetWidth+" X "+ targetHeight);
                this.videoFrag.setVideoResInfo(targetWidth, targetHeight);
            }
        }
    }

    public int getMAX_FPS() {
        return MAX_FPS;
    }

    class CameraSizeComparator implements Comparator<Camera.Size>{
        @Override
        public int compare(Camera.Size size1, Camera.Size size2) {
            if(size1.width < size2.width){
                return 1;
            }
            else if(size1.width > size2.width){
                return -1;
            }
            else{
                return 0;
            }
        }
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

    boolean focused=false;
    @Override
    public void cancelAutoFocus() {
        mCamera.cancelAutoFocus();
        focused = false;
    }

    @Override
    public void setAutoFocus(){
        mCamera.autoFocus(null);
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

    //Flash on for camera preview.
    @Override
    public void setFlashOnOff(boolean flashOn) {
        if(VERBOSE)Log.d(TAG, "SET FLASH TO "+flashOn);
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
        return mCamera == null ? null : mCamera.getParameters().getFlashMode();
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
    @Override
    public String getFocusModeAuto() {
        return Camera.Parameters.FOCUS_MODE_AUTO;
    }

    @Override
    public String getFlashModeOff() {
        return Camera.Parameters.FLASH_MODE_OFF;
    }

    @Override
    public String getFlashModeOn() {
        return Camera.Parameters.FLASH_MODE_ON;
    }

    @Override
    public String getFocusModeVideo() {
        return Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
    }

    @Override
    public String getFlashModeTorch() {
        return Camera.Parameters.FLASH_MODE_TORCH;
    }

    @Override
    public void setSurfaceView(CameraView surfaceView) {
        cameraView = surfaceView;
    }
}
