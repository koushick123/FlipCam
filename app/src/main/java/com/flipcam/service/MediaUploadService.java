package com.flipcam.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i(TAG,"onCreate");
        mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.settings)
                        .setContentTitle("FlipCam")
                        .setContentText("Upload in Progress");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"onStartCommand = "+startId);
        final int startID = startId;
        final String uploadfilepath = (String)intent.getExtras().get("uploadFile");
        userId = (String)intent.getExtras().get("userId");
        Log.i(TAG,"Upload file = "+uploadfilepath);
        mediaUploadHandler = new MediaUploadHandler(this);
        new MediaUploadTask().execute(uploadfilepath, startID+"");
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG,"onDestroy");
        try {
            randomAccessFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        Log.i(TAG,"onLowMemory");
        super.onLowMemory();
    }

    class MediaUploadHandler extends Handler{
        double uploadedSize = 0;
        WeakReference<MediaUploadService> serviceWeakReference;
        public MediaUploadHandler(MediaUploadService service) {
            serviceWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case Constants.UPLOAD_PROGRESS:
                    Log.i(TAG,"upload id = "+uploadId);
                    Double maxSize = (Double)msg.getData().get("maxSize");
                    Double uploadSize = (Double)msg.getData().get("uploadSize");
                    uploadedSize += uploadSize.doubleValue();
                    mBuilder.setProgress((int)maxSize.doubleValue(),(int)uploadedSize,false);
                    double roundOffPercent = (Math.floor((uploadedSize / maxSize.intValue()) * 100.0) * 100.0)/100.0;
                    Log.i(TAG,"Percent done = "+roundOffPercent);
                    mBuilder.setColor(getResources().getColor(R.color.uploadColor));
                    mBuilder.setContentText((int)roundOffPercent+"% Completed     Uploading "+uploadId+" of "+TOTAL_REQUESTS);
                    if(roundOffPercent == 100){
                        uploadedSize = 0;
                    }
                    mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
                    break;
            }
        }
    }

    class MediaUploadTask extends AsyncTask<String,Void,Boolean>{

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            Log.i(TAG,"onPostExecute = "+uploadId);
            if(!success){
                mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText("Unable to upload. Please check your internet connection and try again."));
                mBuilder.setContentText("Upload Interrupted.");
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
            filename = uploadFile.substring(uploadFile.lastIndexOf("/"),uploadFile.length());
            startUpload(uploadId);
            Log.i(TAG,"EXIT Thread");
            return success;
        }
    }

    public void startUpload(String uploadid){
        try {
            randomAccessFile = new RandomAccessFile(new File(uploadFile),"r");
            Bundle params = new Bundle();
            params.putString("upload_phase","start");
            params.putString("file_size",randomAccessFile.length()+"");
            params.putString("uploadID",uploadid);
            Log.i(TAG,"file size = "+randomAccessFile.length());
            GraphRequest postReq = new GraphRequest(AccessToken.getCurrentAccessToken(), "/"+userId+"/videos", params, HttpMethod.POST,postcallback);
            postReq.executeAndWait();
            Log.i(TAG,"Request sent");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void convertFileSize(long fileSize){
        if(fileSize >= Constants.MEGA_BYTE && fileSize < Constants.GIGA_BYTE){
            Log.i(TAG,"MB = "+fileSize);
            double mbconsumed = fileSize/Constants.MEGA_BYTE;
            int mbytes = (int) ((Math.floor(mbconsumed * 100.0))/100.0);
            mBuilder.setProgress(mbytes,0,false);
        }
        else {
            Log.d(TAG,"GB = "+fileSize);
            double gbconsumed = fileSize/Constants.GIGA_BYTE;
            int gbytes = (int) ((Math.floor(gbconsumed * 100.0))/100.0);
            mBuilder.setProgress(gbytes,0,false);
        }
    }

    RandomAccessFile randomAccessFile = null;
    String upload_session_id = null;
    int retryCount = Constants.RETRY_COUNT;
    int subErrorCode;

    GraphRequest.Callback postcallback = new GraphRequest.Callback() {
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
                    mBuilder.setContentText("Possible Connection Loss. Retrying..."+retryCount+" secs");
                    mNotificationManager.notify(Integer.parseInt(uploadId),mBuilder.build());
                    retryCount--;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    GraphRequest postReq = new GraphRequest(AccessToken.getCurrentAccessToken(), "/" + userId + "/videos",
                            response.getRequest().getParameters(),
                            HttpMethod.POST, postcallback);
                    postReq.executeAndWait();
                }
                else if(retryCount == 0){
                    retryCount = Constants.RETRY_COUNT;
                    success = false;
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
                        String start_offset = (String) jsonObject.get("start_offset");
                        String end_offset = (String) jsonObject.get("end_offset");
                        byte[] buffer = new byte[(int)(Long.parseLong(end_offset) - Long.parseLong(start_offset))];
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
                            bundle.putDouble("uploadSize",buffer.length);
                            bundle.putDouble("maxSize",randomAccessFile.length());
                            bundle.putString("filename",uploadFile);
                            message.setData(bundle);
                            message.what = Constants.UPLOAD_PROGRESS;
                            mediaUploadHandler.sendMessage(message);
                            params.putByteArray("video_file_chunk", buffer);
                            GraphRequest postReq = new GraphRequest(AccessToken.getCurrentAccessToken(), "/" + userId + "/videos", params, HttpMethod.POST, postcallback);
                            postReq.executeAndWait();
                        } else {
                            Bundle params = new Bundle();
                            Log.i(TAG, "Complete UPLOAD");
                            params.putString("upload_phase", "finish");
                            params.putString("upload_session_id", upload_session_id);
                            GraphRequest postReq = new GraphRequest(AccessToken.getCurrentAccessToken(), "/" + userId + "/videos", params, HttpMethod.POST, postcallback);
                            postReq.executeAndWait();
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
