package com.flipcam;

import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;

import com.flipcam.constants.Constants;

public class CameraActivity extends AppCompatActivity implements VideoFragment.PermissionInterface, PhotoFragment.PhotoPermission,VideoFragment.SwitchInterface,
PhotoFragment.SwitchPhoto, VideoFragment.LowestThresholdCheckForVideoInterface, PhotoFragment.LowestThresholdCheckForPictureInterface{

    private static final String TAG = "CameraActivity";
    private static final String VIDEO = "1";
    VideoFragment videoFragment = null;
    PhotoFragment photoFragment = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
        checkIfPhoneMemoryIsBelowLowestThreshold();
        setContentView(R.layout.activity_camera);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if(savedInstanceState == null) {
            //Start with video fragment
            showVideoFragment();
        }
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

    public void checkIfPhoneMemoryIsBelowLowestThreshold(){
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        if(sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
            StatFs storageStat = new StatFs(Environment.getDataDirectory().getPath());
            int lowestThreshold = getResources().getInteger(R.integer.minimumMemoryWarning);
            long lowestMemory = lowestThreshold * (long)Constants.MEGA_BYTE;
            Log.d(TAG, "lowestMemory = "+lowestMemory);
            Log.d(TAG, "avail mem = "+storageStat.getAvailableBytes());
            if(storageStat.getAvailableBytes() < lowestMemory){
                LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View thresholdExceededRoot = layoutInflater.inflate(R.layout.threshold_exceeded, null);
                final Dialog thresholdDialog = new Dialog(this);
                TextView memoryLimitMsg = (TextView)thresholdExceededRoot.findViewById(R.id.memoryLimitMsg);
                StringBuilder minimumThreshold = new StringBuilder(lowestThreshold+"");
                minimumThreshold.append(" ");
                minimumThreshold.append(getResources().getString(R.string.MEM_PF_MB));
                memoryLimitMsg.setText(getResources().getString(R.string.minimumThresholdExceeded, minimumThreshold));
                CheckBox disableThreshold = (CheckBox)thresholdExceededRoot.findViewById(R.id.disableThreshold);
                disableThreshold.setVisibility(View.GONE);
                Button okButton = (Button)thresholdExceededRoot.findViewById(R.id.okButton);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        thresholdDialog.dismiss();
                    }
                });
                thresholdDialog.setContentView(thresholdExceededRoot);
                thresholdDialog.setCancelable(false);
                thresholdDialog.show();
            }
        }
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
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        if(!sharedPreferences.getBoolean(Constants.PHONE_MEMORY_DISABLE, true)) {
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
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
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
