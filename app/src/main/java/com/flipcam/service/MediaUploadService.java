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
    int NO_OF_THREADS = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG,"onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand = "+startId);
        String uploadfilepath = (String)intent.getExtras().get("uploadFile");
        Log.d(TAG,"Upload file = "+uploadfilepath);
        new MediaUploadTask().execute(uploadfilepath,startId+"");
        /*uploadFile = uploadfilepath;
        GraphRequest meReq = new GraphRequest(AccessToken.getCurrentAccessToken(), "/me", null,HttpMethod.GET,getcallback);
        meReq.executeAsync();*/
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        try {
            randomAccessFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        Log.d(TAG,"onLowMemory");
        super.onLowMemory();
    }

    String userId;
    Boolean success;
    String uploadFile;

    class MediaUploadTask extends AsyncTask<String,Void,Boolean>{
        int startID;

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            Log.d(TAG,"onPostExecute");
            Log.d(TAG,"Stopping ID = "+startID);
            NO_OF_THREADS--;
            stopSelf(startID);
            Toast.makeText(getApplicationContext(),"FILE UPLOADED",Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPreExecute() {
            NO_OF_THREADS++;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onCancelled() {
            Log.d(TAG,"onCancelled");
            super.onCancelled();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            uploadFile = params[0];
            startID = Integer.parseInt(params[1]);
            GraphRequest meReq = new GraphRequest(AccessToken.getCurrentAccessToken(), "/me", null,HttpMethod.GET,getcallback);
            meReq.executeAndWait();
            Log.d(TAG,"EXIT Thread");
            return success;
        }
    }
    RandomAccessFile randomAccessFile = null;
    String upload_session_id = null;
    long startTimeUpload;
    GraphRequest.Callback getcallback = new GraphRequest.Callback() {
        @Override
        public void onCompleted(GraphResponse response) {
            Log.d(TAG,"Fetch user id = "+response.getRawResponse());
            if(response.getError() != null) {
                Log.d(TAG, "onCompleted /me = " + response.getError().getErrorCode());
                Log.d(TAG, "onCompleted /me = " + response.getError().getSubErrorCode());
            }
            JSONObject jsonObject = response.getJSONObject();
            try {
                userId = (String)jsonObject.get("id");
                Log.d(TAG,"USER ID = "+userId);
                Bundle params = new Bundle();
                startTimeUpload = System.currentTimeMillis();
                params.putString("upload_phase","start");
                randomAccessFile = new RandomAccessFile(new File(uploadFile),"r");
                params.putString("file_size",randomAccessFile.length()+"");
                Log.d(TAG,"file size = "+randomAccessFile.length());
                /*Log.d(TAG,"Photo = "+medias[selectedPosition].getPath());
                Bitmap image = BitmapFactory.decodeFile(medias[selectedPosition].getPath());
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
                Log.d(TAG,"Image compressed");
                params.putByteArray("source",byteArrayOutputStream.toByteArray());*/
                GraphRequest postReq = new GraphRequest(AccessToken.getCurrentAccessToken(), "/"+userId+"/videos", params, HttpMethod.POST,postcallback);
                //Log.d(TAG,"Graph path = "+postReq.getGraphPath());
                postReq.executeAndWait();
                Log.d(TAG,"Request sent");
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    int retryCount = 3;

    GraphRequest.Callback postcallback = new GraphRequest.Callback() {
        @Override
        public void onCompleted(GraphResponse response) {
            Log.d(TAG, "response = " + response.getRawResponse());
            if (response.getError() != null) {
                Log.d(TAG, "Error msg = " + response.getError().getErrorMessage());
                Log.d(TAG, "Error code = " + response.getError().getErrorCode());
                Log.d(TAG, "Error subcode = " + response.getError().getErrorMessage());
                Log.d(TAG, "Error recovery msg = " + response.getError().getErrorRecoveryMessage());
                if(retryCount == 0){
                    stopSelf();
                }
                else{
                    retryCount--;
                    Log.d(TAG,"Retrying....");
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
                        //FileInputStream fileInputStream = new FileInputStream(medias[selectedPosition].getPath());
                        byte[] buffer = new byte[(int)(Long.parseLong(end_offset) - Long.parseLong(start_offset))];
                        randomAccessFile.seek(Long.parseLong(start_offset));
                        if (Long.parseLong(start_offset) != Long.parseLong(end_offset)) {
                            Bundle params = new Bundle();
                            Log.d(TAG, "Upload from " + start_offset + " to " + end_offset);
                            params.putString("upload_phase", "transfer");
                            params.putString("upload_session_id", upload_session_id);
                            params.putString("start_offset", start_offset);
                            randomAccessFile.read(buffer);
                            params.putByteArray("video_file_chunk", buffer);
                            GraphRequest postReq = new GraphRequest(AccessToken.getCurrentAccessToken(), "/" + userId + "/videos", params, HttpMethod.POST, postcallback);
                            postReq.executeAndWait();
                        } else {
                            Bundle params = new Bundle();
                            Log.d(TAG, "Complete UPLOAD");
                            params.putString("upload_phase", "finish");
                            params.putString("upload_session_id", upload_session_id);
                            GraphRequest postReq = new GraphRequest(AccessToken.getCurrentAccessToken(), "/" + userId + "/videos", params, HttpMethod.POST, postcallback);
                            postReq.executeAndWait();
                        }
                    } else {
                        if (jsonObject.has("success")) {
                            Boolean success = (Boolean) jsonObject.get("success");
                            Log.d(TAG, "success = " + success);
                            long endtime = System.currentTimeMillis();
                            Log.d(TAG, "time taken = " + (endtime - startTimeUpload) / 1000 + " secs");
                            /*Toast.makeText(getApplicationContext(),"File "+uploadFile+" uploaded successfully",Toast.LENGTH_LONG).show();
                            stopSelf();*/
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
