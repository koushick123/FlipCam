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
    public final static int SHUTDOWN = 6000;
    public final static int GET_CAMERA_RENDERER_INSTANCE = 8000;
    public final static int SHOW_MEMORY_CONSUMED = 5000;
    public final static int SHOW_ELAPSED_TIME = 10000;
    public final static int RECORD_STOP_ENABLE = 14000;

    //File size for calculating memory consumed
    public final static double KILO_BYTE = 1024.0;
    public final static double MEGA_BYTE = KILO_BYTE * KILO_BYTE;
    public final static double GIGA_BYTE = KILO_BYTE * MEGA_BYTE;

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
    //Phone Memory Limit
    public static final String PHONE_MEMORY_LIMIT = "PhoneMemoryLimit";
    //Save to cloud
    public static final String SAVE_TO_GOOGLE_DRIVE = "SaveToDrive";
    public static final String SAVE_TO_DROPBOX = "SaveToDropBox";
    //Show memory consumed msg
    public static final String SHOW_MEMORY_CONSUMED_MSG = "ShowMemoryConsumedText";
}
