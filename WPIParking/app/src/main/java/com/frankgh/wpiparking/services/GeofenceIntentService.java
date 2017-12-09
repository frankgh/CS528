package com.frankgh.wpiparking.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 12/8/17.
 */

public class GeofenceIntentService extends IntentService {

    private static final String TAG = "GeofenceIS";

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public GeofenceIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    public GeofenceIntentService(Context context) {
        super(TAG);
    }

    /**
     * Handles incoming intents.
     *
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        GeofenceJobIntentService.enqueueWork(this, intent);
    }
}
