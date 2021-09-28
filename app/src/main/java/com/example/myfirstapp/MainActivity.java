package com.example.myfirstapp;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
}