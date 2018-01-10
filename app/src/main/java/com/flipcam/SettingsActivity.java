package com.flipcam;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.flipcam.constants.Constants;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity{

    public static final String TAG = "SettingsActivity";
    LinearLayout phoneMemParentVert;
    TextView phoneMemText;
    TextView phoneMemTextMsg;
    ImageView greenArrow;
    SharedPreferences settingsPref;
    SharedPreferences.Editor settingsEditor;
    RadioButton phoneMemBtn;
    RadioButton sdCardBtn;
    Dialog sdCardDialog;
    LinearLayout sdcardlayout;
    TextView sdCardPathMsg;
    ImageView editSdCardPath;
    LayoutInflater layoutInflater;
    View sdCardRoot;
    View saveToCloudRoot;
    Switch switchOnDrive;
    Switch switchOnDropbox;
    Dialog saveToCloud;
    TextView savetocloudtitle;
    TextView savetocloudmsg;
    DriveClient mDriveClient;
    DriveResourceClient mDriveResourceClient;
    static final int REQUEST_CODE_SIGN_IN = 0;
    static final int REQUEST_CODE_OPEN_ITEM = 1;
    TaskCompletionSource<DriveId> mOpenItemTaskSource;
    GoogleSignInOptions signInOptions;
    GoogleSignInClient googleSignInClient;
    boolean signedInDrive = false;
    boolean signInDropbox = false;
    Dialog cloudUpload;
    View cloudUploadRoot;
    TextView uploadFolderTitle;
    TextView uploadFolderMsg;
    int cloud = 0; //Default to Google Drive. 1 for Dropbox.
    AccountManager accountManager;
    final int GET_ACCOUNTS_PERM = 100;
    Dialog permissionAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
        setContentView(R.layout.activity_settings);
        phoneMemParentVert = (LinearLayout)findViewById(R.id.phoneMemParentVert);
        phoneMemTextMsg = (TextView)findViewById(R.id.phoneMemTextMsg);
        phoneMemText = (TextView)findViewById(R.id.phoneMemText);
        greenArrow = (ImageView)findViewById(R.id.greenArrow);
        phoneMemBtn = (RadioButton)findViewById(R.id.phoneMemButton);
        sdCardBtn = (RadioButton)findViewById(R.id.sdCardbutton);
        sdCardPathMsg = (TextView)findViewById(R.id.sdcardpathmsg);
        editSdCardPath = (ImageView)findViewById(R.id.editSdCardPath);
        switchOnDropbox = (Switch)findViewById(R.id.switchOnDropbox);
        switchOnDrive = (Switch)findViewById(R.id.switchOnDrive);
        sdcardlayout = (LinearLayout)findViewById(R.id.sdcardlayout);
        phoneMemText.setText(getResources().getString(R.string.phoneMemoryLimit, getResources().getInteger(R.integer.minimumMemoryWarning), "MB"));
        getSupportActionBar().setTitle(getResources().getString(R.string.settingTitle));
        settingsPref = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        settingsEditor = settingsPref.edit();
        if(settingsPref.contains(Constants.SAVE_MEDIA_PHONE_MEM)){
            Log.d(TAG,"Phone memory exists");
            if(settingsPref.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM,true)){
                Log.d(TAG,"Phone memory is true");
                phoneMemBtn.setChecked(true);
                sdCardBtn.setChecked(false);
                editSdCardPath.setClickable(false);
            }
            else{
                Log.d(TAG,"Phone memory is false");
                phoneMemBtn.setChecked(false);
                sdCardBtn.setChecked(true);
                editSdCardPath.setClickable(true);
            }
        }
        else{
            Log.d(TAG,"Phone memory NOT exists");
            phoneMemBtn.setChecked(true);
            sdCardBtn.setChecked(false);
        }
        Log.d(TAG,"SD Card Path onCreate = "+settingsPref.getString(Constants.SD_CARD_PATH,""));
        if(settingsPref.contains(Constants.SD_CARD_PATH) && !settingsPref.getString(Constants.SD_CARD_PATH,"").equals("")) {
            String sdcardpath = settingsPref.getString(Constants.SD_CARD_PATH, "");
            showSDCardPath(sdcardpath);
        }
        else{
            hideSDCardPath();
        }
        layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        sdCardRoot = layoutInflater.inflate(R.layout.sd_card_location,null);
        saveToCloudRoot = layoutInflater.inflate(R.layout.save_to_cloud,null);
        cloudUploadRoot = layoutInflater.inflate(R.layout.cloud_upload_folder,null);
        sdCardDialog = new Dialog(this);
        saveToCloud = new Dialog(this);
        cloudUpload = new Dialog(this);
        permissionAccount = new Dialog(this);
        accountManager = (AccountManager)getSystemService(Context.ACCOUNT_SERVICE);
        Log.d(TAG,"saveToCloud = "+saveToCloud );
        updatePhoneMemoryText();
        Signature[] sigs = new Signature[0];
        try {
            sigs = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES).signatures;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        for (Signature sig : sigs)
        {
            Log.d(TAG, "Signature hashcode : " + sig.hashCode());
        }
    }

    public void updatePhoneMemoryText(){
        if(settingsPref.contains(Constants.PHONE_MEMORY_DISABLE)){
            if(!settingsPref.getBoolean(Constants.PHONE_MEMORY_DISABLE, false)){
                String memoryLimit = settingsPref.getString(Constants.PHONE_MEMORY_LIMIT, getResources().getInteger(R.integer.minimumMemoryWarning) + "");
                String memoryMetric = settingsPref.getString(Constants.PHONE_MEMORY_METRIC, "MB");
                phoneMemText.setText(getResources().getString(R.string.phoneMemoryLimit, Integer.parseInt(memoryLimit), memoryMetric));
            }
            else{
                phoneMemText.setText(getResources().getString(R.string.phoneMemoryLimitDisabled));
            }
        }
        else{
            String memoryLimit = settingsPref.getString(Constants.PHONE_MEMORY_LIMIT, getResources().getInteger(R.integer.minimumMemoryWarning) + "");
            String memoryMetric = settingsPref.getString(Constants.PHONE_MEMORY_METRIC, "MB");
            phoneMemText.setText(getResources().getString(R.string.phoneMemoryLimit, Integer.parseInt(memoryLimit), memoryMetric));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        updatePhoneMemoryText();
    }

    public void openSdCardPath(View view){
        if(settingsPref.contains(Constants.SD_CARD_PATH)) {
            ((EditText) sdCardRoot.findViewById(R.id.sdCardPathText)).setText(settingsPref.getString(Constants.SD_CARD_PATH,""));
        }
        Configuration config = getResources().getConfiguration();
        if(config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            TextView sdcardText = (TextView)sdCardRoot.findViewById(R.id.sdCardMsg);
            sdcardText.setText(getResources().getString(R.string.sdCardPathPortrait));
        }
        else{
            TextView sdcardText = (TextView)sdCardRoot.findViewById(R.id.sdCardMsg);
            sdcardText.setText(getResources().getString(R.string.sdCardPathLandscape));
        }
        sdCardDialog.setContentView(sdCardRoot);
        sdCardDialog.setCancelable(false);
        sdCardDialog.show();
    }

    public void selectSaveMedia(View view){
        switch (view.getId()){
            case R.id.phoneMemButton:
                Log.d(TAG,"Save in phone memory");
                settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,true);
                settingsEditor.commit();
                phoneMemBtn.setChecked(true);
                sdCardBtn.setChecked(false);
                editSdCardPath.setClickable(false);
                break;
            case R.id.sdCardbutton:
                Log.d(TAG,"Save in sd card");
                phoneMemBtn.setChecked(false);
                sdCardBtn.setChecked(true);
                editSdCardPath.setClickable(true);
                if(!settingsPref.contains(Constants.SD_CARD_PATH)) {
                    openSdCardPath(view);
                }
                else{
                    settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,false);
                    settingsEditor.commit();
                }
                break;
        }
    }

    public void saveSdCardPath(View view){
        switch (view.getId()){
            case R.id.okSdCard:
                Log.d(TAG,"Checking if path is valid");
                String path;
                path = ((EditText) sdCardRoot.findViewById(R.id.sdCardPathText)).getText().toString();
                Log.d(TAG,"Path = "+path);
                File sdCard = new File(path);
                if(!sdCard.exists() || !sdCard.isDirectory()){
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.sdCardPathNotExist),Toast.LENGTH_SHORT).show();
                }
                else{
                    String fullPath;
                    Log.d(TAG,"Existing path = "+sdCard.getPath());
                    if(!sdCard.getPath().contains(getResources().getString(R.string.app_name))) {
                        if (sdCard.getPath().endsWith("/")) {
                            String pathExcludeFrontSlash = sdCard.getPath().substring(0, sdCard.getPath().length() - 1);
                            fullPath = pathExcludeFrontSlash + getResources().getString(R.string.FC_ROOT);
                        } else {
                            fullPath = sdCard.getPath() + getResources().getString(R.string.FC_ROOT);
                        }
                        Log.d(TAG, "Full path = " + fullPath);
                        File fc = new File(fullPath);
                        if (!fc.exists()) {
                            fc.mkdir();
                            Log.d(TAG, "Able to create FC");
                        }
                        Toast.makeText(getApplicationContext(),getResources().getString(R.string.sdCardPathSaved),Toast.LENGTH_SHORT).show();
                        settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,false);
                        settingsEditor.putString(Constants.SD_CARD_PATH,fc.getPath());
                        settingsEditor.commit();
                        showSDCardPath(fc.getPath());
                    }
                    else{
                        Toast.makeText(getApplicationContext(),getResources().getString(R.string.sdCardPathSaved),Toast.LENGTH_SHORT).show();
                        settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,false);
                        settingsEditor.putString(Constants.SD_CARD_PATH,sdCard.getPath());
                        settingsEditor.commit();
                        showSDCardPath(sdCard.getPath());
                    }
                    sdCardDialog.dismiss();
                }
                break;
            case R.id.cancelSdCard:
                Log.d(TAG,"SD Card Path = "+settingsPref.getString(Constants.SD_CARD_PATH,""));
                if(settingsPref.contains(Constants.SD_CARD_PATH) && !settingsPref.getString(Constants.SD_CARD_PATH,"").equalsIgnoreCase("")){
                    phoneMemBtn.setChecked(false);
                    sdCardBtn.setChecked(true);
                    settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,false);
                    settingsEditor.commit();
                }
                else{
                    phoneMemBtn.setChecked(true);
                    sdCardBtn.setChecked(false);
                    settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,true);
                    settingsEditor.commit();
                }
                sdCardDialog.dismiss();
                Log.d(TAG,"cancel sd card");
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

    public void reDrawSDCardScreen(){
        Configuration config = getResources().getConfiguration();
        if(config.orientation == Configuration.ORIENTATION_PORTRAIT){
            TextView sdcardText = (TextView)sdCardRoot.findViewById(R.id.sdCardMsg);
            sdcardText.setText(getResources().getString(R.string.sdCardPathPortrait));
        }
        else{
            TextView sdcardText = (TextView)sdCardRoot.findViewById(R.id.sdCardMsg);
            sdcardText.setText(getResources().getString(R.string.sdCardPathLandscape));
        }
    }

    public void showMemoryConsumed(View view){
        Intent memoryAct = new Intent(SettingsActivity.this, MemoryLimitActivity.class);
        startActivity(memoryAct);
        overridePendingTransition(R.anim.slide_from_right,R.anim.slide_to_left);
    }

    public void saveToCloudDrive(View view) {
        if (switchOnDrive.isChecked()) {
            cloud = 0;
            savetocloudtitle = (TextView)saveToCloudRoot.findViewById(R.id.savetocloudtitle);
            savetocloudtitle.setText(getResources().getString(R.string.saveToCloudTitle, getResources().getString(R.string.googleDrive)));
            savetocloudmsg = (TextView)saveToCloudRoot.findViewById(R.id.savetocloudmsg);
            savetocloudmsg.setText(getResources().getString(R.string.signinmsg, getResources().getString(R.string.googleDrive)));
            saveToCloud.setContentView(saveToCloudRoot);
            saveToCloud.setCancelable(false);
            saveToCloud.show();
        }
        else{
            if(signedInDrive) {
                googleSignInClient.signOut();
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.signoutcloud, getResources().getString(R.string.googleDrive)), Toast.LENGTH_SHORT).show();
                settingsEditor.putBoolean(Constants.SAVE_TO_GOOGLE_DRIVE , false);
                settingsEditor.commit();
            }
        }
    }

    public void signInToCloud(View view){
        switch (view.getId()){
            case R.id.continueSignIn:
                saveToCloud.dismiss();
                if(cloud == 0){
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
                else{
                    //Sign in to Dropbox
                }
                break;
            case R.id.cancelSignIn:
                saveToCloud.dismiss();
                if(cloud == 0) {
                    settingsEditor.putBoolean(Constants.SAVE_TO_GOOGLE_DRIVE , false);
                    settingsEditor.commit();
                    switchOnDrive.setChecked(false);
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
                if(cloud == 0) {
                    switchOnDrive.setChecked(false);
                }
                permissionAccount.dismiss();
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.permissionRationale), Toast.LENGTH_LONG).show();
                break;
        }
    }

    public void continueToGoogleDrive(){
        signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .requestScopes(Drive.SCOPE_APPFOLDER)
                        .build();
        googleSignInClient = GoogleSignIn.getClient(this, signInOptions);
        Set<Scope> requiredScopes = new HashSet<>(2);
        requiredScopes.add(Drive.SCOPE_FILE);
        requiredScopes.add(Drive.SCOPE_APPFOLDER);
        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        Account[] googleAccount = accountManager.getAccountsByType("com.google");
        if (googleAccount != null && googleAccount.length > 0) {
            for (int i = 0; i < googleAccount.length; i++) {
                Log.d(TAG, "Acc name = " + googleAccount[i].name);
            }
        } else {
            Log.d(TAG, "No google account");
        }
        if ((googleAccount != null && googleAccount.length > 0) && signInAccount != null && signInAccount.getGrantedScopes().containsAll(requiredScopes)) {
            getDriveClient(signInAccount);
            signedInDrive = true;
            checkIfFolderCreatedInDrive();
        } else {
            startActivityForResult(googleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        }
    }

    public void checkIfFolderCreatedInDrive(){
        final String driveFolder = getSharedPreferences(Constants.FC_SETTINGS,Context.MODE_PRIVATE).getString(Constants.GOOGLE_DRIVE_FOLDER,"");
        Log.d(TAG,"folder name = "+driveFolder);
        if(driveFolder != null && !driveFolder.equals("")) {
            String driveId = getSharedPreferences(Constants.FC_SETTINGS,Context.MODE_PRIVATE).getString(Constants.GOOGLE_DRIVE_FOLDER_ID,"");
            Log.d(TAG,"GET DriveId = "+driveId);
            if(driveId != null && !driveId.equals("")){
                DriveId folderId = DriveId.decodeFromString(driveId);
                Task<Metadata> metadata = mDriveResourceClient.getMetadata(folderId.asDriveFolder());
                metadata.addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Unable to get Folder = " + driveFolder);
                        createUploadFolder();
                    }
                });
                metadata.addOnSuccessListener(this, new OnSuccessListener<Metadata>() {
                    @Override
                    public void onSuccess(Metadata metadata) {
                        Log.d(TAG,"metadata isInAppFolder = "+metadata.isInAppFolder());
                        Log.d(TAG, "Drive id is = " + metadata.getDriveId());
                        Log.d(TAG, "created datte = "+metadata.getCreatedDate());
                        Log.d(TAG, "Title = "+metadata.getTitle());
                        Toast.makeText(getApplicationContext(),getResources().getString(R.string.googleDriveFolderCreated, metadata.getTitle()),Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else{
                createUploadFolder();
            }
            /*driveIdTask.addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "Unable to get Folder = " + driveFolder);
                    createUploadFolder();
                }
            });
            driveIdTask.addOnSuccessListener(this, new OnSuccessListener<DriveId>() {
                @Override
                public void onSuccess(DriveId driveId) {
                    Log.d(TAG, "Drive id is = " + driveId);
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.googleDriveFolderCreated),Toast.LENGTH_SHORT).show();
                }
            });*/
        }
        else{
            createUploadFolder();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case GET_ACCOUNTS_PERM:
                if(permissions != null && permissions.length > 0) {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG,"permission given");
                        if(cloud == 0) {
                            continueToGoogleDrive();
                        }
                    } else {
                        Log.d(TAG,"permission rational");
                        saveToCloud.dismiss();
                        if(cloud == 0) {
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.permissionRationale), Toast.LENGTH_LONG).show();
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
                    if(cloud == 0) {
                        switchOnDrive.setChecked(false);
                        signedInDrive = false;
                    }
                    return;
                }

                Task<GoogleSignInAccount> getAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);
                if (getAccountTask.isSuccessful()) {
                    getDriveClient(getAccountTask.getResult());
                    signedInDrive = true;
                    checkIfFolderCreatedInDrive();
                } else {
                    Log.e(TAG, "Sign-in failed.");
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.signinfail),Toast.LENGTH_LONG).show();
                    if(cloud == 0) {
                        switchOnDrive.setChecked(false);
                        signedInDrive = false;
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void getDriveClient(GoogleSignInAccount signInAccount) {
        mDriveClient = Drive.getDriveClient(getApplicationContext(), signInAccount);
        mDriveResourceClient = Drive.getDriveResourceClient(getApplicationContext(), signInAccount);
        Log.d(TAG, "Sign-in SUCCESS.");
    }

    EditText folderNameText;
    public void createUploadFolder(){
        uploadFolderMsg = (TextView)cloudUploadRoot.findViewById(R.id.uploadFolderMsg);
        uploadFolderTitle = (TextView)cloudUploadRoot.findViewById(R.id.uploadFolderTitle);
        if(cloud == 0) {
            uploadFolderMsg.setText(getResources().getString(R.string.uploadFolder, getResources().getString(R.string.googleDrive)));
            uploadFolderTitle.setText(getResources().getString(R.string.uploadFolderTitle, getResources().getString(R.string.googleDrive)));
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
                if(validateFolderNameIsNotEmpty()) {
                    cloudUpload.dismiss();
                    mDriveResourceClient
                            .getRootFolder()
                            .continueWithTask(new Continuation<DriveFolder, Task<DriveFolder>>() {
                                @Override
                                public Task<DriveFolder> then(@NonNull Task<DriveFolder> task)
                                        throws Exception {
                                    DriveFolder parentFolder = task.getResult();
                                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                            .setTitle(folderNameText.getText().toString())
                                            .setMimeType(DriveFolder.MIME_TYPE)
                                            .build();
                                    return mDriveResourceClient.createFolder(parentFolder, changeSet);
                                }
                            })
                            .addOnSuccessListener(this,
                                    new OnSuccessListener<DriveFolder>() {
                                        @Override
                                        public void onSuccess(DriveFolder driveFolder) {
                                            Toast.makeText(getApplicationContext(),
                                                    getResources().getString(R.string.foldercreateSuccessGoogleDrive, folderNameText.getText().toString()),
                                                    Toast.LENGTH_SHORT).show();
                                            Log.d(TAG,"SAVE Driveid = "+driveFolder.getDriveId().encodeToString());
                                            updateGoogleDriveInSetting(folderNameText.getText().toString(),true,driveFolder.getDriveId().encodeToString());
                                        }
                                    })
                            .addOnFailureListener(this, new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d(TAG, "Unable to create folder", e);
                                    Toast.makeText(getApplicationContext(),
                                            getResources().getString(R.string.foldercreateErrorGoogleDrive, folderNameText.getText().toString()),
                                            Toast.LENGTH_SHORT).show();
                                    switchOnDrive.setChecked(false);
                                    signedInDrive = false;
                                    updateGoogleDriveInSetting("",false,"");
                                }
                            });
                }
                else{
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.uploadFolderEmpty),Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.cancelFolder:
                cloudUpload.dismiss();
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.signinfail),Toast.LENGTH_LONG).show();
                if(cloud == 0) {
                    switchOnDrive.setChecked(false);
                    signedInDrive = false;
                    updateGoogleDriveInSetting("",false,"");
                }
        }
    }

    public void updateGoogleDriveInSetting(String folderName, boolean saveTo, String folderId){
        settingsEditor.putString(Constants.GOOGLE_DRIVE_FOLDER,folderName);
        settingsEditor.putBoolean(Constants.SAVE_TO_GOOGLE_DRIVE, saveTo);
        settingsEditor.putString(Constants.GOOGLE_DRIVE_FOLDER_ID, folderId);
        settingsEditor.commit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reDrawSDCardScreen();
    }
}
