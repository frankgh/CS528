package com.frankgh.wpiparking.models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 12/2/17.
 */

public class ParkingEvent {

    public static final int PARKING_EVENT_TYPE_ENTER = 0;
    public static final int PARKING_EVENT_TYPE_EXIT = 1;

    private String lotName;
    private long timestamp;
    private int type;

    public ParkingEvent() {
        // Default constructor required for calls to DataSnapshot.getValue(ParkingEvent.class)
    }

    public ParkingEvent(String lotName, long timestamp, int type) {
        this.lotName = lotName;
        this.timestamp = timestamp;
        this.type = type;
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("lotName", lotName);
        result.put("timestamp", timestamp);
        result.put("type", type);
        return result;
    }

    public String getLotName() {
        return lotName;
    }

    public void setLotName(String lotName) {
        this.lotName = lotName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
