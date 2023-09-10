package com.flipcam;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.flipcam.constants.Constants;
import com.flipcam.util.GLUtil;
import com.flipcam.view.PinchZoomGestureListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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
        instantSettingsParent = findViewById(R.id.instantSettingsParent);
        controlVisbilityPreference = (ControlVisbilityPreference)getApplicationContext();
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        warningMsgRoot = layoutInflater.inflate(R.layout.warning_message,null);
        warningMsg = new Dialog(this);
        if(savedInstanceState == null) {
            //Start with video fragment
            showVideoFragment();
            controlVisbilityPreference.setBrightnessLevel(Constants.NORMAL_BRIGHTNESS);
            controlVisbilityPreference.setBrightnessProgress(0.0f);
        }
        settingsRootView = layoutInflater.inflate(R.layout.brightness_settings, null);
        settingsDialog = new Dialog(this);
        sharedPreferences = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
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

    public void goToSettings(View view){
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
        overridePendingTransition(R.anim.slide_from_right,R.anim.slide_to_left);
    }

    public void toggleMicrophone(View view){
        SharedPreferences micPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor prefsEditor = micPrefs.edit();
        boolean noAudio;
        if(!micPrefs.getBoolean(Constants.NO_AUDIO_MSG, false)){
            //Disable audio
            toggleAudio.setImageDrawable(getResources().getDrawable(R.drawable.microphone_mute_sound_icon));
            prefsEditor.putBoolean(Constants.NO_AUDIO_MSG, true);
            noAudio = true;
        }
        else{
            //Enable audio
            toggleAudio.setImageDrawable(getResources().getDrawable(R.drawable.microphone_music_sound_icon));
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
        if(videoFragment == null) {
            Log.d(TAG,"creating videofragment");
            videoFragment = VideoFragment.newInstance();
            videoFragment.setApplicationContext(getApplicationContext());
        }
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.cameraPreview, videoFragment).commit();
        if(Constants.isAndroidVersionTAndAbove()) {
            if(controlVisbilityPreference.isShowUserManual()) {
                //Show User Manual Dialog
                TextView warningTitle = (TextView) warningMsgRoot.findViewById(R.id.warningTitle);
                warningTitle.setText(getResources().getString(R.string.welcomeTitle));
                TextView warningText = (TextView) warningMsgRoot.findViewById(R.id.warningText);
                warningText.setText(getResources().getString(R.string.welcomeToFlipCam));
                warningText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                //Remove warning sign
                ImageView warningSign = (ImageView) warningMsgRoot.findViewById(R.id.warningSign);
                warningSign.setVisibility(View.GONE);
                LinearLayout buttonLayout = (LinearLayout) warningMsgRoot.findViewById(R.id.buttonLayout);
                buttonLayout.removeAllViews();
                //Add User Manual button
                Button userManualBtn = new Button(this);
                userManualBtn.setText(R.string.openUMButton);
                userManualBtn.setOnClickListener((view) -> {
                    //Go to User Manual activity
                    Intent userManualIntent = new Intent(this, UserManualActivity.class);
                    startActivity(userManualIntent);
                });
                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams
                        (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                buttonParams.setMargins(50, 0, 10, 0);
                buttonParams.weight = 0.5f;
                userManualBtn.setBackgroundColor(getResources().getColor(R.color.thumbnailPlaceholder));
                userManualBtn.setTextColor(getResources().getColor(R.color.turqoise));
                userManualBtn.setPadding(5,0,5,0);
                userManualBtn.setLayoutParams(buttonParams);
                buttonLayout.addView(userManualBtn);

                //Add Close button
                Button closeBtn = new Button(this);
                closeBtn.setText(R.string.closeButton);
                closeBtn.setOnClickListener((view -> {
                    warningMsg.dismiss();
                }));
                buttonParams = new LinearLayout.LayoutParams
                        (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                buttonParams.setMargins(10, 0, 50, 0);
                buttonParams.weight = 0.5f;
                closeBtn.setLayoutParams(buttonParams);
                closeBtn.setBackgroundColor(getResources().getColor(R.color.thumbnailPlaceholder));
                closeBtn.setTextColor(getResources().getColor(R.color.turqoise));
                closeBtn.setPadding(5,0,5,0);
                buttonLayout.addView(closeBtn);

                //Add text for Do not show again
                CheckBox doNotShowAgain = new CheckBox(this);
                doNotShowAgain.setText(R.string.donotshowUserManualMessage);
                doNotShowAgain.setButtonTintList(ColorStateList.valueOf(getResources().getColor(R.color.turqoise)));
                doNotShowAgain.setTextColor(getResources().getColor(R.color.turqoise));
                doNotShowAgain.setOnClickListener((view) -> {
                    if (controlVisbilityPreference.isShowUserManual()) {
                        controlVisbilityPreference.setShowUserManual(false);
                    } else {
                        controlVisbilityPreference.setShowUserManual(true);
                    }
                });
                //Add new Linear Layout for textview
                LinearLayout checkboxLayout = new LinearLayout(this);
                checkboxLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                LinearLayout.LayoutParams checkboxParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                checkboxParams.setMargins(20, 10, 20, 25);
                doNotShowAgain.setLayoutParams(checkboxParams);
                checkboxLayout.addView(doNotShowAgain);

                //Get reference to Parent Layout
                LinearLayout warningParent = (LinearLayout) warningMsgRoot.findViewById(R.id.warningParent);
                warningParent.addView(checkboxLayout);

                warningMsg.setContentView(warningMsgRoot);
                warningMsg.setCancelable(false);
                warningMsg.show();
            }
        }
        else {
            if(controlVisbilityPreference.isShowUserManual()){
                controlVisbilityPreference.setShowUserManual(false);
                Intent userManualIntent = new Intent(this, UserManualActivity.class);
                startActivity(userManualIntent);
            }
        }
        if(VERBOSE)Log.d(TAG, "brightnessLevel SET to = "+controlVisbilityPreference.getBrightnessLevel());
        instantSettingsParent.setVisibility(View.VISIBLE);
        setPinchZoomScaleListener(videoFragment, null);
    }

    public void showPhotoFragment()
    {
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
