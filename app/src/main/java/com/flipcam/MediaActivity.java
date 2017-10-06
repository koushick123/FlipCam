package com.flipcam;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

public class MediaActivity extends AppCompatActivity {

    private static final String TAG = "MediaActivity";
    RelativeLayout mediaPlaceholder;
    VideoView videoView=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        mediaPlaceholder = (RelativeLayout)findViewById(R.id.mediaPlaceholder);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Intent intent = getIntent();
        String path = intent.getStringExtra("mediaPath");
        if(path.endsWith(getResources().getString(R.string.IMG_EXT))) {
            Log.d(TAG,"show image");
            VideoView videoView = (VideoView)findViewById(R.id.recordedVideo);
            mediaPlaceholder.removeView(videoView);
            ImageView picture = new ImageView(this);
            Log.d(TAG, "image path = " + intent.getStringExtra("mediaPath"));
            Bitmap image = BitmapFactory.decodeFile(intent.getStringExtra("mediaPath"));
            picture.setImageBitmap(image);
            mediaPlaceholder.addView(picture);
        }
        else{
            Log.d(TAG,"show video");
            videoView = (VideoView)findViewById(R.id.recordedVideo);
            videoView.setVisibility(View.VISIBLE);
            videoView.setKeepScreenOn(true);
            videoView.setVideoPath("file://"+path);
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(path);
            String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            Log.d(TAG,"Video Width / Height = "+width+" / "+height);
            Log.d(TAG,"Aspect Ratio ==== "+Double.parseDouble(width)/Double.parseDouble(height));
            double videoAR = Double.parseDouble(width)/Double.parseDouble(height);
            WindowManager windowManager = (WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            Log.d(TAG,"Rotation = "+display.getRotation());
            Point screenSize=new Point();
            display.getRealSize(screenSize);
            double screenAR = (double)screenSize.x/(double)screenSize.y;
            Log.d(TAG,"screen width / height= "+screenSize.x+" / "+screenSize.y);
            Log.d(TAG,"Screen AR = "+screenAR);
            if(display.getRotation() == Surface.ROTATION_0){
                if(Math.abs(screenAR-videoAR) < 0.1) {
                    adjustVideoToFitScreen();
                }
            }
            else if(display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270){
                if(Math.abs(screenAR-videoAR) < 0.1) {
                    adjustVideoToFitScreen();
                }
            }
            MediaController mediaController = new MediaController(this);
            videoView.setMediaController(mediaController);
            mediaController.show();
            if(savedInstanceState == null) {
                videoView.start();
            }
        }
    }

    public void adjustVideoToFitScreen(){
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        videoView.setLayoutParams(layoutParams);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if(videoView!=null) {
            Log.d(TAG,"save duration");
            outState.putBoolean("playing",videoView.isPlaying());
            outState.putInt("position", videoView.getCurrentPosition());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG,"onRestoreInstanceState = "+savedInstanceState);
        if(savedInstanceState!=null && videoView!=null){
            videoView.seekTo(savedInstanceState.getInt("position"));
            if(savedInstanceState.getBoolean("playing")) {
                videoView.start();
            }
        }
    }
}
