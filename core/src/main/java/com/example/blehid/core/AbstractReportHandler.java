package com.example.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;
import java.util.UUID;

/**
 * Abstract base class for HID report handlers.
 * Provides common functionality for different types of HID reports.
 */
public abstract class AbstractReportHandler {
    private static final String TAG = "AbstractReportHandler";
    
    protected final BleGattServerManager gattServerManager;
    protected boolean notificationsEnabled = false;
    
    // Common UUIDs for all HID services
    protected static final UUID CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    
    /**
     * Creates a new HID Report Handler.
     *
     * @param gattServerManager The GATT server manager
     */
    public AbstractReportHandler(BleGattServerManager gattServerManager) {
        this.gattServerManager = gattServerManager;
    }
    
    /**
     * Helper method to send notification with retry.
     * 
     * @param charUuid The characteristic UUID
     * @param value The value to send
     * @return true if the notification was sent successfully, false otherwise
     */
    protected boolean sendNotificationWithRetry(UUID charUuid, byte[] value) {
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
     * Enables notifications for a characteristic.
     * 
     * @param characteristic The characteristic to enable notifications for
     * @param initialValue The initial value to send
     * @return true if notifications were enabled, false otherwise
     */
    protected boolean enableNotificationsForCharacteristic(
            BluetoothGattCharacteristic characteristic, byte[] initialValue) {
        
        if (characteristic == null) {
            Log.e(TAG, "Null characteristic provided");
            return false;
        }
        
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CONFIG_UUID);
        if (descriptor == null) {
            Log.e(TAG, "Missing Client Configuration Descriptor (CCCD) for characteristic: " 
                    + characteristic.getUuid());
            return false;
        }
        
        // Enable notifications (0x01, 0x00)
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        Log.d(TAG, "Set descriptor value to enable notifications for " + characteristic.getUuid());
        
        // Set initial value
        characteristic.setValue(initialValue);
        
        // Send two initial reports - one is sometimes not enough for reliable connection
        try {
            gattServerManager.sendNotification(characteristic.getUuid(), initialValue);
            Thread.sleep(20); // Small delay
            gattServerManager.sendNotification(characteristic.getUuid(), initialValue);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending initial report", e);
            return false;
        }
    }
    
    /**
     * Called when notifications are enabled or disabled for a characteristic.
     * 
     * @param characteristicUuid The UUID of the characteristic
     * @param enabled Whether notifications are enabled or disabled
     */
    public void setNotificationsEnabled(UUID characteristicUuid, boolean enabled) {
        if (shouldHandleCharacteristic(characteristicUuid)) {
            this.notificationsEnabled = enabled;
            Log.d(TAG, "Notifications " + (enabled ? "enabled" : "disabled") + 
                      " for " + characteristicUuid);
        }
    }
    
    /**
     * Called when the protocol mode is changed.
     * 
     * @param mode The new protocol mode
     */
    public void setProtocolMode(byte mode) {
        // Default implementation does nothing
        // Override in subclasses if needed
    }
    
    /**
     * Determines if this handler should handle the given characteristic.
     * 
     * @param characteristicUuid The UUID of the characteristic
     * @return true if this handler should handle the characteristic, false otherwise
     */
    protected abstract boolean shouldHandleCharacteristic(UUID characteristicUuid);
    
    /**
     * Enables notifications for the appropriate characteristic.
     */
    protected abstract void enableNotifications();
}
