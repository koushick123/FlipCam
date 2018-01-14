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
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.flipcam.R;
import com.flipcam.constants.Constants;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.DriveStatusCodes;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by koushick on 13-Jan-18.
 */

public class GoogleDriveUploadService extends Service {

    public static final String TAG = "DriveUploadService";
    int NO_OF_REQUESTS = 0;
    Boolean success = false;
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
    DriveFolder uploadToFolder;

    class GoogleUploadHandler extends Handler {
        WeakReference<GoogleDriveUploadService> serviceWeakReference;

        public GoogleUploadHandler(GoogleDriveUploadService service) {
            serviceWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case Constants.UPLOAD_PROGRESS:
                    Log.d(TAG,"upload id = "+uploadId);
                    if(!isImage(uploadFile)) {
                        mBuilder.setContentTitle(getResources().getString(R.string.uploadInProgressTitle));
                        mBuilder.setColor(getResources().getColor(R.color.uploadColor));
                        mBuilder.setContentText(getResources().getString(R.string.uploadInProgress, "Video"));
                        mNotificationManager.notify(Integer.parseInt(uploadId), mBuilder.build());
                    }
                    else{
                        mBuilder.setProgress(0, 0, true);
                        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.uploadInProgress, "Photo")
                                + "\n" + "File "+filename));
                        mBuilder.setColor(getResources().getColor(R.color.uploadColor));
                        mBuilder.setContentTitle(getResources().getString(R.string.uploadInProgressTitle));
                        mBuilder.setContentText(getResources().getString(R.string.uploadInProgress, "Photo"));
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
        Log.d(TAG, "onCreate");
        notifyIcon = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.ic_launcher);
        mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setLargeIcon(notifyIcon)
                .setSmallIcon(R.drawable.ic_file_upload)
                .setContentTitle(getResources().getString(R.string.uploadInProgressTitle))
                .setContentText("Auto Upload in Progress");
        uploadNotification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        initializeGoogleSignIn();
        Set<Scope> requiredScopes = new HashSet<>(2);
        requiredScopes.add(Drive.SCOPE_FILE);
        requiredScopes.add(Drive.SCOPE_APPFOLDER);
        signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        accountManager = (AccountManager)getSystemService(Context.ACCOUNT_SERVICE);
        googleAccount = accountManager.getAccountsByType("com.google");
        if (googleAccount != null && googleAccount.length > 0) {
            for (int i = 0; i < googleAccount.length; i++) {
                Log.d(TAG, "Acc name = " + googleAccount[i].name);
            }
        } else {
            Log.d(TAG, "No google account");
        }
        uploadToFolder = DriveId.decodeFromString(
                getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE).getString(Constants.GOOGLE_DRIVE_FOLDER_ID, "")).asDriveFolder();
        Log.d(TAG, "Upload folder name = "+getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE).getString(Constants.GOOGLE_DRIVE_FOLDER,""));
        googleUploadHandler = new GoogleUploadHandler(this);
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
        SimpleDateFormat sdf = new SimpleDateFormat(getResources().getString(R.string.DATE_FORMAT_FOR_UPLOAD_PROCESS));
        String startID = sdf.format(new Date());
        Log.d(TAG,"onStartCommand = "+startID);
        final String uploadfilepath = (String)intent.getExtras().get("uploadFile");
        Log.d(TAG,"Upload file = "+uploadfilepath);
        googleUploadHandler = new GoogleDriveUploadService.GoogleUploadHandler(this);
        new GoogleDriveUploadTask().execute(uploadfilepath, startID);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    class GoogleDriveUploadTask extends AsyncTask<String,Void,Boolean>{
        @Override
        protected void onPreExecute() {
            Log.d(TAG,"onPreExecute");
            NO_OF_REQUESTS++;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            Log.d(TAG,"onPostExecute = "+uploadId+", success = "+success);
            uploadedSize = 0;
            if(success){
                mBuilder.setColor(getResources().getColor(R.color.uploadColor));
                mBuilder.setContentText(getResources().getString(R.string.uploadSuccessMessageLess, filename));
                mBuilder.setContentTitle(getResources().getString(R.string.uploadCompleted));
                mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.uploadSuccessMessage, filename)));
                mBuilder.setSound(uploadNotification);
                mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
                //Reduce priority so this is pushed down in notification drawer.
                mBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                mBuilder.setSound(null);
                mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
            }
            NO_OF_REQUESTS--;
            Log.d(TAG,"No of requests = "+NO_OF_REQUESTS);
            if(NO_OF_REQUESTS > 0) {
                stopSelf(Integer.parseInt(uploadId));
            }
            else if(NO_OF_REQUESTS == 0){
                stopSelf();
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
            uploadFile = params[0];
            uploadId = params[1];
            filename = uploadFile.substring(uploadFile.lastIndexOf("/") + 1,uploadFile.length());
            startUpload(uploadId);
            Log.d(TAG,"EXIT Thread");
            return success;
        }
    }

    private void getDriveClient(GoogleSignInAccount signInAccount) {
        Log.d(TAG,"getDriveClient");
        mDriveClient = Drive.getDriveClient(getApplicationContext(), signInAccount);
        mDriveResourceClient = Drive.getDriveResourceClient(getApplicationContext(), signInAccount);
        Log.d(TAG, "Sign-in SUCCESS.");
    }

    public boolean isImage(String path)
    {
        if(path.endsWith(getResources().getString(R.string.IMG_EXT)) || path.endsWith(getResources().getString(R.string.ANOTHER_IMG_EXT))){
            return true;
        }
        return false;
    }

    public void startUpload(String uploadId){
        getDriveClient(signInAccount);
        Log.d(TAG, "startUpload");
        googleUploadHandler.sendEmptyMessage(Constants.UPLOAD_PROGRESS);
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
                                Log.d(TAG, "Data size = " + writeLength);
                            } catch (IOException e1) {
                                Log.d(TAG, "Unable to write video file contents.");
                            } finally {
                                outputStream.close();
                            }
                            changeSet = new MetadataChangeSet.Builder()
                                    .setTitle(filename)
                                    .setMimeType("video/mp4")
                                    .setStarred(true)
                                    .build();
                        }
                        else{
                            Bitmap image = BitmapFactory.decodeFile(uploadFile);
                            ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
                            image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
                            try {
                                outputStream.write(bitmapStream.toByteArray());
                            }
                            catch (IOException e1) {
                                Log.d(TAG, "Unable to write image file contents.");
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
                                    .setStarred(true)
                                    .build();
                        }
                        return mDriveResourceClient.createFile(uploadToFolder, changeSet, contents);
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<DriveFile>() {
                            @Override
                            public void onSuccess(DriveFile driveFile) {
                                Toast.makeText(getApplicationContext(),"File "+filename+" Uploaded.",Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(getApplicationContext(),"User needs to Sign in to upload to Google Drive.",Toast.LENGTH_SHORT).show();
                        }
                        else if(e.getMessage().contains(String.valueOf(DriveStatusCodes.DRIVE_RESOURCE_NOT_AVAILABLE))){

                        }
                    }
                });
        }
}
