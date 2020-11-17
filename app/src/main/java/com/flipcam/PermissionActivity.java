package com.flipcam;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.flipcam.constants.Constants;

public class PermissionActivity extends AppCompatActivity {

    final String TAG = "PermissionActivity";
    private final int ALL_PERMISSIONS = 0;
    static final String AUDIO_PERMISSION = "android.permission.RECORD_AUDIO";
    static final String CAMERA_PERMISSION = "android.permission.CAMERA";
    static final String STORAGE_PERMISSIONS = "android.permission.WRITE_EXTERNAL_STORAGE";
    public static final String FC_SHARED_PREFERENCE = "FC_Settings";
    public static final String FC_MEDIA_PREFERENCE = "FC_Media";
    boolean cameraPermission = false;
    boolean audioPermission = false;
    boolean storagePermission = false;
    boolean showMessage = false;
    boolean showPermission = false;
    DialogInterface.OnClickListener exitListener;
    AlertDialog.Builder alertDialog;
    private static SharedPreferences sharedPreferences;
    boolean VERBOSE = false;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ALL_PERMISSIONS) {
            if (permissions != null && permissions.length > 0) {
                if(VERBOSE)Log.d(TAG, "For camera == "+permissions[0]);
                if (permissions[0].equalsIgnoreCase(CAMERA_PERMISSION) && permissions[1].equalsIgnoreCase(AUDIO_PERMISSION) &&
                        permissions[2].equalsIgnoreCase(STORAGE_PERMISSIONS)) {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED
                            && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                        cameraPermission = true;
                        audioPermission = true;
                        storagePermission = true;
                        openCameraFragment();
                    } else {
                        quitFlipCam();
                    }
                }
            } else {
                super.onRequestPermissionsResult(requestCode,permissions,grantResults);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(VERBOSE)Log.d(TAG,"onCreate");
        setContentView(R.layout.activity_permission);
        if(VERBOSE)Log.d(TAG,"saved instance state == "+savedInstanceState);
        if(savedInstanceState!=null) {
            if(VERBOSE)Log.d(TAG, "saved instance state restart == " + savedInstanceState.getBoolean("restart"));
            if(VERBOSE)Log.d(TAG, "saved instance state quit == " + savedInstanceState.getBoolean("quit"));
        }
        sharedPreferences = getSharedPreferences(FC_SHARED_PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("startCamera",false);
        editor.commit();
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if(savedInstanceState!=null){
            if(savedInstanceState.getBoolean("restart")){
                showMessage = true;
                quitFlipCam();
            }
        }
    }

    public static SharedPreferences getSharedPreferences()
    {
        return sharedPreferences;
    }

    @Override
    protected void onDestroy() {
        if(VERBOSE)Log.d(TAG,"onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        if(VERBOSE)Log.d(TAG,"onStart");
        super.onStart();
    }

    @Override
    protected void onStop() {
        if(VERBOSE)Log.d(TAG,"onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        if(VERBOSE)Log.d(TAG,"onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        if(VERBOSE)Log.d(TAG,"onResume");
        super.onResume();
        SharedPreferences sharedPreferences = getSharedPreferences();
        if(sharedPreferences.getBoolean("startCamera",false)){
            if(VERBOSE)Log.d(TAG,"Quit the app");
            finish();
        }
        else if(!showMessage){
            if(VERBOSE)Log.d(TAG,"Check permissions and Start camera = "+showPermission);
            int camerapermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
            int audiopermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
            int storagepermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (camerapermission == PackageManager.PERMISSION_GRANTED && audiopermission == PackageManager.PERMISSION_GRANTED &&
                    storagepermission == PackageManager.PERMISSION_GRANTED) {
                if(VERBOSE)Log.d(TAG, "ALL permissions obtained.");
                cameraPermission = true;
                audioPermission = true;
                storagePermission= true;
                openCameraFragment();
            } else if(!showPermission){
                if(VERBOSE)Log.d(TAG, "Permissions not obtained. Obtain explicitly");
                //Remove shared preferences. This is necessary, since for some devices it is pre-selected
                //leading to errors.
                SharedPreferences videoPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String videoResPref = videoPref.getString(Constants.SELECT_VIDEO_RESOLUTION, null);
                if(VERBOSE)Log.d(TAG, "videoResPref = "+videoResPref);
                SharedPreferences.Editor editor = videoPref.edit();
                editor.remove(Constants.SELECT_VIDEO_RESOLUTION);
                editor.remove(Constants.SUPPORT_VIDEO_RESOLUTIONS);
                editor.remove(Constants.VIDEO_DIMENSION_HIGH);
                editor.remove(Constants.VIDEO_DIMENSION_MEDIUM);
                editor.remove(Constants.VIDEO_DIMENSION_LOW);
                editor.remove(Constants.SUPPORT_PHOTO_RESOLUTIONS);
                editor.remove(Constants.SELECT_PHOTO_RESOLUTION);
                editor.remove(Constants.SUPPORT_PHOTO_RESOLUTIONS_FRONT);
                editor.remove(Constants.SELECT_PHOTO_RESOLUTION_FRONT);
                editor.remove(Constants.SELECT_VIDEO_PLAYER);
                editor.putBoolean(Constants.SHOW_EXTERNAL_PLAYER_MESSAGE, true);
                editor.commit();
                String phoneLoc = getResources().getString(R.string.phoneLocation);
                SharedPreferences.Editor mediaLocEditor = sharedPreferences.edit();
                mediaLocEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
                mediaLocEditor.putString(Constants.MEDIA_LOCATION_VIEW_SELECT, phoneLoc);
                mediaLocEditor.putString(Constants.MEDIA_LOCATION_VIEW_SELECT_PREV, phoneLoc);
                mediaLocEditor.commit();
                if(VERBOSE)Log.d(TAG, "REMOVED SHAREDPREFS");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        ALL_PERMISSIONS);
                showPermission = true;
            }
        }
    }

    void openCameraFragment()
    {
        if(cameraPermission && audioPermission && storagePermission) {
            //Open VideoFragment under CameraActivity showing camera preview.
            SharedPreferences sharedPreferences = getSharedPreferences();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("startCamera", true);
            editor.commit();
            Intent cameraIntent = new Intent(this, CameraActivity.class);
            startActivity(cameraIntent);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if(VERBOSE)Log.d(TAG,"Restore state = "+savedInstanceState);
        if(savedInstanceState!=null && savedInstanceState.getBoolean("quit")) {
            //The activity was restarted because of possible low memory situation.
            if(VERBOSE)Log.d(TAG, "Quit app");
            finish();
        }
        else if(savedInstanceState!= null && savedInstanceState.getBoolean("showPermission")){
            showPermission = savedInstanceState.getBoolean("showPermission");
            if(VERBOSE)Log.d(TAG,"show permission = "+showPermission);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if(VERBOSE)Log.d(TAG,"Save before restart");
        if(showMessage) {
            outState.putBoolean("restart", true);
            outState.putBoolean("quit",false);
            if(VERBOSE)Log.d(TAG, "Saved restart");
        }
        else if(cameraPermission && audioPermission && storagePermission){
            //The activity could be destroyed because of low memory. Keep a flag to quit the activity when you navigate back here.
            outState.putBoolean("quit",true);
            outState.putBoolean("restart",false);
            if(VERBOSE)Log.d(TAG, "Safe to quit");
        }
        outState.putBoolean("showPermission",showPermission);
        super.onSaveInstanceState(outState);
    }

    void quitFlipCam()
    {
        exitListener = new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface,int which)
            {
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        };
        alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(getString(R.string.title));
        alertDialog.setMessage(getString(R.string.message));
        alertDialog.setNeutralButton(R.string.exit, exitListener);
        alertDialog.setCancelable(false);
        alertDialog.show();
        showMessage = true;
    }
}
