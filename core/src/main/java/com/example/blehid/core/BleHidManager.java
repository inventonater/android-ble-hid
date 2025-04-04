package com.example.blehid.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.example.blehid.core.config.BleHidConfig;
import com.example.blehid.core.manager.BleAdvertisingManager;
import com.example.blehid.core.manager.BleConnectionManager;
import com.example.blehid.core.manager.BleGattServiceRegistry;
import com.example.blehid.core.manager.BleLifecycleManager;

/**
 * Central manager class for BLE HID functionality.
 * Coordinates between the advertiser, GATT server, and pairing components.
 */
public class BleHidManager {
    private static final String TAG = "BleHidManager";

    private final Context context;
    private final BleLifecycleManager lifecycleManager;
    private final IPairingManager pairingManager;
    
    // Combined service for mouse, keyboard, and consumer controls
    private BleHidService hidCombinedService;

    private boolean isInitialized = false;

    /**
     * Creates a new BLE HID Manager
     * 
     * @param context Application context
     */
    public BleHidManager(Context context) {
        this.context = context.getApplicationContext(); // Use application context to prevent leaks
        
        // Initialize lifecycle manager with configuration
        BleHidConfig config = new BleHidConfig();
        lifecycleManager = new BleLifecycleManager(context, config);
        
        // Use consolidated pairing manager 
        pairingManager = new PairingManager(this);
        
        // Configure retry settings
        pairingManager.setMaxPairingRetries(3);
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
        
        if (getBluetoothAdapter() == null) {
            Log.e(TAG, "Bluetooth not supported");
            return false;
        }
        
        if (!getBluetoothAdapter().isEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            return false;
        }
        
        if (!isBlePeripheralSupported()) {
            Log.e(TAG, "BLE Peripheral mode not supported");
            return false;
        }
        
        // Initialize the lifecycle manager
        boolean lifecycleInitialized = lifecycleManager.initialize();
        if (!lifecycleInitialized) {
            Log.e(TAG, "Failed to initialize BLE lifecycle manager");
            return false;
        }
        
        // Start the GATT server
        boolean gattStarted = getGattServiceRegistry().start();
        if (!gattStarted) {
            Log.e(TAG, "Failed to start GATT service registry");
            return false;
        }
        
        // Use mouse-only service for simplicity and better compatibility
        hidCombinedService = BleHidServiceBuilder.createMouseService(this);
        
        // Initialize the combined service
        boolean hidInitialized = hidCombinedService.initialize();
        if (!hidInitialized) {
            Log.e(TAG, "Failed to initialize HID combined service");
            getGattServiceRegistry().close();
            return false;
        }
        
        isInitialized = true;
        Log.i(TAG, "BLE HID Manager initialized successfully with combined HID service");
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
        
        return getAdvertisingManager().startAdvertising();
    }

    /**
     * Stops advertising the BLE HID device.
     */
    public void stopAdvertising() {
        if (isInitialized) {
            getAdvertisingManager().stopAdvertising();
        }
    }

    /**
     * Checks if the device supports BLE peripheral mode.
     * 
     * @return true if peripheral mode is supported, false otherwise
     */
    public boolean isBlePeripheralSupported() {
        return lifecycleManager.isBlePeripheralSupported();
    }

    /**
     * Cleans up resources.
     */
    public void close() {
        stopAdvertising();
        
        if (lifecycleManager != null) {
            lifecycleManager.close();
        }
        
        isInitialized = false;
        
        Log.i(TAG, "BLE HID Manager closed");
    }

    // Getters for internal components

    public Context getContext() {
        return context;
    }

    public BluetoothManager getBluetoothManager() {
        return lifecycleManager.getBluetoothManager();
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return lifecycleManager.getBluetoothAdapter();
    }

    public BleGattServiceRegistry getGattServiceRegistry() {
        return lifecycleManager.getServiceRegistry();
    }

    public BleHidService getHidCombinedService() {
        return hidCombinedService;
    }
    
    public BleAdvertisingManager getAdvertisingManager() {
        return lifecycleManager.getAdvertisingManager();
    }
    
    public BleConnectionManager getConnectionManager() {
        return lifecycleManager.getConnectionManager();
    }
    
    public BleLifecycleManager getLifecycleManager() {
        return lifecycleManager;
    }
    
    /**
     * Mouse control methods
     */
    
    /**
     * Moves the mouse pointer by the specified amount.
     * 
     * @param x Horizontal movement (-127 to 127)
     * @param y Vertical movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean moveMouse(int x, int y) {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.movePointer(x, y);
    }
    
    /**
     * Scrolls the mouse wheel.
     *
     * @param amount Scroll amount (-127 to 127, positive for up, negative for down)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean scrollMouseWheel(int amount) {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.scroll(amount);
    }
    
    /**
     * Clicks a mouse button.
     *
     * @param button Button to click (BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean clickMouseButton(int button) {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.click(button);
    }
    
    /**
     * Presses a mouse button down.
     * 
     * @param button Button to press (BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean pressMouseButton(int button) {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.pressButton(button);
    }
    
    /**
     * Releases all mouse buttons.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean releaseMouseButtons() {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.releaseButtons();
    }
    
    // --------------- Keyboard API ---------------
    
    /**
     * Sends a keyboard key press.
     * 
     * @param keyCode Key code to send (see HidKeyboardConstants)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKey(byte keyCode) {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.sendKey(keyCode);
    }
    
    /**
     * Sends a keyboard key press with modifiers.
     * 
     * @param keyCode Key code to send
     * @param modifiers Modifier keys (ctrl, shift, alt, etc.)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeyWithModifiers(byte keyCode, byte modifiers) {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.sendKeyWithModifiers(keyCode, modifiers);
    }
    
    /**
     * Sends multiple keyboard key presses.
     * 
     * @param keyCodes Array of key codes to send (up to 6)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeys(byte[] keyCodes) {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.sendKeys(keyCodes);
    }
    
    /**
     * Sends multiple keyboard key presses with modifiers.
     * 
     * @param keyCodes Array of key codes to send (up to 6)
     * @param modifiers Modifier keys (ctrl, shift, alt, etc.)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeysWithModifiers(byte[] keyCodes, byte modifiers) {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.sendKeysWithModifiers(keyCodes, modifiers);
    }
    
    /**
     * Releases all keyboard keys.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean releaseAllKeys() {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.releaseAllKeys();
    }
    
    // --------------- Consumer Control (Media Keys) API ---------------
    
    /**
     * Sends a media play/pause control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendPlayPause() {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.sendPlayPause();
    }
    
    /**
     * Sends a media next track control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendNextTrack() {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.sendNextTrack();
    }
    
    /**
     * Sends a media previous track control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendPrevTrack() {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.sendPrevTrack();
    }
    
    /**
     * Sends a media volume up control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendVolumeUp() {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.sendVolumeUp();
    }
    
    /**
     * Sends a media volume down control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendVolumeDown() {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.sendVolumeDown();
    }
    
    /**
     * Sends a media mute control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendMute() {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.sendMute();
    }
    
    /**
     * Sends a generic consumer control.
     * 
     * @param controlBit The consumer control bit (see HidConsumerConstants)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendConsumerControl(byte controlBit) {
        if (!isInitialized || getConnectedDevice() == null) {
            Log.e(TAG, "Not connected or initialized");
            return false;
        }
        
        return hidCombinedService.sendConsumerControl(controlBit);
    }

    // Connection management

    /**
     * Called when a device connects.
     * 
     * @param device The connected device
     */
    void onDeviceConnected(BluetoothDevice device) {
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
        
        // Restart advertising after disconnect
        startAdvertising();
    }

    /**
     * Checks if a device is connected.
     * 
     * @return true if a device is connected, false otherwise
     */
    public boolean isConnected() {
        BleConnectionManager connectionManager = getConnectionManager();
        return connectionManager != null && connectionManager.getConnectedDevice() != null;
    }

    /**
     * Gets the connected device.
     * 
     * @return The connected BluetoothDevice, or null if not connected
     */
    public BluetoothDevice getConnectedDevice() {
        BleConnectionManager connectionManager = getConnectionManager();
        return connectionManager != null ? connectionManager.getConnectedDevice() : null;
    }
    
    /**
     * Checks if the device is currently advertising.
     * 
     * @return true if advertising, false otherwise
     */
    public boolean isAdvertising() {
        return getAdvertisingManager() != null && getAdvertisingManager().isAdvertising();
    }
    
    /**
     * Returns the pairing manager instance.
     * 
     * @return The IPairingManager instance
     */
    public IPairingManager getPairingManager() {
        return pairingManager;
    }
    
    /**
     * Returns the BlePairingManager interface for backward compatibility.
     * This method is deprecated and will be removed in future versions.
     * Use getPairingManager() instead.
     * 
     * @return The IPairingManager instance 
     * @deprecated Use getPairingManager() instead
     */
    @Deprecated
    public IPairingManager getBlePairingManager() {
        return pairingManager;
    }
}
