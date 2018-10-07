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
import com.flipcam.constants.Constants;

public class MemoryConsumedPreference extends CheckBoxPreference {

    Context mContext;
    boolean enableSeparator;
    boolean VERBOSE = true;
    public static final String TAG = "MemoryConsumedPref";

    public MemoryConsumedPreference(Context context, boolean enableSep) {
        super(context);
        mContext = context;
        enableSeparator = enableSep;
    }

    @Override
    protected void onBindView(View view) {
        if(VERBOSE)Log.d(TAG, "onBindView = "+view);
        TextView title = view.findViewById(R.id.checkboxTitle);
        title.setText(getTitle());
        final CheckBox summary = view.findViewById(R.id.checkboxSummary);
        summary.setText(getSummary());
        boolean memCon = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Constants.SHOW_MEMORY_CONSUMED_MSG, false);
        summary.setChecked(memCon);
        summary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(VERBOSE)Log.d(TAG, "newValue memory consumed = "+summary.isChecked());
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
                editor.putBoolean(Constants.SHOW_MEMORY_CONSUMED_MSG, summary.isChecked());
                editor.commit();
            }
        });
        LinearLayout seperator = view.findViewById(R.id.separator);
        seperator.setVisibility(enableSeparator ? View.VISIBLE : View.GONE);
        super.onBindView(view);
    }
}
