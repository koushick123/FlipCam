package com.flipcam.clip;

import android.content.Context;
import android.content.SharedPreferences;
import com.flipcam.constants.Constants;
import com.flipcam.util.MediaStorage;
import java.io.File;

/** Keeps the latest original separate from exports and from the current storage selection. */
public final class LatestRecordingStore {
    private static final String PREFS = "FlipCamClips";
    private LatestRecordingStore() { }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static File latest(Context context) {
        SharedPreferences prefs = prefs(context);
        if (!prefs.contains("original")) {
            File newest = newestIn(MediaStorage.getPhoneMediaDirectory(context), null);
            String sd = context.getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE)
                    .getString(Constants.SD_CARD_PATH, "");
            if (!sd.isEmpty()) newest = newestIn(new File(sd), newest);
            prefs.edit().putString("original", newest == null ? "" : newest.getAbsolutePath()).apply();
        }
        return readable(prefs.getString("original", ""));
    }

    private static File newestIn(File folder, File newest) {
        File[] files = folder.listFiles();
        if (files != null) for (File file : files) {
            if (file.isFile() && file.length() > 0 && file.getName().startsWith("FC_VID_")
                    && file.getName().endsWith(".mp4")
                    && (newest == null || file.lastModified() > newest.lastModified())) newest = file;
        }
        return newest;
    }

    private static File readable(String path) {
        if (path == null || path.isEmpty()) return null;
        File file = new File(path);
        return file.isFile() && file.canRead() && file.length() > 0 ? file : null;
    }

    public static void recorded(Context context, File file) {
        SharedPreferences prefs = prefs(context);
        if (!file.getAbsolutePath().equals(prefs.getString("original", ""))) {
            prefs.edit().putString("original", file.getAbsolutePath()).remove("clip")
                    .remove("clipStart").remove("clipEnd").apply();
        }
    }

    public static File clip(Context context) {
        return latest(context) == null ? null : readable(prefs(context).getString("clip", ""));
    }

    public static long clipStart(Context context) { return prefs(context).getLong("clipStart", 0); }
    public static long clipEnd(Context context) { return prefs(context).getLong("clipEnd", 0); }

    public static boolean saveClip(Context context, File original, File clip, long start, long end) {
        File current = latest(context);
        if (current == null || !current.equals(original)) return false;
        return prefs(context).edit().putString("clip", clip.getAbsolutePath())
                .putLong("clipStart", start).putLong("clipEnd", end).commit();
    }
}
