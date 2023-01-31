package com.flipcam.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.flipcam.R;

public class NoAudioCheckboxPreference extends CheckBoxPreference {

    Context mContext;
    boolean enableSeparator;
    boolean VERBOSE = true;
    String selectedKey = "";
    public static final String TAG = "NoAudioChkbxPreference";

    public NoAudioCheckboxPreference(Context context, boolean enableSep, String key) {
        super(context);
        mContext = context;
        enableSeparator = enableSep;
        selectedKey = key;
    }

    @Override
    protected void onBindView(View view) {
        if(VERBOSE)Log.d(TAG, "onBindView = "+view);
        TextView title = view.findViewById(R.id.checkboxTitle);
        title.setText(getTitle());
        final CheckBox summary = view.findViewById(R.id.checkboxSummary);
        summary.setText(getSummary());
        boolean noAudio = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(selectedKey, false);
        summary.setChecked(noAudio);
        summary.setOnClickListener(viewSum -> {
                if(VERBOSE)Log.d(TAG, "selectedKey changed = "+selectedKey+" , "+summary.isChecked());
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
                editor.putBoolean(selectedKey, summary.isChecked());
                editor.commit();
        });
        LinearLayout seperator = view.findViewById(R.id.separator);
        seperator.setVisibility(enableSeparator ? View.VISIBLE : View.GONE);
        super.onBindView(view);
    }
}
