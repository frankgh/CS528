package com.wpics528.android.criminalintent.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.wpics528.android.criminalintent.database.CrimeDbSchema.CrimeTable;

public class CrimeBaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "CrimeBaseHelper";
    private static final int VERSION = 3;
    private static final String DATABASE_NAME = "crimeBase.db";

    private static final String DATABASE_ALTER_PHOTO_COUNT_2 = "ALTER TABLE "
            + CrimeTable.NAME + " ADD COLUMN " + CrimeTable.Cols.PHOTO_COUNT + " INTEGER;";

    public CrimeBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE " + CrimeTable.NAME + "(" +
                " _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                CrimeTable.Cols.UUID + ", " +
                CrimeTable.Cols.TITLE + ", " +
                CrimeTable.Cols.DATE + ", " +
                CrimeTable.Cols.SUSPECT + ", " +
                CrimeTable.Cols.SOLVED + ", " +
                CrimeTable.Cols.PHOTO_COUNT +
                ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            Log.d(TAG, "Adding Photo Count to database");
            db.execSQL(DATABASE_ALTER_PHOTO_COUNT_2);
        }
    }
}
