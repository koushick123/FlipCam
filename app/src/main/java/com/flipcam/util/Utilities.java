package com.flipcam.util;

import android.content.Context;
import android.os.Environment;

import com.flipcam.R;
import com.flipcam.media.FileMedia;
import com.flipcam.media.FileMediaLastModifiedComparator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by koushick on 23-Nov-17.
 */

public class Utilities {

    private static FileMedia[] mediaList;
    private static Context appContext;
    public static FileMedia[] getMediaList(Context ctx){
        appContext = ctx;
        sortAsPerLatest();
        return mediaList;
    }

    private static void sortAsPerLatest(){
        File[] mediaFiles = Environment.getExternalStoragePublicDirectory
                (Environment.DIRECTORY_DCIM + appContext.getResources().getString(R.string.FC_ROOT)).listFiles();
        ArrayList<FileMedia> mediaArrayList = new ArrayList<>();
        for(int i=0;i<mediaFiles.length;i++){
            FileMedia fileMedia = new FileMedia();
            fileMedia.setPath(mediaFiles[i].getPath());
            fileMedia.setLastModified(mediaFiles[i].lastModified());
            mediaArrayList.add(fileMedia);
        }
        Collections.sort(mediaArrayList,new FileMediaLastModifiedComparator());
        mediaList = mediaArrayList.toArray(new FileMedia[mediaArrayList.size()]);
    }
}
