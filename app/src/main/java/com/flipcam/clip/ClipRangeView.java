package com.flipcam.clip;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/** A single contiguous selection over a filmstrip, with two draggable handles. */
public final class ClipRangeView extends View {
    public interface Listener { void changed(long start, long end); }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final List<Bitmap> frames = new ArrayList<>();
    private long duration = 1, start, end = 1;
    private boolean movingStart;
    private Listener listener;

    public ClipRangeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setContentDescription("Clip range: drag the start and end handles");
    }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    public void setListener(Listener listener) { this.listener = listener; }
    public void setRange(long duration, long start, long end) {
        this.duration = Math.max(1, duration); this.start = start; this.end = end; invalidate();
    }
    public void addFrame(Bitmap frame) { frames.add(frame); invalidate(); }
    public void releaseFrames() {
        for (Bitmap frame : frames) frame.recycle();
        frames.clear();
    }
    private float x(long time) { return dp(6) + (getWidth() - dp(12)) * time / duration; }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float top = dp(5), bottom = getHeight() - dp(5), left = dp(6), right = getWidth() - dp(6);
        paint.setColor(0xff292929); canvas.drawRect(left, top, right, bottom, paint);
        float width = (right - left) / 6f;
        for (int i = 0; i < frames.size(); i++) {
            Bitmap frame = frames.get(i);
            int side = Math.min(frame.getWidth(), frame.getHeight());
            int sx = (frame.getWidth() - side) / 2, sy = (frame.getHeight() - side) / 2;
            canvas.drawBitmap(frame, new Rect(sx, sy, sx + side, sy + side),
                    new android.graphics.RectF(left + i * width, top, left + (i+1) * width - dp(1), bottom), paint);
        }
        paint.setColor(0xaa000000);
        canvas.drawRect(left, top, x(start), bottom, paint);
        canvas.drawRect(x(end), top, right, bottom, paint);
        paint.setColor(Color.WHITE); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1));
        canvas.drawRect(x(start), top, x(end), bottom, paint); paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(x(start)-dp(3), dp(1), x(start)+dp(3), getHeight()-dp(1), dp(2), dp(2), paint);
        canvas.drawRoundRect(x(end)-dp(3), dp(1), x(end)+dp(3), getHeight()-dp(1), dp(2), dp(2), paint);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            movingStart = Math.abs(event.getX()-x(start)) < Math.abs(event.getX()-x(end));
            getParent().requestDisallowInterceptTouchEvent(true);
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            performClick(); getParent().requestDisallowInterceptTouchEvent(false);
        } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            getParent().requestDisallowInterceptTouchEvent(false); return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            long time = Math.round((event.getX()-dp(6)) / (getWidth()-dp(12)) * duration / 100) * 100;
            long minimum = Math.min(1000, duration);
            if (movingStart) start = Math.max(0, Math.min(time, end-minimum));
            else end = Math.min(duration, Math.max(time, start+minimum));
            invalidate(); if (listener != null) listener.changed(start, end);
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
