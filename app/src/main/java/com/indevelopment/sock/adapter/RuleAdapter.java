package com.indevelopment.sock.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.indevelopment.sock.activity.AddNewRuleActivity;
import com.indevelopment.sock.Alarm;
import com.indevelopment.sock.model.Rule;
import com.indevelopment.sock.R;

import java.util.List;

public class RuleAdapter extends RecyclerView.Adapter<RuleAdapter.ViewHolder> {

    public static final float MEDIUM_EMPHASIZE_TEXT = 0.6f;
    private static final float HIGH_EMPHASIS_TEXT = 0.87f;
    private static final float DISABLED_TEXT = 0.38f;

    private List<Rule> rules;
    private RecyclerView recyclerView;

    public RuleAdapter(List<Rule> rules, RecyclerView recyclerView) {
        this.rules = rules;
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rule_list, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.bind(rules.get(i), i);
    }

    @Override
    public int getItemCount() {
        return rules.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView ruleName_tv, ruleTime_tv, day_tv;
        SwitchCompat rule_switch;
        String ruleTime;
        LinearLayout linearLayout;
        int requestCode;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ruleName_tv = itemView.findViewById(R.id.rule_name_tv);
            ruleTime_tv = itemView.findViewById(R.id.rule_time_tv);
            rule_switch = itemView.findViewById(R.id.rule_switch);
            day_tv = itemView.findViewById(R.id.day);
            linearLayout = itemView.findViewById(R.id.rule);
        }

        void bind(final Rule rule, final int i) {
            String day = rule.getDay();
            if (rule.isRepeating()) {
                day = "Every " + day;
            }

            ruleTime = rule.getStartRule();
            ruleName_tv.setText(rule.getRuleName());
            ruleTime_tv.setText(ruleTime);
            day_tv.setText(day);
            day_tv.setAlpha(HIGH_EMPHASIS_TEXT);
            requestCode = rule.getRequestCode();

            if (rule.isSwitched()) {
                // Switch the SwitchCompat to On without affecting the Listener
                rule_switch.setOnCheckedChangeListener(null);
                rule_switch.setChecked(true);
            } else {
                ruleName_tv.setTextColor(itemView.getResources().getColor(R.color.grey500));
                day_tv.setTextColor(itemView.getResources().getColor(R.color.grey500));
                ruleTime_tv.setTextColor(itemView.getResources().getColor(R.color.grey500));
                if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                    rule_switch.setTrackTintList(ColorStateList.valueOf
                            (itemView.getResources().getColor(R.color.darkSwitchOffTrackColor)));
                    rule_switch.setThumbTintList(ColorStateList.valueOf
                            (itemView.getResources().getColor(R.color.darkSwitchOffThumbColor)));
                }
                rule_switch.setChecked(false);
            }

            rule_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        Alarm.setAlarm(itemView.getContext(), rule);
                        rule.setSwitched(true);
                        String ruleSet = String.format(itemView.getResources().getString(R.string.rule_set), rule.getRuleName());

                        Snackbar.make(recyclerView, ruleSet, Snackbar.LENGTH_SHORT).show();

                        // Set the rule applied color
                        int colorAccentAttributeId, textColorAttributeId;

                        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                            colorAccentAttributeId = R.color.darkModeColorAccent;
                            textColorAttributeId = R.color.darkModeColorText;
                            rule_switch.setTrackTintList(ColorStateList.valueOf
                                    (itemView.getResources().getColor(R.color.darkSwitchOnTrackColor)));
                        } else {
                            colorAccentAttributeId = R.color.colorAccent;
                            textColorAttributeId = R.color.dayModeColorText;
                            rule_switch.setTrackTintList(ColorStateList.valueOf
                                    (itemView.getResources().getColor(R.color.daySwitchOnTrackColor)));
                        }

                        ruleName_tv.setTextColor(itemView.getResources().getColor(colorAccentAttributeId));
                        day_tv.setTextColor(itemView.getResources().getColor(textColorAttributeId));
                        day_tv.setAlpha(HIGH_EMPHASIS_TEXT);
                        ruleTime_tv.setTextColor(itemView.getResources().getColor(textColorAttributeId));
                        ruleTime_tv.setAlpha(HIGH_EMPHASIS_TEXT);

                        rule_switch.setThumbTintList(ColorStateList.valueOf
                                (itemView.getResources().getColor(colorAccentAttributeId)));
                    } else {
                        if(rule.isSwitched()) {
                            rule.setSwitched(false);
                            Alarm.cancelAlarm(itemView.getContext(), requestCode);

                            // Set the rule unapplied color
                            ruleName_tv.setTextColor(itemView.getResources().getColor(R.color.grey500));
                            day_tv.setAlpha(DISABLED_TEXT);
                            ruleTime_tv.setAlpha(DISABLED_TEXT);

                            if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                                rule_switch.setTrackTintList(ColorStateList.valueOf
                                        (itemView.getResources().getColor(R.color.darkSwitchOffTrackColor)));
                                rule_switch.setThumbTintList(ColorStateList.valueOf
                                        (itemView.getResources().getColor(R.color.darkSwitchOffThumbColor)));
                            } else {
                                rule_switch.setTrackTintList(ColorStateList.valueOf
                                        (itemView.getResources().getColor(R.color.daySwitchOffTrackColor)));
                                rule_switch.setThumbTintList(ColorStateList.valueOf
                                        (itemView.getResources().getColor(R.color.daySwitchOffThumbColor)));
                            }
                        }
                    }
                }
            });

            // Edit rule listener
            linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = v.getContext();
                    Intent intent = new Intent(context, AddNewRuleActivity.class);
                    intent.putExtra(AddNewRuleActivity.RULE_INDEX, i);
                    intent.putExtra(AddNewRuleActivity.RULE, rule);
                    intent.putExtra(AddNewRuleActivity.TITLE, itemView.getResources().getString(R.string.edit_rule_activity));
                    intent.putExtra(AddNewRuleActivity.ALARM_REQUEST_CODE, rule.getRequestCode());
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    context.startActivity(intent);
                }
            });
        }
    }
}