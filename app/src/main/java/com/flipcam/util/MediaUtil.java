package com.flipcam.util;

import android.content.Context;
import android.os.Environment;

import com.flipcam.R;
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

    private static FileMedia[] mediaList;
    private static Context appContext;
    public static FileMedia[] getMediaList(Context ctx){
        appContext = ctx;
        sortAsPerLatest();
        return mediaList;
    }

    private static void sortAsPerLatest() {
        File dcimFc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + appContext.getResources().getString(R.string.FC_ROOT));
        if (dcimFc.exists() && dcimFc.isDirectory() && dcimFc.listFiles().length > 0) {
            File[] mediaFiles = dcimFc.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    if (!file.isDirectory()) {
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
}
