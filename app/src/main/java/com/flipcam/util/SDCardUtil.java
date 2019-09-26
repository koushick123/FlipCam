package com.flipcam.util;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import com.flipcam.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
/*
This class is used to perform all SD Card related activities.
 */
public class SDCardUtil {

    private static final String TAG = "SDCardUtil";
    private static boolean VERBOSE = true;

    //Used to check if external removable storage (like an SDCard exists)
    public static String doesSDCardExist(Context context){
        File[] mediaDirs = context.getExternalMediaDirs();
        if(mediaDirs != null) {
            if (VERBOSE) Log.d(TAG, "mediaDirs = " + mediaDirs.length);
            for (int i = 0; i < mediaDirs.length; i++) {
                if (VERBOSE) Log.d(TAG, "external media dir = " + mediaDirs[i]);
                if (mediaDirs[i] != null) {
                    try {
                        if (Environment.isExternalStorageRemovable(mediaDirs[i])) {
                            if (VERBOSE) Log.d(TAG, "Removable storage = " + mediaDirs[i]);
                            return mediaDirs[i].getPath();
                        }
                    } catch (IllegalArgumentException illegal) {
                        if (VERBOSE) Log.d(TAG, "Not a valid storage device");
                    }
                }
            }
        }
        return null;
    }

    //Used to check if SD Card like storage is writable. Some SD Cards can be read-only
    public static boolean isPathWritable(String sdcardpath){
        try {
            String filename = "/doesSDCardExist_"+String.valueOf(System.currentTimeMillis()).substring(0,5);
            StringBuffer sbsdcardpath = new StringBuffer(sdcardpath);
            sbsdcardpath.append(filename);
            final FileOutputStream createTestFile = new FileOutputStream(sbsdcardpath.toString());
            if(VERBOSE)Log.d(TAG, "Able to create file... SD Card exists");
            if(VERBOSE)Log.d(TAG, "SD Card PATH = "+sbsdcardpath.toString());
            File testfile = new File(sbsdcardpath.toString());
            createTestFile.close();
            testfile.delete();
        } catch (FileNotFoundException e) {
            if(VERBOSE)Log.d(TAG, "Unable to create file... SD Card NOT exists..... "+e.getMessage());
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    //Check if the com.flipcam folder that is created for FlipCam application exists or was removed.
    public static boolean doesSDCardFlipCamFolderExist(String sdCardPath){
        if(VERBOSE)Log.d(TAG, "doesSDCardFlipCamFolderExist");
        if(VERBOSE)Log.d(TAG, "SD Card Path = "+sdCardPath);
        File flipCamFolder = new File(sdCardPath);
        if(flipCamFolder.exists() && flipCamFolder.isDirectory()){
            //The flipcam folder exists.
            return true;
        }
        else{
            return false;
        }
    }

    //Check if the com.flipcam folder that is created is empty or contains media.
    public static boolean doesSDCardFlipCamFolderContainMedia(String sdCardPath, Context context){
        if(VERBOSE)Log.d(TAG, "doesSDCardFlipCamFolderContainMedia");
        if(VERBOSE)Log.d(TAG, "SD Card Path = "+sdCardPath);
        Resources resources = context.getResources();
        File flipCamFolder = new File(sdCardPath);
        return (flipCamFolder.list((dir, name) -> {
            if(name.endsWith(resources.getString(R.string.VID_EXT)) || name.endsWith(resources.getString(R.string.IMG_EXT))
            || name.endsWith(resources.getString(R.string.ANOTHER_IMG_EXT))){
                return true;
            }
            else{
                return false;
            }
        })).length > 0;
    }
}
