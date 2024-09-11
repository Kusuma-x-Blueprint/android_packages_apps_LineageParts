/*
 * SPDX-FileCopyrightText: 2024 Kusuma
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.profiles;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import lineageos.app.Profile;

import java.util.Calendar;
import java.util.UUID;

public class TimeProfileUtils {

    private static final String TAG = "TimeProfileUtils";
    private static final String INTENT_ACTION_PROFILE_TIME_TRIGGER = 
            "lineageos.extra.platform.intent.action.PROFILE_TIME_TRIGGER";

    /*
     * Schedules an alarm to trigger at a specific time.
     */
    public static void scheduleAlarm(Context context, Calendar time, Profile profile, int alarmIndex) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(INTENT_ACTION_PROFILE_TIME_TRIGGER);
        intent.putExtra("PROFILE_UUID", profile.getUuid().toString());
        String uniqueCode = alarmIndex + profile.getUuid().toString();
        int requestCode = uniqueCode.hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | 
                PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pendingIntent);
            Log.i(TAG, "Alarm number " + alarmIndex + " scheduled for profile UUID: " + 
                    profile.getUuid().toString() + " at " + time.getTime());
        }
    }

    /*
     * Cancels the scheduled alarm.
     */
    public static void cancelAlarm(Context context, Profile profile, int alarmIndex) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(INTENT_ACTION_PROFILE_TIME_TRIGGER);
        intent.putExtra("PROFILE_UUID", profile.getUuid().toString());
        String uniqueCode = alarmIndex + profile.getUuid().toString();
        int requestCode = uniqueCode.hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | 
                PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.i(TAG, "Alarm number " + alarmIndex + " canceled for profile UUID: " + 
                    profile.getUuid().toString());
        }
    }

}
