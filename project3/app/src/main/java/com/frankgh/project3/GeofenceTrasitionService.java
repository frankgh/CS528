package com.frankgh.project3;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;


public class GeofenceTrasitionService extends IntentService {

    public static final int GEOFENCE_NOTIFICATION_ID = 0;
    public static final String BROADCAST_ACTION = "com.websmithing.broadcasttest.displayevent";
    private static final String TAG = "GeofenceService";
    static int lab_counter = 0;
    static int lib_counter = 0;
    private final Handler handler = new Handler();
    Intent count_intent;
    Intent count_intent1;
    int fullerCount = 0;
    int gordonCount = 0;
    private TextView textLib, textLab;


    public GeofenceTrasitionService() {
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
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "entered onCreate");
        count_intent = new Intent(BROADCAST_ACTION);

    }

    private void DisplayLoggingInfo(String dets) {
        Log.d(TAG, "entered DisplayLoggingInfo");
        String place = "Fuller";

        if (dets.equals("Entering Gordon Lib")) {
            ++lib_counter;
            place = "Gordon";
            count_intent.putExtra("action", "enter");
        } else if (dets.equals("Entering Fuller Labs")) {
            ++lab_counter;
            count_intent.putExtra("action", "enter");
        }
        if (dets.equals("Exiting Gordon Lib")) {
            count_intent.putExtra("action", "exit");

        } else if (dets.equals("Exiting Fuller Labs")) {
            count_intent.putExtra("action", "exit");

        }
        count_intent.putExtra("lib_counter", String.valueOf(lib_counter));
        count_intent.putExtra("lab_counter", String.valueOf(lab_counter));
        count_intent.putExtra("place", place);
        count_intent.putExtra("dets", dets);
        sendBroadcast(count_intent);
    }

    private String getGeofenceTrasitionDetails(int geoFenceTransition, List<Geofence> triggeringGeofences) {
        Log.d(TAG, "entered getGeofenceTrasitionDetailsNew");
        ArrayList<String> triggeringGeofencesList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesList.add(geofence.getRequestId());
        }

        String status = null;
        if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            status = "Entering ";
        } else if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT)
            status = "Exiting ";

        return status + TextUtils.join(", ", triggeringGeofencesList);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "entered onHandleIntent");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMsg = getErrorString(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMsg);
            return;
        }
        int geoFenceTransition = geofencingEvent.getGeofenceTransition();
        // Check if the transition type is of interest
        Log.d(TAG, "geofenceTrans: " + Integer.toString(geoFenceTransition));
        if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geoFenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Get the geofence that were triggered
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            String geofenceTransitionDetails = getGeofenceTrasitionDetails(geoFenceTransition, triggeringGeofences);

            DisplayLoggingInfo(geofenceTransitionDetails);
            sendNotification(geofenceTransitionDetails);
        }
    }

    private void sendNotification(String msg) {
        Log.i(TAG, "sendNotification: " + msg);

        // Intent to start the main Activity

        Intent notificationIntent = MainActivity.makeNotificationIntent(
                getApplicationContext(), msg
        );

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);


        // Creating and sending Notification
        NotificationManager notificatioMng =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificatioMng.notify(
                GEOFENCE_NOTIFICATION_ID,
                createNotification(msg, notificationPendingIntent));

    }

    // Create notification
    private Notification createNotification(String msg, PendingIntent notificationPendingIntent) {
        Log.d(TAG, "entered createNotification");
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder
                .setSmallIcon(R.drawable.ic_action_location)
                .setColor(Color.RED)
                .setContentTitle(msg)
                .setContentText("Geofence Notification!")
                .setContentIntent(notificationPendingIntent)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .setAutoCancel(true);
        return notificationBuilder.build();
    }

}
