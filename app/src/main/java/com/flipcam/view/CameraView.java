package com.flipcam.view;

import android.Manifest;
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
import android.media.MediaRecorder;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.flipcam.PhotoFragment;
import com.flipcam.R;
import com.flipcam.VideoFragment;
import com.flipcam.camerainterface.CameraOperations;
import com.flipcam.cameramanager.Camera1Manager;
import com.flipcam.cameramanager.Camera2Manager;
import com.flipcam.constants.Constants;
import com.flipcam.util.GLUtil;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.Context.SENSOR_SERVICE;
import static android.os.Environment.getExternalStoragePublicDirectory;
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
    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLConfig mEGLConfig = null;
    // Android-specific extension.
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;
    public static final int FLAG_RECORDABLE = 0x01;
    private int mProgramHandle;
    private int mTextureTarget;
    private int mTextureId;
    private int muMVPMatrixLoc;
    private static final int SIZEOF_FLOAT = 4;
    private int muTexMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    //Surface onto which camera frames are drawn
    EGLSurface eglSurface;
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
    boolean VERBOSE=true;
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
    double kBDelimiter = Constants.KILO_BYTE;
    double mBDelimiter = Constants.MEGA_BYTE;
    double gBDelimiter = Constants.GIGA_BYTE;
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
    public float colorVal = 0.0f;

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
                case Constants.SHOW_MEMORY_CONSUMED:
                    showMemoryConsumed();
                    break;
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
        if(isRecord()){
            if(FRAME_VERBOSE)Log.d(TAG,"Frame avail cnt = "+(++cameraFrameCnt));
        }
        cameraHandler.sendEmptyMessage(Constants.FRAME_AVAILABLE);
    }

    private void prepareEGLDisplayandContext()
    {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null;
            throw new RuntimeException("unable to initialize EGL14");
        }
        EGLConfig config = getConfig(FLAG_RECORDABLE, 2);
        if (config == null) {
            throw new RuntimeException("Unable to find a suitable EGLConfig");
        }
        int[] attrib2_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        EGLContext context = EGL14.eglCreateContext(mEGLDisplay, config, EGL14.EGL_NO_CONTEXT,
                attrib2_list, 0);
        checkEglError("eglCreateContext");
        mEGLConfig = config;
        mEGLContext = context;

        // Confirm with query.
        int[] values = new int[1];
        EGL14.eglQueryContext(mEGLDisplay, mEGLContext, EGL14.EGL_CONTEXT_CLIENT_VERSION,
                values, 0);
        if(VERBOSE)Log.d(TAG, "EGLContext created, client version " + values[0]);
    }

    private void checkEglError(String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    private EGLConfig getConfig(int flags, int version) {
        int renderableType = EGL14.EGL_OPENGL_ES2_BIT;

        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, renderableType,
                EGL14.EGL_NONE, 0,      // placeholder for recordable [@-3]
                EGL14.EGL_NONE
        };
        if ((flags & FLAG_RECORDABLE) != 0) {
            attribList[attribList.length - 3] = EGL_RECORDABLE_ANDROID;
            attribList[attribList.length - 2] = 1;
        }
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                numConfigs, 0)) {
            Log.w(TAG, "unable to find RGB8888 / " + version + " EGLConfig");
            return null;
        }
        return configs[0];
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
    }

    public void showMemoryConsumed()
    {
        if(videoFile.length() >= kBDelimiter && videoFile.length() < mBDelimiter){
            if(VERBOSE)Log.d(TAG,"KB = "+videoFile.length());
            double kbconsumed = videoFile.length()/kBDelimiter;
            memoryConsumed.setText((Math.floor(kbconsumed * 100.0))/100.0 + getResources().getString(R.string.MEM_PF_KB));
        }
        else if(videoFile.length() >= mBDelimiter && videoFile.length() < gBDelimiter){
            if(VERBOSE)Log.d(TAG,"MB = "+videoFile.length());
            double mbconsumed = videoFile.length()/mBDelimiter;
            memoryConsumed.setText((Math.floor(mbconsumed * 100.0))/100.0 + " " + getResources().getString(R.string.MEM_PF_MB));
        }
        else {
            if(VERBOSE)Log.d(TAG,"GB = "+videoFile.length());
            double gbconsumed = videoFile.length()/gBDelimiter;
            memoryConsumed.setText((Math.floor(gbconsumed * 100.0))/100.0 + " " + getResources().getString(R.string.MEM_PF_GB));
        }
    }

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
            backCamera = false;
        }
        else
        {
            backCamera = true;
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
                camera1.setFlashOnOff(false);
            }
        }
        else{
            if(!flashOn) {
                camera1.setFlashOnOff(false);
            }
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
            camera1.setFPS();
            //Resize the preview to match the aspect ratio of selected video resolution.
            if(VERBOSE)Log.d(TAG, "call setLayoutAspectRatio");
            setLayoutAspectRatio();
            camera1.startPreview(surfaceTexture);
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

    public SurfaceTexture getSurfaceTexture(){
        return surfaceTexture;
    }

    //Return 'true' if this method will create a camera capture session while setting a flash, or 'false' if not.
    //If 'false' is returned, a camera capture session will be created in Camera2Manager.
    public boolean switchFlashOnOff(){
        if(VERBOSE)Log.d(TAG,"isSwitch = "+isSwitch());
        if(isSwitch()){
            setSwitch(false);
            if(this.photoFragment!=null) {
                if (this.photoFragment.isFlashOn()) {
                    flashMode = camera1.getFlashModeTorch();
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
                } else if (flashMode.equalsIgnoreCase(camera1.getFlashModeTorch())) {
                    flashBtn.setImageDrawable(getResources().getDrawable(R.drawable.camera_flash_off));
                    if(this.videoFragment!=null){
                        flashOnOff(true);
                        return true;
                    }
                    return false;
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
                        Toast.makeText(getContext(), getResources().getString(R.string.flashModeNotSupported, flashMode), Toast.LENGTH_SHORT).show();
                    }
                }
                flashBtn.setImageDrawable(getResources().getDrawable(R.drawable.camera_flash_on));
                return false;
            }
        }
        else {
            //If you are going out of app and coming back, or switching between phone and video modes, switch off flash.
            flashMode = camera1.getFlashModeOff();
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
        return true;
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
            int rotation;
            if (!backCamera) {
                rotation = (camera1.getCameraInfo().orientation - orientation + 360) % 360;
            } else {  // back-facing camera
                rotation = (camera1.getCameraInfo().orientation + orientation) % 360;
            }
            //if(VERBOSE)Log.d(TAG,"Rotation = "+rotation);
            camera1.setRotation(rotation);
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
        if(backCamera) {
            degree = 180;
        }
        else{
            degree = 0;
        }
        if(!isCamera2()) {
            if (VERBOSE) Log.d(TAG, "Orientation == " + camera1.getCameraInfo().orientation);
            int result = (camera1.getCameraInfo().orientation + degree) % 360;
            result = (360 - result) % 360;
            if (VERBOSE) Log.d(TAG, "Result == " + result);
            camera1.setDisplayOrientation(result);
        }
    }

    private void releaseEGLSurface(){
        EGL14.eglDestroySurface(mEGLDisplay,eglSurface);
    }

    private void releaseProgram(){
        GLES20.glDeleteProgram(mProgramHandle);
    }

    private void releaseEGLContext()
    {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);
        }
        mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        mEGLContext = EGL14.EGL_NO_CONTEXT;
        mEGLConfig = null;
    }

    public void capturePhoto()
    {
        determineOrientation();
        if(this.photoFragment!=null){
            if(this.photoFragment.isFlashOn()){
                camera1.setTorchLight();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        camera1.setRotation(imageRotationAngle);
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
        prepareEGLDisplayandContext();
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
            camera1.setVideoFragmentInstance(this.videoFragment);
        }
        else{
            this.photoFragment.getLatestFileIfExists();
            camera1.setPhotoFragmentInstance(this.photoFragment);
            camera1.setVideoFragmentInstance(null);
        }
        if(!camera1.isCameraReady()) {
            if(VERBOSE)Log.d(TAG, "camera READY");
            measuredWidth = width;
            measuredHeight = height;
            frameCount=0;
            openCameraAndStartPreview();
            if(this.photoFragment!=null && !this.photoFragment.isContinuousAF()) {
                registerAccelSensor();
            }
        }
        if(surfaceTexture!=null) {
            surfaceTexture.setOnFrameAvailableListener(this);
        }
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
            if(surfaceTexture!=null){
                surfaceTexture.release();
                surfaceTexture=null;
            }
            frameCount=0;
            releaseEGLSurface();
            releaseProgram();
            releaseEGLContext();
            if(isRecord()) {
                //If video recording was in progress, ensure recording is stopped and saved, before exiting.
                Log.d(TAG, "Unmute audio");
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                    Log.d(TAG, "setStreamUnMute");
                    audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                }
                else{
                    Log.d(TAG, "adjustStreamVolumeUnMute");
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
                }
                if(VERBOSE)Log.d(TAG,"Recording in progress.... Stop now");
                setRecord(false);
                //Reset the RECORD Matrix to be portrait.
                System.arraycopy(IDENTITY_MATRIX,0,RECORD_IDENTITY_MATRIX,0,IDENTITY_MATRIX.length);
                //Reset Rotation angle
                rotationAngle = 0f;
                Message recordStop = new Message();
                if(!memoryPrefs.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
                    if(videoFragment.doesSDCardExist() != null){
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
        boolean isRecording = false;
        public CameraRenderer()
        {
            //Empty constructor
        }

        @Override
        public void run()
        {
            Looper.prepare();
            cameraHandler = new CameraHandler(this);
            createSurfaceTexture();
            synchronized (renderObj){
                isReady=true;
                renderObj.notify();
            }
            if(VERBOSE)Log.d(TAG,"Main thread notified");
            Looper.loop();
            if(VERBOSE)Log.d(TAG,"Camera Renderer STOPPED");
        }

        private void makeCurrent(EGLSurface surface)
        {
            EGL14.eglMakeCurrent(mEGLDisplay, surface, surface, mEGLContext);
        }
        /**
         * Copyright 2014 Google Inc. All rights reserved.
         Borrowed from Grafika project. This is NOT an official Google Project,
         and has an Open Source license.
         https://github.com/google/grafika
         * Creates a texture object suitable for use with this program.
         * <p>
         * On exit, the texture will be bound.
         */
        public int createGLTextureObject() {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            GLUtil.checkGlError("glGenTextures");

            int texId = textures[0];
            GLES20.glBindTexture(mTextureTarget, texId);
            GLUtil.checkGlError("glBindTexture " + texId);

            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLUtil.checkGlError("glTexParameter");

            return texId;
        }

        private EGLSurface prepareWindowSurface(Surface surface)
        {
            // Create a window surface, and attach it to the Surface we received.
            int[] surfaceAttribs = {
                    EGL14.EGL_NONE
            };
            EGLSurface surface1;
            surface1 = EGL14.eglCreateWindowSurface(mEGLDisplay, mEGLConfig,surface ,
                    surfaceAttribs, 0);
            checkEglError("eglCreateWindowSurface");
            if (surface1 == null) {
                throw new RuntimeException("surface was null");
            }
            return surface1;
        }

        int changer;
        int tex;
        /*
        Copyright 2014 Google Inc. All rights reserved.
         Borrowed from Grafika project. This is NOT an official Google Project,
         and has an Open Source license.
         https://github.com/google/grafika
         */
        void createSurfaceTexture()
        {
            eglSurface = prepareWindowSurface(camSurfHolder.getSurface());
            makeCurrent(eglSurface);
            mProgramHandle = GLUtil.createProgram(GLUtil.VERTEX_SHADER, GLUtil.FRAGMENT_SHADER_EXT);
            maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
            GLUtil.checkLocation(maPositionLoc, "aPosition");
            maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
            GLUtil.checkLocation(maTextureCoordLoc, "aTextureCoord");
            muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
            GLUtil.checkLocation(muMVPMatrixLoc, "uMVPMatrix");
            muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
            GLUtil.checkLocation(muTexMatrixLoc, "uTexMatrix");
            changer = GLES20.glGetUniformLocation(mProgramHandle, "changer");
            GLUtil.checkLocation(changer, "changer");
            tex = GLES20.glGetUniformLocation(mProgramHandle, "sTexture");
            GLUtil.checkLocation(tex, "sTexture");
            mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
            mTextureId = createGLTextureObject();
            surfaceTexture = new SurfaceTexture(mTextureId);
        }
        /**
         * Copyright 2014 Google Inc. All rights reserved.
         Borrowed from Grafika project. This is NOT an official Google Project,
         and has an Open Source license.
         https://github.com/google/grafika

         * Issues the draw call.  Does the full setup on every call.
         *
         * @param mvpMatrix The 4x4 projection matrix.
         * @param vertexBuffer Buffer with vertex position data.
         * @param firstVertex Index of first vertex to use in vertexBuffer.
         * @param vertexCount Number of vertices in vertexBuffer.
         * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
         * @param vertexStride Width, in bytes, of the position data for each vertex (often
         *        vertexCount * sizeof(float)).
         * @param texMatrix A 4x4 transformation matrix for texture coords.  (Primarily intended
         *        for use with SurfaceTexture.)
         * @param texBuffer Buffer with vertex texture data.
         * @param texStride Width, in bytes, of the texture data for each vertex.
         */
        private void draw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex,
                          int vertexCount, int coordsPerVertex, int vertexStride,
                          float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride) {
            GLUtil.checkGlError("draw start");

            tex = GLES20.glGetUniformLocation(mProgramHandle, "sTexture");
            changer = GLES20.glGetUniformLocation(mProgramHandle, "changer");
            // Select the program.
            GLES20.glUseProgram(mProgramHandle);
            GLUtil.checkGlError("glUseProgram");

            // Set the texture.
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(mTextureTarget, textureId);

            // Copy the model / view / projection matrix over.
            GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
            GLUtil.checkGlError("glUniformMatrix4fv");

            // Copy the texture transformation matrix over.
            GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
            GLUtil.checkGlError("glUniformMatrix4fv");
            GLES20.glUniform1i(tex, 0);
            GLES20.glUniform1f(changer, colorVal);
            // Enable the "aPosition" vertex attribute.
            GLES20.glEnableVertexAttribArray(maPositionLoc);
            GLUtil.checkGlError("glEnableVertexAttribArray");

            // Connect vertexBuffer to "aPosition".
            GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex,
                    GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
            GLUtil.checkGlError("glVertexAttribPointer");

            // Enable the "aTextureCoord" vertex attribute.
            GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
            GLUtil.checkGlError("glEnableVertexAttribArray");

            // Connect texBuffer to "aTextureCoord".
            GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
                    GLES20.GL_FLOAT, false, texStride, texBuffer);
            GLUtil.checkGlError("glVertexAttribPointer");

            // Draw the rect.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
            GLUtil.checkGlError("glDrawArrays");

            // Done -- disable vertex array, texture, and program.
            GLES20.glDisableVertexAttribArray(maPositionLoc);
            GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
            GLES20.glBindTexture(mTextureTarget, 0);
            GLES20.glUseProgram(0);
        }

        /**
         * Allocates a direct float buffer, and populates it with the float array data.
         */
        private FloatBuffer createFloatBuffer(float[] coords) {
            // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
            ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
            bb.order(ByteOrder.nativeOrder());
            FloatBuffer fb = bb.asFloatBuffer();
            fb.put(coords);
            fb.position(0);
            return fb;
        }

        public void setupMediaRecorder(int width, int height, int camcorderProf)
        {
            camcorderProfile = CamcorderProfile.get(camera1.getCameraId(),camcorderProf);
            mediaRecorder = new MediaRecorder();
            try {
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            }
            catch(Exception e){
                if(VERBOSE)Log.e(TAG,"Camera not having a mic oriented in the same way. Use the default microphone");
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            }
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mNextVideoAbsolutePath = getFilePath(true);
            videoFile = new File(mNextVideoAbsolutePath);
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
            try {
                mediaRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            encoderSurface = prepareWindowSurface(mediaRecorder.getSurface());
            mediaContent = new ContentValues();
            mediaContent.put("filename", mNextVideoAbsolutePath);
            mediaContent.put("memoryStorage", (memoryPrefs.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true) ? "1" : "0"));
        }

        public String getFilePath(boolean video) {
            String path;
            SharedPreferences sharedPreferences;
            if(video)
            {
                sharedPreferences = videoFragment.getActivity().getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
            }
            else
            {
                sharedPreferences = photoFragment.getActivity().getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
            }
            if(sharedPreferences.contains(Constants.SAVE_MEDIA_PHONE_MEM))
            {
                if(sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
                    path = fetchPhoneMemoryPath(video);
                }
                else{
                    path = sharedPreferences.getString(Constants.SD_CARD_PATH, "");
                    SimpleDateFormat sdf = new SimpleDateFormat(getResources().getString(R.string.DATE_FORMAT_FOR_FILE));
                    String filename = sdf.format(new Date());
                    if(VERBOSE)Log.d(TAG, "sd card filename = " + filename);
                    if(video){
                        path += getResources().getString(R.string.FC_VID_PREFIX) + filename + getResources().getString(R.string.VID_EXT);
                    }
                    else{
                        path += getResources().getString(R.string.FC_IMG_PREFIX) + filename + getResources().getString(R.string.IMG_EXT);
                    }
                    if(VERBOSE)Log.d(TAG, "SD Card Path = "+path);
                }
            }
            else {
                path = fetchPhoneMemoryPath(video);
            }
            return path;
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
            if (video) {
                phonePath = dcim.getPath() + getResources().getString(R.string.FC_VID_PREFIX) + filename + getResources().getString(R.string.VID_EXT);
            } else {
                phonePath = dcim.getPath() + getResources().getString(R.string.FC_IMG_PREFIX) + filename + getResources().getString(R.string.IMG_EXT);
            }
            if(VERBOSE)Log.d(TAG, "Saving media file at = " + phonePath);
            return phonePath;
        }

        void drawFrame()
        {
            if(mEGLConfig!=null && camera1.isCameraReady()) {
                makeCurrent(eglSurface);
                if(FRAME_VERBOSE) Log.d(TAG,"made current");
                //Get next frame from camera
                surfaceTexture.updateTexImage();
                surfaceTexture.getTransformMatrix(mTmpMatrix);

                //Fill the surfaceview with Camera frame
                int viewWidth = getWidth();
                int viewHeight = getHeight();
                if (frameCount == 0) {
                    if(FRAME_VERBOSE)Log.d(TAG, "FRAME Count = "+frameCount);
                    if(FRAME_VERBOSE)Log.d(TAG,"SV Width == "+viewWidth+", SV Height == "+viewHeight);
                }
                GLES20.glViewport(0, 0, viewWidth, viewHeight);
                draw(IDENTITY_MATRIX, createFloatBuffer(GLUtil.FULL_RECTANGLE_COORDS), 0, (GLUtil.FULL_RECTANGLE_COORDS.length / 2), 2, 2 * SIZEOF_FLOAT, mTmpMatrix,
                        createFloatBuffer(GLUtil.FULL_RECTANGLE_TEX_COORDS), mTextureId, 2 * SIZEOF_FLOAT);

                if(FRAME_VERBOSE)Log.d(TAG, "Draw on screen...."+isRecording);
                //Calls eglSwapBuffers.  Use this to "publish" the current frame.
                EGL14.eglSwapBuffers(mEGLDisplay, eglSurface);

                if(isRecording) {
                    makeCurrent(encoderSurface);
                    if (FRAME_VERBOSE) Log.d(TAG, "Made encoder surface current");
                    if(!portrait) {
                        GLES20.glViewport(0, 0, getRecordVideoWidth(), getRecordVideoHeight());
                    }
                    else{
                        GLES20.glViewport(0, 0, getRecordVideoHeight(), getRecordVideoWidth());
                    }
                    draw(RECORD_IDENTITY_MATRIX, createFloatBuffer(GLUtil.FULL_RECTANGLE_COORDS), 0, (GLUtil.FULL_RECTANGLE_COORDS.length / 2), 2, 2 * SIZEOF_FLOAT, mTmpMatrix,
                            createFloatBuffer(GLUtil.FULL_RECTANGLE_TEX_COORDS), mTextureId, 2 * SIZEOF_FLOAT);
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
                    if(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(Constants.SHOW_MEMORY_CONSUMED_MSG, false)) {
                        mainHandler.sendEmptyMessage(Constants.SHOW_MEMORY_CONSUMED);
                    }
                    if (recordStop == -1) {
                        mediaRecorder.start();
                        recordStop = 1;
                    }
                    updateTimer = true;
                    EGLExt.eglPresentationTimeANDROID(mEGLDisplay, encoderSurface, surfaceTexture.getTimestamp());
                    EGL14.eglSwapBuffers(mEGLDisplay, encoderSurface);
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
                        if(isRecord()){
                            if(FRAME_VERBOSE)Log.d(TAG,"render frame = "+(++frameCnt));
                        }
                        break;
                    case Constants.RECORD_START:
                        cameraRenderer.setupMediaRecorder(getRecordVideoWidth(), getRecordVideoHeight(), getCamProfileForRecord());
                        hour = 0; minute = 0; second = 0;
                        isRecording = true;
                        break;
                    case Constants.RECORD_STOP:
                        isRecording = false;
                        recordStop = -1;
                        recordIncomplete = false;
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
                        if(VERBOSE)Log.d(TAG,"stop isRecording == "+isRecording);
                        if(!recordIncomplete){
                            mainHandler.sendEmptyMessage(Constants.RECORD_COMPLETE);
                        }
                        if(VERBOSE)Log.d(TAG, "Exit recording...");
                        if(VERBOSE)Log.d(TAG,"Orig frame = "+frameCount+" , Rendered frame "+frameCnt);
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
            }
            if(VERBOSE)Log.d(TAG,"VideoTimer STOPPED");
        }
    }
}
