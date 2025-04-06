package com.example.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;
import java.util.UUID;

import static com.example.blehid.core.HidMouseConstants.*;

/**
 * Handles the creation and management of HID mouse reports.
 * This class encapsulates the logic for generating and sending mouse reports
 * in both report protocol and boot protocol modes.
 */
public class HidMouseReportHandler {
    private static final String TAG = "HidMouseReportHandler";
    
    // Mouse report: [buttons, x, y, wheel] - 4-byte format without report ID
    private final byte[] mouseReport = new byte[4];
    
    private final BleGattServerManager gattServerManager;
    private final BluetoothGattCharacteristic reportCharacteristic;
    private final BluetoothGattCharacteristic bootMouseInputReportCharacteristic;
    
    private byte currentProtocolMode = PROTOCOL_MODE_REPORT;
    private boolean notificationsEnabled = false;
    
    /**
     * Creates a new HID Mouse Report Handler.
     *
     * @param gattServerManager          The GATT server manager
     * @param reportCharacteristic       The report characteristic
     * @param bootMouseInputReportChar   The boot mouse input report characteristic
     */
    public HidMouseReportHandler(
            BleGattServerManager gattServerManager,
            BluetoothGattCharacteristic reportCharacteristic,
            BluetoothGattCharacteristic bootMouseInputReportChar) {
        this.gattServerManager = gattServerManager;
        this.reportCharacteristic = reportCharacteristic;
        this.bootMouseInputReportCharacteristic = bootMouseInputReportChar;
    }
    
    /**
     * Sends a mouse movement report.
     * 
     * @param device  The connected Bluetooth device
     * @param buttons Button state (bit 0: left, bit 1: right, bit 2: middle)
     * @param x       X movement (-127 to 127)
     * @param y       Y movement (-127 to 127)
     * @param wheel   Wheel movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendMouseReport(BluetoothDevice device, int buttons, int x, int y, int wheel) {
        Log.d(TAG, "sendMouseReport - buttons: " + buttons + ", x: " + x + ", y: " + y + ", wheel: " + wheel);
        
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
        wheel = Math.max(-127, Math.min(127, wheel));
        
        // Update report data
        mouseReport[0] = (byte) (buttons & 0x07);  // Buttons (3 bits)
        mouseReport[1] = (byte) x;                 // X movement
        mouseReport[2] = (byte) y;                 // Y movement
        mouseReport[3] = (byte) wheel;             // Wheel movement
        
        // Log more detailed report information
        Log.d(TAG, String.format("MOUSE REPORT DATA - X: %d (%02X), Y: %d (%02X), buttons=%d, wheel=%d",
                x, (byte)x & 0xFF, y, (byte)y & 0xFF, buttons, wheel));
        Log.d(TAG, String.format("MOUSE REPORT BYTES - [%02X, %02X, %02X, %02X]",
                mouseReport[0], mouseReport[1], mouseReport[2], mouseReport[3]));
        
        boolean success = false;
        
        // Report and Boot Protocol modes now use the same 3-byte report format
        if (currentProtocolMode == PROTOCOL_MODE_REPORT) {
            // Report Protocol Mode - use the regular report characteristic
            reportCharacteristic.setValue(mouseReport);
            
            // Send notification with retry for more reliability
            success = sendNotificationWithRetry(reportCharacteristic.getUuid(), mouseReport);
        } else {
            // Boot Protocol Mode - use the boot mouse input report characteristic
            bootMouseInputReportCharacteristic.setValue(mouseReport);
            
            // Send notification with retry for more reliability
            success = sendNotificationWithRetry(bootMouseInputReportCharacteristic.getUuid(), mouseReport);
        }
        
        if (success) {
            Log.d(TAG, String.format("Mouse report sent: buttons=0x%02X, x=%d, y=%d, protocol=%s",
                    buttons, x, y, currentProtocolMode == PROTOCOL_MODE_REPORT ? "Report" : "Boot"));
        } else {
            Log.e(TAG, "Failed to send mouse report after retries");
        }
        
        return success;
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
     * Sends a mouse button press report.
     * 
     * @param device The connected Bluetooth device
     * @param button The button(s) to press (BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean pressButton(BluetoothDevice device, int button) {
        return sendMouseReport(device, button, 0, 0, 0);
    }
    
    /**
     * Releases all mouse buttons.
     * 
     * @param device The connected Bluetooth device
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean releaseButtons(BluetoothDevice device) {
        return sendMouseReport(device, 0, 0, 0, 0);
    }
    
    /**
     * Sends a mouse movement report with no buttons pressed.
     * 
     * @param device The connected Bluetooth device
     * @param x X movement (-127 to 127)
     * @param y Y movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean movePointer(BluetoothDevice device, int x, int y) {
        Log.d(TAG, "movePointer - x: " + x + ", y: " + y);
        // Removed X-axis inversion to fix horizontal movement issues
        boolean result = sendMouseReport(device, 0, x, y, 0);
        Log.d(TAG, "movePointer result: " + result);
        return result;
    }
    
    /**
     * Sends a mouse wheel scroll report.
     * 
     * @param device The connected Bluetooth device
     * @param amount Scroll amount (-127 to 127, positive for up, negative for down)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean scroll(BluetoothDevice device, int amount) {
        return sendMouseReport(device, 0, 0, 0, amount);
    }
    
    /**
     * Performs a click with the specified button.
     * 
     * @param device The connected Bluetooth device
     * @param button The button to click (BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if both press and release were successful, false otherwise
     */
    public boolean click(BluetoothDevice device, int button) {
        boolean pressResult = pressButton(device, button);
        
        try {
            Thread.sleep(10); // Short delay between press and release
        } catch (InterruptedException e) {
            // Ignore
        }
        
        boolean releaseResult = releaseButtons(device);
        return pressResult && releaseResult;
    }
    
    /**
     * Enables notifications for the appropriate characteristic based on the current protocol mode.
     */
    private void enableNotifications() {
        if (currentProtocolMode == PROTOCOL_MODE_REPORT) {
            enableReportModeNotifications();
        } else {
            enableBootModeNotifications();
        }
    }
    
    /**
     * Enables notifications for report mode.
     */
    private void enableReportModeNotifications() {
        BluetoothGattDescriptor descriptor = reportCharacteristic.getDescriptor(CLIENT_CONFIG_UUID);
        if (descriptor != null) {
            // Enable notifications (0x01, 0x00)
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.d(TAG, "Set report characteristic descriptor value to enable notifications");
            
            // Send a zero report to initialize the connection
            mouseReport[0] = 0; // No buttons
            mouseReport[1] = 0; // No X movement
            mouseReport[2] = 0; // No Y movement
            mouseReport[3] = 0; // No wheel
            reportCharacteristic.setValue(mouseReport);
            
            // Send two initial reports - one is sometimes not enough
            try {
                gattServerManager.sendNotification(reportCharacteristic.getUuid(), mouseReport);
                Thread.sleep(20); // Small delay
                gattServerManager.sendNotification(reportCharacteristic.getUuid(), mouseReport);
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
        BluetoothGattDescriptor descriptor = bootMouseInputReportCharacteristic.getDescriptor(CLIENT_CONFIG_UUID);
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
    
    /**
     * Called when the protocol mode is changed.
     * 
     * @param mode The new protocol mode (PROTOCOL_MODE_REPORT or PROTOCOL_MODE_BOOT)
     */
    public void setProtocolMode(byte mode) {
        if (mode == PROTOCOL_MODE_BOOT || mode == PROTOCOL_MODE_REPORT) {
            this.currentProtocolMode = mode;
            // Reset notifications for the new protocol mode
            this.notificationsEnabled = false;
        }
    }
    
    /**
     * Called when notifications are enabled or disabled for a characteristic.
     * 
     * @param characteristicUuid The UUID of the characteristic
     * @param enabled            Whether notifications are enabled or disabled
     */
    public void setNotificationsEnabled(UUID characteristicUuid, boolean enabled) {
        if ((characteristicUuid.equals(HID_REPORT_UUID) && currentProtocolMode == PROTOCOL_MODE_REPORT) ||
            (characteristicUuid.equals(HID_BOOT_MOUSE_INPUT_REPORT_UUID) && currentProtocolMode == PROTOCOL_MODE_BOOT)) {
            this.notificationsEnabled = enabled;
        }
    }
    
    /**
     * Gets the last mouse report.
     * 
     * @return The last mouse report
     */
    public byte[] getMouseReport() {
        return mouseReport;
    }
    
    /**
     * Gets the boot mouse report.
     * Since we're now using the same 3-byte format for both Report and Boot modes,
     * this just returns the main report.
     * 
     * @return The boot mouse report
     */
    public byte[] getBootMouseReport() {
        return mouseReport;
    }
}
