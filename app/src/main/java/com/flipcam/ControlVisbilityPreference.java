package com.flipcam;

import android.app.Application;

import java.io.Serializable;

/**
 * Created by koushick on 10-Nov-17.
 */

public class ControlVisbilityPreference extends Application implements Serializable{

    private boolean hideControl;
    private int mediaSelectedPosition;
    private int brightnessLevel;

    public int getBrightnessLevel() {
        return brightnessLevel;
    }

    public void setBrightnessLevel(int brightnessLevel) {
        this.brightnessLevel = brightnessLevel;
    }

    public int getMediaSelectedPosition() {
        return mediaSelectedPosition;
    }

    public void setMediaSelectedPosition(int mediaSelectedPosition) {
        this.mediaSelectedPosition = mediaSelectedPosition;
    }

    public boolean isHideControl() {
        return hideControl;
    }

    public void setHideControl(boolean hideControl) {
        this.hideControl = hideControl;
    }
}
