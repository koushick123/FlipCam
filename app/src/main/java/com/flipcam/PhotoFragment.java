package com.flipcam;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.flipcam.constants.Constants;
import com.flipcam.view.CameraView;

import java.io.File;
import java.util.Arrays;

import static android.support.v4.content.FileProvider.getUriForFile;
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
                if(progress > 0){
                    cameraView.unregisterAccelSensor();
                }
                else if(progress == 0){
                    cameraView.registerAccelSensor();
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
        return view;
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
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        cameraView.capturePhoto();
    }

    public void hideImagePreview()
    {
        imagePreview.setVisibility(View.INVISIBLE);
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
                Toast.makeText(getActivity().getApplicationContext(),"Flash Mode " + Camera.Parameters.FLASH_MODE_TORCH + " not supported by this camera.",Toast.LENGTH_SHORT).show();
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
                openMedia(cameraView.getPhotoMediaPath(),false);
            }
        });
    }

    public void getLatestFileIfExists()
    {
        SharedPreferences prefs = getActivity().getSharedPreferences(FC_SHARED_PREFERENCE, Context.MODE_PRIVATE);
        if(prefs.getBoolean("videoCapture",false))
        {
            File dcimFc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + getResources().getString(R.string.FC_VIDEO));
            if (dcimFc.exists() && dcimFc.isDirectory() && dcimFc.listFiles().length > 0) {
                File[] videos = dcimFc.listFiles();
                Arrays.sort(videos);
                Log.d(TAG, "Latest file is = " + videos[videos.length - 1].getPath());
                final String filePath = videos[videos.length - 1].getPath();
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
                            openMedia(filePath, true);
                        }
                    });
                } else {
                    if (videos.length >= 2) {
                        for (int i = videos.length - 2; i >= 0; i--) {
                            vid = mediaMetadataRetriever.getFrameAtTime(Constants.FIRST_SEC_MICRO);
                            //If video cannot be played for whatever reason
                            if (vid != null) {
                                vid = Bitmap.createScaledBitmap(vid, (int) getResources().getDimension(R.dimen.thumbnailWidth),
                                        (int) getResources().getDimension(R.dimen.thumbnailHeight), false);
                                thumbnail.setImageBitmap(vid);
                                thumbnail.setClickable(true);
                                thumbnail.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        openMedia(filePath, true);
                                    }
                                });
                                break;
                            }
                        }
                    } else {
                        setPlaceholderThumbnail();
                    }
                }
            } else {
                setPlaceholderThumbnail();
            }
        }
        else {
            File dcimFc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + getResources().getString(R.string.FC_PICTURE));
            if (dcimFc.exists() && dcimFc.isDirectory() && dcimFc.listFiles().length > 0) {
                File[] pictures = dcimFc.listFiles();
                Arrays.sort(pictures);
                Log.d(TAG, "Latest file is = " + pictures[pictures.length - 1].getPath());
                final String filePath = pictures[pictures.length - 1].getPath();
                Bitmap pic = BitmapFactory.decodeFile(filePath);
                pic = Bitmap.createScaledBitmap(pic, (int) getResources().getDimension(R.dimen.thumbnailWidth),
                        (int) getResources().getDimension(R.dimen.thumbnailHeight), false);
                thumbnail.setImageBitmap(pic);
                thumbnail.setClickable(true);
                thumbnail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openMedia(filePath, false);
                    }
                });
            }
            else{
                setPlaceholderThumbnail();
            }
        }
    }

    public void setPlaceholderThumbnail()
    {
        thumbnail.setImageDrawable(getResources().getDrawable(R.drawable.placeholder));
        thumbnail.setClickable(false);
    }

    private void openMedia(String path,boolean videoType)
    {
        setCameraClose();
        Intent intent = new Intent(getActivity().getApplicationContext(), MediaActivity.class);
        intent.putExtra("mediaPath",path);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            File media = Environment.getExternalStorageDirectory();
            if(!videoType) {
                media = new File(media.getPath()+getResources().getString(R.string.FC_PICTURE));
            }
            else{
                media = new File(media.getPath()+getResources().getString(R.string.FC_VIDEO));
            }
            Log.d(TAG,"media path = "+media.getPath());
            String fileName = path.substring(path.lastIndexOf("/")+1,path.length());
            Log.d(TAG,"File name = "+fileName);
            File newFile = new File(media, fileName);
            Uri contentUri = getUriForFile(getContext(), "com.flipcam.fileprovider", newFile);
            Log.d(TAG,"content uri = "+contentUri.getPath());
            getContext().grantUriPermission("com.flipcam.fileprovider",contentUri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setData(contentUri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
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
