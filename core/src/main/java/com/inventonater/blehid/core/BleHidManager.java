package com.inventonater.blehid.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Central manager class for BLE HID functionality.
 * Coordinates between the advertiser, GATT server, and pairing components.
 */
public class BleHidManager {
    private static final String TAG = "BleHidManager";

    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;
    private final BleAdvertiser advertiser;
    private final BleGattServerManager gattServerManager;
    private final BlePairingManager pairingManager;
    private final BleConnectionManager connectionManager;
    // Using media service for HID functionality
    private final HidMediaService hidMediaService;

    private boolean isInitialized = false;
    private BluetoothDevice connectedDevice = null;

    /**
     * Creates a new BLE HID Manager
     * 
     * @param context Application context
     */
    public BleHidManager(Context context) {
        this.context = context.getApplicationContext(); // Use application context to prevent leaks
        
        // Get Bluetooth adapter
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(TAG, "Bluetooth manager not found");
            bluetoothAdapter = null;
        } else {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        
        // Initialize components
        advertiser = new BleAdvertiser(this);
        gattServerManager = new BleGattServerManager(this);
        pairingManager = new BlePairingManager(this);
        connectionManager = new BleConnectionManager(this);
        hidMediaService = new HidMediaService(this);
    }

    /**
     * Initializes the BLE HID functionality.
     * 
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize() {
        if (isInitialized) {
            Log.w(TAG, "Already initialized");
            return true;
        }
        
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported");
            return false;
        }
        
        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            return false;
        }
        
        if (!isBlePeripheralSupported()) {
            Log.e(TAG, "BLE Peripheral mode not supported");
            return false;
        }
        
        // Initialize components
        boolean gattInitialized = gattServerManager.initialize();
        if (!gattInitialized) {
            Log.e(TAG, "Failed to initialize GATT server");
            return false;
        }
        
        // Initialize HID service
        boolean hidInitialized = hidMediaService.initialize();
        if (!hidInitialized) {
            Log.e(TAG, "Failed to initialize HID service");
            gattServerManager.close();
            return false;
        }
        
        isInitialized = true;
        Log.i(TAG, "BLE HID Manager initialized successfully");
        return true;
    }

    /**
     * Starts advertising the BLE HID device.
     * 
     * @return true if advertising started successfully, false otherwise
     */
    public boolean startAdvertising() {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        return advertiser.startAdvertising();
    }

    /**
     * Stops advertising the BLE HID device.
     */
    public void stopAdvertising() {
        if (isInitialized) {
            advertiser.stopAdvertising();
        }
    }

    /**
     * Checks if the device supports BLE peripheral mode.
     * 
     * @return true if peripheral mode is supported, false otherwise
     */
    public boolean isBlePeripheralSupported() {
        if (bluetoothAdapter == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return bluetoothAdapter.isMultipleAdvertisementSupported();
        }
        
        return false;
    }

    /**
     * Cleans up resources.
     */
    public void close() {
        stopAdvertising();
        
        if (gattServerManager != null) {
            gattServerManager.close();
        }
        
        connectedDevice = null;
        isInitialized = false;
        
        Log.i(TAG, "BLE HID Manager closed");
    }

    // Getters for internal components

    public Context getContext() {
        return context;
    }

    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public BleGattServerManager getGattServerManager() {
        return gattServerManager;
    }

    public HidMediaService getHidMediaService() {
        return hidMediaService;
    }
    
    /**
     * Returns the BleConnectionManager instance.
     * 
     * @return The BleConnectionManager instance
     */
    public BleConnectionManager getConnectionManager() {
        return connectionManager;
    }
    
    // ==================== Media Control Methods ====================
    
    /**
     * Sends a play/pause control.
     *
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean playPause() {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidMediaService.playPause();
    }
    
    /**
     * Sends a next track control.
     *
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean nextTrack() {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidMediaService.nextTrack();
    }
    
    /**
     * Sends a previous track control.
     *
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean previousTrack() {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidMediaService.previousTrack();
    }
    
    /**
     * Sends a volume up control.
     *
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean volumeUp() {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidMediaService.volumeUp();
    }
    
    /**
     * Sends a volume down control.
     *
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean volumeDown() {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidMediaService.volumeDown();
    }
    
    /**
     * Sends a mute control.
     *
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean mute() {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidMediaService.mute();
    }

    // ==================== Mouse Control Methods ====================
    
    /**
     * Moves the mouse pointer by the specified amount.
     *
     * @param x The X movement amount (-127 to 127)
     * @param y The Y movement amount (-127 to 127)
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean moveMouse(int x, int y) {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidMediaService.movePointer(x, y);
    }
    
    /**
     * Presses a mouse button.
     *
     * @param button The button to press (BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean pressMouseButton(int button) {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidMediaService.pressButton(button);
    }
    
    /**
     * Releases all mouse buttons.
     *
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean releaseMouseButtons() {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidMediaService.releaseButtons();
    }
    
    /**
     * Performs a click with the specified button.
     *
     * @param button The button to click (BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean clickMouseButton(int button) {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidMediaService.click(button);
    }
    
    /**
     * Scrolls the mouse wheel.
     *
     * @param amount The scroll amount (-127 to 127)
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean scrollMouseWheel(int amount) {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        // Vertical scrolling is often implemented as mouse movement along the Y axis
        return hidMediaService.movePointer(0, amount);
    }
    
    /**
     * Send a combined media and mouse report.
     * 
     * @param mediaButtons Media button flags (BUTTON_PLAY_PAUSE, etc.)
     * @param mouseButtons Mouse button flags (BUTTON_LEFT, etc.)
     * @param x X-axis movement (-127 to 127)
     * @param y Y-axis movement (-127 to 127)
     * @return true if successful, false otherwise
     */
    public boolean sendCombinedReport(int mediaButtons, int mouseButtons, int x, int y) {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidMediaService.sendCombinedReport(mediaButtons, mouseButtons, x, y);
    }
    
    // ==================== Keyboard Control Methods ====================
    
    /**
     * Sends a single key press.
     * 
     * @param keyCode The key code to send
     * @param modifiers Optional modifier keys (shift, ctrl, alt, etc.)
     * @return true if successful, false otherwise
     */
    public boolean sendKey(byte keyCode, int modifiers) {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidMediaService.sendKey(keyCode, modifiers);
    }
    
    /**
     * Releases all currently pressed keys.
     * 
     * @return true if successful, false otherwise
     */
    public boolean releaseAllKeys() {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidMediaService.releaseAllKeys();
    }
    
    /**
     * Sends multiple keys at once.
     * 
     * @param keyCodes Array of up to 6 key codes to send simultaneously
     * @param modifiers Optional modifier keys (shift, ctrl, alt, etc.)
     * @return true if successful, false otherwise
     */
    public boolean sendKeys(byte[] keyCodes, int modifiers) {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidMediaService.sendKeys(keyCodes, modifiers);
    }
    
    /**
     * Types a single key (press and release).
     * 
     * @param keyCode The key code to type
     * @param modifiers Optional modifier keys (shift, ctrl, alt, etc.)
     * @return true if successful, false otherwise
     */
    public boolean typeKey(byte keyCode, int modifiers) {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidMediaService.typeKey(keyCode, modifiers);
    }
    
    /**
     * Types a string of text character by character.
     * 
     * @param text The text to type
     * @return true if successful, false otherwise
     */
    public boolean typeText(String text) {
        if (!isInitialized || connectedDevice == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidMediaService.typeText(text);
    }
    
    // ==================== Connection Management ====================

    /**
     * Called when a device connects.
     * 
     * @param device The connected device
     */
    void onDeviceConnected(BluetoothDevice device) {
        connectedDevice = device;
        String deviceName = device.getName() != null ? device.getName() : device.getAddress();
        Log.i(TAG, "Device connected: " + deviceName + " (" + device.getAddress() + ")");
        
        // Stop advertising once connected
        stopAdvertising();
        
        // Create client GATT connection to monitor connection parameters
        if (gattServerManager.createClientConnection(device)) {
            Log.d(TAG, "Created client GATT connection for parameter monitoring");
        }
        
        // Notify connection manager
        connectionManager.onDeviceConnected(device);
        
        // Update the foreground service notification
        updateForegroundServiceNotification();
    }

    /**
     * Called when a device disconnects.
     * 
     * @param device The disconnected device
     */
    void onDeviceDisconnected(BluetoothDevice device) {
        String deviceName = device.getName() != null ? device.getName() : device.getAddress();
        Log.i(TAG, "Device disconnected: " + deviceName + " (" + device.getAddress() + ")");
        
        // Notify connection manager
        connectionManager.onDeviceDisconnected();
        
        connectedDevice = null;
        
        // Update the foreground service notification to show disconnected state
        updateForegroundServiceNotification();
        
        // Restart advertising after disconnect
        startAdvertising();
    }
    
    /**
     * Updates the foreground service notification when connection state changes
     */
    private void updateForegroundServiceNotification() {
        // Trigger notification update in the BleHidForegroundService
        Context context = getContext();
        if (context != null) {
            try {
                // Use intent-based approach to refresh the notification
                Intent refreshIntent = new Intent(context, BleHidForegroundService.class);
                refreshIntent.setAction(BleHidForegroundService.ACTION_REFRESH_NOTIFICATION);
                
                // Try to start the service (will refresh if already running)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    // Use startForegroundService for Android 8.0+
                    context.startForegroundService(refreshIntent);
                } else {
                    // Use regular startService for earlier versions
                    context.startService(refreshIntent);
                }
                
                Log.d(TAG, "Sent notification refresh intent to foreground service");
            } catch (Exception e) {
                Log.e(TAG, "Failed to refresh notification: " + e.getMessage());
            }
        }
    }

    /**
     * Checks if a device is connected.
     * 
     * @return true if a device is connected, false otherwise
     */
    public boolean isConnected() {
        return connectedDevice != null;
    }

    /**
     * Gets the connected device.
     * 
     * @return The connected BluetoothDevice, or null if not connected
     */
    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }
    
    /**
     * Returns the BleAdvertiser instance.
     * 
     * @return The BleAdvertiser instance
     */
    public BleAdvertiser getAdvertiser() {
        return advertiser;
    }
    
    /**
     * Checks if the device is currently advertising.
     * 
     * @return true if advertising, false otherwise
     */
    public boolean isAdvertising() {
        return advertiser != null && advertiser.isAdvertising();
    }
    
    /**
     * Returns the BlePairingManager instance.
     * 
     * @return The BlePairingManager instance
     */
    public BlePairingManager getBlePairingManager() {
        return pairingManager;
    }
}
