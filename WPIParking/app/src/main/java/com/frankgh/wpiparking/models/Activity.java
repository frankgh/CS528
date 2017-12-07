package com.frankgh.wpiparking.models;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 12/6/17.
 */

public interface Activity {
    int getId();

    int getDetectedActivityId();

    String getDetectedActivityName();

    long getTimestamp();
}
