package com.frankgh.wpiparking.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.frankgh.wpiparking.db.entity.ActivityEntity;

import java.util.List;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 12/6/17.
 */
@Dao
public interface ActivityDao {
    @Query("SELECT * FROM activity where timestamp >= :timestamp")
    List<ActivityEntity> loadActivitiesNewerThan(int timestamp);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ActivityEntity entity);
}
