package com.example.myfirstapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "PING_CHANNEL";
    private static final int REQUEST_CODE = 1000;

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
        if (ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            sendText();
        } else if (shouldShowRequestPermissionRationale()) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            //showInContextUI(...);
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            // You can directly ask for the permission.
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.SEND_SMS },
                    REQUEST_CODE);
            sendText();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted. Continue the action or workflow
                // in your app.
            } else {
                // Explain to the user that the feature is unavailable because
                // the features requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
            }
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }

    private boolean shouldShowRequestPermissionRationale() {
        return false;
    }

    private void sendText() {
        try {
            SmsManager manager = SmsManager.getDefault();
            String dest = "555 1234";
            String source = "555 1234";
            String text = "Ping reply.";
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