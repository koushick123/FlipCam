package com.flipcam;

import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FolderSelectActivity extends AppCompatActivity {

    public static final String TAG = "FolderSelectActivity";
    @BindView(R.id.phone_folder)
    ImageView phone_folder;
    @BindView(R.id.sdcard_folder)
    ImageView sdcard_folder;
    @BindView(R.id.both_folder)
    ImageView both_folder;
    Drawable folder_circle;
    Drawable folder_no_circle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_select);
        ButterKnife.bind(this);
        folder_circle = getResources().getDrawable(R.drawable.folder_border_circle);
        folder_no_circle = getResources().getDrawable(R.drawable.folder_border_no_circle);
        getSupportActionBar().setTitle(getResources().getString(R.string.folderSelectTitle));
        phone_folder.setOnTouchListener((view, mEvent) -> {
            Log.d(TAG, "EVENT phone_folder = "+mEvent.getAction());
            switch (mEvent.getAction()){
                case  MotionEvent.ACTION_DOWN:
                case  MotionEvent.ACTION_POINTER_DOWN:
                    phone_folder.setBackground(folder_circle);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    phone_folder.setBackground(folder_no_circle);
                    break;
            }
            return true;
        });
        sdcard_folder.setOnTouchListener((view, mEvent) -> {
            Log.d(TAG, "EVENT sdcard_folder = "+mEvent.getAction());
            switch (mEvent.getAction()){
                case  MotionEvent.ACTION_DOWN:
                case  MotionEvent.ACTION_POINTER_DOWN:
                    sdcard_folder.setBackground(folder_circle);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    sdcard_folder.setBackground(folder_no_circle);
                    break;
            }
            return true;
        });
        both_folder.setOnTouchListener((view, mEvent) -> {
            Log.d(TAG, "EVENT both_folder = "+mEvent.getAction());
            switch (mEvent.getAction()){
                case  MotionEvent.ACTION_DOWN:
                case  MotionEvent.ACTION_POINTER_DOWN:
                    both_folder.setBackground(folder_circle);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    both_folder.setBackground(folder_no_circle);
                    break;
            }
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.folder_view_fade_in, R.anim.folder_view_fade_out);
    }
}
