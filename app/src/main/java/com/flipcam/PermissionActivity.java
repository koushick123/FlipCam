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
    private int MY_PERMISSIONS_REQUEST_CAMERA = 0;
    private int MY_PERMISSIONS_REQUEST_AUDIO = 1;
    private int MY_PERMISSIONS_WRITE_STORAGE = 2;
    static final String AUDIO_PERMISSION = "android.permission.RECORD_AUDIO";
    static final String CAMERA_PERMISSION = "android.permission.CAMERA";
    static final String STORAGE_PERMISSIONS = "android.permission.WRITE_EXTERNAL_STORAGE";
    boolean cameraPermission = false;
    boolean audioPermission = false;
    boolean storagePermission = false;
    boolean showMessage = false;
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
                        checkExternalStoragePermission();
                        //openCameraFragment();
                    } else {
                        quitFlipCam();
                    }
                }
            } else {
                super.onRequestPermissionsResult(requestCode,permissions,grantResults);
            }
        } else if (requestCode == MY_PERMISSIONS_WRITE_STORAGE) {
            if (permissions != null && permissions.length > 0) {
                Log.d(TAG, "For audio == "+permissions[0]);
                if (permissions[0].equalsIgnoreCase(STORAGE_PERMISSIONS)) {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
        Log.d(TAG,"onCreate");
        setContentView(R.layout.activity_permission);
        Log.d(TAG,"saved instance state == "+savedInstanceState);
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
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
        super.onDestroy();
        Log.d(TAG,"onDestroy");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"onStop");
    }

    @Override
    protected void onResume() {
        Log.d(TAG,"onResume");
        super.onResume();
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        if(sharedPreferences.getBoolean("startCamera",false)){
            Log.d(TAG,"Quit the app");
            finish();
        }
        else if(!showMessage){
            Log.d(TAG,"Check permissions and Start camera");
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
            SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("startCamera", true);
            editor.commit();
            Intent cameraIntent = new Intent(this, CameraActivity.class);
            startActivity(cameraIntent);
        }
    }

    void checkAudioPermission()
    {
        int permission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            audioPermission = true;
            //openCameraFragment();
        } else {
            Log.d(TAG, "Audio permission not obtained. Obtain explicitly");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_AUDIO);
        }
        if(audioPermission){
            checkExternalStoragePermission();
        }
    }

    void checkExternalStoragePermission()
    {
        int permission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            storagePermission = true;
        } else {
            Log.d(TAG, "Storage permission not obtained. Obtain explicitly");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_WRITE_STORAGE);
        }
        if(storagePermission){
            openCameraFragment();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if(showMessage) {
            outState.putBoolean("restart", true);
            Log.d(TAG, "Saved restart");
        }
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
