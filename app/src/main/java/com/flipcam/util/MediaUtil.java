package com.flipcam.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import com.flipcam.R;
import com.flipcam.constants.Constants;
import com.flipcam.media.FileMedia;
import com.flipcam.media.FileMediaLastModifiedComparator;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by koushick on 23-Nov-17.
 */

public class MediaUtil {

    public static final String TAG = "MediaUtil";
    private static FileMedia[] mediaList;
    private static Context appContext;
    static boolean VERBOSE = false;
    public static FileMedia[] getMediaList(Context ctx){
        appContext = ctx;
        sortAsPerLatest();
        return mediaList;
    }

    private static void sortAsPerLatest() {
        File dcimFc;
        SharedPreferences sharedPreferences = appContext.getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        if(sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)) {
            dcimFc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + appContext.getResources().getString(R.string.FC_ROOT));
        }
        else{
            dcimFc = new File(sharedPreferences.getString(Constants.SD_CARD_PATH, ""));
            if(VERBOSE) Log.d(TAG, "SD card path = "+sharedPreferences.getString(Constants.SD_CARD_PATH, ""));
        }
        if (dcimFc.exists() && dcimFc.isDirectory() && dcimFc.listFiles().length > 0) {
            File[] mediaFiles = dcimFc.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    if (!file.isDirectory() && (file.getPath().endsWith(appContext.getResources().getString(R.string.IMG_EXT)) ||
                            file.getPath().endsWith(appContext.getResources().getString(R.string.ANOTHER_IMG_EXT)) ||
                                    file.getPath().endsWith(appContext.getResources().getString(R.string.VID_EXT)))) {
                        return true;
                    }
                    return false;
                }
            });
            ArrayList<FileMedia> mediaArrayList = new ArrayList<>();
            for (int i = 0; i < mediaFiles.length; i++) {
                    FileMedia fileMedia = new FileMedia();
                    fileMedia.setPath(mediaFiles[i].getPath());
                    fileMedia.setLastModified(mediaFiles[i].lastModified());
                    mediaArrayList.add(fileMedia);
            }
            Collections.sort(mediaArrayList, new FileMediaLastModifiedComparator());
            mediaList = mediaArrayList.toArray(new FileMedia[mediaArrayList.size()]);
        }
        else{
            mediaList = null;
        }
    }

    public static boolean deleteFile(FileMedia media){
        File deleteFile = new File(media.getPath());
        return deleteFile.delete();
    }

    public static boolean doesPathExist(String path){
        sortAsPerLatest();
        for(int i=0;i<mediaList.length;i++){
            if(path.equalsIgnoreCase(mediaList[i].getPath())){
                return true;
            }
        }
        return false;
    }

    public static int getPhotosCount(){
        int count = 0;
        if(mediaList != null && mediaList.length > 0){
            for(int i=0;i<mediaList.length;i++){
                if(mediaList[i].getPath().endsWith(appContext.getResources().getString(R.string.IMG_EXT)) ||
                        mediaList[i].getPath().endsWith(appContext.getResources().getString(R.string.ANOTHER_IMG_EXT))){
                    count++;
                }
            }
        }
        return count;
    }

    public static int getVideosCount() {
        int count = 0;
        if (mediaList != null && mediaList.length > 0) {
            for (int i = 0; i < mediaList.length; i++) {
                if (mediaList[i].getPath().endsWith(appContext.getResources().getString(R.string.VID_EXT))) {
                    count++;
                }
            }
        }
        return count;
    }
}
