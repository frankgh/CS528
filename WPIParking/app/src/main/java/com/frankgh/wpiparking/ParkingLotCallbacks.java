package com.frankgh.wpiparking;

import android.support.annotation.NonNull;

import com.frankgh.wpiparking.models.ParkingLot;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 11/27/17.
 */
public interface ParkingLotCallbacks {
    void onParkingLotAdded(@NonNull ParkingLot lot);
}
