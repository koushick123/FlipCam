package com.flipcam.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.flipcam.ControlVisbilityPreference;
import com.flipcam.GlideApp;
import com.flipcam.R;
import com.flipcam.media.FileMedia;
import com.flipcam.media.Media;
import com.flipcam.util.MediaUtil;

import java.io.File;
import java.io.Serializable;
import java.lang.ref.WeakReference;

import static com.flipcam.constants.Constants.VIDEO_SEEK_UPDATE;

/**
 * Created by Koushick on 28-11-2017.
 */

public class MediaFragment extends Fragment implements MediaPlayer.OnCompletionListener,MediaPlayer.OnPreparedListener,
MediaPlayer.OnErrorListener, Serializable{

    static final String TAG = "MediaFragment";
    transient public MediaPlayer mediaPlayer = null;
    transient public MediaView videoView=null;
    public boolean play=false;
    transient LinearLayout topBar;
    public String path;
    transient ImageButton pause;
    volatile boolean startTracker=false;
    transient MediaFragment.MainHandler mediaHandler;
    SeekBar videoSeek;
    volatile boolean isCompleted=false;
    String duration;
    TextView startTime;
    TextView endTime;
    public boolean playInProgress=false;
    volatile int seconds = 0;
    volatile int minutes = 0;
    volatile int hours = 0;
    public int previousPos = 0;
    transient LinearLayout videoControls;
    transient LinearLayout parentMedia;
    transient FrameLayout frameMedia;
    int framePosition;
    transient ImageView picture;
    transient FileMedia[] images=null;
    ControlVisbilityPreference controlVisbilityPreference;
    transient Display display;
    transient MediaFragment.VideoTracker videoTracker;
    transient public Media savedVideo = null;
    transient boolean recreate = false;
    transient ImageView playCircle;
    transient int imageHeight;
    transient int imageWidth;
    transient FrameLayout mediaPlaceholder;
    boolean VERBOSE = true;
    AudioManager audioManager;

    public static MediaFragment newInstance(int pos,boolean recreate){
        MediaFragment mediaFragment = new MediaFragment();
        Bundle args = new Bundle();
        args.putInt("position", pos);
        args.putBoolean("recreate",recreate);
        mediaFragment.setArguments(args);
        return mediaFragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        audioManager = (AudioManager)getActivity().getSystemService(Context.AUDIO_SERVICE);
        if(VERBOSE)Log.d(TAG,"onActivityCreated = "+path);
        topBar = (LinearLayout)getActivity().findViewById(R.id.topMediaControls);
        videoControls = (LinearLayout)getActivity().findViewById(R.id.videoControls);
        pause = (ImageButton) getActivity().findViewById(R.id.playButton);
        startTime = (TextView)getActivity().findViewById(R.id.startTime);
        if(VERBOSE)Log.d(TAG, "startTime = "+startTime);
        endTime = (TextView)getActivity().findViewById(R.id.endTime);
        if(VERBOSE)Log.d(TAG, "endTime = "+endTime);
        videoSeek = (SeekBar)getActivity().findViewById(R.id.videoSeek);
        if(VERBOSE)Log.d(TAG, "videoSeek = "+videoSeek);
        parentMedia = (LinearLayout)getActivity().findViewById(R.id.parentMedia);
        frameMedia = (FrameLayout)getActivity().findViewById(R.id.frameMedia);
        controlVisbilityPreference = (ControlVisbilityPreference) getActivity().getApplicationContext();
        playCircle = (ImageView)getActivity().findViewById(R.id.playVideo);

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
                    if(recreate){
                        recreate = false;
                        if(VERBOSE)Log.d(TAG,"recreate video");
                        newVideo = new Media();
                        newVideo.setMediaActualDuration(duration);
                        newVideo.setMediaCompleted(false);
                        newVideo.setMediaControlsHide(true);
                        newVideo.setMediaPlaying(false);
                        newVideo.setMediaPosition(0);
                        newVideo.setMediaPreviousPos(0);
                        newVideo.setSeekDuration(Integer.parseInt(duration));
                    }
                }
                else{
                    if(VERBOSE)Log.d(TAG,"setup video");
                    newVideo = new Media();
                    newVideo.setMediaActualDuration(duration);
                    newVideo.setMediaCompleted(false);
                    newVideo.setMediaControlsHide(true);
                    newVideo.setMediaPlaying(false);
                    newVideo.setMediaPosition(0);
                    newVideo.setMediaPreviousPos(0);
                    newVideo.setSeekDuration(Integer.parseInt(duration));
                }
                if(VERBOSE)Log.d(TAG, "Set Seek BAR");
                reConstructVideo(newVideo);
                showTimeElapsed();
                calculateAndDisplayEndTime(Integer.parseInt(duration), true);
            }
        }
    }

    AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener(){
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG, "onAudioFocusChange = "+focusChange);
            switch(focusChange){
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                        Log.d(TAG, "setStreamMute");
                        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                    }
                    else{
                        Log.d(TAG, "adjustStreamVolume");
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    /*if(play) {
                        mediaPlayer.pause();
                        pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow));
                        play = false;
                    }*/
                    audioManager.abandonAudioFocus(this);
                    break;
            }
        }
    };

    public void reConstructVideo(Media savedVideo){
        videoSeek.setMax(savedVideo.getSeekDuration());
        //videoSeek.setThumb(getResources().getDrawable(R.drawable.turqoise));
        videoSeek.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.turqoise)));
        if(VERBOSE)Log.d(TAG, "Retrieve media completed == " + savedVideo.isMediaCompleted());
        if (savedVideo.isMediaCompleted()) {
            videoSeek.setProgress(0);
            isCompleted = true;
        } else {
            if(VERBOSE)Log.d(TAG,"Set SEEK to = "+savedVideo.getMediaPosition());
            mediaPlayer.seekTo(savedVideo.getMediaPosition());
            videoSeek.setProgress(savedVideo.getMediaPosition());
        }
        if(VERBOSE)Log.d(TAG, "Retrieve media playing = " + savedVideo.isMediaPlaying());
        if (savedVideo.isMediaPlaying()) {
            pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
            play = true;
        } else {
            pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow));
            play = false;
        }
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!play) {
                    if(VERBOSE)Log.d(TAG, "Set PLAY post rotate");
                    startPlayingMedia();
                } else {
                    int audioFocus = audioManager.abandonAudioFocus(onAudioFocusChangeListener);
                    if(VERBOSE)Log.d(TAG, "Set PAUSE post rotate = "+audioFocus);
                    if(audioFocus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        mediaPlayer.pause();
                        pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow));
                        play = false;
                    }
                }
            }
        });
        playCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!play) {
                    if(VERBOSE)Log.d(TAG, "Set PLAY Circle post rotate");
                    startPlayingMedia();
                }
            }
        });
        //Get SAVED MEDIA CONTROLS VIEW STATE
        if(VERBOSE)Log.d(TAG, "Retrieve media controls hide = " + savedVideo.isMediaControlsHide());
        if (savedVideo.isMediaControlsHide()) {
            showAllControls();
        } else {
            hideAllControls();
        }
        controlVisbilityPreference.setHideControl(savedVideo.isMediaControlsHide());

        //Get MEDIA DURATION
        if(VERBOSE)Log.d(TAG, "Retrieve media duration = " + savedVideo.getMediaActualDuration());
        duration = savedVideo.getMediaActualDuration();

        //Get SAVED PREVIOUS TIME
        if(VERBOSE)Log.d(TAG, "Retrieve media previous time = " + savedVideo.getMediaPreviousPos());
        previousPos = savedVideo.getMediaPreviousPos();

        //Get CURRENT TIME
        //if(VERBOSE)Log.d(TAG, "Retrieve current time = " + savedInstanceState.getString(MEDIA_CURRENT_TIME));
        if(VERBOSE)Log.d(TAG, "Retrieve current time = " +savedVideo.getMediaPosition() / 1000);
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
        if(savedVideo.isMediaCompleted()){
            //Since we need to seekTo(100), we need to nullify savedVideo.
            this.savedVideo = null;
        }

        videoSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    videoSeek.setProgress(progress);
                    mediaPlayer.seekTo(progress);
                    calculateAndDisplayEndTime(progress, false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.pause();
                    wasPlaying = true;
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(wasPlaying){
                    mediaPlayer.start();
                    wasPlaying = false;
                }
            }
        });
    }

    private void startPlayingMedia(){
        int audioFocus = audioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        Log.d(TAG, "audioFocus = "+audioFocus);
        if(audioFocus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (isCompleted) {
                isCompleted = false;
            }
            mediaPlayer.start();
            playInProgress = true;
            pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
            playCircle.setVisibility(View.GONE);
            play = true;
        }
    }

    boolean wasPlaying = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        framePosition = getArguments().getInt("position");
        recreate = getArguments().getBoolean("recreate");
        //if(VERBOSE)Log.d(TAG,"framePosition = "+framePosition);
        images = MediaUtil.getMediaList(getContext());
        path = images[framePosition].getPath();
        if(VERBOSE)Log.d(TAG,"media is == "+path+", recreate = "+recreate);
        setRetainInstance(true);
    }

    public int getFramePosition(){
        return framePosition;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_surfaceview_video, container, false);
        videoView = (MediaView) view.findViewById(R.id.recordedVideo);
        picture = (ImageView)view.findViewById(R.id.recordedImage);
        mediaPlaceholder = (FrameLayout)view.findViewById(R.id.mediaPlaceholder);
        mediaPlaceholder.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                showMediaControls();
            }
        });
        WindowManager windowManager = (WindowManager)getActivity().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
        Point screenSize=new Point();
        display.getRealSize(screenSize);
        if(VERBOSE)Log.d(TAG,"onCreateView = "+path);
        if(isImage()) {
            if(VERBOSE)Log.d(TAG,"show image");
            videoView.setData(null,path,this);
            videoView.setVisibility(View.GONE);
            Bitmap image = BitmapFactory.decodeFile(path);
            imageHeight = image.getHeight();
            imageWidth = image.getWidth();
            fitPhotoToScreen();
            Uri uri = Uri.fromFile(new File(path));
            GlideApp.with(getContext()).load(uri).into(picture);
        }
        else {
            if(VERBOSE)Log.d(TAG,"show video");
            picture.setVisibility(View.GONE);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnErrorListener(this);
            videoView.setData(mediaPlayer,path,this);
            videoView.setKeepScreenOn(true);
            videoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showMediaControls();
                }
            });
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(path);
            duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        }
        return view;
    }

    public void fitPhotoToScreen(){
        Point screenSize=new Point();
        display.getRealSize(screenSize);
        double screenAR = (double)screenSize.x / (double)screenSize.y;
        if(VERBOSE)Log.d(TAG, "screenSize = "+screenSize.x+" X "+screenSize.y);
        double imageAR = (double)imageWidth / (double)imageHeight;
        if(VERBOSE)Log.d(TAG, "imageSize = "+imageWidth+" X "+imageHeight);
        if(VERBOSE)Log.d(TAG,"imageAR = "+imageAR);
        if(VERBOSE)Log.d(TAG,"screenAR = "+screenAR);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if (display.getRotation() == Surface.ROTATION_0) {
            if(Math.abs(screenAR - imageAR) < 0.01) {
                if(VERBOSE)Log.d(TAG,"Portrait");
                layoutParams.width = screenSize.x;
                layoutParams.height = screenSize.y;
            }
            if(VERBOSE)Log.d(TAG,"1111 layoutParams.width = "+layoutParams.width);
            if(VERBOSE)Log.d(TAG,"layoutParams.height = "+layoutParams.height);
        }
        else if (display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270) {
            if (Math.abs(screenAR - imageAR) < 0.1) {
                if(VERBOSE)Log.d(TAG,"Landscape");
                layoutParams.width = screenSize.x;
                layoutParams.height = screenSize.y;
            }
            if(VERBOSE)Log.d(TAG,"2222 layoutParams.width = "+layoutParams.width);
            if(VERBOSE)Log.d(TAG,"layoutParams.height = "+layoutParams.height);
        }
        picture.setLayoutParams(layoutParams);
    }

    public void fitVideoToScreen(){
        Point screenSize=new Point();
        display.getRealSize(screenSize);
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(path);
        String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        double videoAR = Double.parseDouble(width) / Double.parseDouble(height);
        double screenAR = (double) screenSize.x / (double) screenSize.y;
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if (display.getRotation() == Surface.ROTATION_0) {
            if (Math.abs(screenAR - videoAR) < 0.01) {
                layoutParams.width = screenSize.x;
                layoutParams.height = screenSize.y;
                videoView.setLayoutParams(layoutParams);
            }
            else{
                layoutParams.width = screenSize.x;
                layoutParams.height = (int)(screenSize.x / videoAR);
                layoutParams.gravity = Gravity.CENTER;
                videoView.setLayoutParams(layoutParams);
            }
        } else if (display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270) {
            if (Math.abs(screenAR - videoAR) < 0.1) {
                layoutParams.width = screenSize.x;
                layoutParams.height = screenSize.y;
                videoView.setLayoutParams(layoutParams);
            }
            else{
                layoutParams.width = (int)(videoAR * screenSize.y);
                layoutParams.height = screenSize.y;
                layoutParams.gravity = Gravity.CENTER;
                videoView.setLayoutParams(layoutParams);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(VERBOSE)Log.d(TAG,"onSaveInstanceState = "+path+" , "+playInProgress);
        if(VERBOSE)Log.d(TAG,"getUserVisibleHint ? ="+getUserVisibleHint());
        if(!isImage()) {
            outState.putBoolean("videoPlayed", playInProgress);
            if (getUserVisibleHint()) {
                Media media = new Media();
                media.setMediaPlaying(isMediaPlayingForMinmize);
                media.setMediaPosition(mediaPositionForMinimize);
                media.setMediaControlsHide(controlVisbilityPreference.isHideControl());
                media.setMediaActualDuration(duration);
                if(videoSeek != null) {
                    media.setSeekDuration(videoSeek.getMax());
                }
                media.setMediaCompleted(isCompleted);
                media.setMediaPreviousPos(previousPos);
                outState.putParcelable("currentVideo",media);
                if(VERBOSE)Log.d(TAG,"saving isplaying = "+media.isMediaPlaying());
                if(VERBOSE)Log.d(TAG,"saving seek to = "+media.getMediaPosition());
                getActivity().getIntent().putExtra("saveVideoForMinimize",media);
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.pause();
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(!isImage()) {
            fitVideoToScreen();
        }
        else{
            fitPhotoToScreen();
        }
    }

    int mediaPositionForMinimize;
    boolean isMediaPlayingForMinmize;

    public void showMediaControls()
    {
        if(isImage()) {
            if(VERBOSE)Log.d(TAG,"hide = "+controlVisbilityPreference.isHideControl());
            if(VERBOSE)Log.d(TAG, "videoControls = "+videoControls.getVisibility());
            if (controlVisbilityPreference.isHideControl()) {
                controlVisbilityPreference.setHideControl(false);
                topBar.setVisibility(View.GONE);
                videoControls.setVisibility(View.GONE);
            } else {
                controlVisbilityPreference.setHideControl(true);
                topBar.setVisibility(View.VISIBLE);
                videoControls.setVisibility(View.VISIBLE);
            }
        }
        else{
            if(VERBOSE)Log.d(TAG,"hide = "+controlVisbilityPreference.isHideControl());
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
        if(!play) {
            playCircle.setVisibility(View.VISIBLE);
        }
    }

    public void showAllControls(){
        topBar.setVisibility(View.VISIBLE);
        videoControls.setVisibility(View.VISIBLE);
        pause.setVisibility(View.VISIBLE);
        startTime.setVisibility(View.VISIBLE);
        endTime.setVisibility(View.VISIBLE);
        videoSeek.setVisibility(View.VISIBLE);
        playCircle.setVisibility(View.GONE);
    }

    public void removeVideoControls(){
        pause.setVisibility(View.INVISIBLE);
        startTime.setVisibility(View.INVISIBLE);
        endTime.setVisibility(View.INVISIBLE);
        videoSeek.setVisibility(View.INVISIBLE);
    }

    public void resetMediaPlayer(){
        if(VERBOSE)Log.d(TAG,"getCurrentPosition = "+mediaPlayer.getCurrentPosition());
            mediaPlayer.seekTo(100);
    }

    public void calculateAndDisplayEndTime(int latestPos, boolean eTime)
    {
        int videoLength = latestPos;
        int secs = (videoLength / 1000);
        int hour = 0;
        int mins = 0;
        if(secs >= 60){
            mins = secs / 60;
            if(mins >= 60){
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
        if(eTime) {
            endTime.setText(showHr + " : " + showMin + " : " + showSec);
        }
        else{
            seconds = secs;
            minutes = mins;
            hours = hour;
            startTime.setText(showHr + " : " + showMin + " : " + showSec);
        }
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

    public MediaView getMediaView(){
        return videoView;
    }

    public String getPath(){
        return path;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public void setHours(int hours) {
        this.hours = hours;
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
        if(VERBOSE)Log.d(TAG,"Video Completed == "+path);
        showAllControls();
        controlVisbilityPreference.setHideControl(true);
        isCompleted = true;
        pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow));
        play = false;
        videoSeek.setProgress(0);
        seconds=0; minutes=0; hours=0;
        showTimeElapsed();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        if(VERBOSE)Log.d(TAG,"onPrepared = "+path);
        if(savedVideo == null) {
            mediaPlayer.seekTo(100);
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        if(VERBOSE)Log.d(TAG,"onError = "+path);
        return true;
    }

    class MainHandler extends Handler {
        WeakReference<MediaFragment> mediaFragmentWeakReference;
        MediaFragment mediaAct;

        public MainHandler(MediaFragment mediaFrag) {
            mediaFragmentWeakReference = new WeakReference<>(mediaFrag);
        }

        @Override
        public void handleMessage(Message msg) {
            mediaAct = mediaFragmentWeakReference.get();
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
        if(VERBOSE)Log.d(TAG,"onResume, visible? ="+getUserVisibleHint());
        if(VERBOSE)Log.d(TAG,"Path = "+images[framePosition].getPath());
        if(!isImage()){
            previousPos = 0;
            savedVideo = getActivity().getIntent().getParcelableExtra("saveVideoForMinimize");
            if(VERBOSE)Log.d(TAG,"SAVED VIDEO = "+savedVideo);
        }
        if(getUserVisibleHint()){
            if(isImage()) {
                if(controlVisbilityPreference.isHideControl()) {
                    if(VERBOSE)Log.d(TAG, "show controls onResume");
                    topBar.setVisibility(View.VISIBLE);
                    videoControls.setVisibility(View.VISIBLE);
                    //Do NOT remove below method call.
                    //When user navigates here directly from Gallery,
                    //this is necessary to hide the video controls for an image,
                    //since onPageSelected is not called.
                    removeVideoControls();
                }
                else{
                    if(VERBOSE)Log.d(TAG,"hide controls onResume");
                    topBar.setVisibility(View.GONE);
                    videoControls.setVisibility(View.GONE);
                }
            }
            else{
                videoView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(VERBOSE)Log.d(TAG,"onPause, visible? ="+getUserVisibleHint());
        if(VERBOSE)Log.d(TAG,"Path = "+images[framePosition].getPath());
        if (videoView != null) {
            if(VERBOSE)Log.d(TAG, "Save media state hide = "+controlVisbilityPreference.isHideControl());
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
        //if(VERBOSE)Log.d(TAG,"seconds = "+seconds);
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
            if(VERBOSE)Log.d(TAG,"MAIN WAIT...");
            synchronized (trackerSync){
                while(!isTrackerReady) {
                    try {
                        trackerSync.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if(VERBOSE)Log.d(TAG,"Continue video..");
            int audioFocus = audioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if(audioFocus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mediaPlayer.start();
            }
        }
        else{
            isTrackerReady = true;
        }
    }

    public void stopTrackerThread()
    {
        if(VERBOSE)Log.d(TAG,"Stopping TRACKER NOW...");
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
            if(VERBOSE)Log.d(TAG,"Video Tracker STARTED..."+path);
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
                                //if(VERBOSE)Log.d(TAG,"seconds 1111 == "+seconds);
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
                                    //if(VERBOSE)Log.d(TAG,"seconds == "+seconds);
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
                    if(VERBOSE)Log.d(TAG,"Catching ILLEGALSTATEEXCEPTION. EXIT Tracker = "+path);
                    startTracker = false;
                }
            }
            if(VERBOSE)Log.d(TAG,"Video Tracker thread EXITING..."+path);
        }
    }
}
