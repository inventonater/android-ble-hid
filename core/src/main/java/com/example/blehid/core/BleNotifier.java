package com.example.blehid.core;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import com.example.blehid.core.manager.BleGattServiceRegistry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for sending GATT notifications.
 * Handles notification state tracking and retry logic.
 */
public class BleNotifier {
    private static final String TAG = "BleNotifier";
    
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 50;
    
    private final BleGattServiceRegistry gattServerManager;
    private final Map<UUID, Boolean> notificationsEnabled = new HashMap<>();
    
    /**
     * Creates a new BLE notifier.
     *
     * @param gattServerManager The GATT server manager
     */
    public BleNotifier(BleGattServiceRegistry gattServerManager) {
        this.gattServerManager = gattServerManager;
    }
    
    /**
     * Enables notifications for a characteristic.
     *
     * @param characteristic The characteristic to enable notifications for
     * @param initialValue The initial value to send
     * @return true if successful, false otherwise
     */
    public boolean enableNotificationsForCharacteristic(
            BluetoothGattCharacteristic characteristic, byte[] initialValue) {
        if (characteristic == null) {
            Log.e(TAG, "Cannot enable notifications for null characteristic");
            return false;
        }
        
        UUID charUuid = characteristic.getUuid();
        
        // Set the initial value
        if (initialValue != null) {
            Log.d(TAG, "Setting initial value for " + charUuid + ": " + 
                  Arrays.toString(initialValue));
            characteristic.setValue(initialValue);
        }
        
        notificationsEnabled.put(charUuid, true);
        Log.d(TAG, "Notifications enabled for: " + charUuid);
        
        return true;
    }
    
    /**
     * Sets whether notifications are enabled for a characteristic.
     *
     * @param characteristicUuid The UUID of the characteristic
     * @param enabled Whether notifications are enabled
     */
    public void setNotificationsEnabled(UUID characteristicUuid, boolean enabled) {
        notificationsEnabled.put(characteristicUuid, enabled);
        Log.d(TAG, "Notifications " + (enabled ? "enabled" : "disabled") + 
              " for: " + characteristicUuid);
    }
    
    /**
     * Checks if notifications are enabled for a characteristic.
     *
     * @param characteristicUuid The UUID of the characteristic
     * @return true if notifications are enabled, false otherwise
     */
    public boolean areNotificationsEnabled(UUID characteristicUuid) {
        return notificationsEnabled.containsKey(characteristicUuid) && 
               notificationsEnabled.get(characteristicUuid);
    }
    
    /**
     * Sends a notification for a characteristic.
     *
     * @param characteristicUuid The UUID of the characteristic
     * @param value The value to send
     * @return true if the notification was sent successfully, false otherwise
     */
    public boolean sendNotification(UUID characteristicUuid, byte[] value) {
        if (!areNotificationsEnabled(characteristicUuid)) {
            Log.d(TAG, "Notifications not enabled for: " + characteristicUuid);
            return false;
        }
        
        boolean success = gattServerManager.sendNotification(characteristicUuid, value);
        if (!success) {
            Log.e(TAG, "Failed to send notification for: " + characteristicUuid);
        }
        
        return success;
    }
    
    /**
     * Sends a notification for a characteristic with retry logic.
     *
     * @param characteristicUuid The UUID of the characteristic
     * @param value The value to send
     * @return true if the notification was sent successfully, false otherwise
     */
    public boolean sendNotificationWithRetry(UUID characteristicUuid, byte[] value) {
        // Auto-enable notifications if not already enabled - this is crucial for HID devices
        if (!areNotificationsEnabled(characteristicUuid)) {
            Log.w(TAG, "Auto-enabling notifications for: " + characteristicUuid);
            setNotificationsEnabled(characteristicUuid, true);
        }
        
        // Dump the notification value for debugging
        StringBuilder hexValue = new StringBuilder();
        if (value != null) {
            for (byte b : value) {
                hexValue.append(String.format("%02X ", b));
            }
        }
        Log.d(TAG, "Sending notification for: " + characteristicUuid + " with value: " + hexValue.toString());
        
        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            boolean success = gattServerManager.sendNotification(characteristicUuid, value);
            if (success) {
                Log.d(TAG, "Notification sent successfully for: " + characteristicUuid);
                return true;
            }
            
            Log.w(TAG, "Notification retry " + (retry + 1) + " for: " + characteristicUuid);
            
            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Notification retry interrupted", e);
                return false;
            }
        }
        
        Log.e(TAG, "Failed to send notification after " + MAX_RETRIES + 
              " retries for: " + characteristicUuid);
        return false;
    }
}
