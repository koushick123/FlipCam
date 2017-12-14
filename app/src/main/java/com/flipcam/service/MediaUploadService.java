package com.flipcam.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by koushick on 13-Dec-17.
 */

public class MediaUploadService extends Service {

    public static final String TAG = "MediaUploadService";
    int startID;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i(TAG,"onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"onStartCommand = "+startId);
        startID = startId;
        final String uploadfilepath = (String)intent.getExtras().get("uploadFile");
        userId = (String)intent.getExtras().get("userId");
        Log.i(TAG,"Upload file = "+uploadfilepath);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,"Start NEW UPLOAD TASK");
                new MediaUploadTask().execute(uploadfilepath);
            }
        }).start();
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

    String userId;
    Boolean success;
    String uploadFile;
    String filename;

    class MediaUploadTask extends AsyncTask<String,Integer,Boolean>{

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            Log.i(TAG,"onPostExecute");
            stopSelf(startID);
            if(success) {
                Toast.makeText(getApplicationContext(), "FILE UPLOADED " + filename, Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(getApplicationContext(), "THERE WAS AN ISSUE UPLOADING YOUR FILE" + filename+". PLEASE TRY AGAIN LATER", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            Log.i(TAG,"onCancelled");
            super.onCancelled();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            uploadFile = params[0];
            filename = uploadFile.substring(uploadFile.lastIndexOf("/"),uploadFile.length());
            startUpload();
            Log.i(TAG,"EXIT Thread");
            return success;
        }
    }

    public void startUpload(){
        try {
            randomAccessFile = new RandomAccessFile(new File(uploadFile),"r");
            Bundle params = new Bundle();
            params.putString("upload_phase","start");
            params.putString("file_size",randomAccessFile.length()+"");
            Log.i(TAG,"file size = "+randomAccessFile.length());
                /*Log.i(TAG,"Photo = "+medias[selectedPosition].getPath());
                Bitmap image = BitmapFactory.decodeFile(medias[selectedPosition].getPath());
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
                Log.i(TAG,"Image compressed");
                params.putByteArray("source",byteArrayOutputStream.toByteArray());*/
            GraphRequest postReq = new GraphRequest(AccessToken.getCurrentAccessToken(), "/"+userId+"/videos", params, HttpMethod.POST,postcallback);
            //Log.i(TAG,"Graph path = "+postReq.getGraphPath());
            postReq.executeAndWait();
            Log.i(TAG,"Request sent");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    RandomAccessFile randomAccessFile = null;
    String upload_session_id = null;
    int retryCount = 6;

    GraphRequest.Callback postcallback = new GraphRequest.Callback() {
        @Override
        public void onCompleted(GraphResponse response) {
            Log.i(TAG, "response = " + response.getRawResponse());
            if (response.getError() != null) {
                Log.i(TAG, "Error msg = " + response.getError().getErrorMessage());
                Log.i(TAG, "Error code = " + response.getError().getErrorCode());
                Log.i(TAG, "Error subcode = " + response.getError().getErrorMessage());
                Log.i(TAG, "Error recovery msg = " + response.getError().getErrorRecoveryMessage());
                if(retryCount == 0){
                    stopSelf(startID);
                }
                else{
                    retryCount--;
                    Log.i(TAG,"Retrying....");
                    GraphRequest postReq = new GraphRequest(AccessToken.getCurrentAccessToken(), "/" + userId + "/videos",
                            response.getRequest().getParameters(),
                            HttpMethod.POST, postcallback);
                    postReq.executeAndWait();
                }
            }
            else
            {
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
