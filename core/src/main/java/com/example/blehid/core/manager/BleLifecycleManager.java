package com.example.blehid.core.manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.example.blehid.core.config.BleHidConfig;

/**
 * Manages the lifecycle of the BLE stack.
 * This class is responsible for initializing the BLE stack and
 * coordinating between the various BLE components.
 */
public class BleLifecycleManager {
    private static final String TAG = "BleLifecycleManager";
    
    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;
    
    private BleConnectionManager connectionManager;
    private BleGattServiceRegistry serviceRegistry;
    private BleAdvertisingManager advertisingManager;
    
    private boolean isInitialized = false;
    private final BleHidConfig config;
    
    /**
     * Creates a new BLE lifecycle manager.
     *
     * @param context The application context
     */
    public BleLifecycleManager(Context context) {
        this(context, new BleHidConfig());
    }
    
    /**
     * Creates a new BLE lifecycle manager with the specified configuration.
     *
     * @param context The application context
     * @param config The BLE HID configuration
     */
    public BleLifecycleManager(Context context, BleHidConfig config) {
        this.context = context.getApplicationContext(); // Use application context to prevent leaks
        this.config = config;
        
        // Get Bluetooth adapter
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(TAG, "Bluetooth manager not found");
            bluetoothAdapter = null;
        } else {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
    }
    
    /**
     * Initializes the BLE stack.
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
        
        // Initialize the connection manager
        connectionManager = new BleConnectionManager(this);
        
        // Initialize the service registry
        serviceRegistry = new BleGattServiceRegistry(this);
        
        // Initialize the advertising manager
        advertisingManager = new BleAdvertisingManager(this, connectionManager);
        
        isInitialized = true;
        Log.i(TAG, "BLE lifecycle manager initialized");
        return true;
    }
    
    /**
     * Starts the BLE stack.
     *
     * @return true if successful, false otherwise
     */
    public boolean start() {
        if (!isInitialized) {
            if (!initialize()) {
                return false;
            }
        }
        
        // Start the service registry
        boolean registryStarted = serviceRegistry.start();
        if (!registryStarted) {
            Log.e(TAG, "Failed to start GATT service registry");
            return false;
        }
        
        // Start advertising
        boolean advertisingStarted = advertisingManager.startAdvertising();
        if (!advertisingStarted) {
            Log.e(TAG, "Failed to start advertising");
            return false;
        }
        
        Log.i(TAG, "BLE stack started");
        return true;
    }
    
    /**
     * Stops the BLE stack.
     */
    public void stop() {
        if (advertisingManager != null) {
            advertisingManager.stopAdvertising();
        }
        
        if (serviceRegistry != null) {
            serviceRegistry.stop();
        }
        
        Log.i(TAG, "BLE stack stopped");
    }
    
    /**
     * Checks if BLE peripheral mode is supported on this device.
     *
     * @return true if supported, false otherwise
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
     * Gets the context.
     *
     * @return The context
     */
    public Context getContext() {
        return context;
    }
    
    /**
     * Gets the Bluetooth manager.
     *
     * @return The Bluetooth manager
     */
    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }
    
    /**
     * Gets the Bluetooth adapter.
     *
     * @return The Bluetooth adapter
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }
    
    /**
     * Gets the connection manager.
     *
     * @return The connection manager
     */
    public BleConnectionManager getConnectionManager() {
        return connectionManager;
    }
    
    /**
     * Gets the service registry.
     *
     * @return The service registry
     */
    public BleGattServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }
    
    /**
     * Gets the advertising manager.
     *
     * @return The advertising manager
     */
    public BleAdvertisingManager getAdvertisingManager() {
        return advertisingManager;
    }
    
    /**
     * Gets the configuration.
     *
     * @return The configuration
     */
    public BleHidConfig getConfig() {
        return config;
    }
    
    /**
     * Cleans up resources.
     */
    public void close() {
        stop();
        
        if (connectionManager != null) {
            connectionManager.close();
        }
        
        if (serviceRegistry != null) {
            serviceRegistry.close();
        }
        
        if (advertisingManager != null) {
            advertisingManager.close();
        }
        
        isInitialized = false;
        Log.i(TAG, "BLE lifecycle manager closed");
    }
}
