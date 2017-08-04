package com.flipcam;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

public class PermissionActivity extends AppCompatActivity {

    final String TAG = "PermissionActivity";
    private int MY_PERMISSIONS_REQUEST_CAMERA = 0;
    private int MY_PERMISSIONS_REQUEST_AUDIO = 1;
    static final String AUDIO_PERMISSION = "android.permission.RECORD_AUDIO";
    static final String CAMERA_PERMISSION = "android.permission.CAMERA";
    boolean cameraPermission = false;
    boolean audioPermission = false;
    DialogInterface.OnClickListener exitListener;
    AlertDialog.Builder alertDialog;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            if (permissions != null && permissions.length > 0) {
                Log.d(TAG, "For camera == "+permissions[0]);
                if (permissions[0].equalsIgnoreCase(CAMERA_PERMISSION)) {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        cameraPermission = true;
                        checkAudioPermission();
                    } else {
                        quitFlipCam();
                    }
                }
            } else {
                super.onRequestPermissionsResult(requestCode,permissions,grantResults);
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_AUDIO) {
            if (permissions != null && permissions.length > 0) {
                Log.d(TAG, "For audio == "+permissions[0]);
                if (permissions[0].equalsIgnoreCase(AUDIO_PERMISSION)) {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        audioPermission = true;
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
        setContentView(R.layout.activity_permission);
        Log.d(TAG,"saved instance state == "+savedInstanceState);
        if(savedInstanceState!=null){
            if(savedInstanceState.getBoolean("restart")){
                quitFlipCam();
            }
        }
        else {
            int permission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
            if (permission == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission obtained.");
                cameraPermission = true;
            } else {
                Log.d(TAG, "Camera permission not obtained. Obtain explicitly");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);
            }

            if (cameraPermission) {
                checkAudioPermission();
            }
        }
    }

    void openCameraFragment()
    {
        if(cameraPermission && audioPermission) {
            //Open VideoFragment under CameraActivity showing camera preview.
            Toast.makeText(getApplicationContext(), "ALL Permissions in place. Need to open camera now.", Toast.LENGTH_SHORT).show();
        }
    }

    void checkAudioPermission()
    {
        int permission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            audioPermission = true;
            openCameraFragment();
        } else {
            Log.d(TAG, "Audio permission not obtained. Obtain explicitly");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_AUDIO);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("restart",true);
        Log.d(TAG,"Saved restart");
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
        alertDialog.setTitle("");
        alertDialog.setMessage("");
        alertDialog.setNeutralButton("EXIT", exitListener);
        alertDialog.setCancelable(false);
        alertDialog.show();
    }
}
