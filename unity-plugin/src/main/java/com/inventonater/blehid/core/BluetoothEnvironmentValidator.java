package com.inventonater.blehid.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

public class BluetoothEnvironmentValidator {
    private static final String TAG = "BluetoothEnvValidator";
    
    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;
    
    public BluetoothEnvironmentValidator(Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
    }
    
    public boolean isBluetoothAvailable() {
        if (bluetoothManager == null || bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not available on this device");
            return false;
        }
        return true;
    }
    
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
    
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }
}
