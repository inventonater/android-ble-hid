package com.inventonater.blehid.unity;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.inventonater.blehid.HidDebugListener;
import com.inventonater.blehid.HidManager;
import com.inventonater.blehid.HidReportConstants;
import com.inventonater.blehid.report.KeyboardReporter;
import com.inventonater.blehid.report.MouseReporter;
import com.inventonater.blehid.report.MediaReporter;
import com.inventonater.blehid.unity.UnityCallback;

/**
 * Modern implementation of BLE HID functionality for Unity using BluetoothHidDevice API.
 * Provides methods for initializing, sending HID reports, and managing connections.
 */
public class ModernBleHidPlugin implements HidDebugListener {
    private static final String TAG = "ModernBleHidPlugin";
    
    private static HidManager hidManager;
    private static UnityCallback unityCallback;
    private static boolean isInitialized = false;
    
    /**
     * Initializes the BLE HID plugin with the given context.
     * 
     * @param context The application context
     * @return true if initialization was successful, false otherwise
     */
    public static boolean initialize(Context context) {
        if (isInitialized) {
            Log.w(TAG, "BLE HID Plugin already initialized");
            return true;
        }
        
        if (context == null) {
            Log.e(TAG, "Context is null, cannot initialize");
            return false;
        }
        
        try {
            // Store the application context to prevent leaks
            Context appContext = context.getApplicationContext();
            
            // Create and initialize the HID manager
            hidManager = new HidManager(appContext);
            ModernBleHidPlugin pluginInstance = new ModernBleHidPlugin();
            hidManager.setDebugListener(pluginInstance);
            
            boolean result = hidManager.initialize();
            
            if (result) {
                isInitialized = true;
                Log.i(TAG, "BLE HID Plugin initialized successfully");
                
                // Now register the HID device
                if (hidManager.register()) {
                    Log.i(TAG, "HID device registered successfully");
                } else {
                    Log.e(TAG, "Failed to register HID device");
                    return false;
                }
            } else {
                Log.e(TAG, "Failed to initialize HID Manager");
                hidManager = null;
                return false;
            }
            
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing BLE HID Plugin", e);
            return false;
        }
    }
    
    /**
     * Checks if BLE HID device mode is supported on this device.
     * 
     * @return true if BLE HID device mode is supported, false otherwise
     */
    public static boolean isHidDeviceSupported() {
        if (hidManager == null) {
            Log.e(TAG, "HID Manager not initialized");
            return false;
        }
        
        // If we can initialize, we support HID Device mode
        return true;
    }
    
    /**
     * Checks if the device is connected to a host.
     * 
     * @return true if connected, false otherwise
     */
    public static boolean isConnected() {
        if (!checkInitialized()) return false;
        
        return hidManager.isConnected();
    }
    
    /**
     * Sends a keyboard key press HID report.
     * 
     * @param keyCode The HID key code to send
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean sendKey(int keyCode) {
        if (!checkInitialized()) return false;
        
        if (!hidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot send key");
            return false;
        }
        
        KeyboardReporter keyboardReporter = hidManager.getKeyboardReporter();
        if (keyboardReporter == null) {
            Log.e(TAG, "Keyboard reporter not available");
            return false;
        }
        
        boolean result = keyboardReporter.pressKey((byte)keyCode);
        
        // Release the key after a short delay
        if (result) {
            try {
                Thread.sleep(50);
                keyboardReporter.releaseKey((byte)keyCode);
                Log.d(TAG, "Key sent and released: " + keyCode);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while releasing key", e);
                Thread.currentThread().interrupt();
            }
        } else {
            Log.e(TAG, "Failed to send key: " + keyCode);
        }
        
        return result;
    }
    
    /**
     * Sends multiple keyboard HID keys simultaneously.
     * 
     * @param keyCodes Array of HID key codes to send
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean sendKeys(int[] keyCodes) {
        if (!checkInitialized()) return false;
        
        if (!hidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot send keys");
            return false;
        }
        
        KeyboardReporter keyboardReporter = hidManager.getKeyboardReporter();
        if (keyboardReporter == null) {
            Log.e(TAG, "Keyboard reporter not available");
            return false;
        }
        
        // First release any previously pressed keys
        keyboardReporter.releaseAll();
        
        // Press all keys
        boolean result = true;
        for (int keyCode : keyCodes) {
            if (!keyboardReporter.pressKey((byte)keyCode)) {
                result = false;
            }
        }
        
        // Hold for a moment then release
        try {
            Thread.sleep(50);
            keyboardReporter.releaseAll();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while releasing keys", e);
            Thread.currentThread().interrupt();
        }
        
        if (result) {
            Log.d(TAG, "Keys sent: " + keyCodes.length + " keys");
        } else {
            Log.e(TAG, "Failed to send some keys");
        }
        
        return result;
    }
    
    /**
     * Types a string by sending multiple key presses.
     *
     * @param text The string to type
     * @return true if all keys were sent successfully, false otherwise
     */
    public static boolean typeString(String text) {
        if (!checkInitialized()) return false;
        
        if (!hidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot type string");
            return false;
        }
        
        KeyboardReporter keyboardReporter = hidManager.getKeyboardReporter();
        if (keyboardReporter == null) {
            Log.e(TAG, "Keyboard reporter not available");
            return false;
        }
        
        boolean result = keyboardReporter.typeString(text);
        
        if (result) {
            Log.d(TAG, "String typed: " + text);
        } else {
            Log.e(TAG, "Failed to type string");
        }
        
        return result;
    }
    
    /**
     * Sends a mouse movement report.
     *
     * @param x The horizontal movement (-127 to 127)
     * @param y The vertical movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean moveMouseRelative(int x, int y) {
        if (!checkInitialized()) return false;
        
        if (!hidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot move mouse");
            return false;
        }
        
        MouseReporter mouseReporter = hidManager.getMouseReporter();
        if (mouseReporter == null) {
            Log.e(TAG, "Mouse reporter not available");
            return false;
        }
        
        boolean result = mouseReporter.move(x, y);
        
        if (result) {
            Log.d(TAG, "Mouse moved: x=" + x + ", y=" + y);
        } else {
            Log.e(TAG, "Failed to move mouse");
        }
        
        return result;
    }
    
    /**
     * Sends a mouse button press report.
     *
     * @param button The button to press (Use HidReportConstants.MOUSE_BUTTON_*)
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean pressMouseButton(int button) {
        if (!checkInitialized()) return false;
        
        if (!hidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot press mouse button");
            return false;
        }
        
        MouseReporter mouseReporter = hidManager.getMouseReporter();
        if (mouseReporter == null) {
            Log.e(TAG, "Mouse reporter not available");
            return false;
        }
        
        boolean result = mouseReporter.press(button);
        
        if (result) {
            Log.d(TAG, "Mouse button pressed: " + button);
        } else {
            Log.e(TAG, "Failed to press mouse button");
        }
        
        return result;
    }
    
    /**
     * Sends a mouse button release report.
     *
     * @param button The button to release (Use HidReportConstants.MOUSE_BUTTON_*)
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean releaseMouseButton(int button) {
        if (!checkInitialized()) return false;
        
        if (!hidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot release mouse button");
            return false;
        }
        
        MouseReporter mouseReporter = hidManager.getMouseReporter();
        if (mouseReporter == null) {
            Log.e(TAG, "Mouse reporter not available");
            return false;
        }
        
        boolean result = mouseReporter.release(button);
        
        if (result) {
            Log.d(TAG, "Mouse button released: " + button);
        } else {
            Log.e(TAG, "Failed to release mouse button");
        }
        
        return result;
    }
    
    /**
     * Sends a mouse button click (press and release) report.
     *
     * @param button The button to click (Use HidReportConstants.MOUSE_BUTTON_*)
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean clickMouseButton(int button) {
        if (!checkInitialized()) return false;
        
        if (!hidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot click mouse button");
            return false;
        }
        
        MouseReporter mouseReporter = hidManager.getMouseReporter();
        if (mouseReporter == null) {
            Log.e(TAG, "Mouse reporter not available");
            return false;
        }
        
        boolean result = mouseReporter.click(button);
        
        if (result) {
            Log.d(TAG, "Mouse button clicked: " + button);
        } else {
            Log.e(TAG, "Failed to click mouse button");
        }
        
        return result;
    }
    
    /**
     * Sends a mouse wheel scroll report.
     *
     * @param amount The amount to scroll (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean scrollMouseWheel(int amount) {
        if (!checkInitialized()) return false;
        
        if (!hidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot scroll mouse wheel");
            return false;
        }
        
        MouseReporter mouseReporter = hidManager.getMouseReporter();
        if (mouseReporter == null) {
            Log.e(TAG, "Mouse reporter not available");
            return false;
        }
        
        boolean result = mouseReporter.scroll(amount);
        
        if (result) {
            Log.d(TAG, "Mouse wheel scrolled: " + amount);
        } else {
            Log.e(TAG, "Failed to scroll mouse wheel");
        }
        
        return result;
    }
    
    /**
     * Sends a media key report.
     *
     * @param mediaKey The media key code to send (Use appropriate constant)
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean sendMediaKey(int mediaKey) {
        if (!checkInitialized()) return false;
        
        if (!hidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot send media key");
            return false;
        }
        
        MediaReporter mediaReporter = hidManager.getMediaReporter();
        if (mediaReporter == null) {
            Log.e(TAG, "Media reporter not available");
            return false;
        }
        
        boolean result = false;
        
        switch (mediaKey) {
            case 0xCD: // CONSUMER_USAGE_PLAY_PAUSE
                result = mediaReporter.sendPlayPause();
                break;
            case 0xB5: // CONSUMER_USAGE_SCAN_NEXT
                result = mediaReporter.sendNextTrack();
                break;
            case 0xB6: // CONSUMER_USAGE_SCAN_PREVIOUS
                result = mediaReporter.sendPreviousTrack();
                break;
            case 0xE9: // CONSUMER_USAGE_VOLUME_UP
                result = mediaReporter.sendVolumeUp();
                break;
            case 0xEA: // CONSUMER_USAGE_VOLUME_DOWN
                result = mediaReporter.sendVolumeDown();
                break;
            case 0xE2: // CONSUMER_USAGE_MUTE
                result = mediaReporter.sendMute();
                break;
            default:
                Log.e(TAG, "Unsupported media key: " + mediaKey);
                return false;
        }
        
        if (result) {
            Log.d(TAG, "Media key sent: " + mediaKey);
        } else {
            Log.e(TAG, "Failed to send media key");
        }
        
        return result;
    }
    
    /**
     * Releases all pressed keys.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean releaseAllKeys() {
        if (!checkInitialized()) return false;
        
        if (!hidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot release keys");
            return false;
        }
        
        KeyboardReporter keyboardReporter = hidManager.getKeyboardReporter();
        if (keyboardReporter == null) {
            Log.e(TAG, "Keyboard reporter not available");
            return false;
        }
        
        boolean result = keyboardReporter.releaseAll();
        
        if (result) {
            Log.d(TAG, "All keys released");
        } else {
            Log.e(TAG, "Failed to release keys");
        }
        
        return result;
    }
    
    /**
     * Sets the Unity callback for BLE HID events.
     * 
     * @param callback The callback to set
     */
    public static void setCallback(UnityCallback callback) {
        unityCallback = callback;
    }
    
    /**
     * Gets the address of the connected device.
     * 
     * @return The MAC address of the connected device, or null if not connected
     */
    public static String getConnectedDeviceAddress() {
        if (!checkInitialized() || !hidManager.isConnected()) {
            return null;
        }
        
        BluetoothDevice device = hidManager.getConnectedDevice();
        return device != null ? device.getAddress() : null;
    }
    
    /**
     * Gets the name of the connected device.
     * 
     * @return The name of the connected device, or null if not connected
     */
    public static String getConnectedDeviceName() {
        if (!checkInitialized() || !hidManager.isConnected()) {
            return null;
        }
        
        BluetoothDevice device = hidManager.getConnectedDevice();
        return device != null ? device.getName() : null;
    }
    
    /**
     * Cleans up resources when the plugin is no longer needed.
     */
    public static void close() {
        if (hidManager != null) {
            hidManager.close();
            hidManager = null;
        }
        
        unityCallback = null;
        isInitialized = false;
        Log.i(TAG, "BLE HID Plugin closed");
    }
    
    /**
     * Checks if the HID Manager is initialized.
     * 
     * @return true if initialized, false otherwise
     */
    private static boolean checkInitialized() {
        if (!isInitialized || hidManager == null) {
            Log.e(TAG, "HID Manager not initialized");
            return false;
        }
        
        return true;
    }
    
    @Override
    public void onDebugMessage(String message) {
        Log.d(TAG, "Debug: " + message);
    }
}
