package com.frankgh.wpiparking;

import android.app.Application;
import android.util.Log;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 11/21/17.
 */

public class WPIParkingApplication extends Application {

    private static final String TAG = "WPIParkingApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        setupAlarmManager();
    }

    private void setupAlarmManager() {
        Log.d(TAG, "setupAlarmManager invoked");
//        Intent intent = new Intent(this, RestartService.class);
//        PendingIntent recurringIntent = PendingIntent.getBroadcast(this, REQUEST_CODE,
//                intent, PendingIntent.FLAG_CANCEL_CURRENT);
//        AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//
//        Date firstLog = new Date();
//        firstLog.setTime(firstLog.getTime() + 3000);
//
//        final long INTERVAL_ONE_MINUTE = 60 * 1000;
//
//        // Log repetition
//        alarms.setRepeating(
//                AlarmManager.RTC_WAKEUP,
//                firstLog.getTime(),
//                //AlarmManager.INTERVAL_FIFTEEN_MINUTES,
//                INTERVAL_ONE_MINUTE,
//                recurringIntent);
    }
}
