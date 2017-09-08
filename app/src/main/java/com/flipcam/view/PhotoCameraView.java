package com.flipcam.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by Koushick on 08-09-2017.
 */

public class PhotoCameraView extends SurfaceView implements SurfaceHolder.Callback,SurfaceTexture.OnFrameAvailableListener {

    public PhotoCameraView(Context context, AttributeSet attributeSet)
    {
        super(context,attributeSet);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
}
