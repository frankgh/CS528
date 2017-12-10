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
     * The size of the outer geofence in meters.
     */
    public static final int GEOFENCE_RADIUS_IN_METERS = 1609 / 2; //1609 / 2; // 2 miles, 1.6 km
    /**
     * Map for storing information about airports in the San Francisco bay area.
     */
    public static final HashMap<String, LatLng> WPI_AREA_LANDMARKS = new HashMap<>();
    /**
     * Loitering delay for the geofence
     */
    public static final int GEOFENCE_LOITERING_DELAY = 20 * 1000; // 20 seconds
    /**
     * Re-add geofence after expiration
     */
    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = 7 * 24 * 60 * 60 * 1000; // 7 days in milliseconds
    /**
     * The name of the package
     */
    private static final String PACKAGE_NAME = "com.frankgh.wpiparking.Geofence";
    /**
     * The key to store that the Geofence has been added
     */
    public static final String FENCES_ADDED_KEY = PACKAGE_NAME + ".FENCES_ADDED_KEY";
    /**
     * The intent action which will be fired when your fence is triggered.
     */
    public static final String FENCE_RECEIVER_ACTION = PACKAGE_NAME + ".FENCE_RECEIVER_ACTION";

    static {
        // Worcester Polytechnic Institute.
        WPI_AREA_LANDMARKS.put(LATLNG_WPI, new LatLng(42.274641, -71.80634));
    }

    private Constants() {
    }
}
