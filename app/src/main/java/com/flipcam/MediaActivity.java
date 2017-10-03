package com.flipcam;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.VideoView;

public class MediaActivity extends AppCompatActivity {

    private static final String TAG = "MediaActivity";
    LinearLayout mediaPlaceholder;
    VideoView videoView=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        mediaPlaceholder = (LinearLayout)findViewById(R.id.mediaPlaceholder);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Intent intent = getIntent();
        String path = intent.getStringExtra("mediaPath");
        if(path.endsWith(getResources().getString(R.string.IMG_EXT))) {
            VideoView videoView = (VideoView)findViewById(R.id.recordedVideo);
            mediaPlaceholder.removeView(videoView);
            ImageView picture = new ImageView(this);
            LinearLayout.LayoutParams picParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            picParam.gravity = Gravity.CENTER;
            picture.setLayoutParams(picParam);
            Log.d(TAG, "image path = " + intent.getStringExtra("mediaPath"));
            Bitmap image = BitmapFactory.decodeFile(intent.getStringExtra("mediaPath"));
            picture.setImageBitmap(image);
            mediaPlaceholder.addView(picture);
        }
        else{
            videoView = (VideoView)findViewById(R.id.recordedVideo);
            videoView.setVisibility(View.VISIBLE);
            videoView.setKeepScreenOn(true);
            videoView.setVideoPath("file://"+path);
            MediaController mediaController = new MediaController(this);
            videoView.setMediaController(mediaController);
            mediaController.show();
            mediaController.setPrevNextListeners(null,null);
            if(savedInstanceState == null) {
                videoView.start();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if(videoView!=null && videoView.getCurrentPosition() > 0) {
            Log.d(TAG,"save duration");
            outState.putInt("duration", videoView.getCurrentPosition());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG,"onRestoreInstanceState = "+savedInstanceState);
        if(savedInstanceState!=null && videoView!=null){
            videoView.seekTo(savedInstanceState.getInt("duration"));
            videoView.start();
        }
    }
}
