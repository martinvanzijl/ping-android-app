package com.example.myfirstapp;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Objects;

/**
 * Reads text messages and sends response.
 */
public class TextService extends Service {

    // Command to stop the service.
    public static final String STOP = "STOP";
    public static final String ASK_WHETHER_TO_ALLOW = "ASK_WHETHER_TO_ALLOW";

    // Hack to let activity know about status.
    private static boolean m_isRunning = false;
    private static String m_numberToAskAbout = "555 1234";

    // Receiver object for text messages.
    SmsReceiver smsReceiver = new SmsReceiver();

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

        // Let activity know about status.
        broadcastStatusChange();

        // Keep the service active after activity is closed.
        return START_STICKY;
    }

    public static final String BROADCAST_ACTION = "TEXT_SERVICE_BROADCAST";

    @Override
    public void onDestroy() {
        unregisterReceiver(smsReceiver);
        m_isRunning = false;
        broadcastStatusChange();
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
                    if (body.toString().equals(MainActivity.PING_REQUEST_TEXT)) {
                        if (checkIfAllowed(number)) {
                            sendPingReply(number);
                            System.out.println("Ping reply sent to " + number);
                        } else {
                            System.out.println("Ping request from " + number + " ignored.");
                        }
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

    private boolean askWhetherToAllow(String number) {
        System.out.println("Service is asking whether to allow " + number);
        Intent intent = new Intent();
        intent.setAction(ASK_WHETHER_TO_ALLOW);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(Intent.EXTRA_TEXT, number);
        sendBroadcast(intent);
        return false;
    }

    // Send a text message.
    private void sendText(String number, String text) {
        try {
            SmsManager manager = SmsManager.getDefault();
            manager.sendTextMessage(number, null, text, null, null);
        } catch (SecurityException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    // Send a ping reply to the given number.
    private void sendPingReply(String phoneNumber) {
        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("No permission for getting location information.");
            return;
        }

        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // GPS location can be null if GPS is switched off
                        if (location != null) {
                            onAddressLocated(phoneNumber, location);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println("Error trying to get last GPS location");
                        e.printStackTrace();
                    }
                });
    }

    // Callback for when address is located for the ping reply.
    private void onAddressLocated(String phoneNumber, Location location) {
        StringBuilder builder = new StringBuilder();
        builder.append("Ping reply.");
        builder.append("\nLatitude: ").append(location.getLatitude());
        builder.append("\nLongitude: ").append(location.getLongitude());
        String message = builder.toString();
        sendText(phoneNumber, message);
    }

    /**
     * Give a notification to the user.
     */
    private void giveNotification() {
        String title = "Ping Response Sent";
        String text = "Current location sent via text.";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.message_icon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_MAX);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        int notificationId = 1;
        notificationManager.notify(notificationId, builder.build());
    }

    // Check if service is running.
    public static boolean isRunning() {
        return m_isRunning;
    }
}