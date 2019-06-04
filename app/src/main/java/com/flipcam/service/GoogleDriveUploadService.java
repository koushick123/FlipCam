package com.flipcam.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.flipcam.R;
import com.flipcam.constants.Constants;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by koushick on 13-Jan-18.
 */

public class GoogleDriveUploadService extends Service {

    public static final String TAG = "DriveUploadService";
    int NO_OF_REQUESTS = 0;
    String uploadFile;
    String filename;
    GoogleDriveUploadService.GoogleUploadHandler googleUploadHandler;
    NotificationManager mNotificationManager;
    android.support.v4.app.NotificationCompat.Builder mBuilder;
    String uploadId;
    double uploadedSize = 0;
    Bitmap notifyIcon;
    Uri uploadNotification;
    DriveClient mDriveClient;
    DriveResourceClient mDriveResourceClient;
    GoogleSignInOptions signInOptions;
    GoogleSignInClient googleSignInClient = null;
    AccountManager accountManager;
    GoogleSignInAccount signInAccount;
    Account[] googleAccount;
    MetadataChangeSet changeSet;
    DriveContents contents;
    String folderName;
    String accountName;
    DriveFolder uploadToFolder;
    boolean VERBOSE = false;

    class GoogleUploadHandler extends Handler {
        WeakReference<GoogleDriveUploadService> serviceWeakReference;

        public GoogleUploadHandler(GoogleDriveUploadService service) {
            serviceWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case Constants.UPLOAD_PROGRESS:
                    if(VERBOSE)Log.i(TAG,"upload id = "+uploadId);
                    if(!isImage(uploadFile)) {
                        mBuilder.setContentTitle(getResources().getString(R.string.autoUploadInProgressTitle, getResources().getString(R.string.googleDrive)));
                        mBuilder.setColor(getResources().getColor(R.color.uploadColor));
                        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.uploadInProgress, "Video")
                                + "\n" + "File "+filename));
                        mBuilder.setContentText(getResources().getString(R.string.uploadInProgress, "Video"));
                        mNotificationManager.notify(Integer.parseInt(uploadId), mBuilder.build());
                    }
                    else{
                        mBuilder.setProgress(0, 0, true);
                        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.uploadInProgress, "Photo")
                                + "\n" + "File "+filename));
                        mBuilder.setColor(getResources().getColor(R.color.uploadColor));
                        mBuilder.setContentTitle(getResources().getString(R.string.autoUploadInProgressTitle, getResources().getString(R.string.googleDrive)));
                        //mBuilder.setContentText(getResources().getString(R.string.uploadInProgress, "Photo")+ "\n" + "File "+filename);
                        mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
                    }
                    break;
            }
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(VERBOSE)Log.i(TAG, "onCreate");
        notifyIcon = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.ic_launcher);
        mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setLargeIcon(notifyIcon)
                .setSmallIcon(R.drawable.ic_file_upload)
                .setContentTitle("")
                .setContentText("");
        uploadNotification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        initializeGoogleSignIn();
        Set<Scope> requiredScopes = new HashSet<>(2);
        requiredScopes.add(Drive.SCOPE_FILE);
        requiredScopes.add(Drive.SCOPE_APPFOLDER);
        signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        accountManager = (AccountManager)getSystemService(Context.ACCOUNT_SERVICE);
        googleAccount = accountManager.getAccountsByType("com.google");
        if (googleAccount != null && googleAccount.length > 0) {
            for (int i = 0; i < googleAccount.length; i++) {
                if(VERBOSE)Log.i(TAG, "Acc name = " + googleAccount[i].name);
            }
        } else {
            if(VERBOSE)Log.i(TAG, "No google account");
        }

        if(VERBOSE)Log.i(TAG, "Upload folder name = "+getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE).getString(Constants.GOOGLE_DRIVE_FOLDER,""));
        folderName = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE).getString(Constants.GOOGLE_DRIVE_FOLDER,"");
        accountName = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE).getString(Constants.GOOGLE_DRIVE_ACC_NAME,"");
        googleUploadHandler = new GoogleUploadHandler(this);
        getDriveClient(signInAccount);
    }

    public boolean isConnectedToInternet(){
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //SimpleDateFormat sdf = new SimpleDateFormat(getResources().getString(R.string.DATE_FORMAT_FOR_UPLOAD_PROCESS));
        String randomNo = Math.random()+"";
        String startID = randomNo.substring(randomNo.length() - 5, randomNo.length());
        if(VERBOSE)Log.i(TAG,"onStartCommand = "+startID);
        final String uploadfilepath = (String)intent.getExtras().get("uploadFile");
        if(VERBOSE)Log.i(TAG,"Upload file = "+uploadfilepath);
        new GoogleDriveUploadTask().execute(uploadfilepath, startID);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(VERBOSE)Log.i(TAG, "onDestroy");
    }

    class GoogleDriveUploadTask extends AsyncTask<String,Void,Boolean>{
        Boolean success = null;
        @Override
        protected void onPreExecute() {
            if(VERBOSE)Log.i(TAG,"onPreExecute");
            NO_OF_REQUESTS++;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(VERBOSE)Log.i(TAG,"onPostExecute = "+uploadId+", success = "+success);
            uploadedSize = 0;
            if(success){
                mNotificationManager.cancel(Integer.parseInt(uploadId));
                mBuilder.setColor(getResources().getColor(R.color.turqoise));
                mBuilder.setContentText(getResources().getString(R.string.uploadSuccessMessageLess, filename));
                mBuilder.setContentTitle(getResources().getString(R.string.uploadCompleted, getResources().getString(R.string.googleDrive)));
                mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.uploadSuccessMessage, getResources().getString(R.string.googleDrive),
                        filename)));
                mBuilder.setSound(uploadNotification);
                mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
            }
            NO_OF_REQUESTS--;
            if(VERBOSE)Log.i(TAG,"No of requests = "+NO_OF_REQUESTS);
            if(NO_OF_REQUESTS > 0) {
                stopSelf(Integer.parseInt(uploadId));
            }
            else if(NO_OF_REQUESTS == 0){
                stopSelf();
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
            if(VERBOSE)Log.i(TAG, "doInBackground = "+params[1]);
            uploadFile = params[0];
            uploadId = params[1];
            filename = uploadFile.substring(uploadFile.lastIndexOf("/") + 1,uploadFile.length());
            googleUploadHandler.sendEmptyMessage(Constants.UPLOAD_PROGRESS);
            mDriveClient.requestSync()
                    .addOnFailureListener(new OnFailureListener(){
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if(VERBOSE)Log.i(TAG,"Unable to sync = ");
                            if(VERBOSE)Log.i(TAG,"Message = "+e.getMessage());
                            if(e.getMessage().contains(String.valueOf(DriveStatusCodes.DRIVE_RATE_LIMIT_EXCEEDED))){
                                //Continue as is, since already synced.
                                CustomPropertyKey customPropertyKey = new CustomPropertyKey("owner", CustomPropertyKey.PUBLIC);
                                Query query = new Query.Builder()
                                        .addFilter(Filters.eq(SearchableField.TITLE, folderName))
                                        .addFilter(Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"))
                                        .addFilter(Filters.eq(SearchableField.TRASHED , false))
                                        .addFilter(Filters.eq(customPropertyKey, accountName))
                                        .build();
                                mDriveResourceClient.query(query)
                                        .addOnSuccessListener(new OnSuccessListener<MetadataBuffer>() {
                                            @Override
                                            public void onSuccess(MetadataBuffer metadatas) {
                                                if(VERBOSE)Log.i(TAG, "result metadata = "+metadatas);
                                                Iterator<Metadata> iterator = metadatas.iterator();
                                                if(metadatas.getCount() > 0) {
                                                    while (iterator.hasNext()) {
                                                        Metadata temp = iterator.next();
                                                        if(VERBOSE)Log.i(TAG, "MD title = " + temp.getTitle());
                                                        if(VERBOSE)Log.i(TAG, "MD created date = " + temp.getCreatedDate());
                                                        if(VERBOSE)Log.i(TAG, "MD drive id = " + temp.getDriveId());
                                                        if(VERBOSE)Log.i(TAG, "MD resource id = " + temp.getDriveId().getResourceId());
                                                        mDriveClient.getDriveId(temp.getDriveId().getResourceId())
                                                                .addOnSuccessListener(new OnSuccessListener<DriveId>() {
                                                                    @Override
                                                                    public void onSuccess(DriveId driveId) {
                                                                        uploadToFolder = driveId.asDriveFolder();
                                                                        if(VERBOSE)Log.i(TAG, "New Drive id = " + uploadToFolder.getDriveId());
                                                                        if(VERBOSE)Log.i(TAG, "startUpload");
                                                                        mDriveResourceClient.createContents()
                                                                                .continueWithTask(new Continuation<DriveContents, Task<DriveFile>>() {
                                                                                    @Override
                                                                                    public Task<DriveFile> then(@NonNull Task<DriveContents> task)
                                                                                            throws Exception {
                                                                                        contents = task.getResult();
                                                                                        OutputStream outputStream = contents.getOutputStream();
                                                                                        FileInputStream fileInputStream = new FileInputStream(uploadFile);
                                                                                        if(!isImage(uploadFile)) {
                                                                                            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                                                                                            int data;
                                                                                            byte[] cache = new byte[102400];
                                                                                            int writeLength = 0;
                                                                                            try {
                                                                                                while ((data = bufferedInputStream.read(cache, 0, cache.length)) != -1) {
                                                                                                    outputStream.write(cache, 0, data);
                                                                                                    writeLength += data;
                                                                                                }
                                                                                                if(VERBOSE)Log.i(TAG, "Data size = " + writeLength);
                                                                                            } catch (IOException e1) {
                                                                                                if(VERBOSE)Log.i(TAG, "Unable to write video file contents.");
                                                                                            } finally {
                                                                                                outputStream.close();
                                                                                            }
                                                                                            changeSet = new MetadataChangeSet.Builder()
                                                                                                    .setTitle(filename)
                                                                                                    .setMimeType("video/mp4")
                                                                                                    .build();
                                                                                        }
                                                                                        else{
                                                                                            if(VERBOSE)Log.i(TAG, "Send IMAGE file");
                                                                                            Bitmap image = BitmapFactory.decodeFile(uploadFile);
                                                                                            ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
                                                                                            image.compress(Bitmap.CompressFormat.JPEG, 100, bitmapStream);
                                                                                            try {
                                                                                                if(VERBOSE)Log.i(TAG, "Writing image to contents");
                                                                                                outputStream.write(bitmapStream.toByteArray());
                                                                                            }
                                                                                            catch (IOException e1) {
                                                                                                if(VERBOSE)Log.i(TAG, "Unable to write image file contents.");
                                                                                            } finally {
                                                                                                outputStream.close();
                                                                                            }
                                                                                            StringBuffer mimeType = new StringBuffer("image/");
                                                                                            if(filename.endsWith(getResources().getString(R.string.IMG_EXT))) {
                                                                                                mimeType.append("jpeg");
                                                                                            }
                                                                                            else{
                                                                                                mimeType.append("jpg");
                                                                                            }
                                                                                            changeSet = new MetadataChangeSet.Builder()
                                                                                                    .setTitle(filename)
                                                                                                    .setMimeType(mimeType.toString())
                                                                                                    .build();
                                                                                        }
                                                                                        return mDriveResourceClient.createFile(uploadToFolder, changeSet, contents);
                                                                                    }
                                                                                })
                                                                                .addOnSuccessListener(new OnSuccessListener<DriveFile>() {
                                                                                    @Override
                                                                                    public void onSuccess(DriveFile driveFile) {
                                                                                        success = true;
                                                                                    }
                                                                                })
                                                                                .addOnFailureListener( new OnFailureListener() {
                                                                                    @Override
                                                                                    public void onFailure(@NonNull Exception e) {
                                                                                        Log.e(TAG, "Unable to upload file"+e.getMessage());
                                                                                        e.printStackTrace();
                                                                                        success = false;
                                                                                        if(e.getMessage().contains("The user must be signed in to make this API call")) {
                                                                                            showSignInNeededNotification();
                                                                                        }
                                                                                        else if(e.getMessage().contains(String.valueOf(CommonStatusCodes.TIMEOUT))){
                                                                                            showTimeoutErrorNotification();
                                                                                        }
                                                                                    }
                                                                                });
                                                                    }
                                                                })
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        if(VERBOSE)Log.i(TAG, "unable to get driveid = " + e.getMessage());
                                                                        success = false;
                                                                        showFolderNotExistErrorNotification();
                                                                    }
                                                                });
                                                    }
                                                }
                                                else{
                                                    success = false;
                                                    showFolderNotExistErrorNotification();
                                                    if(VERBOSE)Log.i(TAG, "No folder exists with name = "+folderName);
                                                }
                                                metadatas.release();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                if(VERBOSE)Log.i(TAG, "Failure = "+e.getMessage());
                                                success = false;
                                                if(!isConnectedToInternet()) {
                                                    showUploadErrorNotification();
                                                }
                                            }
                                        });
                            }
                            else if(e.getMessage().contains(String.valueOf(CommonStatusCodes.TIMEOUT))){
                                success = false;
                                showTimeoutErrorNotification();
                            }
                            else if(e.getMessage().contains(String.valueOf(CommonStatusCodes.SIGN_IN_REQUIRED))) {
                                success = false;
                                showSignInNeededNotification();
                            }
                            else{
                                success = false;
                                if(e.getMessage().contains(String.valueOf(CommonStatusCodes.INTERNAL_ERROR)) || !isConnectedToInternet()){
                                    showUploadErrorNotification();
                                }
                            }
                        }
                    })
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if(VERBOSE)Log.i(TAG,"Metadata upto date");
                            CustomPropertyKey customPropertyKey = new CustomPropertyKey("owner", CustomPropertyKey.PUBLIC);
                            Query query = new Query.Builder()
                                    .addFilter(Filters.eq(SearchableField.TITLE, folderName))
                                    .addFilter(Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"))
                                    .addFilter(Filters.eq(SearchableField.TRASHED , false))
                                    .addFilter(Filters.eq(customPropertyKey, accountName))
                                    .build();
                            mDriveResourceClient.query(query)
                                    .addOnSuccessListener(new OnSuccessListener<MetadataBuffer>() {
                                        @Override
                                        public void onSuccess(MetadataBuffer metadatas) {
                                            if(VERBOSE)Log.i(TAG, "result metadata = "+metadatas);
                                            Iterator<Metadata> iterator = metadatas.iterator();
                                            if(metadatas.getCount() > 0) {
                                                while (iterator.hasNext()) {
                                                    Metadata temp = iterator.next();
                                                    if(VERBOSE)Log.i(TAG, "MD title = " + temp.getTitle());
                                                    if(VERBOSE)Log.i(TAG, "MD created date = " + temp.getCreatedDate());
                                                    if(VERBOSE)Log.i(TAG, "MD drive id = " + temp.getDriveId());
                                                    if(VERBOSE)Log.i(TAG, "MD resource id = " + temp.getDriveId().getResourceId());
                                                    mDriveClient.getDriveId(temp.getDriveId().getResourceId())
                                                            .addOnSuccessListener(new OnSuccessListener<DriveId>() {
                                                                @Override
                                                                public void onSuccess(DriveId driveId) {
                                                                    uploadToFolder = driveId.asDriveFolder();
                                                                    if(VERBOSE)Log.i(TAG, "New Drive id = " + uploadToFolder.getDriveId());
                                                                    if(VERBOSE)Log.i(TAG, "startUpload");
                                                                    mDriveResourceClient.createContents()
                                                                            .continueWithTask(new Continuation<DriveContents, Task<DriveFile>>() {
                                                                                @Override
                                                                                public Task<DriveFile> then(@NonNull Task<DriveContents> task)
                                                                                        throws Exception {
                                                                                    contents = task.getResult();
                                                                                    OutputStream outputStream = contents.getOutputStream();
                                                                                    if(doesFileExist()) {
                                                                                        FileInputStream fileInputStream = new FileInputStream(uploadFile);
                                                                                        if (!isImage(uploadFile)) {
                                                                                            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                                                                                            int data;
                                                                                            byte[] cache = new byte[102400];
                                                                                            int writeLength = 0;
                                                                                            try {
                                                                                                while ((data = bufferedInputStream.read(cache, 0, cache.length)) != -1) {
                                                                                                    outputStream.write(cache, 0, data);
                                                                                                    writeLength += data;
                                                                                                }
                                                                                                if(VERBOSE)Log.i(TAG, "Data size = " + writeLength);
                                                                                            } catch (IOException e1) {
                                                                                                if(VERBOSE)Log.i(TAG, "Unable to write video file contents.");
                                                                                            } finally {
                                                                                                outputStream.close();
                                                                                            }
                                                                                            changeSet = new MetadataChangeSet.Builder()
                                                                                                    .setTitle(filename)
                                                                                                    .setMimeType("video/mp4")
                                                                                                    .build();
                                                                                        } else {
                                                                                            if(VERBOSE)Log.i(TAG, "Send IMAGE file");
                                                                                            Bitmap image = BitmapFactory.decodeFile(uploadFile);
                                                                                            ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
                                                                                            image.compress(Bitmap.CompressFormat.JPEG, 100, bitmapStream);
                                                                                            try {
                                                                                                if(VERBOSE)Log.i(TAG, "Writing image to contents");
                                                                                                outputStream.write(bitmapStream.toByteArray());
                                                                                            } catch (IOException e1) {
                                                                                                if(VERBOSE)Log.i(TAG, "Unable to write image file contents.");
                                                                                            } finally {
                                                                                                outputStream.close();
                                                                                            }
                                                                                            StringBuffer mimeType = new StringBuffer("image/");
                                                                                            if (filename.endsWith(getResources().getString(R.string.IMG_EXT))) {
                                                                                                mimeType.append("jpeg");
                                                                                            } else {
                                                                                                mimeType.append("jpg");
                                                                                            }
                                                                                            changeSet = new MetadataChangeSet.Builder()
                                                                                                    .setTitle(filename)
                                                                                                    .setMimeType(mimeType.toString())
                                                                                                    .build();
                                                                                        }
                                                                                    }
                                                                                    else{
                                                                                        if(VERBOSE)Log.i(TAG, "File not found exception");
                                                                                        success = false;
                                                                                        showFileErrorNotification();
                                                                                        stopSelf(Integer.parseInt(uploadId));
                                                                                    }
                                                                                    return mDriveResourceClient.createFile(uploadToFolder, changeSet, contents);
                                                                                }
                                                                            })
                                                                            .addOnSuccessListener(new OnSuccessListener<DriveFile>() {
                                                                                @Override
                                                                                public void onSuccess(DriveFile driveFile) {
                                                                                    success = true;
                                                                                }
                                                                            })
                                                                            .addOnFailureListener( new OnFailureListener() {
                                                                                @Override
                                                                                public void onFailure(@NonNull Exception e) {
                                                                                    Log.e(TAG, "Unable to upload file"+e.getMessage());
                                                                                    e.printStackTrace();
                                                                                    success = false;
                                                                                    if(e.getMessage().contains("The user must be signed in to make this API call")) {
                                                                                        showSignInNeededNotification();
                                                                                    }
                                                                                    else if(e.getMessage().contains(String.valueOf(CommonStatusCodes.TIMEOUT))){
                                                                                        showTimeoutErrorNotification();
                                                                                    }
                                                                                }
                                                                            });
                                                                }
                                                            })
                                                            .addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull Exception e) {
                                                                    if(VERBOSE)Log.i(TAG, "unable to get driveid = " + e.getMessage());
                                                                    success = false;
                                                                    showFolderNotExistErrorNotification();
                                                                }
                                                            });
                                                }
                                            }
                                            else{
                                                success = false;
                                                showFolderNotExistErrorNotification();
                                                if(VERBOSE)Log.i(TAG, "No folder exists with name = "+folderName);
                                            }
                                            metadatas.release();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            if(VERBOSE)Log.i(TAG, "Failure = "+e.getMessage());
                                            success = false;
                                            if(!isConnectedToInternet()) {
                                                showUploadErrorNotification();
                                            }
                                        }
                                    });
                        }
                    });
            while(success == null){

            }
            if(VERBOSE)Log.i(TAG,"EXIT Thread");
            return success;
        }
    }

    public boolean doesFileExist(){
        File uploadfile = new File(uploadFile);
        return !uploadfile.isDirectory() && uploadfile.exists();
    }

    private void getDriveClient(GoogleSignInAccount signInAccount) {
        if(VERBOSE)Log.i(TAG,"getDriveClient");
        mDriveClient = Drive.getDriveClient(getApplicationContext(), signInAccount);
        mDriveResourceClient = Drive.getDriveResourceClient(getApplicationContext(), signInAccount);
        if(VERBOSE)Log.i(TAG, "Sign-in SUCCESS.");
    }

    public boolean isImage(String path)
    {
        if(path.endsWith(getResources().getString(R.string.IMG_EXT)) || path.endsWith(getResources().getString(R.string.ANOTHER_IMG_EXT))){
            return true;
        }
        return false;
    }

    public void showTimeoutErrorNotification(){
        mBuilder.setColor(getResources().getColor(R.color.uploadError));
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.timeoutError)));
        mBuilder.setContentText(getResources().getString(R.string.timeoutErrorLess));
        mBuilder.setContentTitle(getResources().getString(R.string.autoUploadInterrupt, getResources().getString(R.string.googleDrive)));
        mBuilder.setSound(uploadNotification);
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
    }

    public void showUploadErrorNotification(){
        mBuilder.setColor(getResources().getColor(R.color.uploadError));
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.uploadErrorMessage)));
        mBuilder.setContentText(getResources().getString(R.string.uploadErrorMessage));
        mBuilder.setContentTitle(getResources().getString(R.string.autoUploadInterrupt, getResources().getString(R.string.googleDrive)));
        mBuilder.setSound(uploadNotification);
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
    }

    public void showFolderNotExistErrorNotification(){
        mBuilder.setColor(getResources().getColor(R.color.uploadError));
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.folderNotExistError, folderName,
                getResources().getString(R.string.googleDrive))));
        mBuilder.setContentText(getResources().getString(R.string.folderNotExistErrorLess));
        mBuilder.setContentTitle(getResources().getString(R.string.autoUploadInterrupt, getResources().getString(R.string.googleDrive)));
        mBuilder.setSound(uploadNotification);
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
    }

    public void showSignInNeededNotification(){
        mBuilder.setColor(getResources().getColor(R.color.uploadError));
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.signInError)));
        mBuilder.setContentText(getResources().getString(R.string.signInErrorLess));
        mBuilder.setContentTitle(getResources().getString(R.string.autoUploadInterrupt, getResources().getString(R.string.googleDrive)));
        mBuilder.setSound(uploadNotification);
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
    }

    public void showFileErrorNotification(){
        mBuilder.setColor(getResources().getColor(R.color.uploadError));
        mBuilder.setContentText(getResources().getString(R.string.fileErrorMessage, filename));
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.fileErrorMessage, filename)));
        mBuilder.setContentTitle(getResources().getString(R.string.autoUploadInterrupt, getResources().getString(R.string.googleDrive)));
        mBuilder.setSound(uploadNotification);
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
    }
}
