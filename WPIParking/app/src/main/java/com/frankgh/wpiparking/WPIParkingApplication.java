package com.frankgh.wpiparking;

import android.app.Application;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.google.firebase.database.FirebaseDatabase;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 11/21/17.
 */

public class WPIParkingApplication extends Application {

    private static final String TAG = "WPIParkingApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Staring WPI Parking Application");
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        FacebookSdk.sdkInitialize(this);
    }
}
