package com.martinvz.ping;

import android.provider.BaseColumns;

public final class PingDatabaseContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private PingDatabaseContract() {}

    /* Inner class that defines the table contents */
    public static class WhitelistContactEntry implements BaseColumns {
        public static final String TABLE_NAME = "whitelist_contacts";
        public static final String COLUMN_NAME_PHONE_NUMBER = "phone_number";
    }

    static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS " + WhitelistContactEntry.TABLE_NAME + " (" +
                    WhitelistContactEntry._ID + " INTEGER PRIMARY KEY," +
                    WhitelistContactEntry.COLUMN_NAME_PHONE_NUMBER + " TEXT)";

    @SuppressWarnings("unused")
    static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + WhitelistContactEntry.TABLE_NAME;

    /* Inner class that defines the table contents */
    public static class LocationHistoryEntry implements BaseColumns {
        public static final String TABLE_NAME = "location_history";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_NAME_PHONE_NUMBER = "phone_number";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
        public static final String COLUMN_NAME_ADDRESS = "address";
        public static final String COLUMN_NAME_CONTACT_NAME = "contact_name";
    }

    static final String SQL_CREATE_LOCATION_HISTORY =
            "CREATE TABLE IF NOT EXISTS " + LocationHistoryEntry.TABLE_NAME + " (" +
                    LocationHistoryEntry._ID + " INTEGER PRIMARY KEY," +
                    LocationHistoryEntry.COLUMN_NAME_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    LocationHistoryEntry.COLUMN_NAME_PHONE_NUMBER + " TEXT," +
                    LocationHistoryEntry.COLUMN_NAME_LATITUDE + " REAL," +
                    LocationHistoryEntry.COLUMN_NAME_LONGITUDE + " REAL," +
                    LocationHistoryEntry.COLUMN_NAME_ADDRESS + " TEXT," +
                    LocationHistoryEntry.COLUMN_NAME_CONTACT_NAME + " TEXT)";

    static final String SQL_DELETE_LOCATION_HISTORY =
            "DROP TABLE IF EXISTS " + LocationHistoryEntry.TABLE_NAME;
}
