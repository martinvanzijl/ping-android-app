package com.example.myfirstapp;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LocationHistoryActivity extends AppCompatActivity {
    private PingDbHelper dbHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_history);

        // Connect signal handlers.
        findViewById(R.id.buttonExportLocationHistory).setOnClickListener(this::onButtonExportHistoryClick);
        findViewById(R.id.buttonEmailExportedFile).setOnClickListener(this::onButtonEmailClick);

        // Create fields.
        dbHelper = new PingDbHelper(this);

        // Add "back" button at top left.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void onButtonEmailClick(View view) {
        emailExportedFile();
    }

    /**
     * Send an email with the exported file.
     */
    private void emailExportedFile() {

        try {
            // Use file provider URI.
            String authority = getApplicationContext().getPackageName() + ".provider";
            Uri attachmentURI;
            
            // Attach text file if setting enabled.
            String path = getExportedTextFilePath();

            File file = new File(path);
            if (file.exists()) {
                attachmentURI = FileProvider.getUriForFile(this, authority, file);
            }
            else {
                Log.w("Text Message Exporter", "Text file does not exist.");
                showToastMessage("File does not exist.");
                return;
            }

            // Get the address.
            String[] recipients = null;

            // Create the intent.
            String action = Intent.ACTION_SEND;
            Intent emailIntent = new Intent(action);

            // Create the message bodies (in case of multiple attachments).
            String text = "Here is the Ping location history from my phone.";

            // The intent does not have a URI, so declare the "text/plain" MIME type
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, recipients);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Ping Location History Export");

            // Add attachment.
            emailIntent.putExtra(Intent.EXTRA_TEXT, text);
            emailIntent.putExtra(Intent.EXTRA_STREAM, attachmentURI);
            
            // Start the email activity.
            startActivity(emailIntent);
        }
        catch (IOException e) {
            Log.w("Ping", e.getLocalizedMessage());
            showToastMessage("Problem sending email.");
        }
        catch (ActivityNotFoundException e) {
            Log.w("Ping", e.getLocalizedMessage());
            showToastMessage("Could not find app to send email.");
        }
        catch (IllegalArgumentException e) {
            Log.w("Ping", e.getLocalizedMessage());
            showToastMessage("Could not attach file to email.");
        }
    }

    /**
     * Show a "toast" message.
     * @param message The message.
     */
    protected void showToastMessage(String message) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    private void onButtonExportHistoryClick(View view) {
        exportHistory();
    }

    /**
     * Export the location history.
     */
    private void exportHistory() {
        // Read from database.
        try {
            StringBuilder builder = new StringBuilder();

            // Add header row.
            String header = "Time,Phone number,Latitude,Longitude,Address,Contact name";
            builder.append(header).append("\n");

            // Add body rows.
            Cursor cursor = dbHelper.getLocationHistory();
            while (cursor.moveToNext()) {
                String time = cursor.getString(
                        cursor.getColumnIndexOrThrow(PingDatabaseContract.LocationHistoryEntry.COLUMN_NAME_TIME));
                String phoneNumber = cursor.getString(
                        cursor.getColumnIndexOrThrow(PingDatabaseContract.LocationHistoryEntry.COLUMN_NAME_PHONE_NUMBER));
                double latitude = cursor.getDouble(
                        cursor.getColumnIndexOrThrow(PingDatabaseContract.LocationHistoryEntry.COLUMN_NAME_LATITUDE));
                double longitude = cursor.getDouble(
                        cursor.getColumnIndexOrThrow(PingDatabaseContract.LocationHistoryEntry.COLUMN_NAME_LONGITUDE));
                String address = cursor.getString(
                        cursor.getColumnIndexOrThrow(PingDatabaseContract.LocationHistoryEntry.COLUMN_NAME_ADDRESS));
                String contactName = cursor.getString(
                        cursor.getColumnIndexOrThrow(PingDatabaseContract.LocationHistoryEntry.COLUMN_NAME_CONTACT_NAME));

                // Escape commas in address.
                if (!address.isEmpty()) {
                    address = "\"" + address + "\"";
                }

                // Write row.
                String row = time + "," + phoneNumber + "," + latitude + "," + longitude + "," + address + "," + contactName;
                builder.append(row).append("\n");
            }

            // Close the cursor.
            cursor.close();

            // Write to output file.
            writeToFile(builder);

            // Show message.
            showToastMessage("File exported.");
        }
        catch (IllegalArgumentException e) {
            Log.w("Ping", e.getLocalizedMessage());
            showToastMessage("Problem exporting file.");
        }
    }

    /**
     * Write the contents of the builder to the file.
     * @param builder The builder.
     */
    private void writeToFile(StringBuilder builder) {
        try {
            // Get the path.
            String filePath = getExportedTextFilePath();

            // Create the file.
            File file = new File(filePath);
            if (!file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            }

            // Write to the file.
            FileWriter writer = new FileWriter(file);
            writer.write(builder.toString());
            writer.close();
        } catch (IOException e) {
            Log.w("Ping", e.getLocalizedMessage());
        }
    }

    /**
     * Get path to output file.
     * @return The path to the output file.
     */
    private String getExportedTextFilePath() throws IOException {
        File dir = getExportFileDir();
        String fileName = "export.csv";

        return dir + File.separator + fileName;
    }

    /**
     * Get the directory for export files.
     * @return The directory.
     */
    private File getExportFileDir() {
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File exportsDir = new File(documentsDir + File.separator + "ping-exports");

        if (!exportsDir.exists()) {
            Log.i("Ping", "Creating export directory");
            boolean result = exportsDir.mkdirs();

            if (!result) {
                Log.w("Ping", "Could not create directory.");
                exportsDir = documentsDir;
            }
        }

        return exportsDir;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}