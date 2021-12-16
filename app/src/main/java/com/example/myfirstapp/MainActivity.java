package com.example.myfirstapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

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
    private GoogleMap mMap;
//    private Marker mMarker;
    private final Map<String, Marker> mMarkers = new HashMap<>();
    private boolean m_dialogIsRunning = false;
    private boolean m_mapIsExpanded = false;
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat SHORT_TIMESTAMP_FORMAT =
            new SimpleDateFormat("hh:mm aa");
    private ActivityResultLauncher<Intent> chooseContactActivity = null;

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
    }

    // Receives messages from the service.
    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
//            System.out.println("Received broadcast.");
            switch (intent.getAction()) {
                case TextService.BROADCAST_ACTION:
                    updateStatusLabel();
                    notifyIfServiceStopped();
                    break;
                case TextService.ASK_WHETHER_TO_ALLOW: {
                    String phoneNumber = intent.getStringExtra(Intent.EXTRA_TEXT);
                    askWhetherToAllow(phoneNumber);
                    break;
                }
                case TextService.PING_RESPONSE_ACTION: {
                    double latitude = intent.getDoubleExtra(TextService.PING_RESPONSE_LATITUDE, 0);
                    double longitude = intent.getDoubleExtra(TextService.PING_RESPONSE_LONGITUDE, 0);
                    String phoneNumber = intent.getStringExtra(TextService.PING_RESPONSE_CONTACT_NAME);
                    placeMapMarker(phoneNumber, latitude, longitude);
                    break;
                }
            }
        }
    }

    // Notify if the service has stopped.
    private void notifyIfServiceStopped() {
        if (!TextService.isRunning() && notificationWhenServiceStopsEnabled()) {
            Date date = new Date();
            //SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            @SuppressLint("SimpleDateFormat")
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
        if (mMarkers.containsKey(phoneNumber) && !showLocationHistoryEnabled()) {
            // Update the existing marker for the contact.
            Marker marker = mMarkers.get(phoneNumber);
            assert marker != null;
            marker.setPosition(position);
        }
        else {
            // Place a new marker.
            String markerText = phoneNumber;
            String contactName = getContactName(phoneNumber, this);
            if (contactName != null && !contactName.isEmpty()) {
                markerText = contactName;
            }
            markerText += " (" + getShortTimestamp() + ")";
            Marker marker = mMap.addMarker(new MarkerOptions().position(position).title(markerText));
            mMarkers.put(phoneNumber, marker);
        }

        // Go to the placed marker.
        mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
    }

    // Check if "show location history" is enabled.
    private boolean showLocationHistoryEnabled() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean("show_location_history", false);
    }

    // Check if "ignore unlisted contacts" is enabled.
    private boolean ignoreUnlistedContactsEnabled() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean("ignore_unlisted_contacts", false);
    }

    // Check if "warn before stopping service" is enabled.
    private boolean warnBeforeStoppingServiceEnabled() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean("warn_before_stop_service", false);
    }

    // Check if "give notification when service stops" is enabled.
    private boolean notificationWhenServiceStopsEnabled() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean("give_notification_when_service_stops", true);
    }

    // Check if "automatically start service" is enabled.
    private boolean autoStartServiceEnabled() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean("auto_start_service", false);
    }

    // Ask whether to allow a ping request.
    // Does nothing if the preference to ignore unlisted contacts is enabled.
    private void askWhetherToAllow(String phoneNumber) {

        // Check preference.
        if (ignoreUnlistedContactsEnabled()) {
            appendLog("Ignoring request from non-whitelisted contact.");
            return;
        }

        // Avoid showing more than one dialog at a time.
        if (m_dialogIsRunning) {
            System.out.println("Ignoring request from " + phoneNumber + " since dialog is already running.");
            return;
        }
        m_dialogIsRunning = true;

        System.out.println("Asking whether to allow " + phoneNumber);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm");
        builder.setMessage("Allow ping request from " + phoneNumber + "?");

        // Add the buttons
        builder.setPositiveButton("Yes", (dialog, id) -> {
            // User clicked OK button
            PingDbHelper database = new PingDbHelper(getApplicationContext());
            database.addWhitelistContact(phoneNumber);
            System.out.println("Added " + phoneNumber + " to whitelist.");
            m_dialogIsRunning = false;

            // Also send the ping response.
            Intent intent = new Intent(MainActivity.this, TextService.class);
            intent.setAction(TextService.COMMAND_SEND_PING_RESPONSE);
            intent.putExtra(TextService.INTENT_EXTRA_NUMBER, phoneNumber);
            startService(intent);
        });
        builder.setNegativeButton("No", (dialog, id) -> {
            // User cancelled the dialog
            m_dialogIsRunning = false;
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
        ResponseReceiver receiver = new ResponseReceiver();
        registerReceiver(receiver, filter);

        updateStatusLabel();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        // This causes an exception.
        //Logger.getLogger("MainActivity").info("Started app.");
        appendLog("Started app.");

        // Automatically start service if setting is enabled.
        if (autoStartServiceEnabled()) {
            appendLog("Automatically starting service.");
            startTextService();
        }

        // Create result handler.
        chooseContactActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        Intent data = result.getData();
                        assert data != null;
                        pingPhone(data);
                    }
                }
        );
    }

//    @Override
//    protected void onStop() {
//        super.onStop();
//
//        // "Hard stop" the service.
//        appendLog("Force service to stop.");
//        Intent intent = new Intent(MainActivity.this, TextService.class);
//        stopService(intent);
//    }

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
//        Logger.info(this, "Start service button clicked.");
        appendLog("Start service button clicked.");

        startTextService();
    }

    /**
     * Start the app service.
     */
    private void startTextService() {
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
        appendLog("Stop service button clicked.");

        // Check if user should confirm.
        if (warnBeforeStoppingServiceEnabled()) {
            appendLog("Confirming before stopping service.");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Confirm");
            builder.setMessage("Really stop the service?");

            // Add the buttons
            builder.setPositiveButton("Yes", (dialog, id) -> pauseService());
            builder.setNegativeButton("No", null);

            // Create the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.show();

            // Return for now.
            return;
        }

        // No need to confirm. Just stop the service.
        pauseService();
    }

    /**
     * Stop the service.
     */
    private void pauseService() {
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
    @SuppressWarnings("SameParameterValue")
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
    @SuppressWarnings("SameParameterValue")
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
        chooseContactActivity.launch(contactPickerIntent);
    }

    public void onPingButtonClick(View view) {
        // Check for permissions first.
        if (checkForPermission(Manifest.permission.READ_CONTACTS, REQUEST_CODE_PICK_CONTACT)) {
            chooseContactToPing();
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
            String contactName = getContactName(number, this);
            String labelMessage = getString(R.string.ping_request_sent_message, contactName);
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

    // Check if the context has the permission.
    @SuppressWarnings("SameParameterValue")
    private static boolean checkForPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED;
    }

    // Look up contact name from phone number.
    // From:
    // https://stackoverflow.com/questions/3079365/android-retrieve-contact-name-from-phone-number
    public static String getContactName(final String phoneNumber, Context context)
    {
        // Default to empty string.
        String contactName="";

        // Check for permissions first.
        if (!checkForPermission(context, Manifest.permission.READ_CONTACTS)) {
            Log.w("Contact Name", "No permission to read contacts.");
            return contactName;
        }

        // Read the contact name from the database.
        Uri uri=Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(phoneNumber));

        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};

        Cursor cursor=context.getContentResolver().query(uri,projection,null,null,null);

        if (cursor != null) {
            if(cursor.moveToFirst()) {
                contactName=cursor.getString(0);
            }
            cursor.close();
        }

        return contactName;
    }

    // Write a message to the log file.
    public void appendLog(String message) {
        Logger.appendLog(this, message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_logging) {
            Intent intent = new Intent(this, LoggingActivity.class);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_help) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onExpandMapClick(View view) {
        toggleMapExpanded();
    }

    private void toggleMapExpanded() {
        setMapExpanded(!m_mapIsExpanded);
    }

    private void setMapExpanded(boolean expanded) {
        int visibility = expanded ? View.GONE : View.VISIBLE;

        // Show or hide other components.
        findViewById(R.id.buttonPing).setVisibility(visibility);
        findViewById(R.id.textViewPingStatus).setVisibility(visibility);
        findViewById(R.id.buttonWhitelist).setVisibility(visibility);
        findViewById(R.id.textViewServiceStatus).setVisibility(visibility);
        findViewById(R.id.buttonStartService).setVisibility(visibility);
        findViewById(R.id.buttonStopService).setVisibility(visibility);

        // Set image.
        ImageView imageView = findViewById(R.id.imageViewExpandMap);
        if (expanded) {
            imageView.setImageResource(R.drawable.minimize_white);
        }
        else {
            imageView.setImageResource(R.drawable.expand_white);
        }

        m_mapIsExpanded = expanded;
    }

    /**
     * Create a short timestamp.
     * @return A short timestamp.
     */
    private String getShortTimestamp() {
        return SHORT_TIMESTAMP_FORMAT.format(new Date());
    }
}