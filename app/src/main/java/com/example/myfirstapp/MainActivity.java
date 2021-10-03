package com.example.myfirstapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.provider.ContactsContract.Contacts;

public class MainActivity extends AppCompatActivity {

    static final String CHANNEL_ID = "PING_CHANNEL";
    static final String PING_REQUEST_TEXT = "Sent from Ping App. Where are you?";
    private static final int REQUEST_CODE = 1000;
    private ResponseReceiver receiver;

    // Receives messages from the service.
    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateStatusLabel();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();

        IntentFilter filter = new IntentFilter(TextService.BROADCAST_ACTION);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new ResponseReceiver();
        registerReceiver(receiver, filter);

        updateStatusLabel();
    }
    
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void onButtonStartServiceClick(View view) {
        if (checkForPermission(Manifest.permission.READ_SMS) &&
                checkForPermission(Manifest.permission.RECEIVE_SMS) &&
                checkForPermission(Manifest.permission.SEND_SMS)) {
            startService(new Intent(this, TextService.class));
        }
    }

    public void onButtonStopServiceClick(View view) {
        Intent intent = new Intent(this, TextService.class);
        intent.setAction(TextService.STOP);
        startService(intent);
    }

    // Callback for after the user selects whether to give a required permission.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted. Continue the action or workflow
                // in your app.
                System.out.println("Permission was granted.");
            } else {
                // Explain to the user that the feature is unavailable because
                // the features requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
                System.out.println("Without this permission, Ping does not work.");
            }
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }

    private boolean checkForPermission(String permission) {
        if (ContextCompat.checkSelfPermission(
                getApplicationContext(), permission) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            return true;
        } else if (shouldShowRequestPermissionRationale()) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            //showInContextUI(...);
            System.out.println("Ping must be able to read and send text messages to work.");
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            // You can directly ask for the permission.
            ActivityCompat.requestPermissions(this,
                    new String[] { permission },
                    REQUEST_CODE);
        }
        return false;
    }

    // Check if permission rationale should be shown.
    private boolean shouldShowRequestPermissionRationale() {
        return false;
    }

    // Update the service status label.
    private void updateStatusLabel() {
        TextView view = findViewById(R.id.textViewServiceStatus);
        if (TextService.isRunning()) {
            view.setText(R.string.service_is_running_message);
        }
        else {
            view.setText(R.string.service_is_stopped_message);
        }
    }

    // Choose a phone to ping.
    public void onPingButtonClick(View view) {
        // Choose a contact.
        if (checkForPermission(Manifest.permission.READ_CONTACTS)) {
            Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                    Contacts.CONTENT_URI);
            startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
        }
    }

    private static final int CONTACT_PICKER_RESULT = 1001;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CONTACT_PICKER_RESULT:
                    // Handle contact results.
                    System.out.println("Contact picked.");
                    pingPhone(data);
                    break;
                default:
                    System.out.println("Unexpected activity result code: " + requestCode);
            }

        } else {
            // Gracefully handle failure.
            System.out.println("Warning: Activity result not OK.");
        }
    }

    // Ping the phone from the given data.
    private void pingPhone(Intent data) {
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
                    sendPingRequest(number);
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

    // Send a Ping request to the given number.
    private void sendPingRequest(String number) {
        try {
            SmsManager manager = SmsManager.getDefault();
            manager.sendTextMessage(number, null, PING_REQUEST_TEXT, null, null);
            String labelMessage = getString(R.string.ping_request_sent_message, number);
            TextView view = findViewById(R.id.textViewPingStatus);
            view.setText(labelMessage);
        }
        catch (SecurityException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }
}