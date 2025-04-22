package com.inventonater.blehid.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

/**
 * Utility class for validating Bluetooth environment prerequisites.
 * This class provides methods to check if Bluetooth is available,
 * enabled, and supports features required for BLE HID functionality.
 */
public class BluetoothEnvironmentValidator {
    private static final String TAG = "BluetoothEnvValidator";
    
    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;
    
    /**
     * Creates a new BluetoothEnvironmentValidator instance.
     * 
     * @param context The application context
     */
    public BluetoothEnvironmentValidator(Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
    }
    
    /**
     * Checks if Bluetooth is available on this device.
     * 
     * @return true if Bluetooth is available, false otherwise
     */
    public boolean isBluetoothAvailable() {
        if (bluetoothManager == null || bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not available on this device");
            return false;
        }
        return true;
    }
    
    /**
     * Checks if Bluetooth is enabled.
     * 
     * @return true if Bluetooth is enabled, false otherwise
     */
    public boolean isBluetoothEnabled() {
        if (!isBluetoothAvailable()) {
            return false;
        }
        
        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if this device supports BLE peripheral mode.
     * 
     * @return true if peripheral mode is supported, false otherwise
     */
    public boolean isPeripheralModeSupported() {
        if (!isBluetoothAvailable()) {
            return false;
        }
        
        boolean supported = bluetoothAdapter.isMultipleAdvertisementSupported();
        if (!supported) {
            Log.e(TAG, "BLE peripheral mode not supported");
        }
        return supported;
    }
    
    /**
     * Performs all validation checks required for BLE HID functionality.
     * 
     * @return true if all checks pass, false otherwise
     */
    public boolean validateAll() {
        if (!isBluetoothAvailable()) {
            Log.e(TAG, "Bluetooth not available");
            return false;
        }
        
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth not enabled");
            return false;
        }
        
        if (!isPeripheralModeSupported()) {
            Log.e(TAG, "BLE peripheral mode not supported");
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets the Bluetooth adapter.
     * 
     * @return The BluetoothAdapter, or null if not available
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }
}
