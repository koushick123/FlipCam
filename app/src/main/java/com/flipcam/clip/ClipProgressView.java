package com.flipcam.clip;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public final class ClipProgressView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int progress = -1;
    public ClipProgressView(Context context, AttributeSet attrs) { super(context, attrs); }
    public void setProgress(int value) {
        progress = value; setContentDescription(value < 0 ? "Preparing export" : value + "% exported"); invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
        float size = Math.min(getWidth(), getHeight()), inset = size*.05f;
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(size*.045f); paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(0xff333333); canvas.drawCircle(getWidth()/2f, getHeight()/2f, size/2-inset, paint);
        paint.setColor(0xffffffff);
        canvas.drawArc(inset, inset, size-inset, size-inset, -90, progress < 0 ? 35 : 3.6f*progress, false, paint);
        paint.setStyle(Paint.Style.FILL); paint.setTextSize(size*.2f); paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        String text = progress < 0 ? "…" : progress + "%";
        canvas.drawText(text, getWidth()/2f, getHeight()/2f-(paint.ascent()+paint.descent())/2f, paint);
    }
}
