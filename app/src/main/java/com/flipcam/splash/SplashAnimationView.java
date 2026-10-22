package com.flipcam.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.flipcam.R;

/** Matches the approved preview: fixed lens, rotating arrows, then a live-camera fade. */
public final class SplashAnimationView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path circle = new Path();
    private final Path arrowRing = new Path();
    private final RectF iconBounds = new RectF();
    private final Bitmap artwork;
    private final String title;
    private final int durationMs;
    private RadialGradient background;
    private float progress;
    private ValueAnimator animator;
    private boolean cancelled;

    public SplashAnimationView(Context context, int durationMs) {
        super(context);
        this.durationMs = durationMs;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        artwork = BitmapFactory.decodeResource(getResources(), R.drawable.flip_lens_artwork, options);
        title = getResources().getString(R.string.splash_title);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textPaint.setTextSize(20 * getResources().getDisplayMetrics().scaledDensity);
    }

    public void start(Runnable completed) {
        post(() -> {
            if (!isAttachedToWindow()) return;
            animator = ValueAnimator.ofFloat(0, 1);
            animator.setDuration(durationMs);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(value -> {
                progress = (float) value.getAnimatedValue();
                invalidate();
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationCancel(Animator animation) { cancelled = true; }
                @Override public void onAnimationEnd(Animator animation) {
                    if (!cancelled) completed.run();
                }
            });
            animator.start();
        });
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float cx = w / 2f, cy = h * .43f;
        float size = Math.min(w * .578f, 208 * getResources().getDisplayMetrics().density);
        iconBounds.set(cx-size/2, cy-size/2, cx+size/2, cy+size/2);
        circle.reset(); circle.addCircle(cx, cy, size/2, Path.Direction.CW);
        arrowRing.reset(); arrowRing.setFillType(Path.FillType.EVEN_ODD);
        arrowRing.addCircle(cx, cy, size*.432f, Path.Direction.CW);
        arrowRing.addCircle(cx, cy, size*.242f, Path.Direction.CW);
        background = new RadialGradient(cx, cy, Math.max(1, h*.64f),
                new int[]{0xFF171B21, 0xFF0B0E12, 0xFF06080B},
                new float[]{0, .6f, 1}, Shader.TileMode.CLAMP);
    }

    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    private static float ease(float t) {
        return t < .5f ? 4*t*t*t : 1-(float)Math.pow(-2*t+2, 3)/2;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float fade = 1-ease(clamp((progress-.81f)/.19f));
        float reveal = clamp(progress/.11f);
        int layer = canvas.saveLayerAlpha(0, 0, getWidth(), getHeight(), Math.round(255*fade));
        paint.setShader(background); canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(null); paint.setAlpha(Math.round(255*reveal));
        float cx = iconBounds.centerX(), cy = iconBounds.centerY();
        int logo = canvas.save(); canvas.clipPath(circle);
        canvas.drawBitmap(artwork, null, iconBounds, paint);
        int arrows = canvas.save(); canvas.clipPath(arrowRing);
        canvas.rotate(360*ease(clamp((progress-.1f)/.7f)), cx, cy);
        canvas.drawBitmap(artwork, null, iconBounds, paint);
        canvas.restoreToCount(arrows); canvas.restoreToCount(logo);
        textPaint.setAlpha(Math.round(255*reveal));
        float labelY = cy + iconBounds.width()*150/208;
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        canvas.drawText(title, cx, labelY-(metrics.ascent+metrics.descent)/2, textPaint);
        paint.setAlpha(255);
        canvas.restoreToCount(layer);
    }

    @Override protected void onDetachedFromWindow() {
        cancelled = true;
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }
}
