package com.example.myfirstapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "PING_CHANNEL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();
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
        startService(new Intent(this, TextService.class));
    }

    public void onButtonStopServiceClick(View view) {
        Intent intent = new Intent(this, TextService.class);
        intent.setAction(TextService.STOP);
        startService(intent);
    }

    public void onButtonSendTextClick(View view) {
        SmsManager manager = SmsManager.getDefault();
        String dest = "555 1234";
        String source = "555 1234";
        String text = "Ping reply.";

        try {
            manager.sendTextMessage(dest, source, text, null, null);
        }
        catch (SecurityException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    public void onButtonGiveNotificationClick(View view) {
        giveNotification();
    }

    /**
     * Give a notification to the user.
     */
    private void giveNotification() {
        System.out.println("Giving notification...");
        String title = "Ping Response Sent";
        String text = "Current location sent via text.";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.message_icon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_MAX);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        int notificationId = 1;
        notificationManager.notify(notificationId, builder.build());
    }
}