package com.example.blehid.app.ui;

import android.text.format.DateFormat;
import android.widget.TextView;

import java.util.Date;

/**
 * Manages the application logging functionality.
 * Handles log entry display, formatting, and trimming.
 */
public class LogManager {
    private static final String TAG = "LogManager";
    private static final int MAX_LOG_LENGTH = 2000;

    private final TextView logTextView;
    private final StringBuilder logEntries = new StringBuilder();

    /**
     * Creates a new LogManager.
     *
     * @param logTextView The TextView to display logs in
     */
    public LogManager(TextView logTextView) {
        this.logTextView = logTextView;
    }

    /**
     * Adds a timestamped entry to the log.
     * 
     * @param entry The log entry text
     */
    public void addLogEntry(String entry) {
        String timestamp = DateFormat.format("HH:mm:ss", new Date()).toString();
        String logEntry = timestamp + " - " + entry + "\n";
        
        logEntries.insert(0, logEntry); // Add to the beginning
        
        // Trim if too long
        if (logEntries.length() > MAX_LOG_LENGTH) {
            logEntries.setLength(MAX_LOG_LENGTH);
        }
        
        logTextView.setText(logEntries.toString());
    }

    /**
     * Clears all log entries.
     */
    public void clearLog() {
        logEntries.setLength(0);
        logTextView.setText("");
    }

    /**
     * Gets the current log contents.
     * 
     * @return String containing all log entries
     */
    public String getLogContents() {
        return logEntries.toString();
    }
}
