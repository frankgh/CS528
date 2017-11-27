package com.frankgh.wpiparking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 11/27/17.
 */

public class RestartService extends BroadcastReceiver {

    private static final String TAG = "RestartService";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Alarm for RestartService...");
        if (!ApplicationUtils.isServiceRunning(context, ParkingService.class.getName())) {
            Log.d(TAG, "Service is NOT running...");
            context.startService(new Intent(context, ParkingService.class));
        } else {
            Log.d(TAG, "Service is running...");
        }
    }
}
