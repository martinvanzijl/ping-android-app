package com.example.myfirstapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_WRITE_ACCESS = 3000;
    private static SettingsActivity m_instance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_instance = this;

        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            SwitchPreferenceCompat loggingPreference = findPreference("enable_logging");
            loggingPreference.setOnPreferenceClickListener(preference -> {
//                    Log.i("Logging", "Preference clicked.");
                SwitchPreferenceCompat switchPreference = (SwitchPreferenceCompat) preference;
                if (switchPreference.isChecked()) {
//                        Log.i("Logging", "Switch is checked.");
                    if (m_instance != null) {
                        m_instance.checkWriteFilePermissions();
                    }
                }
                return true;
            });
        }
    }

    private void checkWriteFilePermissions() {
        // Start the service if these are granted.
        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (checkForPermission(permission, REQUEST_CODE_WRITE_ACCESS)) {
            Log.i("Logging", "Logging enabled");
        }
    }

    // Check if a single permission is granted.
    private boolean checkForPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(
                getApplicationContext(), permission) ==
                PackageManager.PERMISSION_GRANTED) {
            return true;
        } else if (shouldShowRequestPermissionRationale()) {
            System.out.println("Ping must be able to read and send text messages to work.");
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] { permission },
                    requestCode);
        }
        return false;
    }

    // Check if permission rationale should be shown.
    private boolean shouldShowRequestPermissionRationale() {
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_WRITE_ACCESS) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted.
                Log.i("Logging", "Logging enabled");
            } else {
                Log.w("Logging", "Write access is required to create log files.");
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (item.getItemId()) {
            case android.R.id.home:
                //NavUtils.navigateUpFromSameTask(this);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onEnableLoggingClick(SwitchPreferenceCompat view) {
        Log.i("Logging", "Logging click handler called.");
        if (view.isChecked()) {
            Log.i("Logging", "Switch is checked.");
        }
    }
}