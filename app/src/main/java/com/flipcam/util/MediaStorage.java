package com.flipcam.util;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.flipcam.R;

import java.io.File;

/** Resolves FlipCam's phone-media folder without requiring broad storage access. */
public final class MediaStorage {
    private MediaStorage() { }

    public static File getPhoneMediaDirectory(Context context) {
        File directory;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File[] mediaDirectories = context.getExternalMediaDirs();
            File mediaRoot = mediaDirectories != null && mediaDirectories.length > 0 && mediaDirectories[0] != null
                    ? mediaDirectories[0] : context.getExternalFilesDir(Environment.DIRECTORY_DCIM);
            directory = new File(mediaRoot != null ? mediaRoot : context.getFilesDir(),
                    context.getString(R.string.FC_ROOT).replace("/", ""));
        } else {
            directory = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM + context.getString(R.string.FC_ROOT));
        }
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }
}
