package com.flipcam;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

public class PermissionActivity extends AppCompatActivity {

    final String TAG = "PermissionActivity";
    private final int ALL_PERMISSIONS = 0;
    static final String AUDIO_PERMISSION = "android.permission.RECORD_AUDIO";
    static final String CAMERA_PERMISSION = "android.permission.CAMERA";
    static final String STORAGE_PERMISSIONS = "android.permission.WRITE_EXTERNAL_STORAGE";
    static final String FC_SHARED_PREFERENCE = "FC_Settings";
    boolean cameraPermission = false;
    boolean audioPermission = false;
    boolean storagePermission = false;
    boolean showMessage = false;
    boolean showPermission = false;
    DialogInterface.OnClickListener exitListener;
    AlertDialog.Builder alertDialog;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ALL_PERMISSIONS) {
            if (permissions != null && permissions.length > 0) {
                Log.d(TAG, "For camera == "+permissions[0]);
                if (permissions[0].equalsIgnoreCase(CAMERA_PERMISSION) && permissions[1].equalsIgnoreCase(AUDIO_PERMISSION) &&
                        permissions[2].equalsIgnoreCase(STORAGE_PERMISSIONS)) {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        cameraPermission = true;
                        audioPermission = true;
                        storagePermission = true;
                        openCameraFragment();
                        //checkAudioPermission();
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
        Log.d(TAG,"onCreate");
        setContentView(R.layout.activity_permission);
        Log.d(TAG,"saved instance state == "+savedInstanceState);
        if(savedInstanceState!=null) {
            Log.d(TAG, "saved instance state restart == " + savedInstanceState.getBoolean("restart"));
            Log.d(TAG, "saved instance state quit == " + savedInstanceState.getBoolean("quit"));
        }
        SharedPreferences sharedPreferences = getSharedPreferences(FC_SHARED_PREFERENCE,Context.MODE_PRIVATE);
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

    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        Log.d(TAG,"onStart");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG,"onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG,"onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG,"onResume");
        super.onResume();
        SharedPreferences sharedPreferences = getSharedPreferences(FC_SHARED_PREFERENCE,Context.MODE_PRIVATE);
        if(sharedPreferences.getBoolean("startCamera",false)){
            Log.d(TAG,"Quit the app");
            finish();
        }
        else if(!showMessage){
            Log.d(TAG,"Check permissions and Start camera = "+showPermission);
            int camerapermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
            int audiopermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
            int storagepermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (camerapermission == PackageManager.PERMISSION_GRANTED && audiopermission == PackageManager.PERMISSION_GRANTED &&
                    storagepermission == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "ALL permissions obtained.");
                cameraPermission = true;
                audioPermission = true;
                storagePermission= true;
                openCameraFragment();
            } else if(!showPermission){
                Log.d(TAG, "Permissions not obtained. Obtain explicitly");
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
            SharedPreferences sharedPreferences = getSharedPreferences(FC_SHARED_PREFERENCE,Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("startCamera", true);
            editor.commit();
            Intent cameraIntent = new Intent(this, CameraActivity.class);
            startActivity(cameraIntent);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(TAG,"Restore state = "+savedInstanceState);
        if(savedInstanceState!=null && savedInstanceState.getBoolean("quit")) {
            //The activity was restarted because of possible low memory situation.
            Log.d(TAG, "Quit app");
            finish();
        }
        else if(savedInstanceState!= null && savedInstanceState.getBoolean("showPermission")){
            showPermission = savedInstanceState.getBoolean("showPermission");
            Log.d(TAG,"show permission = "+showPermission);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG,"Save before restart");
        if(showMessage) {
            outState.putBoolean("restart", true);
            outState.putBoolean("quit",false);
            Log.d(TAG, "Saved restart");
        }
        else if(cameraPermission && audioPermission && storagePermission){
            //The activity could be destroyed because of low memory. Keep a flag to quit the activity when you navigate back here.
            outState.putBoolean("quit",true);
            outState.putBoolean("restart",false);
            Log.d(TAG, "Safe to quit");
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
