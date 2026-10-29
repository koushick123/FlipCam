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
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.flipcam.CameraActivity;
import com.flipcam.R;
import com.flipcam.VideoFragment;
import com.flipcam.clip.LatestRecordingStore;
import com.flipcam.data.MediaTableConstants;
import android.net.Uri;
import com.flipcam.camerainterface.CameraOperations;
import com.flipcam.cameramanager.Camera1Manager;
import com.flipcam.cameramanager.Camera2Manager;
import com.flipcam.constants.Constants;
import com.flipcam.util.GLUtil;
import com.flipcam.util.CameraSessionRecovery;
import com.flipcam.util.AudioRecordingPolicy;
import com.flipcam.util.AudioRecordingProfiles;
import androidx.appcompat.app.AlertDialog;
import com.flipcam.util.MediaStorage;
import com.flipcam.util.MediaUtil;
import com.flipcam.util.SDCardUtil;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import java.util.Date;


/**
 * Created by Koushick on 15-08-2017.
 */
/*
CameraView is used to display the camera preview and handle video recording operations.
Video resolution and FPS are configured here.
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
    private volatile String selectedAudioConfiguration = "not-prepared";
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
    private volatile boolean finalizingSuspendedRecording;
    private boolean recordingMusicPolicyActive;
    private boolean musicWasMutedBeforeRecording;
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
    // Only the renderer changes recorder state; the UI reads it to gate controls.
    private enum RecorderState { IDLE, PREPARING, PREPARED, RECORDING, PAUSED }
    private volatile RecorderState recorderState = RecorderState.IDLE;
    private volatile boolean pauseActionPending;

    public boolean canChangeRecordingPause() {
        return isRecord() && !pauseActionPending
                && (recorderState == RecorderState.RECORDING || recorderState == RecorderState.PAUSED);
    }

    private void publishRecordingPauseState() {
        mainHandler.post(() -> {
            pauseActionPending = false;
            if (videoFragment != null) {
                videoFragment.updateRecordingPauseState(
                        recorderState == RecorderState.PAUSED, canChangeRecordingPause());
            }
        });
    }

    int frameCount=0;
    volatile int cameraFrameCnt=0;
    volatile int frameCnt=0;
    CameraOperations camera1;
    boolean isFocusModeSupported=false;
    int orientation = -1;
    private volatile int transitionOrientation = -1;
    OrientationEventListener orientationEventListener;
    volatile int hour=0;
    volatile int minute=0;
    volatile int second=0;
    String mNextVideoAbsolutePath=null;
    TextView timeElapsed;
    TextView memoryConsumed;
    //Default display sizes in case windowManager is not able to return screen size.
    int measuredWidth = 800;
    int measuredHeight = 600;
    String focusMode;
    String flashMode;
    ImageButton flashBtn;
    VideoFragment videoFragment;
    private final SensorManager mSensorManager;
    private AudioManager audioManager;
    private final Sensor mAccelerometer;
    float diff[] = new float[3];
    boolean focusNow = false;
    float sensorValues[] = new float[3];
    File videoFile;
    ImageButton stopButton;
    float imageRotationAngle = 0.0f;

    /**
     * Fit the camera image within the available screen area while preserving
     * the selected recording aspect ratio. The black parent area becomes
     * visible letterboxing only when the screen and camera ratios differ.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int availableWidth = MeasureSpec.getSize(widthMeasureSpec);
        int availableHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (availableWidth <= 0 || availableHeight <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int sourceWidth = Math.max(recordVideoWidth, 1);
        int sourceHeight = Math.max(recordVideoHeight, 1);
        // The camera activity keeps a fixed display orientation (nosensor).
        // Rotating the handset changes the encoder orientation, not the axes of
        // this SurfaceView. Using the encoder's portrait flag here swaps the
        // preview bounds at Record and squeezes a landscape scene into a strip.
        float cameraAspectRatio = (float) sourceHeight / sourceWidth;
        float screenAspectRatio = (float) availableWidth / availableHeight;

        int fittedWidth = availableWidth;
        int fittedHeight = availableHeight;
        if (Math.abs(cameraAspectRatio - screenAspectRatio) > 0.001f) {
            if (screenAspectRatio > cameraAspectRatio) {
                fittedWidth = Math.round(availableHeight * cameraAspectRatio);
            } else {
                fittedHeight = Math.round(availableWidth / cameraAspectRatio);
            }
        }
        setMeasuredDimension(fittedWidth, fittedHeight);
    }
    long previousTime = 0;
    long focusPreviousTime=0;
    int previousOrientation = -1;
    SharedPreferences memoryPrefs;
    int lowestThreshold = getResources().getInteger(R.integer.minimumMemoryWarning);
    long lowestMemory = lowestThreshold * (long)Constants.MEGA_BYTE;
    StatFs availableStatFs = new StatFs(Environment.getDataDirectory().getPath());
    ContentValues mediaContent = new ContentValues();
    boolean stopCamera = false;
    int camProfileForRecord;
    int totalRotation;
    volatile boolean recordPaused = false;
    SharedPreferences sharedPreferences;
    CameraView.VideoTimer videoTimer;

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
        mSensorManager = (SensorManager)getContext().getSystemService(Context.SENSOR_SERVICE);
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
            if (!isRecord()) determineOrientation();
            if(rotationAngle == 90 || rotationAngle == 270){
                if(diff[1] > 0.35){
                    if(VERBOSE)Log.d(TAG,"diff y ="+diff[1]);
                    sensorValues[1] = sensorEvent.values[1];
                    focusNow = true;
                }
                else{
                    if(focusNow) {
                        if(VERBOSE)Log.d(TAG, "Focus now in landscape");
                        camera1.setAutoFocus();
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
                    camera1.setAutoFocus();
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
            if (camView == null || !camView.isAttachedToWindow()
                    || videoFragment == null || videoFragment.getCameraView() != camView
                    || videoFragment.getView() == null || videoFragment.getActivity() == null
                    || videoFragment.getActivity().isFinishing()
                    || videoFragment.getActivity().isDestroyed()) return;
            switch(msg.what)
            {
                case Constants.SHOW_ELAPSED_TIME:
                    if(FRAME_VERBOSE)Log.d(TAG,"show time now");
                    showTimeElapsed();
                    break;
                case Constants.SHOW_PAUSE_TEXT:
                    showPauseText();
                    break;
                case Constants.HIDE_PAUSE_TEXT:
                    if(VERBOSE)Log.d(TAG, "Hide Pause text");
                    hidePauseText();
                    break;
                case Constants.SHOW_MEMORY_CONSUMED:
                    showMemoryConsumed();
                    break;
                case Constants.RECORD_COMPLETE:
                    mediaContent = null;
                    if(VERBOSE)Log.d(TAG,"Update thumbnail now");
                    if (!isRecord()) videoFragment.createAndShowThumbnail(msg.obj instanceof String ? (String) msg.obj : getMediaPath());
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
                    videoFragment.onRecordingStopped();
                    break;
                case Constants.RECORD_STOPPED:
                    if (videoFragment != null && !isRecord() && recorderState == RecorderState.IDLE) {
                        // Rebuild the idle preview controls after recorder and
                        // surface transitions have completed. The synchronous
                        // stop path can run before SurfaceView relayout finishes.
                        videoFragment.showRecordAndThumbnail();
                        videoFragment.onRecordingStopped();
                    }
                    break;
            }
        }
    }

    /** UI callbacks belong to this view, not a later fragment view. */
    public void clearUiCallbacks() {
        stopForNavigation();
        if (mainHandler != null) mainHandler.removeCallbacksAndMessages(null);
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
        // Ignore callbacks already queued by a disconnected camera producer.
        if (surfaceTexture != GLUtil.getSurfaceTexture() || cameraHandler == null) return;
        if(FRAME_VERBOSE)Log.d(TAG,"FRAME Available now");
        if(FRAME_VERBOSE)Log.d(TAG,"is Record = "+isRecord());
        cameraHandler.sendEmptyMessage(Constants.FRAME_AVAILABLE);
    }

    public String getMediaPath(){
        return mNextVideoAbsolutePath;
    }

    public boolean zoomInAndOut(int progress)
    {
        return camera1.zoomInOrOut(progress);
    }

    public CameraOperations getCameraImplementation(){
        return camera1;
    }

    public List<String> getSupportedVideoResolutions() {
        if (camera1 instanceof Camera1Manager) {
            return ((Camera1Manager) camera1).getSupportedVideoResolutions();
        }
        Set<String> resolutions = sharedPreferences.getStringSet(Constants.SUPPORT_VIDEO_RESOLUTIONS, null);
        return resolutions == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(resolutions);
    }

    public void applyVideoResolution() {
        if (camera1 == null || !camera1.isCameraReady() || isRecord()) return;
        camera1.setResolution();
        if (!isCamera2()) setLayoutAspectRatio();
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
        if (camera1 == null || !camera1.isCameraReady()) return false;
        String currentFlashMode = camera1.getFlashMode();
        return currentFlashMode != null
                && !currentFlashMode.equalsIgnoreCase(camera1.getFlashModeOff());
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

    public void showTimeElapsed()
    {
        if(FRAME_VERBOSE)Log.d(TAG,"displaying time = "+second);
        StringBuilder showSec = new StringBuilder("0");
        StringBuilder showMin = new StringBuilder("0");
        StringBuilder showHr = new StringBuilder("0");
        if(second < 10){
            showSec.append(second);
        }
        else{
            showSec.delete(0, showSec.length());
            showSec.append(second);
        }

        if(minute < 10){
            showMin.append(minute);
        }
        else{
            showMin.delete(0, showMin.length());
            showMin.append(minute);
        }

        if(hour < 10){
            showHr.append(hour);
        }
        else{
            showHr.delete(0, showHr.length());
            showHr.append(hour);
        }
        timeElapsed.setText(showHr + ":" + showMin + ":" + showSec);
        //This is added for blinking the timer if recording is paused.
        timeElapsed.setVisibility(View.VISIBLE);
    }

    public void showPauseText(){
        this.videoFragment.getPauseText().setVisibility(View.VISIBLE);
    }

    public void hidePauseText(){
        this.videoFragment.getPauseText().setVisibility(View.INVISIBLE);
    }

    public boolean isPauseTextVisible(){
        return this.videoFragment.getPauseText().getVisibility() == View.VISIBLE;
    }

    public void showMemoryConsumed()
    {
        memoryConsumed.setText(MediaUtil.convertMemoryForDisplay(videoFile.length()));
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

    private volatile boolean cameraTransitionInProgress;
    private AlertDialog cameraFailureDialog;
    private Toast switchFailureToast;
    private boolean switchPreviousFacing;
    private boolean switchRequestedFacing;
    private volatile boolean switchMayRollback;
    private volatile boolean switchSourceReady = true;

    private void showSwitchFailure(boolean requestedBackFacing) {
        mainHandler.post(() -> {
            if (switchFailureToast != null) switchFailureToast.cancel();
            switchFailureToast = Toast.makeText(getContext(), requestedBackFacing
                    ? R.string.cameraSwitchRearFailed : R.string.cameraSwitchFrontFailed,
                    Toast.LENGTH_LONG);
            switchFailureToast.show();
        });
    }

    /** A camera can open successfully yet never deliver frames. Roll back once. */
    private void restoreCameraAfterMissingFrames() {
        if (getWindowToken() == null || cameraTransitionInProgress) return;
        cameraTransitionInProgress = true;
        try {
            CameraSessionRecovery.Result result = CameraSessionRecovery.open(switchPreviousFacing,
                    cameraSession(switchPreviousFacing,
                            sharedPreferences.getString(Constants.SELECT_VIDEO_RESOLUTION, null)));
            backCamera = result.backFacing;
            showSwitchFailure(switchRequestedFacing);
            if (!result.available) cameraUnavailable(result.failure);
        } finally {
            cameraTransitionInProgress = false;
            if (cameraHandler != null && camera1.isCameraReady()) {
                cameraHandler.sendEmptyMessage(Constants.FRAME_AVAILABLE);
            }
        }
    }

    public void switchCamera() {
        if (cameraTransitionInProgress) return;
        CameraRenderer activeRenderer = cameraHandler == null ? null : cameraHandler.getCameraRendererInstance();
        if (activeRenderer != null && activeRenderer.switchAnimationActive) return;
        final boolean previousFacing = backCamera;
        final String previousResolution = sharedPreferences.getString(Constants.SELECT_VIDEO_RESOLUTION, null);
        switchPreviousFacing = previousFacing;
        switchRequestedFacing = !previousFacing;
        switchMayRollback = true;
        cameraTransitionInProgress = true;
        if (cameraHandler != null) {
            cameraHandler.post(() -> {
                CameraRenderer renderer = cameraHandler.getCameraRendererInstance();
                if (renderer != null) renderer.beginSwitchAnimation();
            });
        }
        try {
            unregisterAccelSensor();
            CameraSessionRecovery.Result result = CameraSessionRecovery.switchFacing(previousFacing,
                    cameraSession(previousFacing, previousResolution));
            backCamera = result.backFacing;
            if (result.restored || !result.available) {
                switchMayRollback = false;
                showSwitchFailure(!previousFacing);
            }
            if (!result.available) cameraUnavailable(result.failure);
            else {
                dismissCameraFailure();
                if (result.restored) {
                    Log.w(TAG, "Camera switch failed; original camera restored", result.failure);
                }
            }
        } finally {
            cameraTransitionInProgress = false;
            // Drain a queued producer buffer only after drawing is allowed again.
            // A full SurfaceTexture queue can otherwise suppress the next notification
            // when Retry reopens a camera without a running switch animation.
            if (cameraHandler != null && camera1.isCameraReady()) {
                cameraHandler.sendEmptyMessage(Constants.FRAME_AVAILABLE);
            }
        }
    }

    private CameraSessionRecovery.Session cameraSession(boolean previousFacing, String previousResolution) {
        return new CameraSessionRecovery.Session() {
            @Override public void close() {
                stopAndReleaseCamera();
                replaceClosedCameraSource();
            }
            @Override public boolean isReady() { return camera1.isCameraReady(); }
            @Override public void open(boolean facing) {
                if (!switchSourceReady) throw new IllegalStateException("New camera texture could not be prepared");
                backCamera = facing;
                if (facing == previousFacing && previousResolution != null) {
                    sharedPreferences.edit().putString(Constants.SELECT_VIDEO_RESOLUTION, previousResolution).apply();
                }
                setSwitch(true);
                isFocusModeSupported = false;
                configureCameraAndStartPreview();
                // Drain a pending texture frame that may have arrived while the camera
                // was closed; otherwise a failed teardown can stall frame notifications.
                if (cameraHandler != null) cameraHandler.sendEmptyMessage(Constants.FRAME_AVAILABLE);
            }
        };
    }

    /** Give the next producer its own queue; keep the frozen 2D snapshot untouched. */
    private void replaceClosedCameraSource() {
        CameraRenderer.CameraHandler handler = cameraHandler;
        CameraRenderer renderer = handler == null ? null : handler.getCameraRendererInstance();
        if (renderer == null || (!switchMayRollback && !renderer.switchAnimationActive)) {
            switchSourceReady = true;
            return;
        }
        switchSourceReady = false;
        java.util.concurrent.CountDownLatch drained = new java.util.concurrent.CountDownLatch(1);
        Runnable work = () -> {
            try {
                // A new/lifecycle-restored renderer has no frozen frame to preserve.
                // Let its normal startup path attach the producer instead.
                if (!renderer.switchAnimationActive) {
                    switchSourceReady = true;
                    return;
                }
                if (GLUtil.getmEGLConfig() != null && GLUtil.getSurfaceTexture() != null) {
                    GLUtil.makeCurrent(GLUtil.getEglSurface());
                    GLUtil.replaceCameraSurfaceTexture();
                    GLUtil.getSurfaceTexture().setOnFrameAvailableListener(CameraView.this);
                    renderer.lastCameraTimestampNs = 0;
                    renderer.pendingCameraFrame = false;
                    switchSourceReady = true;
                }
            } catch (RuntimeException error) {
                Log.w(TAG, "Unable to prepare camera texture", error);
            } finally {
                drained.countDown();
            }
        };
        if (android.os.Looper.myLooper() == handler.getLooper()) work.run();
        else if (handler.post(work)) {
            try {
                if (!drained.await(1, java.util.concurrent.TimeUnit.SECONDS)) {
                    Log.w(TAG, "Timed out preparing camera texture");
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void dismissCameraFailure() {
        if (cameraFailureDialog != null) {
            cameraFailureDialog.dismiss();
            cameraFailureDialog = null;
        }
    }

    private void cameraUnavailable(RuntimeException failure) {
        Log.e(TAG, "Camera unavailable after recovery", failure);
        // Preserve the recorder/output long enough to finalize an existing recording.
        if (isRecord()) {
            setRecord(false);
            setKeepScreenOn(false);
            stopTimerThread();
            if (orientationEventListener != null) orientationEventListener.enable();
            System.arraycopy(IDENTITY_MATRIX, 0, RECORD_IDENTITY_MATRIX, 0, IDENTITY_MATRIX.length);
            rotationAngle = 0f;
            hidePauseText();
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            cameraHandler.removeMessages(Constants.RECORD_PAUSE);
            cameraHandler.removeMessages(Constants.RECORD_RESUME);
            cameraHandler.sendEmptyMessage(Constants.RECORD_STOP);
        }
        mainHandler.post(() -> {
            if (getWindowToken() == null || !getHolder().getSurface().isValid()
                    || (cameraFailureDialog != null && cameraFailureDialog.isShowing())) return;
            cameraFailureDialog = new AlertDialog.Builder(getContext())
                    .setTitle(R.string.cameraUnavailableTitle)
                    .setMessage(R.string.cameraUnavailableMessage)
                    .setCancelable(false)
                    .setPositiveButton(R.string.cameraRetry, (dialog, which) -> {
                        cameraFailureDialog = null;
                        openCameraAndStartPreview();
                    })
                    .setNegativeButton(R.string.cameraClose, (dialog, which) -> {
                        cameraFailureDialog = null;
                        if (getContext() instanceof android.app.Activity) {
                            ((android.app.Activity) getContext()).finish();
                        }
                    }).show();
        });
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

    public boolean isBackCamera() {
        return backCamera;
    }

    public void setBackCamera(boolean backCamera) {
        this.backCamera = backCamera;
    }

    public boolean isCameraPermissionAvailable(){
        int camerapermission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA);
        int audiopermission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO);
        if (camerapermission != PackageManager.PERMISSION_GRANTED || audiopermission != PackageManager.PERMISSION_GRANTED ||
                (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ContextCompat.checkSelfPermission(
                        getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            return false;
        }
        else{
            return true;
        }
    }

    //This method sets all the camera parameters and opens the camera. Same is called when you switch the camera.
    public void openCameraAndStartPreview() {
        if (cameraTransitionInProgress) return;
        cameraTransitionInProgress = true;
        try {
            CameraSessionRecovery.Result result = CameraSessionRecovery.open(backCamera,
                    cameraSession(backCamera, sharedPreferences.getString(Constants.SELECT_VIDEO_RESOLUTION, null)));
            if (!result.available) cameraUnavailable(result.failure);
            else dismissCameraFailure();
        } finally {
            cameraTransitionInProgress = false;
            // Drain a queued producer buffer only after drawing is allowed again.
            // A full SurfaceTexture queue can otherwise suppress the next notification
            // when Retry reopens a camera without a running switch animation.
            if (cameraHandler != null && camera1.isCameraReady()) {
                cameraHandler.sendEmptyMessage(Constants.FRAME_AVAILABLE);
            }
        }
    }

    private void configureCameraAndStartPreview()
    {
        if(!isCameraPermissionAvailable()){
            if(VERBOSE)Log.d(TAG,"Permission turned off mostly at settings");
            throw new SecurityException("Camera or microphone permission is unavailable");
        }
        camera1.setSurfaceView(this);
        setStopCamera(false);
        camera1.openCamera(backCamera, getContext());
        if(!isCamera2()) {
            if(!camera1.isCameraReady()){
                Toast.makeText(getContext(),getResources().getString(R.string.noFrontFaceCamera),Toast.LENGTH_SHORT).show();
                return;
            }
            if(VERBOSE)Log.d(TAG, "call getSupportedVideoSizes");
            camera1.getSupportedVideoSizes();
            //Set the video resolution as per selection in settings.
            camera1.setResolution();
            if(!Build.MODEL.contains(getResources().getString(R.string.nokia71))) {
                camera1.setFPS();
            }
            else{
                //Nokia 7.1 front camera shows slow refresh if FPS is set, so keep the existing rear-camera behavior.
                camera1.setFPS();
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
            }
        }
        int flashWidth = (int) getResources().getDimension(R.dimen.flashOnWidth);
        int flashHeight = (int) getResources().getDimension(R.dimen.flashOnHeight);
        flashBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (flashBtn.getParent() instanceof FrameLayout) {
            // The preview controls live in equal-width FrameLayout slots.
            flashBtn.setLayoutParams(new FrameLayout.LayoutParams(
                    flashWidth, flashHeight, Gravity.CENTER));
        } else {
            // Keep the legacy placement when the flash button is in a LinearLayout.
            LinearLayout.LayoutParams flashParams = new LinearLayout.LayoutParams(flashWidth, flashHeight);
            if(isRecord()){
                flashParams.setMargins(0,(int)getResources().getDimension(R.dimen.flashOnLeftMargin),0,0);
                flashParams.weight=0.3f;
            }
            else{
                flashParams.setMargins((int)getResources().getDimension(R.dimen.flashOnLeftMargin),0,0,0);
                flashParams.weight=0.5f;
                flashParams.gravity= Gravity.CENTER;
            }
            flashBtn.setLayoutParams(flashParams);
        }
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
            if (this.videoFragment.isFlashOn()) {
                flashMode = camera1.getFlashModeTorch();
            } else {
                flashMode = camera1.getFlashModeOff();
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
            this.videoFragment.setFlashOn(false);
            return true;
        }
    }

    public synchronized void beginRecordingAudioPolicy() {
        if (recordingMusicPolicyActive) return;
        musicWasMutedBeforeRecording = audioManager.isStreamMute(AudioManager.STREAM_MUSIC);
        recordingMusicPolicyActive = true;
        if (!musicWasMutedBeforeRecording) audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
    }

    public synchronized void endRecordingAudioPolicy() {
        if (!recordingMusicPolicyActive) return;
        recordingMusicPolicyActive = false;
        if (!musicWasMutedBeforeRecording) audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
    }

    public boolean canStartRecording() {
        return recorderState == RecorderState.IDLE && !finalizingSuspendedRecording;
    }

    private boolean hasRecorderThread() {
        return cameraHandler != null && cameraHandler.getLooper().getThread().isAlive();
    }

    public boolean isCallInterruption() {
        int mode = audioManager.getMode();
        return mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION;
    }

    public void stopForNavigation() {
        if (!isRecord() || finalizingSuspendedRecording) return;
        if (hasRecorderThread()) record(false);
        else finalizeSuspendedRecording();
    }

    /** A paused recorder can outlive its preview thread, but never a destroyed view. */
    private synchronized void finalizeSuspendedRecording() {
        if (finalizingSuspendedRecording || mediaRecorder == null) return;
        finalizingSuspendedRecording = true;
        endRecordingAudioPolicy();
        setRecord(false);
        stopTimerThread();
        final MediaRecorder suspended = mediaRecorder;
        final File saved = videoFile;
        new Thread(() -> {
            boolean valid = false;
            try { suspended.stop(); valid = saved != null && saved.isFile() && saved.length() > 0; }
            catch (RuntimeException failure) { Log.e(TAG, "Unable to finalize suspended recording", failure); }
            finally {
                try { suspended.release(); } catch (RuntimeException failure) { Log.w(TAG, "Recorder release failed", failure); }
                if (mediaRecorder == suspended) mediaRecorder = null;
                recorderState = RecorderState.IDLE;
                setRecordPaused(false);
                if (valid) scanSavedRecording();
                finalizingSuspendedRecording = false;
                if (mainHandler != null) {
                    if (valid) mainHandler.obtainMessage(Constants.RECORD_COMPLETE, saved.getAbsolutePath()).sendToTarget();
                    mainHandler.sendEmptyMessage(Constants.RECORD_STOPPED);
                }
            }
        }, "FlipCam-finalize-paused").start();
    }

    public void startRecording() {
        if (!isRecord() && canStartRecording()) record(false);
    }

    public void stopRecording(boolean noSDCard) {
        if (isRecord()) record(noSDCard);
    }

    public void record(boolean noSDCard)
    {
        if (finalizingSuspendedRecording) return;
        if (isRecord() && !hasRecorderThread()) { finalizeSuspendedRecording(); return; }
        if(!isRecord()) {
            if (!canStartRecording()) return;
            // A canceled queued start must never refer to the last saved file.
            videoFile = null;
            mNextVideoAbsolutePath = null;
            determineOrientation();
            if(VERBOSE)Log.d(TAG,"Rot angle == "+rotationAngle+", portrait = "+portrait);
            Matrix.rotateM(RECORD_IDENTITY_MATRIX, 0, rotationAngle , 0, 0, 1);
            // Mark recording before changing the SurfaceView layout. The ensuing
            // surfaceChanged callback must retain the stop/pause controls and
            // immersive recording UI instead of rebuilding the preview controls.
            setRecord(true);
            setLayoutAspectRatio();
            setKeepScreenOn(true);
            videoTimer = startTimerThread();
            cameraHandler.sendEmptyMessage(Constants.RECORD_START);
            // Keep observing posture for switch animation; the recording matrix stays locked.
            camera1.setRecordingHint();
        }
        else{
            cameraHandler.removeMessages(Constants.RECORD_START);
            cameraHandler.removeMessages(Constants.RECORD_PAUSE);
            cameraHandler.removeMessages(Constants.RECORD_RESUME);
            endRecordingAudioPolicy();
            setRecord(false);
            // Return to the selected video aspect ratio for the idle preview.
            // Recording uses the full available display so the live camera
            // image covers the screen behind the recording controls.
            setLayoutAspectRatio();
            setKeepScreenOn(false);
            stopTimerThread();
            hidePauseText();
            if(noSDCard) {
                cameraHandler.sendMessageAtFrontOfQueue(cameraHandler.obtainMessage(Constants.RECORD_STOP_NO_SD_CARD));
            }
            else{
                cameraHandler.sendMessageAtFrontOfQueue(cameraHandler.obtainMessage(Constants.RECORD_STOP));
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
        if (!canChangeRecordingPause() || recorderState != RecorderState.RECORDING) return;
        pauseActionPending = true;
        videoFragment.updateRecordingPauseState(false, false);
        cameraHandler.sendEmptyMessage(Constants.RECORD_PAUSE);
    }

    public void recordResume(){
        if (!hasWindowFocus() || isCallInterruption() || !camera1.isCameraReady()) return;
        if (!canChangeRecordingPause() || recorderState != RecorderState.PAUSED) return;
        pauseActionPending = true;
        videoFragment.updateRecordingPauseState(true, false);
        cameraHandler.sendEmptyMessage(Constants.RECORD_RESUME);
    }

    //This method calculates video orientation as the device rotates between portrait and landscape.
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
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            // Keep the view bounded by the screen in both preview and
            // recording. onMeasure fits it to the selected camera ratio.
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            setLayoutParams(layoutParams);
        }
        requestLayout();
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

    private void stopAndReleaseCamera() {
        // A failing stop must never skip release or leave a partially opened camera behind.
        try { camera1.stopPreview(); }
        catch (RuntimeException error) { Log.w(TAG, "Unable to stop camera preview", error); }
        try { camera1.releaseCamera(); }
        catch (RuntimeException error) { Log.w(TAG, "Unable to release camera", error); }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if(VERBOSE)Log.d(TAG, "surfCreated holder = " + surfaceHolder);
        camSurfHolder = surfaceHolder;
        mainHandler = new MainHandler(this);
        GLUtil.prepareEGLDisplayandContext();
        isReady = false;
        CameraRenderer cameraRenderer = new CameraRenderer();
        cameraRenderer.start();
        waitUntilReady();
        orientationEventListener = new OrientationEventListener(getContext(), SensorManager.SENSOR_DELAY_UI){
            @Override
            public void onOrientationChanged(int i) {
                if (i != OrientationEventListener.ORIENTATION_UNKNOWN) transitionOrientation = i;
                // Sensor updates during capture must not change recording orientation.
                if (isRecord()) return;
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
            // Recording changes the SurfaceView size. Keep the recording overlay
            // and immersive navigation state instead of rebuilding preview controls.
            if (!isRecord()) this.videoFragment.showRecordAndThumbnail();
            // Do not inspect the newest media file on SurfaceView resizes. The
            // newest file may be the MP4 currently being written, and reading
            // its incomplete container can interfere with MediaRecorder.stop().
            camera1.setVideoFragmentInstance(this.videoFragment);
        }
        if(!camera1.isCameraReady()) {
            if(VERBOSE)Log.d(TAG, "camera NOT READY. Must Open");
            measuredWidth = width;
            measuredHeight = height;
            frameCount=0;
            openCameraAndStartPreview();
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
        dismissCameraFailure();
        if (cameraHandler != null) {
            CameraRenderer renderer = cameraHandler.getCameraRendererInstance();
            if (renderer != null) renderer.cancelSwitchAnimation();
        }
        if(VERBOSE)Log.d(TAG,"surfaceDestroyed = "+surfaceHolder);
        if(VERBOSE)Log.d(TAG,"cameraHandler = "+cameraHandler);
        orientationEventListener.disable();
        mSensorManager.unregisterListener(this);
        if(cameraHandler!=null) {
            //Ensure camera's resources are properly released.
            CameraRenderer cameraRenderer = cameraHandler.getCameraRendererInstance();
            if (isRecord()) {
                //If video recording was in progress, ensure recording is stopped and saved, before exiting.
                if(VERBOSE)Log.d(TAG, "Unmute audio");
                endRecordingAudioPolicy();
                if(VERBOSE)Log.d(TAG,"Recording in progress.... Stop now");
                setRecord(false);
                //Reset the RECORD Matrix GLUtil.getSurfaceTexture()to be portrait.
                System.arraycopy(IDENTITY_MATRIX,0,RECORD_IDENTITY_MATRIX,0,IDENTITY_MATRIX.length);
                //Reset Rotation angle
                rotationAngle = 0f;
                hidePauseText();
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
                        if (videoFragment != null && videoFragment.getActivity() instanceof CameraActivity) {
                            ((CameraActivity) videoFragment.getActivity()).updateStorageDestinationLabel();
                        }
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
            // Finalize the recorder on its owning thread before removing its video
            // producer or EGL resources. Audio must not outlive video teardown.
            if(camera1.isCameraReady()) {
                //Switch off flash light if used during recording.
                if(isCamera2()) {
                    setStopCamera(true);
                    camera1.setFlashOnOff(false);
                }
                else {
                    stopAndReleaseCamera();
                }
            }
            if(VERBOSE)Log.d(TAG, "stopped and released Camera");
            cameraHandler.removeMessages(Constants.FRAME_AVAILABLE);
            GLUtil.makeCurrent(GLUtil.getEglSurface());
            if(GLUtil.getSurfaceTexture()!=null){
                GLUtil.getSurfaceTexture().setOnFrameAvailableListener(null);
                GLUtil.getSurfaceTexture().release();
                GLUtil.setSurfaceTexture(null);
            }
            frameCount=0;
            GLUtil.releaseProgram();
            GLUtil.releaseEGLSurface();
            GLUtil.releaseEGLContext();
        }
    }
    /** Publish only finalized recordings, independently of thumbnail/UI lifecycle. */
    private void scanSavedRecording() {
        final File savedVideo = videoFile;
        if (savedVideo == null || !savedVideo.isFile() || savedVideo.length() == 0) return;
        Context appContext = getContext().getApplicationContext();
        try {
            LatestRecordingStore.recorded(appContext, savedVideo);
            ContentValues saved = new ContentValues();
            saved.put("filename", savedVideo.getAbsolutePath());
            saved.put("memoryStorage", memoryPrefs.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true) ? "1" : "0");
            appContext.getContentResolver().insert(
                    Uri.parse(MediaTableConstants.BASE_CONTENT_URI + "/addMedia"), saved);
        } catch (RuntimeException publicationError) {
            Log.w(TAG, "Unable to publish finalized recording", publicationError);
        }
        try {
            MediaScannerConnection.scanFile(getContext().getApplicationContext(),
                    new String[]{savedVideo.getAbsolutePath()}, new String[]{"video/mp4"},
                    (path, uri) -> {
                        if (uri == null) Log.w(TAG, "Recording media scan returned no URI: " + path);
                        else Log.d(TAG, "Recording indexed: " + path + " -> " + uri);
                    });
        } catch (RuntimeException scanError) {
            // Indexing failure must never interrupt a successfully saved recording.
            Log.w(TAG, "Unable to request recording media scan", scanError);
        }
    }

    //This is the class that draws the each camera frame onto the device's screen. It uses OpenGL API to draw the preview frame.
    //It also controls the video recording.
    class CameraRenderer extends Thread
    {
        int recordStop = -1;
        boolean isRecording = false;
        int viewWidth = 0;
        int viewHeight = 0;
        private boolean hasCameraFrame;
        private boolean pendingCameraFrame;
        private long lastCameraTimestampNs;
        private volatile boolean switchAnimationActive;
        private long switchStartedNs;
        private boolean switchLandscape;
        private static final long SWITCH_FLIP_NS = 440_000_000L;
        private long newCameraFrameNs;
        private long lastSwitchRenderNs;
        private int switchWarmupFrames;
        private static final long SWITCH_BLUR_NS = 120_000_000L;
        private static final long SWITCH_REVEAL_NS = 220_000_000L;
        private static final long SWITCH_FRAME_NS = 33_333_333L;
        private final Runnable switchAnimationTick = new Runnable() {
            @Override public void run() {
                if (!switchAnimationActive) return;
                drawFrame(true);
                if (switchAnimationActive) cameraHandler.postDelayed(this, 16);
            }
        };

        void beginSwitchAnimation() {
            if (!hasCameraFrame || GLUtil.getmEGLConfig() == null) return;
            cameraHandler.removeCallbacks(switchAnimationTick);
            GLUtil.makeCurrent(GLUtil.getEglSurface());
            if (!GLUtil.freezeCameraFrame(Math.max(1, getWidth()), Math.max(1, getHeight()), mTmpMatrix)) {
                Log.w(TAG, "Snapshot unavailable; retaining the last camera texture during switch");
            }
            switchAnimationActive = true;
            switchStartedNs = System.nanoTime();
            int posture = transitionOrientation;
            if (posture < 0) posture = portrait ? 0 : 90;
            int quadrant = ((posture + 45) / 90) % 4;
            switchLandscape = quadrant == 1 || quadrant == 3;
            newCameraFrameNs = 0;
            lastSwitchRenderNs = 0;
            switchWarmupFrames = 0;
            pendingCameraFrame = false;
            cameraHandler.post(switchAnimationTick);
        }

        void cancelSwitchAnimation() {
            switchAnimationActive = false;
            switchMayRollback = false;
            switchSourceReady = true;
            cameraHandler.removeCallbacks(switchAnimationTick);
            GLUtil.setFrameOpacity(1f);
            GLUtil.endFrameTransition();
        }

        public CameraRenderer()
        {
            //Empty constructor
        }

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

        public void setupMediaRecorder(int width, int height, int camcorderProf) {
            // A failed new preparation must never delete a prior saved recording.
            videoFile = null;
            mNextVideoAbsolutePath = null;
            selectedAudioConfiguration = "not-prepared";
            camcorderProfile = CamcorderProfile.get(camera1.getCameraId(), camcorderProf);
            boolean noAudio = sharedPreferences.getBoolean(Constants.NO_AUDIO_MSG, false);
            try {
                File requested = new File(getFilePath());
                File reserved = requested;
                while (!reserved.createNewFile()) {
                    String name = requested.getName();
                    int dot = name.lastIndexOf('.');
                    reserved = new File(requested.getParentFile(), name.substring(0, dot)
                            + "-" + java.util.UUID.randomUUID() + name.substring(dot));
                }
                videoFile = reserved;
                mNextVideoAbsolutePath = reserved.getAbsolutePath();
            } catch (IOException failure) {
                throw new IllegalStateException("Unable to reserve recording output", failure);
            }
            final int encodedWidth = portrait ? height : width;
            final int encodedHeight = portrait ? width : height;
            if (noAudio) {
                try {
                    mediaRecorder = prepareRecorder(null, encodedWidth, encodedHeight);
                    selectedAudioConfiguration = "microphone-disabled";
                } catch (IOException failure) {
                    throw new IllegalStateException("Unable to prepare muted recorder", failure);
                }
            } else {
                AudioRecordingPolicy.Prepared<MediaRecorder> prepared = AudioRecordingPolicy.prepare(
                        AudioRecordingPolicy.candidates(camcorderProfile.audioCodec,
                                camcorderProfile.audioSampleRate, camcorderProfile.audioBitRate,
                                camcorderProfile.audioChannels),
                        new AudioRecordingPolicy.Attempt<MediaRecorder>() {
                            @Override public boolean supports(AudioRecordingPolicy.Config config) {
                                return AudioRecordingProfiles.supports(config);
                            }
                            @Override public MediaRecorder prepare(AudioRecordingPolicy.Config config) throws IOException {
                                return prepareRecorder(config, encodedWidth, encodedHeight);
                            }
                        });
                mediaRecorder = prepared.recorder;
                selectedAudioConfiguration = prepared.config.toString();
            }
            Log.i(TAG, "Prepared recording audio: " + selectedAudioConfiguration);
            recorderState = RecorderState.PREPARED;
            encoderSurface = GLUtil.prepareWindowSurface(mediaRecorder.getSurface(), GLUtil.getmEGLDisplay(), GLUtil.getmEGLConfig());
            mediaContent = new ContentValues();
            mediaContent.put("filename", mNextVideoAbsolutePath);
            mediaContent.put("memoryStorage", memoryPrefs.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true) ? "1" : "0");
        }

        private MediaRecorder prepareRecorder(AudioRecordingPolicy.Config audio, int width, int height) throws IOException {
            MediaRecorder candidate = new MediaRecorder();
            try {
                if (audio != null) {
                    try {
                        candidate.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                    } catch (RuntimeException unavailable) {
                        candidate.setAudioSource(MediaRecorder.AudioSource.MIC);
                    }
                }
                candidate.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                candidate.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                // Every attempt uses only the fresh output reserved by this recording.
                candidate.setOutputFile(mNextVideoAbsolutePath);
                candidate.setVideoEncodingBitRate(camcorderProfile.videoBitRate);
                candidate.setVideoFrameRate(camcorderProfile.videoFrameRate);
                candidate.setVideoSize(width, height);
                candidate.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                if (audio != null) {
                    candidate.setAudioEncoder(audio.encoder);
                    candidate.setAudioEncodingBitRate(audio.bitRate);
                    candidate.setAudioSamplingRate(audio.sampleRate);
                    candidate.setAudioChannels(audio.channels);
                }
                candidate.prepare();
                return candidate;
            } catch (IOException | RuntimeException rejected) {
                try { candidate.release(); }
                catch (RuntimeException cleanup) {
                    rejected.addSuppressed(cleanup);
                    throw new AudioRecordingPolicy.CleanupFailure(rejected);
                }
                Log.w(TAG, "Recorder configuration rejected: " + audio, rejected);
                throw rejected;
            }
        }

        private void releaseEncoderSurface() {
            if (encoderSurface != null && encoderSurface != EGL14.EGL_NO_SURFACE) {
                if (GLUtil.getEglSurface() != null
                        && GLUtil.getEglSurface() != EGL14.EGL_NO_SURFACE) {
                    GLUtil.makeCurrent(GLUtil.getEglSurface());
                }
                EGL14.eglDestroySurface(GLUtil.getmEGLDisplay(), encoderSurface);
                encoderSurface = EGL14.EGL_NO_SURFACE;
            }
        }

        public String getFilePath() {
            StringBuilder path = new StringBuilder(Constants.EMPTY);
            SharedPreferences sharedPreferences = videoFragment.getActivity().getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
            if(sharedPreferences.contains(Constants.SAVE_MEDIA_PHONE_MEM))
            {
                if(sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
                    path.append(fetchPhoneMemoryPath());
                }
                else{
                    path.append(sharedPreferences.getString(Constants.SD_CARD_PATH, ""));
                    SimpleDateFormat sdf = new SimpleDateFormat(getResources().getString(R.string.DATE_FORMAT_FOR_FILE));
                    String filename = sdf.format(new Date());
                    if(VERBOSE)Log.d(TAG, "sd card filename = " + filename);
                    path.append(getResources().getString(R.string.PATH_SLASH));
                    path.append(getResources().getString(R.string.FC_VID_PREFIX) + filename + getResources().getString(R.string.VID_EXT));
                    if(VERBOSE)Log.d(TAG, "SD Card Path = "+path);
                }
            }
            else {
                path.append(fetchPhoneMemoryPath());
            }
            return path.toString();
        }
        String defaultMediaPath = MediaStorage.getPhoneMediaDirectory(videoFragment.getApplicationContext()).getPath();
        public String fetchPhoneMemoryPath(){
            StringBuilder phonePath = new StringBuilder("");
            File dcim;
            Context context = videoFragment.getApplicationContext();
            String phoneDirectory = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ? defaultMediaPath
                    : PreferenceManager.getDefaultSharedPreferences(context)
                            .getString(Constants.MEDIA_FILE_PATH, defaultMediaPath);
            dcim = new File(phoneDirectory);
            //Included check for default path only.
            if (!dcim.exists()) {
                dcim.mkdirs();
            }
            SimpleDateFormat sdf = new SimpleDateFormat(getResources().getString(R.string.DATE_FORMAT_FOR_FILE));
            String filename = sdf.format(new Date());
            if(VERBOSE)Log.d(TAG, "filename = " + filename);
            phonePath.append(dcim.getPath());
            phonePath.append(getResources().getString(R.string.PATH_SLASH));
            phonePath.append(getResources().getString(R.string.FC_VID_PREFIX) + filename + getResources().getString(R.string.VID_EXT));
            if(VERBOSE)Log.d(TAG, "Saving media file at = " + phonePath);
            return phonePath.toString();
        }

        void drawFrame() { drawFrame(false); }

        private void drawFrame(boolean animationTick)
        {
            long nowNs = System.nanoTime();
            boolean ready = !cameraTransitionInProgress && camera1.isCameraReady();
            if (!animationTick) pendingCameraFrame = true;
            if (switchAnimationActive && !cameraTransitionInProgress && !camera1.isCameraReady()) {
                cancelSwitchAnimation();
            }
            if (switchAnimationActive && newCameraFrameNs == 0
                    && nowNs - switchStartedNs > 5_000_000_000L) {
                if (switchMayRollback) {
                    switchMayRollback = false;
                    // Keep the frozen image blurred while restoring the prior camera.
                    switchStartedNs = nowNs;
                    switchWarmupFrames = 0;
                    mainHandler.post(() -> restoreCameraAfterMissingFrames());
                } else {
                    cancelSwitchAnimation();
                    mainHandler.post(() -> cameraUnavailable(
                            new IllegalStateException("No frames after camera switch recovery")));
                    return;
                }
            }
            if (GLUtil.getmEGLConfig()!=null && (ready || switchAnimationActive) &&
                    (hasCameraFrame || pendingCameraFrame)) {
                GLUtil.makeCurrent(GLUtil.getEglSurface());
                boolean acquired = false;
                if (ready && pendingCameraFrame && (!switchAnimationActive
                        || nowNs - switchStartedNs >= SWITCH_BLUR_NS)) {
                    GLUtil.getSurfaceTexture().updateTexImage();
                    GLUtil.getSurfaceTexture().getTransformMatrix(mTmpMatrix);
                    pendingCameraFrame = false;
                    long cameraTimestampNs = GLUtil.getSurfaceTexture().getTimestamp();
                    // Multiple notifications (including recovery's explicit queue drain)
                    // can refer to one buffer. Do not finish the transition or count a
                    // live preview frame until a genuinely new camera image is acquired.
                    if (cameraTimestampNs > 0 && cameraTimestampNs != lastCameraTimestampNs) {
                        lastCameraTimestampNs = cameraTimestampNs;
                        hasCameraFrame = true;
                        acquired = true;
                        frameCount++;
                        if (switchAnimationActive && newCameraFrameNs == 0 && ++switchWarmupFrames >= 2) {
                            newCameraFrameNs = Math.max(nowNs, switchStartedNs + SWITCH_FLIP_NS);
                        }
                    }
                }
                if (!hasCameraFrame) return;
                if (switchAnimationActive) {
                    if (lastSwitchRenderNs != 0 && nowNs - lastSwitchRenderNs < SWITCH_FRAME_NS) return;
                    lastSwitchRenderNs = nowNs;
                    float progress = newCameraFrameNs == 0 ? 0f
                            : Math.max(0f, Math.min(1f, (float)(nowNs - newCameraFrameNs) / SWITCH_REVEAL_NS));
                    float mix = progress * progress * (3f - 2f * progress);
                    float blur = newCameraFrameNs == 0
                            ? Math.min(1f, (float)(nowNs - switchStartedNs) / SWITCH_BLUR_NS)
                            : 1f - mix;
                    float flip = Math.min(1f, (float)(nowNs - switchStartedNs) / SWITCH_FLIP_NS);
                    GLUtil.setFrameTransition(blur, mix, flip, switchLandscape);
                    if (newCameraFrameNs != 0 && progress >= 1f) {
                        switchAnimationActive = false;
                        switchMayRollback = false;
                        cameraHandler.removeCallbacks(switchAnimationTick);
                        GLUtil.endFrameTransition();
                    }
                } else {
                    // Preserve the existing normal camera/recorder submission path.
                    // Fresh capture timestamps gate the transition, not normal callback
                    // delivery. Only discard a synthetic tick after its animation ends.
                    if (animationTick && !acquired) return;
                    GLUtil.setFrameOpacity(1f);
                }

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

                // MediaRecorder owns pause filtering and the common A/V timeline.
                // Keep its surface producer flowing while paused, like a camera
                // session still targeting the recorder, instead of starving resume.
                if(isRecording || (isRecord() && recorderState == RecorderState.PAUSED)) {
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

                    if(previousTime == 0){
                        previousTime = System.currentTimeMillis();
                    }
                    else if(!stopButton.isEnabled() && Math.abs(System.currentTimeMillis() - previousTime) >= 1000){
                        if(FRAME_VERBOSE)Log.d(TAG,"difference of 1 sec");
                        previousTime = System.currentTimeMillis();
                        if(recordStop == 1) {
                            mainHandler.sendEmptyMessage(Constants.RECORD_STOP_ENABLE);
                        }
                    }

                    if(sharedPreferences.getBoolean(Constants.SHOW_MEMORY_CONSUMED_MSG, false)) {
                        mainHandler.sendEmptyMessage(Constants.SHOW_MEMORY_CONSUMED);
                    }

                    if (recorderState == RecorderState.PREPARED) {
                        try {
                            mediaRecorder.start();
                            recorderState = RecorderState.RECORDING;
                            updateTimer = true;
                            recordStop = 1;
                            publishRecordingPauseState();
                        } catch (RuntimeException failure) {
                            failRecording("start", failure);
                            return;
                        }
                    }
                    EGLExt.eglPresentationTimeANDROID(GLUtil.getmEGLDisplay(), encoderSurface, getPTSUs());
                    EGL14.eglSwapBuffers(GLUtil.getmEGLDisplay(), encoderSurface);
                }
            }
            availableStatFs.restat(Environment.getDataDirectory().getPath());
            if(isRecord() && memoryPrefs.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)
                    && (availableStatFs.getAvailableBytes() < lowestMemory)) {
                if(FRAME_VERBOSE)Log.d(TAG, "lowestMemory = "+lowestMemory);
                if(FRAME_VERBOSE)Log.d(TAG, "avail mem = "+availableStatFs.getAvailableBytes());
                setRecord(false);
                endRecordingAudioPolicy();
                stopTimerThread();
                isRecording = false;
                recordStop = -1;
                cameraHandler.setRecordIncomplete(false);
                try {
                    mediaRecorder.stop();
                }
                catch(RuntimeException runtime){
                    Log.d(TAG,"Video data not received drawFrame... delete file = "+videoFile.getPath());
                    if(videoFile.delete()){
                        if(FRAME_VERBOSE)Log.d(TAG,"File deleted");
                    }
                    cameraHandler.setRecordIncomplete(true);
                }
                recorderState = RecorderState.IDLE;
                publishRecordingPauseState();
                mediaRecorder.release();
                mediaRecorder = null;
                releaseEncoderSurface();
                if(isCamera2()){
                    stopAndReleaseCamera();
                }
                if(FRAME_VERBOSE)Log.d(TAG,"stop isRecording == "+isRecording);
                if(!cameraHandler.isRecordIncomplete()){
                    scanSavedRecording();
                    mainHandler.sendEmptyMessage(Constants.RECORD_COMPLETE);
                }
                mainHandler.sendEmptyMessage(Constants.RECORD_STOP_LOW_MEMORY);
            }
        }

        @TargetApi(Build.VERSION_CODES.N)
        private void pause(){
            if (!isRecord() || mediaRecorder == null || recorderState != RecorderState.RECORDING) {
                publishRecordingPauseState();
                return;
            }
            try {
                mediaRecorder.pause();
                endRecordingAudioPolicy();
                recorderState = RecorderState.PAUSED;
                isRecording = false;
                updateTimer = false;
                setRecordPaused(true);
                if (videoTimer != null) videoTimer.interrupt();
                // Pausing before the first-second frame check must not leave Stop disabled.
                mainHandler.sendEmptyMessage(Constants.RECORD_STOP_ENABLE);
                mainHandler.sendEmptyMessage(Constants.SHOW_PAUSE_TEXT);
                publishRecordingPauseState();
            } catch (RuntimeException failure) {
                failRecording("pause", failure);
            }
        }

        @TargetApi(Build.VERSION_CODES.N)
        private void resumeRecord(){
            if (!isRecord() || mediaRecorder == null || recorderState != RecorderState.PAUSED) {
                publishRecordingPauseState();
                return;
            }
            try {
                beginRecordingAudioPolicy();
                mediaRecorder.resume();
                recorderState = RecorderState.RECORDING;
                isRecording = true;
                setRecordPaused(false);
                updateTimer = true;
                mainHandler.sendEmptyMessage(Constants.HIDE_PAUSE_TEXT);
                publishRecordingPauseState();
            } catch (RuntimeException failure) {
                failRecording("resume", failure);
            }
        }

        private void failRecording(String operation, RuntimeException failure) {
            Log.e(TAG, "Recorder " + operation + " failed; restoring preview", failure);
            endRecordingAudioPolicy();
            // Reuse finalization so a recoverable partial recording can still be saved.
            setRecord(false);
            stopTimerThread();
            cameraHandler.removeMessages(Constants.RECORD_PAUSE);
            cameraHandler.removeMessages(Constants.RECORD_RESUME);
            cameraHandler.sendEmptyMessage(Constants.RECORD_STOP);
            mainHandler.post(() -> {
                setKeepScreenOn(false);
                orientationEventListener.enable();
                camera1.disableRecordingHint();
                System.arraycopy(IDENTITY_MATRIX, 0, RECORD_IDENTITY_MATRIX, 0, IDENTITY_MATRIX.length);
                rotationAngle = 0f;
                setLayoutAspectRatio();
                hidePauseText();
            });
        }

        protected long getPTSUs() {
            // Keep surface timestamps on the monotonic nanosecond clock. MediaRecorder's
            // pause()/resume() already removes the paused interval from the output timeline;
            // subtracting it here makes the video track shorter than the audio track.
            return System.nanoTime();
        }

        void shutdown()
        {
            cancelSwitchAnimation();
            EGL14.eglMakeCurrent(GLUtil.getmEGLDisplay(), EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
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
                this.cameraRenderer = cameraRenderer;
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
                        if (!isRecord()) break;
                        if (!isRecord() || recorderState != RecorderState.IDLE) break;
                        recorderState = RecorderState.PREPARING;
                        setRecordPaused(false);
                        try {
                            cameraRenderer.setupMediaRecorder(getRecordVideoWidth(), getRecordVideoHeight(), getCamProfileForRecord());
                            hour = 0; minute = 0; second = 0;
                            isRecording = true;
                        } catch (RuntimeException failure) {
                            failRecording("prepare", failure);
                        }
                        break;
                    case Constants.RECORD_STOP:
                        cameraHandler.removeMessages(Constants.RECORD_START);
                        endRecordingAudioPolicy();
                        recordIncomplete = false;
                        try {
                            if (mediaRecorder == null) {
                                // Start was canceled before preparation. There is no
                                // new output to finalize or previous file to delete.
                                recordIncomplete = true;
                            } else {
                                mediaRecorder.stop();
                            }
                        }
                        catch(RuntimeException runtime){
                            Log.e(TAG,"Unable to finalize recording", runtime);
                            if(videoFile != null && videoFile.exists() && videoFile.delete()){
                                if(VERBOSE)Log.d(TAG,"File deleted");
                            }
                            recordIncomplete = true;
                        }
                        recorderState = RecorderState.IDLE;
                        publishRecordingPauseState();
                        cameraHandler.removeMessages(Constants.RECORD_PAUSE);
                        cameraHandler.removeMessages(Constants.RECORD_RESUME);
                        if (mediaRecorder != null) {
                            mediaRecorder.release();
                            mediaRecorder = null;
                        }
                        cameraRenderer.releaseEncoderSurface();
                        if(VERBOSE)Log.d(TAG, "RESET PAUSE");
                        videoFragment.setPause(false);
                        isRecording = false;
                        recordStop = -1;
                        setRecordPaused(false);
                        if(VERBOSE)Log.d(TAG,"stop isRecording == "+isRecording);
                        if(!recordIncomplete && videoFile != null && videoFile.isFile() && videoFile.length() > 0){
                            scanSavedRecording();
                            mainHandler.sendEmptyMessage(Constants.RECORD_COMPLETE);
                        } else if (!recordIncomplete) {
                            Log.e(TAG, "Recorder stopped without producing a non-empty file");
                        }
                        mainHandler.sendEmptyMessage(Constants.RECORD_STOPPED);
                        if(VERBOSE)Log.d(TAG, "Exit recording...");
                        if(VERBOSE)Log.d(TAG,"Orig frame = "+frameCount+" , Rendered frame "+frameCnt);
                        break;
                    case Constants.RECORD_PAUSE:
                        pause();
                        break;
                    case Constants.RECORD_RESUME:
                        resumeRecord();
                        break;
                    case Constants.RECORD_STOP_NO_SD_CARD:
                        isRecording = false;
                        videoFragment.setPause(false);
                        recordStop = -1;
                        recordIncomplete = false;
                        recorderState = RecorderState.IDLE;
                        publishRecordingPauseState();
                        cameraHandler.removeMessages(Constants.RECORD_PAUSE);
                        cameraHandler.removeMessages(Constants.RECORD_RESUME);
                        if (mediaRecorder != null) {
                            mediaRecorder.release();
                            mediaRecorder = null;
                        }
                        cameraRenderer.releaseEncoderSurface();
                        mainHandler.sendEmptyMessage(Constants.HIDE_PAUSE_TEXT);
                        mainHandler.sendEmptyMessage(Constants.RECORD_STOPPED);
                        if(VERBOSE)Log.d(TAG,"stop no SD Card isRecording == "+isRecording);
                        break;
                    case Constants.GET_CAMERA_RENDERER_INSTANCE:
                        getCameraRendererInstance();
                        break;
                }
            }
        }
    }

    public CameraView.VideoTimer startTimerThread()
    {
        startTimer = true;
        CameraView.VideoTimer videoTimer = new CameraView.VideoTimer();
        videoTimer.start();
        return videoTimer;
    }

    public void stopTimerThread()
    {
        startTimer = false;
        updateTimer = false;
        if (videoTimer != null && videoTimer.isAlive()) videoTimer.stopTimer();
    }
    volatile boolean startTimer = false;
    volatile boolean updateTimer = false;

    //This thread controls the timer for video recording. A separate thread was needed, since the CameraRenderer thread was overloaded with displaying
    //and recording each frame, the timer value was incorrect during recording.
    class VideoTimer extends Thread
    {
        private volatile boolean timerRunning = true;
        long startSleep = 0;
        long endSleep = 0;
        long previousTimeBlink = 0;
        long sleepTime = 0;

        private void updateTime(){
            if (second < 59) {
                second++;
            } else if (minute < 59) {
                minute++;
                second = 0;
            } else {
                minute = 0;
                second = 0;
                hour++;
            }
        }

        void stopTimer() {
            timerRunning = false;
            interrupt();
        }

        @Override
        public void run() {
            if(VERBOSE)Log.d(TAG,"VideoTimer STARTED");
            sleepTime = 1000;
            while(timerRunning){
                while (updateTimer && timerRunning) {
                    try {
                        startSleep = System.currentTimeMillis();
                        Thread.sleep(sleepTime);
                        if(sleepTime < 1000){
                            sleepTime = 1000;
                        }
                        updateTime();
                        mainHandler.sendEmptyMessage(Constants.SHOW_ELAPSED_TIME);
                    } catch (InterruptedException e) {
                        if (!timerRunning) break;
                        endSleep = System.currentTimeMillis();
                        //Recalculate sleep time since user has paused recording.
                        sleepTime = Math.max(1, Math.min(1000, 1000 - (endSleep - startSleep)));
                        if(VERBOSE)Log.d(TAG, "PAUSED VIDEO... Sleep incomplete at === "+(endSleep - startSleep) + " milliseconds");
                    }
                    if (!timerRunning) {
                        break;
                    }
                }
                if (!timerRunning) break;
                // Paused sessions can remain in the background; do not busy-spin.
                if (!updateTimer) {
                    try { Thread.sleep(50); } catch (InterruptedException ignored) { }
                }
                //Add blinking text effect
                if(isRecordPaused()){
                    if(Math.abs(System.currentTimeMillis() - previousTimeBlink) >= 700){
                        if(VERBOSE)Log.d(TAG,"difference of 700 msec");
                        previousTimeBlink = System.currentTimeMillis();
                        if(isPauseTextVisible()) {
                            mainHandler.sendEmptyMessage(Constants.HIDE_PAUSE_TEXT);
                        }
                        else{
                            mainHandler.sendEmptyMessage(Constants.SHOW_PAUSE_TEXT);
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
