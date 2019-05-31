package com.flipcam.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.flipcam.R;
import com.flipcam.constants.Constants;
import com.flipcam.media.FileMedia;
import com.flipcam.media.FileMediaLastModifiedComparator;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        File dcimFc = null;
        boolean allLoc = false;
        SharedPreferences sharedPreferences = appContext.getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        String phoneLoc = appContext.getResources().getString(R.string.phoneLocation);
        String sdcardLoc = appContext.getResources().getString(R.string.sdcardLocation);
        if(sharedPreferences.getString(Constants.MEDIA_LOCATION_VIEW_SELECT, phoneLoc).equalsIgnoreCase(phoneLoc)) {
            dcimFc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + appContext.getResources().getString(R.string.FC_ROOT));
        }
        else if(sharedPreferences.getString(Constants.MEDIA_LOCATION_VIEW_SELECT, phoneLoc).equalsIgnoreCase(sdcardLoc)){
            dcimFc = new File(sharedPreferences.getString(Constants.SD_CARD_PATH, ""));
            if(VERBOSE) Log.d(TAG, "SD card path = "+sharedPreferences.getString(Constants.SD_CARD_PATH, ""));
        }
        else{
            //Combine ALL media content
            allLoc = true;
            File phoneMedia = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + appContext.getResources().getString(R.string.FC_ROOT));
            File sdcardMedia = new File(sharedPreferences.getString(Constants.SD_CARD_PATH, ""));
            File[] phonemediaFiles;
            File[] sdcardmediaFiles;
            File[] allMedia = null;
            //Use Streams for Java 1.8 and above
            Stream<File> streamphoneMedia = null;
            Stream<File> streamsdcardMedia = null;
            Stream<File> allStreamMedia;
            //Check for phone media
            phonemediaFiles = getFilesList(phoneMedia);
            //Check for sd card media
            sdcardmediaFiles = getFilesList(sdcardMedia);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if(phonemediaFiles != null && phonemediaFiles.length > 0){
                    streamphoneMedia = Stream.of(phonemediaFiles);
                }
                if(sdcardmediaFiles != null && sdcardmediaFiles.length > 0){
                    streamsdcardMedia = Stream.of(sdcardmediaFiles);
                }
                //Concat both streams
                if(streamphoneMedia != null && streamsdcardMedia!= null) {
                    allStreamMedia = Stream.concat(streamphoneMedia, streamsdcardMedia);
                    allMedia = (File[])allStreamMedia.toArray();
                    mediaList = getSortedList(allMedia);
                }
                else if(streamphoneMedia != null){
                    mediaList = getSortedList((File[])streamphoneMedia.toArray());
                }
                else if(streamsdcardMedia != null){
                    mediaList = getSortedList((File[])streamsdcardMedia.toArray());
                }
                else{
                    mediaList = null;
                }
            }
            else{
                //Iterate phone media
                if(phonemediaFiles != null && sdcardmediaFiles != null) {
                    allMedia = new File[phonemediaFiles.length + sdcardmediaFiles.length];
                    int index = 0;
                    if(phonemediaFiles.length > 0) {
                        for (File media : phonemediaFiles) {
                            allMedia[index++] = media;
                        }
                    }
                    for(File media : sdcardmediaFiles){
                        allMedia[index++] = media;
                    }
                }
            }
        }
        if(!allLoc) {
            File[] mediaFiles = getFilesList(dcimFc);
            if(mediaFiles != null) {
                mediaList = getSortedList(mediaFiles);
            }
            else{
                mediaList = null;
            }
        }
    }

    private static File[] getFilesList(File media){
        File[] mediaFiles = null;
        if(media.exists() && media.isDirectory() && media.listFiles().length > 0){
            mediaFiles = media.listFiles((file) -> {
                if (!file.isDirectory() && (file.getPath().endsWith(appContext.getResources().getString(R.string.IMG_EXT)) ||
                        file.getPath().endsWith(appContext.getResources().getString(R.string.ANOTHER_IMG_EXT)) ||
                        file.getPath().endsWith(appContext.getResources().getString(R.string.VID_EXT)))) {
                    return true;
                }
                return false;
            });
        }
        return mediaFiles;
    }

    private static FileMedia[] getSortedList(File[] mediaFiles){
        ArrayList<FileMedia> mediaArrayList = new ArrayList<>();
        for (int i = 0; i < mediaFiles.length; i++) {
            FileMedia fileMedia = new FileMedia();
            fileMedia.setPath(mediaFiles[i].getPath());
            fileMedia.setLastModified(mediaFiles[i].lastModified());
            mediaArrayList.add(fileMedia);
        }
        Collections.sort(mediaArrayList, new FileMediaLastModifiedComparator());
        mediaList = mediaArrayList.toArray(new FileMedia[mediaArrayList.size()]);
        return mediaList;
    }

    private static boolean doesPathExist(String path){
        sortAsPerLatest();
        for(int i=0;i<mediaList.length;i++){
            if(path.equalsIgnoreCase(mediaList[i].getPath())){
                return true;
            }
        }
        return false;
    }

    private static int getPhotosCount(){
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

    private static int getVideosCount() {
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
