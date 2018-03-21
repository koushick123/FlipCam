package com.flipcam;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderResult;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.flipcam.constants.Constants;
import com.flipcam.media.FileMedia;
import com.flipcam.util.MediaUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.DriveStatusCodes;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
    TextView sdCardPathMsg;
    LayoutInflater layoutInflater;
    CheckBox switchOnDrive;
    CheckBox switchOnDropbox;
    Dialog saveToCloud;
    TextView savetocloudtitle;
    DriveClient mDriveClient;
    DriveResourceClient mDriveResourceClient;
    static final int REQUEST_CODE_SIGN_IN = 0;
    GoogleSignInOptions signInOptions;
    GoogleSignInClient googleSignInClient = null;
    boolean signedInDrive = false;
    Dialog cloudUpload;
    View cloudUploadRoot;
    View signInProgressRoot;
    View saveToCloudRoot;
    View autoUploadEnabledWithFolderRoot;
    View autoUploadEnabledRoot;
    View autoUploadDisabledRoot;
    View uploadFolderCheckRoot;
    View accessGrantedDropboxRoot;
    View shareMediaRoot;
    TextView uploadFolderTitle;
    TextView uploadFolderMsg;
    int cloud = 0; //Default to Google Drive. 1 for Dropbox.
    AccountManager accountManager;
    final int GET_ACCOUNTS_PERM = 100;
    boolean signInProgress = false;
    Dialog permissionAccount;
    Dialog signInProgressDialog;
    ImageView uploadDestIcon;
    Dialog autoUploadEnabledWithFolder;
    Dialog autoUploadEnabled;
    Dialog autoUploadDisabled;
    Dialog uploadFolderCheck;
    Dialog accesGrantedDropbox;
    Dialog shareMedia;
    DbxClientV2 dbxClientV2;
    DbxRequestConfig dbxRequestConfig;
    boolean goToDropbox = false;
    CheckBox showMemoryConsumed;
    View warningMsgRoot;
    Dialog warningMsg;
    Button okButton;
    SDCardEventReceiver sdCardEventReceiver;
    IntentFilter mediaFilters;
    AppWidgetManager appWidgetManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
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
        switchOnDropbox = (CheckBox) findViewById(R.id.switchOnDropbox);
        switchOnDrive = (CheckBox) findViewById(R.id.switchOnDrive);
        sdcardlayout = (LinearLayout)findViewById(R.id.sdcardlayout);
        showMemoryConsumed = (CheckBox)findViewById(R.id.showMemoryConsumed);
        thresholdText.setText(getResources().getString(R.string.memoryThresholdLimit, getResources().getInteger(R.integer.minimumMemoryWarning) + "MB"));
        getSupportActionBar().setTitle(getResources().getString(R.string.settingTitle));
        settingsPref = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        settingsEditor = settingsPref.edit();
        Log.d(TAG,"SD Card Path onCreate = "+settingsPref.getString(Constants.SD_CARD_PATH,""));
        if(settingsPref.contains(Constants.SD_CARD_PATH) && !settingsPref.getString(Constants.SD_CARD_PATH,"").equals("")) {
            String sdcardpath = settingsPref.getString(Constants.SD_CARD_PATH, "");
            showSDCardPath(sdcardpath);
        }
        else{
            hideSDCardPath();
        }
        layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        saveToCloudRoot = layoutInflater.inflate(R.layout.save_to_cloud,null);
        cloudUploadRoot = layoutInflater.inflate(R.layout.cloud_upload_folder,null);
        signInProgressRoot = layoutInflater.inflate(R.layout.task_in_progress,null);
        autoUploadEnabledWithFolderRoot = layoutInflater.inflate(R.layout.auto_upload_enabled_with_folder, null);
        autoUploadEnabledRoot = layoutInflater.inflate(R.layout.auto_upload_enabled, null);
        autoUploadDisabledRoot = layoutInflater.inflate(R.layout.auto_upload_disabled, null);
        uploadFolderCheckRoot = layoutInflater.inflate(R.layout.upload_folder_check, null);
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
        accountManager = (AccountManager)getSystemService(Context.ACCOUNT_SERVICE);
        appWidgetManager = (AppWidgetManager)getSystemService(Context.APPWIDGET_SERVICE);
    }

    class SDCardEventReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context ctx, Intent intent) {
            Log.d(TAG, "onReceive = "+intent.getAction());
            if(intent.getAction().equalsIgnoreCase(Intent.ACTION_MEDIA_UNMOUNTED)){
                //Check if SD Card was selected
                if(!settingsPref.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
                    showSDCardUnavailMessage();
                }
            }
        }
    }

    public void showSDCardUnavailMessage(){
        Log.d(TAG, "SD Card Removed");
        phoneMemBtn.setChecked(true);
        sdCardBtn.setChecked(false);
        settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
        settingsEditor.commit();
        hideSDCardPath();
        LinearLayout warningParent = (LinearLayout)warningMsgRoot.findViewById(R.id.warningParent);
        warningParent.setBackgroundColor(getResources().getColor(R.color.backColorSettingMsg));
        TextView warningTitle = (TextView)warningMsgRoot.findViewById(R.id.warningTitle);
        warningTitle.setText(getResources().getString(R.string.sdCardRemovedTitle));
        ImageView warningSign = (ImageView)warningMsgRoot.findViewById(R.id.warningSign);
        warningSign.setVisibility(View.VISIBLE);
        TextView warningText = (TextView)warningMsgRoot.findViewById(R.id.warningText);
        warningText.setText(getResources().getString(R.string.sdCardNotPresentForRecord));
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

    public void updateSettingsValues(){
        //Update Save Media in
        if(settingsPref.contains(Constants.SAVE_MEDIA_PHONE_MEM)){
            Log.d(TAG,"Phone memory exists");
            if(settingsPref.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
                Log.d(TAG,"Phone memory is true");
                phoneMemBtn.setChecked(true);
                sdCardBtn.setChecked(false);
                hideSDCardPath();
            }
            else{
                Log.d(TAG,"Phone memory is false");
                if(doesSDCardExist() != null) {
                    phoneMemBtn.setChecked(false);
                    sdCardBtn.setChecked(true);
                    showSDCardPath(settingsPref.getString(Constants.SD_CARD_PATH, ""));
                }
                else{
                    showSDCardUnavailMessage();
                }
            }
        }
        else{
            Log.d(TAG,"Phone memory NOT exists");
            phoneMemBtn.setChecked(true);
            sdCardBtn.setChecked(false);
        }
        //Update Phone memory
        if(settingsPref.contains(Constants.PHONE_MEMORY_DISABLE)){
            if(!settingsPref.getBoolean(Constants.PHONE_MEMORY_DISABLE, true)){
                String memoryLimit = settingsPref.getString(Constants.PHONE_MEMORY_LIMIT, getResources().getInteger(R.integer.minimumMemoryWarning) + "");
                String memoryMetric = settingsPref.getString(Constants.PHONE_MEMORY_METRIC, "MB");
                thresholdText.setText(getResources().getString(R.string.memoryThresholdLimit, Integer.parseInt(memoryLimit) + " " + memoryMetric));
            }
            else{
                thresholdText.setText(getResources().getString(R.string.memoryThresholdLimit, getResources().getString(R.string.phoneMemoryLimitDisabled)));
            }
        }
        else{
            thresholdText.setText(getResources().getString(R.string.memoryThresholdLimit, getResources().getString(R.string.phoneMemoryLimitDisabled)));
        }
        //Update Auto upload
        //Google Drive
        if(settingsPref.contains(Constants.SAVE_TO_GOOGLE_DRIVE)){
            if(settingsPref.getBoolean(Constants.SAVE_TO_GOOGLE_DRIVE, false)){
                switchOnDrive.setChecked(true);
            }
            else{
                switchOnDrive.setChecked(false);
            }
        }
        else{
            switchOnDrive.setChecked(false);
        }
        //Dropbox
        if(settingsPref.contains(Constants.SAVE_TO_DROPBOX)){
            if(settingsPref.getBoolean(Constants.SAVE_TO_DROPBOX, false)){
                switchOnDropbox.setChecked(true);
            }
            else{
                switchOnDropbox.setChecked(false);
            }
        }
        else{
            switchOnDropbox.setChecked(false);
        }
        //Show Memory Consumed
        if(settingsPref.contains(Constants.SHOW_MEMORY_CONSUMED_MSG)){
            if(settingsPref.getBoolean(Constants.SHOW_MEMORY_CONSUMED_MSG, false)){
                showMemoryConsumed.setChecked(true);
            }
            else{
                showMemoryConsumed.setChecked(false);
            }
        }
        else{
            showMemoryConsumed.setChecked(false);
        }
    }

    public void resetAllValues(View view){
        LinearLayout shareMediaParent = (LinearLayout)shareMediaRoot.findViewById(R.id.shareMediaParent);
        shareMediaParent.setBackgroundColor(getResources().getColor(R.color.backColorSettingMsg));
        TextView shareTitle = (TextView)shareMediaRoot.findViewById(R.id.shareTitle);
        shareTitle.setText(getResources().getString(R.string.resetTitle));
        TextView shareMsg = (TextView)shareMediaRoot.findViewById(R.id.shareText);
        shareMsg.setText(getResources().getString(R.string.resetMsg));
        Button okBtn = (Button)shareMediaRoot.findViewById(R.id.okToShare);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                okReset();
            }
        });
        Button cancelBtn = (Button)shareMediaRoot.findViewById(R.id.cancelToShare);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelReset();
            }
        });
        shareMedia.setContentView(shareMediaRoot);
        shareMedia.setCancelable(true);
        shareMedia.show();
    }

    public void okReset(){
        //Reset Save Media
        settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
        //Reset threshold warning
        settingsEditor.putBoolean(Constants.PHONE_MEMORY_DISABLE, true);
        //Reset auto upload
        settingsEditor.putBoolean(Constants.SAVE_TO_GOOGLE_DRIVE, false);
        settingsEditor.putBoolean(Constants.SAVE_TO_DROPBOX, false);
        //Reset memory consumed
        settingsEditor.putBoolean(Constants.SHOW_MEMORY_CONSUMED_MSG, false);
        settingsEditor.commit();
        updateSettingsValues();
        shareMedia.dismiss();
    }

    public void cancelReset(){
        shareMedia.dismiss();
    }

    public void updateWidget(){
        HashSet<String> widgetIds = (HashSet)settingsPref.getStringSet(Constants.WIDGET_IDS, null);
        if(widgetIds != null && widgetIds.size() > 0){
            Iterator<String> iterator = widgetIds.iterator();
            while(iterator.hasNext()){
                String widgetId = iterator.next();
                Log.d(TAG, "widgetIds = "+widgetId);
                updateAppWidget(Integer.parseInt(widgetId));
            }
        }
    }

    public void updateAppWidget(int appWidgetId) {
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.flipcam_widget);
        FileMedia[] media = MediaUtil.getMediaList(this);
        if (media != null && media.length > 0) {
            String filepath = media[0].getPath();
            Log.d(TAG, "FilePath = " + filepath);
            if (filepath.endsWith(getResources().getString(R.string.IMG_EXT))
                    || filepath.endsWith(getResources().getString(R.string.ANOTHER_IMG_EXT))) {
                Bitmap latestImage = BitmapFactory.decodeFile(filepath);
                latestImage = Bitmap.createScaledBitmap(latestImage, (int) getResources().getDimension(R.dimen.thumbnailWidth),
                        (int) getResources().getDimension(R.dimen.thumbnailHeight), false);
                Log.d(TAG, "Update Photo thumbnail");
                remoteViews.setViewVisibility(R.id.playCircleWidget, View.INVISIBLE);
                remoteViews.setImageViewBitmap(R.id.imageWidget, latestImage);
                remoteViews.setTextViewText(R.id.widgetMsg, getResources().getString(R.string.widgetMediaMsg));
            } else {
                Bitmap vid = null;
                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                try {
                    mediaMetadataRetriever.setDataSource(filepath);
                    vid = mediaMetadataRetriever.getFrameAtTime(Constants.FIRST_SEC_MICRO);
                } catch (RuntimeException runtime) {
                    File badFile = new File(filepath);
                    badFile.delete();
                    media = MediaUtil.getMediaList(this);
                    if (media != null && media.length > 0) {
                        mediaMetadataRetriever.setDataSource(filepath);
                        vid = mediaMetadataRetriever.getFrameAtTime(Constants.FIRST_SEC_MICRO);
                    } else {
                        remoteViews.setImageViewResource(R.id.imageWidget, R.drawable.placeholder);
                        remoteViews.setTextViewText(R.id.widgetMsg, getResources().getString(R.string.widgetNoMedia));
                    }
                }
                if (vid != null) {
                    vid = Bitmap.createScaledBitmap(vid, (int) getResources().getDimension(R.dimen.thumbnailWidth),
                            (int) getResources().getDimension(R.dimen.thumbnailHeight), false);
                    Log.d(TAG, "Update Video thumbnail");
                    remoteViews.setViewVisibility(R.id.playCircleWidget, View.VISIBLE);
                    remoteViews.setImageViewBitmap(R.id.imageWidget, vid);
                    remoteViews.setTextViewText(R.id.widgetMsg, getResources().getString(R.string.widgetMediaMsg));
                }
            }
        } else {
            remoteViews.setImageViewResource(R.id.imageWidget, R.drawable.placeholder);
            remoteViews.setTextViewText(R.id.widgetMsg, getResources().getString(R.string.widgetNoMedia));
        }
        Log.d(TAG, "Update FC Widget");
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    public void checkShowMemoryConsumed(View view){
        if(showMemoryConsumed.isChecked()){
            settingsEditor.putBoolean(Constants.SHOW_MEMORY_CONSUMED_MSG, true);
        }
        else{
            settingsEditor.putBoolean(Constants.SHOW_MEMORY_CONSUMED_MSG, false);
        }
        settingsEditor.commit();
    }

    public String doesSDCardExist(){
        File[] mediaDirs = getExternalMediaDirs();
        if(mediaDirs != null) {
            Log.d(TAG, "mediaDirs = " + mediaDirs.length);
        }
        for(int i=0;i<mediaDirs.length;i++){
            Log.d(TAG, "external media dir = "+mediaDirs[i]);
            if(mediaDirs[i] != null) {
                try {
                    if (Environment.isExternalStorageRemovable(mediaDirs[i])) {
                        Log.d(TAG, "Removable storage = " + mediaDirs[i]);
                        return mediaDirs[i].getPath();
                    }
                } catch (IllegalArgumentException illegal) {
                    Log.d(TAG, "Not a valid storage device");
                }
            }
        }
        return null;
    }

    public void selectSaveMedia(View view){
        switch (view.getId()){
            case R.id.phoneMemButton:
                Log.d(TAG,"Save in phone memory");
                settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,true);
                settingsEditor.commit();
                updateWidget();
                phoneMemBtn.setChecked(true);
                sdCardBtn.setChecked(false);
                hideSDCardPath();
                break;
            case R.id.sdCardbutton:
                Log.d(TAG,"Save in sd card");
                phoneMemBtn.setChecked(false);
                sdCardBtn.setChecked(true);
                String sdCardPath = doesSDCardExist();
                if(sdCardPath == null){
                    Log.d(TAG, "No SD Card");
                    phoneMemBtn.setChecked(true);
                    sdCardBtn.setChecked(false);
                    settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
                    settingsEditor.commit();
                    hideSDCardPath();
                    LinearLayout warningParent = (LinearLayout)warningMsgRoot.findViewById(R.id.warningParent);
                    warningParent.setBackgroundColor(getResources().getColor(R.color.backColorSettingMsg));
                    TextView warningTitle = (TextView)warningMsgRoot.findViewById(R.id.warningTitle);
                    warningTitle.setText(getResources().getString(R.string.sdCardNotDetectTitle));
                    ImageView warningSign = (ImageView)warningMsgRoot.findViewById(R.id.warningSign);
                    warningSign.setVisibility(View.VISIBLE);
                    TextView warningText = (TextView)warningMsgRoot.findViewById(R.id.warningText);
                    warningText.setText(getResources().getString(R.string.sdCardNotDetectMessage));
                }
                else{
                    settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, false);
                    settingsEditor.putString(Constants.SD_CARD_PATH, sdCardPath);
                    settingsEditor.commit();
                    updateWidget();
                    showSDCardPath(sdCardPath);
                    LinearLayout warningParent = (LinearLayout)warningMsgRoot.findViewById(R.id.warningParent);
                    warningParent.setBackgroundColor(getResources().getColor(R.color.backColorSettingMsg));
                    ImageView warningSign = (ImageView)warningMsgRoot.findViewById(R.id.warningSign);
                    warningSign.setVisibility(View.GONE);
                    TextView warningTitle = (TextView)warningMsgRoot.findViewById(R.id.warningTitle);
                    warningTitle.setText(getResources().getString(R.string.sdCardDetectTitle));
                    TextView warningText = (TextView)warningMsgRoot.findViewById(R.id.warningText);
                    warningText.setText(getResources().getString(R.string.sdCardDetectMessage, sdCardPath));
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

    public void saveToCloudDrive(View view) {
        cloud = Constants.GOOGLE_DRIVE_CLOUD;
        if (switchOnDrive.isChecked()) {
            savetocloudtitle = (TextView)saveToCloudRoot.findViewById(R.id.savetocloudtitle);
            savetocloudtitle.setText(getResources().getString(R.string.saveToCloudTitle, getResources().getString(R.string.googleDrive)));
            ImageView placeHolderIcon = (ImageView)saveToCloudRoot.findViewById(R.id.placeHolderIconSavetoCloud);
            placeHolderIcon.setImageDrawable(getResources().getDrawable(R.drawable.google_drive));
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            imageParams.width = (int)getResources().getDimension(R.dimen.googleDriveIconWidth);
            imageParams.height = (int)getResources().getDimension(R.dimen.googleDriveIconHeight);
            placeHolderIcon.setLayoutParams(imageParams);
            TextView savetoCloudMsg = (TextView)saveToCloudRoot.findViewById(R.id.savetocloudmsg);
            savetoCloudMsg.setText(getResources().getString(R.string.continueToCloud));
            saveToCloud.setContentView(saveToCloudRoot);
            saveToCloud.setCancelable(false);
            saveToCloud.show();
        }
        else{
            boolean saveToDrive = settingsPref.getBoolean(Constants.SAVE_TO_GOOGLE_DRIVE, false);
            if(saveToDrive) {
                if(googleSignInClient == null){
                    initializeGoogleSignIn();
                }
                googleSignInClient.signOut();
                signedInDrive = false;
                showUploadDisabled();
                settingsEditor.putBoolean(Constants.SAVE_TO_GOOGLE_DRIVE , false);
                settingsEditor.commit();
            }
        }
    }

    public void saveToDropBox(View view){
        cloud = Constants.DROPBOX_CLOUD;
        if(switchOnDropbox.isChecked()){
            savetocloudtitle = (TextView)saveToCloudRoot.findViewById(R.id.savetocloudtitle);
            savetocloudtitle.setText(getResources().getString(R.string.saveToCloudTitle, getResources().getString(R.string.dropbox)));
            ImageView placeHolderIcon = (ImageView)saveToCloudRoot.findViewById(R.id.placeHolderIconSavetoCloud);
            placeHolderIcon.setImageDrawable(getResources().getDrawable(R.drawable.dropbox));
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            imageParams.width = (int)getResources().getDimension(R.dimen.dropBoxIconWidth);
            imageParams.height = (int)getResources().getDimension(R.dimen.dropBoxIconHeight);
            placeHolderIcon.setLayoutParams(imageParams);
            TextView savetoCloudMsg = (TextView)saveToCloudRoot.findViewById(R.id.savetocloudmsg);
            savetoCloudMsg.setText(getResources().getString(R.string.saveToCloudDropbox));
            saveToCloud.setContentView(saveToCloudRoot);
            saveToCloud.setCancelable(false);
            saveToCloud.show();
        }
        else{
            if(dbxClientV2 == null){
                dbxRequestConfig = new DbxRequestConfig("dropbox/flipCam");
                dbxClientV2 = new DbxClientV2(dbxRequestConfig, settingsPref.getString(Constants.DROPBOX_ACCESS_TOKEN,""));
            }
            revokeAccessFromDropbox();
            disableDropboxInSetting();
            showUploadDisabled();
        }
    }

    public void createDropboxFolder(View view){
        accesGrantedDropbox.dismiss();
        createUploadFolder();
    }

    public boolean doesPackageExist(Context c, String targetPackage) {
        PackageManager pm = c.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
            if(info != null) {
                Log.d(TAG, "package name= " + info.packageName);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG,"Package "+targetPackage+" does NOT exist");
            return false;
        }
        return true;
    }

    public void signInToCloud(View view){
        switch (view.getId()){
            case R.id.continueSignIn:
                saveToCloud.dismiss();
                if(cloud == Constants.GOOGLE_DRIVE_CLOUD){
                    //Sign in to Google Drive
                    int permissionCheck = ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS);
                    if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        continueToGoogleDrive();
                    }
                    else{
                        permissionAccount.setContentView(R.layout.permission_account);
                        permissionAccount.setCancelable(false);
                        permissionAccount.show();
                    }
                }
                else if(cloud == Constants.DROPBOX_CLOUD){
                    if(!isConnectedToInternet()){
                        Toast.makeText(getApplicationContext(),getResources().getString(R.string.noConnectionMessage),Toast.LENGTH_SHORT).show();
                        switchOnDropbox.setChecked(false);
                        return;
                    }
                    //Sign in to Dropbox
                    goToDropbox = true;
                    Auth.startOAuth2Authentication(getApplicationContext(), getString(R.string.dropBoxAppKey));
                    signInProgress = true;
                    TextView signInText = (TextView)signInProgressRoot.findViewById(R.id.signInText);
                    TextView signInprogressTitle = (TextView)signInProgressRoot.findViewById(R.id.savetocloudtitle);
                    signInprogressTitle.setText(getResources().getString(R.string.signInProgressTitle, getResources().getString(R.string.dropbox)));
                    if(doesPackageExist(this, "com.dropbox.android")){
                        signInText.setText("Opening Dropbox App...");
                    }
                    else {
                        signInText.setText(getResources().getString(R.string.signInProgressDropbox));
                    }
                    ImageView signInImage = (ImageView) signInProgressRoot.findViewById(R.id.signInImage);
                    signInImage.setImageDrawable(getResources().getDrawable(R.drawable.dropbox));
                    signInProgressDialog.setContentView(signInProgressRoot);
                    signInProgressDialog.setCancelable(false);
                    signInProgressDialog.show();
                }
                break;
            case R.id.cancelSignIn:
                saveToCloud.dismiss();
                switch(cloud) {
                    case Constants.GOOGLE_DRIVE_CLOUD:
                        settingsEditor.putBoolean(Constants.SAVE_TO_GOOGLE_DRIVE, false);
                        settingsEditor.commit();
                        switchOnDrive.setChecked(false);
                        break;
                    case Constants.DROPBOX_CLOUD:
                        settingsEditor.putBoolean(Constants.SAVE_TO_DROPBOX, false);
                        settingsEditor.commit();
                        switchOnDropbox.setChecked(false);
                        break;
                    }
                break;
            }
    }

    public void accountsPermission(View view){
        switch (view.getId()){
            case R.id.yesPermission:
                Log.d(TAG,"yesPermission");
                permissionAccount.dismiss();
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.GET_ACCOUNTS}, GET_ACCOUNTS_PERM);
                break;
            case R.id.noPermission:
                Log.d(TAG,"noPermission");
                if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
                    switchOnDrive.setChecked(false);
                }
                permissionAccount.dismiss();
                break;
        }
    }

    public void initializeGoogleSignIn(){
        signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .requestScopes(Drive.SCOPE_APPFOLDER)
                        .build();
        googleSignInClient = GoogleSignIn.getClient(this, signInOptions);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
        try {
            unregisterReceiver(sdCardEventReceiver);
        }catch (IllegalArgumentException illegal){
            Log.d(TAG, "Receiver was never registered");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaFilters.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        mediaFilters.addAction(Intent.ACTION_MEDIA_MOUNTED);
        mediaFilters.addDataScheme("file");
        registerReceiver(sdCardEventReceiver, mediaFilters);
        updateSettingsValues();
        if (signInProgress) {
            signInProgressDialog.dismiss();
            Log.d(TAG, "Reset signinprogess");
            signInProgress = false;
        }
        if(goToDropbox) {
            goToDropbox = false;
            Log.d(TAG, "Access token = " + Auth.getOAuth2Token());
            if(Auth.getOAuth2Token() == null){
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.signInDropboxFail),Toast.LENGTH_LONG).show();
                switchOnDropbox.setChecked(false);
                disableDropboxInSetting();
            }
            else{
                settingsEditor.putString(Constants.DROPBOX_ACCESS_TOKEN, Auth.getOAuth2Token());
                settingsEditor.commit();
                dbxRequestConfig = new DbxRequestConfig("dropbox/flipCam");
                dbxClientV2 = new DbxClientV2(dbxRequestConfig, Auth.getOAuth2Token());
                if(settingsPref.contains(Constants.DROPBOX_FOLDER) && (!settingsPref.getString(Constants.DROPBOX_FOLDER,"").equals("")
                        && !settingsPref.getString(Constants.DROPBOX_FOLDER,"").equalsIgnoreCase(getResources().getString(R.string.app_name)))){
                    checkIfFolderCreatedInDropbox();
                }
                else {
                    //Folder name is same as app name
                    updateDropboxInSetting(getResources().getString(R.string.app_name), true);
                    TextView dropBoxfolderCreated = (TextView) accessGrantedDropboxRoot.findViewById(R.id.dropBoxFolderCreated);
                    dropBoxfolderCreated.setText(getResources().getString(R.string.autouploadFolderUpdated, getResources().getString(R.string.flipCamAppFolder),
                            getResources().getString(R.string.dropbox)));
                    CheckBox dropBoxFolderCreateEnable = (CheckBox) accessGrantedDropboxRoot.findViewById(R.id.dropBoxFolderCreateEnable);
                    dropBoxFolderCreateEnable.setChecked(false);
                    accesGrantedDropbox.setContentView(accessGrantedDropboxRoot);
                    accesGrantedDropbox.setCancelable(false);
                    accesGrantedDropbox.show();
                    switchOnDropbox.setChecked(true);
                }
            }
        }
    }

    public void accessGrantedDropbox(View view){
        accesGrantedDropbox.dismiss();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"onStop");
        if(signInProgress){
            signInProgressDialog.dismiss();
            Log.d(TAG,"Reset signinprogess");
            signInProgress = false;
        }
    }

    String accName;
    public void continueToGoogleDrive(){
        if(!isConnectedToInternet()){
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.noConnectionMessage),Toast.LENGTH_SHORT).show();
            switchOnDrive.setChecked(false);
            return;
        }
        initializeGoogleSignIn();
        Set<Scope> requiredScopes = new HashSet<>(2);
        requiredScopes.add(Drive.SCOPE_FILE);
        requiredScopes.add(Drive.SCOPE_APPFOLDER);
        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        Account[] googleAccount = accountManager.getAccountsByType("com.google");
        if (googleAccount != null && googleAccount.length > 0) {
            if(googleAccount.length > 0){
                Log.d(TAG, "Acc name = " + googleAccount[0].name);
                accName = googleAccount[0].name;
            }
        } else {
            Log.d(TAG, "No google account");
        }
        if ((googleAccount != null && googleAccount.length > 0) && signInAccount != null && signInAccount.getGrantedScopes().containsAll(requiredScopes)) {
            getDriveClient(signInAccount);
            signedInDrive = true;
            checkIfFolderCreatedInDrive();
        } else {
            Log.d(TAG,"startActivity");
            signInProgress = true;
            TextView signInText = (TextView)signInProgressRoot.findViewById(R.id.signInText);
            TextView signInprogressTitle = (TextView)signInProgressRoot.findViewById(R.id.savetocloudtitle);
            if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
                signInprogressTitle.setText(getResources().getString(R.string.signInProgressTitle, getResources().getString(R.string.googleDrive)));
                signInText.setText(getResources().getString(R.string.signInProgress, getResources().getString(R.string.googleDrive)));
                ImageView signInImage = (ImageView) signInProgressRoot.findViewById(R.id.signInImage);
                signInImage.setImageDrawable(getResources().getDrawable(R.drawable.google_drive));
            }
            signInProgressDialog.setContentView(signInProgressRoot);
            signInProgressDialog.setCancelable(false);
            signInProgressDialog.show();
            startActivityForResult(googleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        }
    }

    public boolean isConnectedToInternet(){
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    public void checkIfFolderCreatedInDropbox(){
        if(!isConnectedToInternet()){
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.noConnectionMessage),Toast.LENGTH_SHORT).show();
            switchOnDropbox.setChecked(false);
            return;
        }
        TextView uploadFolderMsg = (TextView)uploadFolderCheckRoot.findViewById(R.id.uploadFolderMsg);
        uploadFolderMsg.setText(getResources().getString(R.string.uploadCheckDropboxMsg, getResources().getString(R.string.dropbox)));
        ImageView signinImage = (ImageView)uploadFolderCheckRoot.findViewById(R.id.signInImage);
        signinImage.setImageDrawable(getResources().getDrawable(R.drawable.dropbox));
        uploadFolderCheck.setContentView(uploadFolderCheckRoot);
        uploadFolderCheck.setCancelable(false);
        uploadFolderCheck.show();
        final String folderName = settingsPref.getString(Constants.DROPBOX_FOLDER, "");
        Log.d(TAG, "saved folderName = "+folderName);
        if (folderName != null && !folderName.equals("")) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3000);
                        com.dropbox.core.v2.files.Metadata metadata = dbxClientV2.files().getMetadata("/"+folderName);
                        Log.d(TAG, "dropbox path display = "+metadata.getPathDisplay());
                        Log.d(TAG, "multiline = "+metadata.toStringMultiline());
                        if(!metadata.getName().equals("")) {
                            Log.d(TAG, "Save folder name in setting");
                            ImageView placeholdericon = (ImageView) autoUploadEnabledRoot.findViewById(R.id.placeHolderIconAutoUpload);
                            placeholdericon.setImageDrawable(getResources().getDrawable(R.drawable.dropbox));
                            TextView autoUploadMsg = (TextView) autoUploadEnabledRoot.findViewById(R.id.autoUploadMsg);
                            autoUploadMsg.setText(getResources().getString(R.string.autouploadFolderUpdated, metadata.getName(), getResources().getString(R.string.dropbox)));
                            TextView folderNameTxt = (TextView) autoUploadEnabledRoot.findViewById(R.id.folderName);
                            folderNameTxt.setText(metadata.getName());
                            autoUploadEnabled.setContentView(autoUploadEnabledRoot);
                            autoUploadEnabled.setCancelable(false);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    uploadFolderCheck.dismiss();
                                    autoUploadEnabled.show();
                                    switchOnDropbox.setChecked(true);
                                }
                            });
                            updateDropboxInSetting(metadata.getName(), true);
                        }
                        else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    createUploadFolder();
                                }
                            });
                        }
                    }
                    catch(GetMetadataErrorException metadataerror){
                        Log.d(TAG, "Folder not present = "+metadataerror.getMessage());
                        TextView signInText = (TextView)signInProgressRoot.findViewById(R.id.signInText);
                        TextView signInprogressTitle = (TextView)signInProgressRoot.findViewById(R.id.savetocloudtitle);
                        signInprogressTitle.setText(getResources().getString(R.string.uploadFolderNotExist));
                        signInText.setText(getResources().getString(R.string.uploadFolderMovedDeleted, folderName));
                        ImageView signInImage = (ImageView) signInProgressRoot.findViewById(R.id.signInImage);
                        signInImage.setImageDrawable(getResources().getDrawable(R.drawable.dropbox));
                        signInProgressDialog.setContentView(signInProgressRoot);
                        signInProgressDialog.setCancelable(false);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                uploadFolderCheck.dismiss();
                                signInProgressDialog.show();
                            }
                        });
                        try {
                            Thread.sleep(3500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //Reset folder to FlipCam and give option to user to recreate a subfolder.
                        updateDropboxInSetting(getResources().getString(R.string.app_name), true);
                        TextView dropBoxfolderCreated = (TextView) accessGrantedDropboxRoot.findViewById(R.id.dropBoxFolderCreated);
                        dropBoxfolderCreated.setText(getResources().getString(R.string.autouploadFolderUpdated, getResources().getString(R.string.flipCamAppFolder),
                                getResources().getString(R.string.dropbox)));
                        final CheckBox dropBoxFolderCreateEnable = (CheckBox) accessGrantedDropboxRoot.findViewById(R.id.dropBoxFolderCreateEnable);
                        accesGrantedDropbox.setContentView(accessGrantedDropboxRoot);
                        accesGrantedDropbox.setCancelable(false);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                signInProgressDialog.dismiss();
                                dropBoxFolderCreateEnable.setChecked(false);
                                accesGrantedDropbox.show();
                                switchOnDropbox.setChecked(true);
                            }
                        });
                    }
                    catch (DbxException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        else{
            uploadFolderCheck.dismiss();
            createUploadFolder();
        }
    }

    public void checkIfFolderCreatedInDrive(){
        TextView uploadFolderMsg = (TextView)uploadFolderCheckRoot.findViewById(R.id.uploadFolderMsg);
        uploadFolderMsg.setText(getResources().getString(R.string.uploadCheckMessage, getResources().getString(R.string.googleDrive)));
        ImageView signinImage = (ImageView)uploadFolderCheckRoot.findViewById(R.id.signInImage);
        signinImage.setImageDrawable(getResources().getDrawable(R.drawable.google_drive));
        uploadFolderCheck.setContentView(uploadFolderCheckRoot);
        uploadFolderCheck.setCancelable(false);
        uploadFolderCheck.show();
        final String folderName = settingsPref.getString(Constants.GOOGLE_DRIVE_FOLDER, "");
        Log.d(TAG, "saved folderName = "+folderName);
        if (folderName != null && !folderName.equals("")) {
            mDriveClient.requestSync()
                    .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "sync success");
                            try {
                                Thread.sleep(1100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            queryForFolder(folderName);
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if(!isConnectedToInternet()) {
                                Toast.makeText(getApplicationContext(),getResources().getString(R.string.noConnectionMessage),Toast.LENGTH_SHORT).show();
                                uploadFolderCheck.dismiss();
                                switchOnDrive.setChecked(false);
                                disableGoogleDriveInSetting();
                                googleSignInClient.signOut();
                            }
                            else if(e.getMessage().contains(String.valueOf(DriveStatusCodes.DRIVE_RATE_LIMIT_EXCEEDED))){
                                Log.d(TAG, "sync already done");
                                //Continue as is, since already synced.
                                try {
                                    Thread.sleep(1100);
                                } catch (InterruptedException e1) {
                                    e.printStackTrace();
                                }
                                queryForFolder(folderName);
                            }
                            else if(e.getMessage().contains(String.valueOf(CommonStatusCodes.TIMEOUT))){
                                Toast.makeText(getApplicationContext(),getResources().getString(R.string.timeoutErrorSync),Toast.LENGTH_SHORT).show();
                                uploadFolderCheck.dismiss();
                                switchOnDrive.setChecked(false);
                                disableGoogleDriveInSetting();
                                googleSignInClient.signOut();
                            }
                        }
                    });
        }
        else{
            uploadFolderCheck.dismiss();
            createUploadFolder();
        }
    }

    CustomPropertyKey ownerKey = new CustomPropertyKey("owner", CustomPropertyKey.PUBLIC);
    Query query = null;
    public void queryForFolder(final String folder){
        if(query == null){
            query = new Query.Builder()
                    .addFilter(Filters.eq(SearchableField.TITLE, folder))
                    .addFilter(Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"))
                    .addFilter(Filters.eq(SearchableField.TRASHED, false))
                    .addFilter(Filters.eq(ownerKey, accName))
                    .build();
        }
        mDriveResourceClient.query(query)
                .addOnSuccessListener(new OnSuccessListener<MetadataBuffer>() {
                    @Override
                    public void onSuccess(MetadataBuffer metadatas) {
                        Log.d(TAG, "result metadata = " + metadatas);
                        Iterator<Metadata> iterator = metadatas.iterator();
                        if (metadatas.getCount() > 0 && iterator.hasNext()) {
                            Metadata metadata = iterator.next();
                            final String driveFolderName = metadata.getTitle();
                            Log.d(TAG, "MD title = " + metadata.getTitle());
                            Log.d(TAG, "MD created date = " + metadata.getCreatedDate());
                            Log.d(TAG, "MD drive id = " + metadata.getDriveId());
                            Log.d(TAG, "MD resource id = " + metadata.getDriveId().getResourceId());
                            mDriveClient.getDriveId(metadata.getDriveId().getResourceId())
                                    .addOnSuccessListener(new OnSuccessListener<DriveId>() {
                                        @Override
                                        public void onSuccess(DriveId driveId) {
                                            uploadFolderCheck.dismiss();
                                            ImageView placeholdericon = (ImageView) autoUploadEnabledRoot.findViewById(R.id.placeHolderIconAutoUpload);
                                            placeholdericon.setImageDrawable(getResources().getDrawable(R.drawable.google_drive));
                                            TextView autoUploadMsg = (TextView) autoUploadEnabledRoot.findViewById(R.id.autoUploadMsg);
                                            autoUploadMsg.setText(getResources().getString(R.string.autouploadFolderUpdated, driveFolderName, getResources().getString(R.string.googleDrive)));
                                            TextView folderName = (TextView) autoUploadEnabledRoot.findViewById(R.id.folderName);
                                            folderName.setText(driveFolderName);
                                            autoUploadEnabled.setContentView(autoUploadEnabledRoot);
                                            autoUploadEnabled.setCancelable(false);
                                            autoUploadEnabled.show();
                                            switchOnDrive.setChecked(true);
                                            updateGoogleDriveInSetting(driveFolderName,true,accName);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            if(!isConnectedToInternet()) {
                                                Toast.makeText(getApplicationContext(),getResources().getString(R.string.noConnectionMessage),Toast.LENGTH_SHORT).show();
                                            }
                                            else if(e.getMessage().contains(String.valueOf(CommonStatusCodes.TIMEOUT))){
                                                Toast.makeText(getApplicationContext(),getResources().getString(R.string.timeoutErrorSync),Toast.LENGTH_SHORT).show();
                                            }
                                            uploadFolderCheck.dismiss();
                                            switchOnDrive.setChecked(false);
                                            disableGoogleDriveInSetting();
                                            googleSignInClient.signOut();
                                        }
                                    });
                        } else {
                            Log.d(TAG, "No folder exists with name = " + folder);
                            uploadFolderCheck.dismiss();
                            createUploadFolder();
                        }
                        metadatas.release();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Failure = " + e.getMessage());
                        if(!isConnectedToInternet()) {
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.noConnectionMessage),Toast.LENGTH_SHORT).show();
                        }
                        else if(e.getMessage().contains(String.valueOf(CommonStatusCodes.TIMEOUT))){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.timeoutErrorSync),Toast.LENGTH_SHORT).show();
                        }
                        uploadFolderCheck.dismiss();
                        switchOnDrive.setChecked(false);
                        disableGoogleDriveInSetting();
                        googleSignInClient.signOut();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case GET_ACCOUNTS_PERM:
                if(permissions != null && permissions.length > 0) {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG,"permission given");
                        if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
                            continueToGoogleDrive();
                        }
                    } else {
                        Log.d(TAG,"permission rational");
                        saveToCloud.dismiss();
                        if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
                            switchOnDrive.setChecked(false);
                        }
                    }
                }
                else{
                    super.onRequestPermissionsResult(requestCode,permissions,grantResults);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode != RESULT_OK) {
                    //Sign in failed due to connection problem or user cancelled it.
                    Log.d(TAG, "Sign-in failed.");
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.signinfail),Toast.LENGTH_LONG).show();
                    if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
                        switchOnDrive.setChecked(false);
                        signedInDrive = false;
                        disableGoogleDriveInSetting();
                    }
                    return;
                }
                Task<GoogleSignInAccount> getAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);
                if (getAccountTask.isSuccessful()) {
                    Log.d(TAG,"isSuccessful");
                    getDriveClient(getAccountTask.getResult());
                    signedInDrive = true;
                    //Check For Connectivity again.
                    if(!isConnectedToInternet()){
                        Log.d(TAG,"NO Internet");
                        Toast.makeText(getApplicationContext(),getResources().getString(R.string.noConnectionMessage),Toast.LENGTH_SHORT).show();
                        switchOnDrive.setChecked(false);
                        disableGoogleDriveInSetting();
                    }
                    else {
                        checkIfFolderCreatedInDrive();
                    }
                } else {
                    Log.e(TAG, "Sign-in failed 222.");
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.signinfail),Toast.LENGTH_LONG).show();
                    if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
                        switchOnDrive.setChecked(false);
                        signedInDrive = false;
                        disableGoogleDriveInSetting();
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void disableGoogleDriveInSetting(){
        settingsEditor.putBoolean(Constants.SAVE_TO_GOOGLE_DRIVE, false);
        settingsEditor.commit();
    }

    public void disableDropboxInSetting(){
        settingsEditor.putBoolean(Constants.SAVE_TO_DROPBOX, false);
        settingsEditor.commit();
    }

    private void getDriveClient(GoogleSignInAccount signInAccount) {
        Log.d(TAG,"getDriveClient");
        mDriveClient = Drive.getDriveClient(getApplicationContext(), signInAccount);
        mDriveResourceClient = Drive.getDriveResourceClient(getApplicationContext(), signInAccount);
        Log.d(TAG, "Sign-in SUCCESS.");
    }

    EditText folderNameText;
    public void createUploadFolder(){
        uploadFolderMsg = (TextView)cloudUploadRoot.findViewById(R.id.uploadFolderMsg);
        uploadFolderTitle = (TextView)cloudUploadRoot.findViewById(R.id.uploadFolderTitle);
        uploadDestIcon = (ImageView) cloudUploadRoot.findViewById(R.id.uploadDestIcon);
        if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
            uploadFolderMsg.setText(getResources().getString(R.string.uploadFolder, getResources().getString(R.string.googleDrive)));
            uploadFolderTitle.setText(getResources().getString(R.string.uploadFolderTitle));
            uploadDestIcon.setImageDrawable(getResources().getDrawable(R.drawable.google_drive));
        }
        else if(cloud == Constants.DROPBOX_CLOUD){
            uploadFolderMsg.setText(getResources().getString(R.string.uploadFolder, getResources().getString(R.string.dropbox)));
            uploadFolderTitle.setText(getResources().getString(R.string.uploadFolderTitle));
            uploadDestIcon.setImageDrawable(getResources().getDrawable(R.drawable.dropbox));
        }
        Log.d(TAG,"Open cloud upload dialog");
        cloudUpload.setContentView(cloudUploadRoot);
        cloudUpload.setCancelable(false);
        cloudUpload.show();
        folderNameText = (EditText)cloudUploadRoot.findViewById(R.id.folderNameText);
        folderNameText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                Log.d(TAG,"hasFocus = "+hasFocus);
                if(hasFocus){
                    cloudUpload.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
    }

    private boolean validateFolderNameDropBox(){
        String folderName = ((EditText)cloudUploadRoot.findViewById(R.id.folderNameText)).getText().toString();
        String[] invalidChars = new String[]{"\\","/","?",":","*","\"","|"};
        for(int i=0;i<invalidChars.length;i++){
            if(folderName.contains(invalidChars[i])){
                return false;
            }
        }
        return true;
    }

    private boolean validateFolderNameIsNotEmpty(){
        String folderName = ((EditText)cloudUploadRoot.findViewById(R.id.folderNameText)).getText().toString();
        return !folderName.trim().equals("");
    }

    public void uploadFolder(View view) {
        switch (view.getId()) {
            case R.id.createFolder:
                if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
                    if (validateFolderNameIsNotEmpty()) {
                        cloudUpload.dismiss();
                        if(!isConnectedToInternet()){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.noConnectionMessage),Toast.LENGTH_SHORT).show();
                            switchOnDrive.setChecked(false);
                            disableDropboxInSetting();
                            return;
                        }
                        showCreateProgress();
                        mDriveResourceClient
                                .getRootFolder()
                                .continueWithTask(new Continuation<DriveFolder, Task<DriveFolder>>() {
                                    @Override
                                    public Task<DriveFolder> then(@NonNull Task<DriveFolder> task)
                                            throws Exception {
                                        DriveFolder parentFolder = task.getResult();
                                        CustomPropertyKey ownerKey = new CustomPropertyKey("owner", CustomPropertyKey.PUBLIC);
                                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                                .setTitle(folderNameText.getText().toString())
                                                .setMimeType(DriveFolder.MIME_TYPE)
                                                .setCustomProperty(ownerKey, accName)
                                                .build();
                                        Log.d(TAG,"Creating folder in Drive");
                                        return mDriveResourceClient.createFolder(parentFolder, changeSet);
                                    }
                                })
                                .addOnSuccessListener(this,
                                        new OnSuccessListener<DriveFolder>() {
                                            @Override
                                            public void onSuccess(DriveFolder driveFolder) {
                                                signInProgressDialog.dismiss();
                                                ImageView placeholdericon = (ImageView) autoUploadEnabledWithFolderRoot.findViewById(R.id.placeHolderIconAutoUpload);
                                                placeholdericon.setImageDrawable(getResources().getDrawable(R.drawable.google_drive));
                                                TextView folderCreated = (TextView) autoUploadEnabledWithFolderRoot.findViewById(R.id.folderCreatedMsg);
                                                folderCreated.setText(getResources().getString(R.string.folderCreatedSuccess, folderNameText.getText().toString()));
                                                TextView autoUploadMsg = (TextView) autoUploadEnabledWithFolderRoot.findViewById(R.id.autoUploadMsg);
                                                autoUploadMsg.setText(getResources().getString(R.string.autouploadFolderCreated, getResources().getString(R.string.googleDrive)));
                                                autoUploadEnabledWithFolder.setContentView(autoUploadEnabledWithFolderRoot);
                                                autoUploadEnabledWithFolder.setCancelable(false);
                                                autoUploadEnabledWithFolder.show();
                                                switchOnDrive.setChecked(true);
                                                updateGoogleDriveInSetting(folderNameText.getText().toString(), true, accName);
                                            }
                                        })
                                .addOnFailureListener(this, new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        signInProgressDialog.dismiss();
                                        Log.d(TAG, "Unable to create folder", e);
                                        Toast.makeText(getApplicationContext(),
                                                getResources().getString(R.string.foldercreateErrorGoogleDrive, getResources().getString(R.string.googleDrive)),
                                                Toast.LENGTH_SHORT).show();
                                        switchOnDrive.setChecked(false);
                                        signedInDrive = false;
                                        updateGoogleDriveInSetting("", false, "");
                                        googleSignInClient.signOut();
                                    }
                                });
                    } else {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.uploadFolderEmpty), Toast.LENGTH_SHORT).show();
                    }
                }
                else if(cloud == Constants.DROPBOX_CLOUD){
                    if(validateFolderNameIsNotEmpty() && validateFolderNameDropBox()) {
                        cloudUpload.dismiss();
                        if(!isConnectedToInternet()){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.noConnectionMessage),Toast.LENGTH_SHORT).show();
                            switchOnDropbox.setChecked(false);
                            disableDropboxInSetting();
                            return;
                        }
                        showCreateProgress();
                        final DbxUserFilesRequests dbxUserFilesRequests = dbxClientV2.files();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        CreateFolderResult createFolderResult = dbxUserFilesRequests.createFolderV2("/" + folderNameText.getText().toString());
                                        String folderId = createFolderResult.getMetadata().getId();
                                        if (folderId != null && !folderId.equals("")) {
                                            ImageView placeholdericon = (ImageView) autoUploadEnabledWithFolderRoot.findViewById(R.id.placeHolderIconAutoUpload);
                                            placeholdericon.setImageDrawable(getResources().getDrawable(R.drawable.dropbox));
                                            TextView folderCreated = (TextView) autoUploadEnabledWithFolderRoot.findViewById(R.id.folderCreatedMsg);
                                            folderCreated.setText(getResources().getString(R.string.folderCreatedSuccess, folderNameText.getText().toString()));
                                            TextView autoUploadMsg = (TextView) autoUploadEnabledWithFolderRoot.findViewById(R.id.autoUploadMsg);
                                            autoUploadMsg.setText(getResources().getString(R.string.autouploadFolderCreated, getResources().getString(R.string.dropbox)));
                                            autoUploadEnabledWithFolder.setContentView(autoUploadEnabledWithFolderRoot);
                                            autoUploadEnabledWithFolder.setCancelable(false);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    signInProgressDialog.dismiss();
                                                    autoUploadEnabledWithFolder.show();
                                                    switchOnDropbox.setChecked(true);
                                                }
                                            });
                                            Log.d(TAG, "getPathDisplay = " + createFolderResult.getMetadata().getPathDisplay());
                                            updateDropboxInSetting(createFolderResult.getMetadata().getName(), true);
                                        } else {
                                            Log.d(TAG, "Unable to create folder");
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    signInProgressDialog.dismiss();
                                                    Toast.makeText(getApplicationContext(),
                                                            getResources().getString(R.string.foldercreateErrorGoogleDrive, getResources().getString(R.string.dropbox)),
                                                            Toast.LENGTH_SHORT).show();
                                                    switchOnDropbox.setChecked(false);
                                                }
                                            });
                                            revokeAccessFromDropbox();
                                            updateDropboxInSetting("", false);
                                        }
                                    } catch (DbxException e) {
                                        Log.d(TAG, "Error in creating folder = "+e.getMessage());
                                        e.printStackTrace();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                signInProgressDialog.dismiss();
                                                Toast.makeText(getApplicationContext(),
                                                        getResources().getString(R.string.foldercreateErrorGoogleDrive, getResources().getString(R.string.dropbox)),
                                                        Toast.LENGTH_SHORT).show();
                                                switchOnDropbox.setChecked(false);
                                            }
                                        });
                                        revokeAccessFromDropbox();
                                        updateDropboxInSetting("", false);
                                    }
                                }
                            }).start();
                    }
                    else{
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.uploadFolderDropbox), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.cancelFolder:
                cloudUpload.dismiss();
                if(cloud == Constants.GOOGLE_DRIVE_CLOUD) {
                    switchOnDrive.setChecked(false);
                    signedInDrive = false;
                    //updateGoogleDriveInSetting("",false,"");
                    disableGoogleDriveInSetting();
                    googleSignInClient.signOut();
                    showUploadDisabled();
                }
                else if(cloud == Constants.DROPBOX_CLOUD){
                    switchOnDropbox.setChecked(false);
                    disableDropboxInSetting();
                    revokeAccessFromDropbox();
                    showUploadDisabled();
                }
        }
    }

    public void showCreateProgress(){
        TextView signInText = (TextView)signInProgressRoot.findViewById(R.id.signInText);
        TextView signInprogressTitle = (TextView)signInProgressRoot.findViewById(R.id.savetocloudtitle);
        signInprogressTitle.setText(getResources().getString(R.string.uploadCheckHeader));
        ImageView signInImage = (ImageView) signInProgressRoot.findViewById(R.id.signInImage);
        switch (cloud){
            case Constants.DROPBOX_CLOUD:
            signInText.setText(getResources().getString(R.string.createFolder, getResources().getString(R.string.dropbox)));
            signInImage.setImageDrawable(getResources().getDrawable(R.drawable.dropbox));
                break;
            case Constants.GOOGLE_DRIVE_CLOUD:
            signInText.setText(getResources().getString(R.string.createFolder, getResources().getString(R.string.googleDrive)));
            signInImage.setImageDrawable(getResources().getDrawable(R.drawable.google_drive));
                break;
        }
        signInProgressDialog.setContentView(signInProgressRoot);
        signInProgressDialog.setCancelable(false);
        signInProgressDialog.show();
    }
    public void revokeAccessFromDropbox(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    dbxClientV2.auth().tokenRevoke();
                    Log.d(TAG, "Token revoked");
                } catch (DbxException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        settingsEditor.remove(Constants.DROPBOX_ACCESS_TOKEN);
        settingsEditor.commit();
    }

    public void showUploadDisabled(){
        ImageView placeholderIcon = (ImageView) autoUploadDisabledRoot.findViewById(R.id.placeHolderIconAutoUploadDisabled);
        TextView disabledMsg = (TextView) autoUploadDisabledRoot.findViewById(R.id.autoUploadDisabledMsg);
        switch (cloud){
            case Constants.GOOGLE_DRIVE_CLOUD:
                placeholderIcon.setImageDrawable(getResources().getDrawable(R.drawable.google_drive));
                disabledMsg.setText(getResources().getString(R.string.signoutcloud, getResources().getString(R.string.googleDrive)));
                break;
            case Constants.DROPBOX_CLOUD:
                placeholderIcon.setImageDrawable(getResources().getDrawable(R.drawable.dropbox));
                disabledMsg.setText(getResources().getString(R.string.üploadDisabledDropbox));
                break;
        }
        autoUploadDisabled.setContentView(autoUploadDisabledRoot);
        autoUploadDisabled.setCancelable(false);
        autoUploadDisabled.show();
    }

    public void closeAutoUploadWithFolder(View view){
        autoUploadEnabledWithFolder.dismiss();
    }

    public void closeAutoUpload(View view){
        autoUploadEnabled.dismiss();
    }

    public void closeAutoUploadDisabled(View view){
        autoUploadDisabled.dismiss();
    }

    public void updateGoogleDriveInSetting(String folderName, boolean saveTo, String accname){
        Log.d(TAG, "Saving folder = "+folderName);
        settingsEditor.putString(Constants.GOOGLE_DRIVE_FOLDER,folderName);
        settingsEditor.putBoolean(Constants.SAVE_TO_GOOGLE_DRIVE, saveTo);
        settingsEditor.putString(Constants.GOOGLE_DRIVE_ACC_NAME, accname);
        settingsEditor.commit();
    }

    public void updateDropboxInSetting(String folderName, boolean saveTo){
        Log.d(TAG, "Saving DB folder = "+folderName);
        settingsEditor.putString(Constants.DROPBOX_FOLDER,folderName);
        settingsEditor.putBoolean(Constants.SAVE_TO_DROPBOX, saveTo);
        settingsEditor.commit();
    }
}
