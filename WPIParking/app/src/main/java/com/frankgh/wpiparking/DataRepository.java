package com.frankgh.wpiparking;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 12/6/17.
 */

import com.frankgh.wpiparking.db.AppDatabase;
import com.frankgh.wpiparking.db.entity.ActivityEntity;

import java.util.List;

/**
 * Repository handling the work with products and comments.
 */
public class DataRepository {

    private static DataRepository sInstance;

    private final AppDatabase mDatabase;

    private DataRepository(final AppDatabase database) {
        mDatabase = database;
    }

    public static DataRepository getInstance(final AppDatabase database) {
        if (sInstance == null) {
            synchronized (DataRepository.class) {
                if (sInstance == null) {
                    sInstance = new DataRepository(database);
                }
            }
        }
        return sInstance;
    }

    public void insert(ActivityEntity entity) {
        mDatabase.activityDao().insert(entity);
    }

    public List<ActivityEntity> loadActivities(final int timestamp) {
        return mDatabase.activityDao().loadActivitiesNewerThan(timestamp);
    }
}
