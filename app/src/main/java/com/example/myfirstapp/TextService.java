package com.example.myfirstapp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsMessage;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Objects;

/**
 * Reads text messages.
 */
public class TextService extends Service {

    public static final String STOP = "STOP";

    SmsReceiver smsReceiver = new SmsReceiver();

    public TextService() {
        System.out.println("*** TextService constructor called. ***");
    }

    @Override
    public void onCreate() {
        System.out.println("*** TextService onCreate called. ***");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("Starting Service Command with Intent: " + intent.getAction());

        registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));

        String action = intent.getAction();
        if (Objects.equals(action, STOP)) {
            stopSelf();
        }

        //return START_NOT_STICKY;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        System.out.println("Service being destroyed.");
        unregisterReceiver(smsReceiver);
    }

    private class SmsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("*** Message receive callback triggered. ***");
            String telnr = "", nachricht = "";

            Bundle extras = intent.getExtras();

            if (extras != null) {
                Object[] pdus = (Object[]) extras.get("pdus");
                if (pdus != null) {

                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = getIncomingMessage(pdu, extras);
                        telnr = smsMessage.getDisplayOriginatingAddress();
                        nachricht += smsMessage.getDisplayMessageBody();
                    }

                    // Here the message content is processed within MainAct
                    //MainActivity.instance().processSMS(telnr.replace("+49", "0").replace(" ", ""), nachricht);
                    System.out.println("Received message: " + nachricht);
                }
            }
        }

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
}