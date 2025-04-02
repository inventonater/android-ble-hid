package com.inventonater.hid.unity;

import android.content.Context;
import android.util.Log;
import com.inventonater.hid.core.BleHid;
import com.inventonater.hid.core.api.ConnectionListener;
import com.inventonater.hid.core.api.LogLevel;
import com.inventonater.hid.core.api.MouseButton;

/**
 * Unity plugin for the BLE HID library.
 * Provides a static interface for Unity to access BLE HID functionality.
 */
public class UnityBleHid {
    private static final String TAG = "UnityBleHid";
    private static UnityConnectionListener listener;
    private static boolean initialized = false;
    
    /**
     * Initializes the BLE HID library.
     *
     * @param context The application context
     * @return true if initialization was successful, false otherwise
     */
    public static boolean initialize(Context context) {
        if (initialized) {
            Log.w(TAG, "BLE HID already initialized");
            return true;
        }
        
        try {
            // Initialize with debug logging for Unity
            boolean success = BleHid.initialize(context, LogLevel.DEBUG);
            
            if (success) {
                initialized = true;
                Log.i(TAG, "BLE HID initialized successfully");
                
                // Set up the connection listener if already provided
                setupConnectionListenerIfNeeded();
            } else {
                Log.e(TAG, "Failed to initialize BLE HID");
            }
            
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing BLE HID", e);
            return false;
        }
    }
    
    /**
     * Sets the connection listener for BLE events.
     *
     * @param listener The listener to set
     */
    public static void setConnectionListener(UnityConnectionListener listener) {
        UnityBleHid.listener = listener;
        setupConnectionListenerIfNeeded();
    }
    
    /**
     * Sets up the connection listener if BleHid is initialized.
     */
    private static void setupConnectionListenerIfNeeded() {
        if (initialized && listener != null) {
            // Create a bridge between our UnityConnectionListener and the Core ConnectionListener
            BleHid.addConnectionListener(new ConnectionListener() {
                @Override
                public void onDeviceConnected(android.bluetooth.BluetoothDevice device) {
                    if (listener != null) {
                        listener.onConnected(device.getAddress());
                    }
                }
                
                @Override
                public void onDeviceDisconnected(android.bluetooth.BluetoothDevice device) {
                    if (listener != null) {
                        listener.onDisconnected(device.getAddress());
                    }
                }
            });
        }
    }
    
    /**
     * Checks if the BLE HID library is initialized.
     *
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Checks if BLE peripheral mode is supported on this device.
     *
     * @return true if BLE peripheral mode is supported, false otherwise
     */
    public static boolean isBlePeripheralSupported() {
        if (!initialized) {
            Log.e(TAG, "BLE HID not initialized");
            return false;
        }
        
        return BleHid.isBlePeripheralSupported();
    }
    
    /**
     * Starts advertising the BLE HID device.
     *
     * @return true if advertising started successfully, false otherwise
     */
    public static boolean startAdvertising() {
        if (!initialized) {
            Log.e(TAG, "BLE HID not initialized");
            return false;
        }
        
        boolean result = BleHid.startAdvertising();
        
        if (result) {
            Log.i(TAG, "BLE advertising started");
        } else {
            Log.e(TAG, "Failed to start BLE advertising");
        }
        
        return result;
    }
    
    /**
     * Stops advertising the BLE HID device.
     */
    public static void stopAdvertising() {
        if (!initialized) {
            Log.e(TAG, "BLE HID not initialized");
            return;
        }
        
        BleHid.stopAdvertising();
        Log.i(TAG, "BLE advertising stopped");
    }
    
    /**
     * Checks if a device is connected.
     *
     * @return true if a device is connected, false otherwise
     */
    public static boolean isConnected() {
        if (!initialized) {
            Log.e(TAG, "BLE HID not initialized");
            return false;
        }
        
        return BleHid.isConnected();
    }
    
    // MOUSE FUNCTIONALITY
    
    /**
     * Moves the mouse pointer.
     *
     * @param x Horizontal movement (-127 to 127)
     * @param y Vertical movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean moveMouse(int x, int y) {
        if (!checkInitializedAndConnected()) return false;
        
        boolean result = BleHid.moveMouse(x, y);
        
        if (!result) {
            Log.e(TAG, "Failed to move mouse");
        }
        
        return result;
    }
    
    /**
     * Clicks a mouse button.
     *
     * @param button Button to click (0=LEFT, 1=RIGHT, 2=MIDDLE)
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean clickMouseButton(int button) {
        if (!checkInitializedAndConnected()) return false;
        
        MouseButton mouseButton = convertToMouseButton(button);
        if (mouseButton == null) {
            Log.e(TAG, "Invalid mouse button: " + button);
            return false;
        }
        
        boolean result = BleHid.clickMouseButton(mouseButton);
        
        if (!result) {
            Log.e(TAG, "Failed to click mouse button");
        }
        
        return result;
    }
    
    /**
     * Presses a mouse button.
     *
     * @param button Button to press (0=LEFT, 1=RIGHT, 2=MIDDLE)
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean pressMouseButton(int button) {
        if (!checkInitializedAndConnected()) return false;
        
        MouseButton mouseButton = convertToMouseButton(button);
        if (mouseButton == null) {
            Log.e(TAG, "Invalid mouse button: " + button);
            return false;
        }
        
        boolean result = BleHid.pressMouseButton(mouseButton);
        
        if (!result) {
            Log.e(TAG, "Failed to press mouse button");
        }
        
        return result;
    }
    
    /**
     * Releases all mouse buttons.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean releaseMouseButtons() {
        if (!checkInitializedAndConnected()) return false;
        
        boolean result = BleHid.releaseMouseButtons();
        
        if (!result) {
            Log.e(TAG, "Failed to release mouse buttons");
        }
        
        return result;
    }
    
    /**
     * Scrolls the mouse wheel.
     *
     * @param amount Scroll amount (-127 to 127, positive for up, negative for down)
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean scrollMouseWheel(int amount) {
        if (!checkInitializedAndConnected()) return false;
        
        boolean result = BleHid.scrollMouseWheel(amount);
        
        if (!result) {
            Log.e(TAG, "Failed to scroll mouse wheel");
        }
        
        return result;
    }
    
    // KEYBOARD FUNCTIONALITY
    
    /**
     * Sends a keyboard key.
     *
     * @param keyCode HID key code
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean sendKey(int keyCode) {
        if (!checkInitializedAndConnected()) return false;
        
        boolean result = BleHid.sendKey(keyCode);
        
        if (!result) {
            Log.e(TAG, "Failed to send key: " + keyCode);
        }
        
        return result;
    }
    
    /**
     * Sends multiple keyboard keys simultaneously.
     *
     * @param keyCodes Array of HID key codes
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean sendKeys(int[] keyCodes) {
        if (!checkInitializedAndConnected()) return false;
        
        boolean result = BleHid.sendKeys(keyCodes);
        
        if (!result) {
            Log.e(TAG, "Failed to send keys");
        }
        
        return result;
    }
    
    /**
     * Releases all pressed keys.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean releaseKeys() {
        if (!checkInitializedAndConnected()) return false;
        
        boolean result = BleHid.releaseKeys();
        
        if (!result) {
            Log.e(TAG, "Failed to release keys");
        }
        
        return result;
    }
    
    /**
     * Shuts down the BLE HID library and releases all resources.
     */
    public static void shutdown() {
        if (!initialized) {
            return;
        }
        
        BleHid.shutdown();
        initialized = false;
        listener = null;
        
        Log.i(TAG, "BLE HID shut down");
    }
    
    /**
     * Helper method to check if the library is initialized and connected.
     *
     * @return true if initialized and connected, false otherwise
     */
    private static boolean checkInitializedAndConnected() {
        if (!initialized) {
            Log.e(TAG, "BLE HID not initialized");
            return false;
        }
        
        if (!BleHid.isConnected()) {
            Log.w(TAG, "No connected device");
            return false;
        }
        
        return true;
    }
    
    /**
     * Helper method to convert int button values to MouseButton enum.
     *
     * @param button Button value (0=LEFT, 1=RIGHT, 2=MIDDLE)
     * @return MouseButton enum value, or null if invalid
     */
    private static MouseButton convertToMouseButton(int button) {
        switch (button) {
            case 0:
                return MouseButton.LEFT;
            case 1:
                return MouseButton.RIGHT;
            case 2:
                return MouseButton.MIDDLE;
            default:
                return null;
        }
    }
}
