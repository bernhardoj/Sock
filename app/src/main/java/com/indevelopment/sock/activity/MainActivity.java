package com.indevelopment.sock.activity;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.indevelopment.sock.adapter.RuleAdapter;
import com.indevelopment.sock.data.RuleData;
import com.indevelopment.sock.model.Rule;
import com.indevelopment.sock.R;

import org.rm3l.maoni.Maoni;
import org.rm3l.maoni.email.MaoniEmailListener;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String RULE_PREFERENCE_KEY = "rule";
    private static final String RULE_PREFERENCE_NAME = "rule_list";

    public static boolean isRecentlyAdded = false;
    public static String recentlyAddedName = "";

    private static boolean isLoad = false;

    boolean isBackPressedTwice = false;

    public static RecyclerView ruleList;
    LinearLayout emptyTextLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Check if a dark mode is already on in the settings
        SharedPreferences darkModePreferences = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
        if (darkModePreferences.contains("dark_mode")) {
            boolean isDark = darkModePreferences.getBoolean("dark_mode", false);
            if (isDark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        }

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            setTheme(R.style.DarkModeAppTheme);
        } else {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.rule_lists_activity);

        ruleList = findViewById(R.id.rv);
        emptyTextLayout = findViewById(R.id.empty_layout);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;
                NotificationManager n = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

                // Make sure user grant access to Do not Disturb for Android M and up
                // before continue to add new rule section
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (n.isNotificationPolicyAccessGranted()) {
                        intent = new Intent(getApplicationContext(), AddNewRuleActivity.class);
                    } else {
                        // Ask the user to grant access
                        intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    }
                } else {
                    intent = new Intent(getApplicationContext(), AddNewRuleActivity.class);
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);

            }
        });

        SharedPreferences sharedPreferences = getSharedPreferences(RULE_PREFERENCE_NAME, MODE_PRIVATE);

        // Load rule if a saved rule exist and haven't load
        if (sharedPreferences.contains(RULE_PREFERENCE_KEY) && !isLoad) {
            loadFromSharedPreference();
            isLoad = true;
        }

        if (RuleData.rules.isEmpty()) {
            emptyTextLayout.setVisibility(View.VISIBLE);
        } else {
            final RuleAdapter adapter = new RuleAdapter(RuleData.rules, ruleList);
            ruleList.setLayoutManager(new LinearLayoutManager(MainActivity.this));
            ruleList.setAdapter(adapter);
        }

        // Show the SnackBar when the alarm applied
        if (isRecentlyAdded) {
            String ruleSet = String.format(getString(R.string.rule_set), recentlyAddedName);
            Snackbar.make(ruleList, ruleSet, Snackbar.LENGTH_SHORT).show();
            isRecentlyAdded = false;
            recentlyAddedName = "";
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_feedback:
                final MaoniEmailListener maoniEmailListener =
                        new MaoniEmailListener(this, "id.indevelopment@gmail.com");
                new Maoni.Builder(this, getPackageName() + ".fileprovider")
                        .withListener(maoniEmailListener)
                        .build()
                        .start(MainActivity.this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveToSharedPreference();
    }

    private void saveToSharedPreference() {
        SharedPreferences sharedPreferences = getSharedPreferences(RULE_PREFERENCE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        final Set<String> set = Collections.synchronizedSet(new HashSet<String>());
        Gson gson = new Gson();

        synchronized (RuleData.rules) {
            for (Rule rule : RuleData.rules) {
                String json = gson.toJson(rule);
                set.add(json);
            }
        }

        editor.putStringSet(RULE_PREFERENCE_KEY, set);
        editor.apply();
    }

    private void loadFromSharedPreference() {
        SharedPreferences sharedPreferences = getSharedPreferences(RULE_PREFERENCE_NAME, MODE_PRIVATE);
        final Set<String> set = Collections.synchronizedSet(sharedPreferences.getStringSet(RULE_PREFERENCE_KEY, null));
        Gson gson = new Gson();

        synchronized (set) {
            for (String sets : set) {
                RuleData.rules.add(gson.fromJson(sets, Rule.class));
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isBackPressedTwice) {
            finish();
        }

        isBackPressedTwice = true;
        Toast.makeText(this, "Press Back again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isBackPressedTwice = false;
            }
        }, 2000);
    }
}
