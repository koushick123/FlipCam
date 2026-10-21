package com.flipcam;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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

public class CameraActivity extends AppCompatActivity implements VideoFragment.PermissionInterface,
VideoFragment.LowestThresholdCheckForVideoInterface
{

    private static final String TAG = "CameraActivity";
    VideoFragment videoFragment = null;
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
    private LinearLayout speedDialMenu;
    private ImageButton speedDialToggle;
    private TextView storageActionLabel;
    private TextView microphoneActionLabel;
    private ImageView speedDialFlashIcon;
    private boolean speedDialExpanded;
    private static final String LEFT_HANDED_PREVIEW = "LeftHandedPreview";
    private TextView alignmentActionLabel;
    private ImageView alignmentActionIcon;
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
        storageDestination = null;
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
        setPinchZoomScaleListener(videoFragment);
    }

    private void setPinchZoomScaleListener(VideoFragment videoFragment){
        if(pinchZoomGestureListener != null){
            pinchZoomGestureListener = null;
        }
        pinchZoomGestureListener = new PinchZoomGestureListener(getApplicationContext(), videoFragment);
        scaleGestureDetector = new ScaleGestureDetector(getApplicationContext(), pinchZoomGestureListener);
    }

    public void setupCameraSpeedDial() {
        brightness = findViewById(R.id.brightness);
        toggleAudio = findViewById(R.id.toggleAudio);
        instantSettingsParent = findViewById(R.id.instantSettingsParent);
        instantSettingsParent.removeAllViews();
        instantSettingsParent.setOrientation(LinearLayout.VERTICAL);
        instantSettingsParent.setGravity(Gravity.BOTTOM | Gravity.END);

        speedDialMenu = new LinearLayout(this);
        speedDialMenu.setOrientation(LinearLayout.VERTICAL);
        speedDialMenu.setGravity(Gravity.END);
        speedDialMenu.setVisibility(View.GONE);

        addSpeedDialAction(R.string.speedDialResolution, R.drawable.ic_speed_dial_resolution,
                null, getString(R.string.speedDialResolutionDescription),
                () -> chooseVideoResolution(null));
        storageActionLabel = addSpeedDialAction(R.string.speedDialStoragePhone,
                R.drawable.ic_storage_destination, null,
                getString(R.string.speedDialStorageDescription),
                () -> chooseStorageDestination(null));

        boolean micMuted = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.NO_AUDIO_MSG, false);
        toggleAudio.setImageResource(micMuted
                ? R.drawable.ic_video_mic_off : R.drawable.ic_video_mic_on);
        toggleAudio.setContentDescription(getString(micMuted
                ? R.string.microphoneMuted : R.string.microphoneEnabled));
        microphoneActionLabel = addSpeedDialAction(
                micMuted ? R.string.speedDialMicOff : R.string.speedDialMicOn,
                0, toggleAudio, getString(R.string.speedDialMicDescription),
                () -> toggleMicrophone(null));
        TextView flashActionLabel = addSpeedDialAction(R.string.speedDialFlash,
                R.drawable.ic_video_flash_on, null,
                getString(R.string.speedDialFlashDescription),
                () -> {
                    if (videoFragment != null) videoFragment.toggleFlashFromSpeedDial();
                });
        View flashRow = (View) flashActionLabel.getParent();
        View flashIconHolder = ((ViewGroup) flashRow).getChildAt(1);
        speedDialFlashIcon = (ImageView) ((ViewGroup) flashIconHolder).getChildAt(0);
        if (videoFragment != null) updateSpeedDialFlashIcon(videoFragment.isFlashOn());
        brightness.setImageResource(R.drawable.ic_video_brightness);
        addSpeedDialAction(R.string.speedDialBrightness, R.drawable.ic_video_brightness,
                brightness, getString(R.string.brightnessContentDescription),
                () -> openBrightnessPopup(null));

        alignmentActionLabel = addSpeedDialAction(R.string.speedDialLeftAlign,
                R.drawable.ic_align_left, null, getString(R.string.speedDialLeftAlign), () -> {});
        ViewGroup alignmentRow = (ViewGroup) alignmentActionLabel.getParent();
        alignmentActionIcon = (ImageView) ((ViewGroup) alignmentRow.getChildAt(1)).getChildAt(0);
        // Moving the menu keeps it expanded and preserves the existing action views.
        alignmentRow.setOnClickListener(view -> {
            getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE).edit()
                    .putBoolean(LEFT_HANDED_PREVIEW, !isLeftHandedPreview()).apply();
            applyPreviewHandedness();
        });

        speedDialToggle = new ImageButton(this);
        speedDialToggle.setImageResource(R.drawable.ic_speed_dial_settings);
        speedDialToggle.setColorFilter(Color.WHITE);
        speedDialToggle.setScaleType(ImageView.ScaleType.FIT_CENTER);
        speedDialToggle.setPadding(dp(14), dp(14), dp(14), dp(14));
        speedDialToggle.setBackground(createSpeedDialCircleBackground(true));
        speedDialToggle.setElevation(dp(6));
        speedDialToggle.setContentDescription(getString(R.string.speedDialOpenDescription));
        speedDialToggle.setOnClickListener(view -> setCameraSpeedDialExpanded(!speedDialExpanded));
        instantSettingsParent.addView(speedDialMenu, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        instantSettingsParent.addView(speedDialToggle,
                new LinearLayout.LayoutParams(dp(52), dp(52)));
        updateStorageDestinationLabel();
        applyPreviewHandedness();
    }

    public boolean isLeftHandedPreview() {
        return getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE)
                .getBoolean(LEFT_HANDED_PREVIEW, false);
    }

    private void applyPreviewHandedness() {
        boolean left = isLeftHandedPreview();
        int side = left ? Gravity.LEFT : Gravity.RIGHT;
        View controls = findViewById(R.id.cameraRightControls);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) controls.getLayoutParams();
        params.gravity = Gravity.BOTTOM | side;
        params.setMarginStart(dp(12));
        params.setMarginEnd(dp(12));
        controls.setLayoutParams(params);
        ((LinearLayout) controls).setGravity(side);
        instantSettingsParent.setGravity(Gravity.BOTTOM | side);
        speedDialMenu.setGravity(side);
        // Reorder labels and icons without mirroring text or the camera surface.
        for (int i = 0; i < speedDialMenu.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) speedDialMenu.getChildAt(i);
            View label = row.getChildAt(0) instanceof TextView ? row.getChildAt(0) : row.getChildAt(1);
            View iconHolder = row.getChildAt(0) instanceof FrameLayout ? row.getChildAt(0) : row.getChildAt(1);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            row.setGravity(Gravity.CENTER_VERTICAL | side);
            if ((left && row.getChildAt(0) != iconHolder) || (!left && row.getChildAt(0) != label)) {
                row.removeView(iconHolder);
                row.addView(iconHolder, left ? 0 : 1);
            }
            LinearLayout.LayoutParams iconParams = (LinearLayout.LayoutParams) iconHolder.getLayoutParams();
            iconParams.setMargins(left ? 0 : dp(8), 0, left ? dp(8) : 0, 0);
            iconHolder.setLayoutParams(iconParams);
        }
        int title = left ? R.string.speedDialRightAlign : R.string.speedDialLeftAlign;
        alignmentActionLabel.setText(title);
        ((View) alignmentActionLabel.getParent()).setContentDescription(getString(title));
        alignmentActionIcon.setImageResource(left ? R.drawable.ic_align_right : R.drawable.ic_align_left);
        if (videoFragment != null) videoFragment.applyPreviewHandedness();
    }

    private TextView addSpeedDialAction(int titleResource, int iconResource, ImageView existingIcon,
                                        String contentDescription, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setMinimumHeight(dp(48));
        row.setFocusable(true);
        row.setClickable(true);
        row.setContentDescription(contentDescription);

        TextView label = new TextView(this);
        label.setText(titleResource);
        label.setTextColor(Color.WHITE);
        label.setTextSize(14);
        label.setPadding(dp(12), dp(8), dp(12), dp(8));
        label.setBackground(createSpeedDialLabelBackground());
        row.addView(label, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        FrameLayout iconHolder = new FrameLayout(this);
        iconHolder.setBackground(createSpeedDialCircleBackground(false));
        LinearLayout.LayoutParams iconHolderParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        iconHolderParams.setMargins(dp(8), 0, 0, 0);
        row.addView(iconHolder, iconHolderParams);

        ImageView icon = existingIcon == null ? new ImageView(this) : existingIcon;
        if (icon.getParent() instanceof ViewGroup) {
            ((ViewGroup) icon.getParent()).removeView(icon);
        }
        icon.setOnClickListener(null);
        icon.setClickable(false);
        icon.setVisibility(View.VISIBLE);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setColorFilter(Color.WHITE);
        if (existingIcon == null) icon.setImageResource(iconResource);
        iconHolder.addView(icon, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));

        row.setOnClickListener(view -> {
            setCameraSpeedDialExpanded(false);
            action.run();
        });
        if (existingIcon == toggleAudio) {
            icon.setOnClickListener(view -> row.performClick());
        }
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(52));
        rowParams.bottomMargin = dp(8);
        speedDialMenu.addView(row, rowParams);
        return label;
    }

    private GradientDrawable createSpeedDialCircleBackground(boolean primary) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(primary ? 0x80333333 : 0x66333333);
        background.setStroke(dp(1), primary ? 0x99FFFFFF : 0x66FFFFFF);
        return background;
    }

    private GradientDrawable createSpeedDialLabelBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(18));
        background.setColor(0xD9111111);
        background.setStroke(dp(1), 0x55FFFFFF);
        return background;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setCameraSpeedDialExpanded(boolean expanded) {
        if (speedDialMenu == null) return;
        speedDialExpanded = expanded;
        speedDialMenu.setVisibility(expanded ? View.VISIBLE : View.GONE);
        speedDialToggle.setImageResource(expanded
                ? R.drawable.ic_speed_dial_close : R.drawable.ic_speed_dial_settings);
        speedDialToggle.setContentDescription(getString(expanded
                ? R.string.speedDialCloseDescription : R.string.speedDialOpenDescription));
    }

    public void closeCameraSpeedDial() {
        setCameraSpeedDialExpanded(false);
    }

    public void showCameraSpeedDial() {
        if (instantSettingsParent == null) return;
        instantSettingsParent.setVisibility(View.VISIBLE);
        instantSettingsParent.invalidate();
    }

    public void updateSpeedDialFlashIcon(boolean flashEnabled) {
        if (speedDialFlashIcon != null) {
            speedDialFlashIcon.setImageResource(flashEnabled
                    ? R.drawable.ic_video_flash_off : R.drawable.ic_video_flash_on);
        }
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
        if (sharedPreferences == null) return;
        boolean savesToPhone = sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
        if (storageDestination != null) {
            storageDestination.setText(savesToPhone ? R.string.storagePhoneShort : R.string.storageSdShort);
            storageDestination.setContentDescription(getString(R.string.storageDestinationDescription,
                    getString(savesToPhone ? R.string.storagePhoneShort : R.string.storageSdShort)));
        }
        if (storageActionLabel != null) {
            storageActionLabel.setText(savesToPhone
                    ? R.string.speedDialStoragePhone : R.string.speedDialStorageSd);
        }
    }

    public void updateMicrophoneActionLabel(boolean micMuted) {
        if (microphoneActionLabel != null) {
            microphoneActionLabel.setText(micMuted
                    ? R.string.speedDialMicOff : R.string.speedDialMicOn);
        }
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
                        String[] dimensions = newResolution.split(" X ");
                        if (dimensions.length == 2) {
                            videoFragment.setVideoResInfo(dimensions[0], dimensions[1]);
                            // Camera preview relayout can finish after the selection callback and
                            // rebuild the overlay. Reassert the label once that resize settles.
                            cameraView.postDelayed(() -> {
                                if (!cameraView.isRecord()) {
                                    videoFragment.setVideoResInfo(dimensions[0], dimensions[1]);
                                }
                            }, 500);
                        }
                    } catch (RuntimeException exception) {
                        Log.e(TAG, "Unable to apply video resolution " + newResolution, exception);
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

    public void toggleMicrophone(View view){
        if (videoFragment != null && videoFragment.getCameraView() != null
                && videoFragment.getCameraView().isRecord()) {
            Toast.makeText(this, R.string.stopRecordingToChangeMicrophone, Toast.LENGTH_SHORT).show();
            return;
        }
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
        updateMicrophoneActionLabel(noAudio);
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
            noAudioText.setTextColor(getResources().getColor(R.color.uiAccent));
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
        });
        Button decreaseBrightness = (Button)settingsRootView.findViewById(R.id.setTimer);
        decreaseBrightness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        });
        settingsDialog.getWindow().setBackgroundDrawableResource(R.color.backColorSettingPopup);
        settingsDialog.show();
    }

    public void showVideoFragment()
    {
        if(videoFragment == null) {
            if(VERBOSE)Log.d(TAG,"creating videofragment");
            videoFragment = VideoFragment.newInstance();
            videoFragment.setApplicationContext(getApplicationContext());
        }
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.cameraPreview, videoFragment).commit();
        if(VERBOSE)Log.d(TAG, "brightnessLevel SET to = "+controlVisbilityPreference.getBrightnessLevel());
        showCameraSpeedDial();
        setPinchZoomScaleListener(videoFragment);
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
        pinchZoomGestureListener.setProgress(0);
    }

    @Override
    public void askPermission() {
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
