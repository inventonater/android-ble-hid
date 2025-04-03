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
    private final IPairingManager pairingManager;
    // Combined service for mouse, keyboard, and consumer controls
    private final HidMouseService hidCombinedService;

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
        // Use consolidated pairing manager 
        pairingManager = new PairingManager(this);
        
        // Configure retry settings
        pairingManager.setMaxPairingRetries(3);
        // Initialize the combined service
        hidCombinedService = new HidMouseService(this);
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
        
        // Initialize the combined service
        boolean hidInitialized = hidCombinedService.initialize();
        if (!hidInitialized) {
            Log.e(TAG, "Failed to initialize HID combined service");
            gattServerManager.close();
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

    public HidMouseService getHidCombinedService() {
        return hidCombinedService;
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
        if (!isInitialized || connectedDevice == null) {
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
        if (!isInitialized || connectedDevice == null) {
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
        if (!isInitialized || connectedDevice == null) {
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
        if (!isInitialized || connectedDevice == null) {
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
        if (!isInitialized || connectedDevice == null) {
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
        if (!isInitialized || connectedDevice == null) {
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
        if (!isInitialized || connectedDevice == null) {
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
        if (!isInitialized || connectedDevice == null) {
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
        if (!isInitialized || connectedDevice == null) {
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
        if (!isInitialized || connectedDevice == null) {
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
        if (!isInitialized || connectedDevice == null) {
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
        if (!isInitialized || connectedDevice == null) {
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
        if (!isInitialized || connectedDevice == null) {
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
        if (!isInitialized || connectedDevice == null) {
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
        if (!isInitialized || connectedDevice == null) {
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
        if (!isInitialized || connectedDevice == null) {
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
        if (!isInitialized || connectedDevice == null) {
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
