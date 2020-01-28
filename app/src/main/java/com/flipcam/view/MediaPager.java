package com.flipcam.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.flipcam.MediaActivity;

public class MediaPager extends ViewPager {

    public static final String TAG = "MediaPager";
    Context appContext;
    MediaActivity mediaActivity;

    public MediaPager(@NonNull Context context, AttributeSet attributeSet) {
        super(context);
        appContext = context;
    }

    public void setMediaActivity(MediaActivity mediaActivity1){
        mediaActivity = mediaActivity1;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d(TAG, "onInterceptTouchEvent");
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.d(TAG, "onTouchEvent");
        return true;
    }
}
