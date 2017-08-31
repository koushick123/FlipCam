package com.flipcam;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import static com.flipcam.PermissionActivity.FC_SHARED_PREFERENCE;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        SharedPreferences sharedPreferences = getSharedPreferences(FC_SHARED_PREFERENCE, Context.MODE_PRIVATE);
        if(sharedPreferences.getBoolean("permission",false)){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("permission",false);
            editor.commit();
            Intent permission = new Intent(this,PermissionActivity.class);
            startActivity(permission);
        }
        else {
            getSupportFragmentManager().beginTransaction().add(R.id.cameraPreview, VideoFragment.newInstance()).commit();
        }
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
}
