package com.example.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Utility class for common Bluetooth operations and conversions.
 * Centralizes shared code used across the BLE HID implementation.
 */
public class BluetoothUtils {
    private static final String TAG = "BluetoothUtils";
    
    // Pairing variants
    public static final int PAIRING_VARIANT_PIN = 0;
    public static final int PAIRING_VARIANT_PASSKEY = 1;
    public static final int PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2;
    public static final int PAIRING_VARIANT_CONSENT = 3;
    public static final int PAIRING_VARIANT_DISPLAY_PASSKEY = 4;
    public static final int PAIRING_VARIANT_DISPLAY_PIN = 5;
    public static final int PAIRING_VARIANT_OOB_CONSENT = 6;
    
    /**
     * Converts a bond state integer to a string for logging.
     * 
     * @param bondState The bond state to convert
     * @return The bond state as a string
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
    
    /**
     * Converts a pairing variant integer to a string for logging.
     * 
     * @param variant The pairing variant to convert
     * @return The pairing variant as a string
     */
    public static String pairingVariantToString(int variant) {
        switch (variant) {
            case PAIRING_VARIANT_PIN:
                return "PIN";
            case PAIRING_VARIANT_PASSKEY:
                return "PASSKEY";
            case PAIRING_VARIANT_PASSKEY_CONFIRMATION:
                return "PASSKEY_CONFIRMATION";
            case PAIRING_VARIANT_CONSENT:
                return "CONSENT (Just Works)";
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
     * Get a human-readable string describing a device.
     * 
     * @param device The device to describe
     * @return A description string
     */
    public static String getDeviceInfo(BluetoothDevice device) {
        if (device == null) {
            return "Unknown device";
        }
        
        return device.getName() + " (" + device.getAddress() + ")";
    }
    
    /**
     * Extract BluetoothDevice from intent safely, handling API level differences.
     * 
     * @param intent The intent containing the BluetoothDevice
     * @return The BluetoothDevice or null if not found
     */
    public static BluetoothDevice getBluetoothDeviceFromIntent(Intent intent) {
        if (intent == null) {
            return null;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
        } else {
            return intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        }
    }
    
    /**
     * Creates a bond with a device, handling potential exceptions.
     * 
     * @param device The device to create bond with
     * @return true if bond creation was initiated, false otherwise
     */
    public static boolean createBond(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "Cannot create bond with null device");
            return false;
        }
        
        try {
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.i(TAG, "Device already bonded: " + getDeviceInfo(device));
                return true;
            }
            
            boolean result = device.createBond();
            Log.d(TAG, "Create bond result for " + getDeviceInfo(device) + ": " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error creating bond", e);
            return false;
        }
    }
    
    /**
     * Removes a bond with a device using reflection.
     * 
     * @param device The device to remove bond with
     * @return true if bond removal was initiated, false otherwise
     */
    public static boolean removeBond(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "Cannot remove bond with null device");
            return false;
        }
        
        try {
            if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                Log.i(TAG, "Device already not bonded: " + getDeviceInfo(device));
                return true;
            }
            
            // Using reflection to call removeBond method
            java.lang.reflect.Method method = device.getClass().getMethod("removeBond");
            boolean result = (Boolean) method.invoke(device);
            Log.d(TAG, "Remove bond result for " + getDeviceInfo(device) + ": " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error removing bond", e);
            return false;
        }
    }
    
    /**
     * Sets the PIN code for pairing using reflection.
     * 
     * @param device The device to set PIN for
     * @param pin The PIN code
     * @return true if PIN was set, false otherwise
     */
    public static boolean setPin(BluetoothDevice device, String pin) {
        if (device == null || pin == null) {
            Log.e(TAG, "Invalid device or PIN");
            return false;
        }
        
        try {
            byte[] pinBytes = pin.getBytes();
            java.lang.reflect.Method method = device.getClass().getMethod("setPin", byte[].class);
            boolean result = (Boolean) method.invoke(device, pinBytes);
            Log.d(TAG, "Set PIN result: " + result + " for " + getDeviceInfo(device));
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error setting PIN", e);
            return false;
        }
    }
    
    /**
     * Confirms a pairing request using reflection.
     * 
     * @param device The device to confirm pairing with
     * @param confirm true to accept, false to reject
     * @return true if confirmation was sent, false otherwise
     */
    public static boolean setPairingConfirmation(BluetoothDevice device, boolean confirm) {
        if (device == null) {
            Log.e(TAG, "Cannot set pairing confirmation for null device");
            return false;
        }
        
        try {
            java.lang.reflect.Method method = device.getClass().getMethod("setPairingConfirmation", boolean.class);
            boolean result = (Boolean) method.invoke(device, confirm);
            Log.d(TAG, "Set pairing confirmation result: " + result + " for " + getDeviceInfo(device));
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error setting pairing confirmation", e);
            return false;
        }
    }
    
    /**
     * Sets a passkey for pairing using reflection.
     * 
     * @param device The device to set passkey for
     * @param passkey The numeric passkey
     * @param confirm Whether to auto-confirm
     * @return true if passkey was set, false otherwise
     */
    public static boolean setPasskey(BluetoothDevice device, int passkey, boolean confirm) {
        if (device == null) {
            Log.e(TAG, "Cannot set passkey for null device");
            return false;
        }
        
        try {
            java.lang.reflect.Method method = device.getClass().getMethod("setPasskey", int.class, boolean.class);
            boolean result = (Boolean) method.invoke(device, passkey, confirm);
            Log.d(TAG, "Set passkey result: " + result + " for " + getDeviceInfo(device));
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error setting passkey", e);
            return false;
        }
    }
}
