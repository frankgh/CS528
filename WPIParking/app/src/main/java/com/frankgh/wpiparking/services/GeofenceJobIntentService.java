package com.frankgh.wpiparking.services;

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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import com.frankgh.wpiparking.ApplicationUtils;
import com.frankgh.wpiparking.Constants;
import com.frankgh.wpiparking.MainActivity;
import com.frankgh.wpiparking.R;
import com.frankgh.wpiparking.models.ParkingLot;
import com.frankgh.wpiparking.receivers.FenceBroadcastReceiver;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.FenceClient;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
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
 *
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 12/8/17.
 */
public class GeofenceJobIntentService extends JobIntentService {

    private static final int JOB_ID = 563;
    private static final String TAG = "GeofenceJIS";
    private static final String CHANNEL_ID = "wpi_parking_01";
    final Handler mHandler = new Handler();
    /**
     * FirebaseAuth for session checking
     */
    private FirebaseAuth mAuth;
    /**
     * Provides access to the Awareness API.
     */
    private FenceClient mFenceClient;
    /**
     * The references to the firebase database.
     */
    private DatabaseReference mDatabase;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, GeofenceJobIntentService.class, JOB_ID, intent);
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

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            return;
        }

        // Get a reference to the firebase database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Get a reference to the awareness fence client
        mFenceClient = Awareness.getFenceClient(this);

        // The state information for the given fence is em
        FenceState fenceState = FenceState.extract(intent);

        if (fenceState.getCurrentState() != FenceState.TRUE) {
            return;
        }

        // Get the name of the fence
        String fenceName = parseFenceName(fenceState.getFenceKey());

        // Get the transition type.
        int geofenceTransition = parseFenceTransition(fenceState.getFenceKey());

        // Get the transition details as a String.
        String geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition, fenceName);

        // Send notification and log the transition details.
        sendNotification(geofenceTransitionDetails);
        Log.i(TAG, geofenceTransitionDetails);

        handleTransition(geofenceTransition, fenceName);
    }

    /**
     * Parse the name of the fence
     *
     * @param fenceKey
     * @return
     */
    private String parseFenceName(String fenceKey) {
        int index = fenceKey.indexOf('-');
        return fenceKey.substring(0, index);
    }

    /**
     * Parse the transition type
     *
     * @param fenceKey
     * @return
     */
    private int parseFenceTransition(String fenceKey) {
        int index = fenceKey.indexOf('-');
        String s = fenceKey.substring(index);
        if ("-entering".equals(s)) {
            return Geofence.GEOFENCE_TRANSITION_ENTER;
        } else if ("-dwell".equals(s)) {
            return Geofence.GEOFENCE_TRANSITION_DWELL;
        } else if ("-exiting".equals(s)) {
            return Geofence.GEOFENCE_TRANSITION_EXIT;
        }
        Log.d(TAG, getString(R.string.unknown_geofence_transition));
        return -1;
    }

    /**
     * Handles the transitions to the geofences
     *
     * @param geofenceTransition the transition type
     * @param fenceName          the name of the triggering fence
     */
    private void handleTransition(int geofenceTransition, String fenceName) {
        switch (geofenceTransition) {
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                readParkingLotData();
                break;

            case Geofence.GEOFENCE_TRANSITION_EXIT:
                // Remove all internal geofences when we leave the container geofence
                if (Constants.LATLNG_WPI.equals(fenceName)) {
                    removeInternalFences();
                }
                break;
        }
    }

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param geofenceTransition The ID of the geofence transition.
     * @param fenceName          The name of the fence.
     * @return The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(
            int geofenceTransition,
            String fenceName) {
        return getTransitionString(geofenceTransition) + ": " + fenceName;
    }

    /**
     * Reads parking lots data and sets up geofences.
     */
    private void readParkingLotData() {
        Log.d(TAG, "readParkingLotData");
        // Get reference to the lots
        DatabaseReference lotReference = mDatabase.child("lots");

        // Read all lots from db
        lotReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final List<ParkingLot> parkingLotList = new ArrayList<>();
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    parkingLotList.add(ApplicationUtils.getLotFromDataSnapshot(singleSnapshot));
                }
                if (parkingLotList.size() > 0) {
                    addFences(parkingLotList);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "onCancelled", databaseError.toException());
            }
        });
    }

    /**
     * Adds and updates fences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    private void addFences(final List<ParkingLot> parkingLotList) {
        Log.d(TAG, "addFences invoked");
        if (!ApplicationUtils.checkPermissions(this)) {
            return;
        }

        // Construct a FenceUpdateRequest
        FenceUpdateRequest.Builder builder = new FenceUpdateRequest.Builder();

        // Get explicit pending intent to handle fence triggers
        PendingIntent mPendingIntent = getFencePendingIntent();

        for (ParkingLot lot : parkingLotList) {
            int version = PreferenceManager.getDefaultSharedPreferences(this).getInt(
                    Constants.GEOFENCES_ADDED_KEY + "." + lot.getName(), -1);
            if (version == -1 || version < lot.getVersion()) {
                // Create primitive fences for during driving
                AwarenessFence duringDrivingFence = DetectedActivityFence.during(
                        DetectedActivityFence.IN_VEHICLE);

                // Create primitive fences for starting driving
                AwarenessFence startingDrivingFence = DetectedActivityFence.starting(
                        DetectedActivityFence.IN_VEHICLE);

                // Create primitive fence for entering location fence
                AwarenessFence enteringLocationFence = LocationFence.entering(
                        lot.getLatitude(),
                        lot.getLongitude(),
                        lot.getRadius());

                // Create primitive fences for exiting location fence
                AwarenessFence exitingLocationFence = LocationFence.exiting(
                        lot.getLatitude(),
                        lot.getLongitude(),
                        lot.getRadius());

                // Create a combination fence to AND primitive fences.
                AwarenessFence drivingIntoParkingLot = AwarenessFence.and(
                        duringDrivingFence, enteringLocationFence);

                // Create a combination fence to AND primitive fences.
                AwarenessFence drivingOutOfParkingLot = AwarenessFence.and(
                        startingDrivingFence, exitingLocationFence);

                // Add fence with lot name as key
                builder.addFence(lot.getName() + "-IN", drivingIntoParkingLot, mPendingIntent);
                builder.addFence(lot.getName() + "-OUT", drivingOutOfParkingLot, mPendingIntent);

                if (version < lot.getVersion()) {
                    // Remove outdated fences from preferences
                    removeGeofencePreference(lot);
                }
            }
        }

        mFenceClient.updateFences(builder.build())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    /**
                     * Runs when the result of calling {@link GeofenceJobIntentService#addFences(List)}} is available.
                     *
                     * @param task the resulting Task, containing either a result or error.
                     */
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            updateGeofencesAdded(parkingLotList);
                            toast(getString(R.string.geofences_added));
                        } else {
                            // Get the status code for the error and log it using a user-friendly message.
                            String errorMessage = GeofenceErrorMessages.getErrorString(getApplicationContext(), task.getException());
                            Log.w(TAG, errorMessage);
                        }
                    }
                });
    }

    /**
     * Removes all the internal fences
     */
    private void removeInternalFences() {
        Log.d(TAG, "removeInternalFences");

        Map<String, ?> map = PreferenceManager.getDefaultSharedPreferences(this).getAll();
        FenceUpdateRequest.Builder builder = new FenceUpdateRequest.Builder();
        final List<String> lotNames = new ArrayList<>();
        for (String key : map.keySet()) {
            if (key.indexOf(Constants.GEOFENCES_ADDED_KEY + ".") == 0) {
                String lotName = key.substring(key.lastIndexOf('.') + 1);
                builder.removeFence(lotName);
                lotNames.add(lotName);
            }
        }

        mFenceClient.updateFences(builder.build()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    toast(getString(R.string.geofences_removed));
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
     * Stores whether geofences were added ore removed in {@link SharedPreferences};
     *
     * @param parkingLotList
     */
    private void updateGeofencesAdded(List<ParkingLot> parkingLotList) {
        if (parkingLotList != null) {
            for (ParkingLot lot : parkingLotList) {
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
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getFencePendingIntent() {
        Log.d(TAG, "getFencePendingIntent invoked");
        Intent intent = new Intent(this, FenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling addFences().
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        toast("All work complete");
    }

    // Helper for showing tests
    void toast(final CharSequence text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GeofenceJobIntentService.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
