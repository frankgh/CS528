package com.frankgh.wpiparking.db.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

import com.frankgh.wpiparking.models.Activity;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 12/6/17.
 */
@Entity(tableName = "activity", indices = {@Index("timestamp")})
public class ActivityEntity implements Activity {

    @PrimaryKey
    private int id;

    @ColumnInfo(name = "detected_activity_id")
    private int detectedActivityId;

    @ColumnInfo(name = "detected_activity_name")
    private String detectedActivityName;

    private long timestamp;

    public ActivityEntity() {
    }

    public ActivityEntity(int id, int detectedActivityId, String name,  long timestamp) {
        this.id = id;
        this.detectedActivityId = detectedActivityId;
        this.detectedActivityName = name;
        this.timestamp = timestamp;
    }

    public ActivityEntity(Activity activity) {
        this.id = activity.getId();
        this.detectedActivityId = activity.getDetectedActivityId();
        this.detectedActivityName = activity.getDetectedActivityName();
        this.timestamp = activity.getTimestamp();
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getDetectedActivityId() {
        return detectedActivityId;
    }

    public void setDetectedActivityId(int detectedActivityId) {
        this.detectedActivityId = detectedActivityId;
    }

    @Override
    public String getDetectedActivityName() {
        return detectedActivityName;
    }

    public void setDetectedActivityName(String detectedActivityName) {
        this.detectedActivityName = detectedActivityName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
