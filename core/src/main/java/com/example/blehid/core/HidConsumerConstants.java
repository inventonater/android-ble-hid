package com.example.blehid.core;

import java.util.UUID;

/**
 * Constants for HID Consumer Control functionality.
 * Contains media keys, report descriptor, and other constants.
 */
public class HidConsumerConstants {
    // Use the standard UUIDs defined in HidMouseConstants
    
    // Media control bit positions
    public static final byte CONSUMER_MUTE         = 0x01; // bit 0
    public static final byte CONSUMER_VOLUME_UP    = 0x02; // bit 1
    public static final byte CONSUMER_VOLUME_DOWN  = 0x04; // bit 2
    public static final byte CONSUMER_PLAY_PAUSE   = 0x08; // bit 3
    public static final byte CONSUMER_NEXT_TRACK   = 0x10; // bit 4
    public static final byte CONSUMER_PREV_TRACK   = 0x20; // bit 5
    public static final byte CONSUMER_STOP         = 0x40; // bit 6
    // bit 7 is reserved/padding
    
    // Consumer control report is just 1 byte (bitmap of controls)
    public static final int CONSUMER_REPORT_SIZE = 1;
    
    // HID Report Map section for consumer controls
    // This is the consumer controls portion only, not the full report map
    public static final byte[] CONSUMER_REPORT_MAP = new byte[] {
        // Consumer Control (Media Keys)
        0x05, 0x0C,       // Usage Page (Consumer)
        0x09, 0x01,       // Usage (Consumer Control)
        (byte)0xA1, 0x01, // Collection (Application)
        
        // Media control buttons
        0x15, 0x00,       // Logical Minimum (0)
        0x25, 0x01,       // Logical Maximum (1)
        (byte)0x75, (byte)0x01,       // Report Size (1)
        (byte)0x95, (byte)0x07,       // Report Count (7)
        
        // Media key usage codes
        0x09, (byte)0xE2, // Usage (Mute)
        0x09, (byte)0xE9, // Usage (Volume Up)
        0x09, (byte)0xEA, // Usage (Volume Down)
        0x09, (byte)0xCD, // Usage (Play/Pause)
        0x09, (byte)0xB5, // Usage (Next Track)
        0x09, (byte)0xB6, // Usage (Previous Track)
        0x09, (byte)0xB7, // Usage (Stop)
        
        (byte)0x81, 0x02, // Input (Data, Variable, Absolute)
        
        // Reserved bit (padding)
        (byte)0x95, (byte)0x01,       // Report Count (1)
        (byte)0x75, (byte)0x01,       // Report Size (1)
        (byte)0x81, 0x01, // Input (Constant) - padding bit
        
        (byte)0xC0        // End Collection
    };
    
    // Full Usage ID values for reference (from USB HID Usage Tables)
    // These are not used directly in the bitmap format above, but are included for reference
    public static final short USAGE_MUTE         = 0x00E2;
    public static final short USAGE_VOLUME_UP    = 0x00E9;
    public static final short USAGE_VOLUME_DOWN  = 0x00EA;
    public static final short USAGE_PLAY_PAUSE   = 0x00CD;
    public static final short USAGE_NEXT_TRACK   = 0x00B5;
    public static final short USAGE_PREV_TRACK   = 0x00B6;
    public static final short USAGE_STOP         = 0x00B7;
    public static final short USAGE_FAST_FORWARD = 0x00B3;
    public static final short USAGE_REWIND       = 0x00B4;
    public static final short USAGE_SCAN_NEXT    = 0x00B8;
    public static final short USAGE_SCAN_PREV    = 0x00B9;
    public static final short USAGE_RANDOM_PLAY  = 0x00B9;
    public static final short USAGE_REPEAT       = 0x00BC;
}
