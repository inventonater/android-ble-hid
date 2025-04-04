package com.example.blehid.core;

import java.util.UUID;

/**
 * Consolidated HID constants for all device types.
 * This class provides a unified entry point for all HID-related constants
 * with nested static classes for better organization.
 */
public class HidConstants {
    // Service UUIDs
    public static final UUID HID_SERVICE_UUID = 
            UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");
    
    // Common Characteristic UUIDs
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
    
    // Report IDs
    public static final byte REPORT_ID_MOUSE = 0x01;
    public static final byte REPORT_ID_KEYBOARD = 0x02;
    public static final byte REPORT_ID_CONSUMER = 0x03;
    
    // Protocol Mode
    public static final byte PROTOCOL_MODE_BOOT = 0x00;
    public static final byte PROTOCOL_MODE_REPORT = 0x01;
    
    // HID Information
    // [bcdHID, bCountryCode, flags]
    // bcdHID: 0x0111 (HID version 1.11)
    // bCountryCode: 0x00 (Not localized)
    // flags: 0x03 (Remote wake + normally connectable)
    public static final byte[] HID_INFORMATION = {0x11, 0x01, 0x00, 0x03};
    
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
        
        (byte)0xC0                     // End Collection
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
    
    /**
     * Mouse-specific constants.
     */
    public static class Mouse {
        // Mouse button constants
        public static final int BUTTON_LEFT = 0x01;
        public static final int BUTTON_RIGHT = 0x02;
        public static final int BUTTON_MIDDLE = 0x04;
        
        // Boot Mouse Input Report UUID
        public static final UUID BOOT_MOUSE_INPUT_REPORT_UUID = 
                UUID.fromString("00002A33-0000-1000-8000-00805f9b34fb");
        
        // Mouse report map section (for reference only - use HidConstants.REPORT_MAP for actual implementation)
        public static final byte[] MOUSE_REPORT_MAP = new byte[] {
            // Mouse (Report ID 1)
            (byte)0x05, (byte)0x01,        // Usage Page (Generic Desktop)
            (byte)0x09, (byte)0x02,        // Usage (Mouse)
            (byte)0xA1, (byte)0x01,        // Collection (Application)
            (byte)0x85, (byte)0x01,        // Report ID (1)
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
            (byte)0xC0                     // End Collection (Application)
        };
    }
    
    /**
     * Keyboard-specific constants.
     */
    public static class Keyboard {
        // Keyboard modifier byte bit masks (byte 0 of report)
        public static final byte MODIFIER_LEFT_CTRL   = (byte)0x01;
        public static final byte MODIFIER_LEFT_SHIFT  = (byte)0x02;
        public static final byte MODIFIER_LEFT_ALT    = (byte)0x04;
        public static final byte MODIFIER_LEFT_GUI    = (byte)0x08;
        public static final byte MODIFIER_RIGHT_CTRL  = (byte)0x10;
        public static final byte MODIFIER_RIGHT_SHIFT = (byte)0x20;
        public static final byte MODIFIER_RIGHT_ALT   = (byte)0x40;
        public static final byte MODIFIER_RIGHT_GUI   = (byte)0x80;
        
        // Standard keyboard report size (8 bytes: modifiers, reserved, 6 keys)
        public static final int KEYBOARD_REPORT_SIZE = 8;
        
        // Maximum number of keys to report at once (6 is standard)
        public static final int MAX_KEYS = 6;
        
        // Key codes based on USB HID Usage Tables
        // Standard letter keys
        public static final byte KEY_A = 0x04;
        public static final byte KEY_B = 0x05;
        public static final byte KEY_C = 0x06;
        public static final byte KEY_D = 0x07;
        public static final byte KEY_E = 0x08;
        public static final byte KEY_F = 0x09;
        public static final byte KEY_G = 0x0A;
        public static final byte KEY_H = 0x0B;
        public static final byte KEY_I = 0x0C;
        public static final byte KEY_J = 0x0D;
        public static final byte KEY_K = 0x0E;
        public static final byte KEY_L = 0x0F;
        public static final byte KEY_M = 0x10;
        public static final byte KEY_N = 0x11;
        public static final byte KEY_O = 0x12;
        public static final byte KEY_P = 0x13;
        public static final byte KEY_Q = 0x14;
        public static final byte KEY_R = 0x15;
        public static final byte KEY_S = 0x16;
        public static final byte KEY_T = 0x17;
        public static final byte KEY_U = 0x18;
        public static final byte KEY_V = 0x19;
        public static final byte KEY_W = 0x1A;
        public static final byte KEY_X = 0x1B;
        public static final byte KEY_Y = 0x1C;
        public static final byte KEY_Z = 0x1D;
        
        // Number keys
        public static final byte KEY_1 = 0x1E;
        public static final byte KEY_2 = 0x1F;
        public static final byte KEY_3 = 0x20;
        public static final byte KEY_4 = 0x21;
        public static final byte KEY_5 = 0x22;
        public static final byte KEY_6 = 0x23;
        public static final byte KEY_7 = 0x24;
        public static final byte KEY_8 = 0x25;
        public static final byte KEY_9 = 0x26;
        public static final byte KEY_0 = 0x27;
        
        // Common control keys
        public static final byte KEY_RETURN = 0x28;  // Enter
        public static final byte KEY_ESCAPE = 0x29;  // Esc
        public static final byte KEY_BACKSPACE = 0x2A;
        public static final byte KEY_TAB = 0x2B;
        public static final byte KEY_SPACE = 0x2C;
        
        // Common punctuation
        public static final byte KEY_MINUS = 0x2D;  // - and _
        public static final byte KEY_EQUALS = 0x2E; // = and +
        public static final byte KEY_LBRACKET = 0x2F; // [ and {
        public static final byte KEY_RBRACKET = 0x30; // ] and }
        public static final byte KEY_BACKSLASH = 0x31; // \ and |
        public static final byte KEY_SEMICOLON = 0x33; // ; and :
        public static final byte KEY_QUOTE = 0x34; // ' and "
        public static final byte KEY_GRAVE = 0x35; // ` and ~
        public static final byte KEY_COMMA = 0x36; // , and <
        public static final byte KEY_PERIOD = 0x37; // . and >
        public static final byte KEY_SLASH = 0x38; // / and ?
        
        // Function keys
        public static final byte KEY_F1 = 0x3A;
        public static final byte KEY_F2 = 0x3B;
        public static final byte KEY_F3 = 0x3C;
        public static final byte KEY_F4 = 0x3D;
        public static final byte KEY_F5 = 0x3E;
        public static final byte KEY_F6 = 0x3F;
        public static final byte KEY_F7 = 0x40;
        public static final byte KEY_F8 = 0x41;
        public static final byte KEY_F9 = 0x42;
        public static final byte KEY_F10 = 0x43;
        public static final byte KEY_F11 = 0x44;
        public static final byte KEY_F12 = 0x45;
        
        // Other control keys
        public static final byte KEY_CAPS_LOCK = 0x39;
        public static final byte KEY_PRINT_SCREEN = 0x46;
        public static final byte KEY_SCROLL_LOCK = 0x47;
        public static final byte KEY_PAUSE = 0x48;
        public static final byte KEY_INSERT = 0x49;
        public static final byte KEY_HOME = 0x4A;
        public static final byte KEY_PAGE_UP = 0x4B;
        public static final byte KEY_DELETE = 0x4C;
        public static final byte KEY_END = 0x4D;
        public static final byte KEY_PAGE_DOWN = 0x4E;
        public static final byte KEY_RIGHT_ARROW = 0x4F;
        public static final byte KEY_LEFT_ARROW = 0x50;
        public static final byte KEY_DOWN_ARROW = 0x51;
        public static final byte KEY_UP_ARROW = 0x52;
        
        // Keypad (numpad) keys
        public static final byte KEY_NUM_LOCK = 0x53;
        public static final byte KEY_KP_DIVIDE = 0x54;
        public static final byte KEY_KP_MULTIPLY = 0x55;
        public static final byte KEY_KP_SUBTRACT = 0x56;
        public static final byte KEY_KP_ADD = 0x57;
        public static final byte KEY_KP_ENTER = 0x58;
        public static final byte KEY_KP_1 = 0x59;
        public static final byte KEY_KP_2 = 0x5A;
        public static final byte KEY_KP_3 = 0x5B;
        public static final byte KEY_KP_4 = 0x5C;
        public static final byte KEY_KP_5 = 0x5D;
        public static final byte KEY_KP_6 = 0x5E;
        public static final byte KEY_KP_7 = 0x5F;
        public static final byte KEY_KP_8 = 0x60;
        public static final byte KEY_KP_9 = 0x61;
        public static final byte KEY_KP_0 = 0x62;
        public static final byte KEY_KP_DECIMAL = 0x63;
        
        // Keyboard report map section (for reference only - use HidConstants.REPORT_MAP for actual implementation)
        public static final byte[] KEYBOARD_REPORT_MAP = new byte[] {
            // Keyboard
            0x05, 0x01,       // Usage Page (Generic Desktop)
            0x09, 0x06,       // Usage (Keyboard)
            (byte)0xA1, 0x01, // Collection (Application)
            
            // Modifier keys (shift, ctrl, alt, etc)
            0x05, 0x07,       // Usage Page (Key Codes)
            0x19, (byte)0xE0, // Usage Minimum (224)
            0x29, (byte)0xE7, // Usage Maximum (231)
            0x15, 0x00,       // Logical Minimum (0)
            0x25, 0x01,       // Logical Maximum (1)
            0x75, 0x01,       // Report Size (1)
            (byte)0x95, (byte)0x08,       // Report Count (8)
            (byte)0x81, 0x02, // Input (Data, Var, Abs) ; Modifier byte
            
            // Reserved byte
            (byte)0x95, (byte)0x01,       // Report Count (1)
            0x75, 0x08,       // Report Size (8)
            (byte)0x81, 0x01, // Input (Const) ; Reserved byte
            
            // Key array (6 keys)
            (byte)0x95, (byte)0x06,       // Report Count (6)
            0x75, 0x08,       // Report Size (8)
            0x15, 0x00,       // Logical Minimum (0)
            0x25, 0x65,       // Logical Maximum (101)
            0x05, 0x07,       // Usage Page (Key Codes)
            0x19, 0x00,       // Usage Minimum (0)
            0x29, 0x65,       // Usage Maximum (101)
            (byte)0x81, 0x00, // Input (Data, Array)
            
            (byte)0xC0        // End Collection
        };
    }
    
    /**
     * Consumer control-specific constants.
     */
    public static class Consumer {
        // Media control bit positions
        public static final byte CONSUMER_MUTE         = 0x01; // bit 0
        public static final byte CONSUMER_VOLUME_UP    = 0x02; // bit 1
        public static final byte CONSUMER_VOLUME_DOWN  = 0x04; // bit 2
        public static final byte CONSUMER_PLAY_PAUSE   = 0x08; // bit 3
        public static final byte CONSUMER_NEXT_TRACK   = 0x10; // bit 4
        public static final byte CONSUMER_PREV_TRACK   = 0x20; // bit 5
        public static final byte CONSUMER_STOP         = 0x40; // bit 6
        // bit 7 is reserved/padding
        
        // Alternative names for the same constants (for backward compatibility)
        public static final byte MUTE         = CONSUMER_MUTE;
        public static final byte VOLUME_UP    = CONSUMER_VOLUME_UP;
        public static final byte VOLUME_DOWN  = CONSUMER_VOLUME_DOWN;
        public static final byte PLAY_PAUSE   = CONSUMER_PLAY_PAUSE;
        public static final byte NEXT_TRACK   = CONSUMER_NEXT_TRACK;
        public static final byte PREV_TRACK   = CONSUMER_PREV_TRACK;
        public static final byte STOP         = CONSUMER_STOP;
        public static final byte RECORD       = (byte)0x80; // Using bit 7 for RECORD
        public static final byte FAST_FORWARD = (byte)0x03; // Using bits 0-1 combination for FAST_FORWARD
        
        // Consumer control report is 2 bytes (16-bit value)
        public static final int CONSUMER_REPORT_SIZE = 2;
        
        // Full Usage ID values for reference (from USB HID Usage Tables)
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
        
        // Consumer control report map section (for reference only - use HidConstants.REPORT_MAP for actual implementation)
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
    }
}
