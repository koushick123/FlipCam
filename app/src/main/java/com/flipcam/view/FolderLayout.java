package com.flipcam.view;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.flipcam.GalleryActivity;
import com.flipcam.MediaActivity;
import com.flipcam.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FolderLayout extends LinearLayout {

    public static final String TAG = "FolderLayout";
    @BindView(R.id.displayLabel)
    TextView displayLabel;
    TypedArray attr;
    @BindView(R.id.mediaFolder)
    ImageView mediaFolder;
    Drawable folder_circle;
    Drawable folder_no_circle;
    MediaActivity mediaActivity;

    public FolderLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        Log.d(TAG, "FolderLayout");
        folder_circle = getResources().getDrawable(R.drawable.folder_border_circle);
        folder_no_circle = getResources().getDrawable(R.drawable.folder_border_no_circle);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.custom_folder, this, true);
        ButterKnife.bind(this);
        attr = context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.FolderLayout, 0, 0);
        String dispText = attr.getString(R.styleable.FolderLayout_displayText);
        Drawable dispImage = attr.getDrawable(R.styleable.FolderLayout_displayImage);
        Log.d(TAG, "dispText = "+dispText);
        displayLabel.setText(dispText);
        mediaFolder.setImageDrawable(dispImage);
        attr.recycle();
        mediaFolder.setOnTouchListener((view, mEvent) -> {
            switch (mEvent.getAction()){
                case  MotionEvent.ACTION_DOWN:
                case  MotionEvent.ACTION_POINTER_DOWN:
                    mediaFolder.setBackground(folder_circle);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mediaFolder.setBackground(folder_no_circle);
                    Log.d(TAG, "DispLabel = "+displayLabel.getText());
                    mediaActivity.getMediaLocation().dismiss();
                    mediaActivity.goToGallery(displayLabel.getText().toString());
                    break;
            }
            return true;
        });
    }

    public void setMediaActivity(MediaActivity mediaAct){
        mediaActivity = mediaAct;
    }


}
