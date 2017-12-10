package com.frankgh.wpiparking;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.frankgh.wpiparking.models.ParkingLot;
import com.google.firebase.database.DataSnapshot;

import java.util.List;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 11/21/17.
 */

public class ApplicationUtils {

    private static final String TAG = "ApplicationUtils";

    static boolean isServiceRunning(@NonNull Context ctx, String serviceClassName) {
        final ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(serviceClassName)) {
                return true;
            }
        }
        return false;
    }

    public static ParkingLot getLotFromDataSnapshot(DataSnapshot singleSnapshot) {
        Log.d(TAG, "getLotFromDataSnapshot");
        ParkingLot parkingLot = singleSnapshot.getValue(ParkingLot.class);
        parkingLot.setName(singleSnapshot.getKey());
        return parkingLot;
    }

    /**
     * Return the current state of the permissions needed.
     */
    public static boolean checkPermissions(@NonNull Context ctx) {
        Log.d(TAG, "checkPermission");
        int permissionState = ActivityCompat.checkSelfPermission(ctx,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Stores whether geofences were added ore removed in {@link SharedPreferences};
     *
     * @param added Whether geofences were added or removed.
     */
    public static void updateFencesAdded(@NonNull Context ctx, long added) {
        PreferenceManager.getDefaultSharedPreferences(ctx)
                .edit()
                .putLong(Constants.FENCES_ADDED_KEY, added)
                .apply();
    }
}
