package com.inventonater.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

/**
 * Utility class for Bluetooth device information formatting and processing.
 * Provides helper methods for formatting device types, bond states, and other
 * device-related information into human-readable strings.
 */
public class BluetoothDeviceHelper {
    private static final String TAG = "BluetoothDeviceHelper";
    
    /**
     * Converts a BluetoothDevice type value to a human-readable string.
     *
     * @param type The device type constant from BluetoothDevice
     * @return A string representation of the device type
     */
    public static String getDeviceTypeString(int type) {
        switch (type) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                return "CLASSIC";
            case BluetoothDevice.DEVICE_TYPE_LE:
                return "LE";
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                return "DUAL";
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
            default:
                return "UNKNOWN";
        }
    }
    
    /**
     * Converts a BluetoothDevice bond state value to a human-readable string.
     *
     * @param bondState The bond state constant from BluetoothDevice
     * @return A string representation of the bond state
     */
    public static String getBondStateString(int bondState) {
        switch (bondState) {
            case BluetoothDevice.BOND_BONDED:
                return "BONDED";
            case BluetoothDevice.BOND_BONDING:
                return "BONDING";
            case BluetoothDevice.BOND_NONE:
            default:
                return "NONE";
        }
    }
    
    /**
     * Formats a BluetoothDevice's UUIDs into a comma-separated string.
     *
     * @param device The BluetoothDevice to get UUIDs from
     * @return A string with all UUIDs, or "None" if no UUIDs are available
     */
    public static String getDeviceUuidsString(BluetoothDevice device) {
        if (device == null) {
            return "None";
        }
        
        if (device.getUuids() == null || device.getUuids().length == 0) {
            return "None";
        }
        
        StringBuilder sb = new StringBuilder();
        for (android.os.ParcelUuid uuid : device.getUuids()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(uuid.toString());
        }
        
        return sb.toString();
    }
    
    /**
     * Creates a user-friendly device information string with name and address.
     *
     * @param device The BluetoothDevice
     * @return A formatted string with device name and address, or "null" if device is null
     */
    public static String getDeviceInfo(BluetoothDevice device) {
        if (device == null) {
            return "null";
        }
        
        String deviceName = device.getName();
        if (deviceName == null || deviceName.isEmpty()) {
            deviceName = "Unknown Device";
        }
        
        return deviceName + " (" + device.getAddress() + ")";
    }
    
    /**
     * Gets a readable device name, defaulting to "Unknown Device" if no name is available.
     *
     * @param device The BluetoothDevice
     * @return The device name or "Unknown Device" if name is null or empty
     */
    public static String getDeviceName(BluetoothDevice device) {
        if (device == null) {
            return "Unknown Device";
        }
        
        String deviceName = device.getName();
        if (deviceName == null || deviceName.isEmpty()) {
            return "Unknown Device";
        }
        
        return deviceName;
    }
    
    /**
     * Checks if a MAC address string is valid.
     *
     * @param address The MAC address to validate
     * @return true if the address is valid, false otherwise
     */
    public static boolean isValidBluetoothAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }
        
        // Standard Bluetooth MAC address format is 6 bytes in hexadecimal 
        // separated by colons (XX:XX:XX:XX:XX:XX)
        return address.matches("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}");
    }
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private BluetoothDeviceHelper() {
        // Utility class should not be instantiated
    }
}
