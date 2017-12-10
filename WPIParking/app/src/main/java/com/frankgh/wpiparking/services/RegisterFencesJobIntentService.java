package com.frankgh.wpiparking.services;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;
import android.widget.Toast;

import com.frankgh.wpiparking.ApplicationUtils;
import com.frankgh.wpiparking.BuildConfig;
import com.frankgh.wpiparking.Constants;
import com.frankgh.wpiparking.R;
import com.frankgh.wpiparking.receivers.GeofenceBroadcastReceiver;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.FenceClient;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.FenceQueryRequest;
import com.google.android.gms.awareness.fence.FenceQueryResponse;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceStateMap;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 12/9/17.
 */

public class RegisterFencesJobIntentService extends JobIntentService implements OnCompleteListener<Void> {
    /**
     * The unique JOB ID
     */
    private static final int JOB_ID = 584;
    private static final String TAG = "RegisterFencesJIS";
    final Handler mHandler = new Handler();
    /**
     * Provides access to the Awareness API.
     */
    private FenceClient mFenceClient;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, RegisterFencesJobIntentService.class, JOB_ID, intent);
    }

    /**
     * Handles incoming intents.
     *
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    public void onHandleWork(Intent intent) {
        Log.d(TAG, "onHandleWork invoked");

        // Get a reference to the awareness fence client
        mFenceClient = Awareness.getFenceClient(this);
        // Query existing fences
        queryFences();
    }

    /**
     * Query all fences
     */
    private void queryFences() {
        Log.d(TAG, "queryFences");

        // Build a list of fences
        final List<String> list = new ArrayList<>();
        // Create map that will contain the fences that need to be added
        final Map<String, String> map = new HashMap<>();

        // Iterate over all landmarks
        for (String key : Constants.WPI_AREA_LANDMARKS.keySet()) {
            // Add dwell and exiting keys
            list.add(key + "-dwell");
            list.add(key + "-exiting");

            // put key, val, to map
            map.put(key + "-dwell", key);
        }

        // Build a fence query request
        FenceQueryRequest request = FenceQueryRequest.forFences(list);

        Log.d(TAG, "Querying fences");
        // Query fences
        mFenceClient.queryFences(request)
                .addOnCompleteListener(new OnCompleteListener<FenceQueryResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<FenceQueryResponse> fenceQueryResult) {
                        Log.d(TAG, "queryFences onComplete");
                        if (!fenceQueryResult.isSuccessful()) {
                            Log.e(TAG, "Could not query fences");
                            return;
                        }
                        Log.d(TAG, "getting fenceStateMap");
                        FenceStateMap stateMap = fenceQueryResult.getResult().getFenceStateMap();
                        for (String fenceKey : stateMap.getFenceKeys()) {
                            map.remove(fenceKey);
                            FenceState fenceState = stateMap.getFenceState(fenceKey);
                            Log.i(TAG, "Fence " + fenceKey + ": "
                                    + fenceState.getCurrentState()
                                    + ", was="
                                    + fenceState.getPreviousState()
                                    + ", lastUpdateTime="
                                    + fenceState.getLastFenceUpdateTimeMillis());
                        }

                        if (!map.isEmpty()) {
                            // Register remaining fences
                            registerFences(map.values());
                        }
                    }
                });
    }

    /**
     * Register all the fences in the array
     *
     * @param fenceList the fences
     */
    @SuppressWarnings("MissingPermission")
    private void registerFences(Collection<String> fenceList) {
        Log.d(TAG, "registerFences");
        if (!ApplicationUtils.checkPermissions(this)) {
            return;
        }
        FenceUpdateRequest.Builder builder = new FenceUpdateRequest.Builder();
        PendingIntent pendingIntent = getFencePendingIntent();

        for (String key : fenceList) {
            LatLng value = Constants.WPI_AREA_LANDMARKS.get(key);

            // Set the circular region of this fence.
            AwarenessFence inLocationFence = LocationFence.in(
                    value.latitude,
                    value.longitude,
                    Constants.GEOFENCE_RADIUS_IN_METERS,
                    Constants.GEOFENCE_LOITERING_DELAY
            );

            // Set the circular region for exit
            AwarenessFence exitingLocationFence = LocationFence.exiting(
                    value.latitude,
                    value.longitude,
                    Constants.GEOFENCE_RADIUS_IN_METERS
            );

            // Add "dwell" fence
            builder.addFence(
                    key + "-dwell",
                    inLocationFence,
                    pendingIntent
            );

            // Add exiting fence
            builder.addFence(
                    key + "-exiting",
                    exitingLocationFence,
                    pendingIntent
            );
        }

        mFenceClient.updateFences(builder.build())
                .addOnCompleteListener(this);
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getFencePendingIntent() {
        Log.d(TAG, "getFencePendingIntent invoked");
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling addGeofences().
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Runs when the result of calling {@link #registerFences(Collection)} is available.
     *
     * @param task the resulting Task, containing either a result or error.
     */
    @Override
    public void onComplete(@NonNull Task<Void> task) {
        if (task.isSuccessful()) {
            Log.d(TAG, getString(R.string.geofences_added));
            ApplicationUtils.updateFencesAdded(this, System.currentTimeMillis());
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this, task.getException());
            Log.w(TAG, errorMessage);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: registerFences being destroyed!!!");
        super.onDestroy();
        if (BuildConfig.DEBUG) {
            toast("All work complete");
        }
    }

    // Helper for showing tests
    void toast(final CharSequence text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RegisterFencesJobIntentService.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
