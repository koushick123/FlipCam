package com.flipcam.constants;

/**
 * Created by koushick on 22-Aug-17.
 */

public class Constants
{
    //Message to be sent to threads
    public final static int FRAME_AVAILABLE = 1000;
    public final static int RECORD_STOP = 2000;
    public final static int RECORD_START = 3000;
    public final static int RECORD_COMPLETE = 13000;
    public final static int RECORD_PAUSE = 14000;
    public final static int RECORD_RESUME = 15000;
    public final static int SHUTDOWN = 6000;
    public final static int GET_CAMERA_RENDERER_INSTANCE = 8000;
    public final static int SHOW_MEMORY_CONSUMED = 5000;
    public final static int SHOW_ELAPSED_TIME = 7000;
    public final static int HIDE_ELAPSED_TIME = 16000;
    public final static int RECORD_STOP_ENABLE = 9000;
    public final static int RECORD_STOP_LOW_MEMORY = 10000;
    public final static int RECORD_STOP_NO_SD_CARD = 11000;
    public final static int SHOW_SELFIE_TIMER = 12000;

    //File size for calculating memory consumed
    public final static double KILO_BYTE = 1024.0;
    public final static double MEGA_BYTE = KILO_BYTE * KILO_BYTE;
    public final static double GIGA_BYTE = KILO_BYTE * MEGA_BYTE;

    public static final String METRIC_KB = "KB";
    public static final String METRIC_MB = "MB";
    public static final String METRIC_GB = "GB";

    //To fetch first frame to display thumbnail
    public static final long FIRST_SEC_MICRO = 1000000;
    //To update seek bar
    public static final int VIDEO_SEEK_UPDATE = 100;
    //Constant for retry count to upload media
    public static final int RETRY_COUNT = 30;
    //Message for upload progress
    public static final int UPLOAD_PROGRESS = 1000;
    //Settings prefs
    public static final String FC_SETTINGS = "FlipCam_Settings";
    //Save Media
    public static final String SAVE_MEDIA_PHONE_MEM = "Save_Media_Phone_Mem";
    public static final String SD_CARD_PATH = "SD_Card_Path";
    //Media Location
    //To check if SD Card location is valid
    public static final String EMPTY = "";
    public static final String MEDIA_LOCATION_VIEW_SELECT = "Media_Location_View_Select";
    //Phone Memory Limit
    public static final String PHONE_MEMORY_LIMIT = "PhoneMemoryLimit";
    public static final String PHONE_MEMORY_METRIC = "PhoneMemoryMetric";
    public static final String PHONE_MEMORY_DISABLE = "PhoneMemoryDisable";
    //Save to cloud
    public static final String SAVE_TO_GOOGLE_DRIVE = "SaveToDrive";
    public static final String GOOGLE_DRIVE_FOLDER = "GoogleDriveFolder";
    public static final String GOOGLE_DRIVE_ACC_NAME = "AccName";
    public static final String SAVE_TO_DROPBOX = "SaveToDropBox";
    public static final String DROPBOX_FOLDER = "DropboxFolder";
    public static final int GOOGLE_DRIVE_CLOUD = 0;
    public static final int DROPBOX_CLOUD = 1;
    public static final String DROPBOX_ACCESS_TOKEN = "DropBoxAccessToken";
    //Show memory consumed msg
    public static final String SHOW_MEMORY_CONSUMED_MSG = "ShowMemoryConsumedText";
    public static final String MEDIA_COUNT_MEM = "mediaCountMem";
    public static final String MEDIA_COUNT_SD_CARD = "mediaCountSdCard";
    public static final String MEDIA_COUNT_ALL = "mediaCountAll";
    //Enable/Disable shutter sound
    public static final String SHUTTER_SOUND = "ShutterSound";
    //Set Selfie Timer
    public static final String SELFIE_TIMER = "SelfieTimer";
    public static final String SELFIE_TIMER_ENABLE = "SelfieTimerEnable";
    //Screen Resolution
    public static final String PREVIEW_RESOLUTION = "PreviewResolution";
    //Video Resolution
    public static final String SELECT_VIDEO_RESOLUTION = "SelectVideoResolution";
    public static final String VIDEO_DIMENSION_HIGH = "videoDimensionHigh";
    public static final String VIDEO_DIMENSION_MEDIUM = "videoDimensionMedium";
    public static final String VIDEO_DIMENSION_LOW = "videoDimensionLow";
    public static final String CAMPROFILE_FOR_RECORD_HIGH = "CamcorderProfileForRecordHigh";
    public static final String CAMPROFILE_FOR_RECORD_MEDIUM = "CamcorderProfileForRecordMedium";
    public static final String CAMPROFILE_FOR_RECORD_LOW = "CamcorderProfileForRecordLow";
    //Video Player
    public static final String SELECT_VIDEO_PLAYER = "SelectVideoPlayer";
    //Normal brightness level
    public static final int NORMAL_BRIGHTNESS = 5;
    public static final float NORMAL_BRIGHTNESS_PROGRESS = 0.0f;
    //Photo Resolution
    public static final String SELECT_PHOTO_RESOLUTION = "SelectPhotoResolution";
    public static final String SELECT_PHOTO_RESOLUTION_FRONT = "SelectPhotoResolutionFront";
    public static final String SUPPORT_PHOTO_RESOLUTIONS = "SupportedPhotoResolutions";
    public static final String SUPPORT_PHOTO_RESOLUTIONS_FRONT = "SupportedPhotoResolutionsFront";
    //Unmounted/Mounted Intent
    public static final String MEDIA_UNMOUNTED = "android.intent.action.MEDIA_UNMOUNTED";
    public static final String MEDIA_MOUNTED = "android.intent.action.MEDIA_MOUNTED";
}
