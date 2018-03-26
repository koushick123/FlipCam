package com.flipcam;

import android.app.Dialog;
import android.app.LoaderManager;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.flipcam.adapter.MediaAdapter;
import com.flipcam.adapter.MediaLoader;
import com.flipcam.constants.Constants;
import com.flipcam.media.FileMedia;
import com.flipcam.util.MediaUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

public class GalleryActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<FileMedia[]>{

    public static final String TAG = "GalleryActivity";
    GridView mediaGrid;
    int scrollPosition = 0;
    SharedPreferences sharedPreferences;
    View warningMsgRoot;
    Dialog warningMsg;
    Button okButton;
    LayoutInflater layoutInflater;
    AppWidgetManager appWidgetManager;
    IntentFilter mediaFilters;
    SDCardEventReceiver sdCardEventReceiver;
    boolean sdCardUnavailWarned = false;
    TextView mediaCount;
    ControlVisbilityPreference controlVisbilityPreference;
    FileMedia[] medias;
    boolean VERBOSE = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(VERBOSE)Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_media_grid);
        sdCardEventReceiver = new SDCardEventReceiver();
        mediaFilters = new IntentFilter();
        getSupportActionBar().setTitle(getResources().getString(R.string.flipCamGallery));
        layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        appWidgetManager = (AppWidgetManager)getSystemService(Context.APPWIDGET_SERVICE);
        warningMsgRoot = layoutInflater.inflate(R.layout.warning_message, null);
        warningMsg = new Dialog(this);
        sharedPreferences = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        mediaCount = (TextView)findViewById(R.id.mediaCount);
        mediaGrid = (GridView) findViewById(R.id.mediaGrid);
        controlVisbilityPreference = (ControlVisbilityPreference)getApplicationContext();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(VERBOSE)Log.d(TAG, "onDestroy");
        controlVisbilityPreference.setMediaSelectedPosition(0);
    }

    private void updateMediaGridFromSource(){
        ImageView noImage = (ImageView) findViewById(R.id.noImage);
        TextView noImageText = (TextView) findViewById(R.id.noImageText);
//        medias = MediaUtil.getMediaList(getApplicationContext());
        if(medias != null && medias.length > 0) {
            noImage.setVisibility(View.GONE);
            noImageText.setVisibility(View.GONE);
            mediaCount.setText(getResources().getString(R.string.galleryCount, MediaUtil.getPhotosCount(), MediaUtil.getVideosCount()));
            MediaAdapter mediaAdapter = new MediaAdapter(getApplicationContext(), medias);
            mediaGrid.setAdapter(mediaAdapter);
            mediaGrid.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {

                }

                @Override
                public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    scrollPosition = firstVisibleItem;
                }
            });
            mediaGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    if(VERBOSE)Log.d(TAG, "onItemSelected = "+position);
                    Intent mediaAct = new Intent(getApplicationContext(), MediaActivity.class);
                    mediaAct.putExtra("mediaPosition",position);
                    mediaAct.putExtra("fromGallery",true);
                    startActivity(mediaAct);
                }
            });
            if(VERBOSE)Log.d(TAG, "selectedMedia Pos = "+controlVisbilityPreference.getMediaSelectedPosition());
            if(controlVisbilityPreference.getMediaSelectedPosition() != -1 && controlVisbilityPreference.getMediaSelectedPosition() < medias.length){
                mediaGrid.setSelection(controlVisbilityPreference.getMediaSelectedPosition());
            }
        }
        else{
            mediaCount.setText(getResources().getString(R.string.galleryCount, 0, 0));
            noImage.setVisibility(View.VISIBLE);
            noImageText.setVisibility(View.VISIBLE);
        }
        updateWidget();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(VERBOSE)Log.d(TAG, "onPause");
        unregisterReceiver(sdCardEventReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(VERBOSE)Log.d(TAG, "onResume");
        mediaFilters.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        mediaFilters.addDataScheme("file");
        registerReceiver(sdCardEventReceiver, mediaFilters);
        if(!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true)) {
            if (doesSDCardExist() == null && !sdCardUnavailWarned) {
                SharedPreferences.Editor settingsEditor = sharedPreferences.edit();
                settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
                settingsEditor.commit();
                sdCardUnavailWarned = true;
                showSDCardUnavailMessage();
                getLoaderManager().initLoader(1, null, this).forceLoad();
            } else {
                getLoaderManager().initLoader(1, null, this).forceLoad();
            }
        }
        else{
            getLoaderManager().initLoader(1, null, this).forceLoad();
        }
    }

    @Override
    public Loader<FileMedia[]> onCreateLoader(int i, Bundle bundle) {
        if(VERBOSE)Log.d(TAG, "onCreateLoader");
        return new MediaLoader(getApplicationContext());
    }

    @Override
    public void onLoadFinished(Loader<FileMedia[]> loader, FileMedia[] fileMedias) {
        if(VERBOSE)Log.d(TAG, "onLoadFinished");
        medias = fileMedias;
        updateMediaGridFromSource();
    }

    @Override
    public void onLoaderReset(Loader<FileMedia[]> loader) {

    }

    class SDCardEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if(VERBOSE)Log.d(TAG, "onReceive = " + intent.getAction());
            if (intent.getAction().equalsIgnoreCase(Intent.ACTION_MEDIA_UNMOUNTED)) {
                //Check if SD Card was selected
                SharedPreferences.Editor settingsEditor = sharedPreferences.edit();
                if (!sharedPreferences.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true) && !sdCardUnavailWarned) {
                    if(VERBOSE)Log.d(TAG, "SD Card Removed");
                    settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
                    settingsEditor.commit();
                    sdCardUnavailWarned = true;
                    showSDCardUnavailMessage();
                    updateMediaGridFromSource();
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(VERBOSE)Log.d(TAG, "scrollPos = "+scrollPosition);
        mediaGrid.setSelection(scrollPosition);
    }

    private String doesSDCardExist(){
        String sdcardpath = sharedPreferences.getString(Constants.SD_CARD_PATH, "");
        try {
            String filename = "/doesSDCardExist_"+String.valueOf(System.currentTimeMillis()).substring(0,5);
            sdcardpath += filename;
            final String sdCardFilePath = sdcardpath;
            final FileOutputStream createTestFile = new FileOutputStream(sdcardpath);
            if(VERBOSE)Log.d(TAG, "Able to create file... SD Card exists");
            File testfile = new File(sdCardFilePath);
            createTestFile.close();
            testfile.delete();
        } catch (FileNotFoundException e) {
            if(VERBOSE)Log.d(TAG, "Unable to create file... SD Card NOT exists..... "+e.getMessage());
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sharedPreferences.getString(Constants.SD_CARD_PATH, "");
    }

    private void showSDCardUnavailMessage(){
        ImageView noImage = (ImageView)findViewById(R.id.noImage);
        noImage.setVisibility(View.VISIBLE);
        TextView noImageText = (TextView)findViewById(R.id.noImageText);
        noImageText.setVisibility(View.VISIBLE);
        SharedPreferences.Editor settingsEditor = sharedPreferences.edit();
        settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM, true);
        settingsEditor.commit();
        TextView warningTitle = (TextView)warningMsgRoot.findViewById(R.id.warningTitle);
        warningTitle.setText(getResources().getString(R.string.sdCardRemovedTitle));
        TextView warningText = (TextView)warningMsgRoot.findViewById(R.id.warningText);
        warningText.setText(getResources().getString(R.string.sdCardNotPresentForRecord));
        okButton = (Button)warningMsgRoot.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                warningMsg.dismiss();
            }
        });
        warningMsg.setContentView(warningMsgRoot);
        warningMsg.setCancelable(false);
        warningMsg.show();
    }

    void updateWidget(){
        HashSet<String> widgetIds = (HashSet)sharedPreferences.getStringSet(Constants.WIDGET_IDS, null);
        if(widgetIds != null && widgetIds.size() > 0){
            Iterator<String> iterator = widgetIds.iterator();
            while(iterator.hasNext()){
                String widgetId = iterator.next();
                if(VERBOSE)Log.d(TAG, "widgetIds = "+widgetId);
                updateAppWidget(Integer.parseInt(widgetId));
            }
        }
    }

    public void updateAppWidget(int appWidgetId) {
        RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.flipcam_widget);
        medias = MediaUtil.getMediaList(this);
        if (medias != null && medias.length > 0) {
            String filepath = medias[0].getPath();
            if(VERBOSE)Log.d(TAG, "FilePath = " + filepath);
            if (filepath.endsWith(getResources().getString(R.string.IMG_EXT))
                    || filepath.endsWith(getResources().getString(R.string.ANOTHER_IMG_EXT))) {
                Bitmap latestImage = BitmapFactory.decodeFile(filepath);
                latestImage = Bitmap.createScaledBitmap(latestImage, (int) getResources().getDimension(R.dimen.thumbnailWidth),
                        (int) getResources().getDimension(R.dimen.thumbnailHeight), false);
                if(VERBOSE)Log.d(TAG, "Update Photo thumbnail");
                remoteViews.setViewVisibility(R.id.playCircleWidget, View.INVISIBLE);
                remoteViews.setImageViewBitmap(R.id.imageWidget, latestImage);
                remoteViews.setTextViewText(R.id.widgetMsg, getResources().getString(R.string.widgetMediaMsg));
            } else {
                Bitmap vid = null;
                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                try {
                    mediaMetadataRetriever.setDataSource(filepath);
                    vid = mediaMetadataRetriever.getFrameAtTime(Constants.FIRST_SEC_MICRO);
                } catch (RuntimeException runtime) {
                    File badFile = new File(filepath);
                    badFile.delete();
                    medias = MediaUtil.getMediaList(this);
                    if (medias != null && medias.length > 0) {
                        mediaMetadataRetriever.setDataSource(filepath);
                        vid = mediaMetadataRetriever.getFrameAtTime(Constants.FIRST_SEC_MICRO);
                    } else {
                        remoteViews.setImageViewResource(R.id.imageWidget, R.drawable.placeholder);
                        remoteViews.setViewVisibility(R.id.playCircleWidget, View.INVISIBLE);
                        remoteViews.setTextViewText(R.id.widgetMsg, getResources().getString(R.string.widgetNoMedia));
                    }
                }
                if (vid != null) {
                    vid = Bitmap.createScaledBitmap(vid, (int) getResources().getDimension(R.dimen.thumbnailWidth),
                            (int) getResources().getDimension(R.dimen.thumbnailHeight), false);
                    if(VERBOSE)Log.d(TAG, "Update Video thumbnail");
                    remoteViews.setViewVisibility(R.id.playCircleWidget, View.VISIBLE);
                    remoteViews.setImageViewBitmap(R.id.imageWidget, vid);
                    remoteViews.setTextViewText(R.id.widgetMsg, getResources().getString(R.string.widgetMediaMsg));
                }
            }
        } else {
            if(VERBOSE)Log.d(TAG, "List empty");
            //List is now empty
            remoteViews.setImageViewResource(R.id.imageWidget, R.drawable.placeholder);
            remoteViews.setViewVisibility(R.id.playCircleWidget, View.INVISIBLE);
            remoteViews.setTextViewText(R.id.widgetMsg, getResources().getString(R.string.widgetNoMedia));
        }
        if(VERBOSE)Log.d(TAG, "Update FC Widget");
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }
}
