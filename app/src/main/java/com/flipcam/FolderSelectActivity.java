package com.flipcam;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class FolderSelectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_select);
        getSupportActionBar().setTitle(getResources().getString(R.string.folderSelectTitle));
    }
}
