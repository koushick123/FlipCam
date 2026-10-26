package com.flipcam.splash;

import android.app.Activity;
import android.view.View;
import android.widget.FrameLayout;

import com.flipcam.R;

/** Launch-only overlay; it never changes camera surfaces or their layout. */
public final class LaunchSplash {
    public static final String EXTRA_FRESH_LAUNCH = "com.flipcam.FRESH_LAUNCH";

    private LaunchSplash() { }

    public static void show(Activity activity) {
        int duration = activity.getResources().getInteger(R.integer.splash_duration_ms);
        if (duration <= 0) return;
        FrameLayout root = activity.findViewById(R.id.parentCamera);
        SplashAnimationView overlay = new SplashAnimationView(activity, duration);
        overlay.setId(R.id.launchSplashOverlay);
        overlay.setElevation(activity.getResources().getDisplayMetrics().density * 100);
        overlay.setClickable(true);
        overlay.setContentDescription(activity.getString(R.string.splash_title));
        overlay.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        root.addView(overlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        overlay.start(() -> root.removeView(overlay));
    }
}
