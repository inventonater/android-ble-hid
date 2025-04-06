package com.example.blehid.tv;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.example.blehid.core.BleAdvertiser;
import com.example.blehid.core.BleGattServerManager;
import com.example.blehid.core.BlePairingManager;
import com.example.blehid.core.BleHidManager;

import java.util.UUID;

/**
 * TvBleHidManager extends the core BLE HID functionality with TV-specific features.
 * This class manages the TV-specific HID services and allows switching between different TV implementations.
 */
public class TvBleHidManager {
    private static final String TAG = "TvBleHidManager";
    private static final String PREFS_NAME = "tv_hid_prefs";
    private static final String PREF_TV_TYPE = "tv_type";
    
    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;
    private final BleAdvertiser advertiser;
    private final BleGattServerManager gattServerManager;
    private final BlePairingManager pairingManager;
    
    private TvHidService tvHidService;
    private boolean isInitialized = false;
    private BluetoothDevice connectedDevice = null;
    private String currentTvType = TvHidServiceFactory.TV_TYPE_LG; // Default to LG
    
    /**
     * Creates a new TV BLE HID Manager.
     * 
     * @param context Application context
     */
    public TvBleHidManager(Context context) {
        this.context = context.getApplicationContext(); // Use application context to prevent leaks
        
        // Get Bluetooth adapter
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(TAG, "Bluetooth manager not found");
            bluetoothAdapter = null;
        } else {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        
        // Create simple wrappers on core components
        BleHidManager tempManager = new BleHidManager(context) {
            public Context getContext() {
                return TvBleHidManager.this.context;
            }
            
            public BluetoothManager getBluetoothManager() {
                return TvBleHidManager.this.bluetoothManager;
            }
            
            public BluetoothAdapter getBluetoothAdapter() {
                return TvBleHidManager.this.bluetoothAdapter;
            }
            
            public void onDeviceConnected(BluetoothDevice device) {
                TvBleHidManager.this.onTvDeviceConnected(device);
            }
            
            public void onDeviceDisconnected(BluetoothDevice device) {
                TvBleHidManager.this.onTvDeviceDisconnected(device);
            }
            
            public BluetoothDevice getConnectedDevice() {
                return TvBleHidManager.this.connectedDevice;
            }
        };
        
        // Initialize components with the temporary manager
        advertiser = new BleAdvertiser(tempManager);
        gattServerManager = new BleGattServerManager(tempManager);
        pairingManager = new BlePairingManager(tempManager);
        
        // Load saved TV type
        loadSettings();
    }
    
    /**
     * Loads settings from SharedPreferences.
     */
    private void loadSettings() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentTvType = prefs.getString(PREF_TV_TYPE, TvHidServiceFactory.TV_TYPE_LG);
        Log.d(TAG, "Loaded TV type: " + currentTvType);
    }
    
    /**
     * Saves settings to SharedPreferences.
     */
    private void saveSettings() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_TV_TYPE, currentTvType);
        editor.apply();
        Log.d(TAG, "Saved TV type: " + currentTvType);
    }
    
    /**
     * Sets the TV type and re-initializes the service if already initialized.
     * 
     * @param tvType The TV type to use
     * @return true if successful, false otherwise
     */
    public boolean setTvType(String tvType) {
        if (tvType == null || tvType.isEmpty()) {
            Log.e(TAG, "Invalid TV type");
            return false;
        }
        
        boolean wasInitialized = isInitialized;
        
        // If already initialized, close the current service
        if (wasInitialized) {
            close();
        }
        
        // Update TV type
        currentTvType = tvType;
        saveSettings();
        
        // Re-initialize if it was previously initialized
        if (wasInitialized) {
            return initialize();
        }
        
        return true;
    }
    
    /**
     * Gets the current TV type.
     * 
     * @return The current TV type
     */
    public String getCurrentTvType() {
        return currentTvType;
    }
    
    /**
     * Initializes the TV BLE HID functionality.
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
        
        // Initialize GATT server
        boolean gattInitialized = gattServerManager.initialize();
        if (!gattInitialized) {
            Log.e(TAG, "Failed to initialize GATT server");
            return false;
        }
        
        // Create and initialize TV service based on current TV type
        tvHidService = TvHidServiceFactory.createService(currentTvType, gattServerManager);
        if (tvHidService == null) {
            Log.e(TAG, "Failed to create TV HID service");
            gattServerManager.close();
            return false;
        }
        
        boolean tvServiceInitialized = tvHidService.initialize();
        if (!tvServiceInitialized) {
            Log.e(TAG, "Failed to initialize TV HID service");
            gattServerManager.close();
            return false;
        }
        
        // Set up connection callbacks
        gattServerManager.setDeviceConnectionCallback(new BleGattServerManager.DeviceConnectionCallback() {
            @Override
            public void onDeviceConnected(BluetoothDevice device) {
                onTvDeviceConnected(device);
            }
            
            @Override
            public void onDeviceDisconnected(BluetoothDevice device) {
                onTvDeviceDisconnected(device);
            }
        });
        
        isInitialized = true;
        Log.i(TAG, "TV BLE HID Manager initialized successfully with " + tvHidService.getImplementationName());
        return true;
    }
    
    /**
     * Starts advertising the TV BLE HID device.
     * 
     * @return true if advertising started successfully, false otherwise
     */
    public boolean startAdvertising() {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        // The BleAdvertiser already has the HID service UUID configured
        return advertiser.startAdvertising();
    }
    
    /**
     * Stops advertising the TV BLE HID device.
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
        
        if (tvHidService != null) {
            tvHidService.close();
            tvHidService = null;
        }
        
        if (gattServerManager != null) {
            gattServerManager.close();
        }
        
        connectedDevice = null;
        isInitialized = false;
        
        Log.i(TAG, "TV BLE HID Manager closed");
    }
    
    /**
     * Called when a TV device connects.
     * 
     * @param device The connected device
     */
    private void onTvDeviceConnected(BluetoothDevice device) {
        connectedDevice = device;
        Log.i(TAG, "TV device connected: " + device.getAddress());
        
        // Set connected device on the TV service
        if (tvHidService != null) {
            tvHidService.setConnectedDevice(device);
        }
        
        // Stop advertising once connected
        stopAdvertising();
    }
    
    /**
     * Called when a TV device disconnects.
     * 
     * @param device The disconnected device
     */
    private void onTvDeviceDisconnected(BluetoothDevice device) {
        Log.i(TAG, "TV device disconnected: " + device.getAddress());
        
        if (tvHidService != null) {
            tvHidService.setConnectedDevice(null);
        }
        
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
     * Checks if the device is currently advertising.
     * 
     * @return true if advertising, false otherwise
     */
    public boolean isAdvertising() {
        return advertiser != null && advertiser.isAdvertising();
    }
    
    /**
     * Gets the BleAdvertiser instance.
     * 
     * @return The BleAdvertiser instance
     */
    public BleAdvertiser getAdvertiser() {
        return advertiser;
    }
    
    /**
     * Gets the BlePairingManager instance.
     * 
     * @return The BlePairingManager instance
     */
    public BlePairingManager getPairingManager() {
        return pairingManager;
    }
    
    /**
     * Gets the current TV HID service.
     * 
     * @return The current TV HID service, or null if not initialized
     */
    public TvHidService getTvHidService() {
        return tvHidService;
    }
    
    //
    // Convenience methods for common TV remote operations
    //
    
    /**
     * Moves the cursor/pointer by the specified amount.
     * 
     * @param x The X movement (-127 to 127)
     * @param y The Y movement (-127 to 127)
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean moveCursor(int x, int y) {
        if (!isInitialized || tvHidService == null || !isConnected()) {
            Log.e(TAG, "Not ready to send commands");
            return false;
        }
        
        return tvHidService.moveCursor(x, y);
    }
    
    /**
     * Performs a click with the select/OK button.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean clickSelectButton() {
        if (!isInitialized || tvHidService == null || !isConnected()) {
            Log.e(TAG, "Not ready to send commands");
            return false;
        }
        
        return tvHidService.clickSelectButton();
    }
    
    /**
     * Performs a directional button click (press and release).
     * 
     * @param direction One of TvHidService.DIRECTION_* constants
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean clickDirectionalButton(int direction) {
        if (!isInitialized || tvHidService == null || !isConnected()) {
            Log.e(TAG, "Not ready to send commands");
            return false;
        }
        
        return tvHidService.clickDirectionalButton(direction);
    }
    
    /**
     * Sends a media control command.
     * 
     * @param mediaControl One of TvHidService.MEDIA_* constants
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean sendMediaControl(int mediaControl) {
        if (!isInitialized || tvHidService == null || !isConnected()) {
            Log.e(TAG, "Not ready to send commands");
            return false;
        }
        
        return tvHidService.sendMediaControl(mediaControl);
    }
    
    /**
     * Sends a back button press.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean pressBackButton() {
        if (!isInitialized || tvHidService == null || !isConnected()) {
            Log.e(TAG, "Not ready to send commands");
            return false;
        }
        
        return tvHidService.pressBackButton();
    }
    
    /**
     * Sends a home button press.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean pressHomeButton() {
        if (!isInitialized || tvHidService == null || !isConnected()) {
            Log.e(TAG, "Not ready to send commands");
            return false;
        }
        
        return tvHidService.pressHomeButton();
    }
    
    // Convenience methods for media controls
    
    /**
     * Sends a play/pause control.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean playPause() {
        return sendMediaControl(TvHidService.MEDIA_PLAY_PAUSE);
    }
    
    /**
     * Sends a next track control.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean nextTrack() {
        return sendMediaControl(TvHidService.MEDIA_NEXT);
    }
    
    /**
     * Sends a previous track control.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean previousTrack() {
        return sendMediaControl(TvHidService.MEDIA_PREVIOUS);
    }
    
    /**
     * Sends a volume up control.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean volumeUp() {
        return sendMediaControl(TvHidService.MEDIA_VOLUME_UP);
    }
    
    /**
     * Sends a volume down control.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean volumeDown() {
        return sendMediaControl(TvHidService.MEDIA_VOLUME_DOWN);
    }
    
    /**
     * Sends a mute control.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean mute() {
        return sendMediaControl(TvHidService.MEDIA_MUTE);
    }
    
    // Convenience methods for D-pad navigation
    
    /**
     * Sends an up button click.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean up() {
        return clickDirectionalButton(TvHidService.DIRECTION_UP);
    }
    
    /**
     * Sends a down button click.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean down() {
        return clickDirectionalButton(TvHidService.DIRECTION_DOWN);
    }
    
    /**
     * Sends a left button click.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean left() {
        return clickDirectionalButton(TvHidService.DIRECTION_LEFT);
    }
    
    /**
     * Sends a right button click.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean right() {
        return clickDirectionalButton(TvHidService.DIRECTION_RIGHT);
    }
    
    /**
     * Sends a center/OK button click.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean center() {
        return clickDirectionalButton(TvHidService.DIRECTION_CENTER);
    }
    
    /**
     * Checks if the currently selected TV implementation supports pointer movement.
     * 
     * @return true if pointer movement is supported, false otherwise
     */
    public boolean supportsPointer() {
        return tvHidService != null && tvHidService.supportsPointer();
    }
    
    /**
     * Checks if the currently selected TV implementation supports media controls.
     * 
     * @return true if media controls are supported, false otherwise
     */
    public boolean supportsMediaControls() {
        return tvHidService != null && tvHidService.supportsMediaControls();
    }
}
