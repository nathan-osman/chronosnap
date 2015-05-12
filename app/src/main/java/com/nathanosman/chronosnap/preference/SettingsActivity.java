package com.nathanosman.chronosnap.preference;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.nathanosman.chronosnap.R;


/**
 * Configurable settings for the application
 *
 * Currently, all settings are displayed within a single PreferenceFragment.
 * As the number of settings grows, I expect that they will need to be grouped
 * differently.
 */
public class SettingsActivity extends PreferenceActivity {
    private static Context mContext;

    /**
     * Fragment populated with settings
     */
    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            // Ensure that the summary is updated when preferences change
            bindPreferenceSummaryToValue(R.string.pref_interval_key, R.string.pref_interval_default);
            bindPreferenceSummaryToValue(R.string.pref_limit_key, R.string.pref_limit_default);
            bindPreferenceSummaryToValue(R.string.pref_camera_key, R.string.pref_camera_default);
            bindPreferenceSummaryToValue(R.string.pref_focus_key, R.string.pref_focus_default);
        }

        /**
         * When a preference changes value, the summary should be updated
         */
        private static Preference.OnPreferenceChangeListener sListener = new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String stringValue = newValue.toString();

                // The method for updating the summary differs based on the preference type
                if (preference instanceof TimeIntervalPreference) {
                    preference.setSummary(((TimeIntervalPreference) preference).getSummary(stringValue));
                } else if (preference instanceof ListPreference) {
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);

                    preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
                } else {
                    preference.setSummary(stringValue);
                }

                return true;
            }
        };

        /**
         * Bind a preference's value to its summary and load the initial value
         */
        private void bindPreferenceSummaryToValue(int keyId, int resId) {

            // Find the preference and bind the listener
            Preference preference = findPreference(getString(keyId));
            preference.setOnPreferenceChangeListener(sListener);

            sListener.onPreferenceChange(preference, PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getString(getString(keyId), getString(resId)));
        }
    }

    /**
     * Display the settings fragment
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    /**
     * Verify that the provided fragment name matches SettingsFragment
     */
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SettingsFragment.class.getName().equals(fragmentName);
    }
}
