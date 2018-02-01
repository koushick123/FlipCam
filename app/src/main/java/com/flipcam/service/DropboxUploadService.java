package com.flipcam.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.UploadSessionAppendV2Uploader;
import com.dropbox.core.v2.files.UploadSessionCursor;
import com.dropbox.core.v2.files.UploadSessionStartResult;
import com.dropbox.core.v2.files.UploadSessionStartUploader;
import com.flipcam.R;
import com.flipcam.constants.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Koushick on 29-01-2018.
 */

public class DropboxUploadService extends Service {

    public static final String TAG = "DropboxUploadService";
    int NO_OF_REQUESTS = 0;
    String uploadFile;
    String filename;
    DropboxUploadService.DropboxUploadHandler dropboxUploadHandler;
    NotificationManager mNotificationManager;
    android.support.v4.app.NotificationCompat.Builder mBuilder;
    String uploadId;
    double uploadedSize = 0;
    Bitmap notifyIcon;
    Uri uploadNotification;
    DbxRequestConfig dbxRequestConfig;
    DbxClientV2 dbxClientV2;
    String folderName;

    class DropboxUploadHandler extends Handler {
        WeakReference<DropboxUploadService> serviceWeakReference;

        public DropboxUploadHandler(DropboxUploadService service) {
            serviceWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case Constants.UPLOAD_PROGRESS:
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SimpleDateFormat sdf = new SimpleDateFormat(getResources().getString(R.string.DATE_FORMAT_FOR_UPLOAD_PROCESS));
        String startID = sdf.format(new Date());
        Log.i(TAG,"onStartCommand = "+startID);
        final String uploadfilepath = (String)intent.getExtras().get("uploadFile");
        Log.i(TAG,"Upload file = "+uploadfilepath);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        notifyIcon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.ic_launcher);
        mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setLargeIcon(notifyIcon)
                .setSmallIcon(R.drawable.ic_file_upload)
                .setContentTitle("")
                .setContentText("");
        uploadNotification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        String accessToken = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE).getString(Constants.DROPBOX_ACCESS_TOKEN,"");
        if(accessToken !=null && !accessToken.equals("")) {
            dbxRequestConfig = new DbxRequestConfig("dropbox/flipCam");
            dbxClientV2 = new DbxClientV2(dbxRequestConfig, Auth.getOAuth2Token());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"onDestroy");
    }

    class DropboxUploadTask extends AsyncTask<String, Void, Boolean>{
        Boolean success = null;
        @Override
        protected void onPreExecute() {
            Log.i(TAG,"onPreExecute");
            NO_OF_REQUESTS++;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            Log.i(TAG,"onPostExecute = "+uploadId+", success = "+success);
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
            Log.i(TAG,"No of requests = "+NO_OF_REQUESTS);
            if(NO_OF_REQUESTS > 0) {
                stopSelf(Integer.parseInt(uploadId));
            }
            else if(NO_OF_REQUESTS == 0){
                stopSelf();
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
            Log.i(TAG, "doInBackground = "+params[1]);
            uploadFile = params[0];
            uploadId = params[1];
            filename = uploadFile.substring(uploadFile.lastIndexOf("/") + 1,uploadFile.length());
            dropboxUploadHandler.sendEmptyMessage(Constants.UPLOAD_PROGRESS);
            DbxUserFilesRequests dbxUserFilesRequests = dbxClientV2.files();
            UploadSessionStartUploader uploadSessionStartUploader = null;
            FileInputStream fileToUpload;
            RandomAccessFile randomAccessFile;
            UploadSessionStartResult uploadSessionStartResult;
            UploadSessionAppendV2Uploader uploadSessionAppendV2Uploader;
            long uploadSize;
            try {
                fileToUpload = new FileInputStream(uploadFile);
                randomAccessFile = new RandomAccessFile(new File(uploadFile),"r");
                uploadSize = randomAccessFile.length();
                String sessionId=null;
                while(true) {
                    if(sessionId == null){
                        uploadSessionStartUploader = dbxUserFilesRequests.uploadSessionStart(false);
                        uploadSessionStartResult = uploadSessionStartUploader.uploadAndFinish(fileToUpload,1024000);
                        sessionId = uploadSessionStartResult.getSessionId();
                    }
                    else {
                        uploadSessionAppendV2Uploader = dbxUserFilesRequests.uploadSessionAppendV2(new UploadSessionCursor(sessionId, 1024000), false);
                    }
                }
            } catch (DbxException e) {
                Log.i(TAG, "DbxException = "+e.getMessage());
            } catch (FileNotFoundException e) {
                Log.i(TAG ,"FileNotFoundException = "+e.getMessage());
            } catch (IOException e) {
                Log.i(TAG, "IOException = "+e.getMessage());
            } finally {
                if(uploadSessionStartUploader != null) {
                    uploadSessionStartUploader.close();
                }
            }
            return success;
        }
    }

    public boolean isImage(String path)
    {
        if(path.endsWith(getResources().getString(R.string.IMG_EXT)) || path.endsWith(getResources().getString(R.string.ANOTHER_IMG_EXT))){
            return true;
        }
        return false;
    }
}
