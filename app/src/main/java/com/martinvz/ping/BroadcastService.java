package com.martinvz.ping;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The timer service.
 */
public class BroadcastService extends Service {

    // Intent ID.
    public static final String COUNTDOWN_BR = "com.martinvz.ping.timer.service";

    // Intent input fields.
    static final String EXTRA_INTERVAL = "EXTRA_INTERVAL";
    static final String EXTRA_INTERVAL_BEFORE_START = "EXTRA_INTERVAL_BEFORE_START";
    static final String EXTRA_END_TIME = "EXTRA_END_TIME";
    static final String EXTRA_CONTACT_NUMBER = "EXTRA_CONTACT_NUMBER";
    static final String EXTRA_START_TIME_SPECIFIED = "EXTRA_START_TIME_SPECIFIED";


    // Intent output fields.
    static final String EXTRA_OUTPUT_FINISHED = "EXTRA_OUTPUT_FINISHED";

    // Intent to broadcast.
    private final Intent bi = new Intent(COUNTDOWN_BR);
    private final Intent mIntentFinished = new Intent(COUNTDOWN_BR);

    // Timer.
    private CountDownTimer cdt = null;
    private final Timer mEndTimer = new Timer();
    private long mInterval = 0;

    // Service ID.
    private static final int FOREGROUND_SERVICE_ID = 2001;

    // Minimum time interval.
    static final long MINIMUM_INTERVAL = 5 * 1000;

    // Number to text.
    private String m_contactNumber;

    // Hack to let activity know about status.
    private static boolean m_isRunning = false;
    private static BroadcastService m_instance = null;
    private Date m_previousPingTime = null;
    private boolean m_startTimeSpecified = false;
    private boolean m_endTimeSpecified = false;

    /**
     * Check if the service is running.
     * @return True if the service is running.
     */
    public static boolean isRunning() {
        return m_isRunning;
    }

    /**
     * Get the current instance of the service.
     * @return The current instance of the service.
     */
    public static BroadcastService getInstance() {
        return m_instance;
    }

    /**
     * Get the contact name.
     * @return The contact name.
     */
    public String getContactName() {
        return MainActivity.getContactName(m_contactNumber, this);
    }

    /**
     * Get the contact number.
     * @return The contact number.
     */
    public String getContactNumber() {
        return m_contactNumber;
    }

    @Override
    public void onCreate() {
        // Call parent method.
        super.onCreate();

        // Set up intent.
        mIntentFinished.putExtra(EXTRA_OUTPUT_FINISHED, true);
    }

    /**
     * Start the timer.
     * @param millisInFuture The interval before the timer finishes, in milliseconds.
     */
    private void startTimer(long millisInFuture) {
        // Cancel existing timer.
        if (cdt != null) {
            cdt.cancel();
        }

        // Create timer.
        cdt = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Send a broadcast.
                bi.putExtra("countdown", millisUntilFinished);
                sendBroadcast(bi);
            }

            @Override
            public void onFinish() {
                // Send the broadcast.
                broadcastTimerFinish();

                // Restart the timer.
                cdt.start();
            }
        };

        // Start timer.
        cdt.start();
    }

    @Override
    public void onDestroy() {
        // Set flag.
        m_isRunning = false;

        // Cancel the timer.
        cdt.cancel();
        mEndTimer.cancel();

        // Send message.
        sendBroadcast(mIntentFinished);

        // Call parent method.
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get intent fields.
        long intervalBeforeStart = intent.getLongExtra(EXTRA_INTERVAL_BEFORE_START, 0);
        long interval = intent.getLongExtra(EXTRA_INTERVAL, 60 * 1000);
        long endTimeStamp = intent.getLongExtra(EXTRA_END_TIME, 0);
        m_contactNumber = intent.getStringExtra(EXTRA_CONTACT_NUMBER);
        m_startTimeSpecified = intent.getBooleanExtra(EXTRA_START_TIME_SPECIFIED, false);
        m_endTimeSpecified = intent.hasExtra(EXTRA_END_TIME);

        // Ensure interval is sensible.
        if (interval < MINIMUM_INTERVAL) {
            Log.w(getLogName(), "Interval too low (" + interval + "). Setting to " + MINIMUM_INTERVAL);
            interval = MINIMUM_INTERVAL;
        }

        // Start the timer.
        if (intervalBeforeStart > 0) {
            // Delayed start.
            startTimerDelayed(intervalBeforeStart, interval);
        }
        else {
            // Immediate start.
            broadcastTimerFinish(); // Send first text message immediately.
            startTimer(interval);
        }

        // Handle the end time.
        if (endTimeStamp > 0) {
            TimerTask endTask = new TimerTask() {
                @Override
                public void run() {
                    onEndTimerTask();
                }
            };

            Date endTime = new Date();
            endTime.setTime(endTimeStamp);
            mEndTimer.schedule(endTask, endTime);
        }

        // Run the service in the foreground, using a "sticky" notification.
        Notification notification = new NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
                .setContentTitle("Ping Timer")
                .setContentText("Sending scheduled ping requests.")
                .setSmallIcon(R.drawable.message_icon)
                .build();
        startForeground(FOREGROUND_SERVICE_ID, notification);

        // Set flag.
        m_isRunning = true;
        m_instance = this;

        // Store interval.
        mInterval = interval;

        // Call parent method.
        return super.onStartCommand(intent, flags, startId);
    }

    private void onEndTimerTask() {
        Log.i(getLogName(), "End task run.");
        stopSelf();
    }

    /**
     * Get name for logging.
     * @return The name for logging.
     */
    @SuppressWarnings("SameReturnValue")
    private String getLogName() {
        return "TimerTest";
    }

    /**
     * Start timer after the given initial delay.
     * @param delayBeforeStart The delay before starting the timer.
     * @param interval The timer interval.
     */

    private void startTimerDelayed(long delayBeforeStart, long interval) {
        // Cancel existing timer.
        if (cdt != null) {
            cdt.cancel();
        }

        // Create timer.
        cdt = new CountDownTimer(delayBeforeStart, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Send a broadcast.
                bi.putExtra("countdown", millisUntilFinished);
                sendBroadcast(bi);
            }

            @Override
            public void onFinish() {
                // Send the broadcast.
                broadcastTimerFinish();

                // Start timer with new interval.
                startTimer(interval);
            }
        };

        // Start timer.
        cdt.start();
    }

    /**
     * Broadcast a message that the timer has finished.
     */
    private void broadcastTimerFinish() {
        // Send request.
        if (m_contactNumber.isEmpty()) {
            Log.w(getLogName(), "Contact not set.");
            return;
        }

        // Send text message.
        sendPingRequest(m_contactNumber);

        // Broadcast update.
        bi.putExtra("countdown", 0L);
        sendBroadcast(bi);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Send a ping request.
     * @param number The phone number.
     */
    private void sendPingRequest(String number) {
        try {
            SmsManager manager = SmsManager.getDefault();
            manager.sendTextMessage(number, null, MainActivity.PING_REQUEST_TEXT, null, null);
            m_previousPingTime = new Date();
        }
        catch (SecurityException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    /**
     * Get the interval between pings.
     * @return The interval in milliseconds.
     */
    public long getInterval() {
        return mInterval;
    }

    /**
     * Get the previous ping time.
     * @return The previous ping time.
     */
    public Date getLastPingTime() {
        return m_previousPingTime;
    }

    /**
     * Check if the start time was specified.
     * @return True if start time was specified.
     */
    public boolean getStartTimeSpecified() {
        return m_startTimeSpecified;
    }

    /**
     * Check if the end time was specified.
     * @return True if end time was specified.
     */
    public boolean getEndTimeSpecified() {
        return m_endTimeSpecified;
    }
}
