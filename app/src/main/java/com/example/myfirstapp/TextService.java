package com.example.myfirstapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Objects;

/**
 * Reads text messages.
 */
public class TextService extends Service {

    public static final String STOP = "STOP";

    public TextService() {
        System.out.println("*** TextService constructor called. ***");
    }

    @Override
    public void onCreate() {
        System.out.println("*** TextService onCreate called. ***");
        giveNotification();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("Intent: " + intent.getAction());

        String action = intent.getAction();
        if (Objects.equals(action, STOP)) {
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        System.out.println("Service being destroyed.");
    }

    /**
     * Give a notification to the user.
     */
    private void giveNotification() {
        String CHANNEL_ID = "PingChannel";
        String textTitle = "Ping Response Sent";
        String textContent = "Current location sent via text.";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(textTitle)
                .setContentText(textContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        int notificationId = 0;
        notificationManager.notify(notificationId, builder.build());
    }

//    /**
//     * Handler of incoming messages from clients.
//     */
//    class IncomingTextMessageHandler extends Handler {
//        @Override
//        public void handleMessage(Message msg) {
////            sendNotification(1, "This is a sample message", "John Doe",
////                    System.currentTimeMillis());
//            Message reply = new Message();
//            final boolean result = sendMessage(reply);
//        }
//    }

//    private void sendReply() {
//        SmsManager manager = SmsManager.getDefault();
//        String dest = "555 1234";
//        String source = "555 1234";
//        String text = "Ping reply.";
//        manager.sendTextMessage(dest, source, text, null, null);
//    }
}