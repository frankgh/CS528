package com.frankgh.wpiparking;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 11/21/17.
 */

public class PermissionUtils {

    private static final String TAG = "PermissionUtils";

    /**
     * Return the current state of the permissions needed.
     */
    static boolean checkPermissions(@NonNull Context context) {
        Log.d(TAG, "checkPermission()");
        int permissionState = ActivityCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }
}
