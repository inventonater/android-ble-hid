package com.example.blehid.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

/**
 * Handles Bluetooth pairing and bonding operations for the BLE HID device.
 */
public class BlePairingManager {
    private static final String TAG = "BlePairingManager";
    
    private final BleHidManager bleHidManager;
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    
    private boolean isRegistered = false;
    private PairingCallback pairingCallback = null;
    
    /**
     * Callback interface for pairing events.
     */
    public interface PairingCallback {
        /**
         * Called when a device requests pairing.
         * 
         * @param device The device requesting pairing
         * @param variant The pairing variant (PIN, passkey, etc.)
         */
        void onPairingRequested(BluetoothDevice device, int variant);
        
        /**
         * Called when the pairing process is complete.
         * 
         * @param device The paired device
         * @param success Whether pairing was successful
         */
        void onPairingComplete(BluetoothDevice device, boolean success);
    }
    
    /**
     * BroadcastReceiver for Bluetooth pairing events.
     */
    private final BroadcastReceiver bondStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                int prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
                
                handleBondStateChanged(device, prevBondState, bondState);
            } 
            else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int variant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
                
                handlePairingRequest(device, variant);
            }
        }
    };
    
    /**
     * Creates a new BLE pairing manager.
     * 
     * @param bleHidManager The parent BLE HID manager
     */
    public BlePairingManager(BleHidManager bleHidManager) {
        this.bleHidManager = bleHidManager;
        this.context = bleHidManager.getContext();
        this.bluetoothAdapter = bleHidManager.getBluetoothAdapter();
    }
    
    /**
     * Registers the bond state receiver to monitor pairing events.
     */
    public void registerReceiver() {
        if (isRegistered) {
            return;
        }
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        
        context.registerReceiver(bondStateReceiver, filter);
        isRegistered = true;
        Log.d(TAG, "Pairing broadcast receiver registered");
    }
    
    /**
     * Unregisters the bond state receiver.
     */
    public void unregisterReceiver() {
        if (!isRegistered) {
            return;
        }
        
        try {
            context.unregisterReceiver(bondStateReceiver);
            isRegistered = false;
            Log.d(TAG, "Pairing broadcast receiver unregistered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to unregister receiver", e);
        }
    }
    
    /**
     * Sets a callback for pairing events.
     * 
     * @param callback The callback to set, or null to remove
     */
    public void setPairingCallback(PairingCallback callback) {
        this.pairingCallback = callback;
        
        // Register receiver if callback set, unregister if removed
        if (callback != null) {
            registerReceiver();
        } else {
            unregisterReceiver();
        }
    }
    
    /**
     * Handles bond state changes.
     * 
     * @param device The device whose bond state changed
     * @param previousState The previous bond state
     * @param newState The new bond state
     */
    private void handleBondStateChanged(BluetoothDevice device, int previousState, int newState) {
        String deviceAddress = device != null ? device.getAddress() : "null";
        
        Log.d(TAG, "Bond state changed for " + deviceAddress + 
                ": " + bondStateToString(previousState) + 
                " -> " + bondStateToString(newState));
        
        if (newState == BluetoothDevice.BOND_BONDED) {
            Log.i(TAG, "Device bonded: " + deviceAddress);
            if (pairingCallback != null) {
                pairingCallback.onPairingComplete(device, true);
            }
        } 
        else if (previousState == BluetoothDevice.BOND_BONDING && 
                 newState == BluetoothDevice.BOND_NONE) {
            Log.w(TAG, "Bonding failed for device: " + deviceAddress);
            if (pairingCallback != null) {
                pairingCallback.onPairingComplete(device, false);
            }
        }
    }
    
    /**
     * Handles pairing requests.
     * 
     * @param device The device requesting pairing
     * @param variant The pairing variant type
     */
    private void handlePairingRequest(BluetoothDevice device, int variant) {
        String deviceAddress = device != null ? device.getAddress() : "null";
        Log.d(TAG, "Pairing request from " + deviceAddress + 
                ", variant: " + pairingVariantToString(variant));
        
        if (pairingCallback != null) {
            pairingCallback.onPairingRequested(device, variant);
        }
    }
    
    /**
     * Initiates pairing with a device.
     * 
     * @param device The device to pair with
     * @return true if pairing was initiated, false otherwise
     */
    public boolean createBond(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "Cannot create bond with null device");
            return false;
        }
        
        try {
            registerReceiver(); // Ensure we're registered to receive events
            
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.i(TAG, "Device already bonded: " + device.getAddress());
                return true;
            }
            
            boolean result = device.createBond();
            Log.d(TAG, "Create bond result for " + device.getAddress() + ": " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error creating bond", e);
            return false;
        }
    }
    
    /**
     * Removes pairing with a device.
     * 
     * @param device The device to unpair
     * @return true if unpairing was successful, false otherwise
     */
    public boolean removeBond(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "Cannot remove bond with null device");
            return false;
        }
        
        try {
            if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                Log.i(TAG, "Device already not bonded: " + device.getAddress());
                return true;
            }
            
            // Using reflection to call removeBond method
            java.lang.reflect.Method method = device.getClass().getMethod("removeBond");
            boolean result = (Boolean) method.invoke(device);
            Log.d(TAG, "Remove bond result for " + device.getAddress() + ": " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error removing bond", e);
            return false;
        }
    }
    
    /**
     * Sets the PIN code for pairing.
     * 
     * @param device The device to set PIN for
     * @param pin The PIN code as a string
     * @return true if the PIN was set, false otherwise
     */
    public boolean setPin(BluetoothDevice device, String pin) {
        if (device == null || pin == null) {
            return false;
        }
        
        try {
            byte[] pinBytes = pin.getBytes();
            java.lang.reflect.Method method = device.getClass().getMethod("setPin", byte[].class);
            boolean result = (Boolean) method.invoke(device, pinBytes);
            Log.d(TAG, "Set PIN result: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error setting PIN", e);
            return false;
        }
    }
    
    /**
     * Confirms a pairing request (for just-works or numeric comparison).
     * 
     * @param device The device to confirm pairing with
     * @param confirm true to accept, false to reject
     * @return true if confirmation was sent, false otherwise
     */
    public boolean setPairingConfirmation(BluetoothDevice device, boolean confirm) {
        if (device == null) {
            return false;
        }
        
        try {
            java.lang.reflect.Method method = device.getClass().getMethod("setPairingConfirmation", boolean.class);
            boolean result = (Boolean) method.invoke(device, confirm);
            Log.d(TAG, "Set pairing confirmation result: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error setting pairing confirmation", e);
            return false;
        }
    }
    
    /**
     * Sets a passkey for pairing.
     * 
     * @param device The device to set passkey for
     * @param passkey The numeric passkey
     * @param confirm Whether to auto-confirm the passkey
     * @return true if the passkey was set, false otherwise
     */
    public boolean setPasskey(BluetoothDevice device, int passkey, boolean confirm) {
        if (device == null) {
            return false;
        }
        
        try {
            java.lang.reflect.Method method = device.getClass().getMethod("setPasskey", int.class, boolean.class);
            boolean result = (Boolean) method.invoke(device, passkey, confirm);
            Log.d(TAG, "Set passkey result: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error setting passkey", e);
            return false;
        }
    }
    
    /**
     * Checks if a device is currently bonded/paired.
     * 
     * @param device The device to check
     * @return true if bonded, false otherwise
     */
    public boolean isBonded(BluetoothDevice device) {
        return device != null && device.getBondState() == BluetoothDevice.BOND_BONDED;
    }
    
    /**
     * Converts a bond state integer to a string for logging.
     * 
     * @param bondState The bond state to convert
     * @return The bond state as a string
     */
    private String bondStateToString(int bondState) {
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
    
    /**
     * Converts a pairing variant integer to a string for logging.
     * 
     * @param variant The pairing variant to convert
     * @return The pairing variant as a string
     */
    // Define our own pairing variant constants since some may not be publicly accessible
    private static final int PAIRING_VARIANT_PIN = 0;
    private static final int PAIRING_VARIANT_PASSKEY = 1;
    private static final int PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2;
    private static final int PAIRING_VARIANT_CONSENT = 3;
    private static final int PAIRING_VARIANT_DISPLAY_PASSKEY = 4;
    private static final int PAIRING_VARIANT_DISPLAY_PIN = 5;
    private static final int PAIRING_VARIANT_OOB_CONSENT = 6;
    
    private String pairingVariantToString(int variant) {
        switch (variant) {
            case PAIRING_VARIANT_PIN:
                return "PIN";
            case PAIRING_VARIANT_PASSKEY:
                return "PASSKEY";
            case PAIRING_VARIANT_PASSKEY_CONFIRMATION:
                return "PASSKEY_CONFIRMATION";
            case PAIRING_VARIANT_CONSENT:
                return "CONSENT";
            case PAIRING_VARIANT_DISPLAY_PASSKEY:
                return "DISPLAY_PASSKEY";
            case PAIRING_VARIANT_DISPLAY_PIN:
                return "DISPLAY_PIN";
            case PAIRING_VARIANT_OOB_CONSENT:
                return "OOB_CONSENT";
            default:
                return "UNKNOWN(" + variant + ")";
        }
    }
    
    /**
     * Cleans up resources when no longer needed.
     */
    public void close() {
        unregisterReceiver();
        pairingCallback = null;
    }
}
