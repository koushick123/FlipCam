package com.flipcam;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.flipcam.constants.Constants;

import static com.flipcam.constants.Constants.GIGA_BYTE;
import static com.flipcam.constants.Constants.MEGA_BYTE;

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
    TextView totalPhoneMemory;
    TextView freeMemory;
    StatFs storageStat;
    String memorymetric;
    String memory;

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate = "+savedInstanceState);
        storageStat = new StatFs(Environment.getDataDirectory().getPath());
        setContentView(R.layout.activity_memory_limit);
        getSupportActionBar().setTitle(getResources().getString(R.string.phoneMemoryLimitHeading));
        memoryThresholdText = (EditText)findViewById(R.id.memoryThresholdText);
        mbButton = (RadioButton)findViewById(R.id.mbButton);
        gbButton = (RadioButton)findViewById(R.id.gbButton);
        disablethresholdCheck = (CheckBox)findViewById(R.id.disablethresholdCheck);
        settingsPref = getSharedPreferences(Constants.FC_SETTINGS, Context.MODE_PRIVATE);
        settingsEditor = settingsPref.edit();
        if(savedInstanceState == null) {
            if (settingsPref.contains(Constants.PHONE_MEMORY_DISABLE)) {
                if (!settingsPref.getBoolean(Constants.PHONE_MEMORY_DISABLE, true)) {
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
                } else {
                    disableThresholdElements();
                    disablethresholdCheck.setChecked(true);
                }
            } else {
                //Set to default values.
                mbButton.setChecked(true);
                gbButton.setChecked(false);
                mbSelect = true;
                disablethresholdCheck.setChecked(false);
                memoryThresholdText.setText(getResources().getInteger(R.integer.minimumMemoryWarning) + "");
                disableCheck = false;
            }
        }
        else{
            //Fetch saved values and populate
            Log.d(TAG, "Memory = "+savedInstanceState.getString("memory"));
            Log.d(TAG, "Metric = "+savedInstanceState.getBoolean("memoryMetric"));
            Log.d(TAG, "Disable check = "+savedInstanceState.getBoolean("memoryThresholdCheck"));
            memoryThresholdText.setText(savedInstanceState.getString("memory"));
            if(savedInstanceState.getBoolean("memoryMetric")){
                mbButton.setChecked(true);
                gbButton.setChecked(false);
            }
            else{
                mbButton.setChecked(false);
                gbButton.setChecked(true);
            }
            disablethresholdCheck.setChecked(savedInstanceState.getBoolean("memoryThresholdCheck"));
            if(!disablethresholdCheck.isChecked()){
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                enableThresholdElements();
            }
            else{
                disableThresholdElements();
            }
        }
        totalPhoneMemory = (TextView)findViewById(R.id.totalMemory);
        freeMemory = (TextView)findViewById(R.id.freeMemory);
        //Convert total memory to better readable format.
        if(storageStat.getTotalBytes() > GIGA_BYTE){
            double gbs = (storageStat.getTotalBytes() / GIGA_BYTE);
            gbs = (Math.ceil(gbs * 100.0))/100.0;
            Log.d(TAG,"GBs = "+gbs);
            totalPhoneMemory.setText(gbs+" GB");
        }
        else{
            double mbs = (storageStat.getTotalBytes() / MEGA_BYTE);
            mbs = (Math.ceil(mbs * 100.0))/100.0;
            Log.d(TAG,"MBs = "+mbs);
            totalPhoneMemory.setText(mbs+" MB");
        }
        //Convert free memory to better readable format.
        if(storageStat.getAvailableBytes() > GIGA_BYTE){
            double gbs = (storageStat.getAvailableBytes() / GIGA_BYTE);
            gbs = (Math.ceil(gbs * 100.0))/100.0;
            Log.d(TAG,"GBs = "+gbs);
            freeMemory.setText(gbs+" GB");
        }
        else{
            double mbs = (storageStat.getAvailableBytes() / MEGA_BYTE);
            mbs = (Math.ceil(mbs * 100.0))/100.0;
            Log.d(TAG,"MBs = "+mbs);
            freeMemory.setText(mbs+" MB");
        }
    }

    public void enableThresholdElements(){
        memoryThresholdText.setEnabled(true);
        gbButton.setEnabled(true);
        mbButton.setEnabled(true);
        disableCheck = false;
        memoryThresholdText.requestFocus();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(memoryThresholdText, InputMethodManager.SHOW_IMPLICIT);
    }

    public void disableThresholdElements(){
        //Phone memory check Disabled
        memoryThresholdText.setEnabled(false);
        gbButton.setEnabled(false);
        mbButton.setEnabled(false);
        disableCheck = true;
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public void selectMB(View view){
        mbButton.setChecked(true);
        gbButton.setChecked(false);
        mbSelect = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        if(!disablethresholdCheck.isChecked()){
            enableThresholdElements();
        }
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

    public void disableThresholdCheck(View view){
        if(disablethresholdCheck.isChecked()){
            disableThresholdElements();
        }
        else{
            enableThresholdElements();
        }
    }

    public boolean calculateIfThresholdIsWithinInternalMemory(){
        Log.d(TAG,"Available size = "+storageStat.getAvailableBytes());
        long availableMem = storageStat.getAvailableBytes();

        if(availableMem > GIGA_BYTE){
            double gbs = (availableMem / GIGA_BYTE);
            gbs = (Math.ceil(gbs * 100.0))/100.0;
            Log.d(TAG,"GBs = "+gbs);
            memory = gbs+"";
            memorymetric = "GB";
        }
        else{
            double mbs = (availableMem / MEGA_BYTE);
            mbs = (Math.ceil(mbs * 100.0))/100.0;
            Log.d(TAG,"MBs = "+mbs);
            memory = mbs+"";
            memorymetric = "MB";
        }
        int threshold = Integer.parseInt(memoryThresholdText.getText().toString());
        long thresholdMem;
        if(mbButton.isChecked()){
            thresholdMem = (long)MEGA_BYTE * threshold;
            Log.d(TAG,"thresholdMem MB = "+thresholdMem );
        }
        else{
            thresholdMem = (long)GIGA_BYTE * threshold;
            Log.d(TAG,"thresholdMem GB = "+thresholdMem );
        }
        if(thresholdMem > storageStat.getAvailableBytes()){
            return false;
        }
        else{
            return true;
        }
    }

    public boolean checkIfHigherThanDefaultThreshold(){
        int defaultThreshold = getResources().getInteger(R.integer.minimumMemoryWarning);
        int threshold = Integer.parseInt(memoryThresholdText.getText().toString());
        long thresholdMem;
        if(mbButton.isChecked()){
            thresholdMem = (long)MEGA_BYTE * threshold;
            Log.d(TAG,"checkIfHigherThanDefault MB = "+thresholdMem );
        }
        else{
            thresholdMem = (long)GIGA_BYTE * threshold;
            Log.d(TAG,"checkIfHigherThanDefault GB = "+thresholdMem );
        }
        if(thresholdMem < (defaultThreshold * MEGA_BYTE)){
            return false;
        }
        else {
            return true;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        outState.putBoolean("memoryMetric",mbSelect);
        outState.putString("memory",memoryThresholdText.getText().toString());
        outState.putBoolean("memoryThresholdCheck",disablethresholdCheck.isChecked());
    }

    @Override
    public void onBackPressed() {
        if (disableCheck) {
            settingsEditor.remove(Constants.PHONE_MEMORY_LIMIT);
            settingsEditor.remove(Constants.PHONE_MEMORY_METRIC);
            settingsEditor.putBoolean(Constants.PHONE_MEMORY_DISABLE, true);
            settingsEditor.commit();
            super.onBackPressed();
        } else {
            if (validateThreshold()) {
                if (calculateIfThresholdIsWithinInternalMemory()) {
                    if(checkIfHigherThanDefaultThreshold()) {
                        super.onBackPressed();
                        settingsEditor.putString(Constants.PHONE_MEMORY_LIMIT, memoryThresholdText.getText().toString());
                        settingsEditor.putString(Constants.PHONE_MEMORY_METRIC, mbSelect ? "MB" : "GB");
                        settingsEditor.putBoolean(Constants.PHONE_MEMORY_DISABLE, false);
                        settingsEditor.commit();
                        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.thresholdSaved, memoryThresholdText.getText().toString(), mbSelect ? "MB" : "GB"),
                                Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.thresholdLowerThanMinimum,getResources().getInteger(R.integer.minimumMemoryWarning)+" "+
                                getResources().getString(R.string.MEM_PF_MB)),
                                Toast.LENGTH_LONG).show();
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.thresholdExceeds,memory,memorymetric), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.validThresholdMsg), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
