package com.flipcam;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.flipcam.media.FileMedia;
import com.flipcam.util.MediaUtil;
import com.flipcam.view.SurfaceViewVideoFragment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import static com.flipcam.PermissionActivity.FC_MEDIA_PREFERENCE;

public class MediaActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener{

    private static final String TAG = "MediaActivity";
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    FileMedia[] medias = null;
    LinearLayout videoControls;
    LinearLayout topMediaControls;
    String duration;
    TextView startTime;
    TextView endTime;
    ImageView pause;
    SeekBar videoSeek;
    LinearLayout timeControls;
    LinearLayout parentMedia;
    HashMap<Integer,SurfaceViewVideoFragment> hashMapFrags = new HashMap<>();
    ControlVisbilityPreference controlVisbilityPreference;
    ImageButton deleteMedia;

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
    Display display;
    Point screenSize;

    public void deleteMedia(int position)
    {
        Log.d(TAG,"Length before delete = "+medias.length);
        Log.d(TAG,"Deleting file = "+medias[position].getPath());
        if(MediaUtil.deleteFile(medias[position])) {
            /*FileMedia[] oldMedia;
            oldMedia = Arrays.copyOf(medias,medias.length);
            Log.d(TAG, "Regenerate the list");
            medias = MediaUtil.getMediaList(getApplicationContext());
            Log.d(TAG,"Length AFTER delete = "+medias.length);
            if(medias.length > 0) {
                previousSelectedFragment = -1;
                //Update hashMapFrags to be able to navigate correctly
                Set<Integer> keys = hashMapFrags.keySet();
                Iterator<Integer> iterator = keys.iterator();
                while(iterator.hasNext()) {
                    Integer pos = iterator.next();
                    Log.d(TAG,"Frag pos = "+pos.intValue()+", Path = "+hashMapFrags.get(pos.intValue()).getPath());
                    if(pos.intValue() == position){
                        hashMapFrags.remove(pos.intValue());
                        hashMapFrags.put(pos.intValue(),hashMapFrags.get(pos.intValue()+1));
                        hashMapFrags.remove(pos.intValue()+1);
                    }
                }
                keys = hashMapFrags.keySet();
                iterator = keys.iterator();
                while(iterator.hasNext()) {
                    Integer pos = iterator.next();
                    Log.d(TAG,"Frag pos AFTER delete = "+pos.intValue()+", Path = "+hashMapFrags.get(pos.intValue()).getPath());
                }
                HashMap<Integer,SurfaceViewVideoFragment> cloneMap = (HashMap)hashMapFrags.clone();
                mPagerAdapter = new MediaSlidePager(getSupportFragmentManager());
                mPager.setAdapter(mPagerAdapter);
                //Repopulate the HashMap to maintain scroll order
                hashMapFrags = cloneMap;
                if (position < oldMedia.length - 1) {
                    Log.d(TAG, "Move to position = " + (position));
                    mPager.setCurrentItem(position);
                } else if(position == oldMedia.length - 1){
                    Log.d(TAG, "Move to LEFT new position = " + (position - 1));
                    mPager.setCurrentItem(position - 1);
                }
            }
            else{
                //Show empty media placeholder
            }*/
            if(position < medias.length - 1){
                setupVideo(hashMapFrags.get(position+1),position+1);
            }
            else if(position == medias.length - 1){
                setupVideo(hashMapFrags.get(position-1),position-1);
            }
            medias = MediaUtil.getMediaList(getApplicationContext());
            mPagerAdapter.notifyDataSetChanged();
        }
        else{
            Toast.makeText(getApplicationContext(),"Unable to delete file",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
        setContentView(R.layout.activity_media);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        WindowManager windowManager = (WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
        screenSize=new Point();
        display.getRealSize(screenSize);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        medias = MediaUtil.getMediaList(getApplicationContext());
        mPager = (ViewPager) findViewById(R.id.mediaPager);
        mPager.setOffscreenPageLimit(1);
        mPagerAdapter = new MediaSlidePager(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        deleteMedia = (ImageButton)findViewById(R.id.deleteMedia);
        deleteMedia.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Log.d(TAG,"Delete position = "+selectedPosition);
                deleteMedia(selectedPosition);
            }
        });
        videoControls = (LinearLayout)findViewById(R.id.videoControls);
        pause = (ImageButton) findViewById(R.id.playButton);
        startTime = (TextView)findViewById(R.id.startTime);
        endTime = (TextView)findViewById(R.id.endTime);
        videoSeek = (SeekBar)findViewById(R.id.videoSeek);
        topMediaControls = (LinearLayout)findViewById(R.id.topMediaControls);
        timeControls = (LinearLayout)findViewById(R.id.timeControls);
        parentMedia = (LinearLayout)findViewById(R.id.parentMedia);
        controlVisbilityPreference = (ControlVisbilityPreference)getApplicationContext();
        if(isImage(medias[0].getPath())) {
            videoControls.setVisibility(View.GONE);
            pause.setVisibility(View.GONE);
            startTime.setVisibility(View.GONE);
            endTime.setVisibility(View.GONE);
            videoSeek.setVisibility(View.GONE);
        }
        if(savedInstanceState == null){
            clearPreferences();
            controlVisbilityPreference.setHideControl(true);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    int previousSelectedFragment = 0;

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        //Log.d(TAG,"onRestoreInstanceState");
        if(savedInstanceState!=null){
            previousSelectedFragment = savedInstanceState.getInt("previousSelectedFragment");
            Log.d(TAG,"previousSelectedFragment was = "+previousSelectedFragment);
            hashMapFrags = (HashMap)savedInstanceState.getSerializable("availableFragments");
            selectedPosition = savedInstanceState.getInt("selectedPosition");
            Log.d(TAG,"select position = "+selectedPosition);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("previousSelectedFragment",previousSelectedFragment);
        outState.putInt("selectedPosition",selectedPosition);
        outState.putSerializable("availableFragments",hashMapFrags);
        super.onSaveInstanceState(outState);
    }

    public void clearPreferences(){
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
    }

    //Default to first fragment, if user did not scroll.
    int selectedPosition = 0;

    @Override
    public void onPageSelected(int position) {
        Log.d(TAG,"onPageSelected = "+position+", previousSelectedFragment = "+previousSelectedFragment);
        selectedPosition = position;
        final SurfaceViewVideoFragment currentFrag = hashMapFrags.get(position);
        Log.d(TAG,"isHideControl = "+controlVisbilityPreference.isHideControl());
        //Reset preferences for every new fragment to be displayed.
        clearPreferences();
        if(previousSelectedFragment != -1) {
            SurfaceViewVideoFragment previousFragment = hashMapFrags.get(previousSelectedFragment);
            //If previous fragment had a video, stop the video and tracker thread immediately.
            if (!isImage(medias[previousSelectedFragment].getPath())) {
                Log.d(TAG, "Stop previous tracker thread = " + previousFragment.path);
                previousFragment.stopTrackerThread();
                if (previousFragment.mediaPlayer.isPlaying()) {
                    Log.d(TAG, "Pause previous playback");
                    previousFragment.mediaPlayer.pause();
                }
            }
        }
        //Display controls based on image/video
        if(isImage(medias[position].getPath())){
            Log.d(TAG,"HIDE VIDEO");
            videoControls.setVisibility(View.GONE);
            pause.setVisibility(View.GONE);
            startTime.setVisibility(View.GONE);
            endTime.setVisibility(View.GONE);
            videoSeek.setVisibility(View.GONE);
        }
        else{
            if(controlVisbilityPreference.isHideControl()) {
                Log.d(TAG,"show controls");
                videoControls.setVisibility(View.VISIBLE);
                pause.setVisibility(View.VISIBLE);
                startTime.setVisibility(View.VISIBLE);
                endTime.setVisibility(View.VISIBLE);
                videoSeek.setVisibility(View.VISIBLE);
            }
            else{
                Log.d(TAG,"hide controls");
                videoControls.setVisibility(View.GONE);
                pause.setVisibility(View.GONE);
                startTime.setVisibility(View.GONE);
                endTime.setVisibility(View.GONE);
                videoSeek.setVisibility(View.GONE);
            }
            setupVideo(currentFrag,position);
            currentFrag.previousPos = 0;
            Log.d(TAG,"Has VIDEO TRACKER STARTED? = "+currentFrag.isStartTracker());
            if(!currentFrag.isStartTracker()){
                currentFrag.startTrackerThread();
            }
        }
        previousSelectedFragment = position;
    }

    public void setupVideo(final SurfaceViewVideoFragment currentFrag, int position){
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(medias[position].getPath());
        duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        calculateAndDisplayEndTime();
        Log.d(TAG,"Set MEDIA = "+medias[position].getPath());
        //Include tracker and reset position to start playing from start.
        videoControls.removeAllViews();
        videoControls.addView(timeControls);
        videoControls.addView(videoSeek);
        videoControls.addView(parentMedia);
        videoSeek.setMax(Integer.parseInt(duration));
        videoSeek.setProgress(0);
        videoSeek.setThumb(getResources().getDrawable(R.drawable.whitecircle));
        videoSeek.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.seekFill)));
        currentFrag.play=false;
        currentFrag.playInProgress=false;
        getIntent().removeExtra("saveVideoForMinimize");
        currentFrag.savedVideo = null;
        currentFrag.setIsPlayCompleted(false);
        final int pos = position;
        currentFrag.mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                Log.d(TAG,"CATCH onError");
                currentFrag.mediaPlayer.reset();
                try {
                    currentFrag.mediaPlayer.setOnCompletionListener(currentFrag);
                    currentFrag.mediaPlayer.setOnPreparedListener(currentFrag);
                    currentFrag.mediaPlayer.setOnErrorListener(currentFrag);
                    currentFrag.mediaPlayer.setDataSource("file://"+medias[pos].getPath());
                    currentFrag.mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        currentFrag.resetMediaPlayer();
        currentFrag.resetVideoTime();
        LinearLayout.LayoutParams pauseParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (display.getRotation() == Surface.ROTATION_0) {
            pauseParams.height = 100;
            pause.setPadding(0, 0, 0, 0);
        } else if (display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270) {
            pauseParams.height = 90;
            pause.setPadding(0, 10, 0, 10);
        }
        pause.setLayoutParams(pauseParams);
        pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!currentFrag.play) {
                    if (currentFrag.isPlayCompleted()) {
                        currentFrag.setIsPlayCompleted(false);
                    }
                    Log.d(TAG,"Set PLAY");
                    currentFrag.playInProgress = true;
                    Log.d(TAG,"Duration of video = "+currentFrag.mediaPlayer.getDuration()+" , path = "+
                            currentFrag.path.substring(currentFrag.path.lastIndexOf("/"),currentFrag.path.length()));
                    currentFrag.mediaPlayer.start();
                    pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white_24dp));
                    currentFrag.play = true;
                } else {
                    Log.d(TAG,"Set PAUSE");
                    currentFrag.mediaPlayer.pause();
                    pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
                    currentFrag.play = false;
                }
            }
        });
    }

    @Override
    public void onPageScrollStateChanged(int state) {
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
        startTime.setText(getString(R.string.START_TIME));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPager.addOnPageChangeListener(this);
        Log.d(TAG,"onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPager.removeOnPageChangeListener(this);
        Log.d(TAG,"onPause");
    }

    public boolean isImage(String path)
    {
        if(path.endsWith(getResources().getString(R.string.IMG_EXT)) || path.endsWith(getResources().getString(R.string.ANOTHER_IMG_EXT))){
            return true;
        }
        return false;
    }

    class MediaSlidePager extends FragmentStatePagerAdapter
    {
        @Override
        public int getCount() {
            return medias.length;
        }

        @Override
        public Fragment getItem(int position) {
            Log.d(TAG,"getItem = "+position);
            SurfaceViewVideoFragment surfaceViewVideoFragment = SurfaceViewVideoFragment.newInstance(position);
            hashMapFrags.put(Integer.valueOf(position),surfaceViewVideoFragment);
            return surfaceViewVideoFragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            hashMapFrags.remove(position);
        }

        public MediaSlidePager(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getItemPosition(Object object) {
            Set<Integer> keys = hashMapFrags.keySet();
            Iterator<Integer> iterator = keys.iterator();
            while(iterator.hasNext()) {
                Integer pos = iterator.next();
                Log.d(TAG,"Frag pos = "+pos.intValue()+", Path = "+hashMapFrags.get(pos.intValue()).getPath());
            }
            return POSITION_NONE;
        }
    }
}
