package com.indevelopment.sock.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.indevelopment.sock.BillingManager;
import com.indevelopment.sock.R;
import com.indevelopment.sock.adapter.MyArrayAdapter;
import com.indevelopment.sock.data.LicenseData;

public class SettingsActivity extends AppCompatActivity {
    private static final String DARK_MODE_KEY = "dark_mode";
    private static final String UNLOCK_PREMIUM_KEY = "unlock_premium";
    private static final String LICENSES_KEY = "licenses";
    private static final String RATE_APP_KEY = "rate_app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            setTheme(R.style.DarkModeAppTheme);
        } else {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat{
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            final SwitchPreferenceCompat darkModeSwitch = findPreference(DARK_MODE_KEY);
            final Preference unlockPremium = findPreference(UNLOCK_PREMIUM_KEY);
            final Preference licenseView = findPreference(LICENSES_KEY);
            final Preference rateApp = findPreference(RATE_APP_KEY);

            // Handle the dark mode here
            if(darkModeSwitch != null) {
                if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                    darkModeSwitch.setChecked(true);
                }

                darkModeSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (darkModeSwitch.isChecked()) {
                            // Set the dark mode
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            darkModeSwitch.setChecked(false);
                            restartActivity();
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            darkModeSwitch.setChecked(true);
                            restartActivity();
                        }
                        return false;
                    }
                });
            }

            // Handle the Google Play billing here
            if(unlockPremium != null) {
                unlockPremium.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new BillingManager(getActivity());
                        return true;
                    }
                });
            }

            if(licenseView != null) {
                licenseView.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Activity activity = getActivity();
                        if(activity != null) {
                            final MyArrayAdapter adapter =
                                    new MyArrayAdapter(activity, R.layout.license_layout, LicenseData.generateLicense());

                            new AlertDialog.Builder(activity)
                                    .setTitle("License")
                                    .setAdapter(adapter, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Intent to browser here
                                            String targetUrl = null;
                                            switch (which) {
                                                case 0:
                                                    targetUrl = "https://github.com/google/gson";
                                                    break;
                                                case 1:
                                                    targetUrl = "https://github.com/maoni-app/maoni";
                                                    break;
                                            }
                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                            intent.setData(Uri.parse(targetUrl));
                                            startActivity(intent);
                                        }
                                    })
                                    .create()
                                    .show();
                            return true;
                        }

                        return false;
                    }
                });
            }

            if(rateApp != null) {
                rateApp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        // Intent to App Store here

                        return true;
                    }
                });
            }
        }

        private void restartActivity() {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}