package com.indevelopment.sock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.AlarmManagerCompat;

import com.indevelopment.sock.activity.AddNewRuleActivity;
import com.indevelopment.sock.data.RuleData;
import com.indevelopment.sock.model.Rule;
import com.indevelopment.sock.receiver.RuleAlarm;

import java.util.Calendar;

public class Alarm {
    private static final String TAG = "Alarm";

    public static void setAlarm(Context context, Rule rule) {
        String[] timeSplit = rule.getStartRule().split(":");
        int day = rule.getDayIdx();
        int requestCode = rule.getRequestCode();
        boolean isRepeating = rule.isRepeating();

        Calendar calendar = Calendar.getInstance();

        int hour = Integer.parseInt(timeSplit[0]);
        int minute = Integer.parseInt(timeSplit[1]);
        int days = (day - calendar.get(Calendar.DAY_OF_WEEK)) + calendar.get(Calendar.DAY_OF_MONTH);

        calendar.set(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                days,
                hour,
                minute,
                0
        );

        // Set the alarm to the next week if the time is behind
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 7);
        }

        Log.d(TAG, "setAlarm: days:" + days + "minute: " + minute + "millis:" + calendar.getTimeInMillis());

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RuleAlarm.class);

        Log.d(TAG, "setAlarm: " + rule.getRuleName());
        intent.putExtra(RuleAlarm.NOTIFICATION_NAME, rule.getRuleName());
        intent.putExtra(RuleAlarm.NOTIFICATION_DETAIL, rule.getStartRule());
        intent.putExtra(RuleAlarm.REPEAT, rule.isRepeating());
        intent.putExtra(RuleAlarm.ACTIONS, rule.getAllActions());
        intent.putExtra(AddNewRuleActivity.RULE_INDEX, RuleData.rules.indexOf(rule));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Set the alarm to be fired every week on the selected day
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
        } else {
            if (isRepeating) {
                alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), 7 * 24 * 60 * 60 * 1000, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
            }
        }
    }

    public static void cancelAlarm(Context context, int reqCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RuleAlarm.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reqCode, intent, 0);
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Alarm cancelled");
    }
}