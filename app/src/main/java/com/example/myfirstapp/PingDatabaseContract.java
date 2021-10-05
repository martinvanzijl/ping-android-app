package com.example.myfirstapp;

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
            "CREATE TABLE " + WhitelistContactEntry.TABLE_NAME + " (" +
                    WhitelistContactEntry._ID + " INTEGER PRIMARY KEY," +
                    WhitelistContactEntry.COLUMN_NAME_PHONE_NUMBER + " TEXT)";

    static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + WhitelistContactEntry.TABLE_NAME;
}
