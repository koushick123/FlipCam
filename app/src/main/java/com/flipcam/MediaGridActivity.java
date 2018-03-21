package com.flipcam;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.GridView;

import com.flipcam.adapter.MediaAdapter;
import com.flipcam.util.MediaUtil;

public class MediaGridActivity extends AppCompatActivity {

    public static final String TAG = "MediaGridActivity";
    GridView mediaGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_grid);
        mediaGrid = (GridView)findViewById(R.id.mediaGrid);
        Log.d(TAG, "onCreate");
        MediaAdapter mediaAdapter = new MediaAdapter(getApplicationContext(), MediaUtil.getMediaList(getApplicationContext()));
        mediaGrid.setAdapter(mediaAdapter);
    }
}
