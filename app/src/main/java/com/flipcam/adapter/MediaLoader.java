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

    public MediaLoader(Context ctx){
        super(ctx);
        context = ctx;
    }

    @Override
    public FileMedia[] loadInBackground() {
        return MediaUtil.getMediaList(context);
    }
}
