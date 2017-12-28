package com.flipcam;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.flipcam.constants.Constants;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    public static final String TAG = "SettingsActivity";
    LinearLayout phoneMemParentVert;
    TextView phoneMemText;
    TextView phoneMemTextMsg;
    ImageView greenArrow;
    SharedPreferences settingsPref;
    SharedPreferences.Editor settingsEditor;
    RadioButton phoneMemBtn;
    RadioButton sdCardBtn;
    Dialog sdCardDialog;
    LinearLayout sdCardParent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
        setContentView(R.layout.activity_settings);
        phoneMemParentVert = (LinearLayout)findViewById(R.id.phoneMemParentVert);
        phoneMemTextMsg = (TextView)findViewById(R.id.phoneMemTextMsg);
        phoneMemText = (TextView)findViewById(R.id.phoneMemText);
        greenArrow = (ImageView)findViewById(R.id.greenArrow);
        phoneMemBtn = (RadioButton)findViewById(R.id.phoneMemButton);
        sdCardBtn = (RadioButton)findViewById(R.id.sdCardbutton);
        sdCardDialog = new Dialog(this);
        sdCardParent = (LinearLayout)findViewById(R.id.sdCardParent);
        reDrawPhoneMem();
        phoneMemText.setText(getResources().getString(R.string.phoneMemoryLimit, getResources().getInteger(R.integer.minimumMemoryWarning)));
        getSupportActionBar().setTitle(getResources().getString(R.string.settingTitle));
        settingsPref = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        settingsEditor = settingsPref.edit();
        if(settingsPref.contains(Constants.SAVE_MEDIA_PHONE_MEM)){
            Log.d(TAG,"Phone memory exists");
            if(settingsPref.getBoolean(Constants.SAVE_MEDIA_PHONE_MEM,true)){
                Log.d(TAG,"Phone memory is true");
                phoneMemBtn.setChecked(true);
                sdCardBtn.setChecked(false);
            }
            else{
                Log.d(TAG,"Phone memory is false");
                phoneMemBtn.setChecked(false);
                sdCardBtn.setChecked(true);
            }
        }
        else{
            Log.d(TAG,"Phone memory NOT exists");
            phoneMemBtn.setChecked(true);
            sdCardBtn.setChecked(false);
        }
        layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        sdCardRoot = layoutInflater.inflate(R.layout.sd_card_location,null);
    }

    public void selectSaveMedia(View view){
        switch (view.getId()){
            case R.id.phoneMemButton:
                Log.d(TAG,"Save in phone memory");
                settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,true);
                settingsEditor.commit();
                phoneMemBtn.setChecked(true);
                sdCardBtn.setChecked(false);
                break;
            case R.id.sdCardbutton:
                Log.d(TAG,"Save in sd card");
                phoneMemBtn.setChecked(false);
                sdCardBtn.setChecked(true);
                sdCardDialog.setContentView(sdCardRoot);
                sdCardDialog.setCancelable(true);
                sdCardDialog.show();
                break;
        }
    }
    LayoutInflater layoutInflater;
    View sdCardRoot;

    public void saveSdCardPath(View view){
        switch (view.getId()){
            case R.id.okSdCard:
                Log.d(TAG,"Checking if path is valid");
                String path = ((EditText)sdCardRoot.findViewById(R.id.sdCardPathText)).getText().toString();
                Log.d(TAG,"Path = "+path);
                File sdCard = new File(path);
                if(!sdCard.exists()){
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.sdCardPathNotExist),Toast.LENGTH_SHORT).show();
                }
                else{
                    String fullPath;
                    if(sdCard.getPath().endsWith("/")) {
                        fullPath = sdCard.getPath() + getResources().getString(R.string.FC_ROOT);
                    }
                    else{
                        fullPath = sdCard.getPath() + "/ "+ getResources().getString(R.string.FC_ROOT);
                    }
                    Log.d(TAG, "Full path = " +fullPath);
                    File fc = new File(fullPath);
                    if(fc.exists()) {
                        Log.d(TAG, "Able to create FC");
                        sdCardDialog.dismiss();
                        Toast.makeText(getApplicationContext(),getResources().getString(R.string.sdCardPathSaved),Toast.LENGTH_SHORT).show();
                        settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,false);
                        settingsEditor.commit();
                        TextView sdCardPath = new TextView(this);
                        sdCardPath.setText(fc.getPath());
                        sdCardPath.setTextColor(getResources().getColor(R.color.turqoise));
                        LinearLayout.LayoutParams sdcardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        sdcardParams.setMargins(20,0,0,0);
                        sdCardPath.setLayoutParams(sdcardParams);
                        sdCardParent.addView(sdCardPath);
                    }
                }
                break;
            case R.id.cancelSdCard:
                sdCardDialog.dismiss();
                Log.d(TAG,"cancel sd card");
                break;
        }
    }

    public void reDrawPhoneMem(){
        Configuration config = getResources().getConfiguration();
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if(config.orientation == Configuration.ORIENTATION_PORTRAIT){
            Log.d(TAG,"oncreate portrait");
            layoutParams.width = getResources().getInteger(R.integer.phoneMemTextWidthPortrait);
            layoutParams.setMargins(getResources().getInteger(R.integer.phoneMemTextLeftMargin),0,0,0);
        }
        else{
            Log.d(TAG,"oncreate landscape");
            layoutParams.width = getResources().getInteger(R.integer.phoneMemTextWidthLandscape);
            layoutParams.setMargins(getResources().getInteger(R.integer.phoneMemTextLeftMargin),0,getResources().getInteger(R.integer.greenArrowRightMargin),0);
        }
        phoneMemParentVert.setLayoutParams(layoutParams);
        phoneMemText.setLayoutParams(layoutParams);
        phoneMemTextMsg.setLayoutParams(layoutParams);
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reDrawPhoneMem();
    }
}
