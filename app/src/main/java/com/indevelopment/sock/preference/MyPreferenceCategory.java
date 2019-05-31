package com.indevelopment.sock.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;

import com.indevelopment.sock.R;
import com.indevelopment.sock.adapter.RuleAdapter;

public class MyPreferenceCategory extends PreferenceCategory {
    public MyPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public MyPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MyPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyPreferenceCategory(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView summary = (TextView) holder.findViewById(android.R.id.summary);

        summary.setAlpha(RuleAdapter.MEDIUM_EMPHASIZE_TEXT);

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            summary.setTextColor(getContext().getResources().getColor(android.R.color.white));
        } else {
            summary.setTextColor(getContext().getResources().getColor(android.R.color.black));
        }
    }
}
