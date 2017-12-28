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
    LinearLayout sdcardlayout;
    TextView sdCardPathMsg;
    ImageView editSdCardPath;
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
        sdCardPathMsg = (TextView)findViewById(R.id.sdcardpathmsg);
        editSdCardPath = (ImageView)findViewById(R.id.editSdCardPath);
        sdCardDialog = new Dialog(this);
        sdcardlayout = (LinearLayout)findViewById(R.id.sdcardlayout);
        //reDrawEditSdCard();
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
                editSdCardPath.setClickable(false);
            }
            else{
                Log.d(TAG,"Phone memory is false");
                phoneMemBtn.setChecked(false);
                sdCardBtn.setChecked(true);
                editSdCardPath.setClickable(true);
            }
            if(settingsPref.contains(Constants.SD_CARD_PATH)) {
                String sdcardpath = settingsPref.getString(Constants.SD_CARD_PATH, "");
                showSDCardPath(sdcardpath);
            }
            else{
                hideSDCardPath();
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

    public void openSdCardPath(View view){
        if(settingsPref.contains(Constants.SD_CARD_PATH)) {
            ((EditText) sdCardRoot.findViewById(R.id.sdCardPathText)).setText(settingsPref.getString(Constants.SD_CARD_PATH,""));
        }
        Configuration config = getResources().getConfiguration();
        if(config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            TextView sdcardText = (TextView)sdCardRoot.findViewById(R.id.sdCardMsg);
            sdcardText.setText(getResources().getString(R.string.sdCardPathPortrait));
        }
        else{
            TextView sdcardText = (TextView)sdCardRoot.findViewById(R.id.sdCardMsg);
            sdcardText.setText(getResources().getString(R.string.sdCardPathLandscape));
        }
        sdCardDialog.setContentView(sdCardRoot);
        sdCardDialog.setCancelable(true);
        sdCardDialog.show();
    }

    public void selectSaveMedia(View view){
        switch (view.getId()){
            case R.id.phoneMemButton:
                Log.d(TAG,"Save in phone memory");
                settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,true);
                settingsEditor.commit();
                phoneMemBtn.setChecked(true);
                sdCardBtn.setChecked(false);
                editSdCardPath.setClickable(false);
                break;
            case R.id.sdCardbutton:
                Log.d(TAG,"Save in sd card");
                phoneMemBtn.setChecked(false);
                sdCardBtn.setChecked(true);
                editSdCardPath.setClickable(true);
                if(!settingsPref.contains(Constants.SD_CARD_PATH)) {
                    openSdCardPath(view);
                }
                else{
                    settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,false);
                    settingsEditor.commit();
                }
                break;
        }
    }
    LayoutInflater layoutInflater;
    View sdCardRoot;

    public void saveSdCardPath(View view){
        switch (view.getId()){
            case R.id.okSdCard:
                Log.d(TAG,"Checking if path is valid");
                String path;
                path = ((EditText) sdCardRoot.findViewById(R.id.sdCardPathText)).getText().toString();
                Log.d(TAG,"Path = "+path);
                File sdCard = new File(path);
                if(!sdCard.exists() || !sdCard.isDirectory()){
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.sdCardPathNotExist),Toast.LENGTH_SHORT).show();
                }
                else{
                    String fullPath;
                    Log.d(TAG,"Existing path = "+sdCard.getPath());
                    if(!sdCard.getPath().contains(getResources().getString(R.string.app_name))) {
                        if (sdCard.getPath().endsWith("/")) {
                            String pathExcludeFrontSlash = sdCard.getPath().substring(0, sdCard.getPath().length() - 1);
                            fullPath = pathExcludeFrontSlash + getResources().getString(R.string.FC_ROOT);
                        } else {
                            fullPath = sdCard.getPath() + getResources().getString(R.string.FC_ROOT);
                        }
                        Log.d(TAG, "Full path = " + fullPath);
                        File fc = new File(fullPath);
                        if (!fc.exists()) {
                            fc.mkdir();
                            Log.d(TAG, "Able to create FC");
                        }
                        Toast.makeText(getApplicationContext(),getResources().getString(R.string.sdCardPathSaved),Toast.LENGTH_SHORT).show();
                        settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,false);
                        settingsEditor.putString(Constants.SD_CARD_PATH,fc.getPath());
                        settingsEditor.commit();
                        showSDCardPath(fc.getPath());
                    }
                    else{
                        Toast.makeText(getApplicationContext(),getResources().getString(R.string.sdCardPathSaved),Toast.LENGTH_SHORT).show();
                        settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,false);
                        settingsEditor.putString(Constants.SD_CARD_PATH,sdCard.getPath());
                        settingsEditor.commit();
                        showSDCardPath(sdCard.getPath());
                    }
                    sdCardDialog.dismiss();
                }
                break;
            case R.id.cancelSdCard:
                if(settingsPref.contains(Constants.SD_CARD_PATH) && !settingsPref.getString(Constants.SD_CARD_PATH,"").equalsIgnoreCase("")){
                    phoneMemBtn.setChecked(false);
                    sdCardBtn.setChecked(true);
                    settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,true);
                    settingsEditor.commit();
                }
                else{
                    phoneMemBtn.setChecked(true);
                    sdCardBtn.setChecked(false);
                    settingsEditor.putBoolean(Constants.SAVE_MEDIA_PHONE_MEM,false);
                    settingsEditor.commit();
                }
                sdCardDialog.dismiss();
                Log.d(TAG,"cancel sd card");
                break;
        }
    }

    public void showSDCardPath(String path){
        sdCardPathMsg.setText(path);
        sdcardlayout.setVisibility(View.VISIBLE);
    }

    public void hideSDCardPath(){
        sdcardlayout.setVisibility(View.GONE);
    }

    public void reDrawSDCardScreen(){
        Configuration config = getResources().getConfiguration();
        if(config.orientation == Configuration.ORIENTATION_PORTRAIT){
            TextView sdcardText = (TextView)sdCardRoot.findViewById(R.id.sdCardMsg);
            sdcardText.setText(getResources().getString(R.string.sdCardPathPortrait));
        }
        else{
            TextView sdcardText = (TextView)sdCardRoot.findViewById(R.id.sdCardMsg);
            sdcardText.setText(getResources().getString(R.string.sdCardPathLandscape));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //reDrawEditSdCard();
        reDrawSDCardScreen();
    }
}
