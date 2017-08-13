package com.flipcam.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by koushick on 12-Aug-17.
 */

public class ScreenReceiver extends BroadcastReceiver
{
    public static final String TAG = "ScreenReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"Intent = "+intent.getAction());
        if(intent.getAction().equalsIgnoreCase(Intent.ACTION_USER_PRESENT)){
            Log.d(TAG,"screen unlocked");
        }
        if(intent.getAction().equalsIgnoreCase(Intent.ACTION_SCREEN_OFF)){
            Log.d(TAG,"screen locked");
        }
    }
}
