package com.example.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;
import java.util.UUID;

import static com.example.blehid.core.HidMediaConstants.*;

/**
 * Handles the creation and management of HID media control reports.
 * This class encapsulates the logic for generating and sending media control reports.
 */
public class HidMediaReportHandler {
    private static final String TAG = "HidMediaReportHandler";
    
    // Combined report: 12 bytes
    // Byte 0: Media buttons
    // Byte 1: Mouse buttons
    // Byte 2: X movement
    // Byte 3: Y movement
    // Byte 4: Keyboard modifiers (CTRL, SHIFT, ALT, etc.)
    // Byte 5: Reserved (always 0)
    // Bytes 6-11: Keyboard keys (up to 6 keys)
    private final byte[] combinedReport = new byte[12];
    
    private final BleGattServerManager gattServerManager;
    private final BluetoothGattCharacteristic reportCharacteristic;
    
    private boolean notificationsEnabled = false;
    
    /**
     * Creates a new HID Media Report Handler.
     *
     * @param gattServerManager     The GATT server manager
     * @param reportCharacteristic  The report characteristic
     */
    public HidMediaReportHandler(
            BleGattServerManager gattServerManager,
            BluetoothGattCharacteristic reportCharacteristic) {
        this.gattServerManager = gattServerManager;
        this.reportCharacteristic = reportCharacteristic;
    }
    
    /**
     * Sends a full combined report with media, mouse, and keyboard data.
     * 
     * @param device The connected Bluetooth device
     * @param mediaButtons Media button state bitmap (see HidMediaConstants)
     * @param mouseButtons Mouse button state bitmap (see HidMediaConstants)
     * @param x X movement (-127 to 127)
     * @param y Y movement (-127 to 127)
     * @param modifiers Keyboard modifiers (CTRL, SHIFT, ALT, etc.)
     * @param keys Array of keyboard key codes (up to 6 keys)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendFullReport(BluetoothDevice device, int mediaButtons, 
                                 int mouseButtons, int x, int y,
                                 int modifiers, byte[] keys) {
        Log.d(TAG, "sendFullReport - mediaButtons: " + mediaButtons 
                + ", mouseButtons: " + mouseButtons + ", x: " + x + ", y: " + y
                + ", modifiers: " + modifiers);
        
        if (device == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        // Simple check to ensure notifications are enabled
        if (!notificationsEnabled) {
            Log.d(TAG, "Ensuring notifications are enabled");
            enableNotifications();
            notificationsEnabled = true;
        }
        
        // Clamp values to byte range
        x = Math.max(-127, Math.min(127, x));
        y = Math.max(-127, Math.min(127, y));
        
        // Update report data
        combinedReport[0] = (byte) (mediaButtons & 0x3F);  // Media buttons (6 bits)
        combinedReport[1] = (byte) (mouseButtons & 0x07);  // Mouse buttons (3 bits)
        combinedReport[2] = (byte) x;                      // X movement
        combinedReport[3] = (byte) y;                      // Y movement
        combinedReport[4] = (byte) (modifiers & 0xFF);     // Keyboard modifiers
        combinedReport[5] = 0;                             // Reserved byte
        
        // Set up to 6 keyboard keys
        int keyCount = (keys != null) ? Math.min(keys.length, 6) : 0;
        for (int i = 0; i < 6; i++) {
            combinedReport[i + 6] = (i < keyCount) ? keys[i] : 0;
        }
        
        // Log detailed report information
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            StringBuilder keyStr = new StringBuilder();
            for (int i = 0; i < keyCount; i++) {
                keyStr.append(String.format("0x%02X ", keys[i]));
            }
            
            Log.d(TAG, String.format("FULL REPORT DATA - mediaButtons=%d, mouseButtons=%d, x=%d, y=%d, modifiers=0x%02X, keys=[%s]",
                    mediaButtons, mouseButtons, x, y, modifiers, keyStr.toString().trim()));
        }
        
        // Send notification with retry for more reliability
        boolean success = sendNotificationWithRetry(reportCharacteristic.getUuid(), combinedReport);
        
        if (success) {
            Log.d(TAG, "Combined report sent successfully");
        } else {
            Log.e(TAG, "Failed to send combined report after retries");
        }
        
        return success;
    }
    
    /**
     * Sends a combined media and mouse report (backward compatible).
     * 
     * @param device  The connected Bluetooth device
     * @param mediaButtons Media button state bitmap (see HidMediaConstants)
     * @param mouseButtons Mouse button state bitmap (see HidMediaConstants)
     * @param x X movement (-127 to 127)
     * @param y Y movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendCombinedReport(BluetoothDevice device, int mediaButtons, 
                                     int mouseButtons, int x, int y) {
        // Keep existing keyboard values
        return sendFullReport(device, mediaButtons, mouseButtons, x, y, 
                            combinedReport[4], extractKeyboardKeys());
    }
    
    /**
     * Sends only a media control report without changing mouse or keyboard state.
     * 
     * @param device  The connected Bluetooth device
     * @param buttons Media button state bitmap (see HidMediaConstants)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendMediaReport(BluetoothDevice device, int buttons) {
        // Keep existing mouse and keyboard values
        return sendFullReport(device, buttons, combinedReport[1], 0, 0, 
                            combinedReport[4], extractKeyboardKeys());
    }
    
    /**
     * Sends only a mouse movement report without changing media or keyboard state.
     * 
     * @param device The connected Bluetooth device
     * @param x X movement (-127 to 127)
     * @param y Y movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean movePointer(BluetoothDevice device, int x, int y) {
        // Keep existing media, mouse button, and keyboard values
        return sendFullReport(device, combinedReport[0], combinedReport[1], x, y, 
                            combinedReport[4], extractKeyboardKeys());
    }
    
    /**
     * Sends only a mouse button report without changing media or keyboard state.
     * 
     * @param device The connected Bluetooth device
     * @param buttons Mouse button state bitmap (see HidMediaConstants)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendMouseButtons(BluetoothDevice device, int buttons) {
        // Keep existing media and keyboard values, set x and y to 0
        return sendFullReport(device, combinedReport[0], buttons, 0, 0, 
                            combinedReport[4], extractKeyboardKeys());
    }
    
    /**
     * Sends only a keyboard report without changing media or mouse state.
     * 
     * @param device The connected Bluetooth device
     * @param modifiers Modifier keys (shift, ctrl, alt, etc.)
     * @param keyCodes Array of key codes to send (up to 6)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeyboardReport(BluetoothDevice device, int modifiers, byte[] keyCodes) {
        // Keep existing media and mouse values
        return sendFullReport(device, combinedReport[0], combinedReport[1], 0, 0, 
                            modifiers, keyCodes);
    }
    
    /**
     * Sends a single key press (with optional modifiers).
     * 
     * @param device The connected Bluetooth device
     * @param keyCode The key code to send
     * @param modifiers Optional modifier keys (shift, ctrl, alt, etc.)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKey(BluetoothDevice device, byte keyCode, int modifiers) {
        byte[] keys = new byte[1];
        keys[0] = keyCode;
        return sendKeyboardReport(device, modifiers, keys);
    }
    
    /**
     * Releases all keyboard keys.
     * 
     * @param device The connected Bluetooth device
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean releaseKeys(BluetoothDevice device) {
        // Send a report with no keys pressed, keeping media and mouse state
        return sendKeyboardReport(device, 0, null);
    }
    
    /**
     * Helper method to send notification with retry.
     */
    private boolean sendNotificationWithRetry(UUID charUuid, byte[] value) {
        boolean success = false;
        for (int retry = 0; retry < 2 && !success; retry++) {
            success = gattServerManager.sendNotification(charUuid, value);
            
            if (!success && retry == 0) {
                Log.w(TAG, "First notification attempt failed, retrying after delay");
                try {
                    Thread.sleep(10); // Small delay before retry
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
        return success;
    }
    
    /**
     * Sends a button press and release for a media control action.
     * 
     * @param device The connected Bluetooth device
     * @param button The button to press and release (see HidMediaConstants button constants)
     * @return true if both press and release were successful, false otherwise
     */
    public boolean sendMediaControlAction(BluetoothDevice device, int button) {
        boolean pressResult = sendMediaReport(device, button);
        
        try {
            Thread.sleep(100); // Delay between press and release
        } catch (InterruptedException e) {
            // Ignore
        }
        
        boolean releaseResult = sendMediaReport(device, 0);
        return pressResult && releaseResult;
    }
    
    /**
     * Performs a mouse button click.
     * 
     * @param device The connected Bluetooth device
     * @param button The button to click (see HidMediaConstants mouse button constants)
     * @return true if both press and release were successful, false otherwise
     */
    public boolean click(BluetoothDevice device, int button) {
        boolean pressResult = sendMouseButtons(device, button);
        
        try {
            Thread.sleep(50); // Short delay between press and release
        } catch (InterruptedException e) {
            // Ignore
        }
        
        boolean releaseResult = sendMouseButtons(device, 0);
        return pressResult && releaseResult;
    }
    
    /**
     * Sends a key press and release.
     * 
     * @param device The connected Bluetooth device
     * @param keyCode The key code to press and release
     * @param modifiers Optional modifier keys (shift, ctrl, alt, etc.)
     * @return true if both the press and release were successful, false otherwise
     */
    public boolean typeKey(BluetoothDevice device, byte keyCode, int modifiers) {
        // Press the key
        boolean pressResult = sendKey(device, keyCode, modifiers);
        
        try {
            Thread.sleep(50); // Small delay between press and release
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // Release the key
        boolean releaseResult = releaseKeys(device);
        
        return pressResult && releaseResult;
    }
    
    /**
     * Helper method to extract the keyboard keys from the combined report.
     * 
     * @return Array of key codes from the combined report
     */
    private byte[] extractKeyboardKeys() {
        byte[] keys = new byte[6];
        for (int i = 0; i < 6; i++) {
            keys[i] = combinedReport[i + 6];
        }
        return keys;
    }
    
    /**
     * Enables notifications for the report characteristic.
     */
    private void enableNotifications() {
        BluetoothGattDescriptor descriptor = reportCharacteristic.getDescriptor(CLIENT_CONFIG_UUID);
        if (descriptor != null) {
            // Enable notifications (0x01, 0x00)
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.d(TAG, "Set report characteristic descriptor value to enable notifications");
            
            // Send a zero report to initialize the connection
            for (int i = 0; i < combinedReport.length; i++) {
                combinedReport[i] = 0; // Initialize all bytes to 0
            }
            reportCharacteristic.setValue(combinedReport);
            
            // Send two initial reports - one is sometimes not enough
            try {
                gattServerManager.sendNotification(reportCharacteristic.getUuid(), combinedReport);
                Thread.sleep(20); // Small delay
                gattServerManager.sendNotification(reportCharacteristic.getUuid(), combinedReport);
            } catch (Exception e) {
                Log.e(TAG, "Error sending initial report", e);
            }
        } else {
            Log.e(TAG, "Missing Client Configuration Descriptor (CCCD) for report characteristic");
        }
    }
    
    /**
     * Called when notifications are enabled or disabled for a characteristic.
     * 
     * @param characteristicUuid The UUID of the characteristic
     * @param enabled            Whether notifications are enabled or disabled
     */
    public void setNotificationsEnabled(UUID characteristicUuid, boolean enabled) {
        if (characteristicUuid.equals(HID_REPORT_UUID)) {
            this.notificationsEnabled = enabled;
        }
    }
    
    /**
     * Gets the last combined report.
     * 
     * @return The last combined report
     */
    public byte[] getMediaReport() {
        return combinedReport;
    }
}
