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
    static boolean VERBOSE = true;
    static boolean fromGallery = false;
    public static FileMedia[] getMediaList(Context ctx, boolean fromGal){
        appContext = ctx;
        fromGallery = fromGal;
        if(fromGallery) {
            sortAsPerLatestForGallery();
        }
        else{
            sortAsPerLatest();
        }
        return mediaList;
    }

    private static void sortAsPerLatestForGallery() {
        File dcimFc = null;
        boolean allLoc = false;
        SharedPreferences sharedPreferences = appContext.getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        String phoneLoc = appContext.getResources().getString(R.string.phoneLocation);
        String sdcardLoc = appContext.getResources().getString(R.string.sdcardLocation);
        if(sharedPreferences.getString(Constants.MEDIA_LOCATION_VIEW_SELECT, phoneLoc).equalsIgnoreCase(phoneLoc)) {
            dcimFc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + appContext.getResources().getString(R.string.FC_ROOT));
            Log.d(TAG, "PHONE For Gallery");
        }
        else if(sharedPreferences.getString(Constants.MEDIA_LOCATION_VIEW_SELECT, phoneLoc).equalsIgnoreCase(sdcardLoc)){
            dcimFc = new File(sharedPreferences.getString(Constants.SD_CARD_PATH, ""));
            if(VERBOSE) Log.d(TAG, "SD card path For Gallery = "+
                    sharedPreferences.getString(Constants.SD_CARD_PATH, ""));
        }
        else{
            //Combine ALL media content
            allLoc = true;
            File phoneMedia = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + appContext.getResources().getString(R.string.FC_ROOT));
            File sdcardMedia = new File(sharedPreferences.getString(Constants.SD_CARD_PATH, ""));
            File[] phonemediaFiles;
            File[] sdcardmediaFiles = null;
            //Check for phone media
            Log.d(TAG, "sdcardMedia = "+sdcardMedia);
            if(sdcardMedia != null) {
                Log.d(TAG, "sdcardMedia abs path = " + sdcardMedia.getAbsolutePath());
                Log.d(TAG, "sdcardMedia name = " + sdcardMedia.getName());
            }
            phonemediaFiles = getFilesList(phoneMedia);
            //Check for sd card media
            if(sdcardMedia!=null && !sdcardMedia.getName().trim().equalsIgnoreCase(Constants.EMPTY)) {
                sdcardmediaFiles = getFilesList(sdcardMedia);
            }

            if(phonemediaFiles != null && sdcardmediaFiles != null) {
                concatAllMedia(phonemediaFiles, sdcardmediaFiles);
            }
            else{
                if(phonemediaFiles != null && phonemediaFiles.length > 0){
                    Log.d(TAG, "Stream Phone");
                    mediaList = getSortedList(phonemediaFiles);
                }
                else if(sdcardmediaFiles != null && sdcardmediaFiles.length > 0){
                    Log.d(TAG, "Stream SD Card");
                    mediaList = getSortedList(sdcardmediaFiles);
                }
                else {
                    mediaList = null;
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

    private static void concatAllMedia(File[] phonemediaFiles, File[] sdcardmediaFiles){
        File[] allMedia;
        //Iterate phone media
        int allMediaCount = 0;
        if(phonemediaFiles != null) {
            allMediaCount += phonemediaFiles.length;
        }
        if(sdcardmediaFiles != null){
            allMediaCount += sdcardmediaFiles.length;
        }
        Log.d(TAG, "allMediaCount = "+allMediaCount);
        allMedia = new File[allMediaCount];
        int index=0;
        if(phonemediaFiles != null){
            for(File phMed : phonemediaFiles){
                allMedia[index++] = phMed;
            }
        }
        if(sdcardmediaFiles != null){
            for(File sdcdMedia : sdcardmediaFiles){
                allMedia[index++] = sdcdMedia;
            }
        }
        if(allMedia != null) {
            Log.d(TAG, "allMedia length = "+allMedia.length);
            mediaList = getSortedList(allMedia);
        }
        else{
            mediaList = null;
        }
    }

    private static void sortAsPerLatest() {
        File dcimFc;
        SharedPreferences sharedPreferences = appContext.getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        if(sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)) {
            dcimFc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + appContext.getResources().getString(R.string.FC_ROOT));
            Log.d(TAG, "PHONE");
        }
        else{
            dcimFc = new File(sharedPreferences.getString(Constants.SD_CARD_PATH, ""));
            if(VERBOSE) Log.d(TAG, "SD card path = "+sharedPreferences.getString(Constants.SD_CARD_PATH, ""));
        }
        File[] mediaFiles = getFilesList(dcimFc);
        if(mediaFiles != null) {
            mediaList = getSortedList(mediaFiles);
        }
        else{
            mediaList = null;
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

    public static boolean deleteFile(FileMedia media) {
        File deleteFile = new File(media.getPath());
        return deleteFile.delete();
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
