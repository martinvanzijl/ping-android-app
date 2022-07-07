package com.example.myfirstapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;

// Class that receives text messages.
public class SmsReceiver extends BroadcastReceiver {
    private static TextService textService = null;

    public SmsReceiver() {
    }

    public SmsReceiver(TextService textService) {
        SmsReceiver.textService = textService;
    }

    public static void setService(TextService textService) {
        SmsReceiver.textService = textService;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        appendLog("Text message received.");

        if (textService == null) {
            System.out.println("Receive ignored since service is null.");
            return;
        }

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
                    if (textService.checkIfAllowed(number)) {
                        textService.sendPingReply(number);
                        System.out.println("Ping reply sent to " + number);
                    } else {
                        System.out.println("Ping request from " + number + " ignored.");
                    }
                } else if (text.startsWith(MainActivity.PING_REPLY_START)) {
                    textService.processPingResponse(text, number);
                }
            }
        }
    }

    // Write a message to the log file.
    private void appendLog(String message) {
        if (textService != null) {
            textService.appendLog(message);
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
