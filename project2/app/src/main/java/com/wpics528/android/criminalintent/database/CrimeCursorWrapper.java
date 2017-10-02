package com.wpics528.android.criminalintent.database;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.wpics528.android.criminalintent.Crime;
import com.wpics528.android.criminalintent.database.CrimeDbSchema.CrimeTable;

import java.util.Date;
import java.util.UUID;

public class CrimeCursorWrapper extends CursorWrapper {
    public CrimeCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Crime getCrime() {
        String uuidString = getString(getColumnIndex(CrimeTable.Cols.UUID));
        String title = getString(getColumnIndex(CrimeTable.Cols.TITLE));
        long date = getLong(getColumnIndex(CrimeTable.Cols.DATE));
        int isSolved = getInt(getColumnIndex(CrimeTable.Cols.SOLVED));
        String suspect = getString(getColumnIndex(CrimeTable.Cols.SUSPECT));
        int photoCount = getInt(getColumnIndex(CrimeTable.Cols.PHOTO_COUNT));
        int isFaceDetectionEnabled = getInt(getColumnIndex(CrimeTable.Cols.FACE_DETECTION));

        Crime crime = new Crime(UUID.fromString(uuidString));
        crime.setTitle(title);
        crime.setDate(new Date(date));
        crime.setSolved(isSolved != 0);
        crime.setSuspect(suspect);
        crime.setPhotoCount(photoCount);
        crime.setFaceDetectionEnabled(isFaceDetectionEnabled != 0);

        return crime;
    }
}
