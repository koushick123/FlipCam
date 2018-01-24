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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.flipcam.R;
import com.flipcam.constants.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by koushick on 13-Dec-17.
 */

public class MediaUploadService extends Service {

    public static final String TAG = "MediaUploadService";
    int NO_OF_REQUESTS = 0;
    String userId;
    Boolean success;
    String uploadFile;
    String filename;
    MediaUploadHandler mediaUploadHandler;
    NotificationManager mNotificationManager;
    android.support.v4.app.NotificationCompat.Builder mBuilder;
    String uploadId;
    int TOTAL_REQUESTS = 0;
    double uploadedSize = 0;
    Bitmap notifyIcon;
    Uri uploadNotification;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i(TAG,"onCreate");
        notifyIcon = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.ic_launcher);
        mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(getApplicationContext())
                        .setLargeIcon(notifyIcon)
                        .setSmallIcon(R.drawable.ic_file_upload)
                        .setContentTitle(getResources().getString(R.string.uploadInProgressTitle))
                        .setContentText("Upload in Progress");
        uploadNotification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SimpleDateFormat sdf = new SimpleDateFormat(getResources().getString(R.string.DATE_FORMAT_FOR_UPLOAD_PROCESS));
        String startID = sdf.format(new Date());
        Log.i(TAG,"onStartCommand = "+startID);
        final String uploadfilepath = (String)intent.getExtras().get("uploadFile");
        userId = (String)intent.getExtras().get("userId");
        Log.i(TAG,"Upload file = "+uploadfilepath);
        mediaUploadHandler = new MediaUploadHandler(this);
        new MediaUploadTask().execute(uploadfilepath, startID);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG,"onDestroy");
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        Log.i(TAG,"onLowMemory");
        super.onLowMemory();
    }

    class MediaUploadHandler extends Handler{
        WeakReference<MediaUploadService> serviceWeakReference;
        public MediaUploadHandler(MediaUploadService service) {
            serviceWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case Constants.UPLOAD_PROGRESS:
                    Log.i(TAG,"upload id = "+uploadId);
                    if(!isImage(uploadFile)) {
                        Double maxSize = (Double) msg.getData().get("maxSize");
                        Double uploadSize = (Double) msg.getData().get("uploadSize");
                        Log.i(TAG, "max size = " + maxSize);
                        Log.i(TAG, "upload size = " + uploadSize);
                        uploadedSize += uploadSize.doubleValue();
                        mBuilder.setProgress((int) maxSize.doubleValue(), (int) uploadedSize, false);
                        mBuilder.setContentTitle(getResources().getString(R.string.uploadInProgressTitle));
                        double roundOffPercent = (Math.floor((uploadedSize / maxSize.intValue()) * 100.0) * 100.0) / 100.0;
                        Log.i(TAG, "Percent done = " + roundOffPercent);
                        mBuilder.setColor(getResources().getColor(R.color.uploadColor));
                        if((int)roundOffPercent < 100.0d) {
                            mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText((int) roundOffPercent + "% Completed of " + convertFileSize(maxSize)
                                    + "\n" + getResources().getString(R.string.uploadInProgress, "Video")
                                    + "\n" + "File "+filename));
                        }
                        else if(roundOffPercent == 100.0d){
                            Log.i(TAG,"Upload finished");
                            mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.uploadFinish)
                                    + "\n" + "File "+filename));
                        }
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

    class MediaUploadTask extends AsyncTask<String,Void,Boolean>{

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            Log.i(TAG,"onPostExecute = "+uploadId);
            uploadedSize = 0;
            if(success){
                mBuilder.setColor(getResources().getColor(R.color.uploadColor));
                mBuilder.setContentText(getResources().getString(R.string.uploadSuccessMessageLess, filename));
                mBuilder.setContentTitle(getResources().getString(R.string.uploadCompleted, getResources().getString(R.string.facebook)));
                mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.uploadSuccessMessage, getResources().getString(R.string.facebook),
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
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                stopSelf();
            }
        }

        @Override
        protected void onCancelled() {
            Log.i(TAG,"onCancelled");
            super.onCancelled();
        }

        @Override
        protected void onPreExecute() {
            Log.i(TAG,"onPreExecute");
            NO_OF_REQUESTS++;
            TOTAL_REQUESTS++;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            uploadFile = params[0];
            uploadId = params[1];
            filename = uploadFile.substring(uploadFile.lastIndexOf("/") + 1,uploadFile.length());
            startUpload(uploadId);
            Log.i(TAG,"EXIT Thread");
            return success;
        }
    }

    public void startUpload(String uploadid){
        try {
            if(doesFileExist()) {
                randomAccessFile = new RandomAccessFile(new File(uploadFile), "r");
                Bundle params = new Bundle();
                GraphRequest postReq;
                GraphRequest.Callback callback;
                String url = "/" + userId;
                //mBuilder.setPriority(Notification.PRIORITY_DEFAULT);
                if (!isImage(uploadFile)) {
                    params.putString("upload_phase", "start");
                    params.putString("file_size", randomAccessFile.length() + "");
                    params.putString("uploadID", uploadid);
                    Log.i(TAG, "file size = " + randomAccessFile.length());
                    callback = postVideoCallback;
                    url += "/videos";
                } else {
                    Bitmap bitmap = BitmapFactory.decodeFile(uploadFile);
                    ByteArrayOutputStream baosBitmap = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baosBitmap);
                    params.putByteArray("source", baosBitmap.toByteArray());
                    Log.i(TAG, "Upload image size = " + baosBitmap.size());
                    baosBitmap.close();
                    bitmap.recycle();
                    callback = postPhotoCallback;
                    url += "/photos";
                    mediaUploadHandler.sendEmptyMessage(Constants.UPLOAD_PROGRESS);
                }
                postReq = new GraphRequest(AccessToken.getCurrentAccessToken(), url, params, HttpMethod.POST, callback);
                postReq.executeAndWait();
                Log.i(TAG, "Request sent");
            }
            else{
                success = false;
                showFileErrorNotification();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            Log.d(TAG,"GB = "+fileSize);
            double gbconsumed = fileSize/Constants.GIGA_BYTE;
            int gbytes = (int) ((Math.floor(gbconsumed * 100.0))/100.0);
            return gbytes+" "+getResources().getString(R.string.MEM_PF_GB);
        }
    }

    public boolean isImage(String path)
    {
        if(path.endsWith(getResources().getString(R.string.IMG_EXT)) || path.endsWith(getResources().getString(R.string.ANOTHER_IMG_EXT))){
            return true;
        }
        return false;
    }

    RandomAccessFile randomAccessFile = null;
    String upload_session_id = null;
    int retryCount = Constants.RETRY_COUNT;
    int subErrorCode;

    public void showUploadErrorNotification(){
        mBuilder.setColor(getResources().getColor(R.color.uploadError));
        mBuilder.setContentText(getResources().getString(R.string.uploadErrorMessage));
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.uploadErrorMessage)));
        mBuilder.setContentTitle(getResources().getString(R.string.errorTitle));
        mBuilder.setSound(uploadNotification);
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
    }

    public void showFileErrorNotification(){
        mBuilder.setColor(getResources().getColor(R.color.uploadError));
        mBuilder.setContentText(getResources().getString(R.string.fileErrorMessage, filename));
        mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.fileErrorMessage, filename)));
        mBuilder.setContentTitle(getResources().getString(R.string.errorTitle));
        mBuilder.setSound(uploadNotification);
        mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
    }

    GraphRequest.Callback postPhotoCallback = new GraphRequest.Callback() {
        @Override
        public void onCompleted(GraphResponse response) {
            Log.i(TAG,"onCompleted response = "+response);
            if(response.getError() != null){
                if(retryCount > 0){
                    Log.i(TAG,"Retrying...."+retryCount);
                    mBuilder.setColor(getResources().getColor(R.color.uploadError));
                    mBuilder.setContentText(getResources().getString(R.string.connectionRetryLess));
                    mBuilder.setContentTitle(getResources().getString(R.string.errorTitle));
                    mBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                    mBuilder.setSound(null);
                    mBuilder.setStyle(
                            new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.connectionRetry, Constants.RETRY_COUNT, retryCount)));
                    mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
                    retryCount--;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    GraphRequest postReq = new GraphRequest(AccessToken.getCurrentAccessToken(), "/" + userId + "/photos",
                            response.getRequest().getParameters(),
                            HttpMethod.POST, postPhotoCallback);
                    postReq.executeAndWait();
                }
                else if(retryCount == 0){
                    retryCount = Constants.RETRY_COUNT;
                    success = false;
                    Log.i(TAG, "Show Photo ERROR = "+uploadId);
                    showUploadErrorNotification();
                }
            }
            else{
                if(retryCount < Constants.RETRY_COUNT){
                    retryCount = Constants.RETRY_COUNT;
                    //mediaUploadHandler.sendEmptyMessage(Constants.UPLOAD_PROGRESS);
                }
                JSONObject jsonObject = response.getJSONObject();
                try {
                    if(jsonObject.has("id") && jsonObject.get("id") != null){
                        success = true;
                    }
                    else{
                        Log.i(TAG,"No id....");
                        success = false;
                        showUploadErrorNotification();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    GraphRequest.Callback postVideoCallback = new GraphRequest.Callback() {
        @Override
        public void onCompleted(GraphResponse response) {
            Log.i(TAG, "response = " + response.getRawResponse());
            if (response.getError() != null) {
                Log.i(TAG, "Error code = " + response.getError().getErrorCode());
                Log.i(TAG, "Error user msg = "+response.getError().getErrorUserMessage());
                Log.i(TAG, "Error subcode = " + response.getError().getSubErrorCode());
                Log.i(TAG, "FB exception = "+response.getError().getException());
                subErrorCode = response.getError().getSubErrorCode();
                if(retryCount > 0){
                    Log.i(TAG,"Retrying...."+retryCount);
                    mBuilder.setColor(getResources().getColor(R.color.uploadError));
                    mBuilder.setContentText(getResources().getString(R.string.connectionRetryLess));
                    mBuilder.setContentTitle(getResources().getString(R.string.errorTitle));
                    mBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                    mBuilder.setSound(null);
                    mBuilder.setStyle(
                            new NotificationCompat.BigTextStyle().bigText(getResources().getString(R.string.connectionRetry, Constants.RETRY_COUNT, retryCount)));
                    mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
                    retryCount--;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    GraphRequest postReq = new GraphRequest(AccessToken.getCurrentAccessToken(), "/" + userId + "/videos",
                            response.getRequest().getParameters(),
                            HttpMethod.POST, postVideoCallback);
                    postReq.executeAndWait();
                }
                else if(retryCount == 0){
                    retryCount = Constants.RETRY_COUNT;
                    success = false;
                    Log.i(TAG, "Show Video ERROR = "+uploadId);
                    showUploadErrorNotification();
                }
            }
            else
            {
                if(retryCount < Constants.RETRY_COUNT){
                    retryCount = Constants.RETRY_COUNT;
                }
                JSONObject jsonObject = response.getJSONObject();
                try {
                    if (jsonObject.has("upload_session_id") || (jsonObject.has("start_offset") || jsonObject.has("end_offset"))) {
                        if (jsonObject.has("upload_session_id")) {
                            upload_session_id = (String) jsonObject.get("upload_session_id");
                        }
                        if(doesFileExist()) {
                            String start_offset = (String) jsonObject.get("start_offset");
                            String end_offset = (String) jsonObject.get("end_offset");
                            byte[] buffer = new byte[(int) (Long.parseLong(end_offset) - Long.parseLong(start_offset))];
                            randomAccessFile.seek(Long.parseLong(start_offset));
                            if (Long.parseLong(start_offset) != Long.parseLong(end_offset)) {
                                Bundle params = new Bundle();
                                Log.i(TAG, "Upload from " + start_offset + " to " + end_offset);
                                params.putString("upload_phase", "transfer");
                                params.putString("upload_session_id", upload_session_id);
                                params.putString("start_offset", start_offset);
                                randomAccessFile.read(buffer);
                                Bundle bundle = new Bundle();
                                Message message = new Message();
                                bundle.putDouble("uploadSize", buffer.length);
                                bundle.putDouble("maxSize", randomAccessFile.length());
                                bundle.putString("filename", uploadFile);
                                message.setData(bundle);
                                message.what = Constants.UPLOAD_PROGRESS;
                                mediaUploadHandler.sendMessage(message);
                                params.putByteArray("video_file_chunk", buffer);
                                GraphRequest postReq = new GraphRequest(AccessToken.getCurrentAccessToken(), "/" + userId + "/videos", params, HttpMethod.POST, postVideoCallback);
                                postReq.executeAndWait();
                            } else {
                                Bundle params = new Bundle();
                                Log.i(TAG, "Complete UPLOAD");
                                params.putString("upload_phase", "finish");
                                params.putString("upload_session_id", upload_session_id);
                                GraphRequest postReq = new GraphRequest(AccessToken.getCurrentAccessToken(), "/" + userId + "/videos", params, HttpMethod.POST, postVideoCallback);
                                postReq.executeAndWait();
                            }
                        }
                        else{
                            Log.i(TAG,"ABORT Upload!!!!!");
                            success = false;
                            showFileErrorNotification();
                        }
                    } else {
                        if (jsonObject.has("success")) {
                            success = (Boolean) jsonObject.get("success");
                            Log.i(TAG, "success = " + success);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };
}
