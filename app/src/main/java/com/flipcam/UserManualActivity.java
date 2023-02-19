package com.flipcam;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.flipcam.adapter.UserManualAdapter;
import com.flipcam.usermanual.FCScreen1;
import com.flipcam.usermanual.FCScreen10;
import com.flipcam.usermanual.FCScreen11;
import com.flipcam.usermanual.FCScreen2;
import com.flipcam.usermanual.FCScreen3;
import com.flipcam.usermanual.FCScreen4;
import com.flipcam.usermanual.FCScreen5;
import com.flipcam.usermanual.FCScreen6;
import com.flipcam.usermanual.FCScreen7;
import com.flipcam.usermanual.FCScreen8;
import com.flipcam.usermanual.FCScreen9;
import com.flipcam.usermanual.FCUserManualExitScreen;
import com.flipcam.usermanual.FCUserManualWelcomeScreen;
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
        userManualAdapter.add(new FCScreen1(),getResources().getString(R.string.fcUserManualScreen1Heading));
        userManualAdapter.add(new FCScreen2(),getResources().getString(R.string.fcUserManualScreen2Heading));
        userManualAdapter.add(new FCScreen3(),getResources().getString(R.string.fcUserManualScreen2Heading));
        userManualAdapter.add(new FCScreen4(),getResources().getString(R.string.fcUserManualScreen4Heading));
        userManualAdapter.add(new FCScreen5(),getResources().getString(R.string.fcUserManualScreen5Heading));
        userManualAdapter.add(new FCScreen6(),getResources().getString(R.string.fcUserManualScreen6Heading));
        userManualAdapter.add(new FCScreen7(),getResources().getString(R.string.fcUserManualScreen7Heading));
        userManualAdapter.add(new FCScreen8(),getResources().getString(R.string.fcUserManualScreen8Heading));
        userManualAdapter.add(new FCScreen9(),getResources().getString(R.string.fcUserManualScreen8Heading));
        userManualAdapter.add(new FCScreen10(),getResources().getString(R.string.fcUserManualScreen10Heading));
        userManualAdapter.add(new FCScreen11(),getResources().getString(R.string.fcUserManualScreen11Heading));
        userManualAdapter.add(new FCUserManualExitScreen(),getResources().getString(R.string.fcUserManualExitHeading));

        //Set the adapter
        viewPager.setAdapter(userManualAdapter);
        viewPager.setOffscreenPageLimit(1);

        tabLayout =  findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
    }
}