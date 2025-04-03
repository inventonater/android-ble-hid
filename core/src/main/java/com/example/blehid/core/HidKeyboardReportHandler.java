package com.example.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import java.util.Arrays;
import java.util.UUID;

import static com.example.blehid.core.HidKeyboardConstants.*;
import static com.example.blehid.core.HidMouseConstants.CLIENT_CONFIG_UUID;

/**
 * Handles the creation and management of HID keyboard reports.
 * This class encapsulates the logic for generating and sending keyboard reports.
 */
public class HidKeyboardReportHandler extends AbstractReportHandler {
    private static final String TAG = "HidKeyboardReportHandler";
    
    // Keyboard report: [modifiers, reserved, key1, key2, key3, key4, key5, key6]
    private final byte[] keyboardReport = new byte[KEYBOARD_REPORT_SIZE];
    
    private final BluetoothGattCharacteristic keyboardCharacteristic;
    
    /**
     * Creates a new HID Keyboard Report Handler.
     *
     * @param gattServerManager     The GATT server manager
     * @param keyboardCharacteristic The keyboard characteristic
     */
    public HidKeyboardReportHandler(
            BleGattServerManager gattServerManager,
            BluetoothGattCharacteristic keyboardCharacteristic) {
        super(gattServerManager);
        this.keyboardCharacteristic = keyboardCharacteristic;
        
        // Initialize empty report buffer
        Arrays.fill(keyboardReport, (byte)0);
    }
    
    /**
     * Sends a keyboard report.
     * 
     * @param device   The connected Bluetooth device
     * @param modifiers Modifier byte (ctrl, shift, alt, etc.)
     * @param keyCodes Array of key codes (up to 6)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeyboardReport(BluetoothDevice device, byte modifiers, byte[] keyCodes) {
        Log.d(TAG, "sendKeyboardReport - modifiers: " + modifiers);
        
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
        
        // Reset report state
        Arrays.fill(keyboardReport, (byte)0);
        
        // Set modifiers
        keyboardReport[0] = modifiers;
        
        // Add key codes (up to 6)
        if (keyCodes != null) {
            int numKeys = Math.min(keyCodes.length, MAX_KEYS);
            for (int i = 0; i < numKeys; i++) {
                keyboardReport[i + 2] = keyCodes[i]; // +2 to skip modifiers and reserved bytes
            }
        }
        
        // Log report
        StringBuilder sb = new StringBuilder("Keyboard report: ");
        for (byte b : keyboardReport) {
            sb.append(String.format("%02X ", b));
        }
        Log.d(TAG, sb.toString());
        
        // Send keyboard report
        return sendReport(device, keyboardReport);
    }
    
    /**
     * Sends a single key press.
     * 
     * @param device   The connected Bluetooth device
     * @param keyCode  The key code to send
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKey(BluetoothDevice device, byte keyCode) {
        byte[] keyCodes = new byte[1];
        keyCodes[0] = keyCode;
        return sendKeyboardReport(device, (byte)0, keyCodes);
    }
    
    /**
     * Sends a key press with modifiers.
     * 
     * @param device    The connected Bluetooth device
     * @param keyCode   The key code to send
     * @param modifiers The modifier byte
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeyWithModifiers(BluetoothDevice device, byte keyCode, byte modifiers) {
        byte[] keyCodes = new byte[1];
        keyCodes[0] = keyCode;
        return sendKeyboardReport(device, modifiers, keyCodes);
    }
    
    /**
     * Sends multiple key presses simultaneously.
     * 
     * @param device   The connected Bluetooth device
     * @param keyCodes Array of key codes (up to 6)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeys(BluetoothDevice device, byte[] keyCodes) {
        return sendKeyboardReport(device, (byte)0, keyCodes);
    }
    
    /**
     * Sends multiple key presses with modifiers.
     * 
     * @param device    The connected Bluetooth device
     * @param keyCodes  Array of key codes (up to 6)
     * @param modifiers The modifier byte
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeysWithModifiers(BluetoothDevice device, byte[] keyCodes, byte modifiers) {
        return sendKeyboardReport(device, modifiers, keyCodes);
    }
    
    /**
     * Releases all keys.
     * 
     * @param device The connected Bluetooth device
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean releaseAllKeys(BluetoothDevice device) {
        // Send an empty report (all zeros)
        Arrays.fill(keyboardReport, (byte)0);
        return sendReport(device, keyboardReport);
    }
    
    /**
     * Helper method to send keyboard report with retry.
     */
    private boolean sendReport(BluetoothDevice device, byte[] report) {
        if (keyboardCharacteristic == null) {
            Log.e(TAG, "Keyboard characteristic not available");
            return false;
        }
        
        keyboardCharacteristic.setValue(report);
        
        // Send with retry for reliability
        return sendNotificationWithRetry(keyboardCharacteristic.getUuid(), report);
    }
    
    /**
     * Implements abstract method to check if this handler should process a characteristic.
     * 
     * @param characteristicUuid The characteristic UUID to check
     * @return true if this handler should handle the characteristic
     */
    @Override
    protected boolean shouldHandleCharacteristic(UUID characteristicUuid) {
        return characteristicUuid.equals(HID_KEYBOARD_INPUT_REPORT_UUID);
    }
    
    /**
     * Implements abstract method to enable notifications.
     */
    @Override
    protected void enableNotifications() {
        BluetoothGattDescriptor descriptor = keyboardCharacteristic.getDescriptor(CLIENT_CONFIG_UUID);
        if (descriptor != null) {
            // Enable notifications
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.d(TAG, "Set keyboard descriptor value to enable notifications");
            
            // Send an empty report to initialize
            Arrays.fill(keyboardReport, (byte)0);
            keyboardCharacteristic.setValue(keyboardReport);
            
            // Send two initial reports (reliability)
            try {
                gattServerManager.sendNotification(keyboardCharacteristic.getUuid(), keyboardReport);
                Thread.sleep(20); // Small delay
                gattServerManager.sendNotification(keyboardCharacteristic.getUuid(), keyboardReport);
            } catch (Exception e) {
                Log.e(TAG, "Error sending initial keyboard report", e);
            }
        } else {
            Log.e(TAG, "Missing CCCD for keyboard characteristic");
        }
    }
    
    /**
     * Called when notifications are enabled or disabled.
     * 
     * @param enabled Whether notifications are enabled
     */
    public void setNotificationsEnabled(boolean enabled) {
        this.notificationsEnabled = enabled;
    }
    
    /**
     * Gets the current keyboard report.
     * 
     * @return The current keyboard report
     */
    public byte[] getKeyboardReport() {
        return keyboardReport;
    }
}
