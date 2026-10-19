package com.flipcam;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.flipcam.constants.Constants;
import com.flipcam.data.MediaTableConstants;
import com.flipcam.media.FileMedia;
import com.flipcam.util.GLUtil;
import com.flipcam.util.MediaUtil;
import com.flipcam.util.SDCardUtil;
import com.flipcam.view.CameraView;
import com.flipcam.view.PinchZoomGestureListener;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static android.widget.Toast.makeText;

public class VideoFragment extends Fragment{

    public static final String TAG = "VideoFragment";
    SeekBar zoombar;
    CameraView cameraView;
    ImageButton switchCamera;
    ImageButton startRecord;
    ImageButton flash;
    ImageView substitute;
    ImageView thumbnail;
    LinearLayout videoBar;
    LinearLayout settingsBar;
    TextView timeElapsed;
    TextView memoryConsumed;
    PermissionInterface permissionInterface;
    LowestThresholdCheckForVideoInterface lowestThresholdCheckForVideoInterface;
    ImageButton stopRecord;
    ImageButton pauseRecord;
    TextView modeText;
    TextView resInfo;
    LinearLayout modeLayout;
    OrientationEventListener orientationEventListener;
    int orientation = -1;
    LinearLayout flashParentLayout;
    FrameLayout thumbnailParent;
    View warningMsgRoot;
    Dialog warningMsg;
    LayoutInflater layoutInflater;
    SDCardEventReceiver sdCardEventReceiver;
    IntentFilter mediaFilters;
    Button okButton;
    TextView pauseText;
    boolean sdCardUnavailWarned = false;
    SharedPreferences sharedPreferences;
    ImageView microThumbnail;
    AppWidgetManager appWidgetManager;
    boolean VERBOSE = false;
    boolean isPause = false;
    View settingsMsgRoot;
    Dialog settingsMsgDialog;
    Context mContext;
    private static VideoFragment fragment = null;
    PinchZoomGestureListener pinchZoomGestureListener;
    int audioSampleRate = -1;
    int audioBitRate = -1;
    int audioChannelInput = -1;
    ControlVisbilityPreference controlVisbilityPreference;

    public static VideoFragment newInstance() {
        Log.d(TAG, "NEW INSTANCE");
        if(fragment == null) {
            fragment = new VideoFragment();
        }
        return fragment;
    }

    public interface PermissionInterface{
        void askPermission();
    }


    public interface LowestThresholdCheckForVideoInterface{
        boolean checkIfPhoneMemoryIsBelowLowestThresholdForVideo();
    }

    public void setApplicationContext(Context ctx){
        mContext = ctx;
    }

    public Context getApplicationContext(){
        return mContext;
    }

    CameraActivity cameraActivity;
    ImageView toggleAudio;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(VERBOSE)Log.d(TAG,"onActivityCreated");
        if(cameraView!=null) {
            cameraView.setWindowManager(getActivity().getWindowManager());
        }
        cameraActivity = (CameraActivity)getActivity();
        toggleAudio = cameraActivity.findViewById(R.id.toggleAudio);
        settingsBar = (LinearLayout)cameraActivity.findViewById(R.id.settingsBar);
        flash = (ImageButton)cameraActivity.findViewById(R.id.flashOn);
        flash.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                setFlash();
            }
        });
        cameraView.setFlashButton(flash);
        modeText = (TextView)cameraActivity.findViewById(R.id.modeInfo);
        resInfo = (TextView)cameraActivity.findViewById(R.id.resInfo);
        modeLayout = (LinearLayout)cameraActivity.findViewById(R.id.modeLayout);
        permissionInterface = (PermissionInterface)cameraActivity;
        lowestThresholdCheckForVideoInterface = (LowestThresholdCheckForVideoInterface)cameraActivity;
        layoutInflater = (LayoutInflater)cameraActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        warningMsgRoot = layoutInflater.inflate(R.layout.warning_message, null);
        warningMsg = new Dialog(cameraActivity);
        settingsMsgRoot = layoutInflater.inflate(R.layout.settings_message, null);
        settingsMsgDialog = new Dialog(cameraActivity);
        mediaFilters = new IntentFilter();
        sdCardEventReceiver = new SDCardEventReceiver();
        sharedPreferences = cameraActivity.getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        appWidgetManager = (AppWidgetManager)cameraActivity.getSystemService(Context.APPWIDGET_SERVICE);
        pinchZoomGestureListener = cameraActivity.getPinchZoomGestureListener();
        // Apply the aligned preview controls after the activity's overlay has
        // completed its first layout so they are present on the initial camera
        // preview, not only after a surface or recording transition.
        settingsBar.post(() -> {
            if (getView() != null && cameraView != null && !cameraView.isRecord()) {
                showRecordAndThumbnail();
                getLatestFileIfExists();
            }
        });
    }

    @Override
    public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public CameraView getCameraView() {
        return cameraView;
    }



    public int getCameraMaxZoom(){
        return cameraView.getCameraMaxZoom();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_video, container, false);

        if(VERBOSE)Log.d(TAG,"Inside video fragment");
        substitute = (ImageView)view.findViewById(R.id.substitute);
        substitute.setVisibility(View.INVISIBLE);
        cameraView = (CameraView)view.findViewById(R.id.cameraSurfaceView);
        GLUtil.colorVal = Constants.NORMAL_BRIGHTNESS_PROGRESS;
        if(VERBOSE)Log.d(TAG,"cameraview onresume visibility= "+cameraView.getWindowVisibility());
        pauseText = view.findViewById(R.id.pauseText);
        zoombar = (SeekBar)view.findViewById(R.id.zoomBar);
        zoombar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.progressFill)));
        cameraView.setSeekBar(zoombar);
        zoombar.setProgress(0);
        zoombar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(cameraView.isCameraReady() && fromUser) {
                    if (cameraView.isSmoothZoomSupported()) {
                        //if(VERBOSE)Log.d(TAG, "Smooth zoom supported");
                        cameraView.smoothZoomInOrOut(progress);
                    } else if (cameraView.isZoomSupported()) {
                        cameraView.zoomInAndOut(progress);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(!cameraView.isSmoothZoomSupported() && !cameraView.isZoomSupported()) {
                    makeText(getActivity().getApplicationContext(), getResources().getString(R.string.zoomNotSupported), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(VERBOSE)Log.d(TAG, "onStopTrackingTouch = "+seekBar.getProgress());
                cameraActivity.getPinchZoomGestureListener().setProgress(seekBar.getProgress());
            }
        });
        thumbnail = (ImageView)view.findViewById(R.id.thumbnail);
        microThumbnail = (ImageView)view.findViewById(R.id.microThumbnail);
        thumbnailParent = (FrameLayout)view.findViewById(R.id.thumbnailParent);
        switchCamera = (ImageButton)view.findViewById(R.id.switchCamera);
        switchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecord.setClickable(false);
                flash.setClickable(false);
                thumbnail.setClickable(false);
                toggleAudio.setClickable(false);

                cameraView.switchCamera();
                getZoomBar().setProgress(0);
                cameraActivity.getPinchZoomGestureListener().setProgress(0);

                zoombar.setProgress(0);
                startRecord.setClickable(true);
                flash.setClickable(true);
                thumbnail.setClickable(true);
                toggleAudio.setClickable(true);
            }
        });
        startRecord = (ImageButton)view.findViewById(R.id.cameraRecord);
        videoBar = (LinearLayout)view.findViewById(R.id.videoFunctions);
        startRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecord.setClickable(false);
                switchCamera.setClickable(false);
                thumbnail.setClickable(false);
                toggleAudio.setClickable(false);
                if(lowestThresholdCheckForVideoInterface.checkIfPhoneMemoryIsBelowLowestThresholdForVideo()){
                    LayoutInflater layoutInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View thresholdExceededRoot = layoutInflater.inflate(R.layout.threshold_exceeded, null);
                    final Dialog thresholdDialog = new Dialog(getActivity());
                    TextView memoryLimitMsg = (TextView)thresholdExceededRoot.findViewById(R.id.memoryLimitMsg);
                    int lowestThreshold = getResources().getInteger(R.integer.minimumMemoryWarning);
                    StringBuilder minimumThreshold = new StringBuilder(lowestThreshold+"");
                    minimumThreshold.append(" ");
                    minimumThreshold.append(getResources().getString(R.string.MEM_PF_MB));
                    if(VERBOSE)Log.d(TAG, "minimumThreshold = "+minimumThreshold);
                    memoryLimitMsg.setText(getResources().getString(R.string.minimumThresholdExceeded, minimumThreshold));
                    CheckBox disableThreshold = (CheckBox)thresholdExceededRoot.findViewById(R.id.disableThreshold);
                    disableThreshold.setVisibility(View.GONE);
                    Button okButton = (Button)thresholdExceededRoot.findViewById(R.id.okButton);
                    okButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            thresholdDialog.dismiss();
                            startRecord.setClickable(true);
                            thumbnail.setClickable(true);
                            switchCamera.setClickable(true);
                            toggleAudio.setClickable(true);
                        }
                    });
                    thresholdDialog.setContentView(thresholdExceededRoot);
                    thresholdDialog.setCancelable(false);
                    thresholdDialog.show();
                }
                else {
                    SharedPreferences.Editor settingsEditor = sharedPreferences.edit();
                    if (sharedPreferences.getBoolean(Constants.PHONE_MEMORY_DISABLE, true)) {
                        if(!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
                            //Check if the FC folder exists inside SD Card.
                            if(SDCardUtil.doesSDCardFlipCamFolderExist(sharedPreferences.getString(Constants.SD_CARD_PATH, ""))){
                                sdCardUnavailWarned = false;
                                prepareAndStartRecord();
                            }
                            else{
                                //If the FC Folder does not exist, create a new folder and continue recording.
                                if(SDCardUtil.doesSDCardExist(getApplicationContext()) == null) {
                                    sdCardUnavailWarned = true;
                                    settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
                                    settingsEditor.commit();
                                    cameraActivity.updateStorageDestinationLabel();
                                    showErrorWarningMessage(getResources().getString(R.string.sdCardRemovedTitle), getResources().getString(R.string.sdCardNotPresentForRecord));
                                    getLatestFileIfExists();
                                }
                                else{
                                    //Continue recording. doesSDCardExist() will create a new folder which can be used for recording.
                                    sdCardUnavailWarned = false;
                                    prepareAndStartRecord();
                                }
                            }
                        }
                        else{
                            prepareAndStartRecord();
                        }
                    } else {
                        checkIfMemoryLimitIsExceeded();
                    }
                }
            }
        });
        if(VERBOSE)Log.d(TAG,"passing videofragment to cameraview");
        cameraView.setFragmentInstance(this);
        orientationEventListener = new OrientationEventListener(getActivity().getApplicationContext(), SensorManager.SENSOR_DELAY_UI){
            @Override
            public void onOrientationChanged(int i) {
                if(orientationEventListener.canDetectOrientation()) {
                    orientation = i;
                    determineOrientation();
                    rotateIcons();
                }
            }
        };
        flashParentLayout = new LinearLayout(getActivity());
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] mediaCodecInfos = mediaCodecList.getCodecInfos();
        if(getAudioBitRate() == -1 || getAudioChannelInput() == -1 || getAudioSampleRate() == -1) {
            for (MediaCodecInfo info : mediaCodecInfos) {
                if(VERBOSE)Log.d(TAG, "Name = " + info.getName());
                if (info.getName().contains("aac")) {
                    String[] medTypes = info.getSupportedTypes();
                    for (String medType : medTypes) {
                        if(VERBOSE)Log.d(TAG, "media types = " + medType);
                        if (medType.contains("mp4a") || medType.contains("mp4") || medType.contains("mpeg4")) {
                            MediaCodecInfo.AudioCapabilities audioCapabilities = info.getCapabilitiesForType(medType).getAudioCapabilities();
                            Range<Integer> bitRates = audioCapabilities.getBitrateRange();
                            if(VERBOSE)Log.d(TAG, "Bit rate range = " + bitRates.getLower() + " , " + bitRates.getUpper());
                            setAudioBitRate(bitRates.getUpper());
                            int[] sampleRates = audioCapabilities.getSupportedSampleRates();
                            Arrays.sort(sampleRates);
                            if(VERBOSE)Log.d(TAG, "Sample rate = " + sampleRates[sampleRates.length - 1]);
                            setAudioSampleRate(sampleRates[sampleRates.length - 1]);
                            setAudioChannelInput(audioCapabilities.getMaxInputChannelCount() > 1 ? 2 : 1);
                            break;
                        }
                    }
                    break;
                }
            }
        }
        controlVisbilityPreference = (ControlVisbilityPreference)getApplicationContext();
        return view;
    }

    public TextView getPauseText() {
        return pauseText;
    }

    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    public void setAudioSampleRate(int audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

    public int getAudioBitRate() {
        return audioBitRate;
    }

    public void setAudioBitRate(int audioBitRate) {
        this.audioBitRate = audioBitRate;
    }

    public int getAudioChannelInput() {
        return audioChannelInput;
    }

    public void setAudioChannelInput(int audioChannelInput) {
        this.audioChannelInput = audioChannelInput;
    }

    class SDCardEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if(VERBOSE)Log.d(TAG, "onReceive = "+intent.getAction());
            if(intent.getAction().equalsIgnoreCase(Intent.ACTION_MEDIA_UNMOUNTED) ||
                    intent.getAction().equalsIgnoreCase(Constants.MEDIA_UNMOUNTED)){
                //Check if SD Card was selected
                SharedPreferences.Editor settingsEditor = sharedPreferences.edit();
                if(!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true) && !sdCardUnavailWarned){
                    if(VERBOSE)Log.d(TAG, "SD Card Removed");
                    settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
                    settingsEditor.commit();
                    cameraActivity.updateStorageDestinationLabel();
                    showErrorWarningMessage(getResources().getString(R.string.sdCardRemovedTitle), getResources().getString(R.string.sdCardNotPresentForRecord));
                    getLatestFileIfExists();
                }
            }
        }
    }

    public void showToastSDCardUnavailWhileRecordMessage(){
        Toast.makeText(getApplicationContext(),getResources().getString(R.string.sdCardRemovedWhileRecord),Toast.LENGTH_LONG).show();
    }

    public void showErrorWarningMessage(String title, String message){
        TextView warningTitle = (TextView)warningMsgRoot.findViewById(R.id.warningTitle);
        warningTitle.setText(title);
        TextView warningText = (TextView)warningMsgRoot.findViewById(R.id.warningText);
        warningText.setText(message);
        okButton = (Button)warningMsgRoot.findViewById(R.id.okButton);
        okButton.setOnClickListener((view) -> {
            startRecord.setClickable(true);
            thumbnail.setClickable(true);
            switchCamera.setClickable(true);
            warningMsg.dismiss();
        });
        warningMsg.setContentView(warningMsgRoot);
        warningMsg.setCancelable(false);
        warningMsg.show();
    }

    public void checkForSDCard(){
        if(VERBOSE)Log.d(TAG, "save media pref = "+sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true));
        if(!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
            if(!SDCardUtil.doesSDCardFlipCamFolderExist(sharedPreferences.getString(Constants.SD_CARD_PATH, ""))) {
                if(VERBOSE)Log.d(TAG, "FC Folder not exist SD Card");
                if(VERBOSE)Log.d(TAG, "showFCFolderNotExistMessage");
                showErrorWarningMessage(getResources().getString(R.string.sdCardFCFolderNotExistTitle), getResources().getString(R.string.sdCardFCFolderNotExistMessage));
            }
        }
        else{
            if(VERBOSE)Log.d(TAG, "displaySDCardNotDetectMessage 2222");
            cameraActivity.displaySDCardNotDetectMessage();
        }
    }

    private boolean isUseFCPlayer(){
        String fcPlayer = getResources().getString(R.string.videoFCPlayer);
        String externalPlayer = getResources().getString(R.string.videoExternalPlayer);
        SharedPreferences videoPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if(videoPrefs.getString(Constants.SELECT_VIDEO_PLAYER, externalPlayer).equalsIgnoreCase(fcPlayer)){
            return true;
        }
        else{
            return false;
        }
    }

    public void prepareAndStartRecord(){
        AudioManager audioManager = cameraView.getAudioManager();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            if(VERBOSE)Log.d(TAG, "setStreamMute");
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
        }
        else{
            if(VERBOSE)Log.d(TAG, "adjustStreamVolume");
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
        }
        startRecord.setEnabled(false);
        thumbnail.setClickable(true);
        switchCamera.setClickable(true);
        toggleAudio.setClickable(true);
        toggleAudio.setVisibility(View.GONE);
        addStopAndPauseIcons();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("videoCapture", true);
        editor.commit();
        cameraView.record(false);
        // Camera size changes can trigger surfaceChanged(), so set recording UI
        // after the camera transition has queued its layout updates.
        hideSettingsBarAndIcon();
    }

    public void checkIfMemoryLimitIsExceeded(){
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        int memoryThreshold = Integer.parseInt(sharedPreferences.getString(Constants.PHONE_MEMORY_LIMIT, ""));
        String memoryMetric = sharedPreferences.getString(Constants.PHONE_MEMORY_METRIC, "");
        StatFs storageStat = new StatFs(Environment.getDataDirectory().getPath());
        long memoryValue = 0;
        String metric = "";
        switch(memoryMetric){
            case "MB":
                memoryValue = (memoryThreshold * (long)Constants.MEGA_BYTE);
                metric = "MB";
                break;
            case "GB":
                memoryValue = (memoryThreshold * (long)Constants.GIGA_BYTE);
                metric = "GB";
                break;
        }
        if(VERBOSE)Log.d(TAG, "memory value = "+memoryValue);
        if(VERBOSE)Log.d(TAG, "Avail mem = "+storageStat.getAvailableBytes());
        if(storageStat.getAvailableBytes() < memoryValue){
            LayoutInflater layoutInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View thresholdExceededRoot = layoutInflater.inflate(R.layout.threshold_exceeded, null);
            final Dialog thresholdDialog = new Dialog(getActivity());
            TextView memoryLimitMsg = (TextView)thresholdExceededRoot.findViewById(R.id.memoryLimitMsg);
            final CheckBox disableThreshold = (CheckBox)thresholdExceededRoot.findViewById(R.id.disableThreshold);
            Button okButton = (Button)thresholdExceededRoot.findViewById(R.id.okButton);
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(VERBOSE)Log.d(TAG, "disableThreshold.isChecked = "+disableThreshold.isChecked());
                    if(disableThreshold.isChecked()){
                        editor.remove(Constants.PHONE_MEMORY_LIMIT);
                        editor.remove(Constants.PHONE_MEMORY_METRIC);
                        editor.putBoolean(Constants.PHONE_MEMORY_DISABLE, true);
                        editor.commit();
                        Toast.makeText(getApplicationContext(),getResources().getString(R.string.minimumThresholdDisabled),Toast.LENGTH_LONG).show();
                    }
                    thresholdDialog.dismiss();
                    prepareAndStartRecord();
                }
            });
            StringBuilder memThreshold = new StringBuilder(memoryThreshold+"");
            memThreshold.append(" ");
            memThreshold.append(metric);
            if(VERBOSE)Log.d(TAG, "memory threshold for display = "+memThreshold);
            memoryLimitMsg.setText(getActivity().getResources().getString(R.string.thresholdLimitExceededMsg, memThreshold.toString()));
            thresholdDialog.setContentView(thresholdExceededRoot);
            thresholdDialog.setCancelable(false);
            thresholdDialog.show();
        }
        else{
            prepareAndStartRecord();
        }
    }

    public void rotateIcons()
    {
        switchCamera.setRotation(rotationAngle);
        flash.setRotation(rotationAngle);
        microThumbnail.setRotation(rotationAngle);
        if(pauseRecord!=null) {
            pauseRecord.setRotation(rotationAngle);
            pauseText.setRotation(rotationAngle);
        }
        thumbnail.setRotation(rotationAngle);
    }

    public SeekBar getZoomBar()
    {
        return zoombar;
    }

    public void addStopAndPauseIcons()
    {
        videoBar.removeAllViews();
        videoBar.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        stopRecord = new ImageButton(getActivity().getApplicationContext());
        stopRecord.setScaleType(ImageView.ScaleType.CENTER_CROP);
        stopRecord.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        stopRecord.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_record_stop));
        stopRecord.setContentDescription(getString(R.string.stopRecording));
        cameraView.setStopButton(stopRecord);

        layoutParams.height=(int)getResources().getDimension(R.dimen.stopButtonHeight);
        layoutParams.width=(int)getResources().getDimension(R.dimen.stopButtonWidth);
        layoutParams.setMargins((int)getResources().getDimension(R.dimen.stopBtnLeftMargin),0,(int)getResources().getDimension(R.dimen.stopBtnRightMargin),0);
        stopRecord.setLayoutParams(layoutParams);
        stopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecordAndSaveFile(false);
            }
        });
        switchCamera.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        switchCamera.setRotation(rotationAngle);
        // In preview this control is hosted by an alignment slot. Detach it
        // before moving it into the recording controls row.
        ViewParent switchCameraParent = switchCamera.getParent();
        if (switchCameraParent instanceof ViewGroup) {
            ((ViewGroup) switchCameraParent).removeView(switchCamera);
        }
        videoBar.addView(switchCamera);
        videoBar.addView(stopRecord);
        addPauseButton();
        toggleAudio.setClickable(false);
    }

    @TargetApi(Build.VERSION_CODES.N)
    public void addPauseButton(){
        pauseRecord = new ImageButton(getActivity().getApplicationContext());
        pauseRecord.setScaleType(ImageView.ScaleType.CENTER_CROP);
        pauseRecord.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        pauseRecord.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_pause));
        pauseRecord.setContentDescription(getString(R.string.pauseRecording));
        pauseRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseRecord.setEnabled(false);
                if(VERBOSE)Log.d(TAG, "isPause ==== "+isPause());
                if(!isPause()) {
                    cameraView.recordPause();
                    pauseRecord.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_resume));
                    pauseRecord.setContentDescription(getString(R.string.resumeRecording));
                    setPause(true);
                }
                else{
                    cameraView.recordResume();
                    pauseRecord.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_pause));
                    pauseRecord.setContentDescription(getString(R.string.pauseRecording));
                    setPause(false);
                }
                pauseRecord.setEnabled(true);
            }
        });
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, (int) getResources().getDimension(R.dimen.recordSubsBtnRightMargin), 0);
        layoutParams.width = (int)getResources().getDimension(R.dimen.pauseButtonWidth);
        layoutParams.height = (int)getResources().getDimension(R.dimen.pauseButtonHeight);
        pauseRecord.setLayoutParams(layoutParams);
        videoBar.addView(pauseRecord);
    }

    public boolean isPause() {
        return isPause;
    }

    public void setPause(boolean pause) {
        isPause = pause;
    }

    public void stopRecordAndSaveFile(boolean lowMemory){
        boolean noSdCard = false;
        stopRecord.setClickable(false);
        switchCamera.setClickable(false);
        if(VERBOSE)Log.d(TAG, "Unmute audio stopRec");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            if(VERBOSE)Log.d(TAG, "setStreamUnMute");
            cameraView.getAudioManager().setStreamMute(AudioManager.STREAM_MUSIC, false);
        }
        else{
            if(VERBOSE)Log.d(TAG, "adjustStreamVolumeUnMute");
            cameraView.getAudioManager().adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
        }
        if(lowMemory){
            LayoutInflater layoutInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View thresholdExceededRoot = layoutInflater.inflate(R.layout.threshold_exceeded, null);
            final Dialog thresholdDialog = new Dialog(getActivity());
            int lowestThreshold = getResources().getInteger(R.integer.minimumMemoryWarning);
            TextView memoryLimitMsg = (TextView)thresholdExceededRoot.findViewById(R.id.memoryLimitMsg);
            StringBuilder minimumThreshold = new StringBuilder(lowestThreshold+"");
            minimumThreshold.append(" ");
            minimumThreshold.append(getResources().getString(R.string.MEM_PF_MB));
            memoryLimitMsg.setText(getResources().getString(R.string.minimumThresholdExceeded, minimumThreshold));
            CheckBox disableThreshold = (CheckBox)thresholdExceededRoot.findViewById(R.id.disableThreshold);
            disableThreshold.setVisibility(View.GONE);
            Button okButton = (Button)thresholdExceededRoot.findViewById(R.id.okButton);
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    thresholdDialog.dismiss();
                    stopRecord.setClickable(true);
                    switchCamera.setClickable(true);
                }
            });
            thresholdDialog.setContentView(thresholdExceededRoot);
            thresholdDialog.setCancelable(false);
            thresholdDialog.show();
            addMediaToDB();
        }
        else {
            if(!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
                if(SDCardUtil.doesSDCardExist(getApplicationContext()) != null){
                    noSdCard = false;
                }
                else{
                    startRecord.setClickable(false);
                    thumbnail.setClickable(false);
                    switchCamera.setClickable(false);
                    noSdCard = true;
                    SharedPreferences.Editor settingsEditor = sharedPreferences.edit();
                    settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
                    settingsEditor.commit();
                    cameraActivity.updateStorageDestinationLabel();
                    showErrorWarningMessage(getResources().getString(R.string.sdCardRemovedTitle), getResources().getString(R.string.sdCardRemovedWhileRecord));
                    getLatestFileIfExists();
                    new Thread(() -> {
                        deleteLatestBadFile();
                    }).start();
                }
            }
            else{
                noSdCard = false;
            }
            cameraView.record(noSdCard);
        }
        showRecordAndThumbnail();
        stopRecord.setClickable(true);
        switchCamera.setClickable(true);
        toggleAudio.setClickable(true);
    }

    float rotationAngle = 0f;
    public void determineOrientation()
    {
        if(orientation != -1) {
            if (((orientation >= 315 && orientation <= 360) || (orientation >= 0 && orientation <= 45)) || (orientation >= 135 && orientation <= 195)) {
                if (orientation >= 135 && orientation <= 195) {
                    //Reverse portrait
                    rotationAngle = 180f;
                } else {
                    //Portrait
                    rotationAngle = 0f;
                }
            } else {
                if (orientation >= 46 && orientation <= 134) {
                    //Reverse Landscape
                    rotationAngle = 270f;
                } else {
                    //Landscape
                    rotationAngle = 90f;
                }
            }
        }
    }

    public void showRecordSaved()
    {
        if (!isAdded()) return;
        // Android 11+ blocks custom-view Toasts. Use a standard text Toast so
        // the save confirmation remains visible on current Android versions.
        Toast.makeText(requireContext(), R.string.RECORD_SAVED, Toast.LENGTH_SHORT).show();
    }

    public void showRecordAndThumbnail()
    {
        cameraActivity.toggleAudio.setVisibility(View.VISIBLE);
        if (pauseText != null) pauseText.setVisibility(View.INVISIBLE);
        View decor = cameraActivity.getWindow().getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        new WindowInsetsControllerCompat(cameraActivity.getWindow(), decor)
                .show(WindowInsetsCompat.Type.navigationBars());
        videoBar.setBackgroundColor(getResources().getColor(R.color.settingsBarColor));
        //Enable Audio if disabled, after recording is completed.
        SharedPreferences micPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor prefsEditor = micPrefs.edit();
        if(micPrefs.getBoolean(Constants.NO_AUDIO_MSG, false)) {
            cameraActivity.toggleAudio.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_mic_on));
            cameraActivity.toggleAudio.setContentDescription(getString(R.string.microphoneEnabled));
            prefsEditor.putBoolean(Constants.NO_AUDIO_MSG, false);
            prefsEditor.apply();
        }
        videoBar.removeAllViews();
        videoBar.setWeightSum(3);
        addBottomControlSlot(switchCamera);
        addBottomControlSlot(startRecord);
        addBottomControlSlot(thumbnailParent);
        settingsBar.removeAllViews();
        settingsBar.setWeightSum(3);
        settingsBar.setBackgroundColor(getResources().getColor(R.color.settingsBarColor));
        flashParentLayout.removeAllViews();
        if(cameraView.isCameraReady()) {
            if (cameraView.isFlashOn()) {
                flash.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_flash_off));
            } else {
                flash.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_flash_on));
            }
        }
        FrameLayout.LayoutParams flashParams = new FrameLayout.LayoutParams(
                (int)getResources().getDimension(R.dimen.flashOnWidth),
                (int)getResources().getDimension(R.dimen.flashOnHeight), Gravity.CENTER);
        flash.setScaleType(ImageView.ScaleType.FIT_CENTER);
        flash.setLayoutParams(flashParams);
        flash.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                setFlash();
            }
        });
        cameraView.setFlashButton(flash);
        modeLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        if (resInfo.getParent() != modeLayout) {
            ViewGroup previousParent = (ViewGroup) resInfo.getParent();
            if (previousParent != null) previousParent.removeView(resInfo);
            modeLayout.addView(resInfo);
        }
        cameraActivity.storageDestination.setGravity(Gravity.CENTER);
        modeText.setText(getResources().getString(R.string.VIDEO_MODE));
        flash.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        addPreviewControlSlot(flash);
        addPreviewControlSlot(modeLayout);
        addPreviewControlSlot(cameraActivity.storageDestination);
    }

    private void addPreviewControlSlot(View control) {
        ViewParent parent = control.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(control);
        }
        FrameLayout slot = new FrameLayout(getActivity());
        slot.addView(control, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        settingsBar.addView(slot, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }

    private void addBottomControlSlot(View control) {
        ViewParent parent = control.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(control);
        }
        FrameLayout slot = new FrameLayout(getActivity());
        slot.addView(control, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        videoBar.addView(slot, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
    }

    public void setVideoResInfo(String width, String height){
        resInfo.setText(getResources().getString(R.string.resolutionDisplay, width, height));
    }

    public void hideSettingsBarAndIcon()
    {
        View decor = cameraActivity.getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        WindowInsetsControllerCompat insetsController =
                new WindowInsetsControllerCompat(cameraActivity.getWindow(), decor);
        insetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        insetsController.hide(WindowInsetsCompat.Type.systemBars());
        // The button tap that starts recording can briefly reveal transient bars
        // on newer Android releases. Reapply the immersive state after that touch.
        decor.postDelayed(() -> {
            if (cameraView.isRecord()) {
                new WindowInsetsControllerCompat(cameraActivity.getWindow(), decor)
                        .hide(WindowInsetsCompat.Type.systemBars());
            }
        }, 350);
        settingsBar.removeAllViews();
        flashParentLayout.removeAllViews();
        if (resInfo.getParent() == modeLayout) {
            modeLayout.removeView(resInfo);
        }
        settingsBar.setWeightSum(0);
        settingsBar.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        flashParentLayout.setOrientation(LinearLayout.VERTICAL);
        flashParentLayout.setGravity(Gravity.CENTER);
        if(cameraView.isFlashOn()) {
            flash.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_flash_off));
        }
        else{
            flash.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_flash_on));
        }
        flash.setOnClickListener((view) -> setFlash());
        LinearLayout.LayoutParams flashParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flashParam.width = (int)getResources().getDimension(R.dimen.flashOnWidth);
        flashParam.height = (int)getResources().getDimension(R.dimen.flashOnHeight);
        flash.setScaleType(ImageView.ScaleType.FIT_CENTER);
        flash.setLayoutParams(flashParam);
        flash.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        cameraView.setFlashButton(flash);
        ViewParent flashParent = flash.getParent();
        if (flashParent instanceof ViewGroup) {
            ((ViewGroup) flashParent).removeView(flash);
        }
        flashParentLayout.addView(flash);
        ViewParent oldFlashOverlay = flashParentLayout.getParent();
        if (oldFlashOverlay instanceof ViewGroup) {
            ((ViewGroup) oldFlashOverlay).removeView(flashParentLayout);
        }
        FrameLayout recordingTopOverlay = new FrameLayout(getActivity());
        recordingTopOverlay.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        FrameLayout.LayoutParams flashOverlayParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL);
        flashOverlayParams.setMargins((int) getResources().getDimension(R.dimen.flashOnLeftMargin), 0, 0, 0);
        recordingTopOverlay.addView(flashParentLayout, flashOverlayParams);

        //Add time elapsed text
        timeElapsed = new TextView(getActivity());
        timeElapsed.setGravity(Gravity.CENTER_HORIZONTAL);
        timeElapsed.setTypeface(Typeface.DEFAULT_BOLD);
        timeElapsed.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        timeElapsed.setTextColor(getResources().getColor(R.color.timeElapsed));
        timeElapsed.setText(getResources().getString(R.string.START_TIME));
        timeElapsed.setTextSize(24);
        timeElapsed.setShadowLayer(3f, 0f, 1f, Color.BLACK);
        FrameLayout.LayoutParams timerOverlayParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        timeElapsed.setLayoutParams(timerOverlayParams);
        cameraView.setTimeElapsedText(timeElapsed);
        recordingTopOverlay.addView(timeElapsed);
        settingsBar.addView(recordingTopOverlay, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        //Add memory consumed text
        memoryConsumed = new TextView(getActivity());
        memoryConsumed.setGravity(Gravity.CENTER_HORIZONTAL);
        memoryConsumed.setTextColor(getResources().getColor(R.color.memoryConsumed));
        memoryConsumed.setTypeface(Typeface.DEFAULT_BOLD);
        memoryConsumed.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        memoryConsumed.setText(getResources().getString(R.string.START_MEMORY));
        LinearLayout.LayoutParams memConsumed = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        memConsumed.setMargins(0, 0, 0, 0);
        memoryConsumed.setTextSize(11);
        memoryConsumed.setLayoutParams(memConsumed);
        cameraView.setMemoryConsumedText(memoryConsumed);
        memoryConsumed.setVisibility(View.GONE);
    }

    public void onRecordingStopped() {
        startRecord.setEnabled(true);
        startRecord.setClickable(true);
    }

    boolean flashOn=false;
    private void setFlash()
    {
        if(!flashOn)
        {
            if(VERBOSE)Log.d(TAG,"Flash on");
            if(cameraView.isFlashModeSupported(cameraView.getCameraImplementation().getFlashModeTorch())) {
                flashOn = true;
                flash.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_flash_off));
                TextView feature = (TextView)settingsMsgRoot.findViewById(R.id.feature);
                feature.setText(getResources().getString(R.string.flashSetting).toUpperCase());
                TextView value = (TextView)settingsMsgRoot.findViewById(R.id.value);
                value.setText(getResources().getString(R.string.torchMode).toUpperCase());
                ImageView heading = (ImageView)settingsMsgRoot.findViewById(R.id.heading);
                heading.setImageDrawable(getResources().getDrawable(R.drawable.torch));
                final Toast settingsMsg = Toast.makeText(getActivity().getApplicationContext(),"",Toast.LENGTH_SHORT);
                settingsMsg.setGravity(Gravity.CENTER,0,0);
                settingsMsg.setView(settingsMsgRoot);
                settingsMsg.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1250);
                            settingsMsg.cancel();
                        }catch (InterruptedException ie){
                            ie.printStackTrace();
                        }
                    }
                }).start();
            }
            else{
                if(cameraView.getCameraImplementation().getFlashModeTorch().equalsIgnoreCase(getResources().getString(R.string.torchMode)))
                {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.flashModeNotSupported, getResources().getString(R.string.torchMode)), Toast.LENGTH_SHORT).show();
                }
                else if(cameraView.getCameraImplementation().getFlashModeTorch().equalsIgnoreCase(getResources().getString(R.string.singleMode)))
                {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.flashModeNotSupported, getResources().getString(R.string.singleMode)), Toast.LENGTH_SHORT).show();
                }
            }
        }
        else
        {
            if(VERBOSE)Log.d(TAG,"Flash off");
            flashOn = false;
            flash.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_flash_on));
            TextView feature = (TextView)settingsMsgRoot.findViewById(R.id.feature);
            feature.setText(getResources().getString(R.string.flashSetting).toUpperCase());
            TextView value = (TextView)settingsMsgRoot.findViewById(R.id.value);
            value.setText(getResources().getString(R.string.flashOffMode).toUpperCase());
            ImageView heading = (ImageView)settingsMsgRoot.findViewById(R.id.heading);
            heading.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_flash_off));
            final Toast settingsMsg = Toast.makeText(getActivity().getApplicationContext(),"",Toast.LENGTH_SHORT);
            settingsMsg.setGravity(Gravity.CENTER,0,0);
            settingsMsg.setView(settingsMsgRoot);
            settingsMsg.show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1250);
                        settingsMsg.cancel();
                    }catch (InterruptedException ie){
                        ie.printStackTrace();
                    }
                }
            }).start();
        }
        cameraView.flashOnOff(flashOn);
    }

    public boolean isFlashOn()
    {
        return flashOn;
    }

    public void setFlashOn(boolean flashOn1)
    {
        flashOn = flashOn1;
    }

    public void askForPermissionAgain()
    {
        if(VERBOSE)Log.d(TAG,"permissionInterface = "+permissionInterface);
        permissionInterface.askPermission();
    }

    public void deleteLatestBadFile(){
        if(VERBOSE)Log.d(TAG, "Deleting bad file.. "+cameraView.getMediaPath());
        File badFile = new File(cameraView.getMediaPath());
        if(badFile.exists()) {
            if(badFile.delete()) {
                if(VERBOSE)Log.d(TAG, "Bad file removed");
            }
        }
    }

    public void createAndShowThumbnail(String mediaPath)
    {
        if (mediaPath == null || mediaPath.isEmpty()) {
            Log.e(TAG, "Cannot show recording thumbnail: media path is empty");
            return;
        }
        //Storing in public folder. This will ensure that the files are visible in other apps as well.
        //Use this for sharing files between apps
        final File video = new File(mediaPath);
        if (!video.isFile() || video.length() == 0) {
            Log.e(TAG, "Cannot show recording thumbnail: output file is missing or empty: " + mediaPath);
            return;
        }
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        Bitmap firstFrame = null;
        try {
            mediaMetadataRetriever.setDataSource(mediaPath);
            // Prefer a sync frame around one second for longer recordings.
            firstFrame = mediaMetadataRetriever.getFrameAtTime(
                    Constants.FIRST_SEC_MICRO, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (firstFrame == null) {
                firstFrame = mediaMetadataRetriever.getFrameAtTime(
                        0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            }
            if (firstFrame == null) {
                String duration = mediaMetadataRetriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (duration != null) {
                    long durationMs = Long.parseLong(duration);
                    firstFrame = mediaMetadataRetriever.getFrameAtTime(
                            (durationMs / 2) * 1000,
                            MediaMetadataRetriever.OPTION_CLOSEST);
                }
            }
        } catch (RuntimeException invalidVideo) {
            Log.e(TAG, "Unable to read the saved recording: " + mediaPath, invalidVideo);
        } finally {
            try {
                mediaMetadataRetriever.release();
            } catch (IOException releaseError) {
                Log.w(TAG, "Unable to release media metadata reader", releaseError);
            }
        }
        if (firstFrame == null) {
            try {
                // Use Android's file thumbnail implementation as a second
                // decoder path. This also handles short clips where the
                // metadata retriever cannot return a sync frame reliably.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    firstFrame = ThumbnailUtils.createVideoThumbnail(video,
                            new Size((int) getResources().getDimension(R.dimen.thumbnailWidth),
                                    (int) getResources().getDimension(R.dimen.thumbnailHeight)), null);
                } else {
                    firstFrame = ThumbnailUtils.createVideoThumbnail(
                            mediaPath, MediaStore.Video.Thumbnails.MINI_KIND);
                }
            } catch (IOException | RuntimeException thumbnailError) {
                Log.e(TAG, "Android thumbnail extraction failed: " + mediaPath, thumbnailError);
            }
        }
        if(firstFrame == null){
            Log.e(TAG, "No video frame could be decoded for thumbnail: " + mediaPath
                    + " (" + video.length() + " bytes)");
            addMediaToDB();
            showRecordSaved();
            return;
        }
        if(VERBOSE)Log.d(TAG,"width = "+firstFrame.getWidth()+" , height = "+firstFrame.getHeight());
        boolean isDetached=false;
        try {
            firstFrame = Bitmap.createScaledBitmap(firstFrame, (int) getResources().getDimension(R.dimen.thumbnailWidth),
                    (int) getResources().getDimension(R.dimen.thumbnailHeight), false);
        }
        catch (IllegalStateException illegal){
            if(VERBOSE)Log.d(TAG,"video fragment is already detached. ");
            isDetached=true;
        }
        showRecordSaved();
        addMediaToDB();
        if(!isDetached) {
            updateMicroThumbnailAsPerPlayer();
            microThumbnail.setVisibility(View.VISIBLE);
            thumbnail.setVisibility(View.VISIBLE);
            thumbnailParent.setVisibility(View.VISIBLE);
            thumbnail.setImageBitmap(firstFrame);
            thumbnail.setClickable(true);
            thumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openMedia();
                }
            });
        }
    }

    private void updateMicroThumbnailAsPerPlayer(){
        if(isUseFCPlayer()){
            microThumbnail.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_circle_outline));
        }
        else{
            microThumbnail.setImageDrawable(getResources().getDrawable(R.drawable.ic_external_play_circle_outline));
        }
    }

    public void addMediaToDB(){
        if (cameraView.getMediaPath() == null || cameraView.getMediaPath().isEmpty()) {
            Log.e(TAG, "Skipping media index update because the recording path is empty");
            return;
        }
        ContentValues mediaContent = new ContentValues();
        mediaContent.put("filename", cameraView.getMediaPath());
        mediaContent.put("memoryStorage", (sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true) ? "1" : "0"));
        if(VERBOSE)Log.d(TAG, "Adding to Media DB");
        getActivity().getContentResolver().insert(Uri.parse(MediaTableConstants.BASE_CONTENT_URI+"/addMedia"),mediaContent);
    }

    public void deleteFileAndRefreshThumbnail(){
        File badFile = new File(filePath);
        badFile.delete();
        if(VERBOSE)Log.d(TAG, "Bad file removed...."+filePath);
        getLatestFileIfExists();
    }

    String filePath = "";
    public void getLatestFileIfExists()
    {
        FileMedia[] medias = MediaUtil.getMediaList(getActivity().getApplicationContext(), false);
        if (medias != null && medias.length > 0) {
            if(VERBOSE)Log.d(TAG, "Latest file is = " + medias[0].getPath());
            filePath = medias[0].getPath();
                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                Bitmap vid = null;
                try {
                    mediaMetadataRetriever.setDataSource(filePath);
                    vid = mediaMetadataRetriever.getFrameAtTime(Constants.FIRST_SEC_MICRO);
                    if (vid == null) {
                        vid = mediaMetadataRetriever.getFrameAtTime(0,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    }
                } catch (RuntimeException runtime) {
                    Log.w(TAG, "Unable to load latest video thumbnail: " + filePath, runtime);
                } finally {
                    try {
                        mediaMetadataRetriever.release();
                    } catch (IOException releaseError) {
                        Log.w(TAG, "Unable to release media metadata reader", releaseError);
                    }
                }
                if(VERBOSE)Log.d(TAG, "Vid = "+vid);
                //If video cannot be played for whatever reason
                if (vid != null) {
                    vid = Bitmap.createScaledBitmap(vid, (int) getResources().getDimension(R.dimen.thumbnailWidth),
                            (int) getResources().getDimension(R.dimen.thumbnailHeight), false);
                    thumbnail.setImageBitmap(vid);
                    updateMicroThumbnailAsPerPlayer();
                    microThumbnail.setVisibility(View.VISIBLE);
                    if(VERBOSE)Log.d(TAG, "set as image bitmap");
                    thumbnail.setClickable(true);
                    thumbnail.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            openMedia();
                        }
                    });
                } else {
                    // Leave an existing video intact and display the default
                    // thumbnail if this device cannot decode a preview frame.
                    microThumbnail.setVisibility(View.INVISIBLE);
                    setPlaceholderThumbnail();
                }
        }
        else{
            microThumbnail.setVisibility(View.INVISIBLE);
            setPlaceholderThumbnail();
        }
    }

    public void setPlaceholderThumbnail()
    {
        thumbnail.setImageDrawable(getResources().getDrawable(R.drawable.placeholder));
        thumbnail.setClickable(false);
    }

    private void openMedia()
    {
        setCameraClose();
        Intent mediaIntent = new Intent(getActivity().getApplicationContext(), MediaActivity.class);
        SharedPreferences.Editor mediaLocEdit = sharedPreferences.edit();
        String mediaLocValue = sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true) ?
                getResources().getString(R.string.phoneLocation) : getResources().getString(R.string.sdcardLocation);
        mediaLocEdit.putString(Constants.MEDIA_LOCATION_VIEW_SELECT, mediaLocValue);
        mediaLocEdit.commit();
        if(controlVisbilityPreference == null){
            controlVisbilityPreference = (ControlVisbilityPreference)getApplicationContext();
        }
        controlVisbilityPreference.setFromGallery(false);
        startActivity(mediaIntent);
    }

    private void setCameraClose()
    {
        //Set this if you want to continue when the launcher activity resumes.
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("startCamera",false);
        editor.commit();
    }

    private void setCameraQuit()
    {
        //Set this if you want to quit the app when launcher activity resumes.
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("startCamera",true);
        editor.commit();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(VERBOSE)Log.d(TAG,"Detached");
    }

    @Override
    public void onResume() {
        super.onResume();
        if(VERBOSE)Log.d(TAG,"onResume");
        if(cameraView!=null){
            cameraView.setVisibility(View.VISIBLE);
        }
        orientationEventListener.enable();
        mediaFilters.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        mediaFilters.addDataScheme("file");
        if(getActivity() != null){
            getActivity().registerReceiver(sdCardEventReceiver, mediaFilters);
        }
        sdCardUnavailWarned = false;
        checkForSDCard();
    }

    @Override
    public void onDestroy() {
        if(VERBOSE)Log.d(TAG,"Fragment destroy...app is being minimized");
        setCameraClose();
        super.onDestroy();
    }

    @Override
    public void onStop() {
        if(VERBOSE)Log.d(TAG,"Fragment stop...app is out of focus");
        super.onStop();
    }

    @Override
    public void onPause() {
        if(VERBOSE)Log.d(TAG,"Fragment pause....app is being quit");
        setCameraQuit();
        if(cameraView!=null){
            if(VERBOSE)Log.d(TAG,"cameraview onpause visibility= "+cameraView.getWindowVisibility());
            if(cameraView.getWindowVisibility() == View.VISIBLE){
                cameraView.setVisibility(View.GONE);
            }
        }
        orientationEventListener.disable();
        if(getActivity() != null){
            getActivity().unregisterReceiver(sdCardEventReceiver);
        }
        super.onPause();
    }
}
