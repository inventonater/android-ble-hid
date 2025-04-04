package com.inventonater.blehid;

/**
 * Interface for receiving debug events from the HID implementation.
 * Implement this interface to receive real-time debug information
 * from the HID implementation for diagnostic purposes.
 */
public interface HidDebugListener {
    /**
     * Called when a debug message is available.
     *
     * @param message The debug message
     */
    void onDebugMessage(String message);
}
