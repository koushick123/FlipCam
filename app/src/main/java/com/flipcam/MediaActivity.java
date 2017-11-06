package com.flipcam;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.io.File;
import java.util.ArrayList;

public class MediaActivity extends AppCompatActivity{

    private static final String TAG = "MediaActivity";
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    File dcimFcImages = null;
    File[] images = null;
    File[] sortedImages = null;

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"onStop");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();
    }

    int selectedPosition = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dcimFcImages = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + getResources().getString(R.string.FC_ROOT));
        images = dcimFcImages.listFiles();
        mPager = (ViewPager) findViewById(R.id.mediaPager);
        mPagerAdapter = new MediaSlidePager(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener(){
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Log.d(TAG,"onPageSelected = "+position);
                selectedPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mPager.setOffscreenPageLimit(1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause");

    }

    public boolean isImage(String path)
    {
        if(path.endsWith(getResources().getString(R.string.IMG_EXT)) || path.endsWith(getResources().getString(R.string.ANOTHER_IMG_EXT))){
            return true;
        }
        return false;
    }

    private class MediaSlidePager extends FragmentStatePagerAdapter
    {
        @Override
        public int getCount() {
            return sortedImages.length;
        }

        @Override
        public Fragment getItem(int position) {
            if(selectedPosition == -1) {
                if (isImage(sortedImages[position].getPath())) {
                    return MediaFragment.newInstance(sortedImages[position].getPath());
                } else {
                    return MediaVideoFragment.newInstance(sortedImages[position].getPath());
                }
            }
            else{
                if (isImage(sortedImages[selectedPosition].getPath())) {
                    return MediaFragment.newInstance(sortedImages[selectedPosition].getPath());
                } else {
                    return MediaVideoFragment.newInstance(sortedImages[selectedPosition].getPath());
                }
            }
        }

        public MediaSlidePager(FragmentManager fm) {
            super(fm);
            //Arrays.sort(images);
            ArrayList<File> tempList = new ArrayList<>();
            for(int i=images.length-1;i>=0;i--){
                tempList.add(images[i]);
            }
            sortedImages = tempList.toArray(new File[tempList.size()]);
            Log.d(TAG,"list sorted = "+sortedImages.length);
        }
    }
}
