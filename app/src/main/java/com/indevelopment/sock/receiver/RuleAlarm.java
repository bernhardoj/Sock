package com.indevelopment.sock.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.util.Log;

import com.indevelopment.sock.R;
import com.indevelopment.sock.activity.AddNewRuleActivity;
import com.indevelopment.sock.activity.MainActivity;
import com.indevelopment.sock.adapter.RuleAdapter;
import com.indevelopment.sock.data.RuleData;
import com.indevelopment.sock.model.Rule;

import java.io.IOException;

public class RuleAlarm extends BroadcastReceiver {
    public static final String NOTIFICATION_NAME = "NOTIFICATION_NAME";
    public static final String NOTIFICATION_DETAIL = "NOTIFICATION_DETAIL";
    public static final String REPEAT = "REPEAT";
    public static final String ACTIONS = "ACTIONS";

    private static final String CHANNEL_ID = "rule_channel";
    private static final String CUSTOM_DETAIL_PREFERENCE_KEY = "custom_detail";
    private static final String TAG = "RuleAlarm";
    private static final int NOTIFICATION_ID = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean[] ruleActions = intent.getBooleanArrayExtra(ACTIONS);

        if(ruleActions[Rule.ACTION_MUTE_ALL]) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            // Mute all volume Action here
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_MUTE, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);
            } else {
                audioManager.setStreamMute(AudioManager.STREAM_ALARM, true);
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
                audioManager.setStreamMute(AudioManager.STREAM_RING, true);
                audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
            }

            if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }

            // Wait for 0.1 second
            // Direct changing won't set the Do Not Disturb on
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }

        if(ruleActions[Rule.ACTION_SHUTDOWN]) {
            try {
                Runtime.getRuntime().exec(new String[]{ "su", "-c", "reboot -p" });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Modify the notification here
        String ruleName = intent.getStringExtra(NOTIFICATION_NAME);
        String ruleStart = intent.getStringExtra(NOTIFICATION_DETAIL);
        boolean isRepeating = intent.getBooleanExtra(REPEAT, true);
        int ruleIndex = intent.getIntExtra(AddNewRuleActivity.RULE_INDEX, 0);

        String ruleTitle = String.format(context.getString(R.string.rule_notification_title), ruleName);
        String ruleDetail = String.format(context.getString(R.string.rule_notification_detail), ruleStart);

        createNotificationChannel(context);

        // Get user custom notification detail
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);

        // Overwrite default notification detail
        // with user custom detail if exist

        String customDetail = sharedPreferences.getString(CUSTOM_DETAIL_PREFERENCE_KEY, "");
        if (customDetail != null) {
            customDetail = customDetail.trim();

            if (!customDetail.equals("")) {
                ruleDetail = customDetail;
            }
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_library_books_notification)
                .setContentTitle(ruleTitle)
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(ruleDetail))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        notificationManager.notify(NOTIFICATION_ID, builder.build());

        // Switch the switch off if not repeating
        if (!isRepeating) {
            RuleData.rules.get(ruleIndex).setSwitched(false);
            // Apply the switch effect in the RecyclerView List
            RuleAdapter adapter = (RuleAdapter) MainActivity.ruleList.getAdapter();
            if(adapter != null) {
                MainActivity.ruleList.getAdapter().notifyDataSetChanged();
            }
        }

        Log.d(TAG, "onReceive: fired");
    }

    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.channel_name);
            String description = context.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}