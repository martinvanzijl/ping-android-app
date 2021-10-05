package com.example.myfirstapp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Objects;

/**
 * Reads text messages and sends response.
 */
public class TextService extends Service {

    // Command to stop the service.
    public static final String STOP = "STOP";

    // Hack to let activity know about status.
    private static boolean m_isRunning = false;

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
                            sendText(number);
                            System.out.println("Ping reply sent to " + number);
                        }
                        else {
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
        }
        else {
            // TODO: Ask whether to allow or not.
            return false;
        }
    }

    // Send a reply.
    private void sendText(String number) {
        try {
            SmsManager manager = SmsManager.getDefault();
            String text = "Ping reply.";
            manager.sendTextMessage(number, null, text, null, null);
        }
        catch (SecurityException e) {
            System.out.println(e.getLocalizedMessage());
        }
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