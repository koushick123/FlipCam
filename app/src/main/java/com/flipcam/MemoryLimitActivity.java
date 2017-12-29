package com.flipcam;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.flipcam.constants.Constants;

public class MemoryLimitActivity extends AppCompatActivity {

    public static final String TAG = "MemoryLimitActivity";
    EditText memoryThresholdText;
    RadioButton mbButton;
    RadioButton gbButton;
    SharedPreferences settingsPref;
    SharedPreferences.Editor settingsEditor;
    CheckBox disablethresholdCheck;
    boolean mbSelect = true;
    boolean disableCheck = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_limit);
        getSupportActionBar().setTitle(getResources().getString(R.string.phoneMemoryLimitHeading));
        memoryThresholdText = (EditText)findViewById(R.id.memoryThresholdText);
        mbButton = (RadioButton)findViewById(R.id.mbButton);
        gbButton = (RadioButton)findViewById(R.id.gbButton);
        disablethresholdCheck = (CheckBox)findViewById(R.id.disablethresholdCheck);
        settingsPref = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        settingsEditor = settingsPref.edit();
        if(settingsPref.contains(Constants.PHONE_MEMORY_DISABLE)) {
            if(!settingsPref.getBoolean(Constants.PHONE_MEMORY_DISABLE, false)) {
                enableThresholdElements();
                disablethresholdCheck.setChecked(false);
                if (settingsPref.contains(Constants.PHONE_MEMORY_LIMIT)) {
                    switch (settingsPref.getString(Constants.PHONE_MEMORY_METRIC, "")) {
                        case "MB":
                            mbButton.setChecked(true);
                            gbButton.setChecked(false);
                            mbSelect = true;
                            break;
                        case "GB":
                            mbButton.setChecked(false);
                            gbButton.setChecked(true);
                            mbSelect = false;
                            break;
                    }
                    memoryThresholdText.setText(settingsPref.getString(Constants.PHONE_MEMORY_LIMIT, getResources().getInteger(R.integer.minimumMemoryWarning) + ""));
                } else {
                    //Set to default values.
                    mbButton.setChecked(true);
                    gbButton.setChecked(false);
                    mbSelect = true;
                    memoryThresholdText.setText(getResources().getInteger(R.integer.minimumMemoryWarning) + "");
                }
            }
            else{
                disableThresholdElements();
                disablethresholdCheck.setChecked(true);
            }
        }
        else{
            //Set to default values.
            mbButton.setChecked(true);
            gbButton.setChecked(false);
            mbSelect = true;
            disablethresholdCheck.setChecked(false);
            memoryThresholdText.setText(getResources().getInteger(R.integer.minimumMemoryWarning) + "");
            disableCheck = false;
        }
        if(!disablethresholdCheck.isChecked()) {
            showSoftKeyboard(memoryThresholdText);
        }
    }

    public void enableThresholdElements(){
        memoryThresholdText.setEnabled(true);
        gbButton.setEnabled(true);
        mbButton.setEnabled(true);
        disableCheck = false;
    }

    public void disableThresholdElements(){
        //Phone memory check Disabled
        memoryThresholdText.setEnabled(false);
        gbButton.setEnabled(false);
        mbButton.setEnabled(false);
        disableCheck = true;
    }

    public void selectMB(View view){
        mbButton.setChecked(true);
        gbButton.setChecked(false);
        mbSelect = true;
    }

    public void selectGB(View view){
        gbButton.setChecked(true);
        mbButton.setChecked(false);
        mbSelect = false;
    }

    public boolean validateThreshold(){
        String threshold = memoryThresholdText.getText().toString();
        if(!threshold.trim().equalsIgnoreCase("")) {
            try {
                Integer.parseInt(threshold);
                return true;
            } catch (NumberFormatException formatException) {
                Log.d(TAG, "NumberFormatException");
                return false;
            }
        }
        else{
            return false;
        }
    }

    public void showSoftKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public void disableThresholdCheck(View view){
        if(disablethresholdCheck.isChecked()){
            disableThresholdElements();
        }
        else{
            enableThresholdElements();
        }
    }

    public void calculateIfThresholdExceedsInternalMemory(){
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        Log.d(TAG,"Free size = "+stat.getFreeBytes());
        Log.d(TAG,"Total size = "+stat.getTotalBytes());
        Log.d(TAG,"Available size = "+stat.getAvailableBytes());
    }

    @Override
    public void onBackPressed() {
        if (disableCheck) {
            super.onBackPressed();
            settingsEditor.remove(Constants.PHONE_MEMORY_LIMIT);
            settingsEditor.remove(Constants.PHONE_MEMORY_METRIC);
            settingsEditor.putBoolean(Constants.PHONE_MEMORY_DISABLE, true);
            settingsEditor.commit();
        } else {
            if (validateThreshold()) {
                super.onBackPressed();
                calculateIfThresholdExceedsInternalMemory();
                settingsEditor.putString(Constants.PHONE_MEMORY_LIMIT, memoryThresholdText.getText().toString());
                settingsEditor.putString(Constants.PHONE_MEMORY_METRIC, mbSelect ? "MB" : "GB");
                settingsEditor.putBoolean(Constants.PHONE_MEMORY_DISABLE, false);
                settingsEditor.commit();
                /*Intent settingsAct = new Intent(this, SettingsActivity.class);
                startActivity(settingsAct);*/
                overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.thresholdSaved, memoryThresholdText.getText().toString(), mbSelect ? "MB" : "GB"),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.validThresholdMsg), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
