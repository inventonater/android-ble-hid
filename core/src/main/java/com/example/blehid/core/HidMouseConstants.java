package com.example.blehid.core;

import java.util.UUID;

/**
 * Constants for HID Mouse functionality.
 * Contains UUIDs, report descriptors, and other constants.
 */
public class HidMouseConstants {
    // Service UUIDs
    public static final UUID HID_SERVICE_UUID = 
            UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");
    
    // Characteristic UUIDs
    public static final UUID HID_INFORMATION_UUID = 
            UUID.fromString("00002A4A-0000-1000-8000-00805f9b34fb");
    public static final UUID HID_CONTROL_POINT_UUID = 
            UUID.fromString("00002A4C-0000-1000-8000-00805f9b34fb");
    public static final UUID HID_REPORT_MAP_UUID = 
            UUID.fromString("00002A4B-0000-1000-8000-00805f9b34fb");
    public static final UUID HID_REPORT_UUID = 
            UUID.fromString("00002A4D-0000-1000-8000-00805f9b34fb");
    public static final UUID HID_PROTOCOL_MODE_UUID = 
            UUID.fromString("00002A4E-0000-1000-8000-00805f9b34fb");
    public static final UUID HID_BOOT_MOUSE_INPUT_REPORT_UUID = 
            UUID.fromString("00002A33-0000-1000-8000-00805f9b34fb");
    
    // Descriptor UUIDs
    public static final UUID CLIENT_CONFIG_UUID = 
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); // CCCD
    public static final UUID REPORT_REFERENCE_UUID = 
            UUID.fromString("00002908-0000-1000-8000-00805f9b34fb");
    
    // HID Information
    // [bcdHID, bCountryCode, flags]
    // bcdHID: 0x0111 (HID version 1.11)
    // bCountryCode: 0x00 (Not localized)
    // flags: 0x03 (Remote wake + normally connectable)
    public static final byte[] HID_INFORMATION = {0x11, 0x01, 0x00, 0x03};
    
    // Protocol Mode
    public static final byte PROTOCOL_MODE_BOOT = 0x00;
    public static final byte PROTOCOL_MODE_REPORT = 0x01;
    
    // Mouse button constants
    public static final int BUTTON_LEFT = 0x01;
    public static final int BUTTON_RIGHT = 0x02;
    public static final int BUTTON_MIDDLE = 0x04;
    
    // Report IDs
    public static final byte REPORT_ID_MOUSE = 0x01;
    public static final byte REPORT_ID_KEYBOARD = 0x02;
    public static final byte REPORT_ID_CONSUMER = 0x03;
    
    // Combined HID Report Map descriptor for all interfaces (using Report IDs)
    public static final byte[] REPORT_MAP = new byte[] {
        // Mouse (Report ID 1)
        (byte)0x05, (byte)0x01,        // Usage Page (Generic Desktop)
        (byte)0x09, (byte)0x02,        // Usage (Mouse)
        (byte)0xA1, (byte)0x01,        // Collection (Application)
        (byte)0x85, (byte)0x01,        // Report ID (1) - MOUSE
        (byte)0x09, (byte)0x01,        // Usage (Pointer)
        (byte)0xA1, (byte)0x00,        // Collection (Physical)
        
        // Buttons (3 buttons: left, right, middle)
        (byte)0x05, (byte)0x09,        // Usage Page (Button)
        (byte)0x19, (byte)0x01,        // Usage Minimum (Button 1)
        (byte)0x29, (byte)0x03,        // Usage Maximum (Button 3)
        (byte)0x15, (byte)0x00,        // Logical Minimum (0)
        (byte)0x25, (byte)0x01,        // Logical Maximum (1)
        (byte)0x95, (byte)0x03,        // Report Count (3)
        (byte)0x75, (byte)0x01,        // Report Size (1)
        (byte)0x81, (byte)0x02,        // Input (Data, Var, Abs)
        
        // Reserved padding (5 bits)
        (byte)0x95, (byte)0x01,        // Report Count (1)
        (byte)0x75, (byte)0x05,        // Report Size (5)
        (byte)0x81, (byte)0x01,        // Input (Const, Array, Abs) - Padding
        
        // X, Y, and wheel movement (-127 to 127 range)
        (byte)0x05, (byte)0x01,        // Usage Page (Generic Desktop)
        (byte)0x09, (byte)0x30,        // Usage (X)
        (byte)0x09, (byte)0x31,        // Usage (Y)
        (byte)0x09, (byte)0x38,        // Usage (Wheel)
        (byte)0x15, (byte)0x81,        // Logical Minimum (-127)
        (byte)0x25, (byte)0x7F,        // Logical Maximum (127)
        (byte)0x75, (byte)0x08,        // Report Size (8)
        (byte)0x95, (byte)0x03,        // Report Count (3)
        (byte)0x81, (byte)0x06,        // Input (Data, Var, Rel)
        
        (byte)0xC0,                    // End Collection (Physical)
        (byte)0xC0,                    // End Collection (Application)
        
        // Keyboard (Report ID 2)
        (byte)0x05, (byte)0x01,        // Usage Page (Generic Desktop)
        (byte)0x09, (byte)0x06,        // Usage (Keyboard)
        (byte)0xA1, (byte)0x01,        // Collection (Application)
        (byte)0x85, (byte)0x02,        // Report ID (2) - KEYBOARD
        
        // Modifier keys (shift, ctrl, alt, etc)
        (byte)0x05, (byte)0x07,        // Usage Page (Key Codes)
        (byte)0x19, (byte)0xE0,        // Usage Minimum (224)
        (byte)0x29, (byte)0xE7,        // Usage Maximum (231)
        (byte)0x15, (byte)0x00,        // Logical Minimum (0)
        (byte)0x25, (byte)0x01,        // Logical Maximum (1)
        (byte)0x75, (byte)0x01,        // Report Size (1)
        (byte)0x95, (byte)0x08,        // Report Count (8)
        (byte)0x81, (byte)0x02,        // Input (Data, Var, Abs) - Modifier byte
        
        // Reserved byte
        (byte)0x95, (byte)0x01,        // Report Count (1)
        (byte)0x75, (byte)0x08,        // Report Size (8)
        (byte)0x81, (byte)0x01,        // Input (Const) - Reserved byte
        
        // Key array (6 keys)
        (byte)0x95, (byte)0x06,        // Report Count (6)
        (byte)0x75, (byte)0x08,        // Report Size (8)
        (byte)0x15, (byte)0x00,        // Logical Minimum (0)
        (byte)0x25, (byte)0x65,        // Logical Maximum (101)
        (byte)0x05, (byte)0x07,        // Usage Page (Key Codes)
        (byte)0x19, (byte)0x00,        // Usage Minimum (0)
        (byte)0x29, (byte)0x65,        // Usage Maximum (101)
        (byte)0x81, (byte)0x00,        // Input (Data, Array)
        
        (byte)0xC0,                    // End Collection
        
        // Consumer Control (Report ID 3)
        (byte)0x05, (byte)0x0C,        // Usage Page (Consumer)
        (byte)0x09, (byte)0x01,        // Usage (Consumer Control)
        (byte)0xA1, (byte)0x01,        // Collection (Application)
        (byte)0x85, (byte)0x03,        // Report ID (3) - CONSUMER
        
        // Consumer Control data (16-bit value)
        (byte)0x15, (byte)0x00,        // Logical Minimum (0)
        (byte)0x26, (byte)0xFF, (byte)0x03, // Logical Maximum (1023)
        (byte)0x19, (byte)0x00,        // Usage Minimum (0)
        (byte)0x2A, (byte)0xFF, (byte)0x03, // Usage Maximum (1023)
        (byte)0x75, (byte)0x10,        // Report Size (16)
        (byte)0x95, (byte)0x01,        // Report Count (1)
        (byte)0x81, (byte)0x00,        // Input (Data, Array)
        
        (byte)0xC0         // End Collection
    };
    
    /**
     * Converts a byte array to a hexadecimal string for logging.
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
}
