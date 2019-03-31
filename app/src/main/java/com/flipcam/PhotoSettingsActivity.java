package com.flipcam;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
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
import com.flipcam.preferences.CustomCheckboxPreference;
import com.flipcam.preferences.ResolutionListPreference;
import com.flipcam.preferences.SelfieTimerPreference;

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
            final CheckBoxPreference shutterSoundPref = new CustomCheckboxPreference(getActivity(), true, Constants.SHUTTER_SOUND);
            shutterSoundPref.setTitle(getResources().getString(R.string.enableShutterSound));
            shutterSoundPref.setSummary(getResources().getString(R.string.enableShutterSoundMsg));
            shutterSoundPref.setKey(Constants.SHUTTER_SOUND);
            shutterSoundPref.setLayoutResource(R.layout.custom_checkbox_setting);
            boolean shutterSound = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(Constants.SHUTTER_SOUND, false);
            if(VERBOSE)Log.d(TAG, "SHUTTER SOUND PREF MGR = "+shutterSound);
            getPreferenceScreen().addPreference(shutterSoundPref);
            //Add Selfie Timer prefs
            SelfieTimerPreference selfieTimerPreference = new SelfieTimerPreference(getActivity(), true, Constants.SELFIE_TIMER);
            selfieTimerPreference.setTitle(getResources().getString(R.string.selfieTimerSettingTitle));
            selfieTimerPreference.setSummary(getResources().getString(R.string.selfieTimerSettingSummary));
            selfieTimerPreference.setKey(Constants.SELFIE_TIMER);
            selfieTimerPreference.setPersistent(true);
            selfieTimerPreference.setLayoutResource(R.layout.custom_selfietimer_setting);
            selfieTimerPreference.setDialogLayoutResource(R.layout.timerpicker);
            getPreferenceScreen().addPreference(selfieTimerPreference);
        }

        private void addResolutionList(boolean backCamera){
            Log.d(TAG, "for backCamera? = "+backCamera);
            ListPreference listPreference;
            Set<String> entries;
            SharedPreferences settingsPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            if(backCamera) {
                listPreference = new ResolutionListPreference(getActivity(), true);
                entries = settingsPrefs.getStringSet(Constants.SUPPORT_PHOTO_RESOLUTIONS, null);
            }
            else{
                listPreference = new ResolutionListPreference(getActivity(), true);
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
                listPreference.setDialogTitle(getResources().getString(R.string.photoResolutionDialogHeadingRear));
                listPreference.setTitle(getResources().getString(R.string.backCamResTitle));
                listPreference.setSummary(getResources().getString(R.string.backCamResSummary));
                listPreference.setKey(Constants.SELECT_PHOTO_RESOLUTION);
                listPreference.setValue(settingsPrefs.getString(Constants.SELECT_PHOTO_RESOLUTION, null));
            }
            else{
                listPreference.setDialogTitle(getResources().getString(R.string.photoResolutionDialogHeadingFront));
                listPreference.setTitle(getResources().getString(R.string.frontCamResTitle));
                listPreference.setSummary(getResources().getString(R.string.frontCamResSummary));
                listPreference.setKey(Constants.SELECT_PHOTO_RESOLUTION_FRONT);
                listPreference.setValue(settingsPrefs.getString(Constants.SELECT_PHOTO_RESOLUTION_FRONT, null));
            }
            listPreference.setLayoutResource(R.layout.custom_photo_setting);
            getPreferenceScreen().addPreference(listPreference);
        }
    }
}
