package com.flipcam.media;

/**
 * Created by koushick on 23-Nov-17.
 */

public class FileMedia{

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
}
