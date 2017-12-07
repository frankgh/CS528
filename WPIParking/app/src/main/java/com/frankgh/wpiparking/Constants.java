/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.frankgh.wpiparking;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 11/20/17.
 */

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

/**
 * Constants used in this sample.
 */

public final class Constants {

    public static final String LATLNG_WPI = "WPI";
    /**
     * The desired time between activity detections. Larger values result in fewer activity
     * detections while improving battery life. A value of 0 results in activity detections at the
     * fastest possible rate.
     */
    public static final long DETECTION_INTERVAL_IN_MILLISECONDS = 30 * 1000; // 30 seconds
    static final int GEOFENCE_RADIUS_IN_METERS = 1609 / 2; // 2 miles, 1.6 km
    /**
     * Map for storing information about airports in the San Francisco bay area.
     */
    static final HashMap<String, LatLng> WPI_AREA_LANDMARKS = new HashMap<>();
    /**
     * Loitering delay for the geofence
     */
    static final int GEOFENCE_LOITERING_DELAY =
            20 * 1000; // 20 seconds
    /**
     * Re-add geofence after expiration
     */
    static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            7 * 24 * 60 * 60 * 1000; // 7 days in milliseconds
    private static final String PACKAGE_NAME = "com.frankgh.wpiparking.Geofence";
    public static final String KEY_ACTIVITY_UPDATES_REQUESTED = PACKAGE_NAME + ".ACTIVITY_UPDATES_REQUESTED";
    public static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";

    static {
        WPI_AREA_LANDMARKS.put(LATLNG_WPI, new LatLng(42.274641, -71.80634)); // Worcester Polytechnic Institute.
    }

    private Constants() {
    }
}
