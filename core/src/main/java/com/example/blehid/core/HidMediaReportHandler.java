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
    
    // Combined report: 4 bytes
    // Byte 0: Media buttons
    // Byte 1: Mouse buttons
    // Byte 2: X movement
    // Byte 3: Y movement
    private final byte[] combinedReport = new byte[4];
    
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
     * Sends a combined media and mouse report.
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
        Log.d(TAG, "sendCombinedReport - mediaButtons: " + mediaButtons 
                + ", mouseButtons: " + mouseButtons + ", x: " + x + ", y: " + y);
        
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
        
        // Log detailed report information
        Log.d(TAG, String.format("COMBINED REPORT DATA - mediaButtons=%d, mouseButtons=%d, x=%d, y=%d",
                mediaButtons, mouseButtons, x, y));
        Log.d(TAG, String.format("COMBINED REPORT BYTES - [%02X, %02X, %02X, %02X]",
                combinedReport[0], combinedReport[1], combinedReport[2], combinedReport[3]));
        
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
     * Sends only a media control report without changing mouse state.
     * 
     * @param device  The connected Bluetooth device
     * @param buttons Media button state bitmap (see HidMediaConstants)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendMediaReport(BluetoothDevice device, int buttons) {
        // Keep existing mouse values
        return sendCombinedReport(device, buttons, combinedReport[1], 0, 0);
    }
    
    /**
     * Sends only a mouse movement report without changing media state.
     * 
     * @param device The connected Bluetooth device
     * @param x X movement (-127 to 127)
     * @param y Y movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean movePointer(BluetoothDevice device, int x, int y) {
        // Keep existing media and mouse button values
        return sendCombinedReport(device, combinedReport[0], combinedReport[1], x, y);
    }
    
    /**
     * Sends only a mouse button report without changing media state.
     * 
     * @param device The connected Bluetooth device
     * @param buttons Mouse button state bitmap (see HidMediaConstants)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendMouseButtons(BluetoothDevice device, int buttons) {
        // Keep existing media values, set x and y to 0
        return sendCombinedReport(device, combinedReport[0], buttons, 0, 0);
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
     * Enables notifications for the report characteristic.
     */
    private void enableNotifications() {
        BluetoothGattDescriptor descriptor = reportCharacteristic.getDescriptor(CLIENT_CONFIG_UUID);
        if (descriptor != null) {
            // Enable notifications (0x01, 0x00)
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.d(TAG, "Set report characteristic descriptor value to enable notifications");
            
            // Send a zero report to initialize the connection
            combinedReport[0] = 0; // No media buttons
            combinedReport[1] = 0; // No mouse buttons
            combinedReport[2] = 0; // No X movement
            combinedReport[3] = 0; // No Y movement
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
