package com.flipcam.media;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by koushick on 21-Nov-17.
 */

public class Media implements Parcelable {

    private boolean mediaPlaying;
    private int mediaPosition;
    private boolean mediaControlsHide;
    private String mediaActualDuration;
    private int seekDuration;
    private boolean mediaCompleted;
    private int mediaPreviousPos;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if(mediaPlaying) {
            parcel.writeInt(0);
        }
        else{
            parcel.writeInt(1);
        }
        parcel.writeInt(mediaPosition);
        if(mediaControlsHide){
            parcel.writeInt(0);
        }
        else{
            parcel.writeInt(1);
        }
        parcel.writeString(mediaActualDuration);
        parcel.writeInt(seekDuration);
        if(mediaCompleted){
            parcel.writeInt(0);
        }
        else{
            parcel.writeInt(1);
        }
        parcel.writeInt(mediaPreviousPos);
    }

    public static final Parcelable.Creator<Media> CREATOR
            = new Parcelable.Creator<Media>() {
        public Media createFromParcel(Parcel in) {
            return new Media(in);
        }

        public Media[] newArray(int size) {
            return new Media[size];
        }
    };

    public Media(){

    }
    private Media(Parcel in){
        mediaPlaying = in.readInt() == 0 ? true : false;
        mediaPosition = in.readInt();
        mediaControlsHide = in.readInt() == 0 ? true : false;
        mediaActualDuration = in.readString();
        seekDuration = in.readInt();
        mediaCompleted = in.readInt() == 0 ? true : false;
        mediaPreviousPos = in.readInt();
    }

    public boolean isMediaPlaying() {
        return mediaPlaying;
    }

    public void setMediaPlaying(boolean mediaPlaying) {
        this.mediaPlaying = mediaPlaying;
    }

    public int getMediaPosition() {
        return mediaPosition;
    }

    public void setMediaPosition(int mediaPosition) {
        this.mediaPosition = mediaPosition;
    }

    public boolean isMediaControlsHide() {
        return mediaControlsHide;
    }

    public void setMediaControlsHide(boolean mediaControlsHide) {
        this.mediaControlsHide = mediaControlsHide;
    }

    public String getMediaActualDuration() {
        return mediaActualDuration;
    }

    public void setMediaActualDuration(String mediaActualDuration) {
        this.mediaActualDuration = mediaActualDuration;
    }

    public int getSeekDuration() {
        return seekDuration;
    }

    public void setSeekDuration(int seekDuration) {
        this.seekDuration = seekDuration;
    }

    public boolean isMediaCompleted() {
        return mediaCompleted;
    }

    public void setMediaCompleted(boolean mediaCompleted) {
        this.mediaCompleted = mediaCompleted;
    }

    public int getMediaPreviousPos() {
        return mediaPreviousPos;
    }

    public void setMediaPreviousPos(int mediaPreviousPos) {
        this.mediaPreviousPos = mediaPreviousPos;
    }
}
