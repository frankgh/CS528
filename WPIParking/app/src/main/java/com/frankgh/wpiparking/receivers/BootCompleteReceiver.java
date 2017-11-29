package com.frankgh.wpiparking.receivers;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 11/28/17.
 */
public class BootCompleteReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = "BootCompleteReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "BootCompleteReceiver");
        //Do what you want/Register Geofences
    }
}
