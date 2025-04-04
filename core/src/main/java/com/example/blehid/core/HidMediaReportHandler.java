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
    
    // Media report: 1 byte for control buttons
    private final byte[] mediaReport = new byte[1];
    
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
     * Sends a media control report.
     * 
     * @param device  The connected Bluetooth device
     * @param buttons Button state bitmap (see HidMediaConstants)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendMediaReport(BluetoothDevice device, int buttons) {
        Log.d(TAG, "sendMediaReport - buttons: " + buttons);
        
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
        
        // Update report data
        mediaReport[0] = (byte) (buttons & 0x3F);  // Buttons (6 bits)
        
        // Log report information
        Log.d(TAG, String.format("MEDIA REPORT DATA - buttons=%d", buttons));
        Log.d(TAG, String.format("MEDIA REPORT BYTES - [%02X]", mediaReport[0]));
        
        // Send notification with retry for more reliability
        boolean success = sendNotificationWithRetry(reportCharacteristic.getUuid(), mediaReport);
        
        if (success) {
            Log.d(TAG, String.format("Media report sent: buttons=0x%02X", buttons));
        } else {
            Log.e(TAG, "Failed to send media report after retries");
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
     * Sends a button press and release for a media control.
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
     * Enables notifications for the report characteristic.
     */
    private void enableNotifications() {
        BluetoothGattDescriptor descriptor = reportCharacteristic.getDescriptor(CLIENT_CONFIG_UUID);
        if (descriptor != null) {
            // Enable notifications (0x01, 0x00)
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.d(TAG, "Set report characteristic descriptor value to enable notifications");
            
            // Send a zero report to initialize the connection
            mediaReport[0] = 0; // No buttons
            reportCharacteristic.setValue(mediaReport);
            
            // Send two initial reports - one is sometimes not enough
            try {
                gattServerManager.sendNotification(reportCharacteristic.getUuid(), mediaReport);
                Thread.sleep(20); // Small delay
                gattServerManager.sendNotification(reportCharacteristic.getUuid(), mediaReport);
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
     * Gets the last media report.
     * 
     * @return The last media report
     */
    public byte[] getMediaReport() {
        return mediaReport;
    }
}
