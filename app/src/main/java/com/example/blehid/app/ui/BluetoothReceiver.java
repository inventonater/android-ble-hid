package com.example.blehid.app.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * BroadcastReceiver for Bluetooth state changes.
 * Handles events like device discovery, pairing, and connection state changes.
 */
public class BluetoothReceiver extends BroadcastReceiver {
    
    public interface Callback {
        void onBluetoothStateChanged(int state);
        void onScanModeChanged(int mode);
        void onDeviceFound(BluetoothDevice device);
        void onBondStateChanged(BluetoothDevice device, int prevState, int newState);
        void onDeviceConnected(BluetoothDevice device);
        void onDeviceDisconnectRequested(BluetoothDevice device);
        void onDeviceDisconnected(BluetoothDevice device);
        void onPairingRequest(BluetoothDevice device, int variant);
        void logEvent(String message);
    }
    
    private final Callback callback;
    
    public BluetoothReceiver(Callback callback) {
        this.callback = callback;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        String deviceInfo = device != null ? 
                (device.getName() != null ? device.getName() : "unnamed") + 
                " (" + device.getAddress() + ")" : "unknown device";
        
        switch (action) {
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                callback.onBluetoothStateChanged(state);
                
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        callback.logEvent("BLUETOOTH: Turned OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        callback.logEvent("BLUETOOTH: Turning OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        callback.logEvent("BLUETOOTH: Turned ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        callback.logEvent("BLUETOOTH: Turning ON");
                        break;
                }
                break;
                
            case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                callback.onScanModeChanged(scanMode);
                
                switch (scanMode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        callback.logEvent("BLUETOOTH: Discoverable");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        callback.logEvent("BLUETOOTH: Connectable but not discoverable");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        callback.logEvent("BLUETOOTH: Not connectable or discoverable");
                        break;
                }
                break;
                
            case BluetoothDevice.ACTION_FOUND:
                callback.onDeviceFound(device);
                callback.logEvent("DEVICE FOUND: " + deviceInfo);
                break;
                
            case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                int prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                callback.onBondStateChanged(device, prevBondState, bondState);
                
                String bondStateStr = bondStateToString(bondState);
                String prevBondStateStr = bondStateToString(prevBondState);
                callback.logEvent("BOND STATE: " + deviceInfo + " " + prevBondStateStr + " -> " + bondStateStr);
                break;
                
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                callback.onDeviceConnected(device);
                callback.logEvent("ACL CONNECTED: " + deviceInfo);
                break;
                
            case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED:
                callback.onDeviceDisconnectRequested(device);
                callback.logEvent("ACL DISCONNECT REQUESTED: " + deviceInfo);
                break;
                
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                callback.onDeviceDisconnected(device);
                callback.logEvent("ACL DISCONNECTED: " + deviceInfo);
                break;
                
            case BluetoothDevice.ACTION_PAIRING_REQUEST:
                int variant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
                callback.onPairingRequest(device, variant);
                callback.logEvent("PAIRING REQUEST: " + deviceInfo + ", variant: " + variant);
                break;
        }
    }
    
    /**
     * Convert a Bluetooth bond state to a readable string.
     */
    public static String bondStateToString(int bondState) {
        switch (bondState) {
            case BluetoothDevice.BOND_NONE:
                return "BOND_NONE";
            case BluetoothDevice.BOND_BONDING:
                return "BOND_BONDING";
            case BluetoothDevice.BOND_BONDED:
                return "BOND_BONDED";
            default:
                return "UNKNOWN(" + bondState + ")";
        }
    }
}
