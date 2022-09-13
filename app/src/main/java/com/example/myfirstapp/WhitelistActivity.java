package com.example.myfirstapp;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

//
// Based off this example:
// https://stackoverflow.com/questions/40584424/simple-android-recyclerview-example
//
public class WhitelistActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ADD_PERMISSION = 2000;
    private static final int CONTACT_PICKER_RESULT = 2001;

    // Intent constants.
    static final String INTENT_CHOOSE_CONTACT = "INTENT_CHOOSE_CONTACT";

    // Fields.
    private boolean m_returnContactOnChoose = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whitelist);

        // Read the intent.
        Intent intent = getIntent();
        if (intent.hasExtra(INTENT_CHOOSE_CONTACT)) {
            // Intent is to choose a contact to ping.
            m_returnContactOnChoose = true;
        }

        // Set up the list.
        RecyclerView view = findViewById(R.id.recyclerViewMain);
        view.setLayoutManager(new LinearLayoutManager(this));
        view.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        updateListView();

        // Add "back" button at top left.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private List<String> readFromDatabase() {
        // Read numbers from database.
        PingDbHelper database = new PingDbHelper(this);
        Cursor cursor = database.getWhitelistContacts();
        List<String> contacts = new ArrayList<>();
        while(cursor.moveToNext()) {
            String phoneNumber = cursor.getString(
                    cursor.getColumnIndexOrThrow(PingDatabaseContract.WhitelistContactEntry.COLUMN_NAME_PHONE_NUMBER));
            contacts.add(phoneNumber);
        }
        cursor.close();
        return contacts;
    }

    private List<String> generateData() {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            data.add("Example Contact #" + i);
        }
        return data;
    }

    public void onButtonAddClick(View view) {
        // Choose a contact.
        if (checkForPermission(Manifest.permission.READ_CONTACTS)) {
            Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                    ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private boolean checkForPermission(String permission) {
        if (ContextCompat.checkSelfPermission(
                getApplicationContext(), permission) ==
                PackageManager.PERMISSION_GRANTED) {
            return true;
        } else if (shouldShowRequestPermissionRationale()) {
            System.out.println("Ping must be able to read contacts to work.");
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] { permission },
                    REQUEST_CODE_ADD_PERMISSION);
        }
        return false;
    }

    private boolean shouldShowRequestPermissionRationale() {
        return false;
    }

    public void onButtonDeleteClick(View view) {
        RecyclerView listView = findViewById(R.id.recyclerViewMain);
        CustomAdapter adapter = (CustomAdapter) listView.getAdapter();

        assert adapter != null;
        if (adapter.isItemSelected()) {
            String phoneNumber = adapter.getSelectedValue();
            PingDbHelper database = new PingDbHelper(this);
            database.deleteWhitelistContact(phoneNumber);
            updateListView();
        }
    }

    private void updateListView() {
        RecyclerView listView = findViewById(R.id.recyclerViewMain);

        // Read phone numbers.
        List<String> phoneNumbers = readFromDatabase();

        // Look up contact names.
        List<String> names = new ArrayList<>();
        for (String number: phoneNumbers) {
            String name = MainActivity.getContactName(number, this);
            names.add(name);
        }

        // Create adapter.
        CustomAdapter adapter = new CustomAdapter(readFromDatabase());
        adapter.setDisplayValues(names);
        adapter.sortEntries();

        // Set callback if required.
        if (m_returnContactOnChoose) {
            adapter.setSelectionNotifier(this::onContactToReturnSelected);
        }

        // Update list view.
        listView.setAdapter(adapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            //noinspection SwitchStatementWithTooFewBranches
            switch (requestCode) {
                case CONTACT_PICKER_RESULT:
                    // Handle contact results.
                    System.out.println("Contact picked.");
                    onContactPicked(data);
                    break;
                default:
                    System.out.println("Unexpected activity result code: " + requestCode);
            }

        } else {
            // Gracefully handle failure.
            System.out.println("Warning: Activity result not OK.");
        }
    }

    private void onContactPicked(Intent data) {

        Uri uri = data.getData();
        if (uri != null) {
            Cursor cursor = null;
            try {
                Uri contentUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                String id = uri.getLastPathSegment();
                String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?";
                String[] selectionArgs = new String[] {id};
                cursor = getContentResolver().query(contentUri, null, selection, selectionArgs, null);

                if (cursor != null && cursor.moveToFirst()) {
                    String columnName = ContactsContract.CommonDataKinds.Phone.NUMBER;
                    int columnIndex = cursor.getColumnIndex(columnName);
                    String number = cursor.getString(columnIndex);
                    System.out.println("Number is: " + number);
                    addContact(number);
                }
            }
            catch (SQLiteException | SecurityException | IllegalArgumentException e) {
                System.out.println(e.getLocalizedMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    void addContact(String phoneNumber) {
        // Update database.
        PingDbHelper database = new PingDbHelper(this);

        // Avoid adding the same contact twice.
        if (database.whitelistContactExists(phoneNumber)) {
            Toast message = Toast.makeText(this, "Contact is already in the list.", Toast.LENGTH_SHORT);
            message.show();
            return;
        }

        // Add the contact.
        database.addWhitelistContact(phoneNumber);

        // Update list view.
        updateListView();
    }

    public void onButtonClearClick(View view) {
        // Update database.
        PingDbHelper database = new PingDbHelper(this);
        database.deleteAllWhitelistContacts();

        // Update list view.
        updateListView();
    }

    public void onButtonMainScreenClick(View view) {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onRecyclerViewMainClick(View view) {
        Log.i("Ping", "Recycler view clicked.");

        if (m_returnContactOnChoose) {
            RecyclerView listView = findViewById(R.id.recyclerViewMain);
            CustomAdapter adapter = (CustomAdapter) listView.getAdapter();

            assert adapter != null;
            if (adapter.isItemSelected()) {
                String phoneNumber = adapter.getSelectedValue();
                Log.i("Ping", "Number chosen:" + phoneNumber);
            }
        }
    }

    /**
     * Callback for when a contact is selected.
     */
    private void onContactToReturnSelected() {
        RecyclerView listView = findViewById(R.id.recyclerViewMain);
        CustomAdapter adapter = (CustomAdapter) listView.getAdapter();

        assert adapter != null;
        if (adapter.isItemSelected()) {
            // Get the contact details.
            String phoneNumber = adapter.getSelectedValue();

            // Create the intent.
            Intent data = new Intent();

            // Set the data to pass back.
            data.setData(Uri.parse(phoneNumber));
            setResult(RESULT_OK, data);

            // Close the activity.
            finish();
        }
    }
}