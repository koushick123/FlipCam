package com.flipcam.cameramanager;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.flipcam.PermissionActivity;
import com.flipcam.PhotoFragment;
import com.flipcam.VideoFragment;
import com.flipcam.camerainterface.CameraOperations;
import com.flipcam.constants.Constants;
import com.flipcam.view.CameraView;

import java.util.Arrays;
import java.util.StringTokenizer;
/*
This class controls all new Camera2 API operations for the camera. The CameraView only uses CameraOperations interface and this is the implementation for
Camera2.
 */
public class Camera2Manager implements CameraOperations {

    private CameraDevice cameraDevice;
    private int cameraOrientation;
    private int cameraId;
    private CameraCharacteristics cameraCharacteristics;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private Point previewSize;
    private Size videoSize;
    StreamConfigurationMap configs;
    public final String TAG = "Camera2Manager";
    private static int VIDEO_WIDTH = 640;  // default dimensions.
    private static int VIDEO_HEIGHT = 480;
    private static Camera2Manager camera2Manager;
    private SurfaceTexture surfaceTexture;
    private PhotoFragment photoFrag;
    private VideoFragment videoFrag;
    boolean VERBOSE = true;
    float[] focalLengths;

    public static Camera2Manager getInstance()
    {
        if(camera2Manager == null){
            camera2Manager = new Camera2Manager();
        }
        return camera2Manager;
    }

    @Override
    public int[] getDisplaySizes() {
        return new int[0];
    }

    @Override
    public Camera.CameraInfo getCameraInfo()
    {
        return null;
    }

    @Override
    public void getSupportedPictureSizes() {

    }

    @Override
    public void openCamera(boolean backCamera, Context context) {
        CameraManager cameraManager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
        try {
             for (String camId : cameraManager.getCameraIdList()) {
                cameraCharacteristics = cameraManager.getCameraCharacteristics(camId);
                cameraOrientation = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                 if (backCamera) {
                     if (cameraOrientation == CameraCharacteristics.LENS_FACING_BACK) {
                         cameraId = Integer.parseInt(camId);
                         break;
                    }
                }
                else{
                     if (cameraOrientation == CameraCharacteristics.LENS_FACING_FRONT) {
                         cameraId = Integer.parseInt(camId);
                         break;
                    }
                }
            }
            configs = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//            previewSize = configs.getOutputSizes(SurfaceTexture.class)[0];
            if(VERBOSE)Log.d(TAG,"callback = "+stateCallback+", cam id = "+cameraId);
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            surfaceTexture = camView.getSurfaceTexture();
            previewSize = new Point();
            WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getRealSize(previewSize);
            cameraManager.openCamera(cameraId + "", stateCallback, null);
        }
        catch (CameraAccessException ac){
            ac.printStackTrace();
        }
    }

    @Override
    public void setAutoExposureAndLock() {

    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            if(VERBOSE)Log.d(TAG, "onOpened == " + cameraOrientation + ", " + surfaceTexture);
            cameraDevice = camera;
            startPreview(surfaceTexture);
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }
    };

    @Override
    public boolean isCameraReady() {
        return (cameraDevice != null);
    }

    @Override
    public void startPreview(SurfaceTexture surfaceTex) {
        surfaceTexture = surfaceTex;
        createCameraPreview();
    }

    private void createCameraPreview() {
        try {
            if(VERBOSE)Log.d(TAG, "previewSize.x = "+previewSize.x+", previewSize.y = "+previewSize.y);
            setResolution(previewSize.x,previewSize.y);

            // We set up a CaptureRequest.Builder with the output Surface.
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            setFPS();

            //Set the focus mode to continuous focus
            if (this.videoFrag != null && isFocusModeSupported(getFocusModeVideo())) {
                if (VERBOSE) Log.d(TAG, "Continuous AF Video");
                setFocusMode(getFocusModeVideo());
            }
            else if (this.photoFrag != null) {
                if(isFocusModeSupported(getFocusModePicture())) {
                    if (VERBOSE) Log.d(TAG, "Continuous AF Picture");
                    setFocusMode(getFocusModePicture());
                    this.photoFrag.setContinuousAF(true);
                }
                else{
                    if (VERBOSE) Log.d(TAG, "Use Auto AF instead");
                    this.photoFrag.setContinuousAF(false);
                }
            }
            // This is the output Surface we need to start preview
            Surface videoSurface = new Surface(surfaceTexture);
            captureRequestBuilder.addTarget(videoSurface);
            if(!camView.switchFlashOnOff()) {
                if (VERBOSE) Log.d(TAG, "beginning capture session");

                // Here, we create a CameraCaptureSession for camera preview.
                cameraDevice.createCaptureSession(Arrays.asList(videoSurface), captureSessionStateCallback, null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void reCreateCaptureSession(){
        Surface videoSurface = new Surface(surfaceTexture);
        try {
            cameraDevice.createCaptureSession(Arrays.asList(videoSurface), captureSessionStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    CameraCaptureSession.StateCallback captureSessionStateCallback = new CameraCaptureSession.StateCallback(){
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSes) {
            //The camera is already closed
            if (null == cameraDevice) {
                return;
            }
            // When the session is ready, we start displaying the preview.
            cameraCaptureSession = cameraCaptureSes;
            if(VERBOSE)Log.d(TAG,"Camera capture session == "+cameraCaptureSession);
            try {
                // Finally, we start displaying the camera preview.
                if(videoFrag != null) {
                    cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                }
                else{
                    cameraCaptureSession.capture(captureRequestBuilder.build(), null, null);
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            if(VERBOSE)Log.d(TAG, "isStopCamera = "+camView.isStopCamera());
            if(camView.isStopCamera()) {
                stopPreview();
                releaseCamera();
            }
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            super.onClosed(session);
            if(VERBOSE)Log.d(TAG, "session CLOSED");
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
        }
    };

    @Override
    public void releaseCamera() {
        cameraCaptureSession.close();
        cameraCaptureSession = null;
    }

    @Override
    public void setFPS() {
        /*captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
        if(configs.isOutputSupportedFor(SurfaceTexture.class)){
            long fps = configs.getOutputMinFrameDuration(SurfaceTexture.class, videoSize);
            Log.d(TAG, "Setting FPS = "+fps);
            captureRequestBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, fps);
        }
        //Set Exposure Compensation
        Range<Integer> compensations = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        Rational aeStep = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
        if(compensations.getLower() != 0 && compensations.getUpper() != 0){
            int maxExp = compensations.getUpper() * aeStep.intValue();
            Log.d(TAG, "max exp = "+maxExp);
            captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long)maxExp / 2);
        }*/
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    @Override
    public void setResolution() {

    }

    CameraView camView;
    public void setSurfaceView(CameraView surfaceView){
        camView = surfaceView;
    }

    @Override
    public void setResolution(int width, int height) {
        if(VERBOSE)Log.d(TAG,"Set Width = "+width);
        if(VERBOSE)Log.d(TAG,"Set Height = "+height);

        SharedPreferences sharedPreferences;
        SharedPreferences.Editor editor;
        if(photoFrag != null) {
            sharedPreferences = photoFrag.getActivity().getSharedPreferences(PermissionActivity.FC_SHARED_PREFERENCE, Context.MODE_PRIVATE);
        }
        else{
            sharedPreferences = videoFrag.getActivity().getSharedPreferences(PermissionActivity.FC_SHARED_PREFERENCE, Context.MODE_PRIVATE);
        }
        //Obtain preview resolution from Preferences if already set, else, measure as below.
        String savedPreview = sharedPreferences.getString(Constants.PREVIEW_RESOLUTION, null);
        if(savedPreview != null && !savedPreview.equals("")){
            StringTokenizer tokenizer = new StringTokenizer(savedPreview,",");
            VIDEO_WIDTH = Integer.parseInt(tokenizer.nextToken());
            if(VERBOSE)Log.d(TAG, "Saved VIDEO_WIDTH = "+VIDEO_WIDTH );
            VIDEO_HEIGHT = Integer.parseInt(tokenizer.nextToken());
            if(VERBOSE)Log.d(TAG, "Saved VIDEO_HEIGHT = "+VIDEO_HEIGHT );
        }
        else{
            //Aspect ratio needs to be reversed, if orientation is portrait.
            double screenAspectRatio = 1.0f / ((double) width / (double) height);
            if (VERBOSE) Log.d(TAG, "SCREEN Aspect Ratio = " + screenAspectRatio);
            Size[] previewSizes = configs.getOutputSizes(SurfaceTexture.class);
            //If none of the camera preview size will (closely) match with screen resolution, default it to take the first preview size value.
            VIDEO_HEIGHT = previewSizes[0].getHeight();
            VIDEO_WIDTH = previewSizes[0].getWidth();
            for(Size vidSize : previewSizes){
                double ar = (double) vidSize.getWidth() / (double) vidSize.getHeight();
                if (VERBOSE) Log.d(TAG, "Aspect ratio for " + vidSize.getWidth() + " / " + vidSize.getHeight() + " is = " + ar);
                int widthDiff = Math.abs(width - vidSize.getHeight());
                int heightDiff = Math.abs(height - vidSize.getWidth());
                if (VERBOSE) Log.d(TAG, "Width diff = " + widthDiff + ", Height diff = " + heightDiff);
                if (Math.abs(screenAspectRatio - ar) <= 0.2 && (widthDiff <= 150 && heightDiff <= 150)) {
                    //Best match for camera preview!!
                    VIDEO_HEIGHT = vidSize.getHeight();
                    VIDEO_WIDTH = vidSize.getWidth();
                    break;
                }
            }
            editor = sharedPreferences.edit();
            StringBuffer resolutions = new StringBuffer();
            editor.putString(Constants.PREVIEW_RESOLUTION,
                    resolutions.append(String.valueOf(VIDEO_WIDTH)).append(",").append(String.valueOf(VIDEO_HEIGHT)).toString());
            editor.commit();
        }
        if(VERBOSE)Log.d(TAG,"HEIGHT == "+VIDEO_HEIGHT+", WIDTH == "+VIDEO_WIDTH);
        videoSize = new Size(VIDEO_WIDTH, VIDEO_HEIGHT);
        surfaceTexture.setDefaultBufferSize(VIDEO_WIDTH, VIDEO_HEIGHT);
    }

    @Override
    public boolean zoomInOrOut(int zoomInOrOut) {
        if(isZoomSupported() && zoomInOrOut >= 0 && zoomInOrOut <= getMaxZoom())
        {
            if(VERBOSE)Log.d(TAG,"Set Current zoom = "+zoomInOrOut);
            captureRequestBuilder.set(CaptureRequest.LENS_FOCAL_LENGTH, (float) zoomInOrOut);
            reCreateCaptureSession();

            return true;
        }
        return false;
    }

    @Override
    public boolean isSmoothZoomSupported() {
        return false;
    }

    @Override
    public void smoothZoomInOrOut(int zoomInOrOut) {

    }

    @Override
    public boolean isZoomSupported() {
//        float[] focalLengths = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        if(focalLengths.length == 1) {
            return false;
        }
        else{
            return true;
        }
    }

    @Override
    public int getMaxZoom() {
        focalLengths = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        Log.d(TAG, "focalLengths = "+focalLengths.length);
        for(float focalLen : focalLengths){
            Log.d(TAG, "Focal length = "+focalLen);
        }
        return (int)focalLengths[focalLengths.length - 1];
    }

    @Override
    public void stopPreview() {
        cameraDevice.close();
        cameraDevice = null;
    }

    @Override
    public void setAutoFocus(boolean noPic) {
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        reCreateCaptureSession();
    }

    @Override
    public void setRecordingHint() {

    }

    @Override
    public void disableRecordingHint() {

    }

    @Override
    public void cancelAutoFocus() {
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
        reCreateCaptureSession();
    }

    @Override
    public void setFocusMode(String focusMode) {
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, Integer.parseInt(focusMode));
    }

    @Override
    public void setAutoFlash() {
        captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
        reCreateCaptureSession();
    }

    @Override
    public void setFlashOnOff(boolean flashOn) {
        if(!flashOn){
            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
        }
        else{
            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
        }
        reCreateCaptureSession();
    }

    @Override
    public String getFlashMode() {
        return captureRequestBuilder.get(CaptureRequest.FLASH_MODE)+"";
    }

    @Override
    public String getFocusMode() {
        return captureRequestBuilder.get(CaptureRequest.CONTROL_AF_MODE)+"";
    }

    @Override
    public boolean isFlashModeSupported(String flashMode) {
        return (cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE));
    }

    @Override
    public boolean isFocusModeSupported(String focusMode) {
        int[] afModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        for(int afMode : afModes){
            if(afMode == Integer.parseInt(focusMode)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void setTorchLight() {
        captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
        reCreateCaptureSession();
    }

    @Override
    public int getCameraId() {
        return cameraId;
    }

    @Override
    public void setDisplayOrientation(int result) {

    }

    @Override
    public void setVideoSize() {

    }

    @Override
    public void setPictureSize() {

    }

    @Override
    public void capturePicture() {

    }

    @Override
    public String getFocusModeAuto() {
        return CameraMetadata.CONTROL_AF_MODE_AUTO+"";
    }

    @Override
    public String getFlashModeOn() {
        return null;
    }

    @Override
    public String getFlashModeOff() {
        return CameraMetadata.FLASH_MODE_OFF+"";
    }

    @Override
    public String getFocusModeVideo() {
        return CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO+"";
    }

    @Override
    public String getFocusModePicture() {
        return CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE+"";
    }

    @Override
    public String getFlashModeTorch() {
        return CameraMetadata.FLASH_MODE_TORCH+"";
    }

    @Override
    public void setRotation(int rotation) {

    }

    @Override
    public void setPhotoFragmentInstance(PhotoFragment photoFragment) {
        photoFrag = photoFragment;
    }

    @Override
    public void setPhotoPath(String mediaPath) {

    }

    @Override
    public void enableShutterSound(boolean enable) {

    }

    @Override
    public void removePreviewCallback() {

    }

    @Override
    public void setVideoFragmentInstance(VideoFragment videoFragment) {
        videoFrag = videoFragment;
    }
}
