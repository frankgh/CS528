package com.frankgh.project3;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

/**
 * @author Rom
 */

public class GeoFenceTransitionService extends IntentService {
    public static final String BROADCAST_ACTION = "GeoFenceTransitionService#GeoFenceTransition";
    private static final String TAG = "GeoFenceService";

    public GeoFenceTransitionService() {
        super(TAG);
    }

    private static String getErrorString(int errorCode) {
        Log.d(TAG, "entered getErrorString");
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "GeoFence not available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Too many GeoFences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "Too many pending intents";
            default:
                return "Unknown error.";
        }
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "entered onHandleIntent");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        // Handling errors
        if (geofencingEvent.hasError()) {
            String errorMsg = getErrorString(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMsg);
            return;
        }

        int geoFenceTransition = geofencingEvent.getGeofenceTransition();
        // Check if the transition type is of interest
        Log.d(TAG, "geoFenceTransition: " + Integer.toString(geoFenceTransition));
        if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geoFenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Send broadcast
            Log.d(TAG, "Broadcasting GeoFenceTransition transition event: " + geoFenceTransition);
            Intent broadcastIntent = new Intent(BROADCAST_ACTION);
            broadcastIntent.putExtra("geoFenceTransitionCode", geoFenceTransition);
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
        }
    }
}
