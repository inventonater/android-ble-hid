package com.example.blehid.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
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
    // Using media service with combined mouse functionality
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
        // Only initialize media service
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
        
        // Initialize only the media service
        boolean hidInitialized = hidMediaService.initialize();
        if (!hidInitialized) {
            Log.e(TAG, "Failed to initialize HID media service");
            gattServerManager.close();
            return false;
        }
        
        isInitialized = true;
        Log.i(TAG, "BLE HID Manager initialized successfully with media service");
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

    // Removed all keyboard-related methods to simplify the codebase

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
     * Media control methods
     */
    
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

    /**
     * Mouse control methods
     */
    
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
        // while holding a special key or using a specific report format.
        // We'll simulate it with vertical movement for simplicity
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
    
    // Connection management

    /**
     * Called when a device connects.
     * 
     * @param device The connected device
     */
    void onDeviceConnected(BluetoothDevice device) {
        connectedDevice = device;
        Log.i(TAG, "Device connected: " + device.getAddress());
        
        // Stop advertising once connected
        stopAdvertising();
    }

    /**
     * Called when a device disconnects.
     * 
     * @param device The disconnected device
     */
    void onDeviceDisconnected(BluetoothDevice device) {
        Log.i(TAG, "Device disconnected: " + device.getAddress());
        connectedDevice = null;
        
        // Restart advertising after disconnect
        startAdvertising();
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
