package com.example.myfirstapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String PING_REPLY_START = "Ping reply.";
    static final String CHANNEL_ID = "PING_CHANNEL";
    static final String PING_REQUEST_TEXT = "Sent from Ping App. Where are you?";
    private static final int REQUEST_CODE_PICK_CONTACT = 1000;
    private static final int REQUEST_CODE_START_SERVICE = 1001;
    private ResponseReceiver receiver;
    private GoogleMap mMap;
//    private Marker mMarker;
    private final Map<String, Marker> mMarkers = new HashMap<>();
    private boolean m_dialogIsRunning = false;

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMarker = mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    // Receives messages from the service.
    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
//            System.out.println("Received broadcast.");
            if (intent.getAction() == TextService.BROADCAST_ACTION) {
                updateStatusLabel();
                notifyIfServiceStopped();
            }
            else if(intent.getAction() == TextService.ASK_WHETHER_TO_ALLOW) {
                String phoneNumber = intent.getStringExtra(Intent.EXTRA_TEXT);
                askWhetherToAllow(phoneNumber);
            }
            else if (intent.getAction() == TextService.PING_RESPONSE_ACTION) {
                double latitude = intent.getDoubleExtra(TextService.PING_RESPONSE_LATITUDE, 0);
                double longitude = intent.getDoubleExtra(TextService.PING_RESPONSE_LONGITUDE, 0);
                String phoneNumber = intent.getStringExtra(TextService.PING_RESPONSE_CONTACT_NAME);
                placeMapMarker(phoneNumber, latitude, longitude);
            }
        }
    }

    // Notify if the service has stopped.
    private void notifyIfServiceStopped() {
        if (!TextService.isRunning()) {
            Date date = new Date();
            //SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String message = "Service stopped at " + format.format(date) + ".";
            giveNotification(message);
        }
    }

    // Update the map marker for the given number.
    private void placeMapMarker(String phoneNumber, double latitude, double longitude) {
        // Add a marker and move the camera
        LatLng position = new LatLng(latitude, longitude);

//        mMarker.remove();
//        mMarker = mMap.addMarker(new MarkerOptions().position(position).title("Pinged Phone"));

        // Place or update the marker.
        if (mMarkers.containsKey(phoneNumber)) {
            Marker marker = mMarkers.get(phoneNumber);
            marker.setPosition(position);
        }
        else {
            String markerText = phoneNumber;
            String contactName = getContactName(phoneNumber, this);
            if (contactName != null && !contactName.isEmpty()) {
                markerText = contactName;
            }
            Marker marker = mMap.addMarker(new MarkerOptions().position(position).title(markerText));
            mMarkers.put(phoneNumber, marker);
        }

        // Go to the placed marker.
        mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
    }

    // Ask whether to allow a ping request.
    private void askWhetherToAllow(String phoneNumber) {

        // Avoid showing more than one dialog at a time.
        if (m_dialogIsRunning) {
            System.out.println("Ignoring request from " + phoneNumber + " since dialog is alraedy running.");
            return;
        }
        m_dialogIsRunning = true;

        System.out.println("Asking whether to allow " + phoneNumber);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm");
        builder.setMessage("Allow ping request from " + phoneNumber + "?");

        // Add the buttons
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                PingDbHelper database = new PingDbHelper(getApplicationContext());
                database.addWhitelistContact(phoneNumber);
                System.out.println("Added " + phoneNumber + " to whitelist.");
                m_dialogIsRunning = false;
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                m_dialogIsRunning = false;
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Show an alert message.
    private void showMessageDialog(String message) {
        // Set up the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Message from Ping");
        builder.setMessage(message);
        builder.setNeutralButton("OK", null);

        // Create the dialog.
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();

        IntentFilter filter = new IntentFilter(TextService.BROADCAST_ACTION);
        filter.addAction(TextService.ASK_WHETHER_TO_ALLOW);
        filter.addAction(TextService.PING_RESPONSE_ACTION);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new ResponseReceiver();
        registerReceiver(receiver, filter);

        updateStatusLabel();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // Create the channel for notifications.
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
        // Check for required permissions.
        String[] requiredPermissions = new String[] {
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
//                Manifest.permission.ACCESS_BACKGROUND_LOCATION
//                Manifest.permission.FOREGROUND_SERVICE
        };

        // Start the service if these are granted.
        if (checkForPermissions(requiredPermissions, REQUEST_CODE_START_SERVICE)) {
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PICK_CONTACT) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted. Continue the action or workflow
                // in your app.
                chooseContactToPing();
            } else {
                System.out.println("Ping must be able to read contacts to work.");
            }
        }
        else if (requestCode == REQUEST_CODE_START_SERVICE) {
            // Check if all results were granted.
            boolean okToStart = true;
            String missingPermission = "";

            if (grantResults.length == 0) {
                okToStart = false;
            }
            else {
                for (int i = 0; i < grantResults.length; ++i) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        missingPermission = permissions[i];
                        okToStart = false;
                        break;
                    }
                }
            }

            // Start service if all granted.
            if (okToStart) {
                startService(new Intent(this, TextService.class));
            }
            else {
                String message = "Could not start service, since not all permissions were granted.";
                message += "\nMissing: " + missingPermission;
                showMessageDialog(message);
            }
        }
    }

    // Return true if the app has the given permission.
    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(
                getApplicationContext(), permission) ==
                PackageManager.PERMISSION_GRANTED;
    }

    // Check that the required permissions are granted.
    // If not, ask for permission with the given request code.
    private boolean checkForPermissions(String[] permissions, int requestCode) {

        boolean mustAsk = false;
        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                mustAsk = true;
                break;
            }
        }

        if (!mustAsk) {
            return true;
        }

        if (shouldShowRequestPermissionRationale()) {
            System.out.println("Ping must read your location and reply to texts to work.");
        } else {
            ActivityCompat.requestPermissions(this, permissions, requestCode);
        }

        return false;
    }

    // Check if a single permission is granted.
    private boolean checkForPermission(String permission, int requestCode) {
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
                    requestCode);
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

    // Give a notification.
    private void giveNotification(String message) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.message_icon)
                .setContentTitle("Message from Ping")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setChannelId(CHANNEL_ID);

        // notificationId is a unique int for each notification that you must define
        int notificationId = 1;
        Notification notification = builder.build();
        notificationManager.notify(notificationId, notification);
    }

    // Choose a phone to ping.
    private void chooseContactToPing() {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                Contacts.CONTENT_URI);
        startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
    }

    public void onPingButtonClick(View view) {
        // Check for permissions first.
        if (checkForPermission(Manifest.permission.READ_CONTACTS, REQUEST_CODE_PICK_CONTACT)) {
            chooseContactToPing();
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

    // Choose allowed contacts.
    public void onWhitelistButtonClick(View view) {
        Intent intent = new Intent(this, WhitelistActivity.class);
        startActivity(intent);
    }

    // Look up contact name from phone number.
    // From:
    // https://stackoverflow.com/questions/3079365/android-retrieve-contact-name-from-phone-number
    public String getContactName(final String phoneNumber, Context context)
    {
        Uri uri=Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(phoneNumber));

        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};

        String contactName="";
        Cursor cursor=context.getContentResolver().query(uri,projection,null,null,null);

        if (cursor != null) {
            if(cursor.moveToFirst()) {
                contactName=cursor.getString(0);
            }
            cursor.close();
        }

        return contactName;
    }
}