package com.flipcam.media;

import java.util.Comparator;

/**
 * Created by koushick on 23-Nov-17.
 */

public class FileMediaLastModifiedComparator implements Comparator<FileMedia> {
    @Override
    public int compare(FileMedia first, FileMedia next) {
        if(first.getLastModified() < next.getLastModified()){
            return -1;
        }
        else if(first.getLastModified() > next.getLastModified()){
            return 1;
        }
        else{
            return 0;
        }
    }
}
