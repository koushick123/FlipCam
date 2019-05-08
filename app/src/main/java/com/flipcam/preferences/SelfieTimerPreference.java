package com.flipcam.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.flipcam.R;

public class SelfieTimerPreference extends DialogPreference {

    Context mContext;
    boolean enableSeparator;
    boolean VERBOSE = true;
    String selectedKey = "";
    NumberPicker numberPicker;
    int timerDefault;
    public static final String TAG = "SelfieTimerPreference";

    public SelfieTimerPreference(Context context, boolean enableSep, String key) {
        super(context);
        mContext = context;
        enableSeparator = enableSep;
        selectedKey = key;
        timerDefault = mContext.getResources().getInteger(R.integer.selfieTimerDefault);
    }

    @Override
    protected void onBindView(View view) {
        if(VERBOSE)Log.d(TAG, "onBindView");
        TextView selfieTimerTitle = (TextView)view.findViewById(R.id.selfieTimerTitle);
        selfieTimerTitle.setText(getTitle());
        TextView selfieTimerCount = (TextView)view.findViewById(R.id.selfieTimerCount);
        selfieTimerCount.setText(getSummary());
        LinearLayout seperator = view.findViewById(R.id.separator);
        seperator.setVisibility(enableSeparator ? View.VISIBLE : View.GONE);
        super.onBindView(view);
    }

    @Override
    protected void onBindDialogView(View view) {
        if(VERBOSE)Log.d(TAG, "onBindDialogView");
        numberPicker = view.findViewById(R.id.timerPickerValue);
        numberPicker.setWrapSelectorWheel(false);
        //Set Number Picker details
        String[] displayedVals = new String[(mContext.getResources().getInteger(R.integer.selfieTimerMax) -
                mContext.getResources().getInteger(R.integer.selfieTimerMin)) + 1];
        for(int i=0;i<displayedVals.length;i++){
            displayedVals[i] = String.valueOf(i+1);
        }
        int defaultTime = mContext.getResources().getInteger(R.integer.selfieTimerDefault);
        SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        int timerValue = defPrefs.getInt(selectedKey, defaultTime);
        Log.d(TAG, "Saved value = "+timerValue);
        numberPicker.setMinValue(mContext.getResources().getInteger(R.integer.selfieTimerMin));
        numberPicker.setMaxValue(mContext.getResources().getInteger(R.integer.selfieTimerMax));
        numberPicker.setValue(timerValue);
        super.onBindDialogView(view);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if(positiveResult) {
            Log.d(TAG, "onDialogClosed SELECTED VAL = " + numberPicker.getValue());
            SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor editor = defPrefs.edit();
            editor.putInt(selectedKey, numberPicker.getValue());
            editor.commit();
        }
        super.onDialogClosed(positiveResult);
    }
}
