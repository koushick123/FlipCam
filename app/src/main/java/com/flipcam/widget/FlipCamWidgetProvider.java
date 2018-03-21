package com.flipcam.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.flipcam.R;
import com.flipcam.constants.Constants;
import com.flipcam.media.FileMedia;
import com.flipcam.util.MediaUtil;

import java.io.File;
import java.util.HashSet;

/**
 * Created by koushick on 20-Mar-18.
 */

public class FlipCamWidgetProvider extends AppWidgetProvider {

    public static final String TAG = "FCAppWidgetProvider";
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");
        HashSet<String> widgetIds = new HashSet<>();
        SharedPreferences.Editor editor = context.getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE).edit();
        for(int i=0;i<appWidgetIds.length;i++){
            int appWidgetId = appWidgetIds[i];
            widgetIds.add(appWidgetId+"");
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.flipcam_widget);
            FileMedia[] media = MediaUtil.getMediaList(context);
            if(media != null && media.length > 0){
                String filepath = media[0].getPath();
                Log.d(TAG, "FilePath = "+filepath);
                if(filepath.endsWith(context.getResources().getString(R.string.IMG_EXT))
                        || filepath.endsWith(context.getResources().getString(R.string.ANOTHER_IMG_EXT)))
                {
                    Bitmap latestImage = BitmapFactory.decodeFile(filepath);
                    latestImage = Bitmap.createScaledBitmap(latestImage,(int)context.getResources().getDimension(R.dimen.thumbnailWidth),
                            (int)context.getResources().getDimension(R.dimen.thumbnailHeight), false);
                    Log.d(TAG, "Update Photo thumbnail");
                    remoteViews.setViewVisibility(R.id.playCircleWidget, View.INVISIBLE);
                    remoteViews.setImageViewBitmap(R.id.imageWidget, latestImage);
                }
                else{
                    Bitmap vid=null;
                    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                    try {
                        mediaMetadataRetriever.setDataSource(filepath);
                        vid = mediaMetadataRetriever.getFrameAtTime(Constants.FIRST_SEC_MICRO);
                    }catch(RuntimeException runtime){
                        File badFile = new File(filepath);
                        badFile.delete();
                        media = MediaUtil.getMediaList(context);
                        if(media != null && media.length > 0) {
                            mediaMetadataRetriever.setDataSource(filepath);
                            vid = mediaMetadataRetriever.getFrameAtTime(Constants.FIRST_SEC_MICRO);
                        }
                        else{
                            remoteViews.setImageViewResource(R.id.imageWidget, R.drawable.placeholder);
                        }
                    }
                    if(vid != null){
                        vid = Bitmap.createScaledBitmap(vid, (int)context.getResources().getDimension(R.dimen.thumbnailWidth),
                                (int)context.getResources().getDimension(R.dimen.thumbnailHeight), false);
                        Log.d(TAG, "Update Video thumbnail");
                        remoteViews.setViewVisibility(R.id.playCircleWidget, View.VISIBLE);
                        remoteViews.setImageViewBitmap(R.id.imageWidget, vid);
                    }
                }
            }
            else{
                remoteViews.setImageViewResource(R.id.imageWidget, R.drawable.placeholder);
            }
            Log.d(TAG, "Update FC Widget");
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        editor.putStringSet(Constants.WIDGET_IDS, widgetIds);
        editor.commit();
    }

    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "Removed ALL Widgets");
        SharedPreferences.Editor editor = context.getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE).edit();
        editor.putStringSet(Constants.WIDGET_IDS, null);
        editor.commit();
    }
}
