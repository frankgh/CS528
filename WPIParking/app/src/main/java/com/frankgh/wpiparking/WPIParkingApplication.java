package com.frankgh.wpiparking;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Date;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 11/21/17.
 */

public class WPIParkingApplication extends Application {

    private static final String TAG = "WPIParkingApplication";
    private static final int REQUEST_CODE = 687;

    @Override
    public void onCreate() {
        super.onCreate();
        setupAlarmManager();
    }

    private void setupAlarmManager() {
        Log.d(TAG, "setupAlarmManager invoked");
        Intent intent = new Intent(this, RestartService.class);
        PendingIntent recurringIntent = PendingIntent.getBroadcast(this, REQUEST_CODE,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Date firstLog = new Date();
        firstLog.setTime(firstLog.getTime() + 3000);

        final long INTERVAL_ONE_MINUTE = 60 * 1000;

        // Log repetition
        alarms.setRepeating(
                AlarmManager.RTC_WAKEUP,
                firstLog.getTime(),
                //AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                INTERVAL_ONE_MINUTE,
                recurringIntent);
    }

//    private void startParkingService() {
//        Log.d(TAG, "startParkingService");
//        // Start the Parking Service
//        doBindService();
//        Intent i = new Intent(getApplicationContext(), ParkingService.class);
//        getApplicationContext().startService(i);
//    }
}
