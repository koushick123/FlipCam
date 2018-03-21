package com.flipcam;

import android.app.Dialog;
import android.app.FragmentTransaction;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.flipcam.constants.Constants;
import com.flipcam.media.FileMedia;
import com.flipcam.util.MediaUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

public class CameraActivity extends AppCompatActivity implements VideoFragment.PermissionInterface, PhotoFragment.PhotoPermission,VideoFragment.SwitchInterface,
PhotoFragment.SwitchPhoto, VideoFragment.LowestThresholdCheckForVideoInterface, PhotoFragment.LowestThresholdCheckForPictureInterface{

    private static final String TAG = "CameraActivity";
    private static final String VIDEO = "1";
    VideoFragment videoFragment = null;
    PhotoFragment photoFragment = null;
    View warningMsgRoot;
    Dialog warningMsg;
    Button okButton;
    LayoutInflater layoutInflater;
    AppWidgetManager appWidgetManager;
    SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
        setContentView(R.layout.activity_camera);
        Log.d(TAG, "showVideo = "+getIntent().getExtras().getBoolean("showVideo"));
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if(savedInstanceState == null) {
            //Start with video fragment
            showVideoFragment();
        }
        layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        appWidgetManager = (AppWidgetManager)getSystemService(Context.APPWIDGET_SERVICE);
        warningMsgRoot = layoutInflater.inflate(R.layout.warning_message,null);
        warningMsg = new Dialog(this);
        sharedPreferences = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor settingsEditor = sharedPreferences.edit();
        if(!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
            if(doesSDCardExist() == null){
                settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
                settingsEditor.commit();
                updateWidget();
                TextView warningTitle = (TextView)warningMsgRoot.findViewById(R.id.warningTitle);
                warningTitle.setText(getResources().getString(R.string.sdCardRemovedTitle));
                TextView warningText = (TextView)warningMsgRoot.findViewById(R.id.warningText);
                warningText.setText(getResources().getString(R.string.sdCardNotPresentForRecord));
                okButton = (Button)warningMsgRoot.findViewById(R.id.okButton);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        warningMsg.dismiss();
                        videoFragment.getLatestFileIfExists();
                    }
                });
                warningMsg.setContentView(warningMsgRoot);
                warningMsg.setCancelable(false);
                warningMsg.show();
            }
        }
    }

    public void updateWidget(){
        HashSet<String> widgetIds = (HashSet)sharedPreferences.getStringSet(Constants.WIDGET_IDS, null);
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
        Log.d(TAG, "Deleted first file");
        RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.flipcam_widget);
            FileMedia[] medias = MediaUtil.getMediaList(this);
        if (medias != null && medias.length > 0) {
            String filepath = medias[0].getPath();
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
                    FileMedia[] media = MediaUtil.getMediaList(this);
                    if (media != null && media.length > 0) {
                        mediaMetadataRetriever.setDataSource(filepath);
                        vid = mediaMetadataRetriever.getFrameAtTime(Constants.FIRST_SEC_MICRO);
                    } else {
                        remoteViews.setImageViewResource(R.id.imageWidget, R.drawable.placeholder);
                        remoteViews.setViewVisibility(R.id.playCircleWidget, View.INVISIBLE);
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
            Log.d(TAG, "List empty");
            //List is now empty
            remoteViews.setImageViewResource(R.id.imageWidget, R.drawable.placeholder);
            remoteViews.setViewVisibility(R.id.playCircleWidget, View.INVISIBLE);
            remoteViews.setTextViewText(R.id.widgetMsg, getResources().getString(R.string.widgetNoMedia));
        }
        Log.d(TAG, "Update FC Widget");
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    public String doesSDCardExist(){
//        File[] storage = new File("/storage").listFiles();
        /*File[] mediaDirs = getExternalMediaDirs();
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
        return null;*/
        String sdcardpath = sharedPreferences.getString(Constants.SD_CARD_PATH, "");
        try {
            String filename = "/doesSDCardExist_"+String.valueOf(System.currentTimeMillis()).substring(0,5);
            sdcardpath += filename;
            final String sdCardFilePath = sdcardpath;
            final FileOutputStream createTestFile = new FileOutputStream(sdcardpath);
            Log.d(TAG, "Able to create file... SD Card exists");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    File testfile = new File(sdCardFilePath);
                    try {
                        createTestFile.close();
                        testfile.delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "Unable to create file... SD Card NOT exists..... "+e.getMessage());
            return null;
        }
        return sharedPreferences.getString(Constants.SD_CARD_PATH, "");
    }

    public void goToSettings(View view){
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
        overridePendingTransition(R.anim.slide_from_right,R.anim.slide_to_left);
    }

    @Override
    public void switchToPhoto() {
        showPhotoFragment();
    }

    @Override
    public void switchToVideo() {
        showVideoFragment();
    }

    public void showVideoFragment()
    {
        if(videoFragment == null) {
            Log.d(TAG,"creating videofragment");
            videoFragment = VideoFragment.newInstance();
        }
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        if(photoFragment!=null) {
            fragmentTransaction.replace(R.id.cameraPreview, videoFragment).commit();
            Log.d(TAG,"photofragment removed");
        }
        else{
            fragmentTransaction.add(R.id.cameraPreview, videoFragment, VIDEO).commit();
            Log.d(TAG,"videofragment added");
        }
    }

    public void showPhotoFragment()
    {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        if(photoFragment == null) {
            Log.d(TAG,"creating photofragment");
            photoFragment = PhotoFragment.newInstance();
        }
        fragmentTransaction.replace(R.id.cameraPreview, photoFragment).commit();
        Log.d(TAG,"photofragment added");
        SharedPreferences.Editor settingsEditor = sharedPreferences.edit();
        if(!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
            //Check if SD Card exists
            if(doesSDCardExist() == null){
                settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
                settingsEditor.commit();
                TextView warningTitle = (TextView)warningMsgRoot.findViewById(R.id.warningTitle);
                warningTitle.setText(getResources().getString(R.string.sdCardRemovedTitle));
                TextView warningText = (TextView)warningMsgRoot.findViewById(R.id.warningText);
                warningText.setText(getResources().getString(R.string.sdCardNotPresentForRecord));
                okButton = (Button)warningMsgRoot.findViewById(R.id.okButton);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        warningMsg.dismiss();
                        photoFragment.getLatestFileIfExists();
                    }
                });
                warningMsg.setContentView(warningMsgRoot);
                warningMsg.setCancelable(false);
                warningMsg.show();
            }
        }
        else if(!checkIfPhoneMemoryIsBelowLowThreshold() && !sharedPreferences.getBoolean(Constants.PHONE_MEMORY_DISABLE, true)) {
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            int memoryThreshold = Integer.parseInt(sharedPreferences.getString(Constants.PHONE_MEMORY_LIMIT, ""));
            String memoryMetric = sharedPreferences.getString(Constants.PHONE_MEMORY_METRIC, "");
            StatFs storageStat = new StatFs(Environment.getDataDirectory().getPath());
            long memoryValue = 0;
            String metric = "";
            switch (memoryMetric) {
                case "MB":
                    memoryValue = (memoryThreshold * (long) Constants.MEGA_BYTE);
                    metric = "MB";
                    break;
                case "GB":
                    memoryValue = (memoryThreshold * (long) Constants.GIGA_BYTE);
                    metric = "GB";
                    break;
            }
            Log.d(TAG, "memory value = " + memoryValue);
            Log.d(TAG, "Avail mem = " + storageStat.getAvailableBytes());
            if (storageStat.getAvailableBytes() < memoryValue) {
                LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View thresholdExceededRoot = layoutInflater.inflate(R.layout.threshold_exceeded, null);
                final Dialog thresholdDialog = new Dialog(this);
                TextView memoryLimitMsg = (TextView) thresholdExceededRoot.findViewById(R.id.memoryLimitMsg);
                final CheckBox disableThreshold = (CheckBox) thresholdExceededRoot.findViewById(R.id.disableThreshold);
                Button okButton = (Button) thresholdExceededRoot.findViewById(R.id.okButton);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "disableThreshold.isChecked = " + disableThreshold.isChecked());
                        if (disableThreshold.isChecked()) {
                            editor.remove(Constants.PHONE_MEMORY_LIMIT);
                            editor.remove(Constants.PHONE_MEMORY_METRIC);
                            editor.putBoolean(Constants.PHONE_MEMORY_DISABLE, true);
                            editor.commit();
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.minimumThresholdDisabled), Toast.LENGTH_LONG).show();
                        }
                        thresholdDialog.dismiss();
                    }
                });
                StringBuilder memThreshold = new StringBuilder(memoryThreshold + "");
                memThreshold.append(" ");
                memThreshold.append(metric);
                Log.d(TAG, "memory threshold for display = " + memThreshold);
                memoryLimitMsg.setText(getResources().getString(R.string.thresholdLimitExceededMsg, memThreshold.toString()));
                thresholdDialog.setContentView(thresholdExceededRoot);
                thresholdDialog.setCancelable(false);
                thresholdDialog.show();
            }
        }
    }

    public boolean checkIfPhoneMemoryIsBelowLowThreshold(){
        if(sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
            StatFs storageStat = new StatFs(Environment.getDataDirectory().getPath());
            int lowestThreshold = getResources().getInteger(R.integer.minimumMemoryWarning);
            long lowestMemory = lowestThreshold * (long)Constants.MEGA_BYTE;
            Log.d(TAG, "lowestMemory = "+lowestMemory);
            Log.d(TAG, "avail mem = "+storageStat.getAvailableBytes());
            if(storageStat.getAvailableBytes() < lowestMemory){
                return true;
            }
            else{
                return false;
            }
        }
        else{
            return false;
        }
    }

    @Override
    public boolean checkIfPhoneMemoryIsBelowLowestThresholdForPicture() {
        return checkIfPhoneMemoryIsBelowLowThreshold();
    }

    @Override
    public boolean checkIfPhoneMemoryIsBelowLowestThresholdForVideo() {
        return checkIfPhoneMemoryIsBelowLowThreshold();
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
    }

    @Override
    public void askPermission() {
        askCameraPermission();
    }

    @Override
    public void askPhotoPermission() {
        askCameraPermission();
    }

    public void askCameraPermission(){
        Log.d(TAG,"start permission act to get permissions");
        Intent permission = new Intent(this,PermissionActivity.class);
        permission.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(permission);
        finish();
    }
}
