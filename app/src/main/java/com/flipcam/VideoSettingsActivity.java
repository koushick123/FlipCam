package com.flipcam;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.flipcam.constants.Constants;
import com.flipcam.preferences.CustomListPreference;
import com.flipcam.preferences.ShutterCheckboxPreference;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class VideoSettingsActivity extends AppCompatActivity {

    public static final String TAG = "VideoSettingsActivity";
    static boolean VERBOSE = false;
    static Context mContext;
    static boolean isHighest4K = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(VERBOSE)Log.d(TAG, "onCreate");
        mContext = getApplicationContext();
        getFragmentManager().beginTransaction().replace(android.R.id.content, new VideoSettingFragment()).commit();
    }

    public static class VideoSettingFragment extends PreferenceFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View rootView = super.onCreateView(inflater, container, savedInstanceState);
            rootView.setBackgroundColor(getResources().getColor(R.color.settingsBarColor));
            return rootView;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (VERBOSE) Log.d(TAG, "VideoSettingFragment onCreate");
            addPreferencesFromResource(R.xml.preferences);
            Resources resources = getActivity().getResources();
            //Video Resolutions
            ListPreference listPreference = new CustomListPreference(getActivity(), true);
            Set<String> resSizes = new LinkedHashSet<>();
            resSizes.add(resources.getString(R.string.videoResHigh));
            resSizes.add(resources.getString(R.string.videoResMedium));
            resSizes.add(resources.getString(R.string.videoResLow));
            CharSequence[] resEntries = new CharSequence[resSizes.size()];
            int index = 0;
            Iterator<String> resolIter = resSizes.iterator();
            while (resolIter.hasNext()) {
                String resol = resolIter.next();
                resEntries[index++] = resol;
            }
            SharedPreferences settingsPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            listPreference.setEntries(resEntries);
            listPreference.setEntryValues(resEntries);
            listPreference.setTitle(resources.getString(R.string.videoResolutionHeading));
            listPreference.setSummary(resources.getString(R.string.videoResolutionSummary));
            listPreference.setKey(Constants.SELECT_VIDEO_RESOLUTION);
            listPreference.setValue(settingsPrefs.getString(Constants.SELECT_VIDEO_RESOLUTION, null));
            listPreference.setDialogTitle(getResources().getString(R.string.videoResolutionHeading));
            listPreference.setLayoutResource(R.layout.custom_list_setting);
            getPreferenceScreen().addPreference(listPreference);
            String highestVideoDimen = settingsPrefs.getString(Constants.VIDEO_DIMENSION_HIGH, null);
            if(highestVideoDimen != null){
                StringTokenizer tokenizer = new StringTokenizer(highestVideoDimen, ":");
                tokenizer.nextToken();
                String highestHeight = tokenizer.nextToken();
                isHighest4K = Integer.parseInt(highestHeight) >= Constants._4K_VIDEO_RESOLUTION;
            }

            listPreference.setOnPreferenceChangeListener((preference,newValue) -> {
                String newRes = (String) newValue;
                if(VERBOSE)Log.d(TAG, "onPreferenceChange = " + newRes);
                if(VERBOSE)Log.d(TAG, "onPreferenceChange pref = " + preference.getKey());
                if(isVideo4K(newRes)){
                    return false;
                }
                return true;
            });
            //Show Memory
            final CheckBoxPreference memoryConsumedPref = new ShutterCheckboxPreference(getActivity(), true, Constants.SHOW_MEMORY_CONSUMED_MSG);
            memoryConsumedPref.setTitle(resources.getString(R.string.showMemConsumed));
            memoryConsumedPref.setSummary(resources.getString(R.string.showMemConsumedMsg));
            memoryConsumedPref.setKey(Constants.SHOW_MEMORY_CONSUMED_MSG);
            memoryConsumedPref.setLayoutResource(R.layout.shutter_checkbox_setting);
            boolean memCon = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(Constants.SHOW_MEMORY_CONSUMED_MSG, false);
            if(VERBOSE)Log.d(TAG, "MEMORY CONSUMED PREF MGR = "+memCon);
            getPreferenceScreen().addPreference(memoryConsumedPref);
            //Video Player
            ListPreference playerPreference = new CustomListPreference(getActivity(), true);
            Set<String> playerEntries = new LinkedHashSet<>();
            playerEntries.add(resources.getString(R.string.videoFCPlayer));
            playerEntries.add(resources.getString(R.string.videoExternalPlayer));
            CharSequence[] playerSummaries = new CharSequence[playerEntries.size()];
            index = 0;
            Iterator<String> playerIter = playerEntries.iterator();
            while (playerIter.hasNext()) {
                String resol = playerIter.next();
                playerSummaries[index++] = resol;
            }
            playerPreference.setEntries(playerSummaries);
            playerPreference.setEntryValues(playerSummaries);
            playerPreference.setTitle(resources.getString(R.string.videoPlayerHeading));
            playerPreference.setSummary(resources.getString(R.string.videoPlayerSummary));
            playerPreference.setKey(Constants.SELECT_VIDEO_PLAYER);
            playerPreference.setValue(settingsPrefs.getString(Constants.SELECT_VIDEO_PLAYER,
                    resources.getString(R.string.videoExternalPlayer)));
            playerPreference.setDialogTitle(getResources().getString(R.string.videoPlayerHeading));
            playerPreference.setLayoutResource(R.layout.custom_list_setting);
            getPreferenceScreen().addPreference(playerPreference);
            playerPreference.setOnPreferenceChangeListener((preference,newValue ) -> {
                String newRes = (String) newValue;
                if(VERBOSE)Log.d(TAG, "onPreferenceChange 2222 = " + newRes);
                if(VERBOSE)Log.d(TAG, "onPreferenceChange pref 2222 = " + preference.getKey());
                return true;
            });
        }

        private boolean isVideo4K(String selectedVal){
            if(selectedVal.equalsIgnoreCase(getString(R.string.videoResHigh)) && isHighest4K){
                Toast _4KNotSupportedMessage = new Toast(getActivity());
                _4KNotSupportedMessage.setGravity(Gravity.CENTER, 0, 0);
                _4KNotSupportedMessage.setDuration(Toast.LENGTH_LONG);
                _4KNotSupportedMessage.setView(LayoutInflater.from(getActivity()).inflate(R.layout.videonotsupported, null));
                _4KNotSupportedMessage.show();
                return true;
            }
            return false;
        }
    }
}
