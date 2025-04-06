package com.example.blehid.unity;

import android.content.Context;
import android.util.Log;

import com.example.blehid.core.BleHidManager;
import com.example.blehid.core.HidMediaConstants;

/**
 * Static interface for Unity to access BLE HID functionality.
 * Provides methods for initializing, controlling advertising, and sending media and mouse events.
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
            
            // Check for Android 12+ permissions - the Unity side will handle requesting them
            // This just logs the status for debugging purposes
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
     * Media Control Methods
     */
    
    /**
     * Sends a play/pause control command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean playPause() {
        if (!checkInitialized()) return false;
        
        if (!bleHidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot send media command");
            return false;
        }
        
        boolean result = bleHidManager.playPause();
        
        if (result) {
            Log.d(TAG, "Play/Pause sent");
        } else {
            Log.e(TAG, "Failed to send Play/Pause");
        }
        
        return result;
    }
    
    /**
     * Sends a next track control command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean nextTrack() {
        if (!checkInitialized()) return false;
        
        if (!bleHidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot send media command");
            return false;
        }
        
        boolean result = bleHidManager.nextTrack();
        
        if (result) {
            Log.d(TAG, "Next Track sent");
        } else {
            Log.e(TAG, "Failed to send Next Track");
        }
        
        return result;
    }
    
    /**
     * Sends a previous track control command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean previousTrack() {
        if (!checkInitialized()) return false;
        
        if (!bleHidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot send media command");
            return false;
        }
        
        boolean result = bleHidManager.previousTrack();
        
        if (result) {
            Log.d(TAG, "Previous Track sent");
        } else {
            Log.e(TAG, "Failed to send Previous Track");
        }
        
        return result;
    }
    
    /**
     * Sends a volume up control command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean volumeUp() {
        if (!checkInitialized()) return false;
        
        if (!bleHidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot send media command");
            return false;
        }
        
        boolean result = bleHidManager.volumeUp();
        
        if (result) {
            Log.d(TAG, "Volume Up sent");
        } else {
            Log.e(TAG, "Failed to send Volume Up");
        }
        
        return result;
    }
    
    /**
     * Sends a volume down control command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean volumeDown() {
        if (!checkInitialized()) return false;
        
        if (!bleHidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot send media command");
            return false;
        }
        
        boolean result = bleHidManager.volumeDown();
        
        if (result) {
            Log.d(TAG, "Volume Down sent");
        } else {
            Log.e(TAG, "Failed to send Volume Down");
        }
        
        return result;
    }
    
    /**
     * Sends a mute control command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean mute() {
        if (!checkInitialized()) return false;
        
        if (!bleHidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot send media command");
            return false;
        }
        
        boolean result = bleHidManager.mute();
        
        if (result) {
            Log.d(TAG, "Mute sent");
        } else {
            Log.e(TAG, "Failed to send Mute");
        }
        
        return result;
    }
    
    /**
     * Mouse Control Methods
     */
    
    /**
     * Moves the mouse pointer by the specified amount.
     *
     * @param x The X movement amount (-127 to 127)
     * @param y The Y movement amount (-127 to 127)
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean moveMouse(int x, int y) {
        if (!checkInitialized()) return false;
        
        if (!bleHidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot move mouse");
            return false;
        }
        
        // Clamp values to valid range
        x = Math.max(-127, Math.min(127, x));
        y = Math.max(-127, Math.min(127, y));
        
        boolean result = bleHidManager.moveMouse(x, y);
        
        if (result) {
            Log.d(TAG, "Mouse moved: x=" + x + ", y=" + y);
        } else {
            Log.e(TAG, "Failed to move mouse");
        }
        
        return result;
    }
    
    /**
     * Presses a mouse button.
     *
     * @param button The button to press (HidMediaConstants.BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean pressMouseButton(int button) {
        if (!checkInitialized()) return false;
        
        if (!bleHidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot press mouse button");
            return false;
        }
        
        boolean result = bleHidManager.pressMouseButton(button);
        
        if (result) {
            Log.d(TAG, "Mouse button pressed: " + button);
        } else {
            Log.e(TAG, "Failed to press mouse button");
        }
        
        return result;
    }
    
    /**
     * Releases all mouse buttons.
     *
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean releaseMouseButtons() {
        if (!checkInitialized()) return false;
        
        if (!bleHidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot release mouse buttons");
            return false;
        }
        
        boolean result = bleHidManager.releaseMouseButtons();
        
        if (result) {
            Log.d(TAG, "Mouse buttons released");
        } else {
            Log.e(TAG, "Failed to release mouse buttons");
        }
        
        return result;
    }
    
    /**
     * Performs a click with the specified button.
     *
     * @param button The button to click (HidMediaConstants.BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean clickMouseButton(int button) {
        if (!checkInitialized()) return false;
        
        if (!bleHidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot click mouse button");
            return false;
        }
        
        boolean result = bleHidManager.clickMouseButton(button);
        
        if (result) {
            Log.d(TAG, "Mouse button clicked: " + button);
        } else {
            Log.e(TAG, "Failed to click mouse button");
        }
        
        return result;
    }
    
    /**
     * Scrolls the mouse wheel.
     *
     * @param amount The scroll amount (-127 to 127)
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean scrollMouseWheel(int amount) {
        if (!checkInitialized()) return false;
        
        if (!bleHidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot scroll mouse wheel");
            return false;
        }
        
        // Clamp value to valid range
        amount = Math.max(-127, Math.min(127, amount));
        
        boolean result = bleHidManager.scrollMouseWheel(amount);
        
        if (result) {
            Log.d(TAG, "Mouse wheel scrolled: " + amount);
        } else {
            Log.e(TAG, "Failed to scroll mouse wheel");
        }
        
        return result;
    }
    
    /**
     * Combined Media and Mouse Control
     */
    
    /**
     * Sends a combined media and mouse report.
     * 
     * @param mediaButtons Media button flags (HidMediaConstants.BUTTON_PLAY_PAUSE, etc.)
     * @param mouseButtons Mouse button flags (HidMediaConstants.BUTTON_LEFT, etc.)
     * @param x X-axis movement (-127 to 127)
     * @param y Y-axis movement (-127 to 127)
     * @return true if successful, false otherwise
     */
    public static boolean sendCombinedReport(int mediaButtons, int mouseButtons, int x, int y) {
        if (!checkInitialized()) return false;
        
        if (!bleHidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot send combined report");
            return false;
        }
        
        // Clamp values to valid range
        x = Math.max(-127, Math.min(127, x));
        y = Math.max(-127, Math.min(127, y));
        
        boolean result = bleHidManager.sendCombinedReport(mediaButtons, mouseButtons, x, y);
        
        if (result) {
            Log.d(TAG, "Combined report sent: media=" + mediaButtons + ", mouse=" + mouseButtons + ", x=" + x + ", y=" + y);
        } else {
            Log.e(TAG, "Failed to send combined report");
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
