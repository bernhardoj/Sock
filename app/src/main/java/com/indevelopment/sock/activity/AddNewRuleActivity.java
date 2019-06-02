package com.indevelopment.sock.activity;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.indevelopment.sock.data.RuleData;
import com.indevelopment.sock.Alarm;
import com.indevelopment.sock.model.Rule;
import com.indevelopment.sock.R;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class AddNewRuleActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TITLE = "TITLE";
    public static final String RULE = "RULE";
    public static final String RULE_INDEX = "RULE_INDEX";
    public static final String ALARM_REQUEST_CODE = "ALARM_REQUEST_CODE";

    boolean isEdit = false;
    boolean[] actions;
    int ruleIndex, mRequestCode;
    String[] dayArrays;

    Spinner day;
    TimePickerDialog timePickerDialog;
    TextInputEditText ruleName_edt, rule_startTime;
    TextInputLayout ruleName_layout, rule_startTime_layout;

    CheckBox muteAll_cBox, shutDown_cBox, repeat_cBox;
    TextView action_tv;
    ScrollView scrollView;
    LinearLayout actions_layout;

    Rule rule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            setTheme(R.style.DarkModeAppTheme);
        } else {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_rule);

        dayArrays = getResources().getStringArray(R.array.day_arrays);
        day = findViewById(R.id.day_spinner);
        rule_startTime = findViewById(R.id.startTime_ed);
        ruleName_edt = findViewById(R.id.rule_name_edt);
        ruleName_layout = findViewById(R.id.ruleName_layout);
        rule_startTime_layout = findViewById(R.id.ruleStart_layout);
        muteAll_cBox = findViewById(R.id.muteAll_cBox);
        shutDown_cBox = findViewById(R.id.shutDown_cBox);
        repeat_cBox = findViewById(R.id.repeat_cBox);
        action_tv = findViewById(R.id.action_tv);
        scrollView = findViewById(R.id.scrollView);
        actions_layout = findViewById(R.id.actions_layout);

        // Set the actions size based on total Action CheckBox available
        actions = new boolean[actions_layout.getChildCount()];

        // Set the day spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, dayArrays);
        Calendar calendar = Calendar.getInstance();

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        day.setAdapter(adapter);
        day.setSelection(calendar.get(Calendar.DAY_OF_WEEK) - 1);

        // Check whether the rule is going to be edited
        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            // Tell the activity that the rule is in edit state
            isEdit = true;

            ruleIndex = bundle.getInt(RULE_INDEX);
            mRequestCode = bundle.getInt(ALARM_REQUEST_CODE);

            // Set activity label name
            this.setTitle(bundle.getString(TITLE));

            rule = bundle.getParcelable(RULE);

            // Set all fields got from parcelable rule
            if (rule != null) {
                ruleName_edt.setText(rule.getRuleName());
                rule_startTime.setText(rule.getStartRule());
                day.setSelection(adapter.getPosition(rule.getDay()));
                muteAll_cBox.setChecked(rule.getActions(Rule.ACTION_MUTE_ALL));
                shutDown_cBox.setChecked(rule.getActions(Rule.ACTION_SHUTDOWN));
                actions[Rule.ACTION_MUTE_ALL] = rule.getActions(Rule.ACTION_MUTE_ALL);
                actions[Rule.ACTION_SHUTDOWN] = rule.getActions(Rule.ACTION_SHUTDOWN);
                repeat_cBox.setChecked(rule.isRepeating());
            }
        }

        rule_startTime.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                rule_startTime_layout.setError(null);
                rule_startTime_layout.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        rule_startTime.setOnClickListener(this);
        muteAll_cBox.setOnClickListener(this);
        shutDown_cBox.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds done button to the action bar.
        getMenuInflater().inflate(R.menu.new_rule_action, menu);

        // Show remove button while in edit rule state
        menu.getItem(0).setVisible(isEdit);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        String ruleName;
        String ruleStartTime;
        final String ruleDay = day.getSelectedItem().toString();
        final boolean isRepeating = repeat_cBox.isChecked();
        if (ruleName_edt.getText() != null) {
            ruleName = ruleName_edt.getText().toString().trim();
        } else {
            ruleName = "";
        }
        if (rule_startTime.getText() != null) {
            ruleStartTime = rule_startTime.getText().toString();
        } else {
            ruleStartTime = "";
        }

        // Spinner index starts from zero,
        // to match with the Calendar day indexing, we need to plus it one
        int dayIdx = day.getSelectedItemPosition() + 1;

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_done:
                boolean isValid = true;

                // Check if the required fields is empty or error found
                if (TextUtils.isEmpty(ruleName)) {
                    ruleName_layout.setErrorEnabled(true);
                    ruleName_layout.setError(getResources().getString(R.string.required_field_text));
                    isValid = false;
                }

                if (TextUtils.isEmpty(ruleStartTime)) {
                    ruleName_layout.setErrorEnabled(true);
                    rule_startTime_layout.setError(getResources().getString(R.string.required_field_text));
                    isValid = false;
                }

                if (isActionsEmpty()) {
                    action_tv.setError("");
                    isValid = false;
                }

                if (isValid) {
                    // Create or edit rule here
                    if (!isEdit) {
                        UUID uuid = UUID.randomUUID();
                        int requestCode = uuid.hashCode();

                        Rule rule = new Rule(ruleName, ruleDay,
                                ruleStartTime, actions, true, dayIdx, requestCode, isRepeating);

                        RuleData.rules.add(rule);

                        // Set the alarm
                        Alarm.setAlarm(getApplicationContext(), rule);
                    } else {
                        RuleData.rules.get(ruleIndex).setRuleName(ruleName);
                        RuleData.rules.get(ruleIndex).setStartRule(ruleStartTime);
                        RuleData.rules.get(ruleIndex).setDay(ruleDay);
                        RuleData.rules.get(ruleIndex).setActions(Rule.ACTION_MUTE_ALL, muteAll_cBox.isChecked());
                        RuleData.rules.get(ruleIndex).setActions(Rule.ACTION_SHUTDOWN, shutDown_cBox.isChecked());
                        RuleData.rules.get(ruleIndex).setRepeating(isRepeating);
                        RuleData.rules.get(ruleIndex).setDayIdx(dayIdx);
                        RuleData.rules.get(ruleIndex).setSwitched(true);

                        // Update the alarm
                        //Alarm.cancelAlarm(getApplicationContext(), mRequestCode);
                        Alarm.setAlarm(getApplicationContext(), RuleData.rules.get(ruleIndex));
                    }
                    MainActivity.isRecentlyAdded = true;
                    MainActivity.recentlyAddedName = ruleName;

                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
                break;
            case R.id.remove_rule:
                RuleData.rules.remove(ruleIndex);

                // Remove the alarm
                Alarm.cancelAlarm(getApplicationContext(), mRequestCode);

                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startTime_ed:
                showTimePickerDialog(v.getId());
                break;
            case R.id.muteAll_cBox:
                if (muteAll_cBox.isChecked()) {
                    actions[Rule.ACTION_MUTE_ALL] = true;
                    action_tv.setError(null);
                } else {
                    actions[Rule.ACTION_MUTE_ALL] = false;
                }
                break;
            case R.id.shutDown_cBox:
                if (shutDown_cBox.isChecked()) {
                    if (isRootGranted()) {
                        shutDown_cBox.setChecked(true);
                        actions[Rule.ACTION_SHUTDOWN] = true;
                    } else {
                        shutDown_cBox.setChecked(false);
                    }
                } else {
                    actions[Rule.ACTION_SHUTDOWN] = false;
                }
                break;
        }
    }

    private boolean isRootGranted() {
        Process p;
        try {
            p = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Your device is not rooted", Toast.LENGTH_SHORT).show();
            return false;
        }

        int result;
        try {
            result = p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Toast.makeText(this, "You need to grant root access", Toast.LENGTH_SHORT).show();
            return false;
        }

        if(result != 0) {
            Toast.makeText(this, "You need to grant root access", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void showTimePickerDialog(int editTextId) {
        final EditText rule_time = findViewById(editTextId);
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        timePickerDialog = new TimePickerDialog(AddNewRuleActivity.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute1) {
                try {
                    String time = String.format(Locale.US, "%02d:%02d", hourOfDay, minute1);
                    rule_time.setText(time);
                    ruleName_layout.setError(null);
                    ruleName_layout.setErrorEnabled(false);
                } catch (NullPointerException np) {
                    Toast.makeText(AddNewRuleActivity.this, "Error, please use send feedback to report the bug!", Toast.LENGTH_SHORT).show();
                }
            }
        }, hour, minute, false);
        timePickerDialog.show();
    }

    private boolean isActionsEmpty() {
        for (boolean b : actions) {
            if (b) return false;
        }
        return true;
    }
}