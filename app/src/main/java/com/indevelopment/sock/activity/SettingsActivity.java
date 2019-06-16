package com.indevelopment.sock.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.android.billingclient.api.Purchase;
import com.indevelopment.sock.billing.BillingManager;
import com.indevelopment.sock.R;
import com.indevelopment.sock.adapter.MyArrayAdapter;
import com.indevelopment.sock.data.LicenseData;

public class SettingsActivity extends AppCompatActivity {
    private static final String DARK_MODE_KEY = "dark_mode";
    private static final String UNLOCK_PREMIUM_KEY = "unlock_premium";
    private static final String LICENSES_KEY = "licenses";
    private static final String RATE_APP_KEY = "rate_app";
    private static final String PRIVACY_POLICY_KEY = "privacy_policy";

    private static final String PACKAGE_NAME = "com.indevelopment.sock";

    private static final String TAG = "SettingsActivity";

    public static String unlockedString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            setTheme(R.style.DarkModeAppTheme);
        } else {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        Log.d(TAG, "Activity created.");

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        BillingManager mBillingManager;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Log.d(TAG, "PreferenceFragment created.");
            final SwitchPreferenceCompat darkModeSwitch = findPreference(DARK_MODE_KEY);
            final Preference unlockPremium = findPreference(UNLOCK_PREMIUM_KEY);
            final Preference licenseView = findPreference(LICENSES_KEY);
            final Preference rateApp = findPreference(RATE_APP_KEY);
            final Preference privacyPolicy = findPreference(PRIVACY_POLICY_KEY);

            unlockedString = getResources().getString(R.string.unlocked);

            mBillingManager = new BillingManager(getActivity(), new BillingManager.BillingUpdatesListener() {
                @Override
                public void onPurchasesUpdated(Purchase purchase) {
                    if (purchase.getSku().equals(BillingManager.ITEM_SKU)) {
                        if (unlockPremium != null && darkModeSwitch != null) {
                            darkModeSwitch.setEnabled(true);
                            unlockPremium.setSummary(unlockedString);
                        } else {
                            Log.w(TAG, "Dark Mode switch or Unlock PREMIUM preference is null");
                        }
                    }
                }
            });

            // Handle the dark mode here
            if (darkModeSwitch != null) {
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
                        return true;
                    }
                });
            } else {
                Log.w(TAG, "Dark mode switch is null");
            }

            // Handle the Google Play billing here
            if (unlockPremium != null) {
                unlockPremium.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        mBillingManager.initiatePurchaseFlow(0);
                        return true;
                    }
                });
            } else {
                Log.w(TAG, "Unlock PREMIUM preference is null");
            }

            if (licenseView != null) {
                licenseView.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Activity activity = getActivity();
                        if (activity != null) {
                            final MyArrayAdapter adapter =
                                    new MyArrayAdapter(activity, R.layout.license_layout, LicenseData.generateLicense());

                            new AlertDialog.Builder(activity)
                                    .setTitle(getResources().getString(R.string.license))
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
            } else {
                Log.w(TAG, "License preference is null");
            }

            if (rateApp != null) {
                rateApp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        // Intent to Play Store here
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("market://details?id=" + PACKAGE_NAME));
                        startActivity(intent);
                        return true;
                    }
                });
            } else {
                Log.w(TAG, "Rate app preference is null");
            }

            if (privacyPolicy != null) {
                privacyPolicy.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        // Intent to Privacy Policy page here
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("https://bernhardoj.github.io/Sock/PrivacyPolicy"));
                        startActivity(intent);
                        return true;
                    }
                });
            } else {
                Log.w(TAG, "Privacy policy preference is null");
            }
        }

        private void restartActivity() {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            Log.d(TAG, "Activity restarted.");
        }

        @Override
        public void onResume() {
            super.onResume();
            mBillingManager.queryPurchases();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mBillingManager.destroy();
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