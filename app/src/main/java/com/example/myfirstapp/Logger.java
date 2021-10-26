package com.example.myfirstapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class Logger {
    private final static LogConfigurator mLogConfigrator = new LogConfigurator();
    private static boolean m_setupOK = false;

    static {
        try {
            configureLog4j();
            m_setupOK = true;
        }
        catch (IOException e) {
            Log.w("Logger", e.getLocalizedMessage());
        }
    }

    // Return the directory for storing log files.
    public static File getLogFileDir() throws IOException {
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File logDir = new File(documentsDir + File.separator + "log-files");

        if (!logDir.exists()) {
            Log.i("Log Files", "Creating logs directory");
            boolean result = logDir.mkdir();

            if (result == false) {
                Log.w("Log Files", "Could not create directory.");
                logDir = documentsDir;
            }
        }

        return logDir;
    }

    private static void configureLog4j() throws IOException {
//        String fileName = Environment.getExternalStorageDirectory() + "/" + "log4j.log";
        String fileName = getLogFileDir() + "/" + "log4j.txt";
        String filePattern = "%d - [%c] - %p : %m%n";
        int maxBackupSize = 10;
        long maxFileSize = 1024 * 1024;

        configure(fileName, filePattern, maxBackupSize, maxFileSize);
    }

    private static void configure(String fileName, String filePattern, int maxBackupSize, long maxFileSize) {
        mLogConfigrator.setFileName(fileName);
        mLogConfigrator.setMaxFileSize(maxFileSize);
        mLogConfigrator.setFilePattern(filePattern);
        mLogConfigrator.setMaxBackupSize(maxBackupSize);
        mLogConfigrator.setUseLogCatAppender(true);
        mLogConfigrator.configure();
    }

    public static void info(Context context, String message) {

        // Check if enabled in settings.
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        boolean enabled = sharedPreferences.getBoolean("enable_logging", false);

        // Log if all OK.
        if (m_setupOK && enabled) {
            try {
                Log.i("Logging", "Writing log message.");
                getLogger("Ping").info(message);
            }
            catch (Exception e) {
                Log.w("Logging", e.getLocalizedMessage());
            }
        }
    }

    public static org.apache.log4j.Logger getLogger(String name) {
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(name);
        return logger;
    }
}
