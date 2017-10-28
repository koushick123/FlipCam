package com.flipcam;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import java.lang.ref.WeakReference;
import java.util.StringTokenizer;

import static com.flipcam.PermissionActivity.FC_MEDIA_PREFERENCE;
import static com.flipcam.constants.Constants.IMAGE_CONTROLS_HIDE;
import static com.flipcam.constants.Constants.MEDIA_ACTUAL_DURATION;
import static com.flipcam.constants.Constants.MEDIA_COMPLETED;
import static com.flipcam.constants.Constants.MEDIA_CONTROLS_HIDE;
import static com.flipcam.constants.Constants.MEDIA_CURRENT_TIME;
import static com.flipcam.constants.Constants.MEDIA_PLAYING;
import static com.flipcam.constants.Constants.MEDIA_POSITION;
import static com.flipcam.constants.Constants.MEDIA_PREVIOUS_POSITION;
import static com.flipcam.constants.Constants.SEEK_DURATION;
import static com.flipcam.constants.Constants.VIDEO_SEEK_UPDATE;

public class MediaActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener{

    private static final String TAG = "MediaActivity";
    RelativeLayout mediaPlaceholder;
    VideoView videoView=null;
    boolean hide=true;
    boolean play=true;
    LinearLayout bottomBar;
    MediaController mediaController;
    LinearLayout mediaBar;
    LinearLayout topBar;
    String path;
    ImageView pause;
    volatile boolean startTracker=false;
    MainHandler mediaHandler;
    SeekBar videoSeek;
    boolean isCompleted=false;
    String duration;
    TextView startTime;
    TextView endTime;
    volatile boolean startTimer=false;
    volatile boolean updateTimer=false;
    volatile int seconds = 0;
    volatile int minutes = 0;
    volatile int hours = 0;
    int previousPos = 0;

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"onStop");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();
    }

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
        Log.d(TAG,"path = "+path);
        WindowManager windowManager = (WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Log.d(TAG,"Rotation = "+display.getRotation());
        Point screenSize=new Point();
        display.getRealSize(screenSize);
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
            videoView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    showMediaControls(view);
                }
            });
            mediaBar = new LinearLayout(this);
            topBar = new LinearLayout(this);
            //Add Top Bar
            LinearLayout parentPlaceholder = new LinearLayout(this);
            LinearLayout.LayoutParams parentParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            parentPlaceholder.setOrientation(LinearLayout.VERTICAL);
            parentPlaceholder.setGravity(Gravity.TOP);
            parentPlaceholder.setLayoutParams(parentParams);

            parentParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            parentParams.height=(int)(0.09 * screenSize.y);
            topBar.setLayoutParams(parentParams);
            topBar.setOrientation(LinearLayout.HORIZONTAL);
            topBar.setBackgroundColor(getResources().getColor(R.color.mediaControlColor));
            display.getRealSize(screenSize);
            //DELETE Button
            ImageButton delete = new ImageButton(this);
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
            topBar.addView(delete);
            //SHARE Button
            ImageButton share = new ImageButton(this);
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
            topBar.addView(share);
            parentPlaceholder.addView(topBar);
            mediaPlaceholder.addView(parentPlaceholder);

            //Add Media controls bar
            parentPlaceholder = new LinearLayout(this);
            parentParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            parentPlaceholder.setOrientation(LinearLayout.VERTICAL);
            parentPlaceholder.setGravity(Gravity.BOTTOM);
            parentPlaceholder.setLayoutParams(parentParams);

            LinearLayout.LayoutParams mediaParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if(display.getRotation() == Surface.ROTATION_0) {
                mediaParams.height = (int) (0.09 * screenSize.y);
            }
            else if(display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270){
                mediaParams.height = (int) (0.15 * screenSize.y);
            }
            mediaBar.setOrientation(LinearLayout.HORIZONTAL);
            mediaBar.setGravity(Gravity.CENTER);
            mediaBar.setBackgroundColor(getResources().getColor(R.color.mediaControlColor));
            mediaBar.setLayoutParams(mediaParams);
            //PAUSE button
            pause = new ImageView(this);
            pause.setBackgroundColor(getResources().getColor(R.color.mediaControlColor));
            pause.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ViewGroup.LayoutParams pauseParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if(display.getRotation() == Surface.ROTATION_0) {
                pauseParams.height=90;
                pause.setPadding(0,0,0,0);
            }
            else if(display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270){
                pauseParams.height=100;
                pause.setPadding(0,10,0,10);
            }
            pause.setLayoutParams(pauseParams);
            pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_black_24dp));
            pause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!play) {
                        if(isCompleted){
                            isCompleted = false;
                        }
                        videoView.start();
                        pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_black_24dp));
                        play=true;
                        updateTimer=true;
                    }
                    else{
                        videoView.pause();
                        pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));
                        play=false;
                        updateTimer=false;
                    }
                }
            });
            mediaBar.addView(pause);
            videoSeek = new SeekBar(this);
            videoSeek.setThumb(getDrawable(R.drawable.whitecircle));
            videoSeek.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.seekFill)));
            videoSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            startTime = new TextView(this);
            startTime.setGravity(Gravity.LEFT);
            startTime.setTextColor(getResources().getColor(R.color.time));
            startTime.setText(getResources().getString(R.string.START_TIME));
            parentPlaceholder.addView(startTime);
            endTime = new TextView(this);
            endTime.setGravity(Gravity.RIGHT);
            endTime.setTextColor(getResources().getColor(R.color.time));
            parentPlaceholder.addView(endTime);
            parentPlaceholder.addView(videoSeek);
            parentPlaceholder.addView(mediaBar);
            mediaPlaceholder.addView(parentPlaceholder);
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(path);
            String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            calculateAndDisplayEndTime();
            videoSeek.setMax(Integer.parseInt(duration));
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
            mediaController = new MediaController(this);
            if(savedInstanceState == null) {
                SharedPreferences mediaValues = getSharedPreferences(FC_MEDIA_PREFERENCE,Context.MODE_PRIVATE);
                SharedPreferences.Editor mediaState = null;
                if(mediaValues!=null){
                    mediaState = mediaValues.edit();
                    if(mediaState!=null){
                        mediaState.clear();
                        mediaState.commit();
                        Log.d(TAG,"CLEAR ALL");
                    }
                }
                videoView.start();
                Log.d(TAG,"Video STARTED");
            }
        }
    }

    public void startTrackerThread()
    {
        mediaHandler = new MainHandler(this);
        startTracker = true;
        VideoTracker videoTracker = new VideoTracker();
        videoTracker.start();
    }

    public void calculateAndDisplayEndTime()
    {
        int videoLength = Integer.parseInt(duration);
        int secs = (videoLength / 1000);
        Log.d(TAG,"total no of secs = "+secs);
        int hour = 0;
        int mins = 0;
        if(secs > 60){
            mins = secs / 60;
            if(mins > 60){
                hour = mins / 60;
                mins = mins % 60;
            }
            secs = secs % 60;
        }
        String showSec = "0";
        String showMin = "0";
        String showHr = "0";
        if(secs < 10){
            showSec += secs;
        }
        else{
            showSec = secs+"";
        }

        if(mins < 10){
            showMin += mins;
        }
        else{
            showMin = mins+"";
        }

        if(hour < 10){
            showHr += hour;
        }
        else{
            showHr = hour+"";
        }
        endTime.setText(showHr+" : "+showMin+" : "+showSec);
    }

    public void stopTimerThread()
    {
        startTimer = false;
    }

    public void stopTrackerThread()
    {
        startTracker = false;
    }

    public boolean isImage()
    {
        if(path.endsWith(getResources().getString(R.string.IMG_EXT)) || path.endsWith(getResources().getString(R.string.ANOTHER_IMG_EXT))){
            return true;
        }
        return false;
    }

    public void showMediaControls(View view)
    {
        if(isImage()) {
            Log.d(TAG,"hide = "+hide);
            if (hide) {
                hide = false;
                bottomBar.setVisibility(View.GONE);
            } else {
                hide = true;
                bottomBar.setVisibility(View.VISIBLE);
            }
        }
        else{
            Log.d(TAG,"hide = "+hide);
            if (hide) {
                hide = false;
                videoSeek.setVisibility(View.GONE);
                topBar.setVisibility(View.GONE);
                mediaBar.setVisibility(View.GONE);
                startTime.setVisibility(View.GONE);
                endTime.setVisibility(View.GONE);
            } else {
                hide = true;
                videoSeek.setVisibility(View.VISIBLE);
                topBar.setVisibility(View.VISIBLE);
                mediaBar.setVisibility(View.VISIBLE);
                startTime.setVisibility(View.VISIBLE);
                endTime.setVisibility(View.VISIBLE);
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
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        if(isImage()){
            SharedPreferences mediaState = getSharedPreferences(FC_MEDIA_PREFERENCE, Context.MODE_PRIVATE);
            if(mediaState!=null && mediaState.contains(IMAGE_CONTROLS_HIDE)){
                if(mediaState.getBoolean(IMAGE_CONTROLS_HIDE,true)){
                    Log.d(TAG,"Visible");
                    bottomBar.setVisibility(View.VISIBLE);
                }
                else{
                    Log.d(TAG,"GONE");
                    bottomBar.setVisibility(View.GONE);
                }
                hide = mediaState.getBoolean(IMAGE_CONTROLS_HIDE,true);
            }
        }
        else {
            videoView.setOnCompletionListener(this);
            previousPos = 0;
            startTrackerThread();
            SharedPreferences mediaState = getSharedPreferences(FC_MEDIA_PREFERENCE, Context.MODE_PRIVATE);
            Log.d(TAG,"media state = "+mediaState);
            if (mediaState != null) {
                //Get SAVED MEDIA DURATION(For SeekBar)
                if(mediaState.contains(SEEK_DURATION)) {
                    Log.d(TAG, "Retrieve media duration = " + mediaState.getInt(SEEK_DURATION, 0));
                    videoSeek.setMax(mediaState.getInt(SEEK_DURATION, 0));
                }
                //Get MEDIA COMPLETED STATE
                if(mediaState.contains(MEDIA_COMPLETED)){
                    Log.d(TAG,"Retrieve media completed == "+mediaState.getBoolean(MEDIA_COMPLETED,false));
                    if(mediaState.getBoolean(MEDIA_COMPLETED,false)){
                        videoView.seekTo(1000);
                        videoSeek.setProgress(0);
                    }
                    else {
                        //Get SAVED MEDIA POSITION
                        if (mediaState.contains(MEDIA_POSITION)) {
                            Log.d(TAG, "Retrieve media position = " + mediaState.getInt(MEDIA_POSITION, 0));
                            if (mediaState.getInt(MEDIA_POSITION, 0) < videoView.getDuration()) {
                                videoView.seekTo(mediaState.getInt(MEDIA_POSITION, 0));
                            } else {
                                videoView.seekTo(mediaState.getInt(MEDIA_POSITION, 0));
                            }
                            videoSeek.setProgress(mediaState.getInt(MEDIA_POSITION, 0));
                        }
                    }
                    isCompleted = mediaState.getBoolean(MEDIA_COMPLETED,false);
                }
                //Get SAVED MEDIA STATE
                if(mediaState.contains(MEDIA_PLAYING)) {
                    if (mediaState.getBoolean(MEDIA_PLAYING, true)) {
                        Log.d(TAG, "Retrieve media controls playing = " + mediaState.getBoolean(MEDIA_PLAYING, true));
                        videoView.start();
                        pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_black_24dp));
                        play = true;
                    } else {
                        videoView.pause();
                        pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));
                        play = false;
                    }
                }
                //Get SAVED MEDIA CONTROLS VIEW STATE
                if(mediaState.contains(MEDIA_CONTROLS_HIDE)){
                    Log.d(TAG, "Retrieve media controls hide = " + mediaState.getBoolean(MEDIA_CONTROLS_HIDE, true));
                    if (mediaState.getBoolean(MEDIA_CONTROLS_HIDE, true)) {
                        topBar.setVisibility(View.VISIBLE);
                        mediaBar.setVisibility(View.VISIBLE);
                    } else {
                        topBar.setVisibility(View.GONE);
                        mediaBar.setVisibility(View.GONE);
                    }
                    hide = mediaState.getBoolean(MEDIA_CONTROLS_HIDE, true);
                }
                //Get SAVED PREVIOUS TIME
                if(mediaState.contains(MEDIA_PREVIOUS_POSITION)){
                    Log.d(TAG,"Retrieve media previous time = "+mediaState.getInt(MEDIA_PREVIOUS_POSITION,0));
                    previousPos = mediaState.getInt(MEDIA_PREVIOUS_POSITION,0);
                }
                //Get CURRENT TIME
                if(mediaState.contains(MEDIA_CURRENT_TIME)){
                    Log.d(TAG,"Retrieve current time = "+mediaState.getString(MEDIA_CURRENT_TIME,getResources().getString(R.string.START_TIME)));
                    String currentTime = mediaState.getString(MEDIA_CURRENT_TIME,getResources().getString(R.string.START_TIME));
                    StringTokenizer timeToken = new StringTokenizer(currentTime,":");
                    seconds = Integer.parseInt(timeToken.nextToken().trim());
                    minutes = Integer.parseInt(timeToken.nextToken().trim());
                    hours = Integer.parseInt(timeToken.nextToken().trim());
                    showTimeElapsed();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
        if(isImage()){
            SharedPreferences.Editor mediaState = getSharedPreferences(FC_MEDIA_PREFERENCE, Context.MODE_PRIVATE).edit();
            Log.d(TAG,"saving hide = "+hide);
            mediaState.putBoolean(IMAGE_CONTROLS_HIDE,hide);
            mediaState.commit();
        }
        else {
            if (videoView != null) {
                Log.d(TAG, "Save media state");
                stopTrackerThread();
                SharedPreferences.Editor mediaState = getSharedPreferences(FC_MEDIA_PREFERENCE, Context.MODE_PRIVATE).edit();
                mediaState.putBoolean(MEDIA_PLAYING, videoView.isPlaying());
                mediaState.putInt(MEDIA_POSITION, videoView.getCurrentPosition());
                mediaState.putBoolean(MEDIA_CONTROLS_HIDE, hide);
                mediaState.putInt(SEEK_DURATION,videoSeek.getMax());
                mediaState.putLong(MEDIA_ACTUAL_DURATION,Long.parseLong(duration));
                mediaState.putBoolean(MEDIA_COMPLETED,isCompleted);
                mediaState.putInt(MEDIA_PREVIOUS_POSITION,previousPos);
                mediaState.putString(MEDIA_CURRENT_TIME,seconds+":"+minutes+":"+hours);
                mediaState.commit();
                if (videoView.isPlaying()) {
                    videoView.pause();
                }
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG,"Video Completed");
        pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));
        play = false;
        isCompleted = true;
        videoView.seekTo(0);
        videoSeek.setProgress(0);
        updateTimer=false;
        seconds=0; minutes=0; hours=0;
        showTimeElapsed();
    }

    class MainHandler extends Handler {
        WeakReference<MediaActivity> mediaActivity;
        MediaActivity mediaAct;

        public MainHandler(MediaActivity mediaActivity1) {
            mediaActivity = new WeakReference<>(mediaActivity1);
        }

        @Override
        public void handleMessage(Message msg) {
            mediaAct = mediaActivity.get();
            switch(msg.what)
            {
                case VIDEO_SEEK_UPDATE:
                    showTimeElapsed();
                    break;
            }
        }
    }

    public void showTimeElapsed()
    {
        String showSec = "0";
        String showMin = "0";
        String showHr = "0";
        if(seconds < 10){
            showSec += seconds;
        }
        else{
            showSec = seconds+"";
        }

        if(minutes < 10){
            showMin += minutes;
        }
        else{
            showMin = minutes+"";
        }

        if(hours < 10){
            showHr += hours;
        }
        else{
            showHr = hours+"";
        }
        startTime.setText(showHr + " : " + showMin + " : " + showSec);
    }

    class VideoTracker extends Thread
    {
        @Override
        public void run() {
            Log.d(TAG,"Video Tracker STARTED...");
            while(startTracker){
                while(videoView.isPlaying()){
                    int latestPos = videoView.getCurrentPosition();
                    videoSeek.setProgress(latestPos);
                    if(latestPos > 999 && latestPos % 1000 >= 0){
                        //showTimeElapsed();
                        if(previousPos == 0) {
                            previousPos = latestPos;
                            if(seconds < 59){
                                seconds++;
                            }
                            else if(minutes < 59){
                                minutes++;
                                seconds = 0;
                            }
                            else{
                                minutes = 0;
                                seconds = 0;
                                hours++;
                            }
                            //Log.d(TAG,"seconds 1111 == "+seconds);
                            mediaHandler.sendEmptyMessage(VIDEO_SEEK_UPDATE);
                        }
                        else{
                            if(Math.abs(previousPos-latestPos) >= 1000){
                                previousPos = latestPos;
                                if(seconds < 59){
                                    seconds++;
                                }
                                else if(minutes < 59){
                                    minutes++;
                                    seconds = 0;
                                }
                                else{
                                    minutes = 0;
                                    seconds = 0;
                                    hours++;
                                }
                                //Log.d(TAG,"seconds == "+seconds);
                                mediaHandler.sendEmptyMessage(VIDEO_SEEK_UPDATE);
                            }
                        }
                    }
                    if(!startTracker){
                        break;
                    }
                }
            }
            Log.d(TAG,"Video Tracker thread EXITING...");
        }
    }
}
