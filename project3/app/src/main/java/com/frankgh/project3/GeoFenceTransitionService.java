package com.frankgh.project3;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rom
 */

public class GeoFenceTransitionService extends IntentService {
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
        Log.d(TAG, "geofenceTrans: " + Integer.toString(geoFenceTransition));
        if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geoFenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Get the geoFence that were triggered
            List<Geofence> triggeringGeoFences = geofencingEvent.getTriggeringGeofences();
            String geoFenceTransitionDetails = getGeoFenceTransitionDetails(geoFenceTransition, triggeringGeoFences);

            sendNotification(geoFenceTransitionDetails);
        }
    }

    private void sendNotification(String msg) {

    }

    private String getGeoFenceTransitionDetails(int geoFenceTransition, List<Geofence> triggeringGeofences) {
        // get the ID of each geoFence triggered
        Log.d(TAG, "entered getGeoFenceTransitionDetails");
        List<String> triggeringGeoFencesList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeoFencesList.add(geofence.getRequestId());
        }

        String status = null;
        if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            status = "Entering ";
        } else if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            status = "Exiting ";
        }
        return status + TextUtils.join(", ", triggeringGeoFencesList);
    }
}
