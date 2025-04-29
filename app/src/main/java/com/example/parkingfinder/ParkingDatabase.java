package com.example.parkingfinder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ParkingDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "parking.db";
    private static final int DATABASE_VERSION = 1;

    // Table name
    private static final String TABLE_PARKING = "parking_locations";

    // Column names
    private static final String KEY_ID = "id";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_NAME = "name";

    public ParkingDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_PARKING_TABLE = "CREATE TABLE " + TABLE_PARKING + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_LATITUDE + " REAL,"
                + KEY_LONGITUDE + " REAL,"
                + KEY_TIMESTAMP + " INTEGER,"
                + KEY_NAME + " TEXT"
                + ")";
        db.execSQL(CREATE_PARKING_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if it exists
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PARKING);
        // Create tables again
        onCreate(db);
    }

    // Insert a new parking location
    public long insertParkingLocation(ParkingLocation parkingLocation) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_LATITUDE, parkingLocation.getLatitude());
        values.put(KEY_LONGITUDE, parkingLocation.getLongitude());
        values.put(KEY_TIMESTAMP, parkingLocation.getTimestamp());
        values.put(KEY_NAME, parkingLocation.getName());

        // Insert row and get the inserted ID
        long id = db.insert(TABLE_PARKING, null, values);
        db.close();
        return id;
    }

    // Get a single parking location by ID
    public ParkingLocation getParkingLocationById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_PARKING,
                new String[]{KEY_ID, KEY_LATITUDE, KEY_LONGITUDE, KEY_TIMESTAMP, KEY_NAME},
                KEY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            ParkingLocation location = new ParkingLocation(
                    cursor.getLong(0),
                    cursor.getDouble(1),
                    cursor.getDouble(2),
                    cursor.getLong(3),
                    cursor.getString(4)
            );
            cursor.close();
            return location;
        }

        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    // Get the most recently added parking location
    public ParkingLocation getLastParkingLocation() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_PARKING,
                new String[]{KEY_ID, KEY_LATITUDE, KEY_LONGITUDE, KEY_TIMESTAMP, KEY_NAME},
                null, null, null, null, KEY_TIMESTAMP + " DESC", "1");

        if (cursor != null && cursor.moveToFirst()) {
            ParkingLocation location = new ParkingLocation(
                    cursor.getLong(0),
                    cursor.getDouble(1),
                    cursor.getDouble(2),
                    cursor.getLong(3),
                    cursor.getString(4)
            );
            cursor.close();
            return location;
        }

        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    // Get all parking locations
    public List<ParkingLocation> getAllParkingLocations() {
        List<ParkingLocation> locationsList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_PARKING + " ORDER BY " + KEY_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                ParkingLocation location = new ParkingLocation(
                        cursor.getLong(0),
                        cursor.getDouble(1),
                        cursor.getDouble(2),
                        cursor.getLong(3),
                        cursor.getString(4)
                );
                locationsList.add(location);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return locationsList;
    }

    // Delete a parking location
    public boolean deleteParkingLocation(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_PARKING, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rowsDeleted > 0;
    }

    // Update a parking location
    public int updateParkingLocation(ParkingLocation parkingLocation) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_LATITUDE, parkingLocation.getLatitude());
        values.put(KEY_LONGITUDE, parkingLocation.getLongitude());
        values.put(KEY_TIMESTAMP, parkingLocation.getTimestamp());
        values.put(KEY_NAME, parkingLocation.getName());

        // Update the row
        return db.update(TABLE_PARKING, values, KEY_ID + " = ?",
                new String[]{String.valueOf(parkingLocation.getId())});
    }
}