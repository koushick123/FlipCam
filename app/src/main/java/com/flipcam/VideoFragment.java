package com.flipcam;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.flipcam.constants.Constants;
import com.flipcam.view.CameraView;

import java.io.File;
import java.util.Arrays;

import static com.flipcam.PermissionActivity.FC_SHARED_PREFERENCE;


public class VideoFragment extends Fragment{

    public static final String TAG = "VideoFragment";
    SeekBar zoombar;
    CameraView cameraView;
    ImageButton switchCamera;
    ImageButton startRecord;
    ImageButton flash;
    ImageButton photoMode;
    ImageView substitute;
    ImageView thumbnail;
    ImageButton settings;
    LinearLayout videoBar;
    LinearLayout settingsBar;
    TextView timeElapsed;
    TextView memoryConsumed;
    PermissionInterface permissionInterface;
    ImageButton stopRecord;
    ImageButton videoMode;
    ImageButton capturePic;

    public VideoFragment() {
        // Required empty public constructor
    }

    public static VideoFragment newInstance() {
        VideoFragment fragment = new VideoFragment();
        return fragment;
    }

    public interface PermissionInterface{
        void askPermission();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG,"onActivityCreated");
        if(cameraView!=null) {
            cameraView.setWindowManager(getActivity().getWindowManager());
        }
        settingsBar = (LinearLayout)getActivity().findViewById(R.id.settingsBar);
        settings = (ImageButton)getActivity().findViewById(R.id.settings);
        flash = (ImageButton)getActivity().findViewById(R.id.flashOn);
        flash.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                setFlash();
            }
        });
        cameraView.setFlashButton(flash);
        permissionInterface = (PermissionInterface)getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_video, container, false);

        Log.d(TAG,"Inside video fragment");
        substitute = (ImageView)view.findViewById(R.id.substitute);
        substitute.setVisibility(View.INVISIBLE);
        cameraView = (CameraView)view.findViewById(R.id.cameraSurfaceView);
        zoombar = (SeekBar)view.findViewById(R.id.zoomBar);
        zoombar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.progressFill)));
        cameraView.setSeekBar(zoombar);
        zoombar.setProgress(0);
        zoombar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Log.d(TAG, "progress = " + progress);
                if (cameraView.isSmoothZoomSupported()) {
                    //Log.d(TAG, "Smooth zoom supported");
                    cameraView.smoothZoomInOrOut(progress);
                } else if(cameraView.isZoomSupported()){
                    cameraView.zoomInAndOut(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(!cameraView.isSmoothZoomSupported() && !cameraView.isZoomSupported()) {
                    Toast.makeText(getContext(), "Zoom not supported for this camera.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        thumbnail = (ImageView)view.findViewById(R.id.thumbnail);
        photoMode = (ImageButton) view.findViewById(R.id.photoMode);
        photoMode.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view1){
                showPhotoIcons();
            }
        });
        switchCamera = (ImageButton)view.findViewById(R.id.switchCamera);
        switchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecord.setClickable(false);
                flash.setClickable(false);
                photoMode.setClickable(false);
                thumbnail.setClickable(false);

                cameraView.switchCamera();

                zoombar.setProgress(0);
                startRecord.setClickable(true);
                flash.setClickable(true);
                photoMode.setClickable(true);
                thumbnail.setClickable(true);
            }
        });
        startRecord = (ImageButton)view.findViewById(R.id.cameraRecord);
        videoBar = (LinearLayout)view.findViewById(R.id.videoFunctions);
        startRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoBar.removeView(startRecord);
                videoBar.removeView(substitute);
                videoBar.removeView(photoMode);
                videoBar.removeView(thumbnail);
                videoBar.removeView(switchCamera);
                addStopAndPauseIcons();
                hideSettingsBarAndIcon();
                cameraView.record();
            }
        });
        Log.d(TAG,"passing videofragment to cameraview");
        cameraView.setFragmentInstance(this);
        return view;
    }

    ImageButton pauseRecord;
    View.OnClickListener pauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!cameraView.isPaused()) {
                pauseRecord.setImageDrawable(getResources().getDrawable(R.drawable.record_start));
                cameraView.pause();
            } else {
                pauseRecord.setImageDrawable(getResources().getDrawable(R.drawable.record_pause));
                cameraView.resume();
            }
        }
    };

    public boolean isNougatAndAbove()
    {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
    }

    public void showPhotoIcons()
    {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        videoBar.removeView(substitute);
        videoBar.removeView(switchCamera);
        videoBar.removeView(startRecord);
        videoBar.removeView(photoMode);
        videoBar.removeView(thumbnail);
        videoBar.addView(substitute);
        videoBar.addView(switchCamera);
        capturePic = new ImageButton(getContext());
        capturePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(cameraView.capturePhoto()){
                    createAndShowThumbnail(cameraView.getPhotoMediaPath());
                }
                else{
                    Log.d(TAG,"Something went wront with picture capture");
                }
            }
        });
        capturePic.setImageDrawable(getResources().getDrawable(R.drawable.capture_picture));
        layoutParams.height=(int)getResources().getDimension(R.dimen.pictureButtonHeight);
        layoutParams.width=(int)getResources().getDimension(R.dimen.pictureButtonWidth);
        layoutParams.setMargins((int)getResources().getDimension(R.dimen.picBtnLeftMargin),0,(int)getResources().getDimension(R.dimen.picBtnRightMargin),0);
        capturePic.setLayoutParams(layoutParams);
        capturePic.setBackgroundColor(getResources().getColor(R.color.settingsBarColor));
        capturePic.setScaleType(ImageView.ScaleType.CENTER_CROP);
        videoBar.addView(capturePic);
        videoMode = new ImageButton(getContext());
        videoMode.setImageDrawable(getResources().getDrawable(R.drawable.video_mode));
        videoMode.setBackgroundColor(getResources().getColor(R.color.settingsBarColor));
        layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins((int)getResources().getDimension(R.dimen.videoModeBtnLeftMargin),0,(int)getResources().getDimension(R.dimen.videoModeBtnRightMargin),0);
        videoMode.setLayoutParams(layoutParams);
        videoMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showVideoIcons();
            }
        });
        videoBar.addView(videoMode);
        videoBar.addView(thumbnail);
    }

    public void showVideoIcons()
    {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        videoBar.removeView(substitute);
        videoBar.removeView(switchCamera);
        videoBar.removeView(capturePic);
        videoBar.removeView(videoMode);
        videoBar.removeView(thumbnail);
        videoBar.addView(substitute);
        videoBar.addView(switchCamera);
        layoutParams.height=(int)getResources().getDimension(R.dimen.stopButtonHeight);
        layoutParams.width=(int)getResources().getDimension(R.dimen.stopButtonWidth);
        layoutParams.setMargins((int)getResources().getDimension(R.dimen.stopBtnLeftMargin),0,(int)getResources().getDimension(R.dimen.stopBtnRightMargin),0);
        startRecord.setScaleType(ImageView.ScaleType.CENTER_CROP);
        startRecord.setLayoutParams(layoutParams);
        videoBar.addView(startRecord);
        layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins((int)getResources().getDimension(R.dimen.photoModeBtnLeftMargin),0,(int)getResources().getDimension(R.dimen.photoModeBtnRightMargin),0);
        photoMode.setLayoutParams(layoutParams);
        videoBar.addView(photoMode);
        layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins((int)getResources().getDimension(R.dimen.thumbBtnLeftMargin),0,(int)getResources().getDimension(R.dimen.thumbBtnRightMargin),0);
        thumbnail.setLayoutParams(layoutParams);
        videoBar.addView(thumbnail);
    }

    public void addStopAndPauseIcons()
    {
        videoBar.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);

        pauseRecord=new ImageButton(getContext());
        pauseRecord.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        pauseRecord.setImageDrawable(getResources().getDrawable(R.drawable.record_pause));
        pauseRecord.setPadding(0, 0, (int) getResources().getDimension(R.dimen.pauseBtnRightPadding), 0);
        pauseRecord.setOnClickListener(pauseListener);

        stopRecord = new ImageButton(getContext());
        stopRecord.setScaleType(ImageView.ScaleType.CENTER_CROP);
        stopRecord.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        stopRecord.setImageDrawable(getResources().getDrawable(R.drawable.record_stop));
        cameraView.setStopButton(stopRecord);

        layoutParams.height=(int)getResources().getDimension(R.dimen.stopButtonHeight);
        layoutParams.width=(int)getResources().getDimension(R.dimen.stopButtonWidth);
        layoutParams.setMargins((int)getResources().getDimension(R.dimen.stopBtnLeftMargin),0,(int)getResources().getDimension(R.dimen.stopBtnRightMargin),0);
        stopRecord.setLayoutParams(layoutParams);
        stopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraView.record();
                showRecordAndThumbnail();
            }
        });
        switchCamera.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        videoBar.addView(switchCamera);
        videoBar.addView(stopRecord);
        videoBar.addView(pauseRecord);
        if(!isNougatAndAbove()) {
            pauseRecord.setVisibility(View.INVISIBLE);
        }
    }

    public void showRecordAndThumbnail()
    {
        videoBar.setBackgroundColor(getResources().getColor(R.color.settingsBarColor));
        videoBar.removeView(stopRecord);
        videoBar.removeView(pauseRecord);
        videoBar.removeView(switchCamera);
        videoBar.addView(substitute);
        videoBar.addView(switchCamera);
        videoBar.addView(startRecord);
        videoBar.addView(photoMode);
        videoBar.addView(thumbnail);
        //thumbnail.setClickable(false);
        settingsBar.removeView(timeElapsed);
        settingsBar.removeView(memoryConsumed);
        settingsBar.removeView(flash);
        flash = new ImageButton(getActivity());
        if(cameraView.isCameraReady()) {
            if (cameraView.isFlashOn()) {
                flash.setImageDrawable(getResources().getDrawable(R.drawable.flash_off));
            } else {
                flash.setImageDrawable(getResources().getDrawable(R.drawable.flash_on));
            }
        }
        LinearLayout.LayoutParams flashParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flashParams.weight=0.5f;
        flashParams.height = (int)getResources().getDimension(R.dimen.flashOnHeight);
        flashParams.width = (int)getResources().getDimension(R.dimen.flashOnWidth);
        flashParams.setMargins((int)getResources().getDimension(R.dimen.flashOnLeftMargin),0,0,0);
        flashParams.gravity=Gravity.CENTER;
        flash.setLayoutParams(flashParams);
        flash.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                setFlash();
            }
        });
        cameraView.setFlashButton(flash);
        settingsBar.addView(flash);
        settingsBar.addView(settings);
        settingsBar.setBackgroundColor(getResources().getColor(R.color.settingsBarColor));
        flash.setBackgroundColor(getResources().getColor(R.color.settingsBarColor));
    }

    public void hideSettingsBarAndIcon()
    {
        settingsBar.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        settingsBar.removeView(settings);
        settingsBar.removeView(flash);
        flash = new ImageButton(getActivity());
        if(cameraView.isFlashOn()) {
            flash.setImageDrawable(getResources().getDrawable(R.drawable.flash_off));
        }
        else{
            flash.setImageDrawable(getResources().getDrawable(R.drawable.flash_on));
        }
        flash.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                setFlash();
            }
        });
        LinearLayout.LayoutParams flashParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flashParam.weight=0.3f;
        flash.setLayoutParams(flashParam);
        flash.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        settingsBar.addView(flash);
        cameraView.setFlashButton(flash);

        //Add time elapsed text
        timeElapsed = new TextView(getActivity());
        timeElapsed.setGravity(Gravity.CENTER_HORIZONTAL);
        timeElapsed.setTypeface(Typeface.DEFAULT_BOLD);
        timeElapsed.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        timeElapsed.setTextColor(getResources().getColor(R.color.timeElapsed));
        timeElapsed.setText(getResources().getString(R.string.START_TIME));
        LinearLayout.LayoutParams timeElapParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        timeElapParam.setMargins(0,(int)getResources().getDimension(R.dimen.timeAndMemTopMargin),0,0);
        timeElapParam.weight=0.3f;
        timeElapsed.setLayoutParams(timeElapParam);
        settingsBar.addView(timeElapsed);
        cameraView.setTimeElapsedText(timeElapsed);

        //Add memory consumed text
        memoryConsumed = new TextView(getActivity());
        memoryConsumed.setGravity(Gravity.CENTER_HORIZONTAL);
        memoryConsumed.setTextColor(getResources().getColor(R.color.memoryConsumed));
        memoryConsumed.setTypeface(Typeface.DEFAULT_BOLD);
        memoryConsumed.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        memoryConsumed.setText(getResources().getString(R.string.START_MEMORY));
        LinearLayout.LayoutParams memConsumed = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        memConsumed.setMargins(0,(int)getResources().getDimension(R.dimen.timeAndMemTopMargin),0,0);
        memConsumed.weight=0.3f;
        memoryConsumed.setLayoutParams(memConsumed);
        settingsBar.addView(memoryConsumed);
        cameraView.setMemoryConsumedText(memoryConsumed);
    }

    boolean flashOn=false;
    private void setFlash()
    {
        if(!flashOn)
        {
            Log.d(TAG,"Flash on");
            if(cameraView.isFlashModeSupported(Camera.Parameters.FLASH_MODE_TORCH)) {
                flashOn = true;
                flash.setImageDrawable(getResources().getDrawable(R.drawable.flash_off));
            }
            else{
                Toast.makeText(getContext(),"Flash Mode " + Camera.Parameters.FLASH_MODE_TORCH + " not supported by this camera.",Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            Log.d(TAG,"Flash off");
            flashOn=false;
            flash.setImageDrawable(getResources().getDrawable(R.drawable.flash_on));
        }
        cameraView.flashOnOff(flashOn);
    }

    public void askForPermissionAgain()
    {
        Log.d(TAG,"permissionInterface = "+permissionInterface);
        permissionInterface.askPermission();
    }

    public void createAndShowThumbnail(String mediaPath)
    {
        //Storing in public folder. This will ensure that the files are visible in other apps as well.
        //Use this for sharing files between apps
        final File video = new File(mediaPath);
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(mediaPath);
        Bitmap firstFrame = mediaMetadataRetriever.getFrameAtTime(Constants.FIRST_SEC_MICRO);
        if(firstFrame == null){
            Log.d(TAG,"NOT A VALID video file");
            if(video != null && video.delete()){
                Log.d(TAG,"Removed file = "+mediaPath);
            }
            return;
        }
        if(video.getName() != null){
            Log.d(TAG,"Video file name = "+video.getName()+", path = "+video.getPath());
            Log.d(TAG,"width = "+firstFrame.getWidth()+" , height = "+firstFrame.getHeight());
            firstFrame = Bitmap.createScaledBitmap(firstFrame,(int)getResources().getDimension(R.dimen.thumbnailWidth),
                    (int)getResources().getDimension(R.dimen.thumbnailHeight),false);
            thumbnail.setImageBitmap(firstFrame);
            thumbnail.setClickable(true);
            thumbnail.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view)
                {
                    openMedia(video.getPath());
                }
            });
        }
    }

    public void getLatestFileIfExists()
    {
        File dcimFc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM+getResources().getString(R.string.FC_VIDEO));
        if(dcimFc.exists() && dcimFc.isDirectory() && dcimFc.listFiles().length > 0)
        {
            File[] videos = dcimFc.listFiles();
            Arrays.sort(videos);
            Log.d(TAG,"Latest file is = "+videos[videos.length-1].getPath());
            final String filePath = videos[videos.length-1].getPath();
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(filePath);
            Bitmap vid = mediaMetadataRetriever.getFrameAtTime(Constants.FIRST_SEC_MICRO);
            //If video cannot be played for whatever reason
            if(vid != null) {
                vid = Bitmap.createScaledBitmap(vid,(int)getResources().getDimension(R.dimen.thumbnailWidth),
                        (int)getResources().getDimension(R.dimen.thumbnailHeight),false);
                thumbnail.setImageBitmap(vid);
                thumbnail.setClickable(true);
                thumbnail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openMedia(filePath);
                    }
                });
            }
            else{
                if(videos.length >= 2) {
                    for (int i = videos.length - 2; i >= 0; i--) {
                        vid = mediaMetadataRetriever.getFrameAtTime(Constants.FIRST_SEC_MICRO);
                        //If video cannot be played for whatever reason
                        if (vid != null) {
                            vid = Bitmap.createScaledBitmap(vid,(int)getResources().getDimension(R.dimen.thumbnailWidth),
                                    (int)getResources().getDimension(R.dimen.thumbnailHeight),false);
                            thumbnail.setImageBitmap(vid);
                            thumbnail.setClickable(true);
                            thumbnail.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    openMedia(filePath);
                                }
                            });
                            break;
                        }
                    }
                }
                else {
                    thumbnail.setImageDrawable(getResources().getDrawable(R.drawable.placeholder));
                    thumbnail.setClickable(false);
                }
            }
        }
        else
        {
            thumbnail.setImageDrawable(getResources().getDrawable(R.drawable.placeholder));
            thumbnail.setClickable(false);
        }
    }

    private void fetchMedia(ImageView thumbnail)
    {
        String removableStoragePath;
        Log.d(TAG,"storage state = "+Environment.getExternalStorageState());
        File fileList[] = new File("/storage/").listFiles();
        //To find location of SD Card, if it exists
        for (File file : fileList)
        {
            if(!file.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()) && file.isDirectory() && file.canRead()) {
                removableStoragePath = file.getAbsolutePath();
                Log.d(TAG,removableStoragePath);
                File newDir = new File(removableStoragePath+"/FC_Media");
                if(!newDir.exists()){
                    newDir.mkdir();
                }
                /*for(File file1 : new File(removableStoragePath).listFiles())
                {
                    Log.d(TAG,"SD Card path = "+file1.getPath());
                }*/
            }
        }
    }

    private void openMedia(String path)
    {
        setCameraClose();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://"+path),"video/*");
        startActivity(intent);
    }

    private void setCameraClose()
    {
        //Set this if you want to continue when the launcher activity resumes.
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(FC_SHARED_PREFERENCE, Context.MODE_PRIVATE).edit();
        editor.putBoolean("startCamera",false);
        editor.commit();
    }

    private void setCameraQuit()
    {
        //Set this if you want to quit the app when launcher activity resumes.
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(FC_SHARED_PREFERENCE, Context.MODE_PRIVATE).edit();
        editor.putBoolean("startCamera",true);
        editor.commit();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG,"Detached");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"Fragment destroy...app is being minimized");
        setCameraClose();
        super.onDestroy();
    }

    @Override
    public void onStop() {
        Log.d(TAG,"Fragment stop...app is out of focus");
        super.onStop();
    }

    @Override
    public void onPause() {
        Log.d(TAG,"Fragment pause....app is being quit");
        setCameraQuit();
        super.onPause();
    }
}
