package com.example.blehid.unity;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.example.blehid.core.BlePairingManager;
import com.example.blehid.core.BleHidManager;

/**
 * Simple pairing callback implementation that forwards events to the Unity layer.
 * This is a simplified version that directly handles pairing events.
 */
public class DirectPairingCallback implements BlePairingManager.PairingCallback {
    private static final String TAG = "DirectPairingCallback";
    
    private final UnityCallback unityCallback;
    
    /**
     * Creates a new DirectPairingCallback.
     * 
     * @param callback The Unity callback to forward events to
     */
    public DirectPairingCallback(UnityCallback callback) {
        this.unityCallback = callback;
    }
    
    @Override
    public void onPairingRequested(BluetoothDevice device, int variant) {
        if (unityCallback != null) {
            Log.i(TAG, "Pairing requested from: " + device.getAddress());
            unityCallback.onPairingRequested(device.getAddress(), variant);
        }
        
        // Auto-accept pairing requests by default
        try {
            // Access the BleHidManager directly from BleHidPlugin
            com.example.blehid.unity.BleHidPlugin.autoAcceptPairing(device);
        } catch (Exception e) {
            Log.e(TAG, "Error auto-accepting pairing request", e);
        }
    }
    
    @Override
    public void onPairingComplete(BluetoothDevice device, boolean success) {
        if (unityCallback != null) {
            if (success) {
                Log.i(TAG, "Pairing successful with: " + device.getAddress());
                unityCallback.onDeviceConnected(device.getAddress());
            } else {
                Log.e(TAG, "Pairing failed with: " + device.getAddress());
                unityCallback.onPairingFailed(device.getAddress());
            }
        }
    }
}
