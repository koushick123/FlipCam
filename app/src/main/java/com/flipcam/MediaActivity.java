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
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

public class MediaActivity extends AppCompatActivity {

    private static final String TAG = "MediaActivity";
    RelativeLayout mediaPlaceholder;
    VideoView videoView=null;
    boolean show=true;
    LinearLayout bottomBar;
    String path;
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
        path = intent.getStringExtra("mediaPath");
        if(isImage()) {
            Log.d(TAG,"show image");
            VideoView videoView = (VideoView)findViewById(R.id.recordedVideo);
            mediaPlaceholder.removeView(videoView);
            ImageView picture = new ImageView(this);
            picture.setId(picture.generateViewId());
            Log.d(TAG, "image path = " + intent.getStringExtra("mediaPath"));
            Bitmap image = BitmapFactory.decodeFile(intent.getStringExtra("mediaPath"));
            picture.setImageBitmap(image);
            mediaPlaceholder.addView(picture);
            LinearLayout parentPlaceholder = new LinearLayout(getApplicationContext());
            LinearLayout.LayoutParams parentParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            parentPlaceholder.setOrientation(LinearLayout.VERTICAL);
            parentPlaceholder.setGravity(Gravity.BOTTOM);
            parentPlaceholder.setLayoutParams(parentParams);
            bottomBar = new LinearLayout(getApplicationContext());
            LinearLayout.LayoutParams bottomParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bottomBar.setBackgroundColor(getResources().getColor(R.color.mediaControlColor));
            bottomBar.setOrientation(LinearLayout.HORIZONTAL);
            WindowManager windowManager = (WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            Log.d(TAG,"Rotation = "+display.getRotation());
            Point screenSize=new Point();
            display.getRealSize(screenSize);
            bottomParams.height=(int)(0.09 * screenSize.y);
            Log.d(TAG,"height = "+bottomParams.height);
            bottomBar.setLayoutParams(bottomParams);
            //DELETE Button
            ImageButton delete = new ImageButton(getApplicationContext());
            LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            delParams.weight=0.5f;
            if(display.getRotation() == Surface.ROTATION_0) {
                delParams.setMargins(0, 15, 0, 0);
            }
            else if(display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270){
                delParams.setMargins(0,10,0,10);
            }
            delete.setLayoutParams(delParams);
            delete.setImageDrawable(getResources().getDrawable(R.drawable.ic_delete_black_24dp));
            delete.setBackgroundColor(getResources().getColor(R.color.mediaControlColor));
            bottomBar.addView(delete);
            //SHARE Button
            ImageButton share = new ImageButton(getApplicationContext());
            LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            shareParams.weight=0.5f;
            if(display.getRotation() == Surface.ROTATION_0) {
                shareParams.setMargins(0, 15, 0, 0);
            }
            else if(display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270){
                shareParams.setMargins(0,10,0,10);
            }
            share.setLayoutParams(shareParams);
            share.setImageDrawable(getResources().getDrawable(R.drawable.ic_share_black_24dp));
            share.setBackgroundColor(getResources().getColor(R.color.mediaControlColor));
            bottomBar.addView(share);
            RelativeLayout.LayoutParams mediaParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mediaParams.addRule(RelativeLayout.BELOW,picture.getId());
            parentPlaceholder.addView(bottomBar);
            mediaPlaceholder.addView(parentPlaceholder);
        }
        else{
            Log.d(TAG,"show video");
            videoView = (VideoView)findViewById(R.id.recordedVideo);
            videoView.setVisibility(View.VISIBLE);
            videoView.setKeepScreenOn(true);
            videoView.setVideoPath("file://"+path);
            LinearLayout parentPlaceholder = new LinearLayout(getApplicationContext());
            LinearLayout.LayoutParams parentParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            parentParams.height=100;
            parentPlaceholder.setLayoutParams(parentParams);
            parentPlaceholder.setOrientation(LinearLayout.VERTICAL);
            parentPlaceholder.setGravity(Gravity.BOTTOM);
            LinearLayout bottomBar = new LinearLayout(getApplicationContext());
            parentParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bottomBar.setLayoutParams(parentParams);
            bottomBar.setOrientation(LinearLayout.HORIZONTAL);
            bottomBar.setBackgroundColor(getResources().getColor(R.color.mediaControlColor));
            WindowManager windowManager = (WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            Point screenSize=new Point();
            display.getRealSize(screenSize);
            ImageButton delete = new ImageButton(getApplicationContext());
            LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            delParams.weight=0.5f;
            if(display.getRotation() == Surface.ROTATION_0) {
                delParams.setMargins(0, 15, 0, 0);
            }
            else if(display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270){
                delParams.setMargins(0,10,0,10);
            }
            delete.setLayoutParams(delParams);
            delete.setImageDrawable(getResources().getDrawable(R.drawable.ic_delete_black_24dp));
            delete.setBackgroundColor(getResources().getColor(R.color.mediaControlColor));
            bottomBar.addView(delete);
            parentPlaceholder.addView(bottomBar);
            mediaPlaceholder.addView(parentPlaceholder);
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(path);
            String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            Log.d(TAG,"Video Width / Height = "+width+" / "+height);
            Log.d(TAG,"Aspect Ratio ==== "+Double.parseDouble(width)/Double.parseDouble(height));
            double videoAR = Double.parseDouble(width)/Double.parseDouble(height);
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

    public boolean isImage()
    {
        return path.endsWith(getResources().getString(R.string.IMG_EXT));
    }

    public void showMediaControls(View view)
    {
        if(isImage()) {
            if (show) {
                show = false;
                bottomBar.setVisibility(View.INVISIBLE);
            } else {
                show = true;
                bottomBar.setVisibility(View.VISIBLE);
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
