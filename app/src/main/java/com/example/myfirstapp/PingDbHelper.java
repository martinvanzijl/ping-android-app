package com.example.myfirstapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class PingDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Ping.db";

    public PingDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PingDatabaseContract.SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(PingDatabaseContract.SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void addWhitelistContact(String phoneNumber) {
        // Avoid adding the same contact twice.
        if (whitelistContactExists(phoneNumber)) {
            Log.w("Database", "Contact already in whitelist, so not added again.");
            return;
        }

        // Gets the data repository in write mode
        SQLiteDatabase db = getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(PingDatabaseContract.WhitelistContactEntry.COLUMN_NAME_PHONE_NUMBER, phoneNumber);

        // Insert the new row
        long newRowId = db.insert(PingDatabaseContract.WhitelistContactEntry.TABLE_NAME, null, values);
//        return newRowId;
    }

    public Cursor getWhitelistContacts() {
        SQLiteDatabase db = getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                PingDatabaseContract.WhitelistContactEntry.COLUMN_NAME_PHONE_NUMBER
        };

        Cursor cursor = db.query(
                PingDatabaseContract.WhitelistContactEntry.TABLE_NAME, // The table to query
                projection, // The array of columns to return (pass null to get all)
                null,
                null,
                null,
                null,
                null
        );

        return cursor;
    }

    public void deleteWhitelistContact(String phoneNumber) {
        // Gets the data repository in write mode
        SQLiteDatabase db = getWritableDatabase();

        // Delete the row
        String whereClause = PingDatabaseContract.WhitelistContactEntry.COLUMN_NAME_PHONE_NUMBER + "=?";
        String[] args = new String[] {phoneNumber};
        db.delete(PingDatabaseContract.WhitelistContactEntry.TABLE_NAME, whereClause, args);
    }

    public void deleteAllWhitelistContacts() {
        // Gets the data repository in write mode
        SQLiteDatabase db = getWritableDatabase();

        // Delete all rows
        db.delete(PingDatabaseContract.WhitelistContactEntry.TABLE_NAME, null, null);
    }

    public boolean whitelistContactExists(String phoneNumberToCheck) {
        Cursor cursor = getWhitelistContacts();
        List<String> contacts = new ArrayList<>();
        while(cursor.moveToNext()) {
            String phoneNumber = cursor.getString(
                    cursor.getColumnIndexOrThrow(PingDatabaseContract.WhitelistContactEntry.COLUMN_NAME_PHONE_NUMBER));
            contacts.add(phoneNumber);
        }
        cursor.close();

        for (String phoneNumber: contacts) {
            if (PhoneNumberUtils.compare(phoneNumber, phoneNumberToCheck)) {
                return true;
            }
        }

        return false;
    }

//    public boolean whitelistContactExists(String phoneNumber) {
//        SQLiteDatabase db = getReadableDatabase();
//
//        String whereClause = PingDatabaseContract.WhitelistContactEntry.COLUMN_NAME_PHONE_NUMBER + "=?";
//        String[] args = new String[] {phoneNumber};
//
//        Cursor cursor = db.query(
//                PingDatabaseContract.WhitelistContactEntry.TABLE_NAME,
//                null,
//                whereClause,
//                args,
//                null,
//                null,
//                null
//        );
//
//        int rows = cursor.getCount();
//        cursor.close();
//
//        return (rows > 0);
//    }
}