package com.flipcam.view;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.opengl.EGL14;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.flipcam.PhotoFragment;
import com.flipcam.R;
import com.flipcam.VideoFragment;
import com.flipcam.camerainterface.CameraOperations;
import com.flipcam.cameramanager.Camera1Manager;
import com.flipcam.cameramanager.Camera2Manager;
import com.flipcam.constants.Constants;
import com.flipcam.util.GLUtil;
import com.flipcam.util.SDCardUtil;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import static android.content.Context.SENSOR_SERVICE;
import static android.os.Environment.getExternalStoragePublicDirectory;
import static com.flipcam.constants.Constants.HIDE_ELAPSED_TIME;
import static com.flipcam.constants.Constants.SHOW_ELAPSED_TIME;

/**
 * Created by Koushick on 15-08-2017.
 */
/*
CameraView is used to display the camera preview using SurfaceView. This is the class where all camera related operations for both photo and video are
performed.
All camera related settings like video resolution, FPS, and photo resolution are set here.
CameraView uses CameraOperations for all camera related functions. CameraOperations is an interface with Camera1 and Camera2 API implementations,
depending on the device's support level.
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback, SurfaceTexture.OnFrameAvailableListener, SensorEventListener{

    public static final String TAG = "CameraView";
    private int VIDEO_WIDTH = 640;  // dimensions for VGA
    private int VIDEO_HEIGHT = 480;
    private int recordVideoWidth = 640;
    private int recordVideoHeight = 480;
    CamcorderProfile camcorderProfile;
    SurfaceTexture surfaceTexture;
    private static final int SIZEOF_FLOAT = 4;
    //Surface to which camera frames are sent for encoding to mp4 format
    EGLSurface encoderSurface=null;
    SurfaceHolder camSurfHolder=null;
    private final float[] mTmpMatrix = new float[16];
    public static final float[] IDENTITY_MATRIX;
    public static final float[] RECORD_IDENTITY_MATRIX;
    static {
        IDENTITY_MATRIX = new float[16];
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
        RECORD_IDENTITY_MATRIX = new float[16];
        Matrix.setIdentityM(RECORD_IDENTITY_MATRIX, 0);
    }
    CameraRenderer.CameraHandler cameraHandler;
    MainHandler mainHandler;
    Object renderObj = new Object();
    volatile boolean isReady=false;
    boolean VERBOSE = false;
    boolean FRAME_VERBOSE=false;
    //Keep in portrait by default.
    boolean portrait=true;
    float rotationAngle = 0.0f;
    boolean backCamera = true;
    volatile boolean isRecord = false;
    MediaRecorder mediaRecorder = null;
    int frameCount=0;
    volatile int cameraFrameCnt=0;
    volatile int frameCnt=0;
    CameraOperations camera1;
    boolean isFocusModeSupported=false;
    int orientation = -1;
    OrientationEventListener orientationEventListener;
    volatile int hour=0;
    volatile int minute=0;
    volatile int second=0;
    String mNextVideoAbsolutePath=null;
    String mNextPhotoAbsolutePath=null;
    TextView timeElapsed;
    TextView memoryConsumed;
    //Default display sizes in case windowManager is not able to return screen size.
    int measuredWidth = 800;
    int measuredHeight = 600;
    String focusMode;
    String flashMode;
    ImageButton flashBtn;
    VideoFragment videoFragment;
    PhotoFragment photoFragment;
    private final SensorManager mSensorManager;
    private AudioManager audioManager;
    private final Sensor mAccelerometer;
    float diff[] = new float[3];
    boolean focusNow = false;
    float sensorValues[] = new float[3];
    File videoFile;
    ImageButton stopButton;
    float imageRotationAngle = 0.0f;
    long previousTime = 0;
    long focusPreviousTime=0;
    int previousOrientation = -1;
    SharedPreferences memoryPrefs;
    int lowestThreshold = getResources().getInteger(R.integer.minimumMemoryWarning);
    long lowestMemory = lowestThreshold * (long)Constants.MEGA_BYTE;
    boolean isPhoneMemory = true;
    StatFs availableStatFs = new StatFs(Environment.getDataDirectory().getPath());
    ContentValues mediaContent = new ContentValues();
    boolean stopCamera = false;
    int camProfileForRecord;
    int totalRotation;
    boolean recordPaused = false;
    boolean recordPauseOffset = false;
    long recordedTimeStamp = -1;
    SharedPreferences sharedPreferences;
    boolean pauseUsedOnce = false;
    Queue recordTimeStamps = new LinkedList();

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if(VERBOSE)Log.d(TAG,"start cameraview");
        getHolder().addCallback(this);
        //Check if device's camera has at least LIMITED support for Camera 2 API. If not, we use Camera 1 API.
        if(isCamera2Supported(context)){
            camera1 = Camera2Manager.getInstance();
        }
        else {
            camera1 = Camera1Manager.getInstance();
        }
        focusMode = camera1.getFocusModeAuto();
        flashMode = camera1.getFlashModeOff();
        mSensorManager = (SensorManager)getContext().getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        audioManager = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    private boolean isCamera2(){
        return camera1 instanceof Camera2Manager;
    }

    public boolean isCamera2Supported(Context context){
        //Camera 2 API support deferred.
        /*boolean supported = false;
        CameraManager cameraManager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics cameraCharacteristics=null;
            for (String camId : cameraManager.getCameraIdList()) {
                cameraCharacteristics = cameraManager.getCameraCharacteristics(camId);
                int cameraOrientation = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraOrientation == CameraCharacteristics.LENS_FACING_BACK) {
                    break;
                }
            }
            int supportLevel = cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if(VERBOSE)Log.d(TAG, "supportLevel = "+supportLevel);
            switch (supportLevel){
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                    supported = false;
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                    supported = true;
                    break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }*/
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //Use accelerometer to check if the device is moving among the x,y or z axes. This means the user is moving the camera and
        //trying to refocus.
        if(Math.abs(System.currentTimeMillis() - focusPreviousTime) >= 240){
            diff[0] = Math.abs(sensorEvent.values[0]-sensorValues[0]);
            diff[1] = Math.abs(sensorEvent.values[1]-sensorValues[1]);
            determineOrientation();
            if(rotationAngle == 90 || rotationAngle == 270){
                if(diff[1] > 0.35){
                    if(VERBOSE)Log.d(TAG,"diff y ="+diff[1]);
                    sensorValues[1] = sensorEvent.values[1];
                    focusNow = true;
                }
                else{
                    if(focusNow) {
                        if(VERBOSE)Log.d(TAG, "Focus now in landscape");
                        camera1.setAutoFocus(true);
                        focusNow = false;
                    }
                }
            }
            else if(diff[0] > 0.6){
                if(VERBOSE)Log.d(TAG, "diff x =" + diff[0]);
                sensorValues[0] = sensorEvent.values[0];
                focusNow = true;
            }
            else{
                if(focusNow) {
                    if(VERBOSE)Log.d(TAG, "Focus now");
                    camera1.setAutoFocus(true);
                    focusNow = false;
                }
            }
            focusPreviousTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        if(VERBOSE)Log.d(TAG,"onAccuracyChanged");
    }

    //Main class handler to receive messages from child threads like CameraRenderer using Android message passing mechanism.
    class MainHandler extends Handler {
        WeakReference<CameraView> cameraView;
        CameraView camView;

        public MainHandler(CameraView cameraView1) {
            cameraView = new WeakReference<>(cameraView1);
        }

        @Override
        public void handleMessage(Message msg) {
            camView = cameraView.get();
            switch(msg.what)
            {
                case Constants.SHOW_ELAPSED_TIME:
                    if(FRAME_VERBOSE)Log.d(TAG,"show time now");
                    showTimeElapsed();
                    break;
                case Constants.HIDE_ELAPSED_TIME:
                    if(VERBOSE)Log.d(TAG, "Hide time elapsed");
                    hideTimeElapsed();
                    break;
                /*case Constants.SHOW_MEMORY_CONSUMED:
                    showMemoryConsumed();
                    break;*/
                case Constants.RECORD_COMPLETE:
                    mediaContent = null;
                    if(VERBOSE)Log.d(TAG,"Update thumbnail now");
                    videoFragment.createAndShowThumbnail(getMediaPath());
                    break;
                case Constants.RECORD_STOP_ENABLE:
                    if(VERBOSE)Log.d(TAG,"Enable stop record");
                    enableStopButton();
                    break;
                case Constants.RECORD_STOP_LOW_MEMORY:
                    setKeepScreenOn(false);
                    orientationEventListener.enable();
                    camera1.disableRecordingHint();
                    //Reset the RECORD Matrix to be portrait.
                    System.arraycopy(IDENTITY_MATRIX,0,RECORD_IDENTITY_MATRIX,0,IDENTITY_MATRIX.length);
                    //Reset Rotation angle
                    rotationAngle = 0f;
                    videoFragment.stopRecordAndSaveFile(true);
                    break;
            }
        }
    }

    void waitUntilReady()
    {
        if(VERBOSE)Log.d(TAG,"Waiting....");
        synchronized (renderObj)
        {
            while(!isReady){
                try {
                    renderObj.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if(VERBOSE)Log.d(TAG,"Come out of WAIT");
    }

    //As each frame is available from the camera, it is sent to CameraRenderer thread to draw on the screen, thereby avoiding performing heavy operations
    //like this on the main thread.
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if(FRAME_VERBOSE)Log.d(TAG,"FRAME Available now");
        if(FRAME_VERBOSE)Log.d(TAG,"is Record = "+isRecord());
        cameraHandler.sendEmptyMessage(Constants.FRAME_AVAILABLE);
    }

    public String getMediaPath(){
        return mNextVideoAbsolutePath;
    }

    public String getPhotoMediaPath()
    {
        return mNextPhotoAbsolutePath;
    }

    public boolean zoomInAndOut(int progress)
    {
        return camera1.zoomInOrOut(progress);
    }

    public CameraOperations getCameraImplementation(){
        return camera1;
    }

    public boolean isZoomSupported()
    {
        return camera1.isZoomSupported();
    }

    public boolean isSmoothZoomSupported()
    {
        return camera1.isSmoothZoomSupported();
    }

    public int getCameraMaxZoom(){
        return camera1.getMaxZoom();
    }

    public void smoothZoomInOrOut(int zoom)
    {
        camera1.smoothZoomInOrOut(zoom);
    }
    SeekBar seekBar;
    public void setSeekBar(SeekBar seekBar)
    {
        this.seekBar = seekBar;
    }

    public void setWindowManager(WindowManager winMgr)
    {
        Point size=new Point();
        winMgr.getDefaultDisplay().getSize(size);
        measuredHeight = ((size != null) ? size.y : measuredHeight);
        measuredWidth = ((size != null) ? size.x : measuredWidth);
    }

    public void setTimeElapsedText(TextView timeElapsedText)
    {
        if(VERBOSE)Log.d(TAG,"Time elapsed textview set");
        timeElapsed = timeElapsedText;
    }

    public void setMemoryConsumedText(TextView memoryConsumedText)
    {
        if(VERBOSE)Log.d(TAG,"Memory consumed");
        memoryConsumed = memoryConsumedText;
    }

    public boolean isFlashOn()
    {
        if(camera1.getFlashMode() == null || camera1.getFlashMode().equalsIgnoreCase(camera1.getFlashModeOff())){
            return false;
        }
        else{
            return true;
        }
    }

    public void registerAccelSensor()
    {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unregisterAccelSensor()
    {
        mSensorManager.unregisterListener(this);
    }

    public boolean isCameraReady()
    {
        return camera1.isCameraReady();
    }

    public boolean isStopCamera() {
        return stopCamera;
    }

    public void setStopCamera(boolean stopCamera) {
        this.stopCamera = stopCamera;
    }

    public boolean isSwitch() {
        return isSwitch;
    }

    public int getTotalRotation() {
        return totalRotation;
    }

    public void setTotalRotation(int totalRotation) {
        this.totalRotation = totalRotation;
    }

    public void setSwitch(boolean aSwitch) {
        isSwitch = aSwitch;
    }

    public int getRecordVideoWidth() {
        return recordVideoWidth;
    }

    public void setRecordVideoWidth(int recordVideoWidth) {
        this.recordVideoWidth = recordVideoWidth;
    }

    public int getRecordVideoHeight() {
        return recordVideoHeight;
    }

    public void setRecordVideoHeight(int recordVideoHeight) {
        this.recordVideoHeight = recordVideoHeight;
    }

    public int getCamProfileForRecord() {
        return camProfileForRecord;
    }

    public void setCamProfileForRecord(int camProfileForRecord) {
        this.camProfileForRecord = camProfileForRecord;
    }

    public boolean isRecord() {
        return isRecord;
    }

    public void setRecord(boolean record) {
        isRecord = record;
    }

    public boolean isRecordPaused() {
        return recordPaused;
    }

    public void setRecordPaused(boolean recordPaused) {
        this.recordPaused = recordPaused;
    }

    public boolean isRecordPauseOffset() {
        return recordPauseOffset;
    }

    public void setRecordPauseOffset(boolean recordPauseOffset) {
        this.recordPauseOffset = recordPauseOffset;
    }

    public boolean isPauseUsedOnce() {
        return pauseUsedOnce;
    }

    public void setPauseUsedOnce(boolean pauseUsedOnce) {
        this.pauseUsedOnce = pauseUsedOnce;
    }

    public void showTimeElapsed()
    {
        if(FRAME_VERBOSE)Log.d(TAG,"displaying time = "+second);
        String showSec = "0";
        String showMin = "0";
        String showHr = "0";
        if(second < 10){
            showSec += second;
        }
        else{
            showSec = second+"";
        }

        if(minute < 10){
            showMin += minute;
        }
        else{
            showMin = minute+"";
        }

        if(hour < 10){
            showHr += hour;
        }
        else{
            showHr = hour+"";
        }
        timeElapsed.setText(showHr + " : " + showMin + " : " + showSec);
        //This is added for blinking the timer if recording is paused.
        timeElapsed.setVisibility(View.VISIBLE);
    }

    public void hideTimeElapsed(){
        timeElapsed.setVisibility(View.INVISIBLE);
    }

    public boolean isTimeVisible(){
        return timeElapsed.getVisibility() == View.VISIBLE;
    }

    /*public void showMemoryConsumed()
    {
        memoryConsumed.setText(MediaUtil.convertMemoryForDisplay(videoFile.length()));
    }*/

    public void setFlashButton(ImageButton flashButton)
    {
        flashBtn = flashButton;
    }

    public void setStopButton(ImageButton stopButton1)
    {
        stopButton = stopButton1;
        if(VERBOSE)Log.d(TAG,"disable stopbutton");
        stopButton.setEnabled(false);
    }

    private void enableStopButton()
    {
        stopButton.setEnabled(true);
    }

    boolean isSwitch = false;

    public void switchCamera()
    {
        if(backCamera)
        {
            setBackCamera(false);
        }
        else
        {
            setBackCamera(true);
        }
        focusMode = camera1.getFocusMode();
        stopAndReleaseCamera();
        unregisterAccelSensor();
        setSwitch(true);
        isFocusModeSupported = false;
        openCameraAndStartPreview();
        if(this.photoFragment!=null && !this.photoFragment.isContinuousAF()) {
            registerAccelSensor();
        }
    }

    public void flashOnOff(boolean flashOn)
    {
        if(this.videoFragment!=null) {
            if (flashOn) {
                camera1.setTorchLight();
            } else {
                camera1.setFlashOnOff(flashOn);
            }
        }
        else{
            camera1.setFlashOnOff(flashOn);
        }
    }

    public AudioManager getAudioManager(){
        return audioManager;
    }

    public boolean isFlashModeSupported(String flashMode)
    {
        return camera1.isFlashModeSupported(flashMode);
    }

    public void setFragmentInstance(VideoFragment videoFragment)
    {
        this.videoFragment = videoFragment;
    }

    public void setPhotoFragmentInstance(PhotoFragment photoFragment)
    {
        this.photoFragment = photoFragment;
    }

    public boolean isBackCamera() {
        return backCamera;
    }

    public void setBackCamera(boolean backCamera) {
        this.backCamera = backCamera;
    }

    public boolean isCameraPermissionAvailable(){
        int camerapermission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA);
        int audiopermission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO);
        int storagepermission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (camerapermission != PackageManager.PERMISSION_GRANTED || audiopermission != PackageManager.PERMISSION_GRANTED ||
                storagepermission != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        else{
            return true;
        }
    }

    //This method sets all the camera parameters and opens the camera. Same is called when you switch the camera.
    public void openCameraAndStartPreview()
    {
        if(!isCameraPermissionAvailable()){
            if(VERBOSE)Log.d(TAG,"Permission turned off mostly at settings");
            if(this.videoFragment!=null) {
                this.videoFragment.askForPermissionAgain();
            }
            else{
                this.photoFragment.askForPermissionAgain();
            }
            return;
        }
        camera1.setSurfaceView(this);
        setStopCamera(false);
        camera1.openCamera(backCamera, getContext());
        if(!isCamera2()) {
            if(!camera1.isCameraReady()){
                Toast.makeText(getContext(),getResources().getString(R.string.noFrontFaceCamera),Toast.LENGTH_SHORT).show();
                return;
            }
            //Set the photo resolution as per selection in settings.
            camera1.getSupportedPictureSizes();
            camera1.setPictureSize();
            //Set the video resolution as per selection in settings.
            if(VERBOSE)Log.d(TAG, "call setResolution");
            camera1.setResolution();
            if(!Build.MODEL.contains(getResources().getString(R.string.nokia71))) {
                camera1.setFPS();
            }
            else{
                //Nokia 7.1 front camera shows slow refresh if FPS is set. So, FPS is not being set for the phone for photos.
                if(this.videoFragment!=null){
                    camera1.setFPS();
                }
                else{
                    //Restrict only for Front camera
                    if(isBackCamera()){
                        camera1.setFPS();
                    }
                }
            }
            //Resize the preview to match the aspect ratio of selected video resolution.
            if(VERBOSE)Log.d(TAG, "call setLayoutAspectRatio");
            setLayoutAspectRatio();
            camera1.startPreview(GLUtil.getSurfaceTexture());
        }
        this.seekBar.setProgress(0);
        this.seekBar.setMax(camera1.getMaxZoom());
        if(VERBOSE)Log.d(TAG,"Setting max zoom = "+camera1.getMaxZoom());
        if(!isCamera2()) {
            //Set the focus mode to continuous focus if recording in progress
            if (this.videoFragment != null && camera1.isFocusModeSupported(camera1.getFocusModeVideo())) {
                if (VERBOSE) Log.d(TAG, "Continuous AF Video");
                camera1.setFocusMode(camera1.getFocusModeVideo());
            } else if (this.photoFragment != null && camera1.isFocusModeSupported(camera1.getFocusModePicture())) {
                if (VERBOSE) Log.d(TAG, "Continuous AF Picture");
                camera1.setFocusMode(camera1.getFocusModePicture());
                this.photoFragment.setContinuousAF(true);
            } else if (this.photoFragment != null && !camera1.isFocusModeSupported(camera1.getFocusModePicture())) {
                if (VERBOSE) Log.d(TAG, "Use Auto AF instead");
                this.photoFragment.setContinuousAF(false);
            }
        }
        LinearLayout.LayoutParams flashParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        if(isRecord()){
            flashParams.setMargins(0,(int)getResources().getDimension(R.dimen.flashOnLeftMargin),0,0);
            flashParams.weight=0.3f;
        }
        else{
            flashParams.setMargins((int)getResources().getDimension(R.dimen.flashOnLeftMargin),0,0,0);
            flashParams.weight=0.5f;
            flashParams.gravity= Gravity.CENTER;
        }
        flashParams.height = (int) getResources().getDimension(R.dimen.flashOnHeight);
        flashParams.width = (int) getResources().getDimension(R.dimen.flashOnWidth);
        flashBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);
        flashBtn.setLayoutParams(flashParams);
        if(!isCamera2()){
            switchFlashOnOff();
        }
        camera1.setAutoExposureAndLock();
    }

    //Return 'true' if this method will create a camera capture session while setting a flash, or 'false' if not.
    //If 'false' is returned, a camera capture session will be created in Camera2Manager.
    public boolean switchFlashOnOff(){
        if(VERBOSE)Log.d(TAG,"isSwitch = "+isSwitch());
        if(isSwitch()){
            setSwitch(false);
            if(this.photoFragment!=null) {
                if (this.photoFragment.isFlashOn()) {
                    flashMode = camera1.getFlashModeOn();
                } else {
                    flashMode = camera1.getFlashModeOff();
                }
            }
            else{
                if (this.videoFragment.isFlashOn()) {
                    flashMode = camera1.getFlashModeTorch();
                } else {
                    flashMode = camera1.getFlashModeOff();
                }
            }

            //Set the flash mode of previous camera
            if(VERBOSE)Log.d(TAG,"flashmode = "+flashMode);
            if (camera1.isFlashModeSupported(flashMode)) {
                if (flashMode.equalsIgnoreCase(camera1.getFlashModeOff())) {
                    flashOnOff(false);
                    flashBtn.setImageDrawable(getResources().getDrawable(R.drawable.camera_flash_on));
                    return true;
                } else {
                    flashBtn.setImageDrawable(getResources().getDrawable(R.drawable.camera_flash_off));
                    flashOnOff(true);
                    return true;
                }
            } else {
                if (flashMode != null && !flashMode.equalsIgnoreCase(camera1.getFlashModeOff())) {
                    if(isCamera2()) {
                        switch(Integer.parseInt(flashMode)) {
                            case 2:
                                Toast.makeText(getContext(), getResources().getString(R.string.flashModeNotSupported, getResources().getString(R.string.torchMode)), Toast.LENGTH_SHORT).show();
                            break;
                            case 1:
                                Toast.makeText(getContext(), getResources().getString(R.string.flashModeNotSupported, getResources().getString(R.string.singleMode)), Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                    else{
                        Toast.makeText(getContext(), getResources().getString(R.string.flashModeNotSupported, flashMode.equalsIgnoreCase(camera1.getFlashModeOn()) ? "On" : flashMode),
                                Toast.LENGTH_SHORT).show();
                    }
                }
                else if(flashMode != null){
                    if(VERBOSE)Log.d(TAG, "FLASH IS OFF");
                    //This means flash mode is off.
                    //If the camera does not support flash mode off, set it manually as default mode.
                    flashOnOff(false);
                    return true;
                }
                flashBtn.setImageDrawable(getResources().getDrawable(R.drawable.camera_flash_on));
                return false;
            }
        }
        else {
            //If you are going out of app and coming back, or switching between phone and video modes, switch off flash.
            flashMode = camera1.getFlashModeOff();
            if(VERBOSE)Log.d(TAG, "SET flashMode TO OFF");
            flashBtn.setImageDrawable(getResources().getDrawable(R.drawable.camera_flash_on));
            flashOnOff(false);
            if(this.photoFragment!=null) {
                this.photoFragment.setFlashOn(false);
            }
            else{
                this.videoFragment.setFlashOn(false);
            }
            return true;
        }
    }

    public void record(boolean noSDCard)
    {
        if(!isRecord()) {
            determineOrientation();
            if(VERBOSE)Log.d(TAG,"Rot angle == "+rotationAngle+", portrait = "+portrait);
            Matrix.rotateM(RECORD_IDENTITY_MATRIX, 0, rotationAngle , 0, 0, 1);
            setLayoutAspectRatio();
            setRecord(true);
            setKeepScreenOn(true);
            startTimerThread();
            cameraHandler.sendEmptyMessage(Constants.RECORD_START);
            orientationEventListener.disable();
            camera1.setRecordingHint();
        }
        else{
            setRecord(false);
            setKeepScreenOn(false);
            stopTimerThread();
            if(noSDCard) {
                cameraHandler.sendEmptyMessage(Constants.RECORD_STOP_NO_SD_CARD);
            }
            else{
                cameraHandler.sendEmptyMessage(Constants.RECORD_STOP);
            }
            orientationEventListener.enable();
            camera1.disableRecordingHint();
            //Reset the RECORD Matrix to be portrait.
            System.arraycopy(IDENTITY_MATRIX,0,RECORD_IDENTITY_MATRIX,0,IDENTITY_MATRIX.length);
            //Reset Rotation angle
            rotationAngle = 0f;
        }
    }

    public void recordPause(){
        cameraHandler.sendEmptyMessage(Constants.RECORD_PAUSE);
    }

    public void recordResume(){
        cameraHandler.sendEmptyMessage(Constants.RECORD_RESUME);
    }

    //This method is used to calculate the orientation necessary for the photo/video when the device is oriented as per portrait or landscape.
    public void determineOrientation() {

        if(orientation != -1) {
            if (((orientation >= 315 && orientation <= 360) || (orientation >= 0 && orientation <= 45)) || (orientation >= 135 && orientation <= 195)) {
                if (orientation >= 135 && orientation <= 195) {
                    //Reverse portrait
                    imageRotationAngle = rotationAngle = 180f;
                } else {
                    //Portrait
                    rotationAngle = 0f;
                    imageRotationAngle = 90f;
                    if(!backCamera){
                        imageRotationAngle = 270f;
                    }
                }
                portrait = true;
            } else {
                if (orientation >= 46 && orientation <= 134) {
                    //Reverse Landscape
                    rotationAngle = 270f;
                    imageRotationAngle = 180f;
                } else {
                    //Landscape
                    imageRotationAngle = rotationAngle = 90f;
                }
                portrait = false;
            }
        }
        else{
            //This device is on a flat surface or parallel to the ground. Check previous orientation.
            if(previousOrientation != -1){
                orientation = previousOrientation;
                determineOrientation();
            }
            else {
                portrait = true;
                rotationAngle = 0f;
            }
        }
        if(!isCamera2()) {
            orientation = (orientation + 45) / 90 * 90;

            if (!backCamera) {
                totalRotation = (camera1.getCameraInfo().orientation - orientation + 360) % 360;
            } else {  // back-facing camera
                totalRotation = (camera1.getCameraInfo().orientation + orientation) % 360;
            }
//            if(VERBOSE)Log.d(TAG,"Rotation in CAMVIEW = "+totalRotation);
            camera1.setRotation(totalRotation);
            setTotalRotation(totalRotation);
        }
    }

    public void setLayoutAspectRatio()
    {
        // Set the preview aspect ratio.
        requestLayout();
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        VIDEO_WIDTH = camera1.getDisplaySizes()[0];
        VIDEO_HEIGHT = camera1.getDisplaySizes()[1];
        int temp = VIDEO_HEIGHT;
        VIDEO_HEIGHT = VIDEO_WIDTH;
        VIDEO_WIDTH = temp;
        layoutParams.height = VIDEO_HEIGHT;
        layoutParams.width = VIDEO_WIDTH;
        if(VERBOSE)Log.d(TAG,"LP Height = "+layoutParams.height);
        if(VERBOSE)Log.d(TAG,"LP Width = "+layoutParams.width);
        if(!portrait) {
            temp = VIDEO_HEIGHT;
            VIDEO_HEIGHT = VIDEO_WIDTH;
            VIDEO_WIDTH = temp;
        }
        int degree;
        if(VERBOSE)Log.d(TAG, "backCamera = "+backCamera);
        if(backCamera) {
            degree = 180;
        }
        else{
            degree = 0;
        }
        if(!isCamera2()) {
            if (VERBOSE) Log.d(TAG, "Orientation == " + camera1.getCameraInfo().orientation);
            int orien = camera1.getCameraInfo().orientation;
            if(orien == 270 && backCamera){
                //Fix for Xiaomi Redmi where the camera orientation is upside down for back camera, when the user starts the application for the first time.
                orien = 90;
            }
            int result = (orien + degree) % 360;
            result = (360 - result) % 360;
            if (VERBOSE) Log.d(TAG, "Result == " + result);
            camera1.setDisplayOrientation(result);
        }
    }

    private void releaseEGLSurface(){
        EGL14.eglDestroySurface(GLUtil.getmEGLDisplay(),GLUtil.getEglSurface());
    }

    private void releaseProgram(){
        GLES20.glDeleteProgram(GLUtil.getmProgramHandle());
    }

    private void releaseEGLContext()
    {
        if (GLUtil.getmEGLDisplay() != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(GLUtil.getmEGLDisplay(), EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroyContext(GLUtil.getmEGLDisplay(), GLUtil.getmEGLContext());
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(GLUtil.getmEGLDisplay());
        }
        GLUtil.setmEGLDisplay(EGL14.EGL_NO_DISPLAY);
        GLUtil.setmEGLContext(EGL14.EGL_NO_CONTEXT);
        GLUtil.setmEGLConfig(null);
    }

    public void capturePhoto()
    {
        determineOrientation();
        if(this.photoFragment.isFlashOn()){
            camera1.setTorchLight();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(VERBOSE)Log.d(TAG, "Shutter sound before click? = "+sharedPreferences.getBoolean(Constants.SHUTTER_SOUND, true));
        camera1.enableShutterSound(sharedPreferences.getBoolean(Constants.SHUTTER_SOUND, true));
        mNextPhotoAbsolutePath = cameraHandler.getCameraRendererInstance().getFilePath(false);
        camera1.setPhotoPath(mNextPhotoAbsolutePath);
        camera1.capturePicture();
    }

    private void stopAndReleaseCamera(){
        camera1.stopPreview();
        camera1.releaseCamera();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if(VERBOSE)Log.d(TAG, "surfCreated holder = " + surfaceHolder);
        camSurfHolder = surfaceHolder;
        mainHandler = new MainHandler(this);
        GLUtil.prepareEGLDisplayandContext();
        CameraRenderer cameraRenderer = new CameraRenderer();
        cameraRenderer.start();
        waitUntilReady();
        orientationEventListener = new OrientationEventListener(getContext(), SensorManager.SENSOR_DELAY_UI){
            @Override
            public void onOrientationChanged(int i) {
                if(orientationEventListener.canDetectOrientation()) {
                    if(previousOrientation == -1) {
                        previousOrientation = orientation = i;
                    }
                    else{
                        if(orientation != -1) {
                            previousOrientation = orientation;
                        }
                        orientation = i;
                    }
                }
            }
        };
        orientationEventListener.enable();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int width, int height) {
        if(VERBOSE)Log.d(TAG,"surfaceChanged = "+surfaceHolder);
        if(VERBOSE)Log.d(TAG,"Width = "+width+", height = "+height);
        if(this.videoFragment!=null) {
            memoryPrefs = this.videoFragment.getActivity().getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
            isPhoneMemory = memoryPrefs.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
            this.videoFragment.showRecordAndThumbnail();
            this.videoFragment.getLatestFileIfExists();
            camera1.setPhotoFragmentInstance(null);
            this.photoFragment = null;
            camera1.setVideoFragmentInstance(this.videoFragment);
        }
        else{
            this.photoFragment.getLatestFileIfExists();
            camera1.setPhotoFragmentInstance(this.photoFragment);
            this.videoFragment = null;
            camera1.setVideoFragmentInstance(null);
        }
        if(!camera1.isCameraReady()) {
            if(VERBOSE)Log.d(TAG, "camera NOT READY. Must Open");
            measuredWidth = width;
            measuredHeight = height;
            frameCount=0;
            openCameraAndStartPreview();
            if(this.photoFragment!=null && !this.photoFragment.isContinuousAF()) {
                registerAccelSensor();
            }
        }
        if(GLUtil.getSurfaceTexture()!=null) {
            GLUtil.getSurfaceTexture().setOnFrameAvailableListener(this);
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return GLUtil.getSurfaceTexture();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if(VERBOSE)Log.d(TAG,"surfaceDestroyed = "+surfaceHolder);
        if(VERBOSE)Log.d(TAG,"cameraHandler = "+cameraHandler);
        orientationEventListener.disable();
        if(this.photoFragment!=null && !this.photoFragment.isContinuousAF()) {
            unregisterAccelSensor();
        }
        mSensorManager.unregisterListener(this);
        if(cameraHandler!=null) {
            //Ensure camera's resources are properly released.
            CameraRenderer cameraRenderer = cameraHandler.getCameraRendererInstance();
            if(camera1.isCameraReady()) {
                //Switch Off countdown if started for selfie timer
                if(this.photoFragment != null){
                    if(this.photoFragment.getCountDown() >= 0 && !isBackCamera()) {
                        if(VERBOSE)Log.d(TAG, "Switch Off Timer");
                        this.photoFragment.setCountDown(-1);
                        this.photoFragment.enableButtons();
                        this.photoFragment.getSelfieCountdown().setVisibility(View.GONE);
                    }
                    //Hide Image highlight and enable buttons, since user can minimize app just before
                    //photo is taken when using Front camera timer
                    this.photoFragment.getImageHighlight().setVisibility(View.INVISIBLE);
                    this.photoFragment.enableButtons();
                    //Since Mediaplayer is initialized for selfie timer, need to stop and release it here
                    if(this.photoFragment.getTimerPlayer() != null) {
                        this.photoFragment.getTimerPlayer().stop();
                        this.photoFragment.getTimerPlayer().release();
                        this.photoFragment.setTimerPlayer(null);
                        if(VERBOSE)Log.d(TAG, "Switch Off TimerPlayer");
                    }
                }
                //Switch off flash light if used during recording.
                if(isCamera2()) {
                    setStopCamera(true);
                    camera1.setFlashOnOff(false);
                }
                else {
                    camera1.removePreviewCallback();
                    stopAndReleaseCamera();
                }
            }
            if(VERBOSE)Log.d(TAG, "stopped and released Camera");
            cameraHandler.removeMessages(Constants.FRAME_AVAILABLE);
            if(GLUtil.getSurfaceTexture()!=null){
                GLUtil.getSurfaceTexture().release();
                GLUtil.setSurfaceTexture(null);
            }
            frameCount=0;
            releaseEGLSurface();
            releaseProgram();
            releaseEGLContext();
            if(isRecord()) {
                //If video recording was in progress, ensure recording is stopped and saved, before exiting.
                if(VERBOSE)Log.d(TAG, "Unmute audio");
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                    if(VERBOSE)Log.d(TAG, "setStreamUnMute");
                    audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                }
                else{
                    if(VERBOSE)Log.d(TAG, "adjustStreamVolumeUnMute");
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
                }
                if(VERBOSE)Log.d(TAG,"Recording in progress.... Stop now");
                setRecord(false);
                //Reset the RECORD Matrix GLUtil.getSurfaceTexture()to be portrait.
                System.arraycopy(IDENTITY_MATRIX,0,RECORD_IDENTITY_MATRIX,0,IDENTITY_MATRIX.length);
                //Reset Rotation angle
                rotationAngle = 0f;
                Message recordStop = new Message();
                if(!memoryPrefs.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
                    if(SDCardUtil.doesSDCardExist(getContext()) != null){
                        recordStop.what = Constants.RECORD_STOP;
                    }
                    else{
                        recordStop.what = Constants.RECORD_STOP_NO_SD_CARD;
                        SharedPreferences.Editor editor = memoryPrefs.edit();
                        editor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
                        editor.commit();
                        videoFragment.showToastSDCardUnavailWhileRecordMessage();
                    }
                }
                else{
                    recordStop.what = Constants.RECORD_STOP;
                }
                cameraHandler.sendMessageAtFrontOfQueue(recordStop);
                if(VERBOSE)Log.d(TAG,"Recording STOPPED");
            }
            cameraHandler.sendEmptyMessage(Constants.SHUTDOWN);
            stopTimerThread();
            try {
                if(cameraRenderer!=null){
                    //If possible wait for camerarenderer to finish before the main thread exits.
                    cameraRenderer.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    //This is the class that draws the each camera frame onto the device's screen. It uses OpenGL API to draw the preview frame.
    //It also controls the video recording.
    class CameraRenderer extends Thread
    {
        int recordStop = -1;
        private final String MIME_TYPE = "video/avc";
        // parameters for recording
        private final int FRAME_RATE = 25;
        private final float BPP = 0.25f;
        private final int TIMEOUT_USEC = 10000;
        boolean isRecording = false;
        int viewWidth = 0;
        int viewHeight = 0;
        Object muxerObj = new Object();
        /**
         * Flag that indicate encoder received EOS(End Of Stream)
         */
        boolean mIsEOS;
        /**
         * Flag the indicate the muxer is running
         */
        boolean mMuxerStarted;
        /**
         * Track Number
         */
        int mTrackIndex;
        /**
         * MediaCodec instance for encoding
         */
        MediaCodec mMediaCodec;				// API >= 16(Android4.1.2)
        /**
         * Weak refarence of MediaMuxerWarapper instance
         */
        MediaMuxer muxer;
        /**
         * BufferInfo instance for dequeuing
         */
        private MediaCodec.BufferInfo mBufferInfo;

        /**
         * previous presentationTimeUs for writing
         */
        private long prevOutputPTSUs = 0;
        public CameraRenderer()
        {
            //Empty constructor
        }

        @Override
        public void run()
        {
            Looper.prepare();
            cameraHandler = new CameraHandler(this);
            GLUtil.createSurfaceTexture(camSurfHolder, GLUtil.getmEGLDisplay(), GLUtil.getmEGLConfig());
            synchronized (renderObj){
                isReady=true;
                renderObj.notify();
            }
            if(VERBOSE)Log.d(TAG,"Main thread notified");
            Looper.loop();
            if(VERBOSE)Log.d(TAG,"Camera Renderer STOPPED");
        }

        private int calcBitRate() {
            final int bitrate = (int)(BPP * FRAME_RATE * getRecordVideoWidth() * getRecordVideoHeight());
            Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
            return bitrate;
        }

        public void setupMediaEncoder() throws IOException {
            if (VERBOSE) Log.i(TAG, "prepare Media Encoder : ");
            final MediaCodecInfo videoCodecInfo = selectVideoCodec(MIME_TYPE);
            if (videoCodecInfo == null) {
                Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
                return;
            }
            if (VERBOSE) Log.i(TAG, "selected codec: " + videoCodecInfo.getName());

            final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, getRecordVideoWidth(), getRecordVideoHeight());
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);	// API >= 18
            format.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate());
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
            if (VERBOSE) Log.i(TAG, "format: " + format);

            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            // get Surface for encoder input
            encoderSurface = GLUtil.prepareWindowSurface( mMediaCodec.createInputSurface(), GLUtil.getmEGLDisplay(), GLUtil.getmEGLConfig());
        }

        /**
         * select color format available on specific codec and we can use.
         * @return 0 if no colorFormat is matched
         */
        public final int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
            if (VERBOSE) Log.i(TAG, "selectColorFormat: ");
            int result = 0;
            final MediaCodecInfo.CodecCapabilities caps;
            caps = codecInfo.getCapabilitiesForType(mimeType);
            int colorFormat;
            for (int i = 0; i < caps.colorFormats.length; i++) {
                colorFormat = caps.colorFormats[i];
                if (isRecognizedViewoFormat(colorFormat)) {
                    if (result == 0)
                        result = colorFormat;
                    break;
                }
            }
            if (result == 0)
                Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
            return result;
        }

        /**
         * color formats that we can use in this class
         */
        private int[] recognizedFormats = new int[] {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
        };

        public final boolean isRecognizedViewoFormat(final int colorFormat) {
            if (VERBOSE) Log.i(TAG, "isRecognizedViewoFormat:colorFormat=" + colorFormat);
            final int n = recognizedFormats != null ? recognizedFormats.length : 0;
            for (int i = 0; i < n; i++) {
                if (recognizedFormats[i] == colorFormat) {
                    return true;
                }
            }
            return false;
        }

        /**
         * select the first codec that match a specific MIME type
         * @param mimeType
         * @return null if no codec matched
         */
        public final MediaCodecInfo selectVideoCodec(final String mimeType) {
            if (VERBOSE) Log.v(TAG, "selectVideoCodec:");

            // get the list of available codecs
            final int numCodecs = MediaCodecList.getCodecCount();
            for (int i = 0; i < numCodecs; i++) {
                final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

                if (!codecInfo.isEncoder()) {	// skipp decoder
                    continue;
                }
                // select first codec that match a specific MIME type and color format
                final String[] types = codecInfo.getSupportedTypes();
                for (int j = 0; j < types.length; j++) {
                    if (types[j].equalsIgnoreCase(mimeType)) {
                        if (VERBOSE) Log.i(TAG, "codec:" + codecInfo.getName() + ",MIME=" + types[j]);
                        final int format = selectColorFormat(codecInfo, mimeType);
                        if (format > 0) {
                            return codecInfo;
                        }
                    }
                }
            }
            return null;
        }

        public void setupMediaRecorder(int width, int height, int camcorderProf)
        {
            camcorderProfile = CamcorderProfile.get(camera1.getCameraId(),camcorderProf);
            mediaRecorder = new MediaRecorder();
            try {
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            } catch (Exception e) {
                if (VERBOSE)
                    Log.e(TAG, "Camera not having a mic oriented in the same way. Use the default microphone");
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            }
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mNextVideoAbsolutePath = getFilePath(true);
//            videoFile = new File(mNextVideoAbsolutePath);
            mediaRecorder.setOutputFile(mNextVideoAbsolutePath);
            mediaRecorder.setVideoEncodingBitRate(camcorderProfile.videoBitRate);
            mediaRecorder.setVideoFrameRate(camcorderProfile.videoFrameRate);
            if(!portrait){
                if(VERBOSE)Log.d(TAG, "LS videoWidth = "+width);
                if(VERBOSE)Log.d(TAG, "LS videoHeight = "+height);
            }
            else{
                int temp = width;
                width = height;
                height = temp;
                if(VERBOSE)Log.d(TAG, "PR videoWidth = "+width);
                if(VERBOSE)Log.d(TAG, "PR videoHeight = "+height);
            }
            mediaRecorder.setVideoSize(width, height);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(videoFragment.getAudioBitRate());
            mediaRecorder.setAudioSamplingRate(videoFragment.getAudioSampleRate());
            mediaRecorder.setAudioChannels(videoFragment.getAudioChannelInput());
            try {
                mediaRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            encoderSurface = GLUtil.prepareWindowSurface(mediaRecorder.getSurface(), GLUtil.getmEGLDisplay(), GLUtil.getmEGLConfig());
            mediaContent = new ContentValues();
            mediaContent.put("filename", mNextVideoAbsolutePath);
            mediaContent.put("memoryStorage", (memoryPrefs.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true) ? "1" : "0"));
        }

        public String getFilePath(boolean video) {
            StringBuilder path = new StringBuilder(Constants.EMPTY);
            SharedPreferences sharedPreferences;
            sharedPreferences = (video ? videoFragment.getActivity().getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE) :
                    photoFragment.getActivity().getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE));
            if(sharedPreferences.contains(Constants.SAVE_MEDIA_PHONE_MEM))
            {
                if(sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
                    path.append(fetchPhoneMemoryPath(video));
                }
                else{
                    path.append(sharedPreferences.getString(Constants.SD_CARD_PATH, ""));
                    SimpleDateFormat sdf = new SimpleDateFormat(getResources().getString(R.string.DATE_FORMAT_FOR_FILE));
                    String filename = sdf.format(new Date());
                    if(VERBOSE)Log.d(TAG, "sd card filename = " + filename);
                    path.append(video ? getResources().getString(R.string.FC_VID_PREFIX) + filename + getResources().getString(R.string.VID_EXT) :
                            getResources().getString(R.string.FC_IMG_PREFIX) + filename + getResources().getString(R.string.IMG_EXT));
                    if(VERBOSE)Log.d(TAG, "SD Card Path = "+path);
                }
            }
            else {
                path.append(fetchPhoneMemoryPath(video));
            }
            return path.toString();
        }

        public String fetchPhoneMemoryPath(boolean video){
            String phonePath;
            File dcim;
            dcim = getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + getResources().getString(R.string.FC_ROOT));
            if (!dcim.exists()) {
                dcim.mkdirs();
            }
            SimpleDateFormat sdf = new SimpleDateFormat(getResources().getString(R.string.DATE_FORMAT_FOR_FILE));
            String filename = sdf.format(new Date());
            if(VERBOSE)Log.d(TAG, "filename = " + filename);
            phonePath = (video ? dcim.getPath() + getResources().getString(R.string.FC_VID_PREFIX) + filename + getResources().getString(R.string.VID_EXT) :
                    dcim.getPath() + getResources().getString(R.string.FC_IMG_PREFIX) + filename + getResources().getString(R.string.IMG_EXT));
            if(VERBOSE)Log.d(TAG, "Saving media file at = " + phonePath);
            return phonePath;
        }

        void signalEndOfInputStream() {
            if (VERBOSE) Log.d(TAG, "sending EOS to encoder");
            // signalEndOfInputStream is only avairable for video encoding with surface
            // and equivalent sending a empty buffer with BUFFER_FLAG_END_OF_STREAM flag.
            encode(null, 0, getPTSUs());
        }

        /**
         * Method to set byte array to the MediaCodec encoder
         * @param buffer
         * @param length　length of byte array, zero means EOS.
         * @param presentationTimeUs
         */
        void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
            final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            while (isRecording) {
                final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufferIndex >= 0) {
                    final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    if (buffer != null) {
                        inputBuffer.put(buffer);
                    }
//	            if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
                    if (length <= 0) {
                        // send EOS
                        mIsEOS = true;
                        if (VERBOSE) Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
                        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
                                presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        break;
                    } else {
                        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
                                presentationTimeUs, 0);
                    }
                    break;
                } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // wait for MediaCodec encoder is ready to encode
                    // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
                    // will wait for maximum TIMEOUT_USEC(10msec) on each call
                }
            }
        }

        void drain() {
            if (mMediaCodec == null) return;
            ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            int encoderStatus, count = 0;
            if (muxer == null) {
//        	throw new NullPointerException("muxer is unexpectedly null");
                Log.w(TAG, "muxer is unexpectedly null");
                return;
            }
            LOOP:	while (isRecording) {
                // get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])
                encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // wait 5 counts(=TIMEOUT_USEC x 5 = 50msec) until data/EOS come
                    if (!mIsEOS) {
                        if (++count > 5)
                            break LOOP;		// out of while
                    }
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE) Log.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                    // this shoud not come when encoding
                    encoderOutputBuffers = mMediaCodec.getOutputBuffers();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (VERBOSE) Log.v(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
                    // this status indicate the output format of codec is changed
                    // this should come only once before actual encoded data
                    // but this status never come on Android4.3 or less
                    // and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
                    if (mMuxerStarted) {	// second time request is error
                        throw new RuntimeException("format changed twice");
                    }
                    // get output format from codec and pass them to muxer
                    // getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
                    final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16
                    mTrackIndex = muxer.addTrack(format);
                    muxer.start();
                    mMuxerStarted = true;
                    /*if (!muxer.start()) {
                        // we should wait until muxer is ready
                        synchronized (muxer) {
                            while (!muxer.isStarted())
                                try {
                                    muxer.wait(100);
                                } catch (final InterruptedException e) {
                                    break LOOP;
                                }
                        }
                    }*/
                } else if (encoderStatus < 0) {
                    // unexpected status
                    if (VERBOSE) Log.w(TAG, "drain:unexpected result from encoder#dequeueOutputBuffer: " + encoderStatus);
                } else {
                    final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        // this never should come...may be a MediaCodec internal error
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                    }
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // You shoud set output format to muxer here when you target Android4.3 or less
                        // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                        // therefor we should expand and prepare output format from buffer data.
                        // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                        if (VERBOSE) Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
                        mBufferInfo.size = 0;
                    }

                    if (mBufferInfo.size != 0) {
                        // encoded data is ready, clear waiting counter
                        count = 0;
                        if (!mMuxerStarted) {
                            // muxer is not ready...this will prrograming failure.
                            throw new RuntimeException("drain:muxer hasn't started");
                        }
                        // write encoded data to muxer(need to adjust presentationTimeUs.
                        mBufferInfo.presentationTimeUs = getPTSUs();
                        muxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                        prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                    }
                    // return buffer to encoder
                    mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        // when EOS come.
                        break;      // out of while
                    }
                }
            }
        }

        long pauseDuration = 0;
        void drawFrame()
        {
            if(GLUtil.getmEGLConfig()!=null && camera1.isCameraReady()) {
                GLUtil.makeCurrent(GLUtil.getEglSurface());
                if(FRAME_VERBOSE) Log.d(TAG,"made current");
                //Get next frame from camera
                GLUtil.getSurfaceTexture().updateTexImage();
                GLUtil.getSurfaceTexture().getTransformMatrix(mTmpMatrix);

                //Fill the surfaceview with Camera frame
                viewWidth = getWidth();
                viewHeight = getHeight();
                if (frameCount == 0) {
                    if(FRAME_VERBOSE)Log.d(TAG, "FRAME Count = "+frameCount);
                    if(FRAME_VERBOSE)Log.d(TAG,"SV Width == "+viewWidth+", SV Height == "+viewHeight);
                }
                GLES20.glViewport(0, 0, viewWidth, viewHeight);
                GLUtil.draw(IDENTITY_MATRIX, GLUtil.createFloatBuffer(GLUtil.FULL_RECTANGLE_COORDS), 0, (GLUtil.FULL_RECTANGLE_COORDS.length / 2), 2, 2 * SIZEOF_FLOAT, mTmpMatrix,
                        GLUtil.createFloatBuffer(GLUtil.FULL_RECTANGLE_TEX_COORDS), 2 * SIZEOF_FLOAT);

                if(FRAME_VERBOSE)Log.d(TAG, "Draw on screen...."+isRecording);
                //Calls eglSwapBuffers.  Use this to "publish" the current frame.
                EGL14.eglSwapBuffers(GLUtil.getmEGLDisplay(), GLUtil.getEglSurface());

                long tempTime;
                if(isRecording) {
                    GLUtil.makeCurrent(encoderSurface);
                    if (FRAME_VERBOSE) Log.d(TAG, "Made encoder surface current");
                    if(!portrait) {
                        GLES20.glViewport(0, 0, getRecordVideoWidth(), getRecordVideoHeight());
                    }
                    else{
                        GLES20.glViewport(0, 0, getRecordVideoHeight(), getRecordVideoWidth());
                    }
                    GLUtil.draw(RECORD_IDENTITY_MATRIX, GLUtil.createFloatBuffer(GLUtil.FULL_RECTANGLE_COORDS), 0, (GLUtil.FULL_RECTANGLE_COORDS.length / 2), 2, 2 * SIZEOF_FLOAT, mTmpMatrix,
                            GLUtil.createFloatBuffer(GLUtil.FULL_RECTANGLE_TEX_COORDS), 2 * SIZEOF_FLOAT);
                    if (FRAME_VERBOSE) Log.d(TAG, "Populated to encoder");

                    if(Math.abs(System.currentTimeMillis() - previousTime) >= 1000){
                        if(FRAME_VERBOSE)Log.d(TAG,"difference of 1 sec");
                        previousTime = System.currentTimeMillis();
                        if(recordStop == 1) {
                            if(!stopButton.isEnabled()) {
                                mainHandler.sendEmptyMessage(Constants.RECORD_STOP_ENABLE);
                            }
                        }
                    }
                    else if(previousTime == 0){
                        previousTime = System.currentTimeMillis();
                    }

                    if(sharedPreferences.getBoolean(Constants.SHOW_MEMORY_CONSUMED_MSG, false)) {
                        mainHandler.sendEmptyMessage(Constants.SHOW_MEMORY_CONSUMED);
                    }

                    if (recordStop == -1) {
                        mediaRecorder.start();
//                        mMediaCodec.start();
                        recordStop = 1;
                    }
//                    drain();
                    updateTimer = true;
                    EGLExt.eglPresentationTimeANDROID(GLUtil.getmEGLDisplay(), encoderSurface, getPTSUs());
                    EGL14.eglSwapBuffers(GLUtil.getmEGLDisplay(), encoderSurface);
                }
            }
            frameCount++;
            availableStatFs.restat(Environment.getDataDirectory().getPath());
            if(isRecord() && isPhoneMemory && (availableStatFs.getAvailableBytes() < lowestMemory)) {
                if(FRAME_VERBOSE)Log.d(TAG, "lowestMemory = "+lowestMemory);
                if(FRAME_VERBOSE)Log.d(TAG, "avail mem = "+availableStatFs.getAvailableBytes());
                setRecord(false);
                stopTimerThread();
                isRecording = false;
                recordStop = -1;
                cameraHandler.setRecordIncomplete(false);
                try {
                    mediaRecorder.stop();
                }
                catch(RuntimeException runtime){
                    if(FRAME_VERBOSE)Log.d(TAG,"Video data not received... delete file = "+videoFile.getPath());
                    if(videoFile.delete()){
                        if(FRAME_VERBOSE)Log.d(TAG,"File deleted");
                    }
                    cameraHandler.setRecordIncomplete(true);
                }
                mediaRecorder.release();
                mediaRecorder = null;
                if(isCamera2()){
                    stopAndReleaseCamera();
                }
                if(FRAME_VERBOSE)Log.d(TAG,"stop isRecording == "+isRecording);
                if(!cameraHandler.isRecordIncomplete()){
                    mainHandler.sendEmptyMessage(Constants.RECORD_COMPLETE);
                }
                mainHandler.sendEmptyMessage(Constants.RECORD_STOP_LOW_MEMORY);
            }
        }

        private volatile long pauseDelayTime=0;
        private volatile long oncePauseTime;

        @TargetApi(Build.VERSION_CODES.N)
        private void pause(){
            isRecording = false;
            updateTimer = false;
            oncePauseTime = System.nanoTime() / 1000;
            mediaRecorder.pause();
        }

        @TargetApi(Build.VERSION_CODES.N)
        private void resumeRecord(){
            isRecording = true;
            oncePauseTime = System.nanoTime() / 1000 - oncePauseTime;
            pauseDelayTime += oncePauseTime;
            mediaRecorder.resume();
        }

        protected long getPTSUs() {
            long result = System.nanoTime() - pauseDelayTime;
            return result;
        }

        void shutdown()
        {
            Looper.myLooper().quit();
        }

        class CameraHandler extends Handler
        {
            WeakReference<CameraRenderer> cameraRender;
            CameraRenderer cameraRenderer;
            boolean recordIncomplete = false;

            public boolean isRecordIncomplete() {
                return recordIncomplete;
            }

            public void setRecordIncomplete(boolean recordIncomplete) {
                this.recordIncomplete = recordIncomplete;
            }

            public CameraHandler(CameraRenderer cameraRenderer){
                cameraRender = new WeakReference<>(cameraRenderer);
            }

            private CameraRenderer getCameraRendererInstance()
            {
                return cameraRenderer;
            }

            @Override
            public void handleMessage(Message msg) {
                cameraRenderer = cameraRender.get();
                switch(msg.what)
                {
                    case Constants.SHUTDOWN:
                        if(VERBOSE)Log.d(TAG,"Shutdown msg received");
                        cameraRenderer.shutdown();
                        break;
                    case Constants.FRAME_AVAILABLE:
                        if(FRAME_VERBOSE)Log.d(TAG,"send to FRAME_AVAILABLE");
                        cameraRenderer.drawFrame();
                        if(FRAME_VERBOSE)Log.d(TAG,"Record = "+isRecord());
                        break;
                    case Constants.RECORD_START:
                        cameraRenderer.setupMediaRecorder(getRecordVideoWidth(), getRecordVideoHeight(), getCamProfileForRecord());
                        /*mNextVideoAbsolutePath = getFilePath(true);
                        try {
                            muxer = new MediaMuxer(mNextVideoAbsolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mBufferInfo = new MediaCodec.BufferInfo();
                        mTrackIndex = -1;
                        mIsEOS = false;
                        try {
                            cameraRenderer.setupMediaEncoder();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/
                        hour = 0; minute = 0; second = 0;
                        isRecording = true;
                        break;
                    case Constants.RECORD_STOP:
                        /*drain();
                        signalEndOfInputStream();
                        drain();*/
                        isRecording = false;
                        recordStop = -1;
                        recordIncomplete = false;
                        setPauseUsedOnce(false);
                        try {
                            mediaRecorder.stop();
                        }
                        catch(RuntimeException runtime){
                            if(VERBOSE)Log.d(TAG,"Video data not received... delete file = "+videoFile.getPath());
                            if(videoFile.delete()){
                                if(VERBOSE)Log.d(TAG,"File deleted");
                            }
                            recordIncomplete = true;
                        }
                        mediaRecorder.release();
                        mediaRecorder = null;
                        /*mMediaCodec.stop();
                        mMediaCodec.release();
                        mMediaCodec = null;*/
                        if(VERBOSE)Log.d(TAG,"stop isRecording == "+isRecording);
                        if(!recordIncomplete){
                            mainHandler.sendEmptyMessage(Constants.RECORD_COMPLETE);
                        }
                        if(VERBOSE)Log.d(TAG, "Exit recording...");
                        if(VERBOSE)Log.d(TAG,"Orig frame = "+frameCount+" , Rendered frame "+frameCnt);
                        break;
                    case Constants.RECORD_PAUSE:
                        if(VERBOSE)Log.d(TAG, "Pausing record");
                        pause();
                        break;
                    case Constants.RECORD_RESUME:
                        if(VERBOSE)Log.d(TAG, "Resuming record");
                        resumeRecord();
                        break;
                    case Constants.RECORD_STOP_NO_SD_CARD:
                        isRecording = false;
                        recordStop = -1;
                        recordIncomplete = false;
                        mediaRecorder.release();
                        mediaRecorder = null;
                        if(VERBOSE)Log.d(TAG,"stop no SD Card isRecording == "+isRecording);
                        break;
                    case Constants.GET_CAMERA_RENDERER_INSTANCE:
                        getCameraRendererInstance();
                        break;
                }
            }
        }
    }

    public void startTimerThread()
    {
        startTimer = true;
        CameraView.VideoTimer videoTimer = new CameraView.VideoTimer();
        videoTimer.start();
    }

    public void stopTimerThread()
    {
        startTimer = false;
    }
    volatile boolean startTimer = false;
    volatile boolean updateTimer = false;

    //This thread controls the timer for video recording. A separate thread was needed, since the CameraRenderer thread was overloaded with displaying
    //and recording each frame, the timer value was incorrect during recording.
    class VideoTimer extends Thread
    {
        long previousTimeBlink = 0;
        @Override
        public void run() {
            if(VERBOSE)Log.d(TAG,"VideoTimer STARTED");
            while(startTimer){
                while(updateTimer){
                    try {
                        Thread.sleep(1000);
                        if(second < 59){
                            second++;
                        }
                        else if(minute < 59){
                            minute++;
                            second = 0;
                        }
                        else{
                            minute = 0;
                            second = 0;
                            hour++;
                        }
                        mainHandler.sendEmptyMessage(SHOW_ELAPSED_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(!startTimer){
                        break;
                    }
                }
                if(isRecordPaused()){
                    //Add blinking text effect
                    if(Math.abs(System.currentTimeMillis() - previousTimeBlink) >= 700){
                        if(VERBOSE)Log.d(TAG,"difference of 1.5 sec");
                        previousTimeBlink = System.currentTimeMillis();
                        if(isTimeVisible()) {
                            mainHandler.sendEmptyMessage(HIDE_ELAPSED_TIME);
                        }
                        else{
                            mainHandler.sendEmptyMessage(SHOW_ELAPSED_TIME);
                        }
                    }
                    else if(previousTimeBlink == 0){
                        previousTimeBlink = System.currentTimeMillis();
                    }
                }
            }
            if(VERBOSE)Log.d(TAG,"VideoTimer STOPPED");
        }
    }
}
