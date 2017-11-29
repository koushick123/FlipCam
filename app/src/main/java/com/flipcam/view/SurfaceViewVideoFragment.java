package com.flipcam.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.flipcam.ControlVisbilityPreference;
import com.flipcam.GlideApp;
import com.flipcam.R;
import com.flipcam.media.FileMedia;
import com.flipcam.media.Media;
import com.flipcam.util.MediaUtil;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;

import static com.flipcam.constants.Constants.VIDEO_SEEK_UPDATE;

/**
 * Created by Koushick on 28-11-2017.
 */

public class SurfaceViewVideoFragment extends Fragment implements SurfaceHolder.Callback, MediaPlayer.OnCompletionListener,MediaPlayer.OnPreparedListener,
MediaPlayer.OnErrorListener, Serializable{

    static final String TAG = "SurfaceViewVideoFragmnt";
    transient public MediaPlayer mediaPlayer = null;
    transient RelativeLayout mediaPlaceholder;
    transient SurfaceView videoView=null;
    public boolean play=false;
    transient LinearLayout topBar;
    public String path;
    transient ImageButton pause;
    volatile boolean startTracker=false;
    transient SurfaceViewVideoFragment.MainHandler mediaHandler;
    transient SeekBar videoSeek;
    volatile boolean isCompleted=false;
    String duration;
    transient TextView startTime;
    transient TextView endTime;
    public boolean playInProgress=false;
    volatile int seconds = 0;
    volatile int minutes = 0;
    volatile int hours = 0;
    public int previousPos = 0;
    transient LinearLayout videoControls;
    int framePosition;
    transient ImageView picture;
    transient FileMedia[] images=null;
    transient ImageView preview;
    ControlVisbilityPreference controlVisbilityPreference;
    transient Display display;
    transient SurfaceViewVideoFragment.VideoTracker videoTracker;

    public static SurfaceViewVideoFragment newInstance(int pos){
        SurfaceViewVideoFragment surfaceViewVideoFragment = new SurfaceViewVideoFragment();
        Bundle args = new Bundle();
        args.putInt("position", pos);
        surfaceViewVideoFragment.setArguments(args);
        return surfaceViewVideoFragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG,"onActivityCreated = "+path);
        topBar = (LinearLayout)getActivity().findViewById(R.id.topMediaControls);
        videoControls = (LinearLayout)getActivity().findViewById(R.id.videoControls);
        pause = (ImageButton) getActivity().findViewById(R.id.playButton);
        startTime = (TextView)getActivity().findViewById(R.id.startTime);
        endTime = (TextView)getActivity().findViewById(R.id.endTime);
        videoSeek = (SeekBar)getActivity().findViewById(R.id.videoSeek);
        controlVisbilityPreference = (ControlVisbilityPreference) getActivity().getApplicationContext();
        if(getUserVisibleHint()) {
            if(!isImage()) {
                Media newVideo;
                if (savedInstanceState != null) {
                    newVideo = savedInstanceState.getParcelable("currentVideo");
                    if(newVideo != null){
                        //Since we will re-construct the saved video using 'currentVideo' Parcelable, no need for this.
                        //This Bundle is created to maintain the saved video state when the user minimizes the app or opens task manager directly.
                        //We will use this in onResume() if it's not null.
                        getActivity().getIntent().removeExtra("saveVideoForMinimize");
                    }
                }
                else{
                    Log.d(TAG,"setup video");
                    newVideo = new Media();
                    newVideo.setMediaActualDuration(duration);
                    newVideo.setMediaCompleted(false);
                    newVideo.setMediaControlsHide(true);
                    newVideo.setMediaPlaying(false);
                    newVideo.setMediaPosition(0);
                    newVideo.setMediaPreviousPos(0);
                    newVideo.setSeekDuration(Integer.parseInt(duration));
                }
                reConstructVideo(newVideo);
                showTimeElapsed();
                calculateAndDisplayEndTime();
            }
        }
        if(!isImage()) {
            if (savedInstanceState != null) {
                if(getUserVisibleHint()) {
                    playInProgress = savedInstanceState.getBoolean("videoPlayed");
                    if (!playInProgress) {
                        Log.d(TAG, "Video NOT played");
                        mediaPlayer.seekTo(100);
                        /*frameBm = savedInstanceState.getParcelable("firstFrame");
                        //preview.setImageBitmap(frameBm);
                        GlideApp.with(getContext()).load(frameBm).into(preview);
                        showFirstFrame();*/
                    } else {
                        Log.d(TAG, "Video played");
                    }
                }
                else{
                    Log.d(TAG,"CREATE Preview with savedinstance");
                    /*frameBm = savedInstanceState.getParcelable("firstFrame");
                    GlideApp.with(getContext()).load(frameBm).into(preview);
                    //preview.setImageBitmap(frameBm);
                    //removeFirstFrame();
                    showFirstFrame();*/

                }
            }
            else{
                Log.d(TAG,"CREATE Preview");
                /*MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(path);
                frameBm = mediaMetadataRetriever.getFrameAtTime(FIRST_SEC_MILLI);
                preview.setImageBitmap(frameBm);*/
                //showFirstFrame();
                //videoView.seekTo(100);
            }
        }
    }

    public void reConstructVideo(Media savedVideo){
        videoSeek.setMax(savedVideo.getSeekDuration());
        videoSeek.setThumb(getResources().getDrawable(R.drawable.whitecircle));
        videoSeek.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.seekFill)));
        Log.d(TAG, "Retrieve media completed == " + savedVideo.isMediaCompleted());
        if (savedVideo.isMediaCompleted()) {
            //mediaPlayer.seekTo(100);
            videoSeek.setProgress(0);
            isCompleted = true;
        } else {
            Log.d(TAG,"Set SEEK to = "+savedVideo.getMediaPosition());
            mediaPlayer.seekTo(savedVideo.getMediaPosition());
            videoSeek.setProgress(savedVideo.getMediaPosition());
        }
        Log.d(TAG, "Retrieve media playing = " + savedVideo.isMediaPlaying());
        if (savedVideo.isMediaPlaying()) {
            pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white_24dp));
            play = true;
        } else {
            //mediaPlayer.pause();
            pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
            play = false;
        }
        LinearLayout.LayoutParams pauseParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (display.getRotation() == Surface.ROTATION_0) {
            pauseParams.height = 100;
            pause.setPadding(0, 0, 0, 0);
        } else if (display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270) {
            pauseParams.height = 90;
            pause.setPadding(0, 10, 0, 10);
        }
        pause.setLayoutParams(pauseParams);
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!play) {
                    if (isCompleted) {
                        isCompleted = false;
                    }
                    Log.d(TAG, "Set PLAY post rotate");
                    //removeFirstFrame();
                    mediaPlayer.start();
                    playInProgress = true;
                    pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white_24dp));
                    play = true;
                } else {
                    Log.d(TAG, "Set PAUSE post rotate");
                    mediaPlayer.pause();
                    pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
                    play = false;
                }
            }
        });
        //Get SAVED MEDIA CONTROLS VIEW STATE
        Log.d(TAG, "Retrieve media controls hide = " + savedVideo.isMediaControlsHide());
        if (savedVideo.isMediaControlsHide()) {
            showAllControls();
        } else {
            hideAllControls();
        }
        controlVisbilityPreference.setHideControl(savedVideo.isMediaControlsHide());

        //Get MEDIA DURATION
        Log.d(TAG, "Retrieve media duration = " + savedVideo.getMediaActualDuration());
        duration = savedVideo.getMediaActualDuration();

        //Get SAVED PREVIOUS TIME
        Log.d(TAG, "Retrieve media previous time = " + savedVideo.getMediaPreviousPos());
        previousPos = savedVideo.getMediaPreviousPos();

        //Get CURRENT TIME
        //Log.d(TAG, "Retrieve current time = " + savedInstanceState.getString(MEDIA_CURRENT_TIME));
        Log.d(TAG, "Retrieve current time = " +savedVideo.getMediaPosition() / 1000);
        if(!isCompleted) {
            seconds = savedVideo.getMediaPosition() / 1000;
            if(seconds < 60){
                minutes = 0;
                hours = 0;
            }
            else{
                minutes = seconds / 60;
                if(minutes < 60) {
                    hours = 0;
                }
                else{
                    hours = minutes / 60;
                    minutes = minutes % 60;
                }
                seconds = seconds % 60;
            }
        }
        else{
            seconds = 0;
            minutes = 0;
            hours = 0;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        framePosition = getArguments().getInt("position");
        //Log.d(TAG,"framePosition = "+framePosition);
        images = MediaUtil.getMediaList(getContext());
        path = images[framePosition].getPath();
        Log.d(TAG,"media is == "+path);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_surfaceview_video, container, false);
        videoView = (SurfaceView) view.findViewById(R.id.recordedVideo);
        mediaPlaceholder = (RelativeLayout)view.findViewById(R.id.mediaPlaceholder);
        mediaPlaceholder.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                showMediaControls();
            }
        });
        WindowManager windowManager = (WindowManager)getActivity().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
        //Log.d(TAG,"Rotation = "+display.getRotation());
        Log.d(TAG,"onCreateView = "+path);
        if(isImage()) {
            Log.d(TAG,"show image");
            videoView.setVisibility(View.GONE);
            picture = new ImageView(getActivity());
            picture.setId(picture.generateViewId());
            Uri uri = Uri.fromFile(new File(path));
            GlideApp.with(getContext()).load(uri).into(picture);
            mediaPlaceholder.addView(picture);
        }
        else {
            Log.d(TAG,"show video");
            mediaPlayer = new MediaPlayer();
            videoView.setKeepScreenOn(true);
            videoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showMediaControls();
                }
            });
            fitVideoToScreen();
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnErrorListener(this);
            SurfaceHolder holder = videoView.getHolder();
            holder.addCallback(this);
        }
        return view;
    }

    public void fitVideoToScreen(){
        Point screenSize=new Point();
        display.getRealSize(screenSize);
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(path);
        String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        Log.d(TAG,"Video Width / Height = "+width+" / "+height);
        Log.d(TAG,"Aspect Ratio ==== "+Double.parseDouble(width)/Double.parseDouble(height));
        double videoAR = Double.parseDouble(width) / Double.parseDouble(height);
        double screenAR = (double) screenSize.x / (double) screenSize.y;
        Log.d(TAG,"Screen AR = "+screenAR);
        if (display.getRotation() == Surface.ROTATION_0) {
            if (Math.abs(screenAR - videoAR) < 0.1) {
                ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
                layoutParams.width = screenSize.x;
                layoutParams.height = screenSize.y;
                videoView.setLayoutParams(layoutParams);
            }
            else{
                ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
                layoutParams.width = screenSize.x;
                layoutParams.height = (int)(screenSize.x / videoAR);
                videoView.setLayoutParams(layoutParams);
            }
        } else if (display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270) {
            if (Math.abs(screenAR - videoAR) < 0.1) {
                ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
                layoutParams.width = screenSize.x;
                layoutParams.height = screenSize.y;
                videoView.setLayoutParams(layoutParams);
            }
            else{
                ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
                layoutParams.width = (int)(videoAR * screenSize.y);
                layoutParams.height = screenSize.y;
                videoView.setLayoutParams(layoutParams);
            }
        }
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG,"onSaveInstanceState = "+path+" , "+playInProgress);
        Log.d(TAG,"getUserVisibleHint ? ="+getUserVisibleHint());
        if(!isImage()) {
            //outState.putParcelable("firstFrame",frameBm);
            outState.putBoolean("videoPlayed", playInProgress);
            //removeFirstFrame();
            if (getUserVisibleHint()) {
                Media media = new Media();
                //media.setMediaPlaying(videoView.isPlaying());
                media.setMediaPlaying(isMediaPlayingForMinmize);
                //media.setMediaPosition(videoView.getCurrentPosition());
                media.setMediaPosition(mediaPositionForMinimize);
                media.setMediaControlsHide(controlVisbilityPreference.isHideControl());
                media.setMediaActualDuration(duration);
                media.setSeekDuration(videoSeek.getMax());
                media.setMediaCompleted(isCompleted);
                media.setMediaPreviousPos(previousPos);
                outState.putParcelable("currentVideo",media);
                /*if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }*/
                Log.d(TAG,"saving isplaying = "+media.isMediaPlaying());
                Log.d(TAG,"saving seek to = "+media.getMediaPosition());
                getActivity().getIntent().putExtra("saveVideoForMinimize",media);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(!isImage()) {
            fitVideoToScreen();
        }
    }

    int mediaPositionForMinimize;
    boolean isMediaPlayingForMinmize;

    public void showMediaControls()
    {
        if(isImage()) {
            Log.d(TAG,"hide = "+controlVisbilityPreference.isHideControl());
            if (controlVisbilityPreference.isHideControl()) {
                controlVisbilityPreference.setHideControl(false);
                topBar.setVisibility(View.GONE);
            } else {
                controlVisbilityPreference.setHideControl(true);
                topBar.setVisibility(View.VISIBLE);
            }
        }
        else{
            Log.d(TAG,"hide = "+controlVisbilityPreference.isHideControl());
            if (controlVisbilityPreference.isHideControl()) {
                controlVisbilityPreference.setHideControl(false);
                hideAllControls();
            } else {
                controlVisbilityPreference.setHideControl(true);
                showAllControls();
            }
        }
    }

    public void hideAllControls(){
        topBar.setVisibility(View.GONE);
        videoControls.setVisibility(View.GONE);
        pause.setVisibility(View.GONE);
        startTime.setVisibility(View.GONE);
        endTime.setVisibility(View.GONE);
        videoSeek.setVisibility(View.GONE);
    }

    public void showAllControls(){
        topBar.setVisibility(View.VISIBLE);
        videoControls.setVisibility(View.VISIBLE);
        pause.setVisibility(View.VISIBLE);
        startTime.setVisibility(View.VISIBLE);
        endTime.setVisibility(View.VISIBLE);
        videoSeek.setVisibility(View.VISIBLE);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG,"surfaceCreated = "+path);
        if(!isImage()){
            try{
                mediaPlayer.setDisplay(surfaceHolder);
            }
            catch (IllegalStateException illegal){
                Log.d(TAG,"IllegalStateException ");
                /*mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnCompletionListener(this);
                mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.setOnErrorListener(this);
                mediaPlayer.setDisplay(surfaceHolder);*/
            }
            try {
                mediaPlayer.setDataSource("file://"+path);
                mediaPlayer.prepare();
                Log.d(TAG,"MP prepared");
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(getUserVisibleHint()){
                startTrackerThread();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG,"surfaceDestroyed = "+path);
        if(!isImage()){
            Log.d(TAG,"Released");
            stopTrackerThread();
            try {
                videoTracker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mediaPlayer.release();
        }
    }

    public void resetMediaPlayer(){
        if(mediaPlayer.getCurrentPosition() > 100){
            mediaPlayer.seekTo(100);
        }
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

    public void resetVideoTime(){
        hours = 0;
        minutes = 0;
        seconds = 0;
    }

    public boolean isPlayCompleted(){
        return isCompleted;
    }

    public void setIsPlayCompleted(boolean playCompleted){
        isCompleted = playCompleted;
    }
    public boolean isImage()
    {
        if(path.endsWith(getResources().getString(R.string.IMG_EXT)) || path.endsWith(getResources().getString(R.string.ANOTHER_IMG_EXT))){
            return true;
        }
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG,"Video Completed == "+path);
        showAllControls();
        //mediaPlayer.pause();
        isCompleted = true;
        pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
        play = false;
        //mediaPlayer.seekTo(0);
        videoSeek.setProgress(0);
        seconds=0; minutes=0; hours=0;
        showTimeElapsed();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.d(TAG,"onPrepared = "+path);
        mediaPlayer.seekTo(100);
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        Log.d(TAG,"onError = "+path);
        return true;
    }

    class MainHandler extends Handler {
        WeakReference<SurfaceViewVideoFragment> surfaceViewVideoFragmentWeakReference;
        SurfaceViewVideoFragment mediaAct;

        public MainHandler(SurfaceViewVideoFragment surfVidFrag) {
            surfaceViewVideoFragmentWeakReference = new WeakReference<>(surfVidFrag);
        }

        @Override
        public void handleMessage(Message msg) {
            mediaAct = surfaceViewVideoFragmentWeakReference.get();
            switch(msg.what)
            {
                case VIDEO_SEEK_UPDATE:
                    showTimeElapsed();
                    break;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onResume, visible? ="+getUserVisibleHint());
        Log.d(TAG,"Path = "+images[framePosition].getPath());
        if(!isImage()){
            previousPos = 0;
            //startTrackerThread();
        }
        else{
            Media savedVideo = getActivity().getIntent().getParcelableExtra("saveVideoForMinimize");
            Log.d(TAG,"SAVED VIDEO = "+savedVideo);
            if(savedVideo != null) {
                Log.d(TAG,"MP state = "+mediaPlayer);
                if(mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setOnCompletionListener(this);
                    mediaPlayer.setOnPreparedListener(this);
                    mediaPlayer.setOnErrorListener(this);
                }
                reConstructVideo(savedVideo);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG,"onPause, visible? ="+getUserVisibleHint());
        Log.d(TAG,"Path = "+images[framePosition].getPath());
        if (videoView != null) {
            Log.d(TAG, "Save media state hide = "+controlVisbilityPreference.isHideControl());
            //stopTrackerThread();
        }
        if(!isImage()) {
            if (getUserVisibleHint()) {
                mediaPositionForMinimize = mediaPlayer.getCurrentPosition();
                isMediaPlayingForMinmize = mediaPlayer.isPlaying();
            }
        }
    }

    public void showTimeElapsed()
    {
        String showSec = "0";
        String showMin = "0";
        String showHr = "0";
        //Log.d(TAG,"seconds = "+seconds);
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

    transient Object trackerSync = new Object();
    volatile boolean isTrackerReady = false;

    public void startTrackerThread()
    {
        mediaHandler = new MainHandler(this);
        startTracker = true;
        videoTracker = new VideoTracker();
        videoTracker.start();
        if(play){
            isTrackerReady = false;
            Log.d(TAG,"MAIN WAIT...");
            synchronized (trackerSync){
                while(!isTrackerReady) {
                    try {
                        trackerSync.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            Log.d(TAG,"Continue video..");
            mediaPlayer.start();
        }
        else{
            isTrackerReady = true;
        }
    }

    public void stopTrackerThread()
    {
        Log.d(TAG,"Stopping TRACKER NOW...");
        startTracker = false;
    }

    public boolean isStartTracker()
    {
        return startTracker;
    }

    class VideoTracker extends Thread
    {
        @Override
        public void run() {
            if(!isTrackerReady) {
                synchronized (trackerSync) {
                    isTrackerReady = true;
                    trackerSync.notify();
                }
            }
            Log.d(TAG,"Video Tracker STARTED..."+path);
            while(startTracker){
                try {
                    while (mediaPlayer.isPlaying() && !isCompleted) {
                        int latestPos = mediaPlayer.getCurrentPosition();
                        videoSeek.setProgress(latestPos);
                        if (latestPos > 999 && latestPos % 1000 >= 0) {
                            //showTimeElapsed();
                            if (previousPos == 0) {
                                previousPos = latestPos;
                                if (seconds < 59) {
                                    seconds++;
                                } else if (minutes < 59) {
                                    minutes++;
                                    seconds = 0;
                                } else {
                                    minutes = 0;
                                    seconds = 0;
                                    hours++;
                                }
                                //Log.d(TAG,"seconds 1111 == "+seconds);
                                mediaHandler.sendEmptyMessage(VIDEO_SEEK_UPDATE);
                            } else {
                                if (Math.abs(previousPos - latestPos) >= 1000) {
                                    previousPos = latestPos;
                                    if (seconds < 59) {
                                        seconds++;
                                    } else if (minutes < 59) {
                                        minutes++;
                                        seconds = 0;
                                    } else {
                                        minutes = 0;
                                        seconds = 0;
                                        hours++;
                                    }
                                    //Log.d(TAG,"seconds == "+seconds);
                                    if(seconds > Integer.parseInt(duration) / 1000){
                                        break;
                                    }
                                    mediaHandler.sendEmptyMessage(VIDEO_SEEK_UPDATE);
                                }
                            }
                        }
                        if (!startTracker) {
                            break;
                        }
                    }
                    if(isCompleted){
                        videoSeek.setProgress(0);
                    }
                }
                catch(IllegalStateException illegal){
                    Log.d(TAG,"Catching ILLEGALSTATEEXCEPTION. EXIT Tracker = "+path);
                    startTracker = false;
                }
            }
            Log.d(TAG,"Video Tracker thread EXITING..."+path);
        }
    }
}
