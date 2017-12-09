package com.frankgh.wpiparking.services;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import com.frankgh.wpiparking.models.ParkingEvent;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 12/7/17.
 */
public class FenceJobIntentService extends JobIntentService {

    /**
     * TAG for the Fence
     */
    private static final String TAG = "FenceIS";
    /**
     * The JOB ID for the Job Intent Service
     */
    private static final int JOB_ID = 827;
    /**
     * FirebaseAuth for session checking
     */
    private FirebaseAuth mAuth;
    /**
     * The references to the firebase database.
     */
    private DatabaseReference mDatabase;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, FenceJobIntentService.class, JOB_ID, intent);
    }

    /**
     * Handles incoming intents.
     *
     * @param intent sent by Fence API.
     */
    @Override
    public void onHandleWork(Intent intent) {
        Log.d(TAG, "onHandleIntent invoked");

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Log.d(TAG, "User not authenticated");
            return;
        }

        // Get a reference to the firebase database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // The state information for the given fence is em
        FenceState fenceState = FenceState.extract(intent);

        if (fenceState.getCurrentState() != FenceState.TRUE) {
            Log.e(TAG, fenceState.toString());
            return;
        }

        // The lot name is part of the key to the fence
        String lotName = parseLotName(fenceState.getFenceKey());

        int parkingEvent = parseParkingEvent(fenceState.getFenceKey());

        // Write a parking event to db to update
        writeNewParkingEvent(
                mAuth.getCurrentUser().getUid(), // user id
                lotName, // lot name
                fenceState.getLastFenceUpdateTimeMillis(), //System.currentTimeMillis(),
                parkingEvent
        );
    }

    /**
     * Parse the lot name from the fence key
     *
     * @param fenceKey the fence key
     * @return the lot name
     */
    private String parseLotName(String fenceKey) {
        int index = fenceKey.indexOf('-');
        return fenceKey.substring(0, index);
    }

    /**
     * Parse the parking event from the fence key
     *
     * @param fenceKey the fence key
     * @return the parking event
     */
    private int parseParkingEvent(String fenceKey) {
        int index = fenceKey.indexOf('-');
        return "-IN".equals(fenceKey.substring(index)) ?
                ParkingEvent.PARKING_EVENT_TYPE_ENTER :
                ParkingEvent.PARKING_EVENT_TYPE_EXIT;
    }

    /**
     * Write a new parking event to the database
     *
     * @param userId    uid
     * @param lotName   lot name
     * @param timestamp timestamp
     * @param type      parking event type
     */
    private void writeNewParkingEvent(String userId, String lotName, long timestamp, int type) {
        String key = mDatabase.child("parking-events").push().getKey();
        ParkingEvent event = new ParkingEvent(lotName, timestamp, type);
        Map<String, Object> postValues = event.toMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/parking-events/" + userId + "/" + key, postValues);
        mDatabase.updateChildren(childUpdates);
    }
}
