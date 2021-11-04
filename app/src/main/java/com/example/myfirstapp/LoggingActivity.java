package com.example.myfirstapp;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.IOException;

public class LoggingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging);

        // Add "back" button at top left.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Show the logging location.
        updateLoggingLabel();
    }

    // Update the label about logging.
    private void updateLoggingLabel() {
        // Check if enabled in settings.
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        boolean enabled = sharedPreferences.getBoolean("enable_logging", false);

        // Show location if enabled.
        if (enabled) {
            try {
                TextView label = findViewById(R.id.textViewLogFilesLocation);
                String text = "Logging is enabled. The log files location is:\n";
                text += Logger.getLogFileDir().getAbsolutePath();
                label.setText(text);
            }
            catch (IOException e) {
                Log.w("Logging", e.getLocalizedMessage());
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}