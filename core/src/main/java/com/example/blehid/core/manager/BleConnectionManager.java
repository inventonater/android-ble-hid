package com.example.blehid.core.manager;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import com.example.blehid.core.IPairingManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages BLE device connections and bond states.
 * This class is responsible for connection state changes, pairing, and bond management.
 */
public class BleConnectionManager {
    private static final String TAG = "BleConnectionManager";
    
    private final BleLifecycleManager lifecycleManager;
    private final List<ConnectionStateCallback> connectionCallbacks = new ArrayList<>();
    private BluetoothDevice connectedDevice = null;
    private IPairingManager pairingManager;
    
    /**
     * Creates a new BLE connection manager.
     *
     * @param lifecycleManager The BLE lifecycle manager
     */
    public BleConnectionManager(BleLifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }
    
    /**
     * Sets the pairing manager.
     *
     * @param pairingManager The pairing manager
     */
    public void setPairingManager(IPairingManager pairingManager) {
        this.pairingManager = pairingManager;
    }
    
    /**
     * Registers a connection state callback.
     *
     * @param callback The callback to register
     */
    public void registerConnectionCallback(ConnectionStateCallback callback) {
        if (callback != null && !connectionCallbacks.contains(callback)) {
            connectionCallbacks.add(callback);
        }
    }
    
    /**
     * Unregisters a connection state callback.
     *
     * @param callback The callback to unregister
     */
    public void unregisterConnectionCallback(ConnectionStateCallback callback) {
        connectionCallbacks.remove(callback);
    }
    
    /**
     * Called when a device connects.
     *
     * @param device The connected device
     */
    public void onDeviceConnected(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "Null device connected");
            return;
        }
        
        Log.i(TAG, "Device connected: " + device.getAddress());
        connectedDevice = device;
        
        // If we require bonding and the device is not bonded, initiate pairing
        if (lifecycleManager.getConfig().getDeviceConfig().isRequireBonding() && 
                device.getBondState() != BluetoothDevice.BOND_BONDED) {
            if (pairingManager != null) {
                pairingManager.createBond(device);
            } else {
                Log.w(TAG, "Pairing manager not set, cannot initiate pairing");
            }
        }
        
        // Notify callbacks
        for (ConnectionStateCallback callback : new ArrayList<>(connectionCallbacks)) {
            callback.onDeviceConnected(device);
        }
    }
    
    /**
     * Called when a device disconnects.
     *
     * @param device The disconnected device
     */
    public void onDeviceDisconnected(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "Null device disconnected");
            return;
        }
        
        Log.i(TAG, "Device disconnected: " + device.getAddress());
        
        // Get the current bond state
        int bondState = device.getBondState();
        
        // Only keep reference for fully bonded devices that are not in the pairing process
        boolean keepReference = false;
        
        // Check if this is a fully bonded device AND is not in the pairing process
        if (bondState == BluetoothDevice.BOND_BONDED && pairingManager != null) {
            IPairingManager.PairingState pairingState = pairingManager.getPairingState();
            
            // Only keep the reference if we're not actively pairing/unpairing
            if (pairingState == IPairingManager.PairingState.IDLE || 
                pairingState == IPairingManager.PairingState.BONDED) {
                keepReference = true;
            }
        }
        
        if (keepReference) {
            Log.d(TAG, "Keeping bonded device reference for HID reports");
            // Keep the device reference to allow continued HID report sending
        } else if (connectedDevice != null && connectedDevice.equals(device)) {
            Log.d(TAG, "Clearing device reference (bond state: " + bondState + ")");
            connectedDevice = null;
        }
        
        // Notify callbacks
        for (ConnectionStateCallback callback : new ArrayList<>(connectionCallbacks)) {
            callback.onDeviceDisconnected(device);
        }
        
        // If auto-start advertising is enabled and we don't have a connected device, restart advertising
        if (connectedDevice == null && lifecycleManager.getConfig().getConnectionConfig().isAutoStartAdvertising()) {
            BleAdvertisingManager advertisingManager = lifecycleManager.getAdvertisingManager();
            if (advertisingManager != null && !advertisingManager.isAdvertising()) {
                advertisingManager.startAdvertising();
            }
        }
    }
    
    /**
     * Called when a device bond state changes.
     *
     * @param device The device
     * @param bondState The new bond state
     * @param previousBondState The previous bond state
     */
    public void onBondStateChanged(BluetoothDevice device, int bondState, int previousBondState) {
        if (device == null) {
            return;
        }
        
        Log.d(TAG, "Bond state changed for " + device.getAddress() + 
              " from " + getBondStateName(previousBondState) + 
              " to " + getBondStateName(bondState));
        
        // Notify callbacks
        for (ConnectionStateCallback callback : new ArrayList<>(connectionCallbacks)) {
            if (callback instanceof BondStateCallback) {
                ((BondStateCallback) callback).onBondStateChanged(device, bondState, previousBondState);
            }
        }
        
        // If bonding was successful and this is our connected device, notify
        if (bondState == BluetoothDevice.BOND_BONDED && 
                connectedDevice != null && connectedDevice.equals(device)) {
            Log.i(TAG, "Device bonded successfully: " + device.getAddress());
            for (ConnectionStateCallback callback : new ArrayList<>(connectionCallbacks)) {
                if (callback instanceof BondStateCallback) {
                    ((BondStateCallback) callback).onBondingSuccessful(device);
                }
            }
        }
        
        // If bonding failed, handle failure
        if (previousBondState == BluetoothDevice.BOND_BONDING && 
                bondState == BluetoothDevice.BOND_NONE) {
            Log.e(TAG, "Bonding failed for device: " + device.getAddress());
            for (ConnectionStateCallback callback : new ArrayList<>(connectionCallbacks)) {
                if (callback instanceof BondStateCallback) {
                    ((BondStateCallback) callback).onBondingFailed(device);
                }
            }
        }
    }
    
    /**
     * Gets the connected device.
     *
     * @return The connected device, or null if not connected
     */
    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
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
     * Disconnects the current device.
     *
     * @return true if disconnection was initiated, false otherwise
     */
    public boolean disconnect() {
        if (connectedDevice == null) {
            Log.d(TAG, "No device to disconnect");
            return false;
        }
        
        // In peripheral mode, we can't directly disconnect, but we can notify about disconnection
        Log.d(TAG, "Simulating device disconnection for: " + connectedDevice.getAddress());
        BluetoothDevice device = connectedDevice;
        connectedDevice = null;
        
        // Notify callbacks
        for (ConnectionStateCallback callback : new ArrayList<>(connectionCallbacks)) {
            callback.onDeviceDisconnected(device);
        }
        
        return true;
    }
    
    /**
     * Returns a string representation of a bond state.
     *
     * @param bondState The bond state
     * @return A string representing the bond state
     */
    private String getBondStateName(int bondState) {
        switch (bondState) {
            case BluetoothDevice.BOND_NONE:
                return "BOND_NONE";
            case BluetoothDevice.BOND_BONDING:
                return "BOND_BONDING";
            case BluetoothDevice.BOND_BONDED:
                return "BOND_BONDED";
            default:
                return "UNKNOWN (" + bondState + ")";
        }
    }
    
    /**
     * Cleans up resources.
     */
    public void close() {
        disconnect();
        connectionCallbacks.clear();
        pairingManager = null;
    }
    
    /**
     * Connection state callback interface.
     */
    public interface ConnectionStateCallback {
        /**
         * Called when a device connects.
         *
         * @param device The connected device
         */
        void onDeviceConnected(BluetoothDevice device);
        
        /**
         * Called when a device disconnects.
         *
         * @param device The disconnected device
         */
        void onDeviceDisconnected(BluetoothDevice device);
    }
    
    /**
     * Bond state callback interface.
     */
    public interface BondStateCallback extends ConnectionStateCallback {
        /**
         * Called when a device bond state changes.
         *
         * @param device The device
         * @param bondState The new bond state
         * @param previousBondState The previous bond state
         */
        void onBondStateChanged(BluetoothDevice device, int bondState, int previousBondState);
        
        /**
         * Called when bonding is successful.
         *
         * @param device The bonded device
         */
        void onBondingSuccessful(BluetoothDevice device);
        
        /**
         * Called when bonding fails.
         *
         * @param device The device that failed to bond
         */
        void onBondingFailed(BluetoothDevice device);
    }
}
