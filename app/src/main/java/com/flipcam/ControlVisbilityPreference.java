package com.flipcam;

import android.app.Application;

import java.io.Serializable;

/**
 * Created by koushick on 10-Nov-17.
 */

public class ControlVisbilityPreference extends Application implements Serializable{

    private boolean hideControl;

    public boolean isHideControl() {
        return hideControl;
    }

    public void setHideControl(boolean hideControl) {
        this.hideControl = hideControl;
    }
}
