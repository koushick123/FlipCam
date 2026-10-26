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
import java.util.Iterator;
import java.util.List;
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
    private boolean previewStarted = false;
    private SurfaceTexture previewSurfaceTexture;

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
        SharedPreferences sharedPreferences = obtainSettingsPrefs();
        List<String> supportedResolutions = getSupportedVideoResolutions();
        String selectedResolution = sharedPreferences.getString(Constants.SELECT_VIDEO_RESOLUTION, null);
        if (!supportedResolutions.isEmpty() && !supportedResolutions.contains(selectedResolution)) {
            sharedPreferences.edit()
                    .putString(Constants.SELECT_VIDEO_RESOLUTION, supportedResolutions.get(0))
                    .apply();
        }
    }

    public List<String> getSupportedVideoResolutions() {
        List<String> resolutions = new java.util.ArrayList<>();
        if (mCamera == null) return resolutions;

        List<Camera.Size> videoSizes = mCamera.getParameters().getSupportedVideoSizes();
        if (videoSizes == null || videoSizes.isEmpty()) {
            videoSizes = mCamera.getParameters().getSupportedPreviewSizes();
        }
        if (videoSizes == null || videoSizes.isEmpty()) return resolutions;
        TreeSet<Dimension> sortedSizes = new TreeSet<>();
        for (Camera.Size size : videoSizes) {
            if (getCamcorderProfileForResolution(size.width, size.height) != null) {
                sortedSizes.add(new Dimension(size.width, size.height));
            }
        }
        for (Dimension size : sortedSizes) {
            resolutions.add(size.toString());
        }
        return resolutions;
    }

    public HashMap<String, Integer> camcorderVideoRes =  new HashMap<String, Integer>(){
        {
            put("1920x1080", CamcorderProfile.QUALITY_1080P);
            put("3840x2160", CamcorderProfile.QUALITY_2160P);
            put("720x480", CamcorderProfile.QUALITY_480P);
            put("1280x720", CamcorderProfile.QUALITY_720P);
            put("640x480", CamcorderProfile.QUALITY_VGA);
            put("352x288", CamcorderProfile.QUALITY_CIF);
            put("176x144", CamcorderProfile.QUALITY_QCIF);
            put("320x240", CamcorderProfile.QUALITY_QVGA);
        }
    };

    private Integer getCamcorderProfileForResolution(int width, int height) {
        Integer mappedQuality = camcorderVideoRes.get(width + "x" + height);
        if (profileMatchesResolution(mappedQuality, width, height)) return mappedQuality;

        int[] fallbackQualities = {CamcorderProfile.QUALITY_HIGH, CamcorderProfile.QUALITY_LOW};
        for (int quality : fallbackQualities) {
            if (profileMatchesResolution(quality, width, height)) return quality;
        }
        return null;
    }

    private boolean profileMatchesResolution(Integer quality, int width, int height) {
        if (quality == null || !CamcorderProfile.hasProfile(cameraId, quality)) return false;
        CamcorderProfile profile = CamcorderProfile.get(cameraId, quality);
        return profile.videoFrameWidth == width && profile.videoFrameHeight == height;
    }

    @Override
    public void openCamera(boolean backCamera, Context context) {
        appContext = context;
        cameraId = -1;
        int requestedFacing = backCamera
                ? Camera.CameraInfo.CAMERA_FACING_BACK
                : Camera.CameraInfo.CAMERA_FACING_FRONT;
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == requestedFacing) {
                cameraId = i;
                setCameraInfo(cameraInfo);
                mCamera = Camera.open(i);
                if (VERBOSE) Log.d(TAG, backCamera ? "Open back facing camera" : "Open front facing camera");
                break;
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
        previewStarted = false;
        previewSurfaceTexture = null;
        Camera releasing = mCamera;
        mCamera = null;
        parameters = null;
        zoomChangeListener = false;
        if (releasing != null) releasing.release();
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
        if (mCamera == null) return;

        boolean restartPreview = previewStarted && previewSurfaceTexture != null;
        if (restartPreview) {
            mCamera.stopPreview();
            previewStarted = false;
        }
        try {
            // Always base changes on the camera's current parameters; the cached object
            // can be stale after other camera controls have called setParameters().
            parameters = mCamera.getParameters();
            setVideoSize();
            setPreviewSizeForTargetRatio();
            mCamera.setParameters(parameters);
            parameters = mCamera.getParameters();
        } finally {
            if (restartPreview) startPreview(previewSurfaceTexture);
        }
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
        if (mCamera != null && previewStarted) {
            mCamera.stopPreview();
            previewStarted = false;
        }
    }

    @Override
    public void startPreview(SurfaceTexture surfaceTexture) {
        if (mCamera == null) return;
        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mCamera.startPreview();
        previewSurfaceTexture = surfaceTexture;
        previewStarted = true;
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
            Integer profile = getCamcorderProfileForResolution(
                    Integer.parseInt(targetWidth), Integer.parseInt(targetHeight));
            if (profile != null) cameraView.setCamProfileForRecord(profile);
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
