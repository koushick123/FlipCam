package com.flipcam;

import android.app.Dialog;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.FileProvider;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.flipcam.constants.Constants;
import com.flipcam.data.MediaTableConstants;
import com.flipcam.media.FileMedia;
import com.flipcam.model.Dimension;
import com.flipcam.model.MediaDetail;
import com.flipcam.util.MediaUtil;
import com.flipcam.util.SDCardUtil;
import com.flipcam.view.FolderLayout;
import com.flipcam.view.MediaFragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

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
    SeekBar videoSeek;
    LinearLayout timeControls;
    LinearLayout parentMedia;
    HashMap<Integer,MediaFragment> hashMapFrags = new HashMap<>();
    ControlVisbilityPreference controlVisbilityPreference;
    ImageButton deleteMedia;
    Display display;
    Point screenSize;
    boolean isDelete = false;
    int previousSelectedFragment = 0;
    //Default to first fragment, if user did not scroll.
    int selectedPosition = 0;
    int deletePosition = -1;
    int itemCount = 0;
    ImageView noImage;
    TextView noImageText;
    ImageButton pause;
    ImageButton shareMedia;
    Dialog deleteAlert;
    Dialog shareAlert;
    Dialog noConnAlert;
    Dialog shareToFBAlert;
    Dialog logoutFB;
    Dialog permissionFB;
    Dialog appNotExist;
    Dialog mediaLocation;
    NotificationManager mNotificationManager;
    Bitmap notifyIcon;
    Uri queueNotification;
    ImageView playCircle;
    View deleteMediaRoot;
    View taskInProgressRoot;
    View mediaLocationView;
    View mediaInfoView;
    LayoutInflater layoutInflater;
    Dialog taskAlert;
    Dialog mediaMsg;
    IntentFilter mediaFilters;
    SharedPreferences sharedPreferences;
    SDCardEventReceiver sdCardEventReceiver;
    AppWidgetManager appWidgetManager;
    boolean VERBOSE = true;
    AudioManager audioManager;
    ImageView folderViewOn;
    @BindView(R.id.infoMedia)
    ImageView infoMedia;
    FolderLayout phoneFolder;
    FolderLayout sdcardFolder;
    FolderLayout bothFolder;
    boolean fromGallery = false;
    String fcPlayer;
    String externalPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(VERBOSE)Log.d(TAG,"onCreate");
        setContentView(R.layout.activity_media);
        ButterKnife.bind(this);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        WindowManager windowManager = (WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
        screenSize=new Point();
        display.getRealSize(screenSize);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mediaFilters = new IntentFilter();
        fcPlayer = getResources().getString(R.string.videoFCPlayer);
        externalPlayer = getResources().getString(R.string.videoExternalPlayer);
        sdCardEventReceiver = new SDCardEventReceiver();
        layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        sharedPreferences = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        videoControls = (LinearLayout)findViewById(R.id.videoControls);
        fromGallery = getIntent().getExtras().getBoolean("fromGallery");
        Log.d(TAG, "fromGallery = "+fromGallery);
        if(!fromGallery){
            if(!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)){
                if(doesSDCardExist() == null){
                    exitToPreviousActivity();
                    return;
                }
                else {
                    medias = MediaUtil.getMediaList(getApplicationContext(), fromGallery);
                }
            }
            else {
                medias = MediaUtil.getMediaList(getApplicationContext(), fromGallery);
            }
        }
        else{
            //If the MediaActivity is opened from Gallery we should not check for SAVE_MEDIA_PHONE_MEM preferences
            //The MediaUtil will open based on selection.
            //All over the app, where ever we use SAVE_MEDIA_PHONE_MEM to check for location preferences, we need to see if
            //we are coming from Video/Photo fragment or Gallery using fromGallery. A user can choose to see SD Card media even
            //though under location preferences he may choose phone storage, and vice versa.
            medias = MediaUtil.getMediaList(getApplicationContext(), fromGallery);
        }
        mPager = (ViewPager) findViewById(R.id.mediaPager);
        mPager.setOffscreenPageLimit(1);
        mPagerAdapter = new MediaSlidePager(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        if(fromGallery) {
            int mediaPos = getIntent().getExtras().getInt("mediaPosition");
            if(VERBOSE)Log.d(TAG, "Intent extra = " +mediaPos);
            mPager.setCurrentItem(mediaPos);
            selectedPosition = previousSelectedFragment = mediaPos;
        }
        else if(getIntent().getExtras().getBoolean("fromMenu")){
            int scrollPos = getIntent().getExtras().getInt("scrollTo");
            Log.d(TAG, "go to page = "+scrollPos);
            mPager.setCurrentItem(scrollPos);
            selectedPosition = previousSelectedFragment = scrollPos;
        }
        deleteMedia = (ImageButton)findViewById(R.id.deleteMedia);
        deleteMedia.setOnClickListener((view) -> {
            Log.d(TAG, "from the Gallery = "+fromGallery);
            medias = MediaUtil.getMediaList(getApplicationContext(), fromGallery);
            if(medias != null) {
                deleteMediaRoot = layoutInflater.inflate(R.layout.delete_media, null);
                if(VERBOSE)Log.d(TAG, "Delete position = " + selectedPosition);
                TextView deleteMsg = (TextView) deleteMediaRoot.findViewById(R.id.deleteMsg);
                if (isImage(medias[selectedPosition].getPath())) {
                    deleteMsg.setText(getResources().getString(R.string.deleteMessage, getResources().getString(R.string.photo)));
                } else {
                    deleteMsg.setText(getResources().getString(R.string.deleteMessage, getResources().getString(R.string.video)));
                }
                deleteAlert = new Dialog(this);
                deleteAlert.setContentView(deleteMediaRoot);
                deleteAlert.setCancelable(true);
                deleteAlert.show();
            }
        });
        mediaLocation = new Dialog(this);
        noConnAlert = new Dialog(this);
        pause = (ImageButton) findViewById(R.id.playButton);
        shareMedia = (ImageButton)findViewById(R.id.shareMedia);
        shareToFBAlert = new Dialog(this);
        shareAlert = new Dialog(this);
        logoutFB = new Dialog(this);
        permissionFB = new Dialog(this);
        appNotExist = new Dialog(this);
        shareMedia.setOnClickListener((view) -> {
            Log.d(TAG, "from Gallery? = "+fromGallery);
            medias = MediaUtil.getMediaList(getApplicationContext(), fromGallery);
            if(medias != null) {
                if(VERBOSE)Log.d(TAG, "Share position = " + selectedPosition);
                Uri mediaUri;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    if(VERBOSE)Log.d(TAG, "For OREO and above use FileProvider");
                    mediaUri = FileProvider.getUriForFile(MediaActivity.this, BuildConfig.APPLICATION_ID+".provider",
                            new File(medias[selectedPosition].getPath()));
                }
                else {
                    mediaUri = Uri.fromFile(new File(medias[selectedPosition].getPath()));
                }
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, mediaUri);
                if (isImage(medias[selectedPosition].getPath())) {
                    shareIntent.setType(getResources().getString(R.string.imageType));
                } else {
                    shareIntent.setType(getResources().getString(R.string.videoType));
                }
                if (doesAppExistForIntent(shareIntent)) {
                    if(VERBOSE)Log.d(TAG, "Apps exists");
                    Intent chooser;
                    if (isImage(medias[selectedPosition].getPath())) {
                        chooser = Intent.createChooser(shareIntent, getResources().getString(R.string.chooserTitleImage));
                    } else {
                        chooser = Intent.createChooser(shareIntent, getResources().getString(R.string.chooserTitleVideo));
                    }
                    if (shareIntent.resolveActivity(getPackageManager()) != null) {
                        if(VERBOSE)Log.d(TAG, "Start activity to choose");
                        if(!fromGallery) {
                            if (!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)) {
                                if (doesSDCardExist() == null) {
                                    exitToPreviousActivity();
                                    return;
                                }
                            }
                        }
                        else{
                            if(sharedPreferences.getString(Constants.MEDIA_LOCATION_VIEW_SELECT, phoneLoc).
                                    equalsIgnoreCase(sdcardLoc) ||
                                sharedPreferences.getString(Constants.MEDIA_LOCATION_VIEW_SELECT, phoneLoc).
                                        equalsIgnoreCase(allLoc)){
                                if (doesSDCardExist() == null) {
                                    exitToPreviousActivity();
                                    return;
                                }
                            }
                        }
                        startActivity(chooser);
                    }
                }
            }
        });
        startTime = (TextView)findViewById(R.id.startTime);
        endTime = (TextView)findViewById(R.id.endTime);
        videoSeek = (SeekBar)findViewById(R.id.videoSeek);
        topMediaControls = (LinearLayout)findViewById(R.id.topMediaControls);
        timeControls = (LinearLayout)findViewById(R.id.timeControls);
        parentMedia = (LinearLayout)findViewById(R.id.parentMedia);
        controlVisbilityPreference = (ControlVisbilityPreference)getApplicationContext();
        noImage = (ImageView)findViewById(R.id.noImage);
        noImageText = (TextView)findViewById(R.id.noImageText);
        playCircle = (ImageView)findViewById(R.id.playVideo);
        folderViewOn = (ImageView)findViewById(R.id.folderViewOn);
        if(VERBOSE)Log.d(TAG, "savedInstanceState = "+savedInstanceState);
        if(savedInstanceState == null){
            clearMediaPreferences();
            controlVisbilityPreference.setHideControl(true);
            reDrawPause();
            reDrawTopMediaControls();
            if(isImage(medias[0].getPath())) {
                if(VERBOSE)Log.d(TAG, "Hide PlayForVideo");
                removeVideoControls();
                hidePlayForVideo();
            }
            else{
                if(sharedPreferences.getString(Constants.SELECT_VIDEO_PLAYER, externalPlayer).equalsIgnoreCase(fcPlayer)) {
                    playCircle.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_circle_outline));
                    if (!controlVisbilityPreference.isHideControl()) {
                        if (VERBOSE) Log.d(TAG, "Show PlayForVideo");
                        setupPlayForVideo(0);
                        showPlayForVideo();
                    }
                }
                else{
                    removeVideoControls();
                    setupPlayCircleForExternalPlayer();
                }
            }
        }
        else{
            selectedPosition = savedInstanceState.getInt("selectedPosition");
            if(VERBOSE)Log.d(TAG, "get selectedPosition = "+selectedPosition);
            if(isImage(medias[selectedPosition].getPath())) {
                removeVideoControls();
                hidePlayForVideo();
            }
            else{
                if(sharedPreferences.getString(Constants.SELECT_VIDEO_PLAYER, externalPlayer).equalsIgnoreCase(fcPlayer)) {
                    playCircle.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_circle_outline));
                    if (!controlVisbilityPreference.isHideControl()) {
                        setupPlayForVideo(0);
                        showPlayForVideo();
                    }
                }
                else{
                    removeVideoControls();
                    setupPlayCircleForExternalPlayer();
                }
            }
        }
        folderViewOn.setOnClickListener((view1) -> {
                mediaLocationView = layoutInflater.inflate(R.layout.medialocation, null);
                phoneFolder = mediaLocationView.findViewById(R.id.phoneFolder);
                sdcardFolder = mediaLocationView.findViewById(R.id.sdcardFolder);
                bothFolder = mediaLocationView.findViewById(R.id.bothFolder);
                phoneFolder.setMediaActivity(this);
                sdcardFolder.setMediaActivity(this);
                bothFolder.setMediaActivity(this);
                mediaLocation.setContentView(mediaLocationView);
                mediaLocation.setCancelable(true);
                mediaLocation.show();
        });
        notifyIcon = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.ic_launcher);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        queueNotification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        taskInProgressRoot = layoutInflater.inflate(R.layout.task_in_progress, null);
        taskAlert = new Dialog(this);
        appWidgetManager = (AppWidgetManager)getSystemService(Context.APPWIDGET_SERVICE);
        phoneLoc = getResources().getString(R.string.phoneLocation);
        sdcardLoc = getResources().getString(R.string.sdcardLocation);
        allLoc = getResources().getString(R.string.allLocation);
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
                    audioManager.abandonAudioFocus(this);
                    break;
            }
        }
    };

    public Dialog getMediaLocation(){
        return mediaLocation;
    }

    public void goToGallery(String selectedFolderLabel){
        //Save media location selection in a FC Setting preference. Use this in Gallery to load media.
        SharedPreferences.Editor mediaLocEdit = sharedPreferences.edit();
        mediaLocEdit.putString(Constants.MEDIA_LOCATION_VIEW_SELECT, selectedFolderLabel);
        mediaLocEdit.commit();
        Intent galleryAct = new Intent(getApplicationContext(), GalleryActivity.class);
        galleryAct.putExtra("fromMedia", true);
        startActivity(galleryAct);
        overridePendingTransition(R.anim.slide_from_right,R.anim.slide_to_left);
    }
    String phoneLoc;
    String sdcardLoc;
    String allLoc;
    @Override
    protected void onStop() {
        super.onStop();
        if(VERBOSE)Log.d(TAG,"onStop fromGal = "+fromGallery);
        if(medias != null) {
            SharedPreferences.Editor mediaState = sharedPreferences.edit();
            if(!fromGallery) {
                if (sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)) {
                    mediaState.putInt(Constants.MEDIA_COUNT_MEM, medias.length);
                } else {
                    mediaState.putInt(Constants.MEDIA_COUNT_SD_CARD, medias.length);
                }
            }
            else{
                if(sharedPreferences.getString(Constants.MEDIA_LOCATION_VIEW_SELECT, phoneLoc).equalsIgnoreCase(
                        sdcardLoc)){
                    mediaState.putInt(Constants.MEDIA_COUNT_SD_CARD, medias.length);
                }
                else if(sharedPreferences.getString(Constants.MEDIA_LOCATION_VIEW_SELECT, phoneLoc).equalsIgnoreCase(
                        phoneLoc)){
                    mediaState.putInt(Constants.MEDIA_COUNT_MEM, medias.length);
                }
                else{
                    mediaState.putInt(Constants.MEDIA_COUNT_ALL, medias.length);
                }
            }
            mediaState.commit();
            if(VERBOSE)Log.d(TAG, "Media length before leaving = " + medias.length);
        }
        else{
            clearMediaPreferences();
        }
        if(VERBOSE)Log.d(TAG ,"selectedPosition = "+selectedPosition);
        if(hashMapFrags.get(selectedPosition) != null) {
            hashMapFrags.get(selectedPosition).getMediaView().setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        if(VERBOSE)Log.d(TAG,"onDestroy");
        super.onDestroy();
    }

    public void reDrawPause(){
        LinearLayout.LayoutParams pauseParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if(display.getRotation() == Surface.ROTATION_0) {
            pauseParams.weight = 0.1f;
        }
        else{
            pauseParams.weight = 0.04f;
        }
        pauseParams.height = (int)getResources().getDimension(R.dimen.playButtonHeight);
        pauseParams.gravity = Gravity.CENTER;
        pause.setScaleType(ImageView.ScaleType.CENTER_CROP);
        pause.setLayoutParams(pauseParams);
    }

    void exitToPreviousActivity(){
        if(fromGallery) {
            exitMediaAndShowNoSDCardInGallery();
        }
        else {
            exitMediaAndShowNoSDCard();
        }
    }

    public void reDrawTopMediaControls(){
        FrameLayout.LayoutParams topMediaParams = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if(display.getRotation() == Surface.ROTATION_0){
            topMediaParams.height = (int) getResources().getDimension(R.dimen.topMediaBarPortrait);
            deleteMedia.setImageDrawable(getResources().getDrawable(R.drawable.ic_delete_portrait));
            shareMedia.setImageDrawable(getResources().getDrawable(R.drawable.ic_share_portrait));
        }
        else{
            topMediaParams.height = (int) getResources().getDimension(R.dimen.topMediaBarLandscape);
            deleteMedia.setImageDrawable(getResources().getDrawable(R.drawable.ic_delete));
            shareMedia.setImageDrawable(getResources().getDrawable(R.drawable.ic_share));
        }
        topMediaControls.setLayoutParams(topMediaParams);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(VERBOSE)Log.d(TAG,"onConfigurationChanged = "+display.getRotation());
        reDrawPause();
        reDrawTopMediaControls();
    }

    public String doesSDCardExist(){
        String sdcardpath = sharedPreferences.getString(Constants.SD_CARD_PATH, "");
        try {
            String filename = "/doesSDCardExist_"+String.valueOf(System.currentTimeMillis()).substring(0,5);
            sdcardpath += filename;
            final String sdCardFilePath = sdcardpath;
            final FileOutputStream createTestFile = new FileOutputStream(sdcardpath);
            if(VERBOSE)Log.d(TAG, "Able to create file... SD Card exists");
            File testfile = new File(sdCardFilePath);
            testfile.delete();
            createTestFile.close();
        } catch (FileNotFoundException e) {
            if(VERBOSE)Log.d(TAG, "Unable to create file... SD Card NOT exists..... "+e.getMessage());
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sharedPreferences.getString(Constants.SD_CARD_PATH, "");
    }

    public void showMediaInfo(View view){
        if(medias!=null) {
            mediaMsg = new Dialog(this);
            mediaInfoView = layoutInflater.inflate(R.layout.media_info, null);
            mediaMsg.setContentView(mediaInfoView);
            mediaMsg.setCancelable(true);
            mediaMsg.show();
            MediaDetail mediaInfo = new MediaDetail();
            Dimension dimension;
            String path = medias[selectedPosition].getPath();
            boolean isAnImage = true;
            try {
                int width;
                int height;
                if(isImage(path)) {
                    ExifInterface photoMetaData = new ExifInterface(path);
                    width = Integer.parseInt(photoMetaData.getAttribute(ExifInterface.TAG_IMAGE_WIDTH));
                    height = Integer.parseInt(photoMetaData.getAttribute(ExifInterface.TAG_IMAGE_LENGTH));
                    int orientation = Integer.parseInt(photoMetaData.getAttribute(ExifInterface.TAG_ORIENTATION));
                    Log.d(TAG, "Image Orientation = "+orientation);
                    if(width == 0 || width == -1 || height == 0 || height == -1){
                        Log.d(TAG, "PATH = "+path);
                        Bitmap selImage = BitmapFactory.decodeFile(path);
                        width = selImage.getWidth();
                        height = selImage.getHeight();
                    }
                    if(orientation != ExifInterface.ORIENTATION_UNDEFINED){
                        if(orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270){
                            //For Portrait, swap dimension values
                            int temp = width;
                            width = height;
                            height = temp;
                        }
                    }
                }
                else{
                    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                    mediaMetadataRetriever.setDataSource(path);
                    width = Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                    height = Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                    isAnImage = false;
                }
                if(VERBOSE)Log.d(TAG, "WIDTH X HEIGHT = "+width +" X "+height);
                dimension = new Dimension(width, height);
                mediaInfo.setResolution(dimension);
                File selectedFile = new File(path);
                Date dateCreated = new Date(selectedFile.lastModified());
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");
                String dateDisplay = simpleDateFormat.format(dateCreated);
                if (VERBOSE) Log.d(TAG, "Date display = " + dateDisplay);
                mediaInfo.setDateCreated(dateDisplay);
                String name = path.substring(path.lastIndexOf("/") + 1);
                if (VERBOSE) Log.d(TAG, "Name = " + name);
                mediaInfo.setName(name);
                if (VERBOSE) Log.d(TAG, "Path = " + path.substring(0, path.lastIndexOf("/")));
                mediaInfo.setPath(path.substring(0, path.lastIndexOf("/")));
                mediaInfo.setSize(MediaUtil.convertMemoryForDisplay(selectedFile.length()));
                populateMediaDetail(mediaInfo, isAnImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void populateMediaDetail(MediaDetail mediaInfo, boolean isImage){
        TextView mediaName = mediaInfoView.findViewById(R.id.name);
        TextView mediaPath = mediaInfoView.findViewById(R.id.path);
        TextView mediaDimension = mediaInfoView.findViewById(R.id.dimension);
        TextView mediaDateCreated = mediaInfoView.findViewById(R.id.dateCreated);
        TextView mediaSize = mediaInfoView.findViewById(R.id.size);
        TextView mediaInfoTitle = mediaInfoView.findViewById(R.id.mediaInfoTitle);
        Button okBtn = mediaInfoView.findViewById(R.id.okButton);
        okBtn.setOnClickListener((view) -> {
            mediaMsg.dismiss();
        });
        //Set the labels and values for Media Info
        Resources resources = getResources();
        if(isImage){
            mediaInfoTitle.setText(resources.getString(R.string.mediaInfoTitle, resources.getString(R.string.PHOTO_MODE)));
        }
        else{
            mediaInfoTitle.setText(resources.getString(R.string.mediaInfoTitle, resources.getString(R.string.VIDEO_MODE)));
        }
        StringBuffer mediaLabelAndValue = new StringBuffer();
        //NAME
        mediaLabelAndValue.append(resources.getString(R.string.mediaInfoName));
        mediaLabelAndValue.append(mediaInfo.getName());
        mediaName.setText(mediaLabelAndValue.toString());
        mediaLabelAndValue.delete(0, mediaLabelAndValue.length());
        //PATH
        mediaLabelAndValue.append(resources.getString(R.string.mediaInfoPath));
        mediaLabelAndValue.append(mediaInfo.getPath());
        mediaPath.setText(mediaLabelAndValue.toString());
        mediaLabelAndValue.delete(0, mediaLabelAndValue.length());
        //DATE CREATED
        mediaLabelAndValue.append(resources.getString(R.string.mediaInfoDateCreated));
        mediaLabelAndValue.append(mediaInfo.getDateCreated());
        mediaDateCreated.setText(mediaLabelAndValue.toString());
        mediaLabelAndValue.delete(0, mediaLabelAndValue.length());
        //DIMENSION
        mediaLabelAndValue.append(resources.getString(R.string.mediaInfoDimension));
        mediaLabelAndValue.append(mediaInfo.getResolution());
        mediaDimension.setText(mediaLabelAndValue.toString());
        mediaLabelAndValue.delete(0, mediaLabelAndValue.length());
        //SIZE
        mediaLabelAndValue.append(resources.getString(R.string.mediaInfoSize));
        mediaLabelAndValue.append(mediaInfo.getSize());
        mediaSize.setText(mediaLabelAndValue.toString());
    }

    public void deleteMedia(int position)
    {
        if(!fromGallery) {
            if (!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)) {
                if (doesSDCardExist() == null) {
                    taskAlert.dismiss();
                    exitToPreviousActivity();
                    return;
                }
            }
        }
        if(VERBOSE)Log.d(TAG,"Length before delete = "+medias.length);
        if(VERBOSE)Log.d(TAG,"Deleting file = "+medias[position].getPath());
        String deletePath = medias[position].getPath();
        if(MediaUtil.deleteFile(medias[position])) {
            if(VERBOSE)Log.d(TAG, "deletePath = "+deletePath);
            getContentResolver().delete(Uri.parse(MediaTableConstants.BASE_CONTENT_URI + "/deleteMedia"), null, new String[]{deletePath});
            itemCount = 0;
            isDelete = true;
            if(position == medias.length - 1){
                //onPageSelected is called when deleting last media. Need to make previousSelectedFragment as -1.
                previousSelectedFragment = -1;
            }
            medias = MediaUtil.getMediaList(getApplicationContext(), fromGallery);
            if(medias != null) {
                runOnUiThread(() -> {
                    if(VERBOSE)Log.d(TAG, "BEFORE notifyDataSetChanged");
                    mPagerAdapter.notifyDataSetChanged();
                    if(VERBOSE)Log.d(TAG, "AFTER notifyDataSetChanged");
                    taskAlert.dismiss();
                });
            }
            else{
                runOnUiThread(() -> {
                    taskAlert.dismiss();
                    showNoImagePlaceholder();
                });
            }
        }
        else{
            runOnUiThread(() -> {
                taskAlert.dismiss();
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.unableToDelete),Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(VERBOSE)Log.d(TAG, "onSaveInstanceState = "+selectedPosition);
        outState.putInt("selectedPosition",selectedPosition);
    }

    public void setupPlayForVideo(final int videoPos){
        if(VERBOSE)Log.d(TAG, "setupPlayForVideo");
        playCircle.setClickable(true);
        playCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaFragment currentFrag = hashMapFrags.get(videoPos);
                startPlayingMedia(currentFrag, false);
            }
        });
    }

    private void startPlayingMedia(MediaFragment currentFrag, boolean fromPauseBtn){
        int audioFocus = audioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if(audioFocus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (currentFrag.isPlayCompleted()) {
                currentFrag.setIsPlayCompleted(false);
            }
            if(!fromPauseBtn) {
                if (VERBOSE) Log.d(TAG, "Set PLAY using Circle");
            }
            else{
                if(VERBOSE)Log.d(TAG,"Set PLAY");
            }
            currentFrag.playInProgress = true;
            if (VERBOSE)
                Log.d(TAG, "Duration of video = " + currentFrag.mediaPlayer.getDuration() + " , path = " +
                        currentFrag.path.substring(currentFrag.path.lastIndexOf("/"), currentFrag.path.length()));
            currentFrag.mediaPlayer.start();
            if (!fromPauseBtn) {
                pause.setVisibility(View.VISIBLE);
            }
            pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
            currentFrag.play = true;
            playCircle.setVisibility(View.GONE);
        }
    }

    public void showPlayForVideo(){
        playCircle.setVisibility(View.VISIBLE);
    }

    public void hidePlayForVideo(){
        playCircle.setVisibility(View.GONE);
    }

    public boolean doesAppExistForIntent(Intent shareIntent){
        PackageManager packageManager = getPackageManager();
        List activities = packageManager.queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if(VERBOSE)Log.d(TAG, "No of activities that can share = "+activities.size());
        boolean isIntentSafe = activities.size() > 0;
        return isIntentSafe;
    }

    public void okToClose(View view){
        noConnAlert.dismiss();
    }

    public void delete(View view){
        if(VERBOSE)Log.d(TAG,"DELETE");
        deleteAlert.dismiss();
        TextView savetocloudtitle = (TextView)taskInProgressRoot.findViewById(R.id.savetocloudtitle);
        TextView signInText = (TextView)taskInProgressRoot.findViewById(R.id.signInText);
        ImageView signInImage = (ImageView)taskInProgressRoot.findViewById(R.id.signInImage);
        signInImage.setVisibility(View.INVISIBLE);
        signInText.setText(getResources().getString(R.string.deleteMediaMsg));
        savetocloudtitle.setText(getResources().getString(R.string.deleteTitle));
        taskInProgressRoot.setBackgroundColor(getResources().getColor(R.color.mediaControlColor));
        taskAlert.setContentView(taskInProgressRoot);
        taskAlert.setCancelable(false);
        taskAlert.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                deleteMedia(selectedPosition);
            }
        }).start();
    }

    public void cancel(View view){
        if(VERBOSE)Log.d(TAG,"CANCEL");
        deleteAlert.dismiss();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    private void clearMediaPreferences(){
        SharedPreferences mediaValues = getSharedPreferences(FC_MEDIA_PREFERENCE,Context.MODE_PRIVATE);
        SharedPreferences.Editor mediaState = null;
        if(mediaValues!=null){
            mediaState = mediaValues.edit();
            if(mediaState!=null){
                mediaState.clear();
                mediaState.commit();
                if(VERBOSE)Log.d(TAG,"CLEAR ALL");
            }
        }
    }

    private void setupPlayCircleForExternalPlayer(){
        playCircle.setVisibility(View.VISIBLE);
        playCircle.setImageDrawable(getResources().getDrawable(R.drawable.ic_external_play_circle_outline));
        playCircle.setOnClickListener((view) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(medias[selectedPosition].getPath()));
            intent.setDataAndType(Uri.parse(medias[selectedPosition].getPath()),
                    getResources().getString(R.string.videoType));
            startActivity(intent);
        });
    }

    @Override
    public void onPageSelected(int position) {
        if(!fromGallery) {
            if (!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)) {
                String sdCard = doesSDCardExist();
                if (sdCard == null) {
                    exitToPreviousActivity();
                    return;
                }
            }
        }
        if(VERBOSE)Log.d(TAG,"onPageSelected = "+position+", previousSelectedFragment = "+previousSelectedFragment);
        selectedPosition = position;
        final MediaFragment currentFrag = hashMapFrags.get(position);
        if(VERBOSE)Log.d(TAG,"isHideControl = "+controlVisbilityPreference.isHideControl());
        //Reset preferences for every new fragment to be displayed.
        clearMediaPreferences();
        if(previousSelectedFragment != -1) {
            MediaFragment previousFragment = hashMapFrags.get(previousSelectedFragment);
            //If previous fragment had a video, stop the video and tracker thread immediately.
            if(VERBOSE)Log.d(TAG, "medias length = "+medias.length);
            if (!isImage(medias[previousSelectedFragment].getPath())) {
                if(VERBOSE)Log.d(TAG, "Stop previous tracker thread = " + previousFragment.path);
                previousFragment.stopTrackerThread();
                if (previousFragment.mediaPlayer.isPlaying()) {
                    if(VERBOSE)Log.d(TAG, "Pause previous playback");
                    previousFragment.mediaPlayer.pause();
                }
            }
        }
        //Display controls based on image/video
        if(isImage(medias[position].getPath())){
            if(VERBOSE)Log.d(TAG,"HIDE VIDEO");
            hidePlayForVideo();
            removeVideoControls();
        }
        else{
            if(sharedPreferences.getString(Constants.SELECT_VIDEO_PLAYER, externalPlayer).equalsIgnoreCase(fcPlayer)) {
                playCircle.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_circle_outline));
                if (controlVisbilityPreference.isHideControl()) {
                    if (VERBOSE) Log.d(TAG, "show controls");
                    showControls();
                } else {
                    if (VERBOSE) Log.d(TAG, "hide controls");
                    removeVideoControls();
                }
                setupVideo(currentFrag, position);
                currentFrag.previousPos = 0;
                if (VERBOSE)
                    Log.d(TAG, "Has VIDEO TRACKER STARTED? = " + currentFrag.isStartTracker());
                if (!currentFrag.isStartTracker()) {
                    currentFrag.startTrackerThread();
                }
            }
            else{
                removeVideoControls();
                setupPlayCircleForExternalPlayer();
            }
        }
        previousSelectedFragment = position;
    }

    public void showControls(){
        pause.setVisibility(View.VISIBLE);
        startTime.setVisibility(View.VISIBLE);
        endTime.setVisibility(View.VISIBLE);
        videoSeek.setVisibility(View.VISIBLE);
    }

    public void removeVideoControls(){
        pause.setVisibility(View.INVISIBLE);
        startTime.setVisibility(View.INVISIBLE);
        endTime.setVisibility(View.INVISIBLE);
        videoSeek.setVisibility(View.INVISIBLE);
    }

    public void setupVideoControls(final int position){
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(medias[position].getPath());
        duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        calculateAndDisplayEndTime(Integer.parseInt(duration), true, position);
        if(VERBOSE)Log.d(TAG,"Set MEDIA = "+medias[position].getPath());
        //Include tracker and reset position to start playing from start.
        videoControls.removeAllViews();
        videoControls.addView(timeControls);
        videoControls.addView(videoSeek);
        videoControls.addView(parentMedia);
        videoSeek.setMax(Integer.parseInt(duration));
        videoSeek.setProgress(0);
        videoSeek.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.turqoise)));
        videoSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    videoSeek.setProgress(progress);
                    hashMapFrags.get(position).mediaPlayer.seekTo(progress);
                    calculateAndDisplayEndTime(progress, false, position);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(hashMapFrags.get(position).mediaPlayer.isPlaying()){
                    hashMapFrags.get(position).mediaPlayer.pause();
                    wasPlaying = true;
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(wasPlaying){
                    hashMapFrags.get(position).mediaPlayer.start();
                    wasPlaying = false;
                }
            }
        });
    }

    boolean wasPlaying = false;

    public void setupVideo(final MediaFragment currentFrag, int position){
        setupVideoControls(position);
        currentFrag.play=false;
        currentFrag.playInProgress=false;
        getIntent().removeExtra("saveVideoForMinimize");
        currentFrag.savedVideo = null;
        currentFrag.setIsPlayCompleted(false);
        final int pos = position;
        currentFrag.mediaPlayer.setOnErrorListener((mediaPlayer, what, extra) -> {
            if(VERBOSE)Log.d(TAG,"CATCH onError = "+extra);
            if(extra == MediaPlayer.MEDIA_ERROR_IO){
                //Possible file not found since SD Card removed
                if(!fromGallery) {
                    if (!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)) {
                        exitMediaAndShowNoSDCard();
                        return true;
                    }
                }
            }
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
        });
        currentFrag.resetMediaPlayer();
        currentFrag.resetVideoTime();
        reDrawPause();
        pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow));
        pause.setOnClickListener((view) -> {
            if (!currentFrag.play) {
                startPlayingMedia(currentFrag, true);
            } else {
                if(VERBOSE)Log.d(TAG,"Set PAUSE");
                currentFrag.mediaPlayer.pause();
                pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow));
                currentFrag.play = false;
            }
        });
        setupPlayForVideo(position);
        if(!controlVisbilityPreference.isHideControl()) {
            showPlayForVideo();
        }
        else{
            hidePlayForVideo();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    public void calculateAndDisplayEndTime(int latestPos, boolean eTime, int videoPos)
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
            startTime.setText(getResources().getString(R.string.START_TIME));
            endTime.setText(showHr + " : " + showMin + " : " + showSec);
        }
        else{
            hashMapFrags.get(videoPos).setSeconds(secs);
            hashMapFrags.get(videoPos).setMinutes(mins);
            hashMapFrags.get(videoPos).setHours(hour);
            startTime.setText(showHr + " : " + showMin + " : " + showSec);
        }
    }

    class SDCardEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if(VERBOSE)Log.d(TAG, "onReceive = "+intent.getAction());
            handleSDCardUnmounted(intent);
        }
    }

    public void handleSDCardUnmounted(Intent receiveIntent){
        if(receiveIntent.getAction().equalsIgnoreCase(Intent.ACTION_MEDIA_UNMOUNTED) ||
                receiveIntent.getAction().equalsIgnoreCase(Constants.MEDIA_UNMOUNTED)){
            //Check if SD Card was selected
            if(!fromGallery) {
                if (!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)) {
                    exitMediaAndShowNoSDCard();
                }
            }
            else{
                Log.d(TAG, "Media Location View = "+
                        sharedPreferences.getString(Constants.MEDIA_LOCATION_VIEW_SELECT, phoneLoc));
                if(sharedPreferences.getString(Constants.MEDIA_LOCATION_VIEW_SELECT, phoneLoc).equalsIgnoreCase(sdcardLoc)
                        || sharedPreferences.getString(Constants.MEDIA_LOCATION_VIEW_SELECT, phoneLoc).equalsIgnoreCase(allLoc)){
                    exitMediaAndShowNoSDCard();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(VERBOSE)Log.d(TAG,"onResume from Gallery = "+fromGallery);
        mPager.addOnPageChangeListener(this);
        mediaFilters.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        mediaFilters.addDataScheme("file");
        registerReceiver(sdCardEventReceiver, mediaFilters);
        if(!fromGallery) {
            if (!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)) {
                if (doesSDCardExist() == null) {
                    exitToPreviousActivity();
                    return;
                } else {
                    refreshMediaFromSource();
                }
            } else {
                refreshMediaFromSource();
            }
        }
        else{
            Log.d(TAG, "Media Location View Select = "+
                    sharedPreferences.getString(Constants.MEDIA_LOCATION_VIEW_SELECT, phoneLoc));
            if(sharedPreferences.getString(Constants.MEDIA_LOCATION_VIEW_SELECT, phoneLoc).equalsIgnoreCase(sdcardLoc) ||
                    sharedPreferences.getString(Constants.MEDIA_LOCATION_VIEW_SELECT, phoneLoc).equalsIgnoreCase(allLoc)){
                String sdcardPath = SDCardUtil.doesSDCardExist(getApplicationContext());
                if((sdcardPath == null || sdcardPath.equalsIgnoreCase("")) ||
                        !SDCardUtil.isPathWritable(sdcardPath)){
                    exitToPreviousActivity();
                    return;
                }
            }
            refreshMediaFromSource();
        }
    }

    public void refreshMediaFromSource(){
        itemCount = 0;
        int oldLength;
        if(VERBOSE)Log.d(TAG, "refreshMediaSource fromGallery = "+fromGallery);
        if(!fromGallery) {
            if (sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)) {
                oldLength = getSharedPreferences(FC_MEDIA_PREFERENCE, Context.MODE_PRIVATE).getInt(Constants.MEDIA_COUNT_MEM, 0);
            } else {
                oldLength = getSharedPreferences(FC_MEDIA_PREFERENCE, Context.MODE_PRIVATE).getInt(Constants.MEDIA_COUNT_SD_CARD, 0);
            }
        }
        else{
            if(sharedPreferences.getString(Constants.MEDIA_LOCATION_VIEW_SELECT, phoneLoc).equalsIgnoreCase(phoneLoc)){
                oldLength = getSharedPreferences(FC_MEDIA_PREFERENCE, Context.MODE_PRIVATE).getInt(Constants.MEDIA_COUNT_MEM, 0);
            }
            else if(sharedPreferences.getString(Constants.MEDIA_LOCATION_VIEW_SELECT, sdcardLoc).equalsIgnoreCase(sdcardLoc)){
                oldLength = getSharedPreferences(FC_MEDIA_PREFERENCE, Context.MODE_PRIVATE).getInt(Constants.MEDIA_COUNT_SD_CARD, 0);
            }
            else{
                oldLength = getSharedPreferences(FC_MEDIA_PREFERENCE, Context.MODE_PRIVATE).getInt(Constants.MEDIA_COUNT_ALL, 0);
            }
        }
        medias = MediaUtil.getMediaList(getApplicationContext(), fromGallery);
        if(medias != null) {
            if (medias.length < oldLength) {
                if(VERBOSE)Log.d(TAG, "Possible deletions outside of App");
                isDelete = true;
                previousSelectedFragment = -1;
            } else {
                if(VERBOSE)Log.d(TAG, "Files added or no change");
            }
            hideNoImagePlaceholder();
            mPagerAdapter.notifyDataSetChanged();
        }
        else{
            clearMediaPreferences();
            showNoImagePlaceholder();
        }
    }

    public void exitMediaAndShowNoSDCard(){
        if(VERBOSE)Log.d(TAG, "exitMediaAndShowNoSDCard");
        Intent camera = new Intent(this,CameraActivity.class);
        camera.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        camera.putExtra("fromGallery", fromGallery);
        startActivity(camera);
        finish();
    }

    void exitMediaAndShowNoSDCardInGallery(){
        if(VERBOSE)Log.d(TAG, "exitMediaAndShowNoSDCardInGallery");
        Intent mediaGrid = new Intent(this,GalleryActivity.class);
        mediaGrid.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        mediaGrid.putExtra("fromMedia", true);
        startActivity(mediaGrid);
        finish();
    }

    public void hideNoImagePlaceholder(){
        parentMedia.setVisibility(View.VISIBLE);
        mPager.setVisibility(View.VISIBLE);
        noImage.setVisibility(View.GONE);
        noImageText.setVisibility(View.GONE);
    }

    public void showNoImagePlaceholder(){
        //No Images
        videoControls.setVisibility(View.GONE);
        mPager.setVisibility(View.GONE);
        noImage.setVisibility(View.VISIBLE);
        noImageText.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPager.removeOnPageChangeListener(this);
        unregisterReceiver(sdCardEventReceiver);
        if(VERBOSE)Log.d(TAG,"onPause");
        if(fromGallery) {
            controlVisbilityPreference.setMediaSelectedPosition(selectedPosition);
        }
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
            if(VERBOSE)Log.d(TAG,"getItem = "+position);
            MediaFragment mediaFragment;
            if(isDelete) {
                isDelete = false;
                if(VERBOSE)Log.d(TAG, "fromGallery sent to MediaFragment = "+fromGallery);
                mediaFragment = MediaFragment.newInstance(position, true, fromGallery);
                if(mediaFragment.getUserVisibleHint()) {
                    if (isImage(medias[position].getPath())) {
                        if(VERBOSE)Log.d(TAG, "IS image");
                        removeVideoControls();
                    } else {
                        if(VERBOSE)Log.d(TAG, "IS video");
                        showControls();
                        setupVideoControls(position);
                    }
                }
            }
            else{
                mediaFragment = MediaFragment.newInstance(position, false, fromGallery);
            }
            hashMapFrags.put(Integer.valueOf(position),mediaFragment);
            return mediaFragment;
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
            MediaFragment fragment = (MediaFragment)object;
            if(VERBOSE)Log.d(TAG,"getItemPos = "+fragment.getPath()+", POS = "+fragment.getFramePosition()+", Uservisible? = "+fragment.getUserVisibleHint());
            itemCount++;
            if(MediaUtil.doesPathExist(fragment.getPath())){
                if(deletePosition != -1) {
                    if (deletePosition < medias.length) {
                        if(fragment.getFramePosition() == (deletePosition + 1) || fragment.getFramePosition() == (deletePosition + 2)) {
                            if(VERBOSE)Log.d(TAG, "Recreate the next fragment as well");
                            if(itemCount == 3) {
                                deletePosition = -1;
                            }
                        }
                        return POSITION_NONE;
                    } else if (deletePosition == medias.length - 1 && fragment.getFramePosition() == (deletePosition - 1)) {
                        if(VERBOSE)Log.d(TAG, "Recreate the previous fragment as well");
                        deletePosition = -1;
                        return POSITION_NONE;
                    }
                }
                return POSITION_UNCHANGED;
            }
            else {
                deletePosition = fragment.getFramePosition();
                return POSITION_NONE;
            }
        }
    }
}
