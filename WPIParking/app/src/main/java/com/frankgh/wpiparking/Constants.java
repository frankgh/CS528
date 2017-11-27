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

final class Constants {

    public static final String LATLNG_WPI = "WPI";

    static final float GEOFENCE_RADIUS_IN_METERS = 1609; // 1 mile, 1.6 km
    /**
     * Map for storing information about airports in the San Francisco bay area.
     */
    static final HashMap<String, LatLng> WPI_AREA_LANDMARKS = new HashMap<>();
    private static final String PACKAGE_NAME = "com.google.android.gms.location.Geofence";
    static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";
    /**
     * Used to set an expiration time for a geofence. After this amount of time Location Services
     * stops tracking the geofence.
     */
    private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;
    /**
     * For this sample, geofences expire after twelve hours.
     */
    static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;

    static {
        // Worcester Polytechnic Institute.
        WPI_AREA_LANDMARKS.put(LATLNG_WPI, new LatLng(42.274641, -71.80634));
    }

    private Constants() {
    }
}
