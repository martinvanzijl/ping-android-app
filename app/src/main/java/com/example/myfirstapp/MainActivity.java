package com.example.myfirstapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String PING_REPLY_START = "Ping reply.";
    static final String CHANNEL_ID = "PING_CHANNEL";
    static final String PING_REQUEST_TEXT = "Sent from Ping App. Where are you?";
    private static final int REQUEST_CODE_PICK_CONTACT = 1000;
    private static final int REQUEST_CODE_START_SERVICE = 1001;
    // --Commented out by Inspection (9/15/2022 1:48 PM):private static final int OSM_MAP_REQUEST_CODE = 1002;
    private GoogleMap mMap = null;
    private MapView map = null;
    private boolean m_usingOSM = true;
//    private Marker mMarker;
    private final Map<String, MarkerProxy> mLatestMarkers = new HashMap<>();
    private final List<MarkerProxy> mHistoricMarkers = new ArrayList<>();
    private boolean m_dialogIsRunning = false;
    private boolean m_mapIsExpanded = false;
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat SHORT_TIMESTAMP_FORMAT =
            new SimpleDateFormat("hh:mm aa");
    private ActivityResultLauncher<Intent> chooseContactActivity = null;
    private ActivityResultLauncher<Intent> chooseContactFromWhitelistActivity = null;
    private PingDbHelper dbHelper = null;

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
                    updateStartButtonLabel();
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
                    String address = intent.getStringExtra(TextService.PING_RESPONSE_ADDRESS);
                    try {
                        placeMapMarker(phoneNumber, latitude, longitude, address);
                    } catch (Exception e) {
                        Logger.appendLog(getApplicationContext(),
                                "Could not place map marker: " + e.getLocalizedMessage());
                    }
                    break;
                }
            }
        }
    }

    /**
     * Update the start button label, depending on service status.
     */
    private void updateStartButtonLabel() {
        TextView view = findViewById(R.id.buttonStartService);
        if (TextService.isRunning()) {
            view.setText(R.string.restart_service_label);
        }
        else {
            view.setText(R.string.start_service_label);
        }
    }

    // Notify if the service has stopped.
    private void notifyIfServiceStopped() {
        if (!TextService.isRunning() && notificationWhenServiceStopsEnabled()) {
            giveServiceStoppedNotification();
        }
    }

    /**
     * Show a notification that the service has stopped.
     */
    private void giveServiceStoppedNotification() {
        Date date = new Date();
        //SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String message = "Service stopped at " + format.format(date) + ".";
        giveNotification(message);
    }

    // Update the map marker for the given number.
    private void placeMapMarker(String phoneNumber, double latitude, double longitude, String address) {
        // Add a marker and move the camera
        LatLng position = new LatLng(latitude, longitude);

        // Hide the previous marker for the contact if "show history" is turned off.
        if (mLatestMarkers.containsKey(phoneNumber) && !showLocationHistoryEnabled()) {
            MarkerProxy marker = mLatestMarkers.get(phoneNumber);
            assert marker != null;
            marker.setVisible(false);
        }

        // Place a new marker.
        String markerText = phoneNumber;
        String contactName = getContactName(phoneNumber, this);
        if (contactName != null && !contactName.isEmpty()) {
            markerText = contactName;
        }
        String timestamp = getShortTimestamp();
        markerText += " (" + timestamp + ")";

        // Make proxy.
        MarkerProxy markerProxy = null;

        if (!m_usingOSM) {
            // Using Google Maps.
            Marker marker = mMap.addMarker(new MarkerOptions().position(position).title(markerText));

            // Set proxy.
            markerProxy = new MarkerProxy(marker);

            // Go to the placed marker.
            mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
        }

        if (m_usingOSM) {
            // Create point.
            GeoPoint point = new GeoPoint(latitude, longitude);

            // Add marker.
            org.osmdroid.views.overlay.Marker mOverlay = new org.osmdroid.views.overlay.Marker(map);
            mOverlay.setPosition(point);
            mOverlay.setTitle(markerText);
            mOverlay.setSubDescription("");
            map.getOverlays().add(mOverlay);

            // Set proxy.
            markerProxy = new MarkerProxy(mOverlay);

            // Move the camera to the point.
            IMapController mapController = map.getController();
            mapController.setCenter(point);
        }

        // Update map.
        mLatestMarkers.put(phoneNumber, markerProxy);

        // Update list.
        mHistoricMarkers.add(markerProxy);

        // Update database.
        if (storeLocationHistoryEnabled()) {
            dbHelper.addLocationHistory(phoneNumber, latitude, longitude, address, contactName);
        }
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

    // Check if "store location history" is enabled.
    private boolean storeLocationHistoryEnabled() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean("store_location_history", false);
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

        // Get contact name.
        String contactName = getContactName(phoneNumber, this);

        // Build the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm");
        builder.setMessage("Allow ping request from " + contactName + "?");

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

        // Check if using OSM.
        m_usingOSM = isOsmSelected();

        // Load OsmDroid configuration. Do this before setting the layout.
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, android.preference.PreferenceManager.getDefaultSharedPreferences(ctx));

        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        // Debug.
        Log.i("Ping", "Map configuration loaded.");

        // Set the layout.
        setContentView(R.layout.activity_main);

        // Set the map fragment.
        createMapFragment();

        // Handle copyright label.
        TextView textView = findViewById(R.id.textViewOSMCopyright);

        if (m_usingOSM) {
            // Add click handler to copyright label.
            textView.setOnClickListener(view -> onTextViewOSMCopyRightClick());
        }
        else {
            // Using Google Maps. Hide the label.
            textView.setVisibility(View.GONE);
        }

        // Create the notification channel.
        createNotificationChannel();

        IntentFilter filter = new IntentFilter(TextService.BROADCAST_ACTION);
        filter.addAction(TextService.ASK_WHETHER_TO_ALLOW);
        filter.addAction(TextService.PING_RESPONSE_ACTION);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        ResponseReceiver receiver = new ResponseReceiver();
        registerReceiver(receiver, filter);

        updateStatusLabel();

        // This causes an exception.
        //Logger.getLogger("MainActivity").info("Started app.");
        appendLog("Started app.");

        // Automatically start service if setting is enabled.
        if (autoStartServiceEnabled()) {
            appendLog("Automatically starting service.");

            try {
                startTextService();
            }
            catch (IllegalStateException e) {
                Log.w("Ping", e.getLocalizedMessage());
                showToastMessage("Problem starting service.");
            }
        }

        // Create result handler.
        chooseContactActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        Intent data = result.getData();
                        assert data != null;
                        choosePingType();
                        pingPhone(data);
                    }
                }
        );

        chooseContactFromWhitelistActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    // Get the returned data.
                    Intent intent = result.getData();
                    assert intent != null;

                    // Send the ping request.
                    String phoneNumber = intent.getDataString();
                    sendPingRequest(phoneNumber);
                }
            }
        );

        // Create preference listener.
        SharedPreferences.OnSharedPreferenceChangeListener prefListener = (preferences, key) -> {
            Log.i("Preferences", "Settings key changed: " + key);

            // Handle location history preference change.
            if (key.equals("show_location_history")) {
                // Get value of preference.
                boolean showHistoricMarkers = showLocationHistoryEnabled();

                // Show or hide historic markers.
                for (MarkerProxy marker : mHistoricMarkers) {
                    marker.setVisible(showHistoricMarkers);
                }

                // Always show latest.
                for (MarkerProxy marker : mLatestMarkers.values()) {
                    marker.setVisible(true);
                }
            }
        };

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(prefListener);

        dbHelper = new PingDbHelper(this);
    }

    // Store the selected value.
    PingType chosenPingType = PingType.ONCE;

    /**
     * Choose what type of ping to send (single or recurring).
     * @return The type of ping to send.
     */
    PingType choosePingType() {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new PingTypeDialogFragment();
        dialog.show(getSupportFragmentManager(), "PingTypeDialogFragment");

        // Return.
        Log.i("Ping", "Returning ping type.");
        return chosenPingType;
    }

    /**
     * Show a "toast" message.
     * @param message The message.
     */
    @SuppressWarnings("SameParameterValue")
    protected void showToastMessage(String message) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    /**
     * Handle click on OSM copyright link.
     */
    private void onTextViewOSMCopyRightClick() {
        String link = "https://www.openstreetmap.org/copyright";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        startActivity(browserIntent);
    }

    /**
     * Check if OpenStreetMap is selected in the preferences.
     * @return True if user selected OpenStreetMap.
     */
    private boolean isOsmSelected() {
        // Get preference value.
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        String selectedOption = sharedPreferences.getString("map_type", "");

        // Check if it is OSM.
        return selectedOption.equals(getString(R.string.osm));
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

    @SuppressWarnings("unused")
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

        // Give warning if location services are disabled.
        warnIfLocationServicesDisabled();
    }

    /**
     * Check if location services are enabled on this phone.
     * @param context The app context.
     * @return True if location services are enabled.
     * From: https://stackoverflow.com/questions/10311834/how-to-check-if-location-services-are-enabled/54648795#54648795
     */
    @SuppressWarnings("deprecation")
    public static Boolean isLocationEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // This is a new method provided in API 28
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm.isLocationEnabled();
        } else {
            // This was deprecated in API 28
            int mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            return (mode != Settings.Secure.LOCATION_MODE_OFF);
        }
    }

    /**
     * Warn the user if location services on the phone is disabled.
     */
    private void warnIfLocationServicesDisabled() {
        if (!isLocationEnabled(this)) {
            showMessageDialog("Location services are disabled. To ensure that the phone's " +
                    "location is sent in ping replies, enable the setting, then restart the app.");
        }
    }

    @SuppressWarnings("unused")
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
    @SuppressWarnings("SameReturnValue")
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
        boolean chooseFromPingContacts = choosePingContactFromWhitelistEnabled();
        if (chooseFromPingContacts) {
            Intent contactPickerIntent = new Intent(this, WhitelistActivity.class);
            contactPickerIntent.putExtra(WhitelistActivity.INTENT_CHOOSE_CONTACT, true);
            chooseContactFromWhitelistActivity.launch(contactPickerIntent);
        }
        else {
            Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                    Contacts.CONTENT_URI);
            chooseContactActivity.launch(contactPickerIntent);
        }
    }

    /**
     * Check whether to choose ping contact from the whitelist screen.
     * @return The preference value.
     */
    private boolean choosePingContactFromWhitelistEnabled() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean("choose_ping_contact_from_whitelist", false);
    }

    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
//        else if (id == R.id.action_logging) {
//            Intent intent = new Intent(this, LoggingActivity.class);
//            startActivity(intent);
//            return true;
//        }
        else if (id == R.id.action_location_history) {
            Intent intent = new Intent(this, LocationHistoryActivity.class);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_scheduled_ping) {
            Intent intent = new Intent(this, ScheduledPingActivity.class);
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

    @SuppressWarnings("unused")
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // "Hard stop" the service.
        appendLog("Force service to stop.");

        // Give notification if required.
        if (TextService.isRunning() && notificationWhenServiceStopsEnabled()) {
            giveServiceStoppedNotification();
        }

        Intent intent = new Intent(MainActivity.this, TextService.class);
        stopService(intent);

        // Stop the "scheduled ping" service.
        stopService(new Intent(this, BroadcastService.class));
    }

// --Commented out by Inspection START (9/14/2022 8:20 AM):
//    private void requestPermissionsIfNecessary(String[] permissions) {
//        ArrayList<String> permissionsToRequest = new ArrayList<>();
//        for (String permission : permissions) {
//            if (ContextCompat.checkSelfPermission(this, permission)
//                    != PackageManager.PERMISSION_GRANTED) {
//                // Permission is not granted
//                permissionsToRequest.add(permission);
//            }
//        }
//        if (permissionsToRequest.size() > 0) {
//            ActivityCompat.requestPermissions(
//                    this,
//                    permissionsToRequest.toArray(new String[0]),
//                    OSM_MAP_REQUEST_CODE);
//        }
//    }
// --Commented out by Inspection STOP (9/14/2022 8:20 AM)

    @Override
    protected void onStart() {
        super.onStart();

        // Set up the map.
        setUpMap();
    }

    /**
     * Set up the OSM map.
     */
    private void setUpMap() {

        if (m_usingOSM) {
            // Get the fragment.
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.map);
            OsmMapFragment osmMapFragment = (OsmMapFragment) fragment;

            // Get the OSM map view.
            assert osmMapFragment != null;
            map = osmMapFragment.getMapView();

            // Check that map exists.
            assert map != null;
        }
        else if (mMap == null) {
            // Use Google Maps.

            // Get the map fragment.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            assert mapFragment != null;
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * Create the map fragment.
     */
    private void createMapFragment() {

        // Create new fragment and transaction
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setReorderingAllowed(true);

        // Replace whatever is in the fragment_container view with this fragment
        if (m_usingOSM) {
            transaction.add(R.id.map, com.example.myfirstapp.OsmMapFragment.class, null);
        } else {
            transaction.add(R.id.map, com.google.android.gms.maps.SupportMapFragment.class, null);
        }

        // Commit the transaction
//        transaction.commitNow();
        transaction.commit();
    }
}