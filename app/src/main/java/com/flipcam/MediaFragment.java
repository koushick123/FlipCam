package com.flipcam;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
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

import static com.flipcam.PermissionActivity.FC_MEDIA_PREFERENCE;
import static com.flipcam.constants.Constants.IMAGE_CONTROLS_HIDE;
import static com.flipcam.constants.Constants.VIDEO_SEEK_UPDATE;

/**
 * Created by koushick on 29-Oct-17.
 */

public class MediaFragment extends Fragment implements MediaPlayer.OnCompletionListener{

    private static final String TAG = "MediaFragment";
    RelativeLayout mediaPlaceholder;
    LinearLayout parentMedia;
    VideoView videoView=null;
    boolean hide=true;
    boolean play=false;
    LinearLayout bottomBar;
    MediaController mediaController;
    LinearLayout mediaBar;
    LinearLayout topBar;
    String path;
    ImageView pause;
    volatile boolean startTracker=false;
    MediaFragment.MainHandler mediaHandler;
    SeekBar videoSeek;
    boolean isCompleted=false;
    String duration;
    TextView startTime;
    TextView endTime;
    volatile boolean updateTimer=false;
    volatile int seconds = 0;
    volatile int minutes = 0;
    volatile int hours = 0;
    int previousPos = 0;
    LinearLayout videoControls;

    public static MediaFragment newInstance(String path){
        MediaFragment mediaFragment = new MediaFragment();
        Bundle args = new Bundle();
        Log.d(TAG, "image path = " + path);
        args.putString("mediaPath", path);
        mediaFragment.setArguments(args);
        return mediaFragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG,"onActivityCreated");
        WindowManager windowManager = (WindowManager)getActivity().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point screenSize=new Point();
        display.getRealSize(screenSize);
        parentMedia = (LinearLayout)getActivity().findViewById(R.id.parentMedia);
        topBar = (LinearLayout)getActivity().findViewById(R.id.topMediaControls);
        videoControls = (LinearLayout)getActivity().findViewById(R.id.videoControls);
        pause = (ImageButton) getActivity().findViewById(R.id.playButton);
        startTime = (TextView)getActivity().findViewById(R.id.startTime);
        endTime = (TextView)getActivity().findViewById(R.id.endTime);
        videoSeek = (SeekBar)getActivity().findViewById(R.id.videoSeek);
        if(!isImage()) {
            /*videoControls.setVisibility(View.VISIBLE);
            pause.setVisibility(View.VISIBLE);
            startTime.setVisibility(View.VISIBLE);
            endTime.setVisibility(View.VISIBLE);
            videoSeek.setVisibility(View.VISIBLE);
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(path);
            String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            calculateAndDisplayEndTime();
            videoSeek.setMax(Integer.parseInt(duration));
            videoSeek.setThumb(getResources().getDrawable(R.drawable.whitecircle));
            videoSeek.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.seekFill)));
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
            mediaController = new MediaController(getActivity());
            LinearLayout.LayoutParams pauseParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            if (display.getRotation() == Surface.ROTATION_0) {
                pauseParams.height = 90;
                pause.setPadding(0, 0, 0, 0);
            } else if (display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270) {
                pauseParams.height = 100;
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
                        videoView.start();
                        pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white_24dp));
                        play = true;
                        updateTimer = true;
                    } else {
                        videoView.pause();
                        pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
                        play = false;
                        updateTimer = false;
                    }
                }
            });*/
        }
        else{
            videoControls.setVisibility(View.GONE);
            /*pause.setVisibility(View.GONE);
            startTime.setVisibility(View.GONE);
            endTime.setVisibility(View.GONE);
            videoSeek.setVisibility(View.GONE);*/
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG,"onStop");
    }

    public int calculateInSampleSize(BitmapFactory.Options opt,int reqwidth, int reqheight)
    {
        int height = opt.outHeight;
        int width = opt.outWidth;
        Log.d(TAG,"orig width= "+width);
        Log.d(TAG,"orig height= "+height);
        int sampleSize = 1;
        if(reqwidth < width || reqheight < height){
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            Log.d(TAG,"(halfHeight / sampleSize) = "+(halfHeight / sampleSize));
            Log.d(TAG,"(halfWidth / sampleSize) = "+(halfWidth / sampleSize));
            while((halfHeight / sampleSize) > reqheight && (halfWidth / sampleSize) > reqwidth){
                Log.d(TAG,"(halfHeight / sampleSize) inside = "+(halfHeight / sampleSize));
                Log.d(TAG,"(halfWidth / sampleSize) inside = "+(halfWidth / sampleSize));
                sampleSize *= 2;
            }
        }
        Log.d(TAG,"samplesize = "+sampleSize);
        return sampleSize;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        path = getArguments().getString("mediaPath");
        Log.d(TAG,"path = "+path);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_media, container, false);
        mediaPlaceholder = (RelativeLayout)view.findViewById(R.id.mediaPlaceholder);
        videoView = (VideoView)view.findViewById(R.id.recordedVideo);
        WindowManager windowManager = (WindowManager)getActivity().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Log.d(TAG,"Rotation = "+display.getRotation());
        Point screenSize=new Point();
        display.getRealSize(screenSize);
        if(isImage()) {
            Log.d(TAG,"show image");
            //videoView.setVisibility(View.GONE);
            ImageView picture = (ImageView)view.findViewById(R.id.picture);
            //picture.setId(picture.generateViewId());
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds=true;
            BitmapFactory.decodeFile(path,options);
            if(display.getRotation() == Surface.ROTATION_0) {
                Log.d(TAG,"for portrait");
                options.inSampleSize = calculateInSampleSize(options, screenSize.x, screenSize.y);
            }
            else if(display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270){
                Log.d(TAG,"for landscape");
                options.inSampleSize = calculateInSampleSize(options, screenSize.y, screenSize.x);
            }
            options.inJustDecodeBounds=false;
            Bitmap image = BitmapFactory.decodeFile(path,options);
            picture.setImageBitmap(image);
            //mediaPlaceholder.addView(picture);
        }
        else{
            /*Log.d(TAG,"show video");
            videoView = (VideoView)view.findViewById(R.id.recordedVideo);
            videoView.setVisibility(View.VISIBLE);
            videoView.setKeepScreenOn(true);
            videoView.setVideoPath("file://"+path);
            videoView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    showMediaControls(view);
                }
            });*/
        }
        if(savedInstanceState == null) {
            SharedPreferences mediaValues = getActivity().getSharedPreferences(FC_MEDIA_PREFERENCE,Context.MODE_PRIVATE);
            SharedPreferences.Editor mediaState = null;
            if(mediaValues!=null){
                mediaState = mediaValues.edit();
                if(mediaState!=null){
                    mediaState.clear();
                    mediaState.commit();
                    Log.d(TAG,"CLEAR ALL");
                }
            }
            //videoView.start();
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        if(isImage()){
            SharedPreferences mediaState = getActivity().getSharedPreferences(FC_MEDIA_PREFERENCE, Context.MODE_PRIVATE);
            if(mediaState!=null && mediaState.contains(IMAGE_CONTROLS_HIDE)){
                if(mediaState.getBoolean(IMAGE_CONTROLS_HIDE,true)){
                    Log.d(TAG,"Visible");
                    //bottomBar.setVisibility(View.VISIBLE);
                }
                else{
                    Log.d(TAG,"GONE");
                    //bottomBar.setVisibility(View.GONE);
                }
                hide = mediaState.getBoolean(IMAGE_CONTROLS_HIDE,true);
            }
        }
        else {
            /*videoView.setOnCompletionListener(this);
            previousPos = 0;
            startTrackerThread();
            SharedPreferences mediaState = getActivity().getSharedPreferences(FC_MEDIA_PREFERENCE, Context.MODE_PRIVATE);
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
                        pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white_24dp));
                        play = true;
                    } else {
                        videoView.pause();
                        pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
                        play = false;
                    }
                }
                //Get SAVED MEDIA CONTROLS VIEW STATE
                if(mediaState.contains(MEDIA_CONTROLS_HIDE)){
                    Log.d(TAG, "Retrieve media controls hide = " + mediaState.getBoolean(MEDIA_CONTROLS_HIDE, true));
                    if (mediaState.getBoolean(MEDIA_CONTROLS_HIDE, true)) {
                        topBar.setVisibility(View.VISIBLE);
                        videoControls.setVisibility(View.VISIBLE);
                        videoSeek.setVisibility(View.VISIBLE);
                        startTime.setVisibility(View.VISIBLE);
                        endTime.setVisibility(View.VISIBLE);
                    } else {
                        topBar.setVisibility(View.GONE);
                        videoControls.setVisibility(View.GONE);
                        videoSeek.setVisibility(View.GONE);
                        startTime.setVisibility(View.GONE);
                        endTime.setVisibility(View.GONE);
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
            }*/
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");
        if(isImage()){
            SharedPreferences.Editor mediaState = getActivity().getSharedPreferences(FC_MEDIA_PREFERENCE, Context.MODE_PRIVATE).edit();
            Log.d(TAG,"saving hide = "+hide);
            mediaState.putBoolean(IMAGE_CONTROLS_HIDE,hide);
            mediaState.commit();
        }
        else {
            /*if (videoView != null) {
                Log.d(TAG, "Save media state");
                stopTrackerThread();
                SharedPreferences.Editor mediaState = getActivity().getSharedPreferences(FC_MEDIA_PREFERENCE, Context.MODE_PRIVATE).edit();
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
            }*/
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG,"Video Completed");
        pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
        play = false;
        isCompleted = true;
        videoView.seekTo(0);
        videoSeek.setProgress(0);
        updateTimer=false;
        seconds=0; minutes=0; hours=0;
        showTimeElapsed();
    }

    public void startTrackerThread()
    {
        mediaHandler = new MainHandler(this);
        startTracker = true;
        MediaFragment.VideoTracker videoTracker = new VideoTracker();
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

    class MainHandler extends Handler {
        WeakReference<MediaFragment> mediaFragment;
        MediaFragment mediaAct;

        public MainHandler(MediaFragment mediaFragment1) {
            mediaFragment = new WeakReference<>(mediaFragment1);
        }

        @Override
        public void handleMessage(Message msg) {
            mediaAct = mediaFragment.get();
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
