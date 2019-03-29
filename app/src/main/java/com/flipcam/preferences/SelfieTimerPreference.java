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
        if(PreferenceManager.getDefaultSharedPreferences(mContext).getString(selectedKey, "3") != null &&
                !PreferenceManager.getDefaultSharedPreferences(mContext).getString(selectedKey, "3").equalsIgnoreCase("")) {
            int timerValue = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString(selectedKey, "3"));
            numberPicker.setValue(timerValue);
        }
        else{
            numberPicker.setValue(3);
        }
        numberPicker.setMinValue(mContext.getResources().getInteger(R.integer.selfieTimerMin));
        numberPicker.setMaxValue(mContext.getResources().getInteger(R.integer.selfieTimerMax));
        ((EditText)numberPicker.getChildAt(0)).setInputType(InputType.TYPE_NULL);
        super.onBindDialogView(view);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if(positiveResult) {
            Log.d(TAG, "onDialogClosed SEL VAL = " + numberPicker.getValue());
            int timerValue = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString(selectedKey, "3"));
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
            editor.putInt(selectedKey, timerValue);
            editor.commit();
        }
        super.onDialogClosed(positiveResult);
    }
}
