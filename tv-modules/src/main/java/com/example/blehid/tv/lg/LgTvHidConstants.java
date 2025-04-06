package com.example.blehid.tv.lg;

import java.util.UUID;

/**
 * Constants for LG Smart TV HID functionality.
 * Contains optimized button mappings and report descriptor for LG TVs.
 */
public class LgTvHidConstants {
    // Standard HID service and characteristic UUIDs
    public static final UUID HID_SERVICE_UUID = 
            UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");
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
    
    // Descriptor UUIDs
    public static final UUID CLIENT_CONFIG_UUID = 
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); // CCCD
    public static final UUID REPORT_REFERENCE_UUID = 
            UUID.fromString("00002908-0000-1000-8000-00805f9b34fb");
    
    // HID Information - same for most HID devices
    // [bcdHID, bCountryCode, flags]
    // bcdHID: 0x0111 (HID version 1.11)
    // bCountryCode: 0x00 (Not localized)
    // flags: 0x03 (Remote wake + normally connectable)
    public static final byte[] HID_INFORMATION = {0x11, 0x01, 0x00, 0x03};
    
    // Protocol Mode
    public static final byte PROTOCOL_MODE_REPORT = 0x01;
    
    // LG TV button bit positions - first byte of report
    // Bit mapping for directional controls, optimized for LG TV remote behavior
    public static final byte BUTTON_UP = 0x01;         // Bit 0
    public static final byte BUTTON_DOWN = 0x02;       // Bit 1
    public static final byte BUTTON_LEFT = 0x04;       // Bit 2
    public static final byte BUTTON_RIGHT = 0x08;      // Bit 3
    public static final byte BUTTON_SELECT = 0x10;     // Bit 4
    public static final byte BUTTON_BACK = 0x20;       // Bit 5
    public static final byte BUTTON_HOME = 0x40;       // Bit 6
    public static final byte BUTTON_MENU = (byte) 0x80; // Bit 7 (cast to byte because 0x80 is negative in two's complement)
    
    // Media button bit positions - part of the first byte, overlapping with directional controls
    // These are defined separately since they may be used in a separate context
    public static final byte BUTTON_PLAY_PAUSE = 0x01; // Same as UP
    public static final byte BUTTON_NEXT = 0x08;       // Same as RIGHT
    public static final byte BUTTON_PREVIOUS = 0x04;   // Same as LEFT
    public static final byte BUTTON_VOLUME_UP = 0x02;  // Same as DOWN
    public static final byte BUTTON_VOLUME_DOWN = 0x20; // Same as BACK
    public static final byte BUTTON_MUTE = 0x10;       // Same as SELECT
    
    // Optimized HID Report Map descriptor for LG TVs
    // Uses a single report format without report IDs for maximum compatibility
    public static final byte[] REPORT_MAP = new byte[] {
        // Usage Page (Generic Desktop)
        (byte)0x05, (byte)0x01,
        // Usage (Mouse)
        (byte)0x09, (byte)0x02,
        // Collection (Application)
        (byte)0xA1, (byte)0x01,
            // Usage (Pointer)
            (byte)0x09, (byte)0x01,
            // Collection (Physical)
            (byte)0xA1, (byte)0x00,
                
                // First byte: Buttons
                // Usage Page (Button)
                (byte)0x05, (byte)0x09,
                // Usage Minimum (Button 1)
                (byte)0x19, (byte)0x01,
                // Usage Maximum (Button 8)
                (byte)0x29, (byte)0x08,
                // Logical Minimum (0)
                (byte)0x15, (byte)0x00,
                // Logical Maximum (1)
                (byte)0x25, (byte)0x01,
                // Report Count (8)
                (byte)0x95, (byte)0x08,
                // Report Size (1)
                (byte)0x75, (byte)0x01,
                // Input (Data, Variable, Absolute)
                (byte)0x81, (byte)0x02,
                
                // X and Y movement (relative pointer)
                // Usage Page (Generic Desktop)
                (byte)0x05, (byte)0x01,
                // Usage (X)
                (byte)0x09, (byte)0x30,
                // Usage (Y)
                (byte)0x09, (byte)0x31,
                // Logical Minimum (-127)
                (byte)0x15, (byte)0x81,
                // Logical Maximum (127)
                (byte)0x25, (byte)0x7F,
                // Report Size (8)
                (byte)0x75, (byte)0x08,
                // Report Count (2)
                (byte)0x95, (byte)0x02,
                // Input (Data, Variable, Relative)
                (byte)0x81, (byte)0x06,
                
            // End Collection (Physical)
            (byte)0xC0,
        // End Collection (Application)
        (byte)0xC0
    };
    
    /**
     * Converts byte array to hex string for logging purposes.
     * 
     * @param bytes The byte array to convert
     * @return The hex string representation
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
