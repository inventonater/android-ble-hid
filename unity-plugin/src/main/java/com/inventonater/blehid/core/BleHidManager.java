package com.inventonater.blehid.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Central manager class for BLE HID functionality.
 * Coordinates between the advertiser, GATT server, and pairing components.
 * Now supports persistent device identity and bond management.
 */
public class BleHidManager {
    private static final String TAG = "BleHidManager";

    private final Context context;
    private final BluetoothEnvironmentValidator environmentValidator;
    private final BluetoothAdapter bluetoothAdapter;
    private final BleAdvertiser advertiser;
    private final BleGattServerManager gattServerManager;
    private final BlePairingManager pairingManager;
    private final BleConnectionManager connectionManager;
    // Using media service for HID functionality
    private final HidMediaService hidMediaService;
    
    // Initialization coordinator
    private final BleInitializationCoordinator initializationCoordinator;

    private boolean isInitialized = false;
    private BluetoothDevice connectedDevice = null;

    /**
     * Creates a new BLE HID Manager
     * 
     * @param context Application context
     */
    public BleHidManager(Context context) {
        this.context = context.getApplicationContext(); // Use application context to prevent leaks
        
        // Initialize environment validator first
        this.environmentValidator = new BluetoothEnvironmentValidator(context);
        
        // Get Bluetooth adapter via the validator
        this.bluetoothAdapter = environmentValidator.getBluetoothAdapter();
        
        // Initialize components
        this.advertiser = new BleAdvertiser(this);
        this.gattServerManager = new BleGattServerManager(this);
        this.pairingManager = new BlePairingManager(this);
        this.connectionManager = new BleConnectionManager(this);
        this.hidMediaService = new HidMediaService(this);
        
        // Create initialization coordinator last
        this.initializationCoordinator = new BleInitializationCoordinator(
            context, gattServerManager, hidMediaService);
    }

    /**
     * Initializes the BLE HID functionality.
     * Delegates to the BleInitializationCoordinator which handles the initialization process.
     * 
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize() {
        boolean success = initializationCoordinator.initialize();
        if (success) {
            isInitialized = true;
            Log.i(TAG, "BLE HID Manager initialized successfully");
        } else {
            Log.e(TAG, "Initialization failed");
        }
        
        return success;
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
     * Sets the device identity used for advertising.
     * This identity will persist across app restarts so peripheral can be recognized
     * by previously paired devices.
     * 
     * @param identityUuid UUID string to use as unique identity
     * @param deviceName Custom name to use for the device (can be null for default)
     * @return true if identity was set successfully
     */
    public boolean setBleIdentity(String identityUuid, String deviceName) {
        if (!isInitialized) {
            Log.e(TAG, "Cannot set identity: Not initialized");
            return false;
        }
        
        return advertiser.setDeviceIdentity(identityUuid, deviceName);
    }
    
    /**
     * Gets a list of devices currently bonded to this peripheral.
     * 
     * @return List of bonded BluetoothDevice objects
     */
    public List<BluetoothDevice> getBondedDevices() {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Cannot get bonded devices: No Bluetooth adapter");
            return new ArrayList<>();
        }
        
        Set<BluetoothDevice> bondedSet = bluetoothAdapter.getBondedDevices();
        return new ArrayList<>(bondedSet);
    }
    
    /**
     * Gets detailed information about bonded devices.
     * 
     * @return List of maps containing device information
     */
    public List<Map<String, String>> getBondedDevicesInfo() {
        List<Map<String, String>> deviceInfoList = new ArrayList<>();
        
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Cannot get bonded devices info: No Bluetooth adapter");
            return deviceInfoList;
        }
        
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            Map<String, String> deviceInfo = new HashMap<>();
            deviceInfo.put("name", BluetoothDeviceHelper.getDeviceName(device));
            deviceInfo.put("address", device.getAddress());
            deviceInfo.put("type", BluetoothDeviceHelper.getDeviceTypeString(device.getType()));
            deviceInfo.put("bondState", BluetoothDeviceHelper.getBondStateString(device.getBondState()));
            deviceInfo.put("uuids", BluetoothDeviceHelper.getDeviceUuidsString(device));
            
            deviceInfoList.add(deviceInfo);
        }
        
        return deviceInfoList;
    }
    
    /**
     * Checks if a specific device is bonded to this peripheral.
     * 
     * @param address The MAC address of the device to check
     * @return true if the device is bonded
     */
    public boolean isDeviceBonded(String address) {
        if (bluetoothAdapter == null || address == null || address.isEmpty()) {
            return false;
        }
        
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        return device != null && device.getBondState() == BluetoothDevice.BOND_BONDED;
    }
    
    /**
     * Removes a bond with a specific device.
     * 
     * @param address The MAC address of the device to forget
     * @return true if the device was forgotten or already not bonded
     */
    public boolean removeBond(String address) {
        if (bluetoothAdapter == null || address == null || address.isEmpty()) {
            Log.e(TAG, "Cannot remove bond: Invalid parameters");
            return false;
        }
        
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            if (device == null) {
                Log.e(TAG, "Cannot remove bond: Device not found");
                return false;
            }
            
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                Log.i(TAG, "Device not bonded: " + address);
                return true; // Already not bonded
            }
            
            return pairingManager.removeBond(device);
        } catch (Exception e) {
            Log.e(TAG, "Error removing bond: " + e.getMessage(), e);
            return false;
        }
    }
    
    // Helper methods for device information have been moved to BluetoothDeviceHelper class
    
    /**
     * Validates the connection state for HID operations.
     * Checks if the system is initialized and if a device is connected.
     * 
     * @return true if the system is ready for HID operations, false otherwise
     */
    protected boolean validateConnectionState() {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (connectedDevice == null) {
            Log.e(TAG, "No device connected");
            return false;
        }
        
        return true;
    }

    /**
     * Checks if the device supports BLE peripheral mode.
     * Delegates to the BluetoothEnvironmentValidator for the actual check.
     * 
     * @return true if peripheral mode is supported, false otherwise
     */
    public boolean isBlePeripheralSupported() {
        return environmentValidator.isPeripheralModeSupported();
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
        return (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
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
        if (!validateConnectionState()) {
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
        if (!validateConnectionState()) {
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
        if (!validateConnectionState()) {
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
        if (!validateConnectionState()) {
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
        if (!validateConnectionState()) {
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
        if (!validateConnectionState()) {
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
        if (!validateConnectionState()) {
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
        if (!validateConnectionState()) {
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
        if (!validateConnectionState()) {
            return false;
        }
        
        return hidMediaService.releaseButtons();
    }
    
    /**
     * Releases a specific mouse button.
     *
     * @param button The button to release (BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean releaseMouseButton(int button) {
        if (!validateConnectionState()) {
            return false;
        }
        
        return hidMediaService.releaseButton(button);
    }
    
    /**
     * Performs a click with the specified button.
     *
     * @param button The button to click (BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean clickMouseButton(int button) {
        if (!validateConnectionState()) {
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
        if (!validateConnectionState()) {
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
        if (!validateConnectionState()) {
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
        if (!validateConnectionState()) {
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
        if (!validateConnectionState()) {
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
        if (!validateConnectionState()) {
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
        if (!validateConnectionState()) {
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
        if (!validateConnectionState()) {
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
        Log.i(TAG, "Device connected: " + BluetoothDeviceHelper.getDeviceInfo(device));
        
        // Stop advertising once connected
        stopAdvertising();
        
        // Create client GATT connection to monitor connection parameters
        if (gattServerManager.createClientConnection(device)) {
            Log.d(TAG, "Created client GATT connection for parameter monitoring");
        }
        
        // Notify connection manager
        connectionManager.onDeviceConnected(device);
        
        // Kickstart HID functionality to ensure it works on reconnection
        kickstartHidFunctionality(device);
    }
    
    /**
     * Kickstarts HID functionality for a connected device.
     * This ensures HID capabilities are properly initialized on both new connections and reconnections.
     * 
     * @param device The connected device
     */
    private void kickstartHidFunctionality(BluetoothDevice device) {
        Log.i(TAG, "Kickstarting HID functionality for device: " + device.getAddress());
        
        // 1. Force notification setup for HID characteristics
        gattServerManager.setupHidNotifications(device);
        
        // 2. Send empty reports to initialize HID state
        if (hidMediaService != null) {
            // Send empty/zero reports for each HID function to initialize the state
            hidMediaService.sendInitialReports();
        }
    }

    /**
     * Called when a device disconnects.
     * 
     * @param device The disconnected device
     */
    void onDeviceDisconnected(BluetoothDevice device) {
        Log.i(TAG, "Device disconnected: " + BluetoothDeviceHelper.getDeviceInfo(device));
        
        // Notify connection manager
        connectionManager.onDeviceDisconnected();
        
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
     * Forces a disconnection regardless of the current connection state.
     * This method should be used only in exceptional cases where you need to 
     * forcibly close a connection outside the normal BLE disconnect flow, such as:
     * - When handling external disconnect requests (e.g., from UI)
     * - When cleaning up resources during application shutdown
     * - When resolving connection state inconsistencies
     * 
     * For normal connection lifecycles, the system should rely on the automatic
     * callbacks (onDeviceConnected/onDeviceDisconnected) instead.
     */
    public void clearConnectedDevice() {
        Log.i(TAG, "Forcing disconnection");
        BluetoothDevice device = connectedDevice;
        connectedDevice = null;
        if (device != null) connectionManager.onDeviceDisconnected();
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
