package com.flipcam;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
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

import static com.facebook.FacebookSdk.getApplicationContext;
import static com.flipcam.PermissionActivity.FC_SHARED_PREFERENCE;

/**
 * Created by koushick on 02-Oct-17.
 */

public class PhotoFragment extends Fragment {

    public static final String TAG="PhotoFragment";
    SeekBar zoombar;
    CameraView cameraView;
    ImageButton switchCamera;
    ImageButton flash;
    ImageButton videoMode;
    ImageView substitute;
    ImageView thumbnail;
    ImageButton settings;
    LinearLayout photoBar;
    LinearLayout settingsBar;
    PhotoPermission photoPermission;
    SwitchPhoto switchPhoto;
    ImageButton capturePic;
    ImageView imagePreview;
    TextView modeText;
    boolean continuousAF = true;
    OrientationEventListener orientationEventListener;
    int orientation = -1;

    public interface PhotoPermission{
        void askPhotoPermission();
    }
    public interface SwitchPhoto{
        void switchToVideo();
    }
    public PhotoFragment() {
        //Required empty public constructor
    }

    public static PhotoFragment newInstance(){
        PhotoFragment photoFragment = new PhotoFragment();
        return photoFragment;
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
        modeText = (TextView)getActivity().findViewById(R.id.modeInfo);
        modeText.setText(getResources().getString(R.string.PHOTO_MODE));
        photoPermission = (PhotoFragment.PhotoPermission)getActivity();
        switchPhoto = (PhotoFragment.SwitchPhoto)getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_photo, container, false);

        Log.d(TAG,"Inside photo fragment");
        substitute = (ImageView)view.findViewById(R.id.photoSubstitute);
        substitute.setVisibility(View.INVISIBLE);
        cameraView = (CameraView)view.findViewById(R.id.photocameraSurfaceView);
        zoombar = (SeekBar)view.findViewById(R.id.photoZoomBar);
        zoombar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.progressFill)));
        cameraView.setSeekBar(zoombar);
        zoombar.setProgress(0);
        zoombar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Log.d(TAG, "progress = " + progress);
                if(!isContinuousAF()) {
                    if (progress > 0) {
                        cameraView.unregisterAccelSensor();
                    } else if (progress == 0) {
                        cameraView.registerAccelSensor();
                    }
                }
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
                    Toast.makeText(getActivity().getApplicationContext(), "Zoom not supported for this camera.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        thumbnail = (ImageView)view.findViewById(R.id.photoThumbnail);
        videoMode = (ImageButton) view.findViewById(R.id.videoMode);
        videoMode.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view1){
                switchPhoto.switchToVideo();
            }
        });
        capturePic = (ImageButton)view.findViewById(R.id.cameraCapture);
        capturePic.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                showImagePreview();
            }
        });
        switchCamera = (ImageButton)view.findViewById(R.id.photoSwitchCamera);
        switchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                capturePic.setClickable(false);
                flash.setClickable(false);
                videoMode.setClickable(false);
                thumbnail.setClickable(false);

                cameraView.switchCamera();

                zoombar.setProgress(0);
                capturePic.setClickable(true);
                flash.setClickable(true);
                videoMode.setClickable(true);
                thumbnail.setClickable(true);
            }
        });

        photoBar = (LinearLayout)view.findViewById(R.id.photoFunctions);
        Log.d(TAG,"passing photofragment to cameraview");
        cameraView.setPhotoFragmentInstance(this);
        cameraView.setFragmentInstance(null);
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

    public void rotateIcons()
    {
        switchCamera.setRotation(rotationAngle);
        videoMode.setRotation(rotationAngle);
        flash.setRotation(rotationAngle);
        thumbnail.setRotation(rotationAngle);
    }

    public void showImageSaved()
    {
        LinearLayout recordSavedLayout = new LinearLayout(getActivity());
        recordSavedLayout.setGravity(Gravity.CENTER);
        determineOrientation();
        recordSavedLayout.setRotation(rotationAngle);
        recordSavedLayout.setOrientation(LinearLayout.VERTICAL);
        recordSavedLayout.setBackgroundColor(getResources().getColor(R.color.savedMsg));
        TextView recordSavedText = new TextView(getActivity());
        recordSavedText.setText(getResources().getString(R.string.IMAGE_SAVED));
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
                    Thread.sleep(700);
                    showCompleted.cancel();
                }catch (InterruptedException ie){
                    ie.printStackTrace();
                }
            }
        }).start();
    }

    public boolean isContinuousAF() {
        return continuousAF;
    }

    public void setContinuousAF(boolean continuousAF) {
        this.continuousAF = continuousAF;
    }

    public SeekBar getZoomBar()
    {
        return zoombar;
    }

    public ImageButton getCapturePic()
    {
        return capturePic;
    }

    public void showImagePreview()
    {
        imagePreview.setImageBitmap(cameraView.getDrawingCache());
        imagePreview.setVisibility(View.VISIBLE);
        cameraView.capturePhoto();
    }

    public void hideImagePreview()
    {
        imagePreview.setVisibility(View.INVISIBLE);
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        if(sharedPreferences.getBoolean(Constants.SAVE_TO_GOOGLE_DRIVE, false)) {
            Log.d(TAG, "Auto uploading to Google Drive");
            //Auto upload to Google Drive enabled
            Intent googleDriveUploadIntent = new Intent(getApplicationContext(), GoogleDriveUploadService.class);
            googleDriveUploadIntent.putExtra("uploadFile", cameraView.getPhotoMediaPath());
            Log.d(TAG, "Uploading file = "+cameraView.getPhotoMediaPath());
            getActivity().startService(googleDriveUploadIntent);
        }
        if(sharedPreferences.getBoolean(Constants.SAVE_TO_DROPBOX, false)){
            Log.d(TAG, "Auto upload to Dropbox");
            //Auto upload to Dropbox enabled
            Intent dropboxUploadIntent = new Intent(getApplicationContext(), DropboxUploadService.class);
            dropboxUploadIntent.putExtra("uploadFile", cameraView.getPhotoMediaPath());
            Log.d(TAG, "Uploading file = "+cameraView.getPhotoMediaPath());
            getActivity().startService(dropboxUploadIntent);
        }
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
                Toast.makeText(getActivity().getApplicationContext(),"Flash Mode " + Camera.Parameters.FLASH_MODE_TORCH + " not supported by this camera.",Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            Log.d(TAG,"Flash off");
            flashOn=false;
            flash.setImageDrawable(getResources().getDrawable(R.drawable.camera_flash_on));
        }
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
        Log.d(TAG,"permissionInterface = "+photoPermission);
        photoPermission.askPhotoPermission();
    }

    public void createAndShowPhotoThumbnail(Bitmap photo)
    {
        Log.d(TAG,"create photo thumbnail");
        Bitmap firstFrame = Bitmap.createScaledBitmap(photo,(int)getResources().getDimension(R.dimen.thumbnailWidth),
                (int)getResources().getDimension(R.dimen.thumbnailHeight),false);
        thumbnail.setImageBitmap(firstFrame);
        thumbnail.setClickable(true);
        thumbnail.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                openMedia(cameraView.getPhotoMediaPath());
            }
        });
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
                            openMedia(filePath);
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
                        openMedia(filePath);
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

    private void openMedia(String path)
    {
        setCameraClose();
        Intent intent = new Intent(getActivity().getApplicationContext(), MediaActivity.class);
        intent.putExtra("mediaPath",path);
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
        orientationEventListener.enable();
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
        orientationEventListener.disable();
        super.onPause();
    }
}
