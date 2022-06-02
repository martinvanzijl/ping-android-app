package com.example.myfirstapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ScheduledPingActivity extends AppCompatActivity {

    // Request codes.
    private static final int REQUEST_CODE_START = 1000;
    private static final int REQUEST_CODE_CHOOSE_CONTACT = 1001;
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat SHORT_TIMESTAMP_FORMAT =
            new SimpleDateFormat("hh:mm aa");

    // Fields.
    private ActivityResultLauncher<Intent> chooseContactActivity = null;
    private String m_contactNumber = "";
    private final BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateGUI(intent);
        }
    };

    // Hack to store start/end date settings.
    private static Date m_lastStartTime = null;
    private static Date m_lastEndTime = null;

    /**
     * Convenience method to create a calendar set to given time.
     * @param time The time.
     * @return The calendar.
     */
    private Calendar calendarFromTime(Date time) {
        // Safety check.
        if (time == null) {
            Log.w(getLogName(), "Cannot convert null time to calendar.");
            return null;
        }

        // Convert.
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        return calendar;
    }

    /**
     * Update the GUI from the service.
     * @param intent The intent from the service.
     */
    private void updateGUI(Intent intent) {
        // Update label.
        if (intent.getExtras() != null) {
            if (intent.getBooleanExtra(BroadcastService.EXTRA_OUTPUT_FINISHED, false)) {
                // Stopped.
                TextView textView = findViewById(R.id.textViewRunning);
                textView.setText(R.string.label_stopped);
            }
            else {
                // Interval update.
                long millisUntilFinished = intent.getLongExtra("countdown", -1);
                updateCountdownLabel(millisUntilFinished);

                // Send text message if required.
                if (millisUntilFinished == 0) {
                    onTimerTask();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scheduled_ping);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        findViewById(R.id.buttonScheduleChooseContact).setOnClickListener(this::onButtonChooseContactClick);
        findViewById(R.id.buttonScheduleSave).setOnClickListener(this::onButtonSaveClick);
        findViewById(R.id.buttonScheduleStop).setOnClickListener(this::onButtonStopClick);

        // Set scheduled start to now.
        Calendar calendar = Calendar.getInstance();
        setStartTime(calendar);

        // Set default end time to 1 hour from now.
        calendar.add(Calendar.HOUR, 1);
        setEndTime(calendar);

        // Set default interval.
        Spinner spinnerIntervalMinutes = findViewById(R.id.spinnerScheduleMinutes);
        spinnerIntervalMinutes.setSelection(5); // Every 5 minutes.

        // Create result handler.
        chooseContactActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        Intent data = result.getData();
                        assert data != null;
                        setContact(data);
                    }
                }
        );

        // Check if service is already running.
        if (BroadcastService.isRunning()) {
            // Contact.
            m_contactNumber = BroadcastService.getInstance().getContactNumber();
            String contactName = BroadcastService.getInstance().getContactName();
            updateContactLabel(contactName);

            // Interval.
            long interval = BroadcastService.getInstance().getInterval();
            updateIntervalSettings(interval);

            // From/To settings.
            SwitchCompat switchFrom = findViewById(R.id.switchScheduleFrom);
            switchFrom.setChecked(BroadcastService.getInstance().getStartTimeSpecified());

            setStartTime(calendarFromTime(m_lastStartTime));

            SwitchCompat switchUntil = findViewById(R.id.switchScheduleUntil);
            switchUntil.setChecked(BroadcastService.getInstance().getEndTimeSpecified());

            setEndTime(calendarFromTime(m_lastEndTime));

            // Labels at bottom.
            TextView view = findViewById(R.id.textViewStatus);
            Date date = BroadcastService.getInstance().getLastPingTime();
            if (date == null) {
                view.setText(getString(R.string.label_running_but_no_ping_sent_yet));
            }
            else {
                String timeStamp = getShortTimestamp(date);
                view.setText(getString(R.string.label_ping_sent, timeStamp));
            }
        }
    }

    /**
     * Set start time widgets to given calendar time.
     * @param calendar The time.
     */
    private void setStartTime(Calendar calendar) {
        setTimeFields(
                R.id.spinnerScheduleStartHour,
                R.id.spinnerScheduleStartMinute,
                R.id.spinnerScheduleStartAM,
                calendar);
    }

    /**
     * Set end time widgets to given calendar time.
     * @param calendar The time.
     */
    private void setEndTime(Calendar calendar) {
        setTimeFields(
                R.id.spinnerScheduleEndHour,
                R.id.spinnerScheduleEndMinute,
                R.id.spinnerScheduleEndAM,
                calendar);
    }

    /**
     * Set time field widgets to given calendar time.
     * @param hourSpinnerId The ID of the hour spinner.
     * @param minuteSpinnerId The ID of the minute spinner.
     * @param amPmSpinnerId The ID of the AM/PM spinner.
     * @param calendar The time.
     */
    private void setTimeFields(int hourSpinnerId, int minuteSpinnerId, int amPmSpinnerId,
                               Calendar calendar) {
        if (calendar == null) {
            Log.w(getLogName(), "Cannot set time fields for null date.");
            return;
        }

        Spinner spinnerHour = findViewById(hourSpinnerId);
        Spinner spinnerMinute = findViewById(minuteSpinnerId);
        Spinner spinnerAM = findViewById(amPmSpinnerId);

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        spinnerHour.setSelection(getHourIn12HourTime(hour) - 1);
        spinnerAM.setSelection(hour < 12 ? 0 : 1);
        spinnerMinute.setSelection(minute);
    }

    /**
     * Update the interval widgets to reflect the given interval.
     * @param intervalInMS The interval in milliseconds.
     */
    private void updateIntervalSettings(long intervalInMS) {
        // Calculate.
        long intervalInSeconds = intervalInMS / 1000;
        long hours = intervalInSeconds / 3600;
        long secondsLeft = intervalInSeconds - hours * 3600;
        long minutes = secondsLeft / 60;
        long seconds = secondsLeft - minutes * 60;

        // Update widgets.
        Spinner spinnerHours = findViewById(R.id.spinnerScheduleHours);
        spinnerHours.setSelection((int) hours);

        Spinner spinnerMinutes = findViewById(R.id.spinnerScheduleMinutes);
        spinnerMinutes.setSelection((int) minutes);

        Spinner spinnerSeconds = findViewById(R.id.spinnerScheduleSeconds);
        spinnerSeconds.setSelection((int) seconds);
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
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


    /**
     * Update the label for the countdown timer.
     * @param millisecondsLeft The number of milliseconds left.
     */
    private void updateCountdownLabel(long millisecondsLeft) {
        // Calculate the time left.
        int secondsLeft = (int) Math.ceil(millisecondsLeft / 1000.0);

        // Format the time.
        String timeLeft = formatSeconds(secondsLeft);

        // Update the label.
        TextView textView = findViewById(R.id.textViewRunning);
        textView.setText(getString(R.string.label_time_left, timeLeft));
    }

    /**
     * Convert the number of seconds to format "HH:MM:SS".
     * From https://stackoverflow.com/questions/22545644/how-to-convert-seconds-into-hhmmss
     * @param timeInSeconds The number of seconds.
     * @return The time in "HH:MM:SS" format.
     */
    private static String formatSeconds(int timeInSeconds)
    {
        int hours = timeInSeconds / 3600;
        int secondsLeft = timeInSeconds - hours * 3600;
        int minutes = secondsLeft / 60;
        int seconds = secondsLeft - minutes * 60;

        String formattedTime = "";
        if (hours < 10)
            formattedTime += "0";
        formattedTime += hours + ":";

        if (minutes < 10)
            formattedTime += "0";
        formattedTime += minutes + ":";

        if (seconds < 10)
            formattedTime += "0";
        formattedTime += seconds ;

        return formattedTime;
    }

    /**
     * Set the contact to send Ping requests to.
     * @param data The intent data.
     */
    private void setContact(Intent data) {

        // Read contact details from database.
        Uri uri = data.getData();
        if (uri != null) {
            Cursor cursor = null;
            try {
                Uri contentUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                String id = uri.getLastPathSegment();
                String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?";
                String[] selectionArgs = new String[] {id};
                cursor = getContentResolver().query(contentUri, null, selection, selectionArgs, null);

                if (cursor != null && cursor.moveToFirst()) {
                    // Store number.
                    String columnName = ContactsContract.CommonDataKinds.Phone.NUMBER;
                    int columnIndex = cursor.getColumnIndex(columnName);
                    m_contactNumber = cursor.getString(columnIndex);

                    // Update label with name.
                    columnName = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME;
                    columnIndex = cursor.getColumnIndex(columnName);
                    String name = cursor.getString(columnIndex);

                    updateContactLabel(name);
                }
            }
            catch (SQLiteException | SecurityException | IllegalArgumentException e) {
                Log.w("Exception", e.getLocalizedMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    /**
     * Update the contact label.
     * @param contactName The chosen contact's name.
     */
    private void updateContactLabel(String contactName) {
        TextView label = findViewById(R.id.textViewScheduleSendTo);
        label.setText(getString(R.string.label_send_ping_to, contactName));
    }

    private void onButtonChooseContactClick(View view) {
        // Declare required permissions.
        String[] requiredPermissions = new String[] {
                Manifest.permission.READ_CONTACTS,
        };

        // Choose contact if permission is present.
        if (checkForPermissions(requiredPermissions, REQUEST_CODE_CHOOSE_CONTACT)) {
            chooseFilterContact();
        }
    }

    /**
     * Choose contact to send scheduled Ping request to.
     */
    private void chooseFilterContact() {
        // Create intent.
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);

        // Start activity.
        chooseContactActivity.launch(contactPickerIntent);
    }

    private int getHourIn12HourTime(int hour) {
        if (hour == 0) {
            // Midnight (12 AM)
            return 12;
        }
        else if (hour <= 12) {
            // 1 AM to 12 noon.
            return hour;
        }
        else {
            // 1 PM to 11 PM.
            return hour % 12;
        }
    }

    public void onButtonSaveClick(View view) {
        // Check for required permissions.
        String[] requiredPermissions = new String[] {
                Manifest.permission.SEND_SMS
        };

        // Start the service if these are granted.
        if (checkForPermissions(requiredPermissions, REQUEST_CODE_START)) {
            startSchedule();
        }
    }

    // Return true if the app has the given permission.
    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(getApplicationContext(), permission) ==
                PackageManager.PERMISSION_GRANTED;
    }

    // Check that the required permissions are granted.
    // If not, ask for permission with the given request code.
    private boolean checkForPermissions(String[] permissions, int requestCode) {

        boolean mustAsk = false;
        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                mustAsk = true;
                break;
            }
        }

        if (!mustAsk) {
            return true;
        }

        if (shouldShowRequestPermissionRationale()) {
            System.out.println("Permission to send text messages is required.");
        } else {
            ActivityCompat.requestPermissions(this, permissions, requestCode);
        }

        return false;
    }

    /**
     * Check if rationale should be shown.
     * @return True if rationale should be shown.
     */
    private boolean shouldShowRequestPermissionRationale() {
        return false;
    }

    /**
     * Start sending scheduled Ping requests.
     */
    public void startSchedule() {

        // Check that contact is set.
        if (m_contactNumber.isEmpty()) {
            showToastMessage("Please choose a contact first.");
            return;
        }

        // Create intent.
        Intent intent = new Intent(this, BroadcastService.class);

        // Get selected hours interval.
        Spinner spinner = findViewById(R.id.spinnerScheduleHours);
        String text = spinner.getSelectedItem().toString();
        long intervalInHours = Long.parseLong(text);
        long intervalInMinutes = intervalInHours * 60;
        long intervalInSeconds = intervalInMinutes * 60;
        long intervalInMS = intervalInSeconds * 1000;

        // Get selected minute interval.
        spinner = findViewById(R.id.spinnerScheduleMinutes);
        text = spinner.getSelectedItem().toString();
        intervalInMinutes = Long.parseLong(text);
        intervalInSeconds = intervalInMinutes * 60;
        intervalInMS += intervalInSeconds * 1000;

        // Set selected seconds interval.
        spinner = findViewById(R.id.spinnerScheduleSeconds);
        text = spinner.getSelectedItem().toString();
        intervalInSeconds = Long.parseLong(text);
        intervalInMS += intervalInSeconds * 1000;

        // Get the selected start time.
        Date startTime = getStartTime();
        Calendar calendar = Calendar.getInstance();
        long now = calendar.getTimeInMillis();
        calendar.setTime(startTime);

        long intervalBeforeStart = 0;

        SwitchCompat switchFrom = findViewById(R.id.switchScheduleFrom);
        if (switchFrom.isChecked()) {
            long start = calendar.getTimeInMillis();
            intervalBeforeStart = start - now;

            if (intervalBeforeStart <= 1000) {
                // Start immediately.
                intervalBeforeStart = 0;
            }
        }

        // Ensure interval is sensible.
        if (intervalInMS < BroadcastService.MINIMUM_INTERVAL) {
            intervalInMS = BroadcastService.MINIMUM_INTERVAL;
            long intervalSeconds = intervalInMS / 1000;
            showToastMessage("Using " + intervalSeconds + " seconds for interval.");
        }

        // Update intent.
        intent.putExtra(BroadcastService.EXTRA_INTERVAL_BEFORE_START, intervalBeforeStart);
        intent.putExtra(BroadcastService.EXTRA_INTERVAL, intervalInMS);
        intent.putExtra(BroadcastService.EXTRA_CONTACT_NUMBER, m_contactNumber);
        intent.putExtra(BroadcastService.EXTRA_START_TIME_SPECIFIED, switchFrom.isChecked());

        Log.i(getLogName(), "Start time: " + calendar.getTime().toString());
        Log.i(getLogName(), "Interval before start: " + intervalBeforeStart);

        // Get end time.
        Date endTime = getEndTime();
        SwitchCompat switchUntil = findViewById(R.id.switchScheduleUntil);
        if (switchUntil.isChecked()) {
            intent.putExtra(BroadcastService.EXTRA_END_TIME, endTime.getTime());
            Log.i(getLogName(), "Must end at: " + endTime.toString());
        }

        // Update labels.
        TextView textView = findViewById(R.id.textViewRunning);
        textView.setText(R.string.label_running);

        if (intervalBeforeStart > 0) {
            TextView textViewStatus = findViewById(R.id.textViewStatus);
            textViewStatus.setText(getString(R.string.label_running_but_no_ping_sent_yet));
        }

        // Start the service.
        startService(intent);
        Log.i(getLogName(), "Started service");

        // Save the start/end time settings.
        m_lastStartTime = startTime;
        m_lastEndTime = endTime;
    }

    private void stopTimer() {
        Log.i(getLogName(), "Stopping timer.");

//        if (mTimer != null) {
//            mTimer.cancel();
//            mTimer = null;
//        }

        // Stop the service.
        stopService(new Intent(this, BroadcastService.class));

        // Update labels.
        TextView textView = findViewById(R.id.textViewRunning);
        textView.setText(R.string.label_stopped);
    }

    /**
     * Get the end time specified by the widgets.
     * @return The end time.
     */
    private Date getEndTime() {
        return getTimeFromWidgets(
                R.id.spinnerScheduleEndHour,
                R.id.spinnerScheduleEndMinute,
                R.id.spinnerScheduleEndAM);
    }

    /**
     * Get the start time specified by the widgets.
     * @return The start time.
     */
    private Date getStartTime() {
        return getTimeFromWidgets(
                R.id.spinnerScheduleStartHour,
                R.id.spinnerScheduleStartMinute,
                R.id.spinnerScheduleStartAM);
    }

    /**
     * Get the time specified by the given widgets.
     * @param hourSpinnerId The spinner for hour.
     * @param minuteSpinnerId The spinner for minute.
     * @param amPmSpinnerId The spinner for AM/PM.
     * @return The time specified.
     */
    private Date getTimeFromWidgets(int hourSpinnerId, int minuteSpinnerId, int amPmSpinnerId) {
        // Get the selected time.
        Spinner spinnerAM = findViewById(amPmSpinnerId);
        Spinner spinnerMinute = findViewById(minuteSpinnerId);
        Spinner spinnerHour = findViewById(hourSpinnerId);

        Calendar calendar = Calendar.getInstance();

        int amIndex = spinnerAM.getSelectedItemPosition();
        calendar.set(Calendar.AM_PM, amIndex == 0 ? Calendar.AM : Calendar.PM);

        int hour = Integer.parseInt(spinnerHour.getSelectedItem().toString());
        if (hour == 12) hour = 0; // See https://stackoverflow.com/questions/14286600/java-calendar-issues-setting-12pm
        calendar.set(Calendar.HOUR, hour);

        int minute = Integer.parseInt(spinnerMinute.getSelectedItem().toString());
        calendar.set(Calendar.MINUTE, minute);

        calendar.set(Calendar.SECOND, 0);

        return calendar.getTime();
    }

    private void onTimerTask() {

        try {
            // Log.
            Log.i(getLogName(), "Task run.");

            // Update label.
            runOnUiThread(() -> {
                String dateStr = getShortTimestamp();
                TextView view = findViewById(R.id.textViewStatus);
                view.setText(getString(R.string.label_ping_sent, dateStr));
            });

            Logger.appendLog(this, "Timer task finished OK.");
        }
        catch (Exception e) {
            Logger.appendLog(this, e.getLocalizedMessage());
        }
    }

    public void onButtonStopClick(View view) {
        Log.i(getLogName(), "Stop clicked.");

        // Stop existing timer.
        stopTimer();
    }

    private String getLogName() {
        return getApplicationContext().getPackageName();
    }

    // Callback for after the user selects whether to give a required permission.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_START) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted. Continue the action or workflow in your app.
                startSchedule();
            } else {
                System.out.println("Permission to send text messages is required.");
            }
        }
        else if (requestCode == REQUEST_CODE_CHOOSE_CONTACT) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted. Continue the action or workflow in your app.
                chooseFilterContact();
            } else {
                System.out.println("Permission to read contacts is required.");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Create a short timestamp for the current time.
     * @return A short timestamp for the current time.
     */
    private String getShortTimestamp() {
        return getShortTimestamp(new Date());
    }

    /**
     * Create a short timestamp.
     * @return A short timestamp.
     */
    private String getShortTimestamp(Date date) {
        return SHORT_TIMESTAMP_FORMAT.format(date);
    }

    /**
     * Show a "toast" message.
     * @param message The message.
     */
    protected void showToastMessage(@SuppressWarnings("SameParameterValue") String message) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(br, new IntentFilter(BroadcastService.COUNTDOWN_BR));
        Log.i(getLogName(), "Registered broadcast receiver");
    }

//    @Override
//    public void onDestroy() {
//        stopService(new Intent(this, BroadcastService.class));
//        Log.i(getLogName(), "Stopped service");
//        super.onDestroy();
//    }
}

