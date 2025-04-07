package com.inventonater.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;
import java.util.UUID;

/**
 * Consolidated handler for HID reports.
 * Manages report creation and transmission for media, mouse, and keyboard functionality.
 */
public class HidReportHandler {
    private static final String TAG = "HidReportHandler";
    
    /**
     * Types of HID reports that can be sent.
     */
    public enum ReportType {
        MEDIA,
        MOUSE,
        KEYBOARD,
        COMBINED
    }
    
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
    private final BluetoothGattCharacteristic bootMouseInputReportCharacteristic;
    
    private byte currentProtocolMode = HidConstants.Protocol.MODE_REPORT;
    private boolean notificationsEnabled = false;
    private boolean bootNotificationsEnabled = false;
    
    /**
     * Creates a new HID Report Handler.
     *
     * @param gattServerManager The GATT server manager
     * @param reportCharacteristic The main report characteristic
     * @param bootMouseInputReportChar The boot mouse input report characteristic (optional)
     */
    public HidReportHandler(
            BleGattServerManager gattServerManager,
            BluetoothGattCharacteristic reportCharacteristic,
            BluetoothGattCharacteristic bootMouseInputReportChar) {
        this.gattServerManager = gattServerManager;
        this.reportCharacteristic = reportCharacteristic;
        this.bootMouseInputReportCharacteristic = bootMouseInputReportChar;
    }
    
    /**
     * Creates a new HID Report Handler without boot protocol support.
     *
     * @param gattServerManager The GATT server manager
     * @param reportCharacteristic The report characteristic
     */
    public HidReportHandler(
            BleGattServerManager gattServerManager,
            BluetoothGattCharacteristic reportCharacteristic) {
        this(gattServerManager, reportCharacteristic, null);
    }
    
    // ==================== Media Control Methods ====================
    
    /**
     * Sends a media control report.
     * 
     * @param device The connected Bluetooth device
     * @param buttons Media button state bitmap (see HidConstants.Media)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendMediaReport(BluetoothDevice device, int buttons) {
        // Keep existing mouse and keyboard values
        return sendFullReport(device, buttons, combinedReport[1], 0, 0, 
                            combinedReport[4], extractKeyboardKeys());
    }
    
    /**
     * Sends a button press and release for a media control action.
     * 
     * @param device The connected Bluetooth device
     * @param button The button to press and release (see HidConstants.Media)
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
    
    // ==================== Mouse Control Methods ====================
    
    /**
     * Sends a mouse movement report.
     * 
     * @param device The connected Bluetooth device
     * @param x X movement (-127 to 127)
     * @param y Y movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean movePointer(BluetoothDevice device, int x, int y) {
        Log.d(TAG, "movePointer - x: " + x + ", y: " + y);
        
        // In boot protocol mode, use simpler report if available
        if (currentProtocolMode == HidConstants.Protocol.MODE_BOOT && 
                bootMouseInputReportCharacteristic != null) {
            return sendBootMouseReport(device, combinedReport[1], x, y);
        }
        
        // Otherwise use the combined report, keeping existing media, mouse button, and keyboard values
        boolean result = sendFullReport(device, combinedReport[0], combinedReport[1], x, y, 
                            combinedReport[4], extractKeyboardKeys());
        
        Log.d(TAG, "movePointer result: " + result);
        return result;
    }
    
    /**
     * Sends a mouse button press report.
     * 
     * @param device The connected Bluetooth device
     * @param buttons Mouse button state bitmap (see HidConstants.Mouse)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendMouseButtons(BluetoothDevice device, int buttons) {
        // In boot protocol mode, use simpler report if available
        if (currentProtocolMode == HidConstants.Protocol.MODE_BOOT && 
                bootMouseInputReportCharacteristic != null) {
            return sendBootMouseReport(device, buttons, 0, 0);
        }
        
        // Otherwise use the combined report, keeping existing media and keyboard values, no movement
        return sendFullReport(device, combinedReport[0], buttons, 0, 0, 
                            combinedReport[4], extractKeyboardKeys());
    }
    
    /**
     * Performs a mouse button click.
     * 
     * @param device The connected Bluetooth device
     * @param button The button to click (see HidConstants.Mouse)
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
     * Sends a boot protocol mouse report.
     * Only used in boot protocol mode when bootMouseInputReportCharacteristic is available.
     * 
     * @param device The connected device
     * @param buttons Button state
     * @param x X movement
     * @param y Y movement
     * @return true if successful, false otherwise
     */
    private boolean sendBootMouseReport(BluetoothDevice device, int buttons, int x, int y) {
        if (device == null || bootMouseInputReportCharacteristic == null) {
            return false;
        }
        
        // Boot mouse report is 3 bytes: [buttons, x, y]
        byte[] bootReport = new byte[3];
        bootReport[0] = (byte)(buttons & 0x07);  // Buttons (3 bits)
        bootReport[1] = (byte)(Math.max(-127, Math.min(127, x)));  // X movement
        bootReport[2] = (byte)(Math.max(-127, Math.min(127, y)));  // Y movement
        
        // Ensure notifications are enabled
        if (!bootNotificationsEnabled) {
            enableBootModeNotifications();
            bootNotificationsEnabled = true;
        }
        
        // Send notification
        return sendNotificationWithRetry(
                bootMouseInputReportCharacteristic.getUuid(), bootReport);
    }
    
    // ==================== Keyboard Control Methods ====================
    
    /**
     * Sends a keyboard report with modifiers and key codes.
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
     * Sends a single key press.
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
    
    // ==================== Combined Report Methods ====================
    
    /**
     * Sends a full combined report with media, mouse, and keyboard data.
     * 
     * @param device The connected Bluetooth device
     * @param mediaButtons Media button state bitmap
     * @param mouseButtons Mouse button state bitmap
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
            enableReportModeNotifications();
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
     * Sends a combined media and mouse report.
     * 
     * @param device The connected Bluetooth device
     * @param mediaButtons Media button state bitmap
     * @param mouseButtons Mouse button state bitmap
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
    
    // ==================== Helper Methods ====================
    
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
     * Enables notifications for report mode.
     */
    private void enableReportModeNotifications() {
        BluetoothGattDescriptor descriptor = reportCharacteristic.getDescriptor(
                HidConstants.Uuids.CLIENT_CONFIG);
                
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
     * Enables notifications for boot mode.
     */
    private void enableBootModeNotifications() {
        if (bootMouseInputReportCharacteristic == null) {
            Log.e(TAG, "Boot mouse report characteristic not available");
            return;
        }
        
        BluetoothGattDescriptor descriptor = bootMouseInputReportCharacteristic.getDescriptor(
                HidConstants.Uuids.CLIENT_CONFIG);
                
        if (descriptor != null) {
            // Enable notifications (0x01, 0x00)
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.d(TAG, "Set boot mouse characteristic descriptor value to enable notifications");
            
            // Send a zero boot report to initialize the connection
            byte[] bootReport = new byte[3];
            bootReport[0] = 0; // No buttons
            bootReport[1] = 0; // No X movement
            bootReport[2] = 0; // No Y movement
            bootMouseInputReportCharacteristic.setValue(bootReport);
            
            // Send two initial reports - one is sometimes not enough
            try {
                gattServerManager.sendNotification(bootMouseInputReportCharacteristic.getUuid(), bootReport);
                Thread.sleep(20); // Small delay
                gattServerManager.sendNotification(bootMouseInputReportCharacteristic.getUuid(), bootReport);
            } catch (Exception e) {
                Log.e(TAG, "Error sending initial boot report", e);
            }
        } else {
            Log.e(TAG, "Missing Client Configuration Descriptor (CCCD) for boot mouse characteristic");
        }
    }
    
    // ==================== Configuration Methods ====================
    
    /**
     * Called when the protocol mode is changed.
     * 
     * @param mode The new protocol mode (MODE_REPORT or MODE_BOOT)
     */
    public void setProtocolMode(byte mode) {
        if (mode == HidConstants.Protocol.MODE_BOOT || mode == HidConstants.Protocol.MODE_REPORT) {
            this.currentProtocolMode = mode;
            // Reset notifications for the new protocol mode
            this.notificationsEnabled = false;
            this.bootNotificationsEnabled = false;
        }
    }
    
    /**
     * Called when notifications are enabled or disabled for a characteristic.
     * 
     * @param characteristicUuid The UUID of the characteristic
     * @param enabled Whether notifications are enabled or disabled
     */
    public void setNotificationsEnabled(UUID characteristicUuid, boolean enabled) {
        if (characteristicUuid.equals(HidConstants.Uuids.HID_REPORT)) {
            this.notificationsEnabled = enabled;
        } else if (characteristicUuid.equals(HidConstants.Uuids.HID_BOOT_MOUSE_INPUT_REPORT)) {
            this.bootNotificationsEnabled = enabled;
        }
    }
    
    /**
     * Gets the last combined report.
     * 
     * @return The last combined report
     */
    public byte[] getReport() {
        return combinedReport;
    }
    
    /**
     * Gets the boot mouse report (simplified version of the combined report).
     * 
     * @return The boot mouse report
     */
    public byte[] getBootMouseReport() {
        // Create a 3-byte boot mouse report from the combined report
        byte[] bootReport = new byte[3];
        bootReport[0] = combinedReport[1]; // Mouse buttons
        bootReport[1] = combinedReport[2]; // X
        bootReport[2] = combinedReport[3]; // Y
        return bootReport;
    }
}
