package com.flipcam.cameramanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.flipcam.PermissionActivity;
import com.flipcam.PhotoFragment;
import com.flipcam.R;
import com.flipcam.VideoFragment;
import com.flipcam.camerainterface.CameraOperations;
import com.flipcam.constants.Constants;
import com.flipcam.view.CameraView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * Created by Koushick on 02-08-2017.
 */

public class Camera1Manager implements CameraOperations, Camera.OnZoomChangeListener, Camera.ShutterCallback,Camera.PictureCallback, Camera.PreviewCallback {

    private Camera mCamera;
    public final String TAG = "Camera1Manager";
    private static int VIDEO_WIDTH = 640;  // default dimensions.
    private static int VIDEO_HEIGHT = 480;
    //Safe to assume every camera would support 15 fps.
    int MIN_FPS = 15;
    int MAX_FPS = 15;
    int cameraId = -1;
    Camera.Parameters parameters;
    Camera.CameraInfo info = new Camera.CameraInfo();
    private PhotoFragment photoFrag;
    private VideoFragment videoFrag;
    String photoPath;
    float rotation;
    private static Camera1Manager camera1Manager;
    Bitmap photo;
    boolean VERBOSE = true;
    CameraView cameraView;
    Context appContext;
    Resources resources;
    WindowManager windowManager;
    Point screenSize = new Point();
    Display display;
    public static Camera1Manager getInstance()
    {
        if(camera1Manager == null){
            camera1Manager = new Camera1Manager();
        }
        return camera1Manager;
    }

    @Override
    public int[] getPreviewSizes()
    {
        int[] sizes = new int[2];
        sizes[0] = VIDEO_WIDTH;
        sizes[1] = VIDEO_HEIGHT;
        return sizes;
    }

    @Override
    public Camera.CameraInfo getCameraInfo()
    {
        return info;
    }

    @Override
    public List<Camera.Size> getSupportedPictureSizes() {
        return mCamera.getParameters().getSupportedPictureSizes();
    }

    @Override
    public void openCamera(boolean backCamera, Context context) {
        for(int i=0;i<Camera.getNumberOfCameras();i++)
        {
            Camera.getCameraInfo(i, info);
            if(backCamera) {
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    mCamera = Camera.open(i);
                    if(VERBOSE)Log.d(TAG,"Open back facing camera");
                    cameraId = i;
                    break;
                }
            }
            else{
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    mCamera = Camera.open(i);
                    if(VERBOSE)Log.d(TAG,"Open front facing camera");
                    cameraId = i;
                    break;
                }
            }
        }
        if(cameraId != -1) {
            parameters = mCamera.getParameters();
            parameters.setExposureCompensation((parameters.getMaxExposureCompensation()/2) > 0 ? parameters.getMaxExposureCompensation()/2 : 0);
            mCamera.setParameters(parameters);
            if(VERBOSE)Log.d(TAG,"exp comp set = "+parameters.getExposureCompensation());
            mCamera.setPreviewCallback(this);
        }
        else{
            mCamera=null;
        }
        windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
        display.getSize(screenSize);
        appContext = context;
    }

    @Override
    public void releaseCamera() {
        mCamera.release();
        mCamera = null;
        zoomChangeListener = false;
    }

    @Override
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
        if(VERBOSE)Log.d(TAG,"Setting min and max Fps  == "+MIN_FPS+" , "+MAX_FPS);
        parameters.setPreviewFpsRange(MIN_FPS,MAX_FPS);
        mCamera.setParameters(parameters);
    }

    @Override
    public void setDisplayOrientation(int result) {
        mCamera.setDisplayOrientation(result);
    }

    @Override
    public void setResolution(int width, int height) {
        if(VERBOSE)Log.d(TAG,"Screen Width = "+width);
        if(VERBOSE)Log.d(TAG,"Screen Height = "+height);

        /*SharedPreferences sharedPreferences;
        if(photoFrag != null) {
            sharedPreferences = photoFrag.getActivity().getSharedPreferences(PermissionActivity.FC_SHARED_PREFERENCE, Context.MODE_PRIVATE);
            resources = photoFrag.getActivity().getResources();
        }
        else{
            sharedPreferences = videoFrag.getActivity().getSharedPreferences(PermissionActivity.FC_SHARED_PREFERENCE, Context.MODE_PRIVATE);
            resources = videoFrag.getActivity().getResources();
        }
        //Obtain preview resolution from Preferences if already set, else measure as below.
        String savedPreview = sharedPreferences.getString(Constants.PREVIEW_RESOLUTION, null);
        if(savedPreview != null && !savedPreview.equals("")){
            StringTokenizer tokenizer = new StringTokenizer(savedPreview,",");
            VIDEO_WIDTH = Integer.parseInt(tokenizer.nextToken());
            if(VERBOSE)Log.d(TAG, "Saved VIDEO_WIDTH = "+VIDEO_WIDTH );
            VIDEO_HEIGHT = Integer.parseInt(tokenizer.nextToken());
            if(VERBOSE)Log.d(TAG, "Saved VIDEO_HEIGHT = "+VIDEO_HEIGHT );
        }*/
        //Aspect ratio needs to be reversed, if orientation is portrait.
        screenAspectRatio = 1.0f / ((double) width / (double) height);
        if (VERBOSE) Log.d(TAG, "SCREEN Aspect Ratio = " + screenAspectRatio);
        setVideoSize();
        choosePreviewSizeForVideo();
        /*editor = sharedPreferences.edit();
        StringBuffer resolutions = new StringBuffer();
        editor.putString(Constants.PREVIEW_RESOLUTION, resolutions.append(String.valueOf(VIDEO_WIDTH)).append(",").append(String.valueOf(VIDEO_HEIGHT)).toString());
        editor.commit();*/
        if(VERBOSE)Log.d(TAG,"HEIGHT == "+VIDEO_HEIGHT+", WIDTH == "+VIDEO_WIDTH);
        parameters.setPreviewSize(VIDEO_WIDTH, VIDEO_HEIGHT);
        if(!cameraView.isRecord()) {
            if(videoFrag!=null) {
                SharedPreferences settings = videoFrag.getActivity().getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor1 = settings.edit();
//                editor1.remove(Constants.VIDEO_DIMENSION_HIGH);
//                editor1.remove(Constants.VIDEO_DIMENSION_MEDIUM);
//                editor1.remove(Constants.VIDEO_DIMENSION_LOW);
                editor1.commit();
            }
        }
        mCamera.setParameters(parameters);
    }
    List<Camera.Size> previewSizes;
    double targetVideoRatio;
    double screenAspectRatio;

    private void choosePreviewSizeForVideo(){

        //For chosen video resolution, we need to display a preview that is a closest match to targetVideoRatio.
        double arDiff = Double.MAX_VALUE;
        Collections.sort(previewSizes, new CameraSizeComparator());
        for(Camera.Size previews : previewSizes){
            double previewAR = (double)previews.width / (double)previews.height;
            if(Math.abs(previewAR - targetVideoRatio) < arDiff){
                arDiff = Math.abs(previewAR - targetVideoRatio);
                Log.d(TAG, "arDiff = "+arDiff);
                VIDEO_WIDTH = previews.width;
                VIDEO_HEIGHT = previews.height;
                Log.d(TAG, "Video width = "+VIDEO_WIDTH+", Video height = "+VIDEO_HEIGHT);
            }
        }
    }

    @Override
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
        if(VERBOSE)Log.d(TAG,"Current zoom = "+zoomInOrOut);
        if(isZoomSupported() && zoomInOrOut >= 0 && zoomInOrOut <= parameters.getMaxZoom())
        {
            if(VERBOSE)Log.d(TAG,"Set Current zoom = "+zoomInOrOut);
            parameters.setZoom(zoomInOrOut);
            mCamera.setParameters(parameters);
            return true;
        }
        return false;
    }

    boolean zoomChangeListener = false;
    public boolean isSmoothZoomSupported()
    {
        if(VERBOSE)Log.d(TAG,"smooth zoom = "+parameters.isSmoothZoomSupported());
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
        if(VERBOSE)Log.d(TAG,"take pic");
        capture=true;
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setZoom(zoomedVal);
        mCamera.takePicture(this, null, null, this);
        mCamera.setParameters(parameters);
    }

    @Override
    public void setPhotoFragmentInstance(PhotoFragment photoFragment){
        photoFrag = photoFragment;
    }

    @Override
    public void setVideoFragmentInstance(VideoFragment videoFragment) {
        videoFrag = videoFragment;
    }

    @Override
    public void setPhotoPath(String mediaPath)
    {
        photoPath = mediaPath;
    }

    @Override
    public void setRotation(float rot){
        rotation = rot;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        if(VERBOSE)Log.d(TAG, "Picture wil be saved at loc = " + photoPath);
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
            if(VERBOSE)Log.d(TAG, "photo is ready");
            camera.startPreview();
            photoFrag.getCapturePic().setClickable(true);
            photoFrag.hideImagePreview();
            photoFrag.showImageSaved();
        }
    }

    @Override
    public void onShutter() {
        if(VERBOSE)Log.d(TAG,"Photo captured");
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

    private SharedPreferences obtainSettingsPrefs(){
        SharedPreferences sharedPreferences;
        if(videoFrag != null) {
            sharedPreferences = videoFrag.getActivity().getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        }
        else{
            sharedPreferences = photoFrag.getActivity().getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        }
        return sharedPreferences;
    }

    @Override
    public void setVideoSize() {
        SharedPreferences sharedPreferences;
        previewSizes = parameters.getSupportedPreviewSizes();

        //Need to choose preview size whose aspect ratio closely matches that of the chosen video resolution.
        sharedPreferences = obtainSettingsPrefs();
        String selectedRes;
        if((selectedRes = sharedPreferences.getString(Constants.SELECT_VIDEO_RESOLUTION, null)) != null) {

            //Choose video dimension based on selected resolution.
            if (selectedRes.equalsIgnoreCase(appContext.getResources().getString(R.string.videoResHigh))) {

                if (sharedPreferences.getString(Constants.VIDEO_DIMENSION_HIGH, null) != null) {
                    String dimension = sharedPreferences.getString(Constants.VIDEO_DIMENSION_HIGH, null);
                    StringTokenizer tokenizer = new StringTokenizer(dimension, ":");
                    String targetWidth = tokenizer.nextToken();
                    String targetHeight = tokenizer.nextToken();
                    targetVideoRatio = Double.parseDouble(targetWidth) / Double.parseDouble(targetHeight);
                }
                else{
                    //As of this release, the highest video resolution will not exceed the resolution of the screen.
                    targetVideoRatio = screenAspectRatio;
                }
                Log.d(TAG, "targetVideoRatio for HIGH = "+targetVideoRatio);
            }
            else if(selectedRes.equalsIgnoreCase(appContext.getResources().getString(R.string.videoResMedium))){
                if (sharedPreferences.getString(Constants.VIDEO_DIMENSION_MEDIUM, null) != null) {
                    String dimension = sharedPreferences.getString(Constants.VIDEO_DIMENSION_MEDIUM, null);
                    StringTokenizer tokenizer = new StringTokenizer(dimension, ":");
                    String targetWidth = tokenizer.nextToken();
                    String targetHeight = tokenizer.nextToken();
                    targetVideoRatio = Double.parseDouble(targetWidth) / Double.parseDouble(targetHeight);
                }
                else{
                    targetVideoRatio = chooseMediumResolution();
                }
                Log.d(TAG, "targetVideoRatio for MEDIUM = "+targetVideoRatio);
            }
            else{
                if (sharedPreferences.getString(Constants.VIDEO_DIMENSION_LOW, null) != null) {
                    String dimension = sharedPreferences.getString(Constants.VIDEO_DIMENSION_LOW, null);
                    StringTokenizer tokenizer = new StringTokenizer(dimension, ":");
                    String targetWidth = tokenizer.nextToken();
                    String targetHeight = tokenizer.nextToken();
                    targetVideoRatio = Double.parseDouble(targetWidth) / Double.parseDouble(targetHeight);
                }
                else{
                    if(VERBOSE)Log.d(TAG, "SET QUALITY_LOW as low");
                    CamcorderProfile lowProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
                    cameraView.setCamProfileForRecord(CamcorderProfile.QUALITY_LOW);
                    cameraView.setRecordVideoWidth(lowProfile.videoFrameWidth);
                    cameraView.setRecordVideoHeight(lowProfile.videoFrameHeight);
                    targetVideoRatio = ((double)(lowProfile.videoFrameWidth) / (double)(lowProfile.videoFrameHeight));
                }
                Log.d(TAG, "targetVideoRatio for LOW = "+targetVideoRatio);
            }
        }
        else{
            targetVideoRatio = screenAspectRatio;
        }
    }

    int screenWidth;
    private double chooseMediumResolution(){
        SharedPreferences sharedPreferences = obtainSettingsPrefs();
        StringTokenizer videoDimen;
        previewSizes = parameters.getSupportedPreviewSizes();
        if(display.getRotation() == Surface.ROTATION_0 || display.getRotation() == Surface.ROTATION_180){
            screenWidth = screenSize.x;
        }
        else{
            //In Landscape mode, get screensize y.
            screenWidth = screenSize.y;
        }
        if(screenWidth > resources.getInteger(R.integer.fullHDWidth)){

            //Choose a resolution that matches closest to 1080P, as medium resolution.
            if(CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P)) {
                if (VERBOSE) Log.d(TAG, "SET 1080P as medium");
                videoDimen = new StringTokenizer(findClosestResolutionInCamera(CamcorderProfile.QUALITY_1080P), ":");
                cameraView.setCamProfileForRecord(CamcorderProfile.QUALITY_1080P);
            }
            else{
                videoDimen = checkForMediumResolutions();
            }
        }
        else if(VIDEO_WIDTH == resources.getInteger(R.integer.fullHDWidth)){

            videoDimen = checkForMediumResolutions();
        }
        else{
            //Choose a resolution that matches closest to 480P as medium resolution.
            if(CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
                if (VERBOSE) Log.d(TAG, "SET 480P as medium");
                videoDimen = new StringTokenizer(findClosestResolutionInCamera(CamcorderProfile.QUALITY_480P), ":");
                cameraView.setCamProfileForRecord(CamcorderProfile.QUALITY_480P);
            }
            else if(CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QVGA)){
                if(VERBOSE)Log.d(TAG, "SET QVGA as medium");
                videoDimen = new StringTokenizer(findClosestResolutionInCamera(CamcorderProfile.QUALITY_QVGA), ":");
                cameraView.setCamProfileForRecord(CamcorderProfile.QUALITY_QVGA);
            }
            else if(CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QCIF)){
                if(VERBOSE)Log.d(TAG, "SET QCIF as medium");
                videoDimen = new StringTokenizer(findClosestResolutionInCamera(CamcorderProfile.QUALITY_QCIF), ":");
                cameraView.setCamProfileForRecord(CamcorderProfile.QUALITY_QCIF);
            }
            else{
                if(VERBOSE)Log.d(TAG, "SET HIGH since other medium resolutions are not supported");
                videoDimen = new StringTokenizer(findClosestResolutionInCamera(CamcorderProfile.QUALITY_HIGH), ":");
                cameraView.setCamProfileForRecord(CamcorderProfile.QUALITY_HIGH);
            }
        }
        cameraView.setRecordVideoWidth(Integer.parseInt(videoDimen.nextToken()));
        cameraView.setRecordVideoHeight(Integer.parseInt(videoDimen.nextToken()));
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if(VERBOSE)Log.d(TAG, "Selected RESOLUTION video width = "+cameraView.getRecordVideoWidth()+" X "+cameraView.getRecordVideoHeight());
        editor.putString(Constants.VIDEO_DIMENSION_MEDIUM, cameraView.getRecordVideoWidth()+":"+cameraView.getRecordVideoHeight());
        editor.putInt(Constants.CAMPROFILE_FOR_RECORD_MEDIUM, cameraView.getCamProfileForRecord());
        editor.commit();
        return (double)cameraView.getRecordVideoWidth() / (double)cameraView.getRecordVideoHeight();
    }

    private StringTokenizer checkForMediumResolutions(){
        StringTokenizer videoDimen;
        if(CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
            if(VERBOSE)Log.d(TAG, "SET 720P as medium");
            videoDimen = new StringTokenizer(findClosestResolutionInCamera(CamcorderProfile.QUALITY_720P), ":");
            cameraView.setCamProfileForRecord(CamcorderProfile.QUALITY_720P);
        }
        else if(CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)){
            if(VERBOSE)Log.d(TAG, "SET 480P as medium");
            videoDimen = new StringTokenizer(findClosestResolutionInCamera(CamcorderProfile.QUALITY_480P), ":");
            cameraView.setCamProfileForRecord(CamcorderProfile.QUALITY_480P);
        }
        else if(CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QVGA)){
            if(VERBOSE)Log.d(TAG, "SET QVGA as medium");
            videoDimen = new StringTokenizer(findClosestResolutionInCamera(CamcorderProfile.QUALITY_QVGA), ":");
            cameraView.setCamProfileForRecord(CamcorderProfile.QUALITY_QVGA);
        }
        else if(CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QCIF)){
            if(VERBOSE)Log.d(TAG, "SET QCIF as medium");
            videoDimen = new StringTokenizer(findClosestResolutionInCamera(CamcorderProfile.QUALITY_QCIF), ":");
            cameraView.setCamProfileForRecord(CamcorderProfile.QUALITY_QCIF);
        }
        else{
            if(VERBOSE)Log.d(TAG, "SET HIGH since other medium resolutions are not supported");
            videoDimen = new StringTokenizer(findClosestResolutionInCamera(CamcorderProfile.QUALITY_HIGH), ":");
            cameraView.setCamProfileForRecord(CamcorderProfile.QUALITY_HIGH);
        }
        return videoDimen;
    }

    private String findClosestResolutionInCamera(int camcorderProf){
        CamcorderProfile profile = CamcorderProfile.get(cameraId, camcorderProf);
        TreeMap<Double, String> ratioDimension = new TreeMap<>();
        List<Camera.Size> videoSizes = parameters.getSupportedVideoSizes();
        if(videoSizes == null){
            videoSizes = parameters.getSupportedPreviewSizes();
        }
        Collections.sort(videoSizes, new CameraSizeComparator());
        for(Camera.Size videoSize: videoSizes){
            double videoRatio = (double) videoSize.width / (double) videoSize.height;
            if (VERBOSE) Log.d(TAG, "Video ratio for " + videoSize.width + " / " + videoSize.height + " is = " + videoRatio);
            //screenWidth is width in portrait mode, and since all video sizes are width / height in landscape mode, we use videoSize.height
            if(videoSize.height <= screenWidth) {
                ratioDimension.put(Math.abs((double)videoSize.width - (double)profile.videoFrameWidth),
                        profile.videoFrameWidth + ":" + profile.videoFrameHeight);
            }
        }
        Log.d(TAG, "Dimension closest to "+camcorderProf+" = "+ratioDimension.get(0d));
        return ratioDimension.get(0d);
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
    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if(success) {
                if(VERBOSE)Log.d(TAG,"auto focus set successfully");
                focused = success;
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
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if(capture)
        {
            try {
                capture = false;
                if(VERBOSE)Log.d(TAG, "inside onpreviewframe");
                int previewWidth = camera.getParameters().getPreviewSize().width;
                int previewHeight = camera.getParameters().getPreviewSize().height;
                YuvImage yuvImage = new YuvImage(bytes, ImageFormat.NV21, previewWidth, previewHeight, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, previewWidth, previewHeight), 100, baos);
                Bitmap thumb = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size());
                baos.close();
                Matrix rotate = new Matrix();
                rotate.setRotate(rotation);
                if(VERBOSE)Log.d(TAG,"rotation = "+rotation);
                thumb = Bitmap.createBitmap(thumb, 0, 0, previewWidth, previewHeight, rotate, false);
                photoFrag.createAndShowPhotoThumbnail(thumb);
                if(VERBOSE)Log.d(TAG, "photo thumbnail created");
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    @Override
    public void setSurfaceView(CameraView surfaceView) {
        cameraView = surfaceView;
    }
}
