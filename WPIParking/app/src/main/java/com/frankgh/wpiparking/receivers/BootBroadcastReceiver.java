package com.frankgh.wpiparking.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.frankgh.wpiparking.services.RegisterFencesJobIntentService;

/**
 * We need to re-register fences when rebooting
 *
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 12/9/17.
 */
public class BootBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "BootBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "onReceive");
            RegisterFencesJobIntentService.enqueueWork(context, intent);
        }
    }
}
