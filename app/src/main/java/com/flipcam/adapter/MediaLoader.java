package com.flipcam.adapter;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.flipcam.media.FileMedia;
import com.flipcam.util.MediaUtil;

/**
 * Created by Koushick on 26-03-2018.
 */

public class MediaLoader extends AsyncTaskLoader<FileMedia[]> {

    Context context;
    boolean fromGallery;

    public MediaLoader(Context ctx, boolean fromGal){
        super(ctx);
        context = ctx;
        fromGallery = fromGal;
    }

    @Override
    public FileMedia[] loadInBackground() {
        return MediaUtil.getMediaList(context, fromGallery);
    }
}
