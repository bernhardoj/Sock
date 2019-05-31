package com.indevelopment.sock.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.indevelopment.sock.Alarm;
import com.indevelopment.sock.model.Rule;

import java.util.Set;

public class AfterBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("rule_list", Context.MODE_PRIVATE);
            Set<String> set = sharedPreferences.getStringSet("rule", null);
            Gson gson = new Gson();
            Rule rule;

            // Re-set all the cancelled alarm caused by phone reboot
            assert set != null;
            for (String sets : set) {
                rule = gson.fromJson(sets, Rule.class);
                Alarm.setAlarm(context, rule);
            }
        }
    }
}
