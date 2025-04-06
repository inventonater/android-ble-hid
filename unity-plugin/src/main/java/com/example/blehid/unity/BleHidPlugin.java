package com.example.blehid.unity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.example.blehid.core.BleHidManager;
import com.example.blehid.core.BlePairingManager;
import com.example.blehid.plugin.BuildConfig;

/**
 * Static interface for Unity to access BLE HID functionality.
 * Simplified to follow Android app implementation patterns.
 */
public class BleHidPlugin {
    private static final String TAG = "BleHidPlugin";
    
    private static BleHidManager bleHidManager;
    private static UnityCallback callback;
    private static boolean isInitialized = false;
    private static boolean isRegistered = false;
    
    // Version information methods - these are simple and direct
    public static String getPluginVersion() {
        return BuildConfig.PLUGIN_VERSION;
    }
    
    public static long getPluginBuildNumber() {
        return BuildConfig.PLUGIN_BUILD_NUMBER;
    }
    
    public static String getBuildTimestamp() {
        return BuildConfig.BUILD_TIMESTAMP;
    }
    
    /**
     * Auto-accepts a pairing request for the given device.
     * Used by DirectPairingCallback to simplify pairing.
     * 
     * @param device The device requesting pairing
     */
    public static void autoAcceptPairing(BluetoothDevice device) {
        if (bleHidManager != null) {
            try {
                bleHidManager.getBlePairingManager().setPairingConfirmation(device, true);
                Log.i(TAG, "Auto-accepted pairing for " + device.getAddress());
            } catch (Exception e) {
                Log.e(TAG, "Error auto-accepting pairing", e);
            }
        } else {
            Log.e(TAG, "Cannot auto-accept pairing, BleHidManager is null");
        }
    }
    
    /**
     * Initializes the BLE HID plugin with the given context.
     */
    public static boolean initialize(Context context) {
        if (isInitialized) {
            Log.i(TAG, "BLE HID Plugin already initialized");
            return true;
        }
        
        if (context == null) {
            Log.e(TAG, "Context is null, cannot initialize");
            return false;
        }
        
        try {
            // Store the application context to prevent leaks
            Context appContext = context.getApplicationContext();
            
            // Log permission status for diagnostics
            if (android.os.Build.VERSION.SDK_INT >= 31) { // Android 12
                Log.i(TAG, "Running on Android 12+, checking permissions status");
                boolean hasAdvertisePermission = hasPermission(appContext, "android.permission.BLUETOOTH_ADVERTISE");
                boolean hasConnectPermission = hasPermission(appContext, "android.permission.BLUETOOTH_CONNECT");
                boolean hasScanPermission = hasPermission(appContext, "android.permission.BLUETOOTH_SCAN");
                
                Log.i(TAG, "BLUETOOTH_ADVERTISE permission: " + (hasAdvertisePermission ? "Granted" : "Not granted"));
                Log.i(TAG, "BLUETOOTH_CONNECT permission: " + (hasConnectPermission ? "Granted" : "Not granted"));
                Log.i(TAG, "BLUETOOTH_SCAN permission: " + (hasScanPermission ? "Granted" : "Not granted"));
            }
            
            // Create and initialize the BLE HID manager
            bleHidManager = new BleHidManager(appContext);
            boolean result = bleHidManager.initialize();
            
            if (result) {
                isInitialized = true;
                Log.i(TAG, "BLE HID Plugin initialized successfully");
                
                // Register for Bluetooth state changes, mirroring the Android app
                registerBluetoothReceiver(appContext);
                
                // Set up connection callback
                if (callback != null) {
                    bleHidManager.getBlePairingManager().setPairingCallback(new DirectPairingCallback(callback));
                }
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
     * Registers a broadcast receiver for Bluetooth state changes.
     */
    private static void registerBluetoothReceiver(Context context) {
        if (isRegistered) return;
        
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
            
            context.registerReceiver(bluetoothReceiver, filter);
            isRegistered = true;
            Log.i(TAG, "Bluetooth broadcast receiver registered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register Bluetooth receiver", e);
        }
    }
    
    /**
     * Bluetooth state receiver, similar to the Android app implementation.
     */
    private static final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            
            if (action == null) return;
            
            switch (action) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    if (device != null && callback != null) {
                        Log.i(TAG, "Device connected: " + device.getAddress());
                        callback.onDeviceConnected(device.getAddress());
                    }
                    break;
                    
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    if (device != null && callback != null) {
                        Log.i(TAG, "Device disconnected: " + device.getAddress());
                        callback.onDeviceDisconnected(device.getAddress());
                    }
                    break;
                    
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    if (device != null && bondState == BluetoothDevice.BOND_NONE && callback != null) {
                        Log.i(TAG, "Bond removed: " + device.getAddress());
                        callback.onDeviceDisconnected(device.getAddress());
                    }
                    break;
            }
        }
    };
    
    /**
     * Helper method to check if a permission is granted
     */
    private static boolean hasPermission(Context context, String permission) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true; // On older versions, permissions are granted at install time
    }
    
    /**
     * Checks if BLE peripheral mode is supported on this device.
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
     */
    public static boolean startAdvertising() {
        if (!checkInitialized()) return false;
        
        try {
            boolean success = bleHidManager.startAdvertising();
            if (success) {
                Log.i(TAG, "BLE advertising started");
                // Notify Unity of state change
                if (callback != null) {
                    callback.onAdvertisingStateChanged(true);
                }
            } else {
                Log.e(TAG, "Failed to start advertising");
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error starting advertising", e);
            return false;
        }
    }
    
    /**
     * Stops advertising the BLE HID device.
     */
    public static void stopAdvertising() {
        if (!checkInitialized()) return;
        
        try {
            bleHidManager.stopAdvertising();
            Log.i(TAG, "BLE advertising stopped");
            // Notify Unity of state change
            if (callback != null) {
                callback.onAdvertisingStateChanged(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping advertising", e);
        }
    }
    
    /**
     * Checks if the device is connected to a host.
     */
    public static boolean isConnected() {
        if (!checkInitialized()) return false;
        
        try {
            return bleHidManager.isConnected();
        } catch (Exception e) {
            Log.e(TAG, "Error checking connection", e);
            return false;
        }
    }
    
    /**
     * Checks if the device is currently advertising.
     */
    public static boolean isAdvertising() {
        if (!checkInitialized()) return false;
        
        try {
            return bleHidManager.isAdvertising();
        } catch (Exception e) {
            Log.e(TAG, "Error checking advertising state", e);
            return false;
        }
    }
    
    // Media Control Methods - direct calls with minimal error handling
    
    public static boolean playPause() {
        if (!checkConnection()) return false;
        
        try {
            return bleHidManager.playPause();
        } catch (Exception e) {
            Log.e(TAG, "Error sending play/pause command", e);
            return false;
        }
    }
    
    public static boolean nextTrack() {
        if (!checkConnection()) return false;
        
        try {
            return bleHidManager.nextTrack();
        } catch (Exception e) {
            Log.e(TAG, "Error sending next track command", e);
            return false;
        }
    }
    
    public static boolean previousTrack() {
        if (!checkConnection()) return false;
        
        try {
            return bleHidManager.previousTrack();
        } catch (Exception e) {
            Log.e(TAG, "Error sending previous track command", e);
            return false;
        }
    }
    
    public static boolean volumeUp() {
        if (!checkConnection()) return false;
        
        try {
            return bleHidManager.volumeUp();
        } catch (Exception e) {
            Log.e(TAG, "Error sending volume up command", e);
            return false;
        }
    }
    
    public static boolean volumeDown() {
        if (!checkConnection()) return false;
        
        try {
            return bleHidManager.volumeDown();
        } catch (Exception e) {
            Log.e(TAG, "Error sending volume down command", e);
            return false;
        }
    }
    
    public static boolean mute() {
        if (!checkConnection()) return false;
        
        try {
            return bleHidManager.mute();
        } catch (Exception e) {
            Log.e(TAG, "Error sending mute command", e);
            return false;
        }
    }
    
    // Mouse Control Methods - direct calls with minimal error handling
    
    public static boolean moveMouse(int x, int y) {
        if (!checkConnection()) return false;
        
        try {
            // Clamp values to valid range
            x = Math.max(-127, Math.min(127, x));
            y = Math.max(-127, Math.min(127, y));
            
            return bleHidManager.moveMouse(x, y);
        } catch (Exception e) {
            Log.e(TAG, "Error moving mouse", e);
            return false;
        }
    }
    
    public static boolean pressMouseButton(int button) {
        if (!checkConnection()) return false;
        
        try {
            return bleHidManager.pressMouseButton(button);
        } catch (Exception e) {
            Log.e(TAG, "Error pressing mouse button", e);
            return false;
        }
    }
    
    public static boolean releaseMouseButtons() {
        if (!checkConnection()) return false;
        
        try {
            return bleHidManager.releaseMouseButtons();
        } catch (Exception e) {
            Log.e(TAG, "Error releasing mouse buttons", e);
            return false;
        }
    }
    
    public static boolean clickMouseButton(int button) {
        if (!checkConnection()) return false;
        
        try {
            return bleHidManager.clickMouseButton(button);
        } catch (Exception e) {
            Log.e(TAG, "Error clicking mouse button", e);
            return false;
        }
    }
    
    public static boolean scrollMouseWheel(int amount) {
        if (!checkConnection()) return false;
        
        try {
            // Clamp value to valid range
            amount = Math.max(-127, Math.min(127, amount));
            
            return bleHidManager.scrollMouseWheel(amount);
        } catch (Exception e) {
            Log.e(TAG, "Error scrolling mouse wheel", e);
            return false;
        }
    }
    
    // Combined Media and Mouse Control
    
    public static boolean sendCombinedReport(int mediaButtons, int mouseButtons, int x, int y) {
        if (!checkConnection()) return false;
        
        try {
            // Clamp values to valid range
            x = Math.max(-127, Math.min(127, x));
            y = Math.max(-127, Math.min(127, y));
            
            return bleHidManager.sendCombinedReport(mediaButtons, mouseButtons, x, y);
        } catch (Exception e) {
            Log.e(TAG, "Error sending combined report", e);
            return false;
        }
    }
    
    /**
     * Sets the Unity callback for BLE HID events.
     */
    public static void setCallback(UnityCallback callback) {
        BleHidPlugin.callback = callback;
        
        if (bleHidManager != null) {
            bleHidManager.getBlePairingManager().setPairingCallback(new DirectPairingCallback(callback));
        }
    }
    
    /**
     * Gets the address of the connected device.
     */
    public static String getConnectedDeviceAddress() {
        if (!checkConnection()) return null;
        
        try {
            return bleHidManager.getConnectedDevice().getAddress();
        } catch (Exception e) {
            Log.e(TAG, "Error getting connected device address", e);
            return null;
        }
    }
    
    /**
     * Gets the current system state as a set of flags.
     */
    public static byte getSystemState() {
        byte state = 0;
        
        if (isInitialized && bleHidManager != null) {
            state |= BleHidProtocol.State.INITIALIZED;
            
            try {
                if (bleHidManager.isConnected()) {
                    state |= BleHidProtocol.State.CONNECTED;
                }
                
                if (bleHidManager.isAdvertising()) {
                    state |= BleHidProtocol.State.ADVERTISING;
                }
                
                if (bleHidManager.isBlePeripheralSupported()) {
                    state |= BleHidProtocol.State.PERIPHERAL_SUPPORTED;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting system state", e);
            }
        }
        
        return state;
    }
    
    /**
     * Verifies that a particular aspect of the system state is active.
     */
    public static boolean verifyState(byte stateFlag) {
        byte currentState = getSystemState();
        return (currentState & stateFlag) == stateFlag;
    }
    
    /**
     * Cleans up resources when the plugin is no longer needed.
     */
    public static void close() {
        // Basic cleanup without executeCommand wrapper
        if (bleHidManager != null) {
            try {
                bleHidManager.close();
                Log.i(TAG, "BLE HID resources closed");
            } catch (Exception e) {
                Log.e(TAG, "Error closing BLE HID resources", e);
            } finally {
                bleHidManager = null;
                callback = null;
                isInitialized = false;
                
                // Unregister the broadcast receiver
                try {
                    if (isRegistered) {
                        Context context = UnityEventBridge.getUnityActivity();
                        if (context != null) {
                            context.unregisterReceiver(bluetoothReceiver);
                            isRegistered = false;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error unregistering receiver", e);
                }
            }
        }
    }
    
    /**
     * Checks if the BLE HID Manager is initialized.
     */
    private static boolean checkInitialized() {
        if (!isInitialized || bleHidManager == null) {
            Log.e(TAG, "BLE HID Manager not initialized");
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if connected to a device.
     */
    private static boolean checkConnection() {
        if (!checkInitialized()) return false;
        
        try {
            if (!bleHidManager.isConnected()) {
                Log.w(TAG, "Not connected to a host");
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error checking connection", e);
            return false;
        }
    }
}
