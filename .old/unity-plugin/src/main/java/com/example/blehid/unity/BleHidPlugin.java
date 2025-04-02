package com.example.blehid.unity;

import android.content.Context;
import android.util.Log;

import com.example.blehid.core.BleHidManager;

/**
 * Static interface for Unity to access BLE HID functionality.
 * Provides methods for initializing, controlling advertising, and sending key events.
 */
public class BleHidPlugin {
    private static final String TAG = "BleHidPlugin";
    
    private static BleHidManager bleHidManager;
    private static UnityCallback callback;
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
            
            // Create and initialize the BLE HID manager
            bleHidManager = new BleHidManager(appContext);
            boolean result = bleHidManager.initialize();
            
            if (result) {
                isInitialized = true;
                Log.i(TAG, "BLE HID Plugin initialized successfully");
            } else {
                Log.e(TAG, "Failed to initialize BLE HID Manager");
                bleHidManager = null;
            }
            
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing BLE HID Plugin", e);
            return false;
        }
    }
    
    /**
     * Checks if BLE peripheral mode is supported on this device.
     * 
     * @return true if BLE peripheral mode is supported, false otherwise
     */
    public static boolean isBlePeripheralSupported() {
        if (bleHidManager == null) {
            Log.e(TAG, "BLE HID Manager not initialized");
            return false;
        }
        
        return bleHidManager.isBlePeripheralSupported();
    }
    
    /**
     * Starts advertising the BLE HID device.
     * 
     * @return true if advertising started successfully, false otherwise
     */
    public static boolean startAdvertising() {
        if (!checkInitialized()) return false;
        
        boolean result = bleHidManager.startAdvertising();
        
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
        if (!checkInitialized()) return;
        
        bleHidManager.stopAdvertising();
        Log.i(TAG, "BLE advertising stopped");
    }
    
    /**
     * Checks if the device is connected to a host.
     * 
     * @return true if connected, false otherwise
     */
    public static boolean isConnected() {
        if (!checkInitialized()) return false;
        
        return bleHidManager.isConnected();
    }
    
    /**
     * Sends a keyboard HID report with the specified key.
     * 
     * @param keyCode The HID key code to send
     * @return true if the report was sent successfully, false otherwise
     */
    public static boolean sendKey(int keyCode) {
        if (!checkInitialized()) return false;
        
        if (!bleHidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot send key");
            return false;
        }
        
        boolean result = bleHidManager.sendKey(keyCode);
        
        if (result) {
            Log.d(TAG, "Key sent: " + keyCode);
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
        
        if (!bleHidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot send keys");
            return false;
        }
        
        boolean result = bleHidManager.sendKeys(keyCodes);
        
        if (result) {
            Log.d(TAG, "Keys sent: " + keyCodes.length + " keys");
        } else {
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
        if (!checkInitialized()) return false;
        
        if (!bleHidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot release keys");
            return false;
        }
        
        boolean result = bleHidManager.releaseKeys();
        
        if (result) {
            Log.d(TAG, "Keys released");
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
        BleHidPlugin.callback = callback;
        
        if (bleHidManager != null) {
            // Set up connection callback
            ConnectionCallback connectionCallback = new ConnectionCallback(callback);
            bleHidManager.getBlePairingManager().setPairingCallback(connectionCallback);
        }
    }
    
    /**
     * Gets the address of the connected device.
     * 
     * @return The MAC address of the connected device, or null if not connected
     */
    public static String getConnectedDeviceAddress() {
        if (!checkInitialized() || !bleHidManager.isConnected()) {
            return null;
        }
        
        return bleHidManager.getConnectedDevice().getAddress();
    }
    
    /**
     * Cleans up resources when the plugin is no longer needed.
     */
    public static void close() {
        if (bleHidManager != null) {
            bleHidManager.close();
            bleHidManager = null;
        }
        
        callback = null;
        isInitialized = false;
        Log.i(TAG, "BLE HID Plugin closed");
    }
    
    /**
     * Checks if the BLE HID Manager is initialized.
     * 
     * @return true if initialized, false otherwise
     */
    private static boolean checkInitialized() {
        if (!isInitialized || bleHidManager == null) {
            Log.e(TAG, "BLE HID Manager not initialized");
            return false;
        }
        
        return true;
    }
    
    /**
     * Inner class for handling connection callbacks and forwarding them to Unity.
     */
    private static class ConnectionCallback implements com.example.blehid.core.BlePairingManager.PairingCallback {
        private final UnityCallback unityCallback;
        
        ConnectionCallback(UnityCallback callback) {
            this.unityCallback = callback;
        }
        
        @Override
        public void onPairingRequested(android.bluetooth.BluetoothDevice device, int variant) {
            if (unityCallback != null) {
                unityCallback.onPairingRequested(device.getAddress(), variant);
            }
            
            // Auto-accept pairing requests
            bleHidManager.getBlePairingManager().setPairingConfirmation(device, true);
        }
        
        @Override
        public void onPairingComplete(android.bluetooth.BluetoothDevice device, boolean success) {
            if (unityCallback != null) {
                if (success) {
                    unityCallback.onDeviceConnected(device.getAddress());
                } else {
                    unityCallback.onPairingFailed(device.getAddress());
                }
            }
        }
    }
}
