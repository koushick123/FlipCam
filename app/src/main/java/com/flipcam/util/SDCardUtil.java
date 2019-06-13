package com.flipcam.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import com.flipcam.constants.Constants;

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
            if(VERBOSE) Log.d(TAG, "mediaDirs = " + mediaDirs.length);
        }
        for(int i=0;i<mediaDirs.length;i++){
            if(VERBOSE)Log.d(TAG, "external media dir = "+mediaDirs[i]);
            if(mediaDirs[i] != null) {
                try {
                    if (Environment.isExternalStorageRemovable(mediaDirs[i])) {
                        if(VERBOSE)Log.d(TAG, "Removable storage = " + mediaDirs[i]);
                        return mediaDirs[i].getPath();
                    }
                } catch (IllegalArgumentException illegal) {
                    if(VERBOSE)Log.d(TAG, "Not a valid storage device");
                }
            }
        }
        return null;
    }

    //Used to check if SD Card like storage is writable. Some SD Cards can be read-only
    public static boolean isPathWritable(String sdcardpath){
        try {
            String filename = "/doesSDCardExist_"+String.valueOf(System.currentTimeMillis()).substring(0,5);
            sdcardpath += filename;
            final String sdCardFilePath = sdcardpath;
            final FileOutputStream createTestFile = new FileOutputStream(sdcardpath);
            if(VERBOSE)Log.d(TAG, "Able to create file... SD Card exists");
            File testfile = new File(sdCardFilePath);
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
}
