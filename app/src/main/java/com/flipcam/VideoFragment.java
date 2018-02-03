package com.flipcam;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.flipcam.constants.Constants;
import com.flipcam.media.FileMedia;
import com.flipcam.service.DropboxUploadService;
import com.flipcam.service.GoogleDriveUploadService;
import com.flipcam.util.MediaUtil;
import com.flipcam.view.CameraView;

import java.io.File;

import static android.widget.Toast.makeText;
import static com.facebook.FacebookSdk.getApplicationContext;
import static com.flipcam.PermissionActivity.FC_SHARED_PREFERENCE;
import static com.flipcam.R.id.modeInfo;


public class VideoFragment extends android.app.Fragment{

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
    SwitchInterface switchInterface;
    ImageButton stopRecord;
    ImageView imagePreview;
    TextView modeText;
    LinearLayout modeLayout;
    TextView mode;
    OrientationEventListener orientationEventListener;
    int orientation = -1;

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

    public interface SwitchInterface{
        void switchToPhoto();
    }

    public interface StartUploadService{
        void startAutoUpload(String filename);
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
        modeText = (TextView)getActivity().findViewById(modeInfo);
        modeLayout = (LinearLayout)getActivity().findViewById(R.id.modeLayout);
        mode = (TextView)getActivity().findViewById(R.id.mode);
        permissionInterface = (PermissionInterface)getActivity();
        switchInterface = (SwitchInterface)getActivity();
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
        Log.d(TAG,"cameraview onresume visibility= "+cameraView.getWindowVisibility());
        zoombar = (SeekBar)view.findViewById(R.id.zoomBar);
        zoombar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.progressFill)));
        cameraView.setSeekBar(zoombar);
        zoombar.setProgress(0);
        zoombar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Log.d(TAG, "progress = " + progress);
                if(cameraView.isCameraReady()) {
                    if (cameraView.isSmoothZoomSupported()) {
                        //Log.d(TAG, "Smooth zoom supported");
                        cameraView.smoothZoomInOrOut(progress);
                    } else if (cameraView.isZoomSupported()) {
                        cameraView.zoomInAndOut(progress);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(!cameraView.isSmoothZoomSupported() && !cameraView.isZoomSupported()) {
                    makeText(getActivity().getApplicationContext(), "Zoom not supported for this camera.", Toast.LENGTH_SHORT).show();
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
                switchInterface.switchToPhoto();
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
                videoBar.removeAllViews();
                addStopAndPauseIcons();
                hideSettingsBarAndIcon();
                SharedPreferences.Editor editor = getActivity().getSharedPreferences(FC_SHARED_PREFERENCE, Context.MODE_PRIVATE).edit();
                editor.putBoolean("videoCapture",true);
                editor.commit();
                cameraView.record();
            }
        });
        Log.d(TAG,"passing videofragment to cameraview");
        cameraView.setFragmentInstance(this);
        cameraView.setPhotoFragmentInstance(null);
        imagePreview = (ImageView)view.findViewById(R.id.imagePreview);
        orientationEventListener = new OrientationEventListener(getActivity().getApplicationContext(), SensorManager.SENSOR_DELAY_UI){
            @Override
            public void onOrientationChanged(int i) {
                if(orientationEventListener.canDetectOrientation()) {
                    orientation = i;
                    determineOrientation();
                    rotateIcons();
                }
            }
        };
        return view;
    }

    public void rotateIcons()
    {
        switchCamera.setRotation(rotationAngle);
        photoMode.setRotation(rotationAngle);
        flash.setRotation(rotationAngle);
        thumbnail.setRotation(rotationAngle);
    }

    public void addStopAndPauseIcons()
    {
        videoBar.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);

        stopRecord = new ImageButton(getActivity().getApplicationContext());
        stopRecord.setScaleType(ImageView.ScaleType.CENTER_CROP);
        stopRecord.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        stopRecord.setImageDrawable(getResources().getDrawable(R.drawable.camera_record_stop));
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
                showRecordSaved();
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
                if(sharedPreferences.getBoolean(Constants.SAVE_TO_GOOGLE_DRIVE, false)) {
                    Log.d(TAG, "Auto uploading to Google Drive");
                    //Auto upload to Google Drive enabled.
                    Intent googleDriveUploadIntent = new Intent(getApplicationContext(), GoogleDriveUploadService.class);
                    googleDriveUploadIntent.putExtra("uploadFile", cameraView.getMediaPath());
                    Log.d(TAG, "Uploading file = "+cameraView.getMediaPath());
                    getActivity().startService(googleDriveUploadIntent);
                }
                if(sharedPreferences.getBoolean(Constants.SAVE_TO_DROPBOX, false)){
                    Log.d(TAG, "Auto upload to Dropbox");
                    //Auto upload to Dropbox enabled
                    Intent dropboxUploadIntent = new Intent(getApplicationContext(), DropboxUploadService.class);
                    dropboxUploadIntent.putExtra("uploadFile", cameraView.getMediaPath());
                    Log.d(TAG, "Uploading file = "+cameraView.getMediaPath());
                    getActivity().startService(dropboxUploadIntent);
                }
            }
        });
        switchCamera.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        switchCamera.setRotation(rotationAngle);
        ImageView recordSubstitute = new ImageView(getActivity());
        recordSubstitute.setImageDrawable(getResources().getDrawable(R.drawable.placeholder));
        layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0,0,(int)getResources().getDimension(R.dimen.recordSubsBtnRightMargin),0);
        recordSubstitute.setLayoutParams(layoutParams);
        recordSubstitute.setVisibility(View.INVISIBLE);
        videoBar.addView(switchCamera);
        videoBar.addView(stopRecord);
        videoBar.addView(recordSubstitute);
    }

    float rotationAngle = 0f;
    public void determineOrientation()
    {
        if(orientation != -1) {
            if (((orientation >= 315 && orientation <= 360) || (orientation >= 0 && orientation <= 45)) || (orientation >= 135 && orientation <= 195)) {
                if (orientation >= 135 && orientation <= 195) {
                    //Reverse portrait
                    rotationAngle = 180f;
                } else {
                    //Portrait
                    rotationAngle = 0f;
                }
            } else {
                if (orientation >= 46 && orientation <= 134) {
                    //Reverse Landscape
                    rotationAngle = 270f;
                } else {
                    //Landscape
                    rotationAngle = 90f;
                }
            }
        }
    }

    public void showRecordSaved()
    {
        LinearLayout recordSavedLayout = new LinearLayout(getActivity());
        recordSavedLayout.setGravity(Gravity.CENTER);
        recordSavedLayout.setOrientation(LinearLayout.VERTICAL);
        recordSavedLayout.setBackgroundColor(getResources().getColor(R.color.savedMsg));
        determineOrientation();
        recordSavedLayout.setRotation(rotationAngle);
        TextView recordSavedText = new TextView(getActivity());
        recordSavedText.setText(getResources().getString(R.string.RECORD_SAVED));
        ImageView recordSavedImg = new ImageView(getActivity());
        recordSavedImg.setImageDrawable(getResources().getDrawable(R.drawable.ic_done_white));
        recordSavedText.setPadding((int)getResources().getDimension(R.dimen.recordSavePadding),(int)getResources().getDimension(R.dimen.recordSavePadding),
                (int)getResources().getDimension(R.dimen.recordSavePadding),(int)getResources().getDimension(R.dimen.recordSavePadding));
        recordSavedText.setTextColor(getResources().getColor(R.color.saveText));
        recordSavedImg.setPadding(0,0,0,(int)getResources().getDimension(R.dimen.recordSaveImagePaddingBottom));
        recordSavedLayout.addView(recordSavedText);
        recordSavedLayout.addView(recordSavedImg);
        final Toast showCompleted = Toast.makeText(getActivity().getApplicationContext(),"",Toast.LENGTH_SHORT);
        showCompleted.setGravity(Gravity.CENTER,0,0);
        showCompleted.setView(recordSavedLayout);
        showCompleted.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                    showCompleted.cancel();
                }catch (InterruptedException ie){
                    ie.printStackTrace();
                }
            }
        }).start();
    }

    public void showRecordAndThumbnail()
    {
        videoBar.setBackgroundColor(getResources().getColor(R.color.settingsBarColor));
        videoBar.removeAllViews();
        videoBar.addView(substitute);
        videoBar.addView(switchCamera);
        videoBar.addView(startRecord);
        videoBar.addView(photoMode);
        videoBar.addView(thumbnail);
        settingsBar.removeAllViews();
        if(cameraView.isCameraReady()) {
            if (cameraView.isFlashOn()) {
                flash.setImageDrawable(getResources().getDrawable(R.drawable.camera_flash_off));
            } else {
                flash.setImageDrawable(getResources().getDrawable(R.drawable.camera_flash_on));
            }
        }
        LinearLayout.LayoutParams flashParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flashParams.weight=0.5f;
        flashParams.height = (int)getResources().getDimension(R.dimen.flashOnHeight);
        flashParams.width = (int)getResources().getDimension(R.dimen.flashOnWidth);
        flashParams.setMargins((int)getResources().getDimension(R.dimen.flashOnLeftMargin),0,0,0);
        flashParams.gravity=Gravity.CENTER;
        flash.setScaleType(ImageView.ScaleType.FIT_CENTER);
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
        settingsBar.addView(modeLayout);
        settingsBar.addView(settings);
        modeText.setText(getResources().getString(R.string.VIDEO_MODE));
        settingsBar.setBackgroundColor(getResources().getColor(R.color.settingsBarColor));
        flash.setBackgroundColor(getResources().getColor(R.color.settingsBarColor));
    }

    public void hideSettingsBarAndIcon()
    {
        settingsBar.setBackgroundColor(getResources().getColor(R.color.transparentBar));
        settingsBar.removeView(settings);
        settingsBar.removeView(modeLayout);
        if(cameraView.isFlashOn()) {
            flash.setImageDrawable(getResources().getDrawable(R.drawable.camera_flash_off));
        }
        else{
            flash.setImageDrawable(getResources().getDrawable(R.drawable.camera_flash_on));
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
        flashParam.setMargins(0,(int)getResources().getDimension(R.dimen.flashOnTopMargin),0,0);
        flashParam.width = (int)getResources().getDimension(R.dimen.flashOnWidth);
        flashParam.height = (int)getResources().getDimension(R.dimen.flashOnHeight);
        flash.setScaleType(ImageView.ScaleType.FIT_CENTER);
        flash.setLayoutParams(flashParam);
        flash.setBackgroundColor(getResources().getColor(R.color.transparentBar));
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
                flash.setImageDrawable(getResources().getDrawable(R.drawable.camera_flash_off));
            }
            else{
                makeText(getActivity().getApplicationContext(),"Flash Mode " + Camera.Parameters.FLASH_MODE_TORCH + " not supported by this camera.",Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            Log.d(TAG,"Flash off");
            flashOn = false;
            flash.setImageDrawable(getResources().getDrawable(R.drawable.camera_flash_on));
        }
        cameraView.flashOnOff(flashOn);
    }

    public boolean isFlashOn()
    {
        return flashOn;
    }

    public void setFlashOn(boolean flashOn1)
    {
        flashOn = flashOn1;
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
        Log.d(TAG,"width = "+firstFrame.getWidth()+" , height = "+firstFrame.getHeight());
        boolean isDetached=false;
        try {
            firstFrame = Bitmap.createScaledBitmap(firstFrame, (int) getResources().getDimension(R.dimen.thumbnailWidth),
                    (int) getResources().getDimension(R.dimen.thumbnailHeight), false);
        }
        catch (IllegalStateException illegal){
            Log.d(TAG,"video fragment is already detached. ");
            isDetached=true;
        }
        if(!isDetached) {
            thumbnail.setImageBitmap(firstFrame);
            thumbnail.setClickable(true);
            thumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openMedia();
                }
            });
        }
    }

    public boolean isImage(String path)
    {
        if(path.endsWith(getResources().getString(R.string.IMG_EXT)) || path.endsWith(getResources().getString(R.string.ANOTHER_IMG_EXT))){
            return true;
        }
        return false;
    }

    public void getLatestFileIfExists()
    {
        FileMedia[] medias = MediaUtil.getMediaList(getActivity().getApplicationContext());
        if (medias != null && medias.length > 0) {
            Log.d(TAG, "Latest file is = " + medias[0].getPath());
            final String filePath = medias[0].getPath();
            if (!isImage(filePath)) {
                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(filePath);
                Bitmap vid = mediaMetadataRetriever.getFrameAtTime(Constants.FIRST_SEC_MICRO);
                //If video cannot be played for whatever reason
                if (vid != null) {
                    vid = Bitmap.createScaledBitmap(vid, (int) getResources().getDimension(R.dimen.thumbnailWidth),
                            (int) getResources().getDimension(R.dimen.thumbnailHeight), false);
                    thumbnail.setImageBitmap(vid);
                    thumbnail.setClickable(true);
                    thumbnail.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            openMedia();
                        }
                    });
                } else {
                    setPlaceholderThumbnail();
                }
            } else {
                Bitmap pic = BitmapFactory.decodeFile(filePath);
                pic = Bitmap.createScaledBitmap(pic, (int) getResources().getDimension(R.dimen.thumbnailWidth),
                        (int) getResources().getDimension(R.dimen.thumbnailHeight), false);
                thumbnail.setImageBitmap(pic);
                thumbnail.setClickable(true);
                thumbnail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openMedia();
                    }
                });
            }
        }
        else{
            setPlaceholderThumbnail();
        }
    }

    public void setPlaceholderThumbnail()
    {
        thumbnail.setImageDrawable(getResources().getDrawable(R.drawable.placeholder));
        thumbnail.setClickable(false);
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

    private void openMedia()
    {
        setCameraClose();
        Intent mediaIntent = new Intent(getActivity().getApplicationContext(), MediaActivity.class);
        startActivity(mediaIntent);
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
        if(cameraView!=null){
            cameraView.setVisibility(View.VISIBLE);
        }
        orientationEventListener.enable();
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
        if(cameraView!=null){
            Log.d(TAG,"cameraview onpause visibility= "+cameraView.getWindowVisibility());
            if(cameraView.getWindowVisibility() == View.VISIBLE){
                cameraView.setVisibility(View.GONE);
            }
        }
        orientationEventListener.disable();
        super.onPause();
    }
}
