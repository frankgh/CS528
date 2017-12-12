package com.frankgh.wpiparking.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.frankgh.wpiparking.services.FenceJobIntentService;

/**
 * A basic BroadcastReceiver to handle intents from from the Awareness API.
 *
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 12/8/17.
 */
public class FenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "FenceBR";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        FenceJobIntentService.enqueueWork(context, intent);
    }
}
