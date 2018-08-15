package com.flipcam;

import android.app.Dialog;
import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.flipcam.constants.Constants;
import com.flipcam.data.MediaTableConstants;
import com.flipcam.media.FileMedia;
import com.flipcam.service.DropboxUploadService;
import com.flipcam.service.GoogleDriveUploadService;
import com.flipcam.util.MediaUtil;
import com.flipcam.view.CameraView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

import static com.facebook.FacebookSdk.getApplicationContext;

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
    LowestThresholdCheckForPictureInterface lowestThresholdCheckForPictureInterface;
    SwitchPhoto switchPhoto;
    ImageButton capturePic;
    ImageView imagePreview;
    ImageView imageHighlight;
    TextView modeText;
    TextView resInfo;
    boolean continuousAF = true;
    OrientationEventListener orientationEventListener;
    int orientation = -1;
    ExifInterface exifInterface=null;
    View warningMsgRoot;
    Dialog warningMsg;
    LayoutInflater layoutInflater;
    SDCardEventReceiver sdCardEventReceiver;
    IntentFilter mediaFilters;
    Button okButton;
    SharedPreferences sharedPreferences;
    boolean sdCardUnavailWarned = false;
    FrameLayout thumbnailParent;
    FrameLayout photoCameraView;
    ImageView microThumbnail;
    AppWidgetManager appWidgetManager;
    boolean VERBOSE = true;

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

    public interface LowestThresholdCheckForPictureInterface{
        boolean checkIfPhoneMemoryIsBelowLowestThresholdForPicture();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(VERBOSE)Log.d(TAG,"onActivityCreated");
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
        resInfo = (TextView)getActivity().findViewById(R.id.resInfo);
        imageHighlight = (ImageView)getActivity().findViewById(R.id.imageHighlight);
        modeText.setText(getResources().getString(R.string.PHOTO_MODE));
        photoPermission = (PhotoFragment.PhotoPermission)getActivity();
        switchPhoto = (PhotoFragment.SwitchPhoto)getActivity();
        lowestThresholdCheckForPictureInterface = (LowestThresholdCheckForPictureInterface)getActivity();
        layoutInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        warningMsgRoot = layoutInflater.inflate(R.layout.warning_message, null);
        warningMsg = new Dialog(getActivity());
        mediaFilters = new IntentFilter();
        sdCardEventReceiver = new SDCardEventReceiver();
        sharedPreferences = getActivity().getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        appWidgetManager = (AppWidgetManager)getActivity().getSystemService(Context.APPWIDGET_SERVICE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_photo, container, false);

        if(VERBOSE)Log.d(TAG,"Inside photo fragment");
        substitute = (ImageView)view.findViewById(R.id.photoSubstitute);
        substitute.setVisibility(View.INVISIBLE);
        cameraView = (CameraView)view.findViewById(R.id.photocameraSurfaceView);
        zoombar = (SeekBar)view.findViewById(R.id.photoZoomBar);
        imagePreview = (ImageView)view.findViewById(R.id.imagePreview);
        zoombar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.progressFill)));
        cameraView.setSeekBar(zoombar);
        zoombar.setProgress(0);
        zoombar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //if(VERBOSE)Log.d(TAG, "progress = " + progress);
                if(!isContinuousAF()) {
                    if (progress > 0) {
                        cameraView.unregisterAccelSensor();
                    } else if (progress == 0) {
                        cameraView.registerAccelSensor();
                    }
                }
                if(cameraView.isCameraReady()) {
                    if (cameraView.isSmoothZoomSupported()) {
                        //if(VERBOSE)Log.d(TAG, "Smooth zoom supported");
                        cameraView.smoothZoomInOrOut(progress);
                    } else if (cameraView.isZoomSupported()) {
                        cameraView.zoomInAndOut(progress);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(!cameraView.isSmoothZoomSupported() && !cameraView.isZoomSupported()) {
                    Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.zoomNotSupported), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        thumbnail = (ImageView)view.findViewById(R.id.photoThumbnail);
        microThumbnail = (ImageView)view.findViewById(R.id.microThumbnail);
        thumbnailParent = (FrameLayout)view.findViewById(R.id.thumbnailParent);
        photoCameraView = (FrameLayout)view.findViewById(R.id.photoCameraView);
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
                capturePic.setClickable(false);
                videoMode.setClickable(false);
                switchCamera.setClickable(false);
                thumbnail.setClickable(false);
                if(lowestThresholdCheckForPictureInterface.checkIfPhoneMemoryIsBelowLowestThresholdForPicture()){
                    LayoutInflater layoutInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View thresholdExceededRoot = layoutInflater.inflate(R.layout.threshold_exceeded, null);
                    final Dialog thresholdDialog = new Dialog(getActivity());
                    TextView memoryLimitMsg = (TextView)thresholdExceededRoot.findViewById(R.id.memoryLimitMsg);
                    int lowestThreshold = getResources().getInteger(R.integer.minimumMemoryWarning);
                    StringBuilder minimumThreshold = new StringBuilder(lowestThreshold+"");
                    minimumThreshold.append(" ");
                    minimumThreshold.append(getResources().getString(R.string.MEM_PF_MB));
                    if(VERBOSE)Log.d(TAG, "minimumThreshold = "+minimumThreshold);
                    memoryLimitMsg.setText(getResources().getString(R.string.minimumThresholdExceeded, minimumThreshold));
                    CheckBox disableThreshold = (CheckBox)thresholdExceededRoot.findViewById(R.id.disableThreshold);
                    disableThreshold.setVisibility(View.GONE);
                    Button okButton = (Button)thresholdExceededRoot.findViewById(R.id.okButton);
                    okButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            capturePic.setClickable(true);
                            videoMode.setClickable(true);
                            switchCamera.setClickable(true);
                            thumbnail.setClickable(true);
                            thresholdDialog.dismiss();
                        }
                    });
                    thresholdDialog.setContentView(thresholdExceededRoot);
                    thresholdDialog.setCancelable(false);
                    thresholdDialog.show();
                }
                else {
                    if(!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
                        if(doesSDCardExist() == null && !sdCardUnavailWarned){
                            sdCardUnavailWarned = true;
                            showSDCardUnavailMessage();
                        }
                        else{
                            sdCardUnavailWarned = false;
                            showImagePreview();
                        }
                    }
                    else {
                        showImagePreview();
                    }
                }
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
        if(VERBOSE)Log.d(TAG,"passing photofragment to cameraview");
        cameraView.setPhotoFragmentInstance(this);
        cameraView.setFragmentInstance(null);
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
        WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        display.getSize(size);
        return view;
    }

    Point size = new Point();

    class SDCardEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if(VERBOSE)Log.d(TAG, "onReceive = "+intent.getAction());
            if(intent.getAction().equalsIgnoreCase(Intent.ACTION_MEDIA_UNMOUNTED)){
                //Check if SD Card was selected
                if(!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true) && !sdCardUnavailWarned){
                    sdCardUnavailWarned = true;
                    showSDCardUnavailMessage();
                }
            }
        }
    }

    public void showSDCardUnavailMessage(){
        if(VERBOSE)Log.d(TAG, "SD Card Removed");
        SharedPreferences.Editor settingsEditor = sharedPreferences.edit();
        settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
        settingsEditor.commit();
        TextView warningTitle = (TextView)warningMsgRoot.findViewById(R.id.warningTitle);
        warningTitle.setText(getResources().getString(R.string.sdCardRemovedTitle));
        TextView warningText = (TextView)warningMsgRoot.findViewById(R.id.warningText);
        warningText.setText(getResources().getString(R.string.sdCardNotPresentForRecord));
        okButton = (Button)warningMsgRoot.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                warningMsg.dismiss();
                capturePic.setClickable(true);
                videoMode.setClickable(true);
                switchCamera.setClickable(true);
                thumbnail.setClickable(true);
            }
        });
        warningMsg.setContentView(warningMsgRoot);
        warningMsg.setCancelable(false);
        warningMsg.show();
        updateWidget();
        getLatestFileIfExists();
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
        microThumbnail.setRotation(rotationAngle);
        if(exifInterface!=null && !filePath.equalsIgnoreCase(""))
        {
            if(isImage(filePath)) {
                if(exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase(String.valueOf(ExifInterface.ORIENTATION_ROTATE_90))) {
                    rotationAngle += 90f;
                }
                else if(exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase(String.valueOf(ExifInterface.ORIENTATION_ROTATE_270))) {
                    rotationAngle += 270f;
                }
            }
        }
        thumbnail.setRotation(rotationAngle);
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

    public ImageButton getSwitchCamera() {
        return switchCamera;
    }

    public void setSwitchCamera(ImageButton switchCamera) {
        this.switchCamera = switchCamera;
    }

    public ImageButton getVideoMode() {
        return videoMode;
    }

    public void setVideoMode(ImageButton videoMode) {
        this.videoMode = videoMode;
    }

    public ImageView getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(ImageView thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void showImagePreview()
    {
        //Use imagePreview to show the captured preview even after zoom in.
        imagePreview.setImageBitmap(cameraView.getDrawingCache());
        imagePreview.setVisibility(View.VISIBLE);
        //Use imageHighlight to highlight the borders when a picture is being taken.
        imageHighlight.setImageDrawable(getResources().getDrawable(R.drawable.photo_highlight));
        imageHighlight.setVisibility(View.VISIBLE);
        capturePic.setClickable(false);
        videoMode.setClickable(false);
        switchCamera.setClickable(false);
        thumbnail.setClickable(false);
        cameraView.capturePhoto();
    }

    public void animatePhotoShrink(){
        Log.d(TAG, "animatePhotoShrink");
        int adjustWidth = (int)getResources().getDimension(R.dimen.thumbnailWidth);
        int adjustHeight = (int)getResources().getDimension(R.dimen.thumbnailHeight);
        ScaleAnimation scaleAnim = new ScaleAnimation(1.0f,0.1f, 1.0f, 0.1f, size.x - adjustWidth / 2,
                size.y - adjustHeight / 2);
        scaleAnim.setInterpolator(new LinearInterpolator());
        scaleAnim.setDuration(250);
        imageHighlight.startAnimation(scaleAnim);
        imageHighlight.setVisibility(View.INVISIBLE);
    }

    public void hideImagePreview()
    {
        imagePreview.setVisibility(View.INVISIBLE);
        updateWidget();
        ContentValues mediaContent = new ContentValues();
        mediaContent.put("filename", cameraView.getPhotoMediaPath());
        mediaContent.put("memoryStorage", (sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true) ? "1" : "0"));
        if(VERBOSE)Log.d(TAG, "Adding to Media DB");
        getActivity().getContentResolver().insert(Uri.parse(MediaTableConstants.BASE_CONTENT_URI+"/addMedia"),mediaContent);
        if(sharedPreferences.getBoolean(Constants.SAVE_TO_GOOGLE_DRIVE, false)) {
            if(VERBOSE)Log.d(TAG, "Auto uploading to Google Drive");
            //Auto upload to Google Drive enabled
            Intent googleDriveUploadIntent = new Intent(getApplicationContext(), GoogleDriveUploadService.class);
            googleDriveUploadIntent.putExtra("uploadFile", cameraView.getPhotoMediaPath());
            if(VERBOSE)Log.d(TAG, "Uploading file = "+cameraView.getPhotoMediaPath());
            getActivity().startService(googleDriveUploadIntent);
        }
        if(sharedPreferences.getBoolean(Constants.SAVE_TO_DROPBOX, false)){
            if(VERBOSE)Log.d(TAG, "Auto upload to Dropbox");
            //Auto upload to Dropbox enabled
            Intent dropboxUploadIntent = new Intent(getApplicationContext(), DropboxUploadService.class);
            dropboxUploadIntent.putExtra("uploadFile", cameraView.getPhotoMediaPath());
            if(VERBOSE)Log.d(TAG, "Uploading file = "+cameraView.getPhotoMediaPath());
            getActivity().startService(dropboxUploadIntent);
        }
    }

    public void updateWidget(){
        HashSet<String> widgetIds = (HashSet)sharedPreferences.getStringSet(Constants.WIDGET_IDS, null);
        if(widgetIds != null && widgetIds.size() > 0){
            Iterator<String> iterator = widgetIds.iterator();
            while(iterator.hasNext()){
                String widgetId = iterator.next();
                if(VERBOSE)Log.d(TAG, "widgetIds = "+widgetId);
                updateAppWidget(Integer.parseInt(widgetId));
            }
        }
    }

    public void checkForSDCard(){
        if(VERBOSE)Log.d(TAG, "getActivity = "+getActivity());
        if(!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
            if(doesSDCardExist() == null) {
                showSDCardUnavailMessage();
            }
        }
    }

    public String doesSDCardExist(){
        String sdcardpath = sharedPreferences.getString(Constants.SD_CARD_PATH, "");
        try {
            String filename = "/doesSDCardExist_"+String.valueOf(System.currentTimeMillis()).substring(0,5);
            sdcardpath += filename;
            final String sdCardFilePath = sdcardpath;
            final FileOutputStream createTestFile = new FileOutputStream(sdcardpath);
            if(VERBOSE)Log.d(TAG, "Able to create file... SD Card exists");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    File testfile = new File(sdCardFilePath);
                    try {
                        createTestFile.close();
                        testfile.delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (FileNotFoundException e) {
            if(VERBOSE)Log.d(TAG, "Unable to create file... SD Card NOT exists..... "+e.getMessage());
            return null;
        }
        return sharedPreferences.getString(Constants.SD_CARD_PATH, "");
    }

    boolean flashOn=false;
    private void setFlash()
    {
        if(!flashOn)
        {
            if(VERBOSE)Log.d(TAG,"Flash on");
            if(cameraView.isFlashModeSupported(cameraView.getCameraImplementation().getFlashModeTorch())) {
                flashOn = true;
                flash.setImageDrawable(getResources().getDrawable(R.drawable.camera_flash_off));
            }
            else{
                Toast.makeText(getActivity().getApplicationContext(),"Flash Mode " + cameraView.getCameraImplementation().getFlashModeTorch() + " not supported by this camera.",Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            if(VERBOSE)Log.d(TAG,"Flash off");
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
        if(VERBOSE)Log.d(TAG,"permissionInterface = "+photoPermission);
        photoPermission.askPhotoPermission();
    }

    public void createAndShowPhotoThumbnail(Bitmap photo)
    {
        if(VERBOSE)Log.d(TAG,"create photo thumbnail");
        Bitmap firstFrame = Bitmap.createScaledBitmap(photo,(int)getResources().getDimension(R.dimen.thumbnailWidth),
                (int)getResources().getDimension(R.dimen.thumbnailHeight),false);
        microThumbnail.setVisibility(View.INVISIBLE);
        thumbnail.setImageBitmap(firstFrame);
        thumbnail.setClickable(true);
        thumbnail.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                openMedia();
            }
        });
    }

    public void setPhotoResInfo(String width, String height){
        resInfo.setText(width+" X "+height);
    }

    public boolean isImage(String path)
    {
        if(path.endsWith(getResources().getString(R.string.IMG_EXT)) || path.endsWith(getResources().getString(R.string.ANOTHER_IMG_EXT))){
            return true;
        }
        return false;
    }
    String filePath = "";

    public void deleteFileAndRefreshThumbnail(){
        File badFile = new File(filePath);
        badFile.delete();
        if(VERBOSE)Log.d(TAG, "Bad file removed...."+filePath);
        getLatestFileIfExists();
    }

    public void getLatestFileIfExists()
    {
        FileMedia[] medias = MediaUtil.getMediaList(getActivity().getApplicationContext());
        if (medias != null && medias.length > 0) {
            if(VERBOSE)Log.d(TAG, "Latest file is = " + medias[0].getPath());
            filePath = medias[0].getPath();
            if (!isImage(filePath)) {
                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                try {
                    mediaMetadataRetriever.setDataSource(filePath);
                } catch (RuntimeException runtime){
                    if(VERBOSE)Log.d(TAG, "RuntimeException "+runtime.getMessage());
                    if(!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
                        //Possible bad file in SD Card. Remove it.
                        deleteFileAndRefreshThumbnail();
                        return;
                    }
                }
                Bitmap vid = mediaMetadataRetriever.getFrameAtTime(Constants.FIRST_SEC_MICRO);
                //If video cannot be played for whatever reason
                if (vid != null) {
                    vid = Bitmap.createScaledBitmap(vid, (int) getResources().getDimension(R.dimen.thumbnailWidth),
                            (int) getResources().getDimension(R.dimen.thumbnailHeight), false);
                    thumbnail.setImageBitmap(vid);
                    microThumbnail.setVisibility(View.VISIBLE);
                    thumbnail.setClickable(true);
                    thumbnail.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            openMedia();
                        }
                    });
                } else {
                    //Possible bad file in SD Card. Remove it.
                    deleteFileAndRefreshThumbnail();
                    return;
                }
            } else {
                try {
                    exifInterface = new ExifInterface(filePath);
                    if(VERBOSE)Log.d(TAG, "TAG_ORIENTATION = "+exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION));
                    Bitmap pic = BitmapFactory.decodeFile(filePath);
                    pic = Bitmap.createScaledBitmap(pic, (int) getResources().getDimension(R.dimen.thumbnailWidth),
                            (int) getResources().getDimension(R.dimen.thumbnailHeight), false);
                    thumbnail.setImageBitmap(pic);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                microThumbnail.setVisibility(View.INVISIBLE);
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
            microThumbnail.setVisibility(View.INVISIBLE);
            setPlaceholderThumbnail();
        }
    }

    public void setPlaceholderThumbnail()
    {
        thumbnail.setImageDrawable(getResources().getDrawable(R.drawable.placeholder));
        thumbnail.setClickable(false);
    }

    private void openMedia()
    {
        setCameraClose();
        Intent intent = new Intent(getActivity().getApplicationContext(), MediaActivity.class);
        intent.putExtra("fromGallery",false);
        startActivity(intent);
    }

    private void setCameraClose()
    {
        //Set this if you want to continue when the launcher activity resumes.
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("startCamera",false);
        editor.commit();
    }

    private void setCameraQuit()
    {
        //Set this if you want to quit the app when launcher activity resumes.
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("startCamera",true);
        editor.commit();
    }

    public void updateAppWidget(int appWidgetId) {
        RemoteViews remoteViews = new RemoteViews(getActivity().getPackageName(), R.layout.flipcam_widget);
        FileMedia[] media = MediaUtil.getMediaList(getActivity());
        if (media != null && media.length > 0) {
            String filepath = media[0].getPath();
            if(VERBOSE)Log.d(TAG, "FilePath = " + filepath);
            if (filepath.endsWith(getResources().getString(R.string.IMG_EXT))
                    || filepath.endsWith(getResources().getString(R.string.ANOTHER_IMG_EXT))) {
                Bitmap latestImage = BitmapFactory.decodeFile(filepath);
                latestImage = Bitmap.createScaledBitmap(latestImage, (int) getResources().getDimension(R.dimen.thumbnailWidth),
                        (int) getResources().getDimension(R.dimen.thumbnailHeight), false);
                if(VERBOSE)Log.d(TAG, "Update Photo thumbnail");
                remoteViews.setViewVisibility(R.id.playCircleWidget, View.INVISIBLE);
                remoteViews.setImageViewBitmap(R.id.imageWidget, latestImage);
                remoteViews.setTextViewText(R.id.widgetMsg, getResources().getString(R.string.widgetMediaMsg));
            } else {
                Bitmap vid = null;
                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                try {
                    mediaMetadataRetriever.setDataSource(filepath);
                    vid = mediaMetadataRetriever.getFrameAtTime(Constants.FIRST_SEC_MICRO);
                } catch (RuntimeException runtime) {
                    File badFile = new File(filepath);
                    badFile.delete();
                    media = MediaUtil.getMediaList(getActivity());
                    if (media != null && media.length > 0) {
                        mediaMetadataRetriever.setDataSource(filepath);
                        vid = mediaMetadataRetriever.getFrameAtTime(Constants.FIRST_SEC_MICRO);
                    } else {
                        remoteViews.setImageViewResource(R.id.imageWidget, R.drawable.placeholder);
                        remoteViews.setViewVisibility(R.id.playCircleWidget, View.INVISIBLE);
                        remoteViews.setTextViewText(R.id.widgetMsg, getResources().getString(R.string.widgetNoMedia));
                    }
                }
                if (vid != null) {
                    vid = Bitmap.createScaledBitmap(vid, (int) getResources().getDimension(R.dimen.thumbnailWidth),
                            (int) getResources().getDimension(R.dimen.thumbnailHeight), false);
                    if(VERBOSE)Log.d(TAG, "Update Video thumbnail");
                    remoteViews.setViewVisibility(R.id.playCircleWidget, View.VISIBLE);
                    remoteViews.setImageViewBitmap(R.id.imageWidget, vid);
                    remoteViews.setTextViewText(R.id.widgetMsg, getResources().getString(R.string.widgetMediaMsg));
                }
            }
        } else {
            remoteViews.setImageViewResource(R.id.imageWidget, R.drawable.placeholder);
            remoteViews.setViewVisibility(R.id.playCircleWidget, View.INVISIBLE);
            remoteViews.setTextViewText(R.id.widgetMsg, getResources().getString(R.string.widgetNoMedia));
        }
        if(VERBOSE)Log.d(TAG, "Update FC Widget");
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(VERBOSE)Log.d(TAG,"Detached");
    }

    @Override
    public void onResume() {
        super.onResume();
        orientationEventListener.enable();
        if(VERBOSE)Log.d(TAG,"onResume");
        mediaFilters.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        mediaFilters.addDataScheme("file");
        if(getActivity() != null){
            getActivity().registerReceiver(sdCardEventReceiver, mediaFilters);
        }
        checkForSDCard();
    }

    @Override
    public void onDestroy() {
        if(VERBOSE)Log.d(TAG,"Fragment destroy...app is being minimized");
        setCameraClose();
        super.onDestroy();
    }

    @Override
    public void onStop() {
        if(VERBOSE)Log.d(TAG,"Fragment stop...app is out of focus");
        super.onStop();
    }

    @Override
    public void onPause() {
        if(VERBOSE)Log.d(TAG,"Fragment pause....app is being quit");
        setCameraQuit();
        orientationEventListener.disable();
        if(getActivity() != null){
            getActivity().unregisterReceiver(sdCardEventReceiver);
        }
        super.onPause();
    }
}
