package com.example.blehid.core;

import java.util.UUID;

/**
 * Constants for HID Media Player functionality.
 * Contains UUIDs, report descriptors, and other constants.
 */
public class HidMediaConstants {
    // Service UUIDs - same as HID Mouse
    public static final UUID HID_SERVICE_UUID = 
            UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");
    
    // Characteristic UUIDs - same as HID Mouse
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
    
    // Descriptor UUIDs - same as HID Mouse
    public static final UUID CLIENT_CONFIG_UUID = 
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); // CCCD
    public static final UUID REPORT_REFERENCE_UUID = 
            UUID.fromString("00002908-0000-1000-8000-00805f9b34fb");
    
    // HID Information - same as HID Mouse
    // [bcdHID, bCountryCode, flags]
    // bcdHID: 0x0111 (HID version 1.11)
    // bCountryCode: 0x00 (Not localized)
    // flags: 0x03 (Remote wake + normally connectable)
    public static final byte[] HID_INFORMATION = {0x11, 0x01, 0x00, 0x03};
    
    // Protocol Mode
    public static final byte PROTOCOL_MODE_REPORT = 0x01;
    
    // Media control constants (first byte of report)
    public static final int BUTTON_PLAY_PAUSE = 0x01;
    public static final int BUTTON_NEXT_TRACK = 0x02;
    public static final int BUTTON_PREVIOUS_TRACK = 0x04;
    public static final int BUTTON_VOLUME_UP = 0x08;
    public static final int BUTTON_VOLUME_DOWN = 0x10;
    public static final int BUTTON_MUTE = 0x20;
    
    // Mouse button constants (second byte of report)
    public static final int BUTTON_LEFT = 0x01;
    public static final int BUTTON_RIGHT = 0x02;
    public static final int BUTTON_MIDDLE = 0x04;
    
    // Combined HID Report Map descriptor for both consumer control (media player) and mouse
    public static final byte[] REPORT_MAP = new byte[] {
        // === First part: Consumer Controls ===
        // USAGE_PAGE (Consumer)
        (byte)0x05, (byte)0x0C,
        // USAGE (Consumer Control)
        (byte)0x09, (byte)0x01,
        // COLLECTION (Application)
        (byte)0xA1, (byte)0x01,
        
        // LOGICAL_MINIMUM (0)
        (byte)0x15, (byte)0x00,
        // LOGICAL_MAXIMUM (1)
        (byte)0x25, (byte)0x01,
        // REPORT_SIZE (1)
        (byte)0x75, (byte)0x01,
        // REPORT_COUNT (6)
        (byte)0x95, (byte)0x06,
        
        // USAGE (Play/Pause)
        (byte)0x09, (byte)0xCD,
        // USAGE (Next Track)
        (byte)0x09, (byte)0xB5,
        // USAGE (Previous Track)
        (byte)0x09, (byte)0xB6,
        // USAGE (Volume Increment)
        (byte)0x09, (byte)0xE9,
        // USAGE (Volume Decrement)
        (byte)0x09, (byte)0xEA,
        // USAGE (Mute)
        (byte)0x09, (byte)0xE2,
        // INPUT (Data,Var,Abs)
        (byte)0x81, (byte)0x02,
        
        // REPORT_COUNT (2)
        (byte)0x95, (byte)0x02,
        // INPUT (Const,Var,Abs)
        (byte)0x81, (byte)0x01,
        
        // === Second part: Mouse Controls ===
        // USAGE_PAGE (Generic Desktop)
        (byte)0x05, (byte)0x01,
        // USAGE (Mouse)
        (byte)0x09, (byte)0x02,
        // COLLECTION (Application)
        (byte)0xA1, (byte)0x01,
        // USAGE (Pointer)
        (byte)0x09, (byte)0x01,
        // COLLECTION (Physical)
        (byte)0xA1, (byte)0x00,
        
        // Buttons (3 buttons: left, right, middle)
        // USAGE_PAGE (Button)
        (byte)0x05, (byte)0x09,
        // USAGE_MINIMUM (Button 1)
        (byte)0x19, (byte)0x01,
        // USAGE_MAXIMUM (Button 3)
        (byte)0x29, (byte)0x03,
        // LOGICAL_MINIMUM (0)
        (byte)0x15, (byte)0x00,
        // LOGICAL_MAXIMUM (1)
        (byte)0x25, (byte)0x01,
        // REPORT_COUNT (3)
        (byte)0x95, (byte)0x03,
        // REPORT_SIZE (1)
        (byte)0x75, (byte)0x01,
        // INPUT (Data,Var,Abs)
        (byte)0x81, (byte)0x02,
        
        // Reserved padding (5 bits)
        // REPORT_COUNT (1)
        (byte)0x95, (byte)0x01,
        // REPORT_SIZE (5)
        (byte)0x75, (byte)0x05,
        // INPUT (Const,Array,Abs) - Padding
        (byte)0x81, (byte)0x01,
        
        // X and Y movement (-127 to 127 range)
        // USAGE_PAGE (Generic Desktop)
        (byte)0x05, (byte)0x01,
        // USAGE (X)
        (byte)0x09, (byte)0x30,
        // USAGE (Y)
        (byte)0x09, (byte)0x31,
        // LOGICAL_MINIMUM (-127)
        (byte)0x15, (byte)0x81,
        // LOGICAL_MAXIMUM (127)
        (byte)0x25, (byte)0x7F,
        // REPORT_SIZE (8)
        (byte)0x75, (byte)0x08,
        // REPORT_COUNT (2)
        (byte)0x95, (byte)0x02,
        // INPUT (Data,Var,Rel)
        (byte)0x81, (byte)0x06,
        
        // END_COLLECTION (Physical)
        (byte)0xC0,
        // END_COLLECTION (Application)
        (byte)0xC0,
        
        // END_COLLECTION (Consumer Control Application)
        (byte)0xC0
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
