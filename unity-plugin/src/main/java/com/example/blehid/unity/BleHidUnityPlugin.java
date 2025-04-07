package com.example.blehid.unity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.example.blehid.core.BleHidManager;
import com.example.blehid.core.BlePairingManager;
import com.example.blehid.core.HidConstants;

/**
 * Main Unity plugin class for BLE HID functionality.
 * This class serves as the primary interface between Unity and the BLE HID core.
 */
public class BleHidUnityPlugin {
    private static final String TAG = "BleHidUnityPlugin";
    
    // Error codes
    public static final int ERROR_INITIALIZATION_FAILED = 1001;
    public static final int ERROR_NOT_INITIALIZED = 1002;
    public static final int ERROR_NOT_CONNECTED = 1003;
    public static final int ERROR_BLUETOOTH_DISABLED = 1004;
    public static final int ERROR_PERIPHERAL_NOT_SUPPORTED = 1005;
    public static final int ERROR_ADVERTISING_FAILED = 1006;
    public static final int ERROR_INVALID_PARAMETER = 1007;
    
    private static BleHidUnityPlugin instance;
    private Activity unityActivity;
    private BleHidManager bleHidManager;
    private BleHidUnityCallback callback;
    private boolean isInitialized = false;
    
    /**
     * Get the singleton instance of the plugin.
     */
    public static synchronized BleHidUnityPlugin getInstance() {
        if (instance == null) {
            instance = new BleHidUnityPlugin();
        }
        return instance;
    }
    
    private BleHidUnityPlugin() {
        // Private constructor to prevent direct instantiation
    }
    
    /**
     * Initialize the plugin with the Unity activity context.
     * 
     * @param activity The Unity activity
     * @param callback Callback interface for Unity events
     * @return true if initialization succeeded, false otherwise
     */
    public boolean initialize(Activity activity, BleHidUnityCallback callback) {
        if (activity == null) {
            Log.e(TAG, "Activity cannot be null");
            return false;
        }
        
        this.unityActivity = activity;
        this.callback = callback;
        
        // Create BLE HID manager
        bleHidManager = new BleHidManager(activity);
        
        // Check if BLE peripheral mode is supported
        if (!bleHidManager.isBlePeripheralSupported()) {
            String error = "BLE peripheral mode is not supported on this device";
            Log.e(TAG, error);
            if (callback != null) {
                callback.onError(ERROR_PERIPHERAL_NOT_SUPPORTED, error);
                callback.onInitializeComplete(false, error);
            }
            return false;
        }
        
        // Set up pairing callback
        bleHidManager.getBlePairingManager().setPairingCallback(new BlePairingManager.PairingCallback() {
            @Override
            public void onPairingRequested(BluetoothDevice device, int variant) {
                final String deviceInfo = getDeviceInfo(device);
                final String message = "Pairing requested by " + deviceInfo + ", variant: " + variant;
                Log.d(TAG, message);
                
                if (callback != null) {
                    callback.onDebugLog(message);
                    callback.onPairingStateChanged("REQUESTED", device.getAddress());
                }
                
                // Auto-accept pairing requests
                bleHidManager.getBlePairingManager().setPairingConfirmation(device, true);
            }
            
            @Override
            public void onPairingComplete(BluetoothDevice device, boolean success) {
                final String deviceInfo = getDeviceInfo(device);
                final String result = success ? "SUCCESS" : "FAILED";
                final String message = "Pairing " + result + " with " + deviceInfo;
                Log.d(TAG, message);
                
                if (callback != null) {
                    callback.onDebugLog(message);
                    callback.onPairingStateChanged(result, device.getAddress());
                    updateConnectionStatus();
                }
            }
        });
        
        // Initialize the BLE HID functionality
        boolean initialized = bleHidManager.initialize();
        isInitialized = initialized;
        
        if (initialized) {
            Log.d(TAG, "BLE HID initialized successfully");
            if (callback != null) {
                callback.onInitializeComplete(true, "BLE HID initialized successfully");
            }
        } else {
            String error = "BLE HID initialization failed";
            Log.e(TAG, error);
            if (callback != null) {
                callback.onError(ERROR_INITIALIZATION_FAILED, error);
                callback.onInitializeComplete(false, error);
            }
        }
        
        return initialized;
    }
    
    /**
     * Check if the plugin has been initialized successfully.
     */
    public boolean isInitialized() {
        return isInitialized && bleHidManager != null;
    }
    
    /**
     * Start BLE advertising.
     * 
     * @return true if advertising started successfully, false otherwise
     */
    public boolean startAdvertising() {
        if (!checkInitialized()) return false;
        
        boolean result = bleHidManager.startAdvertising();
        if (result) {
            Log.d(TAG, "Advertising started");
            if (callback != null) {
                callback.onAdvertisingStateChanged(true, "Advertising started");
                callback.onDebugLog("Advertising started");
            }
        } else {
            String error = "Failed to start advertising";
            Log.e(TAG, error);
            if (callback != null) {
                callback.onError(ERROR_ADVERTISING_FAILED, error);
                callback.onAdvertisingStateChanged(false, error);
                callback.onDebugLog(error);
            }
        }
        
        return result;
    }
    
    /**
     * Stop BLE advertising.
     */
    public void stopAdvertising() {
        if (!checkInitialized()) return;
        
        bleHidManager.stopAdvertising();
        Log.d(TAG, "Advertising stopped");
        if (callback != null) {
            callback.onAdvertisingStateChanged(false, "Advertising stopped");
            callback.onDebugLog("Advertising stopped");
        }
    }
    
    /**
     * Check if BLE advertising is active.
     */
    public boolean isAdvertising() {
        if (!checkInitialized()) return false;
        return bleHidManager.isAdvertising();
    }
    
    /**
     * Check if a device is connected.
     */
    public boolean isConnected() {
        if (!checkInitialized()) return false;
        return bleHidManager.isConnected();
    }
    
    /**
     * Get information about the connected device.
     * 
     * @return A string array with [deviceName, deviceAddress] or null if not connected
     */
    public String[] getConnectedDeviceInfo() {
        if (!checkInitialized() || !bleHidManager.isConnected()) {
            return null;
        }
        
        BluetoothDevice device = bleHidManager.getConnectedDevice();
        if (device == null) return null;
        
        String deviceName = device.getName();
        if (deviceName == null || deviceName.isEmpty()) {
            deviceName = "Unknown Device";
        }
        
        return new String[] { deviceName, device.getAddress() };
    }
    
    /**
     * Send a keyboard key press and release.
     * 
     * @param keyCode The HID key code
     * @return true if the key was sent successfully, false otherwise
     */
    public boolean sendKey(byte keyCode) {
        if (!checkConnected()) return false;
        
        boolean result = bleHidManager.sendKey(keyCode, 0); // Add 0 for modifier parameter
        if (result) {
            // Release key after a short delay
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                        bleHidManager.releaseAllKeys(); // Changed to the correct method name
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }).start();
        }
        
        return result;
    }
    
    /**
     * Send a keyboard key with modifier keys.
     * 
     * @param keyCode The HID key code
     * @param modifiers Modifier keys (bit flags)
     * @return true if the key was sent successfully, false otherwise
     */
    public boolean sendKeyWithModifiers(byte keyCode, byte modifiers) {
        if (!checkConnected()) return false;
        
        boolean result = bleHidManager.sendKey(keyCode, modifiers);
        if (result) {
            // Release key after a short delay
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                        bleHidManager.releaseAllKeys(); // Changed to the correct method name
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }).start();
        }
        
        return result;
    }
    
    /**
     * Type a string of text.
     * 
     * @param text The text to type
     * @return true if the text was sent successfully, false otherwise
     */
    public boolean typeText(String text) {
        if (!checkConnected()) return false;
        
        return bleHidManager.typeText(text);
    }
    
    /**
     * Send a mouse movement.
     * 
     * @param deltaX X-axis movement (-127 to 127)
     * @param deltaY Y-axis movement (-127 to 127)
     * @return true if the movement was sent successfully, false otherwise
     */
    public boolean moveMouse(int deltaX, int deltaY) {
        if (!checkConnected()) return false;
        
        // Clamp values to valid range
        deltaX = Math.max(-127, Math.min(127, deltaX));
        deltaY = Math.max(-127, Math.min(127, deltaY));
        
        return bleHidManager.moveMouse(deltaX, deltaY);
    }
    
    /**
     * Send a mouse button click.
     * 
     * @param button The button to click (0=left, 1=right, 2=middle)
     * @return true if the click was sent successfully, false otherwise
     */
    public boolean clickMouseButton(int button) {
        if (!checkConnected()) return false;
        
        return bleHidManager.clickMouseButton(button);
    }
    
    /**
     * Send a media play/pause command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean playPause() {
        if (!checkConnected()) return false;
        return bleHidManager.playPause();
    }
    
    /**
     * Send a media next track command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean nextTrack() {
        if (!checkConnected()) return false;
        return bleHidManager.nextTrack();
    }
    
    /**
     * Send a media previous track command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean previousTrack() {
        if (!checkConnected()) return false;
        return bleHidManager.previousTrack();
    }
    
    /**
     * Send a media volume up command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean volumeUp() {
        if (!checkConnected()) return false;
        return bleHidManager.volumeUp();
    }
    
    /**
     * Send a media volume down command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean volumeDown() {
        if (!checkConnected()) return false;
        return bleHidManager.volumeDown();
    }
    
    /**
     * Send a media mute command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean mute() {
        if (!checkConnected()) return false;
        return bleHidManager.mute();
    }
    
    /**
     * Get diagnostic information about the BLE HID state.
     * 
     * @return A string with diagnostic information
     */
    public String getDiagnosticInfo() {
        if (!checkInitialized()) return "Not initialized";
        
        StringBuilder info = new StringBuilder();
        info.append("Initialized: ").append(isInitialized()).append("\n");
        info.append("Advertising: ").append(bleHidManager.isAdvertising()).append("\n");
        info.append("Connected: ").append(bleHidManager.isConnected()).append("\n");
        
        if (bleHidManager.isConnected()) {
            BluetoothDevice device = bleHidManager.getConnectedDevice();
            if (device != null) {
                info.append("Device: ").append(getDeviceInfo(device)).append("\n");
                info.append("Bond State: ").append(bondStateToString(device.getBondState())).append("\n");
            }
        }
        
        return info.toString();
    }
    
    /**
     * Release all resources and close the plugin.
     */
    public void close() {
        if (bleHidManager != null) {
            bleHidManager.close();
        }
        
        isInitialized = false;
        Log.d(TAG, "Plugin closed");
    }
    
    /**
     * Update the connection status and notify Unity.
     */
    private void updateConnectionStatus() {
        if (callback == null || !isInitialized) return;
        
        if (bleHidManager.isConnected()) {
            BluetoothDevice device = bleHidManager.getConnectedDevice();
            if (device != null) {
                String deviceName = device.getName();
                if (deviceName == null || deviceName.isEmpty()) {
                    deviceName = "Unknown Device";
                }
                callback.onConnectionStateChanged(true, deviceName, device.getAddress());
            }
        } else {
            callback.onConnectionStateChanged(false, null, null);
        }
    }
    
    /**
     * Get a formatted string with device information.
     */
    private String getDeviceInfo(BluetoothDevice device) {
        if (device == null) return "null";
        
        String deviceName = device.getName();
        if (deviceName == null || deviceName.isEmpty()) {
            deviceName = "Unknown Device";
        }
        
        return deviceName + " (" + device.getAddress() + ")";
    }
    
    /**
     * Convert a Bluetooth bond state to a readable string.
     */
    private String bondStateToString(int bondState) {
        switch (bondState) {
            case BluetoothDevice.BOND_NONE:
                return "BOND_NONE";
            case BluetoothDevice.BOND_BONDING:
                return "BOND_BONDING";
            case BluetoothDevice.BOND_BONDED:
                return "BOND_BONDED";
            default:
                return "UNKNOWN(" + bondState + ")";
        }
    }
    
    /**
     * Check if the plugin is initialized and log an error if not.
     */
    private boolean checkInitialized() {
        if (!isInitialized || bleHidManager == null) {
            Log.e(TAG, "Plugin not initialized");
            if (callback != null) {
                callback.onError(ERROR_NOT_INITIALIZED, "Plugin not initialized");
            }
            return false;
        }
        return true;
    }
    
    /**
     * Check if a device is connected and log an error if not.
     */
    private boolean checkConnected() {
        if (!checkInitialized()) return false;
        
        if (!bleHidManager.isConnected()) {
            Log.e(TAG, "No device connected");
            if (callback != null) {
                callback.onError(ERROR_NOT_CONNECTED, "No device connected");
            }
            return false;
        }
        return true;
    }
}
