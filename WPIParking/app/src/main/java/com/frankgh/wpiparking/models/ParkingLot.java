package com.frankgh.wpiparking.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 11/23/17.
 */

@IgnoreExtraProperties
public class ParkingLot {
    private String name;
    private String address;
    private int capacity;
    private String displayName;
    private double latitude;
    private double longitude;
    private int threshold;
    private int used;
    private int radius;

    public ParkingLot() {
        // Default constructor required for calls to DataSnapshot.getValue(ParkingLot.class)
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("address", address);
        result.put("capacity", capacity);
        result.put("displayName", displayName);
        result.put("latitude", latitude);
        result.put("longitude", longitude);
        result.put("threshold", threshold);
        result.put("used", used);
        result.put("radius", radius);

        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public int getUsed() {
        return used;
    }

    public void setUsed(int used) {
        this.used = used;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    @Exclude
    public boolean isFull() {
        return capacity - used <= threshold;
    }

    @Exclude
    public int getAvailable() {
        return capacity - used - threshold;
    }
}
