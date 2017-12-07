package com.frankgh.wpiparking;

import android.app.Application;

import com.frankgh.wpiparking.db.AppDatabase;
import com.google.firebase.database.FirebaseDatabase;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 11/21/17.
 */

public class WPIParkingApplication extends Application {

    private static final String TAG = "WPIParkingApplication";

    private AppExecutors mAppExecutors;


    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        mAppExecutors = new AppExecutors();
    }

    public AppDatabase getDatabase() {
        return AppDatabase.getInstance(this, mAppExecutors);
    }

    public DataRepository getRepository() {
        return DataRepository.getInstance(getDatabase());
    }
}
