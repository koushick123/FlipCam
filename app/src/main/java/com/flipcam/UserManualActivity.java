package com.flipcam;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.flipcam.adapter.UserManualAdapter;
import com.flipcam.usermanual.FCGridViewScreen;
import com.flipcam.usermanual.FCMediaChooseScreen;
import com.flipcam.usermanual.FCMediaPhotoScreen;
import com.flipcam.usermanual.FCMediaVideoScreen;
import com.flipcam.usermanual.FCPhotoScreen;
import com.flipcam.usermanual.FCSettingsSDCardScreen;
import com.flipcam.usermanual.FCSettingsScreen;
import com.flipcam.usermanual.FCUserManualExitScreen;
import com.flipcam.usermanual.FCUserManualWelcomeScreen;
import com.flipcam.usermanual.FCVideoRecordingPauseScreen;
import com.flipcam.usermanual.FCVideoRecordingScreen;
import com.flipcam.usermanual.FCVideoRecordingScreen2;
import com.flipcam.usermanual.FCVideoScreen;
import com.google.android.material.tabs.TabLayout;

public class UserManualActivity extends AppCompatActivity {

    private UserManualAdapter userManualAdapter;
    private ViewPager viewPager;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_manual);
        getSupportActionBar().setTitle(getResources().getString(R.string.fcUserManualHeading));
        viewPager = findViewById(R.id.userManualPager);

        userManualAdapter = new UserManualAdapter(getSupportFragmentManager());
        userManualAdapter.add(new FCUserManualWelcomeScreen(),getResources().getString(R.string.fcUserManualWelcome));
        userManualAdapter.add(new FCVideoScreen(),getResources().getString(R.string.fcUserManualScreen1Heading));
        userManualAdapter.add(new FCVideoRecordingScreen(),getResources().getString(R.string.fcUserManualScreen2Heading));
        userManualAdapter.add(new FCVideoRecordingScreen2(),getResources().getString(R.string.fcUserManualScreen2Heading));
        userManualAdapter.add(new FCVideoRecordingPauseScreen(),getResources().getString(R.string.fcUserManualScreen4Heading));
        userManualAdapter.add(new FCPhotoScreen(),getResources().getString(R.string.fcUserManualScreen5Heading));
        userManualAdapter.add(new FCSettingsScreen(),getResources().getString(R.string.fcUserManualScreen6Heading));
        userManualAdapter.add(new FCSettingsSDCardScreen(),getResources().getString(R.string.fcUserManualScreen7Heading));
        userManualAdapter.add(new FCMediaPhotoScreen(),getResources().getString(R.string.fcUserManualScreen8Heading));
        userManualAdapter.add(new FCMediaVideoScreen(),getResources().getString(R.string.fcUserManualScreen8Heading));
        userManualAdapter.add(new FCMediaChooseScreen(),getResources().getString(R.string.fcUserManualScreen10Heading));
        userManualAdapter.add(new FCGridViewScreen(),getResources().getString(R.string.fcUserManualScreen11Heading));
        userManualAdapter.add(new FCUserManualExitScreen(),getResources().getString(R.string.fcUserManualExitHeading));

        //Set the adapter
        viewPager.setAdapter(userManualAdapter);
        viewPager.setOffscreenPageLimit(1);

        tabLayout =  findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
    }
}