package com.flipcam;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.flipcam.constants.Constants;
import com.flipcam.model.Dimension;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class PhotoSettingsActivity extends AppCompatActivity {

    public static final String TAG = "PhotoSettingsActivity";
    static boolean VERBOSE = true;
    static Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(VERBOSE)Log.d(TAG, "onCreate");
        mContext = getApplicationContext();
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PhotoSettingFragment()).commit();
    }

    public static class PhotoSettingFragment extends PreferenceFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View rootView = super.onCreateView(inflater, container, savedInstanceState);
            rootView.setBackgroundColor(getResources().getColor(R.color.settingsBarColor));
            return rootView;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if(VERBOSE)Log.d(TAG, "PhotoSettingFragment onCreate");
            addPreferencesFromResource(R.xml.preferences);
            //Add Back Camera list prefs
            addResolutionList(true);
            //Add Front camera list prefs
            addResolutionList(false);
        }

        private void addResolutionList(boolean backCamera){
            Log.d(TAG, "for backCamera? = "+backCamera);
            ListPreference listPreference;
            Set<String> entries;
            SharedPreferences settingsPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            if(backCamera) {
                listPreference = new MyListPreference(getActivity(), true);
                entries = settingsPrefs.getStringSet(Constants.SUPPORT_PHOTO_RESOLUTIONS, null);
            }
            else{
                listPreference = new MyListPreference(getActivity(), true);
                entries = settingsPrefs.getStringSet(Constants.SUPPORT_PHOTO_RESOLUTIONS_FRONT, null);
            }
            int index=0;
            TreeSet<Dimension> sortedPicsSizes = new TreeSet<>();
            if (VERBOSE) Log.d(TAG, "photoRes SIZE = " + entries.size());
            int width = 0, height = 0;
            //Sort all sizes in descending order.
            for (String resol : entries) {
                width = Integer.parseInt(resol.substring(0, resol.indexOf(" ")));
                height = Integer.parseInt(resol.substring(resol.lastIndexOf(" ") + 1, resol.length()));
                sortedPicsSizes.add(new Dimension(width, height));
            }
            CharSequence[] resEntries = new CharSequence[sortedPicsSizes.size()];
            Iterator<Dimension> resolIter = sortedPicsSizes.iterator();
            while (resolIter.hasNext()) {
                Dimension dimen = resolIter.next();
                width = dimen.getWidth();
                height = dimen.getHeight();
                resEntries[index++] = width + " X "+height;
            }
            listPreference.setEntries(resEntries);
            listPreference.setEntryValues(resEntries);
            listPreference.setPersistent(true);
            if(backCamera) {
                listPreference.setTitle(getResources().getString(R.string.backCamResTitle));
                listPreference.setSummary(getResources().getString(R.string.backCamResSummary));
                listPreference.setKey(Constants.SELECT_PHOTO_RESOLUTION);
                listPreference.setValue(settingsPrefs.getString(Constants.SELECT_PHOTO_RESOLUTION, null));
            }
            else{
                listPreference.setTitle(getResources().getString(R.string.frontCamResTitle));
                listPreference.setSummary(getResources().getString(R.string.frontCamResSummary));
                listPreference.setKey(Constants.SELECT_PHOTO_RESOLUTION_FRONT);
                listPreference.setValue(settingsPrefs.getString(Constants.SELECT_PHOTO_RESOLUTION_FRONT, null));
            }
            listPreference.setDialogTitle(getResources().getString(R.string.photoResolutionDialogHeading));
            listPreference.setLayoutResource(R.layout.custom_photo_setting);
            getPreferenceScreen().addPreference(listPreference);
            listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String newRes = (String)newValue;
                    Log.d(TAG, "onPreferenceChange = "+newRes);
                    Log.d(TAG, "onPreferenceChange pref = "+preference.getKey());
                    return true;
                }
            });
        }
    }
}
