package com.flipcam.util;

import android.app.Activity;
import android.os.Build;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.RequiresApi;

/** Bridges Android 16's system Back event to the activity's existing navigation policy. */
public final class BackNavigation {
    private BackNavigation() { }

    public static void register(Activity activity) {
        // Older releases still dispatch the legacy override. Keep that path unchanged.
        if (Build.VERSION.SDK_INT >= 36) Api36.register(activity);
    }

    @RequiresApi(36)
    private static final class Api36 {
        static void register(Activity activity) {
            // The window owns this callback; its lifetime ends with the activity.
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, activity::onBackPressed);
        }
    }
}
