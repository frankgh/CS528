package com.frankgh.wpiparking.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.frankgh.wpiparking.services.GeofenceJobIntentService;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 12/8/17.
 */
public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceBR";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        GeofenceJobIntentService.enqueueWork(context, intent);
    }
}
