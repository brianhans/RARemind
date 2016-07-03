package com.brianhans.remind;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by Brian on 7/2/16.
 */
public class AlarmStartUp extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences pref = context.getSharedPreferences(MainActivity.PREFRENCE_NAME, Context.MODE_PRIVATE);
        boolean disabled = pref.getBoolean(MainActivity.DISABLED, true);

        if(!disabled){
            int hour = pref.getInt(MainActivity.HOUR, -1);
            int minute = pref.getInt(MainActivity.MINUTE, -1);

            //Get date for hour and minute
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            if(calendar.HOUR_OF_DAY < hour){
                calendar.add(Calendar.DATE, 1);
            }
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);

            //Set alarm
            Intent alarmIntent = new Intent(context, Alarm.class);
            PendingIntent pendingAlarmIntent = PendingIntent.getBroadcast(context, MainActivity.ALARM_CODE, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY,pendingAlarmIntent);
        }
    }

}
