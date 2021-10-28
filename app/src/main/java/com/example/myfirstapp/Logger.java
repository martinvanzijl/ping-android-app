package com.example.myfirstapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
    // Write a message to the log file.
    public static void appendLog(Context context, String text)
    {
        // Check if enabled in settings.
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        boolean enabled = sharedPreferences.getBoolean("enable_logging", false);

        // Exit if not enabled.
        if (!enabled) {
            return;
        }

        // Write message.
        try {
            // Get the file name.
            File dir = getLogFileDir();
            String fileName = dir + "/log.txt";
            File logFile = new File(fileName);

            // Create log file if it does not exist.
            if (!logFile.exists()) {
                logFile.createNewFile();
            }

            // Write the message.
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            Log.w("Logging", e.getLocalizedMessage());
        }
    }

    // Return the directory for storing log files.
    public static File getLogFileDir() throws IOException {
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File logDir = new File(documentsDir + File.separator + "log-files");

        if (!logDir.exists()) {
            Log.i("Log Files", "Creating logs directory");
            boolean result = logDir.mkdirs();

            if (result == false) {
                Log.w("Log Files", "Could not create directory.");
                logDir = documentsDir;
            }
        }

        return logDir;
    }
}
