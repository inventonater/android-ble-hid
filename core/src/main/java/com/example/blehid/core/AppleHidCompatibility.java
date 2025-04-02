package com.example.blehid.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

/**
 * Special compatibility layer for Apple devices (macOS/iOS).
 * 
 * Apple devices have specific requirements for BLE HID devices
 * that differ from the standard specification. This class provides
 * the necessary adjustments to improve compatibility.
 */
public class AppleHidCompatibility {
    private static final String TAG = "AppleHidCompatibility";
    
    // Apple-specific service UUID used for identifying BLE HID devices
    // This is used alongside the standard HID service UUID
    private static final UUID APPLE_HID_IDENTIFIER_UUID = 
            UUID.fromString("9FA480E0-4967-4542-9390-D343DC5D04AE");
    
    // Report Map template adjusted for Apple compatibility
    // This is a modified HID descriptor that follows Apple's expected format
    private static final byte[] APPLE_COMPATIBLE_MOUSE_REPORT_MAP = {
        // Mouse
        (byte) 0x05, (byte) 0x01,                    // USAGE_PAGE (Generic Desktop)
        (byte) 0x09, (byte) 0x02,                    // USAGE (Mouse)
        (byte) 0xa1, (byte) 0x01,                    // COLLECTION (Application)
        (byte) 0x85, (byte) 0x01,                    //   REPORT_ID (1)
        (byte) 0x09, (byte) 0x01,                    //   USAGE (Pointer)
        (byte) 0xa1, (byte) 0x00,                    //   COLLECTION (Physical)
        
        // Buttons (1-3)
        (byte) 0x05, (byte) 0x09,                    //     USAGE_PAGE (Button)
        (byte) 0x19, (byte) 0x01,                    //     USAGE_MINIMUM (Button 1)
        (byte) 0x29, (byte) 0x03,                    //     USAGE_MAXIMUM (Button 3)
        (byte) 0x15, (byte) 0x00,                    //     LOGICAL_MINIMUM (0)
        (byte) 0x25, (byte) 0x01,                    //     LOGICAL_MAXIMUM (1)
        (byte) 0x75, (byte) 0x01,                    //     REPORT_SIZE (1)
        (byte) 0x95, (byte) 0x03,                    //     REPORT_COUNT (3)
        (byte) 0x81, (byte) 0x02,                    //     INPUT (Data, Variable, Absolute)
        
        // Reserved bits (5 bits) - must be 0
        (byte) 0x75, (byte) 0x05,                    //     REPORT_SIZE (5)
        (byte) 0x95, (byte) 0x01,                    //     REPORT_COUNT (1)
        (byte) 0x81, (byte) 0x01,                    //     INPUT (Constant) - CHANGED from 0x03 to 0x01
        
        // X and Y movement
        (byte) 0x05, (byte) 0x01,                    //     USAGE_PAGE (Generic Desktop)
        (byte) 0x09, (byte) 0x30,                    //     USAGE (X)
        (byte) 0x09, (byte) 0x31,                    //     USAGE (Y)
        (byte) 0x15, (byte) 0x81,                    //     LOGICAL_MINIMUM (-127)
        (byte) 0x25, (byte) 0x7f,                    //     LOGICAL_MAXIMUM (127)
        (byte) 0x75, (byte) 0x08,                    //     REPORT_SIZE (8)
        (byte) 0x95, (byte) 0x02,                    //     REPORT_COUNT (2)
        (byte) 0x81, (byte) 0x06,                    //     INPUT (Data, Variable, Relative)
        
        // Vertical wheel
        (byte) 0x09, (byte) 0x38,                    //     USAGE (Wheel)
        (byte) 0x15, (byte) 0x81,                    //     LOGICAL_MINIMUM (-127)
        (byte) 0x25, (byte) 0x7f,                    //     LOGICAL_MAXIMUM (127)
        (byte) 0x75, (byte) 0x08,                    //     REPORT_SIZE (8)
        (byte) 0x95, (byte) 0x01,                    //     REPORT_COUNT (1)
        (byte) 0x81, (byte) 0x06,                    //     INPUT (Data, Variable, Relative)
        
        // End collections
        (byte) 0xc0,                                 //   END_COLLECTION (Physical)
        (byte) 0xc0                                  // END_COLLECTION (Application)
    };
    
    // HID Information format for Apple devices
    // Version 1.1, country code 0, with RemoteWake and NormallyConnectable flags
    private static final byte[] APPLE_HID_INFORMATION = {
        (byte) 0x11, (byte) 0x01,     // bcdHID (HID spec version 1.1)
        (byte) 0x00,                  // bCountryCode (0 = not specified)
        (byte) 0x03                   // Flags (0x03 = RemoteWake + NormallyConnectable)
    };

    /**
     * Applies Apple-specific compatibility adjustments to HID services.
     * 
     * @param hidService The HID service to modify
     */
    public static void applyAppleCompatibility(BluetoothGattService hidService) {
        if (hidService == null) {
            Log.e(TAG, "Cannot apply Apple compatibility to null HID service");
            return;
        }
        
        try {
            // Update HID Information for Apple compatibility
            BluetoothGattCharacteristic hidInfoChar = 
                    hidService.getCharacteristic(UUID.fromString("00002A4A-0000-1000-8000-00805f9b34fb"));
            if (hidInfoChar != null) {
                hidInfoChar.setValue(APPLE_HID_INFORMATION);
                Log.d(TAG, "Updated HID Information for Apple compatibility");
            }
            
            // Update report map for Apple compatibility
            BluetoothGattCharacteristic reportMapChar = 
                    hidService.getCharacteristic(UUID.fromString("00002A4B-0000-1000-8000-00805f9b34fb"));
            if (reportMapChar != null) {
                reportMapChar.setValue(APPLE_COMPATIBLE_MOUSE_REPORT_MAP);
                Log.d(TAG, "Updated Report Map for Apple compatibility");
            }
            
            Log.i(TAG, "Applied Apple compatibility adjustments to HID service");
        } catch (Exception e) {
            Log.e(TAG, "Error applying Apple compatibility", e);
        }
    }
    
    /**
     * Sets a more Mac-friendly device name in the Bluetooth adapter.
     * 
     * @param bluetoothAdapter The Bluetooth adapter to modify
     */
    public static void setAppleFriendlyDeviceName(BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Cannot set name on null Bluetooth adapter");
            return;
        }
        
        try {
            // Use a name format that works better with macOS
            String deviceName = "Inventonater HID Mouse";
            bluetoothAdapter.setName(deviceName);
            Log.i(TAG, "Set Apple-friendly device name: " + deviceName);
        } catch (Exception e) {
            Log.e(TAG, "Error setting Apple-friendly device name", e);
        }
    }
    
    /**
     * Gets the Apple-compatible mouse report map.
     * 
     * @return The Apple-compatible report map
     */
    public static byte[] getAppleCompatibleMouseReportMap() {
        return APPLE_COMPATIBLE_MOUSE_REPORT_MAP;
    }
    
    /**
     * Gets the Apple-compatible HID information.
     * 
     * @return The Apple-compatible HID information
     */
    public static byte[] getAppleHidInformation() {
        return APPLE_HID_INFORMATION;
    }
}
