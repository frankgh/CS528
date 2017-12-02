package com.frankgh.wpiparking.services;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 11/20/17.
 */

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.frankgh.wpiparking.ApplicationUtils;
import com.frankgh.wpiparking.Constants;
import com.frankgh.wpiparking.MainActivity;
import com.frankgh.wpiparking.R;
import com.frankgh.wpiparking.models.ParkingLot;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Listener for geofence transition changes.
 * <p>
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition. Creates a notification
 * as the output.
 */
public class GeofenceTransitionsIntentService extends IntentService implements
        OnCompleteListener<Void> {

    private static final String TAG = "GeofenceTransitionsIS";
    private static final String CHANNEL_ID = "wpi_parking_01";

    /**
     * Provides access to the Geofencing API.
     */
    private GeofencingClient mGeofencingClient;
    /**
     * The references to the firebase database.
     */
    private DatabaseReference mDatabaseReference;
    /**
     * The list of geofences for the parking lots.
     */
    private List<Geofence> mGeofenceList;
    private List<ParkingLot> mLotsForGeofenceList;
    private List<String> mOutdatedGeofenceList;

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public GeofenceTransitionsIntentService() {
        // Use the TAG to name the worker thread.
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
        Log.d(TAG, "onHandleIntent invoked");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        database.setPersistenceEnabled(true);
        mDatabaseReference = database.getReference();

        mGeofencingClient = LocationServices.getGeofencingClient(this);

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            setupInternalGeofences();
        }

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                // Remove all internal geofences when we leave the container geofence
                for (Geofence geofence : triggeringGeofences) {
                    if (Constants.LATLNG_WPI.equals(geofence.getRequestId())) {
                        removeInternalGeofences();
                    }
                }
            }

            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition,
                    triggeringGeofences);

            // Send notification and log the transition details.
            sendNotification(geofenceTransitionDetails);
            Log.i(TAG, geofenceTransitionDetails);
        }
    }

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param geofenceTransition  The ID of the geofence transition.
     * @param triggeringGeofences The geofence(s) triggered.
     * @return The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {
        Log.d(TAG, "getGeofenceTransitionDetails invoked");
        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }

    /**
     * Removes all the internal geofences
     */
    private void removeInternalGeofences() {
        Log.d(TAG, "removeInternalGeofences");

        Map<String, ?> map = PreferenceManager.getDefaultSharedPreferences(this)
                .getAll();
        final List<String> lotNames = new ArrayList<>();
        for (String key : map.keySet()) {
            if (key.indexOf(Constants.GEOFENCES_ADDED_KEY + ".") == 0) {
                lotNames.add(key.substring(key.lastIndexOf('.') + 1));
            }
        }
        mGeofencingClient.removeGeofences(lotNames).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), R.string.geofences_removed, Toast.LENGTH_SHORT).show();
                    for (String lot : lotNames) {
                        removeGeofencePreference(lot);
                    }
                } else {
                    // Get the status code for the error and log it using a user-friendly message.
                    String errorMessage = GeofenceErrorMessages.getErrorString(getApplicationContext(), task.getException());
                    Log.w(TAG, errorMessage);
                }

            }
        });
    }

    /**
     * Reads parking lots data and sets up geofences.
     */
    private void setupInternalGeofences() {
        Log.d(TAG, "setupInternalGeofences");
        // Initialize Database
        DatabaseReference lotReference = mDatabaseReference.child("lots");
        lotReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final List<ParkingLot> parkingLotList = new ArrayList<>();
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    parkingLotList.add(ApplicationUtils.getLotFromDataSnapshot(singleSnapshot));
                }
                populateGeofenceList(parkingLotList);
                removeOutdatedGeofences();
                addGeofences();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "onCancelled", databaseError.toException());
            }
        });
    }

    private void populateGeofenceList(final List<ParkingLot> parkingLotList) {
        Log.d(TAG, "populateGeofenceList invoked");
        List<Geofence> geofenceList = new ArrayList<>();
        List<ParkingLot> lotsForGeofenceList = new ArrayList<>();
        List<String> outdatedGeofenceList = new ArrayList<>();
        for (ParkingLot lot : parkingLotList) {
            int version = PreferenceManager.getDefaultSharedPreferences(this).getInt(
                    Constants.GEOFENCES_ADDED_KEY + "." + lot.getName(), -1);
            if (version == -1) {
                lotsForGeofenceList.add(lot);
                geofenceList.add(new Geofence.Builder()
                        // Set the request ID of the geofence. This is a string to identify this
                        // geofence.
                        .setRequestId(lot.getName())

                        // Set the circular region of this geofence.
                        .setCircularRegion(
                                lot.getLatitude(),
                                lot.getLongitude(),
                                lot.getRadius()
                        )

                        // Set the expiration duration of the geofence. This geofence gets automatically
                        // removed after this period of time.
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)

                        // Set the transition types of interest. Alerts are only generated for these
                        // transition. We track entry and exit transitions in this sample.
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                Geofence.GEOFENCE_TRANSITION_EXIT)

                        // Create the geofence.
                        .build());
            } else if (version < lot.getVersion()) {
                outdatedGeofenceList.add(lot.getName());
                removeGeofencePreference(lot);
            }
        }
        mGeofenceList = geofenceList;
        mLotsForGeofenceList = lotsForGeofenceList;
        mOutdatedGeofenceList = outdatedGeofenceList;
    }

    /**
     * Removes geofences that have been updated in the database. This method should be called
     * after the user has granted the location permission.
     */
    private void removeOutdatedGeofences() {
        if (!ApplicationUtils.checkPermissions(this)) {
            return;
        }

        if (mOutdatedGeofenceList != null && mOutdatedGeofenceList.size() > 0) {
            mGeofencingClient.removeGeofences(mOutdatedGeofenceList);
        }
    }

    /**
     * Adds geofences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    private void addGeofences() {
        if (!ApplicationUtils.checkPermissions(this)) {
            return;
        }

        if (mGeofenceList != null && mGeofenceList.size() > 0) {
            mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                    .addOnCompleteListener(this);
        }
    }

    /**
     * Runs when the result of calling {@link #addGeofences()} is available.
     *
     * @param task the resulting Task, containing either a result or error.
     */
    @Override
    public void onComplete(@NonNull Task<Void> task) {
        if (task.isSuccessful()) {
            updateGeofencesAdded();
            Toast.makeText(this, R.string.geofences_added, Toast.LENGTH_SHORT).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this, task.getException());
            Log.w(TAG, errorMessage);
        }
    }

    /**
     * Stores whether geofences were added ore removed in {@link SharedPreferences};
     */
    private void updateGeofencesAdded() {
        if (mLotsForGeofenceList != null) {
            for (ParkingLot lot : mLotsForGeofenceList) {
                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putInt(Constants.GEOFENCES_ADDED_KEY + "." + lot.getName(), lot.getVersion())
                        .apply();
            }
        }
    }

    /**
     * Remove the preference for a given parking lot.
     */
    private void removeGeofencePreference(ParkingLot lot) {
        removeGeofencePreference(lot.getName());
    }

    /**
     * Remove the preference for a given parking lot name.
     */
    private void removeGeofencePreference(String name) {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .remove(Constants.GEOFENCES_ADDED_KEY + "." + name)
                .apply();
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        Log.d(TAG, "getGeofencePendingIntent invoked");
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling addGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MainActivity.
     */
    private void sendNotification(String notificationDetails) {
        Log.d(TAG, "sendNotification invoked");
        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        Notification.Builder builder = new Notification.Builder(this);

        // Define the notification settings.
        builder.setSmallIcon(R.drawable.ic_launcher_background)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_launcher_background))
                .setColor(Color.RED)
                .setContentTitle(notificationDetails)
                .setContentText(getString(R.string.geofence_transition_notification_text))
                .setContentIntent(notificationPendingIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(mChannel);

            builder.setChannelId(CHANNEL_ID); // Channel ID
        }

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType A transition type constant defined in Geofence
     * @return A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                return getString(R.string.geofence_transition_dwell);
            default:
                return getString(R.string.unknown_geofence_transition);
        }
    }

//    /**
//     * This sample hard codes geofence data. A real app might dynamically create geofences based on
//     * the user's location.
//     */
//    private void populateGeofenceList() {
//        if (mAdapter == null)
//            return;
//        List<ParkingLot> parkingLotList = mAdapter.getParkingLots();
//        if (parkingLotList == null || parkingLotList.size() == 0)
//            return;
//
//        List<Geofence> geofenceList = new ArrayList<>();
//        List<ParkingLot> lotsForGeofenceList = new ArrayList<>();
//        List<String> outdatedGeofenceList = new ArrayList<>();
//        for (ParkingLot lot : parkingLotList) {
//            int version = PreferenceManager.getDefaultSharedPreferences(this).getInt(
//                    Constants.GEOFENCES_ADDED_KEY + "." + lot.getName(), -1);
//            if (version == -1) {
//                lotsForGeofenceList.add(lot);
//                geofenceList.add(new Geofence.Builder()
//                        // Set the request ID of the geofence. This is a string to identify this
//                        // geofence.
//                        .setRequestId(lot.getName())
//
//                        // Set the circular region of this geofence.
//                        .setCircularRegion(
//                                lot.getLatitude(),
//                                lot.getLongitude(),
//                                lot.getRadius()
//                        )
//
//                        // Set the expiration duration of the geofence. This geofence gets automatically
//                        // removed after this period of time.
//                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
//
//                        // Set the transition types of interest. Alerts are only generated for these
//                        // transition. We track entry and exit transitions in this sample.
//                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
//                                Geofence.GEOFENCE_TRANSITION_EXIT)
//
//                        // Create the geofence.
//                        .build());
//            } else if (version < lot.getVersion()) {
//                outdatedGeofenceList.add(lot.getName());
//                removeGeofencePreference(lot);
//            }
//        }
//        mGeofenceList = geofenceList;
//        mLotsForGeofenceList = lotsForGeofenceList;
//        mOutdatedGeofenceList = outdatedGeofenceList;
//    }
//
//    /**
//     * Removes geofences that have been updated in the database. This method should be called
//     * after the user has granted the location permission.
//     */
//    private void removeOutdatedGeofences() {
//        if (!checkPermissions()) {
//            showSnackbar(getString(R.string.insufficient_permissions));
//            return;
//        }
//
//        if (mOutdatedGeofenceList != null && mOutdatedGeofenceList.size() > 0) {
//            mGeofencingClient.removeGeofences(mOutdatedGeofenceList);
//        }
//    }
}