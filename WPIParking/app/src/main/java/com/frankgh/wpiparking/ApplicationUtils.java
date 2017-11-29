package com.frankgh.wpiparking;

import android.app.ActivityManager;
import android.content.Context;
import android.support.annotation.NonNull;
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

    static ParkingLot getLotFromDataSnapshot(DataSnapshot singleSnapshot) {
        Log.d(TAG, "getLotFromDataSnapshot");
        ParkingLot parkingLot = singleSnapshot.getValue(ParkingLot.class);
        parkingLot.setName(singleSnapshot.getKey());
        return parkingLot;
    }
}
