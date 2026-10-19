package com.flipcam;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentTransaction;

import com.flipcam.constants.Constants;
import com.flipcam.model.Dimension;
import com.flipcam.util.GLUtil;
import com.flipcam.view.CameraView;
import com.flipcam.view.PinchZoomGestureListener;
import com.flipcam.util.SDCardUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class CameraActivity extends AppCompatActivity implements VideoFragment.PermissionInterface, PhotoFragment.PhotoPermission,VideoFragment.SwitchInterface,
PhotoFragment.SwitchPhoto, VideoFragment.LowestThresholdCheckForVideoInterface, PhotoFragment.LowestThresholdCheckForPictureInterface
{

    private static final String TAG = "CameraActivity";
    VideoFragment videoFragment = null;
    PhotoFragment photoFragment = null;
    View warningMsgRoot;
    Dialog warningMsg;
    Button okButton;
    LayoutInflater layoutInflater;
    SharedPreferences sharedPreferences;
    boolean VERBOSE = false;
    View settingsRootView;
    Dialog settingsDialog;
    ImageView brightness;
    ImageView toggleAudio;
    TextView storageDestination;
    TextView videoResolution;
    LinearLayout instantSettingsParent;
    ControlVisbilityPreference controlVisbilityPreference;
    boolean fromGallery = false;
    PinchZoomGestureListener pinchZoomGestureListener;
    ScaleGestureDetector scaleGestureDetector;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(VERBOSE)Log.d(TAG,"onCreate");
        setContentView(R.layout.activity_camera);
        brightness = (ImageView)findViewById(R.id.brightness);
        toggleAudio = findViewById(R.id.toggleAudio);
        storageDestination = findViewById(R.id.storageDestination);
        videoResolution = findViewById(R.id.resInfo);
        instantSettingsParent = findViewById(R.id.instantSettingsParent);
        controlVisbilityPreference = (ControlVisbilityPreference)getApplicationContext();
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(getResources().getColor(R.color.settingsBarColor));
        getWindow().setNavigationBarColor(getResources().getColor(R.color.settingsBarColor));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }
        if(savedInstanceState == null) {
            //Start with video fragment
            showVideoFragment();
            controlVisbilityPreference.setBrightnessLevel(Constants.NORMAL_BRIGHTNESS);
            controlVisbilityPreference.setBrightnessProgress(0.0f);
        }
        layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        warningMsgRoot = layoutInflater.inflate(R.layout.warning_message,null);
        warningMsg = new Dialog(this);
        settingsRootView = layoutInflater.inflate(R.layout.brightness_settings, null);
        settingsDialog = new Dialog(this);
        sharedPreferences = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        updateStorageDestinationLabel();
        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            fromGallery = bundle.getBoolean("fromGallery");
        }
        setPinchZoomScaleListener(videoFragment!=null ? videoFragment : null, photoFragment!=null ? photoFragment : null);
    }

    private void setPinchZoomScaleListener(VideoFragment videoFragment, PhotoFragment photoFragment){
        if(pinchZoomGestureListener != null){
            pinchZoomGestureListener = null;
        }
        pinchZoomGestureListener = new PinchZoomGestureListener(getApplicationContext(), videoFragment,
                photoFragment);
        scaleGestureDetector = new ScaleGestureDetector(getApplicationContext(), pinchZoomGestureListener);
    }

    public PinchZoomGestureListener getPinchZoomGestureListener(){
        return pinchZoomGestureListener;
    }

    void displaySDCardNotDetectMessage(){
        if(VERBOSE)Log.d(TAG, "displaySDCardNotDetectMessage");
        //The below variable is needed to check if there was SD Card removed in MediaActivity which caused the control
        // to come here.
        if(fromGallery) {
            //Show SD Card not detected, please insert sd card to try again.
            TextView warningTitle = (TextView) warningMsgRoot.findViewById(R.id.warningTitle);
            warningTitle.setText(getResources().getString(R.string.sdCardNotDetectTitle));
            TextView warningText = (TextView) warningMsgRoot.findViewById(R.id.warningText);
            warningText.setText(getResources().getString(R.string.sdCardNotDetectMessage));
            okButton = (Button) warningMsgRoot.findViewById(R.id.okButton);
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    warningMsg.dismiss();
                    videoFragment.getLatestFileIfExists();
                }
            });
            warningMsg.setContentView(warningMsgRoot);
            warningMsg.setCancelable(false);
            warningMsg.show();
            if(VERBOSE)Log.d(TAG, "MESSAGE SHOWN");
        }
    }

    public String doesSDCardExist(){
        String sdcardpath = sharedPreferences.getString(Constants.SD_CARD_PATH, "");
        try {
            String filename = "/doesSDCardExist_"+String.valueOf(System.currentTimeMillis()).substring(0,5);
            sdcardpath += filename;
            final String sdCardFilePath = sdcardpath;
            final FileOutputStream createTestFile = new FileOutputStream(sdcardpath);
            if(VERBOSE)Log.d(TAG, "Able to create file... SD Card exists");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    File testfile = new File(sdCardFilePath);
                    try {
                        createTestFile.close();
                        testfile.delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (FileNotFoundException e) {
            if(VERBOSE)Log.d(TAG, "Unable to create file... SD Card NOT exists..... "+e.getMessage());
            return null;
        }
        return sharedPreferences.getString(Constants.SD_CARD_PATH, "");
    }

    public void chooseStorageDestination(View view) {
        if (videoFragment != null && videoFragment.getCameraView() != null
                && videoFragment.getCameraView().isRecord()) {
            Toast.makeText(this, R.string.stopRecordingToChangeStorage, Toast.LENGTH_SHORT).show();
            return;
        }
        boolean savesToPhone = sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
        String[] destinations = {
                getString(R.string.storagePhoneShort),
                getString(R.string.storageSdShort)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.saveMedia)
                .setSingleChoiceItems(destinations, savesToPhone ? 0 : 1, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) {
                        sharedPreferences.edit()
                                .putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)
                                .apply();
                        updateStorageDestinationLabel();
                    } else {
                        selectSdCardDestination();
                    }
                })
                .setNegativeButton(R.string.cancelBtn, null)
                .show();
    }

    private void selectSdCardDestination() {
        String sdCardPath = SDCardUtil.doesSDCardExist(getApplicationContext());
        if (sdCardPath == null) {
            fallBackToPhoneStorage(R.string.sdCardNotDetectTitle, R.string.sdCardNotPresentForRecord);
            return;
        }
        if (!SDCardUtil.isPathWritable(sdCardPath)) {
            fallBackToPhoneStorage(R.string.sdCardWriteError, R.string.sdCardWriteErrorMessage);
            return;
        }

        sharedPreferences.edit()
                .putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, false)
                .putString(Constants.SD_CARD_PATH, sdCardPath)
                .apply();
        updateStorageDestinationLabel();
    }

    private void fallBackToPhoneStorage(int titleId, int messageId) {
        sharedPreferences.edit()
                .putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)
                .apply();
        updateStorageDestinationLabel();
        new AlertDialog.Builder(this)
                .setTitle(titleId)
                .setMessage(messageId)
                .setPositiveButton(R.string.okBtn, null)
                .show();
    }

    public void updateStorageDestinationLabel() {
        if (storageDestination == null || sharedPreferences == null) return;
        boolean savesToPhone = sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
        storageDestination.setText(savesToPhone ? R.string.storagePhoneShort : R.string.storageSdShort);
        storageDestination.setContentDescription(getString(R.string.storageDestinationDescription,
                getString(savesToPhone ? R.string.storagePhoneShort : R.string.storageSdShort)));
    }

    public void chooseVideoResolution(View view) {
        if (videoFragment == null || videoFragment.getCameraView() == null) return;
        CameraView cameraView = videoFragment.getCameraView();
        if (cameraView.isRecord()) {
            Toast.makeText(this, R.string.stopRecordingToChangeResolution, Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> supportedResolutions = cameraView.getSupportedVideoResolutions();
        if (supportedResolutions.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.videoResolutionUnavailable)
                    .setPositiveButton(R.string.okBtn, null)
                    .show();
            return;
        }

        TreeSet<Dimension> sortedSizes = new TreeSet<>();
        for (String resolution : supportedResolutions) {
            String[] dimensions = resolution.split(" X ");
            if (dimensions.length == 2) {
                sortedSizes.add(new Dimension(Integer.parseInt(dimensions[0]),
                        Integer.parseInt(dimensions[1])));
            }
        }
        List<String> options = new ArrayList<>();
        for (Dimension size : sortedSizes) options.add(size.toString());
        String[] entries = options.toArray(new String[0]);
        SharedPreferences videoPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String selectedResolution = videoPreferences.getString(Constants.SELECT_VIDEO_RESOLUTION, null);
        int selectedIndex = options.indexOf(selectedResolution);

        new AlertDialog.Builder(this)
                .setTitle(R.string.videoResolutionHeading)
                .setSingleChoiceItems(entries, selectedIndex, (dialog, which) -> {
                    String newResolution = entries[which];
                    dialog.dismiss();
                    if (newResolution.equals(selectedResolution)) return;

                    videoPreferences.edit()
                            .putString(Constants.SELECT_VIDEO_RESOLUTION, newResolution)
                            .apply();
                    try {
                        cameraView.applyVideoResolution();
                    } catch (RuntimeException exception) {
                        videoPreferences.edit()
                                .putString(Constants.SELECT_VIDEO_RESOLUTION, selectedResolution)
                                .apply();
                        if (selectedResolution != null) cameraView.applyVideoResolution();
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.videoResolutionUnavailable)
                                .setPositiveButton(R.string.okBtn, null)
                                .show();
                    }
                })
                .setNegativeButton(R.string.cancelBtn, null)
                .show();
    }

    private void setVideoResolutionControlEnabled(boolean enabled) {
        if (videoResolution == null) return;
        videoResolution.setOnClickListener(enabled ? this::chooseVideoResolution : null);
        videoResolution.setClickable(enabled);
        videoResolution.setFocusable(enabled);
        videoResolution.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, null, enabled ? getDrawable(R.drawable.ic_dropdown_arrow) : null, null);
        if (enabled) videoResolution.setBackgroundResource(R.drawable.ripple);
        else videoResolution.setBackground(null);
        videoResolution.setContentDescription(enabled
                ? getString(R.string.videoResolutionSelectorDescription)
                : null);
    }

    public void toggleMicrophone(View view){
        SharedPreferences micPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor prefsEditor = micPrefs.edit();
        boolean noAudio;
        if(!micPrefs.getBoolean(Constants.NO_AUDIO_MSG, false)){
            //Disable audio
            toggleAudio.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_mic_off));
            toggleAudio.setContentDescription(getString(R.string.microphoneMuted));
            prefsEditor.putBoolean(Constants.NO_AUDIO_MSG, true);
            noAudio = true;
        }
        else{
            //Enable audio
            toggleAudio.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_mic_on));
            toggleAudio.setContentDescription(getString(R.string.microphoneEnabled));
            prefsEditor.putBoolean(Constants.NO_AUDIO_MSG, false);
            noAudio = false;
        }
        prefsEditor.commit();
        showToggleAudioMessage(noAudio);
    }

    public void showToggleAudioMessage(boolean noAudio)
    {
        LinearLayout noAudioLayout = new LinearLayout(this);
        noAudioLayout.setPadding(30,50,30,50);
        noAudioLayout.setGravity(Gravity.CENTER);
        noAudioLayout.setOrientation(LinearLayout.VERTICAL);
        noAudioLayout.setBackgroundColor(getResources().getColor(R.color.savedMsg));
        TextView noAudioText = new TextView(this);
        if(noAudio) {
            noAudioText.setTextColor(getResources().getColor(R.color.audioDisable));
            noAudioText.setText(getResources().getString(R.string.audioDisableMessage));
        }
        else{
            noAudioText.setTextColor(getResources().getColor(R.color.audioEnable));
            noAudioText.setText(getResources().getString(R.string.audioEnableMessage));
        }
        noAudioLayout.addView(noAudioText);
        final Toast showCompleted = Toast.makeText(this.getApplicationContext(),"",Toast.LENGTH_SHORT);
        showCompleted.setGravity(Gravity.CENTER,0,0);
        showCompleted.setView(noAudioLayout);
        showCompleted.show();
        new Thread(() -> {
        try {
                Thread.sleep(1500);
                showCompleted.cancel();
            }
            catch (InterruptedException ie){
                ie.printStackTrace();
            }
        }).start();
    }
    public void openBrightnessPopup(View view){
        TextView header = (TextView)settingsRootView.findViewById(R.id.timerText);
        header.setText(getResources().getString(R.string.brightnessHeading));
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        settingsDialog.setContentView(settingsRootView);
        settingsDialog.setCancelable(true);
        WindowManager.LayoutParams lp = settingsDialog.getWindow().getAttributes();
        lp.dimAmount = 0.0f;
        lp.width = (int)(size.x * 0.8);
        final SeekBar brightnessBar = (SeekBar)settingsRootView.findViewById(R.id.brightnessBar);
        brightnessBar.setMax(10);
        brightnessBar.setProgress(controlVisbilityPreference.getBrightnessLevel());
        brightnessBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(controlVisbilityPreference.getBrightnessLevel());
            }
        });
        Button increaseBrightness = (Button)settingsRootView.findViewById(R.id.increaseBrightness);
        increaseBrightness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isVideo()) {
                    if(GLUtil.colorVal < 0.25f) {
                        GLUtil.colorVal += 0.05f;
                        brightnessBar.incrementProgressBy(1);
                        controlVisbilityPreference.setBrightnessLevel(brightnessBar.getProgress());
                    }
                    else{
                        GLUtil.colorVal = 0.25f;
                    }
                    controlVisbilityPreference.setBrightnessProgress(GLUtil.colorVal);
                }
            }
        });
        Button decreaseBrightness = (Button)settingsRootView.findViewById(R.id.setTimer);
        decreaseBrightness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isVideo()){
                    if(GLUtil.colorVal > -0.25f) {
                        GLUtil.colorVal -= 0.05f;
                        brightnessBar.incrementProgressBy(-1);
                        controlVisbilityPreference.setBrightnessLevel(brightnessBar.getProgress());
                    }
                    else{
                        GLUtil.colorVal = -0.25f;
                    }
                    controlVisbilityPreference.setBrightnessProgress(GLUtil.colorVal);
                }
            }
        });
        settingsDialog.getWindow().setBackgroundDrawableResource(R.color.backColorSettingPopup);
        settingsDialog.show();
    }

    private boolean isVideo(){
        return videoFragment!=null;
    }

    @Override
    public void switchToPhoto() {
        showPhotoFragment();
    }

    @Override
    public void switchToVideo() {
        showVideoFragment();
    }

    public void showVideoFragment()
    {
        setVideoResolutionControlEnabled(true);
        if(videoFragment == null) {
            if(VERBOSE)Log.d(TAG,"creating videofragment");
            videoFragment = VideoFragment.newInstance();
            videoFragment.setApplicationContext(getApplicationContext());
        }
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.cameraPreview, videoFragment).commit();
        if(VERBOSE)Log.d(TAG, "brightnessLevel SET to = "+controlVisbilityPreference.getBrightnessLevel());
        instantSettingsParent.setVisibility(View.VISIBLE);
        setPinchZoomScaleListener(videoFragment, null);
    }

    public void showPhotoFragment()
    {
        setVideoResolutionControlEnabled(false);
        instantSettingsParent.setVisibility(View.GONE);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if(photoFragment == null) {
            if(VERBOSE)Log.d(TAG,"creating photofragment");
            photoFragment = PhotoFragment.newInstance();
            photoFragment.setApplicationContext(getApplicationContext());
        }
        setPinchZoomScaleListener(null, photoFragment);
        fragmentTransaction.replace(R.id.cameraPreview, photoFragment).commit();
        if(VERBOSE)Log.d(TAG,"photofragment added");
        SharedPreferences.Editor settingsEditor = sharedPreferences.edit();
        if(!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
            //Check if SD Card exists
            if(doesSDCardExist() == null){
                settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
                settingsEditor.commit();
                updateStorageDestinationLabel();
                TextView warningTitle = (TextView)warningMsgRoot.findViewById(R.id.warningTitle);
                warningTitle.setText(getResources().getString(R.string.sdCardRemovedTitle));
                TextView warningText = (TextView)warningMsgRoot.findViewById(R.id.warningText);
                warningText.setText(getResources().getString(R.string.sdCardNotPresentForRecord));
                okButton = (Button)warningMsgRoot.findViewById(R.id.okButton);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        warningMsg.dismiss();
                        photoFragment.getLatestFileIfExists();
                    }
                });
                warningMsg.setContentView(warningMsgRoot);
                warningMsg.setCancelable(false);
                warningMsg.show();
            }
        }
        else if(!checkIfPhoneMemoryIsBelowLowThreshold() && !sharedPreferences.getBoolean(Constants.PHONE_MEMORY_DISABLE, true)) {
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            int memoryThreshold = Integer.parseInt(sharedPreferences.getString(Constants.PHONE_MEMORY_LIMIT, ""));
            String memoryMetric = sharedPreferences.getString(Constants.PHONE_MEMORY_METRIC, "");
            StatFs storageStat = new StatFs(Environment.getDataDirectory().getPath());
            long memoryValue = 0;
            String metric = "";
            switch (memoryMetric) {
                case Constants.METRIC_MB:
                    memoryValue = (memoryThreshold * (long) Constants.MEGA_BYTE);
                    metric = Constants.METRIC_MB;
                    break;
                case Constants.METRIC_GB:
                    memoryValue = (memoryThreshold * (long) Constants.GIGA_BYTE);
                    metric = Constants.METRIC_GB;
                    break;
            }
            if(VERBOSE)Log.d(TAG, "memory value = " + memoryValue);
            if(VERBOSE)Log.d(TAG, "Avail mem = " + storageStat.getAvailableBytes());
            if (storageStat.getAvailableBytes() < memoryValue) {
                LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View thresholdExceededRoot = layoutInflater.inflate(R.layout.threshold_exceeded, null);
                final Dialog thresholdDialog = new Dialog(this);
                TextView memoryLimitMsg = (TextView) thresholdExceededRoot.findViewById(R.id.memoryLimitMsg);
                final CheckBox disableThreshold = (CheckBox) thresholdExceededRoot.findViewById(R.id.disableThreshold);
                Button okButton = (Button) thresholdExceededRoot.findViewById(R.id.okButton);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(VERBOSE)Log.d(TAG, "disableThreshold.isChecked = " + disableThreshold.isChecked());
                        if (disableThreshold.isChecked()) {
                            editor.remove(Constants.PHONE_MEMORY_LIMIT);
                            editor.remove(Constants.PHONE_MEMORY_METRIC);
                            editor.putBoolean(Constants.PHONE_MEMORY_DISABLE, true);
                            editor.commit();
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.minimumThresholdDisabled), Toast.LENGTH_LONG).show();
                        }
                        thresholdDialog.dismiss();
                    }
                });
                StringBuilder memThreshold = new StringBuilder(memoryThreshold + "");
                memThreshold.append(" ");
                memThreshold.append(metric);
                if(VERBOSE)Log.d(TAG, "memory threshold for display = " + memThreshold);
                memoryLimitMsg.setText(getResources().getString(R.string.thresholdLimitExceededMsg, memThreshold.toString()));
                thresholdDialog.setContentView(thresholdExceededRoot);
                thresholdDialog.setCancelable(false);
                thresholdDialog.show();
            }
        }
    }

    public boolean checkIfPhoneMemoryIsBelowLowThreshold(){
        if(sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
            StatFs storageStat = new StatFs(Environment.getDataDirectory().getPath());
            int lowestThreshold = getResources().getInteger(R.integer.minimumMemoryWarning);
            long lowestMemory = lowestThreshold * (long)Constants.MEGA_BYTE;
            if(VERBOSE)Log.d(TAG, "lowestMemory = "+lowestMemory);
            if(VERBOSE)Log.d(TAG, "avail mem = "+storageStat.getAvailableBytes());
            if(storageStat.getAvailableBytes() < lowestMemory){
                return true;
            }
            else{
                return false;
            }
        }
        else{
            return false;
        }
    }

    @Override
    public boolean checkIfPhoneMemoryIsBelowLowestThresholdForPicture() {
        return checkIfPhoneMemoryIsBelowLowThreshold();
    }

    @Override
    public boolean checkIfPhoneMemoryIsBelowLowestThresholdForVideo() {
        return checkIfPhoneMemoryIsBelowLowThreshold();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(VERBOSE)Log.d(TAG,"onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(VERBOSE)Log.d(TAG,"onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Reset No Audio switch to enable audio by default when user exits the app.
        SharedPreferences.Editor defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        defaultSharedPrefs.remove(Constants.NO_AUDIO_MSG);
        defaultSharedPrefs.commit();
        Log.d(TAG,"onDestroy");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(VERBOSE)Log.d(TAG,"onResume");
        if (sharedPreferences != null
                && !sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)) {
            String sdCardPath = SDCardUtil.doesSDCardExist(getApplicationContext());
            if (sdCardPath == null) {
                fallBackToPhoneStorage(R.string.sdCardNotDetectTitle, R.string.sdCardNotPresentForRecord);
            } else if (!SDCardUtil.isPathWritable(sdCardPath)) {
                fallBackToPhoneStorage(R.string.sdCardWriteError, R.string.sdCardWriteErrorMessage);
            } else {
                sharedPreferences.edit().putString(Constants.SD_CARD_PATH, sdCardPath).apply();
            }
        }
        updateStorageDestinationLabel();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(VERBOSE)Log.d(TAG,"onPause");
        if(this.videoFragment!=null) {
            this.videoFragment.getZoomBar().setProgress(0);
        }
        else if(this.photoFragment!=null) {
            this.photoFragment.getZoomBar().setProgress(0);
        }
        pinchZoomGestureListener.setProgress(0);
    }

    @Override
    public void askPermission() {
        askCameraPermission();
    }

    @Override
    public void askPhotoPermission() {
        askCameraPermission();
    }

    public void askCameraPermission(){
        if(VERBOSE)Log.d(TAG,"start permission act to get permissions");
        Intent permission = new Intent(this,PermissionActivity.class);
        permission.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(permission);
        finish();
    }
}
