package io.github.cjybyjk.systemupdater;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.Set;

import io.github.cjybyjk.systemupdater.R;

public class SettingsActivity extends AppCompatActivity {

    private final static String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.action_settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private SharedPreferences mSharedPreferences;
        private MultiSelectListPreference mPrefAutoDownload;
        private MultiSelectListPreference mPrefUpdateType;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            mPrefAutoDownload = findPreference("auto_download");
            mPrefUpdateType = findPreference("update_type");

            OnPreferenceChange(mPrefAutoDownload, null);
            OnPreferenceChange(mPrefUpdateType, null);

            mPrefAutoDownload.setOnPreferenceChangeListener(new MultiSelectListPreference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return OnPreferenceChange(preference, newValue);
                }
            });

            mPrefUpdateType.setOnPreferenceChangeListener(new MultiSelectListPreference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return OnPreferenceChange(preference, newValue);
                }
            });
        }


        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }

        private boolean OnPreferenceChange(Preference preference, @Nullable Object newValue) {
            try {
                if (preference == mPrefAutoDownload) {
                    Set<String> prefsValue = (Set) newValue;
                    if (prefsValue == null) {
                        prefsValue = mSharedPreferences.getStringSet(preference.getKey(), prefsValue);
                    }
                    if (prefsValue.contains("wifi") && prefsValue.contains("data")) {
                        preference.setSummary(R.string.setting_auto_updates_download_wifi_and_data);
                    } else if (prefsValue.contains("wifi")) {
                        preference.setSummary(R.string.setting_auto_updates_download_wifi);
                    } else if (prefsValue.contains("data")) {
                        preference.setSummary(R.string.setting_auto_updates_download_data);
                    } else {
                        preference.setSummary(R.string.setting_auto_updates_download_never);
                    }
                } else if (preference == mPrefUpdateType) {
                    Set<String> prefsValue = (Set) newValue;
                    if (prefsValue == null) {
                        prefsValue = mSharedPreferences.getStringSet(preference.getKey(), prefsValue);
                    }
                    if (prefsValue.contains("full") && prefsValue.contains("incremental")) {
                        preference.setSummary(R.string.setting_update_type_all);
                    } else if (prefsValue.contains("full")) {
                        preference.setSummary(R.string.text_update_type_full);
                    } else if (prefsValue.contains("incremental")) {
                        preference.setSummary(R.string.text_update_type_incremental);
                    } else {
                        preference.setSummary("");
                    }
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            } finally {
                return true;
            }
        }
    }
}