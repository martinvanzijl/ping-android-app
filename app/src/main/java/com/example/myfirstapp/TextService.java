package com.example.myfirstapp;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Reads text messages and sends response.
 */
public class TextService extends Service {

    // Command to stop the service.
    public static final String STOP = "STOP";
    public static final String ASK_WHETHER_TO_ALLOW = "ASK_WHETHER_TO_ALLOW";
    public static final String PING_RESPONSE_ACTION = "PING_RESPONSE";
    public static final String PING_RESPONSE_LATITUDE = "PING_RESPONSE_LATITUDE";
    public static final String PING_RESPONSE_LONGITUDE = "PING_RESPONSE_LONGITUDE";
    public static final String PING_RESPONSE_ADDRESS = "PING_RESPONSE_ADDRESS";
    public static final String PING_RESPONSE_CONTACT_NAME = "PING_RESPONSE_CONTACT_NAME";
    public static final String COMMAND_SEND_PING_RESPONSE = "COMMAND_SEND_PING_RESPONSE";
    public static final String INTENT_EXTRA_NUMBER = "INTENT_EXTRA_NUMBER";

    // Hack to let activity know about status.
    private static boolean m_isRunning = false;

    // Receiver object for text messages.
    final SmsReceiver smsReceiver = new SmsReceiver();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Be notified when texts are received.
        registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));

        // Check whether to stop to service.
        String action = intent.getAction();
        if (Objects.equals(action, STOP)) {
            stopSelf();
            m_isRunning = false;
        } else {
            m_isRunning = true;
        }

        // Check whether to send response.
        if (Objects.equals(action, COMMAND_SEND_PING_RESPONSE)) {
            String number = intent.getStringExtra(INTENT_EXTRA_NUMBER);
            sendPingReply(number);
        }

        // Let activity know about status.
        broadcastStatusChange();
        
        // Keep the service active after activity is closed.
        return START_STICKY;
    }

    public static final String BROADCAST_ACTION = "TEXT_SERVICE_BROADCAST";

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(smsReceiver);
        }
        catch (IllegalArgumentException e) {
            Log.w("Ping", "Receiver not registered, so cannot unregister.");
        }

        m_isRunning = false;
        broadcastStatusChange();

        appendLog("Service destroyed.");
    }

    // Broadcast to activity that status has been changed.
    private void broadcastStatusChange() {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_ACTION);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        sendBroadcast(intent);
    }

    // Class that receives text messages.
    private class SmsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            appendLog("Text message received.");

            String number = "";
            StringBuilder body = new StringBuilder();

            Bundle extras = intent.getExtras();

            if (extras != null) {
                Object[] pdus = (Object[]) extras.get("pdus");
                if (pdus != null) {

                    // Get the message details.
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = getIncomingMessage(pdu, extras);
                        body.append(smsMessage.getDisplayMessageBody());
                        number = smsMessage.getOriginatingAddress();
                    }

                    // Send a reply.
                    String text = body.toString();
                    if (text.equals(MainActivity.PING_REQUEST_TEXT)) {
                        if (checkIfAllowed(number)) {
                            sendPingReply(number);
                            System.out.println("Ping reply sent to " + number);
                        } else {
                            System.out.println("Ping request from " + number + " ignored.");
                        }
                    }
                    else if(text.startsWith(MainActivity.PING_REPLY_START)){
                        processPingResponse(text, number);
                    }
                }
            }
        }

        // Get the text message from the raw object.
        private SmsMessage getIncomingMessage(Object object, Bundle bundle) {
            SmsMessage smsMessage;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String format = bundle.getString("format");
                smsMessage = SmsMessage.createFromPdu((byte[]) object, format);
            } else {
                smsMessage = SmsMessage.createFromPdu((byte[]) object);
            }

            return smsMessage;
        }
    }

    // Write a message to the log file.
    private void appendLog(String message) {
        Logger.appendLog(getApplicationContext(), message);
    }

    private void processPingResponse(String text, String phoneNumber) {
        try {
            double latitude = 0;
            double longitude = 0;
            String address = "";
            String[] lines = text.split("\n");
            for (String line : lines) {
                if (line.startsWith("Latitude: ")) {
                    String numberString = line.replace("Latitude: ", "");
                    latitude = Double.parseDouble(numberString);
                } else if (line.startsWith("Longitude: ")) {
                    String numberString = line.replace("Longitude: ", "");
                    longitude = Double.parseDouble(numberString);
                } else if (line.startsWith("Address: ")) {
                    address = line.replace("Address: ", "");
                }
            }
            System.out.println("Broadcasting ping response.");

            Intent intent = new Intent();
            intent.setAction(PING_RESPONSE_ACTION);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            //intent.putExtra(Intent.EXTRA_TEXT, new LocationData(latitude, longitude));
            intent.putExtra(PING_RESPONSE_LATITUDE, latitude);
            intent.putExtra(PING_RESPONSE_LONGITUDE, longitude);
            intent.putExtra(PING_RESPONSE_ADDRESS, address);
            intent.putExtra(PING_RESPONSE_CONTACT_NAME, phoneNumber);
            sendBroadcast(intent);
        }
        catch (NumberFormatException e) {
             Log.w("Ping", e.getLocalizedMessage());
        }
    }

    // Check if ping request is allowed from number.
    private boolean checkIfAllowed(String number) {
        PingDbHelper database = new PingDbHelper(this);
        if (database.whitelistContactExists(number)) {
            return true;
        } else {
            askWhetherToAllow(number);
            return false;
        }
    }

    private void askWhetherToAllow(String number) {
        System.out.println("Service is asking whether to allow " + number);
        Intent intent = new Intent();
        intent.setAction(ASK_WHETHER_TO_ALLOW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(Intent.EXTRA_TEXT, number);
        sendBroadcast(intent);
    }

    // Send a text message.
    private void sendText(String number, String text) {
        appendLog("Trying to send text message.");

        try {
            SmsManager manager = SmsManager.getDefault();
            manager.sendTextMessage(number, null, text, null, null);
            appendLog("Text message sent.");
        } catch (SecurityException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    // Send a ping reply to the given number.
    private void sendPingReply(String phoneNumber) {
        appendLog("Trying to send ping reply.");

        // Get the location provider.
        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check that the right permissions have been granted.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("No permission for getting location information.");
            appendLog("No permission to get location data.");
            return;
        }

        // Try getting the current location.
        mFusedLocationClient.getCurrentLocation(LocationRequest.QUALITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    System.out.println("Current location gotten");

                    // GPS location can be null if GPS is switched off
                    // or not gotten in reasonable time.
                    if (location != null) {
                        appendLog("Got current location.");
                        onAddressLocated(phoneNumber, location);
                    }
                    else {
                        appendLog("Current location was null.");
                        replyWithLastLocation(phoneNumber);
                    }
                })
                .addOnFailureListener(e -> {
                    System.out.println("Error trying to get current GPS location");
                    appendLog("Failed to get current location.");
                    replyWithLastLocation(phoneNumber);
                });
    }

    // Reply with the last (previous known) location.
    private void replyWithLastLocation(String phoneNumber) {
        // Get the location provider.
        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check that the right permissions have been granted.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("No permission for getting location information.");
            return;
        }

        // Get the last (previous known) location.
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    System.out.println("Last location gotten");

                    // GPS location can be null if GPS is switched off
                    if (location != null) {
                        appendLog("Got last location.");
                        onAddressLocated(phoneNumber, location);
                    }
                    else {
                        appendLog("Last location was null.");
                    }
                })
                .addOnFailureListener(e -> {
                    System.out.println("Error trying to get last GPS location");
                    appendLog("Failed to get last location.");
                });
    }

    // Callback for when address is located for the ping reply.
    private void onAddressLocated(String phoneNumber, Location location) {
        System.out.println("Sending text with location.");

        StringBuilder builder = new StringBuilder();
        builder.append(MainActivity.PING_REPLY_START);

        // Include address information if the preference is set.
        if (sendAddressEnabled()) {
            builder.append("\nAddress: ").append(getAddressFor(location));
        }

        builder.append("\nLatitude: ").append(location.getLatitude());
        builder.append("\nLongitude: ").append(location.getLongitude());
        String message = builder.toString();
        sendText(phoneNumber, message);
    }

    // Get the address for the given location.
    // From: https://stackoverflow.com/questions/9409195/how-to-get-complete-address-from-latitude-and-longitude
    private String getAddressFor(Location location) {
        try {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(this, Locale.getDefault());

            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5

            //noinspection UnnecessaryLocalVariable
            String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
//        String city = addresses.get(0).getLocality();
//        String state = addresses.get(0).getAdminArea();
//        String country = addresses.get(0).getCountryName();
//        String postalCode = addresses.get(0).getPostalCode();
//        String knownName = addresses.get(0).getFeatureName(); // Only if available else return NULL

            return address;
        } catch (IOException e) {
            Log.w("Address", e.getLocalizedMessage());
            appendLog("Exception getting address: " + e.getLocalizedMessage());
            return "Unknown (Exception)";
        }
    }

    // Check whether or not to send the address in replies.
    private boolean sendAddressEnabled() {
        // Read value from settings.
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean("include_address_in_reply", false);
    }

    // Check if service is running.
    public static boolean isRunning() {
        return m_isRunning;
    }
}