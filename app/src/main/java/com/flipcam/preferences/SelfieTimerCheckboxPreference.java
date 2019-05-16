package com.flipcam.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.flipcam.R;
import com.flipcam.constants.Constants;

public class SelfieTimerCheckboxPreference extends CheckBoxPreference {

    Context mContext;
    boolean enableSeparator;
    boolean VERBOSE = true;
    String selectedKey = "";
    public static final String TAG = "SelfieTimerCheckboxPref";
    PreferenceScreen prefScreen;
    SelfieTimerPreference selfieTimerPreference;

    public SelfieTimerCheckboxPreference(Context context, boolean enableSep, String key, PreferenceScreen pScreen) {
        super(context);
        mContext = context;
        enableSeparator = enableSep;
        selectedKey = key;
        prefScreen = pScreen;
    }

    private void enableSelfieTimerPreference(){
        Log.d(TAG, "addSelfieTimerPreference");
        selfieTimerPreference.getSelfieTimerTitle().setTextColor(mContext.getResources().getColor(R.color.turqoise));
        selfieTimerPreference.getSelfieTimerCount().setTextColor(mContext.getResources().getColor(R.color.turqoise));
        selfieTimerPreference.setSelectable(true);
    }

    private void disableSelfieTimerPreference(){
        Log.d(TAG, "removeSelfieTimerPreference");
        selfieTimerPreference.getSelfieTimerTitle().setTextColor(mContext.getResources().getColor(R.color.turqoiseDark));
        selfieTimerPreference.getSelfieTimerCount().setTextColor(mContext.getResources().getColor(R.color.turqoiseDark));
        selfieTimerPreference.setSelectable(false);
    }

    @Override
    protected void onBindView(View view) {
        if(VERBOSE)Log.d(TAG, "onBindView");
        selfieTimerPreference = (SelfieTimerPreference) prefScreen.findPreference(Constants.SELFIE_TIMER);
        TextView title = view.findViewById(R.id.checkboxTitle);
        title.setText(getTitle());
        final CheckBox summary = view.findViewById(R.id.checkboxSummary);
        summary.setText(getSummary());
        boolean memCon;
        memCon = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(selectedKey, false);
        summary.setChecked(memCon);
        summary.setOnClickListener((view1) -> {
                if(VERBOSE)Log.d(TAG, "selectedKey changed = "+selectedKey+" , "+summary.isChecked());
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
                editor.putBoolean(selectedKey, summary.isChecked());
                editor.commit();
                if(summary.isChecked()){
                    enableSelfieTimerPreference();
                }
                else{
                    disableSelfieTimerPreference();
                }
            }
        );
        LinearLayout seperator = view.findViewById(R.id.separator);
        seperator.setVisibility(enableSeparator ? View.VISIBLE : View.GONE);
        if(summary.isChecked()){
            Log.d(TAG, "Set Turqoise");
            enableSelfieTimerPreference();
        }
        else{
            Log.d(TAG, "Set Turqoise Dark");
            disableSelfieTimerPreference();
        }
        super.onBindView(view);
    }
}
