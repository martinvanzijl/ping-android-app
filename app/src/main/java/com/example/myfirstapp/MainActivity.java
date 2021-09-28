package com.example.myfirstapp;

import android.content.Intent;
import android.os.Bundle;
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
}