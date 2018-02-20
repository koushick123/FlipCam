package com.flipcam.service;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CommitInfo;
import com.dropbox.core.v2.files.DbxUserFilesRequests;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadSessionAppendV2Uploader;
import com.dropbox.core.v2.files.UploadSessionCursor;
import com.dropbox.core.v2.files.UploadSessionFinishUploader;
import com.dropbox.core.v2.files.UploadSessionStartResult;
import com.dropbox.core.v2.files.UploadSessionStartUploader;
import com.dropbox.core.v2.files.WriteMode;
import com.flipcam.R;
import com.flipcam.constants.Constants;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
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
    int retryCount = Constants.RETRY_COUNT;

    class DropboxUploadHandler extends Handler {
        WeakReference<DropboxUploadService> serviceWeakReference;

        public DropboxUploadHandler(DropboxUploadService service) {
            serviceWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case Constants.UPLOAD_PROGRESS:
                    Long maxSize = (Long) msg.getData().get("maxSize");
                    Long uploadSize = (Long) msg.getData().get("uploadSize");
                    Log.i(TAG, "max size = " + maxSize);
                    Log.i(TAG, "upload size = " + uploadSize);
                    uploadedSize += uploadSize.longValue();
                    Log.i(TAG, "Total uploaded size = "+uploadedSize);
                    mBuilder.setProgress((int) maxSize.longValue(), (int) uploadedSize, false);
                    mBuilder.setContentTitle(getResources().getString(R.string.autoUploadInProgressTitle, getResources().getString(R.string.dropbox)));
                    double roundOffPercent = (Math.floor((uploadedSize / maxSize.longValue()) * 100.0) * 100.0) / 100.0;
                    Log.i(TAG, "Percent done = " + roundOffPercent);
                    mBuilder.setColor(getResources().getColor(R.color.uploadColor));
                    if((int)roundOffPercent < 100.0d) {
                        if(!isImage(uploadFile)){
                            mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText((int) roundOffPercent + "% Completed of " + convertFileSize(maxSize)
                                    + "\n" + getResources().getString(R.string.uploadInProgress, getResources().getString(R.string.video))
                                    + "\n" + "File " + filename));
                            mBuilder.setContentText(getResources().getString(R.string.uploadInProgress, getResources().getString(R.string.video)));
                        }
                        else {
                            mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText((int) roundOffPercent + "% Completed of " + convertFileSize(maxSize)
                                    + "\n" + getResources().getString(R.string.uploadInProgress, getResources().getString(R.string.photo))
                                    + "\n" + "File " + filename));
                            mBuilder.setContentText(getResources().getString(R.string.uploadInProgress, getResources().getString(R.string.photo)));
                        }
                    }
                    else if(roundOffPercent == 100.0d){
                        Log.i(TAG,"Upload finished");
                        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.uploadFinish)
                                + "\n" + "File "+filename));
                    }
                    mNotificationManager.notify(Integer.parseInt(uploadId), mBuilder.build());
                    break;
            }
        }
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
            dbxClientV2 = new DbxClientV2(dbxRequestConfig, accessToken);
        }
        folderName = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE).getString(Constants.DROPBOX_FOLDER,"");
        dropboxUploadHandler = new DropboxUploadHandler(this);
        Log.i(TAG, "folder name = "+folderName);
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
        new DropboxUploadTask().execute(uploadfilepath,startID);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"onDestroy");
    }

    public boolean isConnectedToInternet(){
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
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
                mBuilder.setContentTitle(getResources().getString(R.string.uploadCompleted, getResources().getString(R.string.dropbox)));
                mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.uploadSuccessMessage, getResources().getString(R.string.dropbox),
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
            mBuilder.setSound(null);
            DbxUserFilesRequests dbxUserFilesRequests = dbxClientV2.files();
            UploadSessionStartUploader uploadSessionStartUploader = null;
            FileInputStream fileToUpload;
            RandomAccessFile randomAccessFile = null;
            UploadSessionStartResult uploadSessionStartResult;
            BufferedInputStream bufferedInputStream;
            UploadSessionAppendV2Uploader uploadSessionAppendV2Uploader = null;
            try {
                fileToUpload = new FileInputStream(uploadFile);
                bufferedInputStream = new BufferedInputStream(fileToUpload);
                randomAccessFile = new RandomAccessFile(new File(uploadFile),"r");
                String sessionId = null;
                int readSize;
                byte[] cache = new byte[500 * 1024];
                if(!isImage(uploadFile)) {
                    cache = new byte[2000 * 1024];
                }
                long bytesUploaded = 0;
                UploadSessionCursor uploadSessionCursor = null;
                while ((readSize = bufferedInputStream.read(cache, 0, cache.length)) != -1) {
                    if (!doesFileExist()) {
                        success = false;
                        sessionId = null;
                        showFileErrorNotification();
                        break;
                    }
                    Log.i(TAG, "Read " + readSize + " bytes");
                    if (sessionId == null) {
                        uploadSessionStartUploader = dbxUserFilesRequests.uploadSessionStart(false);
                        uploadSessionStartResult = uploadSessionStartUploader.uploadAndFinish(new ByteArrayInputStream(cache), readSize);
                        sessionId = uploadSessionStartResult.getSessionId();
                        Log.i(TAG, "Obtained session id = " + sessionId);
                        bytesUploaded = readSize;
                        Log.i(TAG, "Uploaded " + readSize + " bytes");
                    } else {
                        uploadSessionAppendV2Uploader = dbxUserFilesRequests.uploadSessionAppendV2(uploadSessionCursor, false);
                        Log.i(TAG, "Appended session");
                        uploadSessionAppendV2Uploader.uploadAndFinish(new ByteArrayInputStream(cache), readSize);
                        bytesUploaded += readSize;
                        Log.i(TAG, "Uploaded " + bytesUploaded + " bytes");
                    }
                    uploadSessionCursor = new UploadSessionCursor(sessionId, bytesUploaded);
                    Bundle bundle = new Bundle();
                    Message message = new Message();
                    bundle.putLong("uploadSize", readSize);
                    bundle.putLong("maxSize", randomAccessFile.length());
                    bundle.putString("filename", uploadFile);
                    message.setData(bundle);
                    message.what = Constants.UPLOAD_PROGRESS;
                    dropboxUploadHandler.sendMessage(message);
                }
                if (sessionId != null) {
                    String path;
                    if (!folderName.toUpperCase().equals(getResources().getString(R.string.app_name).toUpperCase())) {
                        path = "/" + folderName + "/" + filename;
                    } else {
                        path = "/" + filename;
                    }
                    Log.i(TAG, "Path = " + path);
                    CommitInfo.Builder builder = CommitInfo.newBuilder(path);
                    builder.withAutorename(true);
                    builder.withMode(WriteMode.ADD);
                    builder.withMute(false);
                    UploadSessionFinishUploader uploadSessionFinishUploader = dbxUserFilesRequests.uploadSessionFinish(uploadSessionCursor, builder.build());
                    Log.i(TAG, "Upload session finished");
                    FileMetadata fileMetadata = uploadSessionFinishUploader.uploadAndFinish(fileToUpload, bytesUploaded);
                    Log.i(TAG, "Uploaded file MD getPathDisplay = " + fileMetadata.getPathDisplay());
                    Log.i(TAG, "Uploaded file MD getId = " + fileMetadata.getId());
                    Log.i(TAG, "Uploaded file MD getName = " + fileMetadata.getName());
                    Log.i(TAG, "Uploaded file MD getSize = " + fileMetadata.getSize());
                    Log.i(TAG, "Upload done");
                    success = true;
                }
            } catch (DbxException e) {
                Log.i(TAG, "DbxException = " + e.getMessage());
                if (e.getMessage().contains("Unable to resolve host")) {
                    showUploadErrorNotification();
                }
                else if(e.getMessage().contains("failed to connect")){
                    showTimeoutErrorNotification();
                }
                else if(e.getMessage().contains("invalid_access_token")){
                    showFolderNotExistErrorNotification();
                }
                success = false;
            } catch (FileNotFoundException e) {
                Log.i(TAG ,"FileNotFoundException = "+e.getMessage());
                success = false;
                showFileErrorNotification();
            } catch (IOException e) {
                Log.i(TAG, "IOException = " + e.getMessage());
                success = false;
            } finally {
                if(uploadSessionStartUploader != null) {
                    uploadSessionStartUploader.close();
                }
                if(uploadSessionAppendV2Uploader != null){
                    uploadSessionAppendV2Uploader.close();
                }
                try {
                    if(randomAccessFile != null) {
                        randomAccessFile.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
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

    public boolean doesFileExist(){
        File uploadfile = new File(uploadFile);
        return !uploadfile.isDirectory() && uploadfile.exists();
    }

    public String convertFileSize(double fileSize){
        if(fileSize >= Constants.MEGA_BYTE && fileSize < Constants.GIGA_BYTE){
            Log.i(TAG,"MB = "+fileSize);
            double mbconsumed = fileSize/Constants.MEGA_BYTE;
            int mbytes = (int) ((Math.floor(mbconsumed * 100.0))/100.0);
            return mbytes+" "+getResources().getString(R.string.MEM_PF_MB);
        }
        else {
            Log.i(TAG,"GB = "+fileSize);
            double gbconsumed = fileSize/Constants.GIGA_BYTE;
            int gbytes = (int) ((Math.floor(gbconsumed * 100.0))/100.0);
            return gbytes+" "+getResources().getString(R.string.MEM_PF_GB);
        }
    }

    public void showUploadErrorNotification(){
        mBuilder.setColor(getResources().getColor(R.color.uploadError));
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.uploadErrorMessage)));
        mBuilder.setContentText(getResources().getString(R.string.uploadErrorMessage));
        mBuilder.setContentTitle(getResources().getString(R.string.autoUploadInterrupt, getResources().getString(R.string.dropbox)));
        mBuilder.setSound(uploadNotification);
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
    }

    public void showFileErrorNotification(){
        mBuilder.setColor(getResources().getColor(R.color.uploadError));
        mBuilder.setContentText(getResources().getString(R.string.fileErrorMessage, filename));
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.fileErrorMessage, filename)));
        mBuilder.setContentTitle(getResources().getString(R.string.autoUploadInterrupt, getResources().getString(R.string.dropbox)));
        mBuilder.setSound(uploadNotification);
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
    }

    public void showTimeoutErrorNotification(){
        mBuilder.setColor(getResources().getColor(R.color.uploadError));
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.timeoutError)));
        mBuilder.setContentText(getResources().getString(R.string.timeoutErrorLess));
        mBuilder.setContentTitle(getResources().getString(R.string.autoUploadInterrupt, getResources().getString(R.string.dropbox)));
        mBuilder.setSound(uploadNotification);
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
    }

    public void showFolderNotExistErrorNotification(){
        mBuilder.setColor(getResources().getColor(R.color.uploadError));
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.folderNotExistError, getResources().getString(R.string.flipCamAppFolder),
                getResources().getString(R.string.dropbox))));
        mBuilder.setContentText(getResources().getString(R.string.folderNotExistErrorLess));
        mBuilder.setContentTitle(getResources().getString(R.string.autoUploadInterrupt, getResources().getString(R.string.dropbox)));
        mBuilder.setSound(uploadNotification);
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
    }
}
