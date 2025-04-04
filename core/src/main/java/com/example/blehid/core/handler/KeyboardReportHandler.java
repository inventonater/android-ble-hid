package com.example.blehid.core.handler;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.example.blehid.core.AbstractReportHandler;
import com.example.blehid.core.BleNotifier;
import com.example.blehid.core.HidConstants;
import com.example.blehid.core.manager.BleGattServiceRegistry;
import com.example.blehid.core.report.KeyboardReport;

import java.util.Arrays;

/**
 * Handler for keyboard HID reports.
 */
public class KeyboardReportHandler extends AbstractReportHandler<KeyboardReport> {
    private static final String TAG = "KeyboardReportHandler";
    
    /**
     * Creates a new keyboard report handler.
     *
     * @param gattServerManager The GATT server manager
     * @param notifier The BLE notifier
     * @param primaryCharacteristic The primary characteristic for report mode
     */
    public KeyboardReportHandler(
            BleGattServiceRegistry gattServerManager,
            BleNotifier notifier,
            BluetoothGattCharacteristic primaryCharacteristic) {
        super(gattServerManager, notifier, primaryCharacteristic);
    }
    
    @Override
    protected KeyboardReport createEmptyReport() {
        return new KeyboardReport();
    }
    
    @Override
    public byte getReportId() {
        return HidConstants.REPORT_ID_KEYBOARD;
    }
    
    /**
     * Sends a key press for a single key.
     *
     * @param device Connected device
     * @param keyCode Key code to send
     * @return true if successful, false otherwise
     */
    public boolean sendKey(BluetoothDevice device, byte keyCode) {
        return sendKeyWithModifiers(device, keyCode, (byte) 0);
    }
    
    /**
     * Sends a key press for a single key with modifiers.
     *
     * @param device Connected device
     * @param keyCode Key code to send
     * @param modifiers Modifier keys (CTRL, SHIFT, etc.)
     * @return true if successful, false otherwise
     */
    public boolean sendKeyWithModifiers(BluetoothDevice device, byte keyCode, byte modifiers) {
        // Send key press
        KeyboardReport report = new KeyboardReport(modifiers, keyCode);
        boolean pressResult = sendReport(device, report);
        
        try {
            // Small delay to make the key press noticeable
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Log.w(TAG, "Key press delay interrupted", e);
        }
        
        // Send key release
        boolean releaseResult = sendReport(device, createEmptyReport());
        
        return pressResult && releaseResult;
    }
    
    /**
     * Sends multiple keys at once.
     *
     * @param device Connected device
     * @param keyCodes Array of key codes to send (up to 6)
     * @return true if successful, false otherwise
     */
    public boolean sendKeys(BluetoothDevice device, byte[] keyCodes) {
        return sendKeysWithModifiers(device, keyCodes, (byte) 0);
    }
    
    /**
     * Sends multiple keys at once with modifiers.
     *
     * @param device Connected device
     * @param keyCodes Array of key codes to send (up to 6)
     * @param modifiers Modifier keys (CTRL, SHIFT, etc.)
     * @return true if successful, false otherwise
     */
    public boolean sendKeysWithModifiers(BluetoothDevice device, byte[] keyCodes, byte modifiers) {
        // Validate key codes array - not null, not more than MAX_KEYS (6)
        if (keyCodes == null) {
            Log.e(TAG, "Key codes array is null");
            return false;
        }
        
        if (keyCodes.length > HidConstants.Keyboard.MAX_KEYS) {
            Log.w(TAG, "Too many key codes provided, truncating to " + HidConstants.Keyboard.MAX_KEYS);
            keyCodes = Arrays.copyOf(keyCodes, HidConstants.Keyboard.MAX_KEYS);
        }
        
        // Send key press
        KeyboardReport report = new KeyboardReport(modifiers, keyCodes);
        boolean pressResult = sendReport(device, report);
        
        try {
            // Small delay to make the key press noticeable
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Log.w(TAG, "Key press delay interrupted", e);
        }
        
        // Send key release
        boolean releaseResult = sendReport(device, createEmptyReport());
        
        return pressResult && releaseResult;
    }
    
    /**
     * Releases all keys.
     *
     * @param device Connected device
     * @return true if successful, false otherwise
     */
    public boolean releaseAllKeys(BluetoothDevice device) {
        return sendReport(device, createEmptyReport());
    }
    
    /**
     * Gets the current keyboard report.
     *
     * @return Formatted keyboard report
     */
    public byte[] getKeyboardReport() {
        return createEmptyReport().format();
    }
    
    /**
     * Types a character by sending the appropriate key code and modifiers.
     * This is a convenience method for sending character inputs.
     *
     * @param device Connected device
     * @param character Character to type
     * @return true if successful, false otherwise
     */
    public boolean typeCharacter(BluetoothDevice device, char character) {
        byte keyCode = 0;
        byte modifiers = 0;
        
        // Convert character to key code and modifiers
        // This is a simplified implementation - a complete one would handle
        // all printable ASCII characters
        if (character >= 'a' && character <= 'z') {
            keyCode = (byte)(HidConstants.Keyboard.KEY_A + (character - 'a'));
        } else if (character >= 'A' && character <= 'Z') {
            keyCode = (byte)(HidConstants.Keyboard.KEY_A + (character - 'A'));
            modifiers = HidConstants.Keyboard.MODIFIER_LEFT_SHIFT;
        } else if (character >= '0' && character <= '9') {
            keyCode = (byte)(HidConstants.Keyboard.KEY_1 + (character - '1'));
            if (character == '0') {
                keyCode = HidConstants.Keyboard.KEY_0;
            }
        } else if (character == ' ') {
            keyCode = HidConstants.Keyboard.KEY_SPACE;
        } else {
            Log.w(TAG, "Unsupported character: " + character);
            return false;
        }
        
        return sendKeyWithModifiers(device, keyCode, modifiers);
    }
    
    /**
     * Types a string by sending each character sequentially.
     *
     * @param device Connected device
     * @param text Text to type
     * @return true if successful, false otherwise
     */
    public boolean typeText(BluetoothDevice device, String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        
        boolean result = true;
        for (int i = 0; i < text.length(); i++) {
            result &= typeCharacter(device, text.charAt(i));
            
            try {
                // Small delay between characters
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Log.w(TAG, "Text typing delay interrupted", e);
            }
        }
        
        return result;
    }
}
