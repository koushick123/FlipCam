package com.flipcam;

import android.content.Context;
import android.preference.ListPreference;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class ResolutionListPreference extends ListPreference {

    Context mContext;
    public static final String TAG = "CustomListPref";

    public ResolutionListPreference(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected void onBindView(View view) {
        Log.d(TAG, "onBindView = "+view);
        TextView title = (TextView)view.findViewById(R.id.resolTitle);
        title.setText(getTitle());
        TextView summary = (TextView)view.findViewById(R.id.resolSummary);
        summary.setText(getSummary());
        super.onBindView(view);
    }

    @Override
    public void setEntries(CharSequence[] entries) {
        super.setEntries(entries);
        Log.d(TAG, "setEntries");
    }

    @Override
    public void setEntryValues(CharSequence[] entryValues) {
        super.setEntryValues(entryValues);
        Log.d(TAG, "setEntryValues");
    }
}
