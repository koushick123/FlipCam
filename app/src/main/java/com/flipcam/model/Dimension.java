package com.flipcam.model;

import android.support.annotation.NonNull;

public class Dimension implements Comparable<Dimension>{

    public Dimension(int w, int h){
        width = w;
        height = h;
    }

    private int width;
    private int height;

    //Sort in descending order to display the largest resolution first.
    @Override
    public int compareTo(@NonNull Dimension dimension) {
        if(width > dimension.getWidth()){
            return -1;
        }
        else if(width < dimension.getWidth()){
            return 1;
        }
        else{
            if(height > dimension.getHeight()){
                return -1;
            }
            else if(height < dimension.getHeight()){
                return 1;
            }
            return 0;
        }
    }

    @Override
    public String toString() {
        return width+" X "+height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
