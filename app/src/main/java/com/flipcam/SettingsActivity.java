package com.flipcam;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.flipcam.adapter.FeedbackMailTask;
import com.flipcam.constants.Constants;
import com.flipcam.util.MediaUtil;
import com.flipcam.util.SDCardUtil;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class SettingsActivity extends AppCompatActivity{

    public static final String TAG = "SettingsActivity";
    LinearLayout phoneMemParentVert;
    TextView thresholdText;
    TextView phoneMemTextMsg;
    ImageView greenArrow;
    SharedPreferences settingsPref;
    SharedPreferences.Editor settingsEditor;
    RadioButton phoneMemBtn;
    RadioButton sdCardBtn;
    Dialog sdCardDialog;
    LinearLayout sdcardlayout;
    LinearLayout phoneMemLayout;
    TextView sdCardPathMsg;
    LayoutInflater layoutInflater;
    Dialog saveToCloud;
    Dialog cloudUpload;
    View accessGrantedDropboxRoot;
    View shareMediaRoot;
    AccountManager accountManager;
    final int GET_ACCOUNTS_PERM = 100;
    boolean signInProgress = false;
    Dialog permissionAccount;
    Dialog signInProgressDialog;
    Dialog autoUploadEnabledWithFolder;
    Dialog autoUploadEnabled;
    Dialog autoUploadDisabled;
    Dialog uploadFolderCheck;
    Dialog accesGrantedDropbox;
    Dialog shareMedia;
    View warningMsgRoot;
    Dialog warningMsg;
    Button okButton;
    SDCardEventReceiver sdCardEventReceiver;
    IntentFilter mediaFilters;
    AppWidgetManager appWidgetManager;
    ControlVisbilityPreference controlVisbilityPreference;
    boolean VERBOSE = false;
    LinearLayout photoResolutionParent;
    LinearLayout videoSettingParent;
    EditText feedback_information;
    TextView phoneMempathmsg;
    String mediaPath;
    ImageView editPhoneMemPath;
    String defaultMediaPath;

    public EditText getFeedback_information() {
        return feedback_information;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(VERBOSE)if(VERBOSE)Log.d(TAG,"onCreate");
        setContentView(R.layout.activity_settings);
        mediaFilters = new IntentFilter();
        sdCardEventReceiver = new SDCardEventReceiver();
        phoneMemParentVert = (LinearLayout)findViewById(R.id.phoneMemParentVert);
        phoneMemTextMsg = (TextView)findViewById(R.id.phoneMemTextMsg);
        thresholdText = (TextView)findViewById(R.id.thresholdText);
        greenArrow = (ImageView)findViewById(R.id.greenArrow);
        phoneMemBtn = (RadioButton)findViewById(R.id.phoneMemButton);
        sdCardBtn = (RadioButton)findViewById(R.id.sdCardbutton);
        sdCardPathMsg = (TextView)findViewById(R.id.sdcardpathmsg);
        sdcardlayout = (LinearLayout)findViewById(R.id.sdcardlayout);
        phoneMemLayout = findViewById(R.id.phoneMemLayout);
        phoneMempathmsg = findViewById(R.id.phoneMempathmsg);
        editPhoneMemPath = findViewById(R.id.editPhoneMemPath);
        photoResolutionParent = (LinearLayout)findViewById(R.id.photoResolutionParent);
        videoSettingParent = (LinearLayout)findViewById(R.id.videoSettingParent);
        thresholdText.setText(getString(R.string.memoryThresholdLimit, getResources().getInteger(R.integer.minimumMemoryWarning) + "MB"));
        getSupportActionBar().setTitle(getString(R.string.settingTitle));
        defaultMediaPath = getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + getResources().getString(R.string.FC_ROOT)).getPath();
        mediaPath = PreferenceManager.getDefaultSharedPreferences(this).getString("mediaFilePath", defaultMediaPath);
        settingsPref = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        settingsEditor = settingsPref.edit();
        if(VERBOSE)Log.d(TAG,"SD Card Path onCreate = "+settingsPref.getString(Constants.SD_CARD_PATH,""));
        if(settingsPref.contains(Constants.SD_CARD_PATH) && !settingsPref.getString(Constants.SD_CARD_PATH,"").equals("")) {
            String sdcardpath = settingsPref.getString(Constants.SD_CARD_PATH, "");
            showSDCardPath(sdcardpath);
            phoneMemLayout.setVisibility(View.GONE);
        }
        else{
            hideSDCardPath();
            phoneMemLayout.setVisibility(View.VISIBLE);
            phoneMempathmsg.setText(mediaPath);
        }
        layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        accessGrantedDropboxRoot = layoutInflater.inflate(R.layout.access_granted_dropbox, null);
        warningMsgRoot = layoutInflater.inflate(R.layout.warning_message,null);
        shareMediaRoot = layoutInflater.inflate(R.layout.share_media, null);
        warningMsg = new Dialog(this);
        sdCardDialog = new Dialog(this);
        saveToCloud = new Dialog(this);
        cloudUpload = new Dialog(this);
        permissionAccount = new Dialog(this);
        signInProgressDialog = new Dialog(this);
        autoUploadEnabledWithFolder = new Dialog(this);
        autoUploadEnabled = new Dialog(this);
        autoUploadDisabled = new Dialog(this);
        uploadFolderCheck = new Dialog(this);
        accesGrantedDropbox = new Dialog(this);
        shareMedia = new Dialog(this);
        feedback_information = (EditText) findViewById(R.id.feedback_information);
        accountManager = (AccountManager)getSystemService(Context.ACCOUNT_SERVICE);
        appWidgetManager = (AppWidgetManager)getSystemService(Context.APPWIDGET_SERVICE);
        controlVisbilityPreference = (ControlVisbilityPreference)getApplicationContext();
        photoResolutionParent.setOnClickListener(photoResolutionParentListener);
        videoSettingParent.setOnClickListener(videoResolutionParentListener);
    }

    View.OnClickListener photoResolutionParentListener = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            Intent photoSettings = new Intent(getApplicationContext(), PhotoSettingsActivity.class);
            startActivity(photoSettings);
        }
    };

    View.OnClickListener videoResolutionParentListener = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            Intent videoSettings = new Intent(getApplicationContext(), VideoSettingsActivity.class);
            startActivity(videoSettings);
        }
    };

    class SDCardEventReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if(VERBOSE)Log.d(TAG, "onReceive = "+intent.getAction());
            if(intent.getAction().equalsIgnoreCase(Intent.ACTION_MEDIA_UNMOUNTED)){
                //Check if SD Card was selected
                if(!settingsPref.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
                    showSDCardUnavailMessage();
                }
            }
        }
    }

    public void showSDCardUnavailMessage(){
        if(VERBOSE)Log.d(TAG, "SD Card Removed");
        phoneMemBtn.setChecked(true);
        sdCardBtn.setChecked(false);
        settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
        settingsEditor.commit();
        hideSDCardPath();
        LinearLayout warningParent = (LinearLayout)warningMsgRoot.findViewById(R.id.warningParent);
        warningParent.setBackgroundColor(getResources().getColor(R.color.backColorSettingMsg));
        TextView warningTitle = (TextView)warningMsgRoot.findViewById(R.id.warningTitle);
        warningTitle.setText(getString(R.string.sdCardRemovedTitle));
        ImageView warningSign = (ImageView)warningMsgRoot.findViewById(R.id.warningSign);
        warningSign.setVisibility(View.VISIBLE);
        TextView warningText = (TextView)warningMsgRoot.findViewById(R.id.warningText);
        warningText.setText(getString(R.string.sdCardNotPresentForRecord));
        okButton = (Button)warningMsgRoot.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                warningMsg.dismiss();
            }
        });
        warningMsg.setContentView(warningMsgRoot);
        warningMsg.setCancelable(false);
        warningMsg.show();
    }

    public void showSDCardWriteErrorMessage(){
        if(VERBOSE)Log.d(TAG, "SD Card Removed");
        phoneMemBtn.setChecked(true);
        sdCardBtn.setChecked(false);
        settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
        settingsEditor.commit();
        hideSDCardPath();
        LinearLayout warningParent = (LinearLayout)warningMsgRoot.findViewById(R.id.warningParent);
        warningParent.setBackgroundColor(getResources().getColor(R.color.backColorSettingMsg));
        TextView warningTitle = (TextView)warningMsgRoot.findViewById(R.id.warningTitle);
        warningTitle.setText(getString(R.string.sdCardWriteError));
        ImageView warningSign = (ImageView)warningMsgRoot.findViewById(R.id.warningSign);
        warningSign.setVisibility(View.VISIBLE);
        TextView warningText = (TextView)warningMsgRoot.findViewById(R.id.warningText);
        warningText.setText(getString(R.string.sdCardWriteErrorMessage));
        okButton = (Button)warningMsgRoot.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                warningMsg.dismiss();
            }
        });
        warningMsg.setContentView(warningMsgRoot);
        warningMsg.setCancelable(false);
        warningMsg.show();
    }

    public void sendFeedback(View view){
        if(!getFeedback_information().getText().toString().trim().equals("")){

            new FeedbackMailTask(getApplicationContext(), this).execute();
        }
        else{
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.noFeedbackMsg), Toast.LENGTH_SHORT).show();
        }
    }

    public void updateSettingsValues(){
        //Update Save Media in
        if(settingsPref.contains(Constants.SAVE_MEDIA_PHONE_MEM)){
            if(VERBOSE)Log.d(TAG,"Phone memory exists");
            if(settingsPref.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
                if(VERBOSE)Log.d(TAG,"Phone memory is true");
                phoneMemBtn.setChecked(true);
                sdCardBtn.setChecked(false);
                phoneMemLayout.setVisibility(View.VISIBLE);
                phoneMempathmsg.setText(mediaPath);
                hideSDCardPath();
            }
            else{
                if(VERBOSE)Log.d(TAG,"Phone memory is false");
                if(SDCardUtil.doesSDCardExist(getApplicationContext()) != null) {
                    phoneMemBtn.setChecked(false);
                    phoneMemLayout.setVisibility(View.GONE);
                    sdCardBtn.setChecked(true);
                    if(SDCardUtil.isPathWritable(settingsPref.getString(Constants.SD_CARD_PATH, ""))) {
                        showSDCardPath(settingsPref.getString(Constants.SD_CARD_PATH, ""));
                    }
                    else{
                        showSDCardWriteErrorMessage();
                    }
                }
                else{
                    showSDCardUnavailMessage();
                }
            }
        }
        else{
            if(VERBOSE)Log.d(TAG,"Phone memory NOT exists");
            phoneMemBtn.setChecked(true);
            sdCardBtn.setChecked(false);
        }
        //Video Resolution
        String selRes = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(Constants.SELECT_VIDEO_RESOLUTION, null);
        if(VERBOSE)Log.d(TAG, "SELECTED VIDEO RES PREF MGR = "+selRes);
        //Photo Resolution
        selRes = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(Constants.SELECT_PHOTO_RESOLUTION, null);
        if(VERBOSE)Log.d(TAG, "SELECTED PIC RES PREF MGR = "+selRes);
        //Memory consumed
        boolean memCon = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(Constants.SHOW_MEMORY_CONSUMED_MSG, false);
        if(VERBOSE)Log.d(TAG, "MEMORY CONSUMED PREF MGR = "+memCon);
        //Update Phone memory
        if(settingsPref.contains(Constants.PHONE_MEMORY_DISABLE)){
            if(!settingsPref.getBoolean(Constants.PHONE_MEMORY_DISABLE, true)){
                String memoryLimit = settingsPref.getString(Constants.PHONE_MEMORY_LIMIT, getResources().getInteger(R.integer.minimumMemoryWarning) + "");
                String memoryMetric = settingsPref.getString(Constants.PHONE_MEMORY_METRIC, "MB");
                thresholdText.setText(getString(R.string.memoryThresholdLimit, Integer.parseInt(memoryLimit) + " " + memoryMetric));
            }
            else{
                thresholdText.setText(getString(R.string.memoryThresholdLimit, getString(R.string.phoneMemoryLimitDisabled)));
            }
        }
        else{
            thresholdText.setText(getString(R.string.memoryThresholdLimit, getString(R.string.phoneMemoryLimitDisabled)));
        }
    }

    public void openDirectory(View view) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        startActivityForResult(Intent.createChooser(i, "Choose directory"), 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent resultData) {
        if (requestCode == 1000 && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri mediaUri = resultData.getData();
                Uri docUri = DocumentsContract.buildDocumentUriUsingTree(mediaUri, DocumentsContract.getTreeDocumentId(mediaUri));
                mediaPath = MediaUtil.getPathFromUri(this, docUri);
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putString("mediaFilePath",mediaPath);
                editor.apply();
                Log.d(TAG, "File path = "+mediaPath);
            }
            else{
                mediaPath = defaultMediaPath;
            }
        }
        super.onActivityResult(requestCode,resultCode,resultData);
    }

    public void selectSaveMedia(View view){
        switch (view.getId()){
            case R.id.phoneMemButton:
                if(VERBOSE)Log.d(TAG,"Save in phone memory");
                settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,true);
                settingsEditor.commit();
                phoneMemBtn.setChecked(true);
                sdCardBtn.setChecked(false);
                hideSDCardPath();
                phoneMemLayout.setVisibility(View.VISIBLE);
                phoneMempathmsg.setText(mediaPath);
                controlVisbilityPreference.setMediaSelectedPosition(0);
                break;
            case R.id.sdCardbutton:
                if(VERBOSE)Log.d(TAG,"Save in sd card");
                phoneMemBtn.setChecked(false);
                sdCardBtn.setChecked(true);
                phoneMemLayout.setVisibility(View.GONE);
                String sdCardPath = SDCardUtil.doesSDCardExist(getApplicationContext());
                if(sdCardPath == null){
                    if(VERBOSE)Log.d(TAG, "No SD Card");
                    phoneMemBtn.setChecked(true);
                    sdCardBtn.setChecked(false);
                    settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
                    settingsEditor.commit();
                    phoneMemLayout.setVisibility(View.VISIBLE);
                    phoneMempathmsg.setText(mediaPath);
                    hideSDCardPath();
                    LinearLayout warningParent = (LinearLayout)warningMsgRoot.findViewById(R.id.warningParent);
                    warningParent.setBackgroundColor(getResources().getColor(R.color.backColorSettingMsg));
                    TextView warningTitle = (TextView)warningMsgRoot.findViewById(R.id.warningTitle);
                    warningTitle.setText(getString(R.string.sdCardNotDetectTitle));
                    ImageView warningSign = (ImageView)warningMsgRoot.findViewById(R.id.warningSign);
                    warningSign.setVisibility(View.VISIBLE);
                    TextView warningText = (TextView)warningMsgRoot.findViewById(R.id.warningText);
                    warningText.setText(getString(R.string.sdCardNotDetectMessage));
                }
                else{
                    if(SDCardUtil.isPathWritable(sdCardPath)) {
                        settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, false);
                        settingsEditor.putString(Constants.SD_CARD_PATH, sdCardPath);
                        settingsEditor.commit();
                        showSDCardPath(sdCardPath);
                        LinearLayout warningParent = (LinearLayout) warningMsgRoot.findViewById(R.id.warningParent);
                        warningParent.setBackgroundColor(getResources().getColor(R.color.backColorSettingMsg));
                        ImageView warningSign = (ImageView) warningMsgRoot.findViewById(R.id.warningSign);
                        warningSign.setVisibility(View.GONE);
                        TextView warningTitle = (TextView) warningMsgRoot.findViewById(R.id.warningTitle);
                        warningTitle.setText(getString(R.string.sdCardDetectTitle));
                        TextView warningText = (TextView) warningMsgRoot.findViewById(R.id.warningText);
                        warningText.setText(getString(R.string.sdCardDetectMessage, sdCardPath));
                        controlVisbilityPreference.setMediaSelectedPosition(0);
                    }
                    else{
                        phoneMemBtn.setChecked(true);
                        sdCardBtn.setChecked(false);
                        settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
                        settingsEditor.commit();
                        phoneMemLayout.setVisibility(View.VISIBLE);
                        phoneMempathmsg.setText(mediaPath);
                        hideSDCardPath();
                        showSDCardWriteErrorMessage();
                        break;
                    }
                }
                warningMsg.setContentView(warningMsgRoot);
                warningMsg.setCancelable(false);
                warningMsg.show();
                okButton = (Button)warningMsgRoot.findViewById(R.id.okButton);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        warningMsg.dismiss();
                    }
                });
                break;
        }
    }

    public void showSDCardPath(String path){
        sdCardPathMsg.setText(path);
        sdcardlayout.setVisibility(View.VISIBLE);
    }

    public void hideSDCardPath(){
        sdcardlayout.setVisibility(View.GONE);
    }

    public void showMemoryConsumed(View view){
        Intent memoryAct = new Intent(SettingsActivity.this, MemoryLimitActivity.class);
        startActivity(memoryAct);
        overridePendingTransition(R.anim.slide_from_right,R.anim.slide_to_left);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
    }
    @Override
    protected void onPause() {
        super.onPause();
        if(VERBOSE)Log.d(TAG,"onPause");
        try {
            unregisterReceiver(sdCardEventReceiver);
        }catch (IllegalArgumentException illegal){
            if(VERBOSE)Log.d(TAG, "Receiver was never registered");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mediaFilters.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        mediaFilters.addAction(Intent.ACTION_MEDIA_MOUNTED);
        mediaFilters.addDataScheme("file");
        registerReceiver(sdCardEventReceiver, mediaFilters);
        updateSettingsValues();
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
        if(signInProgress){
            signInProgressDialog.dismiss();
            if(VERBOSE)Log.d(TAG,"Reset signinprogess");
            signInProgress = false;
        }
    }
}
