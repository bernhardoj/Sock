package com.indevelopment.sock.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import com.indevelopment.sock.adapter.RuleAdapter;

public class MySwitchPreferenceCompat extends SwitchPreferenceCompat {
    public MySwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public MySwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MySwitchPreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MySwitchPreferenceCompat(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView title = (TextView) holder.findViewById(android.R.id.title);
        TextView summary = (TextView) holder.findViewById(android.R.id.summary);

        summary.setAlpha(RuleAdapter.MEDIUM_EMPHASIZE_TEXT);
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            title.setTextColor(getContext().getResources().getColor(android.R.color.white));
            summary.setTextColor(getContext().getResources().getColor(android.R.color.white));
        } else {
            title.setTextColor(getContext().getResources().getColor(android.R.color.black));
            summary.setTextColor(getContext().getResources().getColor(android.R.color.black));
        }
    }

    @Override
    public void onAttached() {
        super.onAttached();
        setIconSpaceReserved(false);
    }
}
