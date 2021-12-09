package com.flipcam.media;

/**
 * Created by koushick on 23-Nov-17.
 */

public class FileMedia implements Comparable<FileMedia>{

    String path;
    long lastModified;

    public String getPath() {
        return path;
    }

    public void setPath(String fileName) {
        this.path = fileName;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public int compareTo(FileMedia fileMedia) {
        if(getLastModified() < fileMedia.getLastModified()){
            return 1;
        }
        else if(getLastModified() > fileMedia.getLastModified()){
            return -1;
        }
        else{
            return 0;
        }
    }
}
