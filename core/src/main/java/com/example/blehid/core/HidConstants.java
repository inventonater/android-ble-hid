package com.example.blehid.core;

import java.util.UUID;

/**
 * Consolidated constants for all HID functionality.
 * Organized into logical groups using nested classes.
 */
public class HidConstants {
    private HidConstants() {
        // Private constructor to prevent instantiation
    }
    
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
     * BLE Service and Characteristic UUIDs for HID functionality.
     */
    public static class Uuids {
        // Service UUIDs
        public static final UUID HID_SERVICE = 
                UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");
        
        // Characteristic UUIDs
        public static final UUID HID_INFORMATION = 
                UUID.fromString("00002A4A-0000-1000-8000-00805f9b34fb");
        public static final UUID HID_CONTROL_POINT = 
                UUID.fromString("00002A4C-0000-1000-8000-00805f9b34fb");
        public static final UUID HID_REPORT_MAP = 
                UUID.fromString("00002A4B-0000-1000-8000-00805f9b34fb");
        public static final UUID HID_REPORT = 
                UUID.fromString("00002A4D-0000-1000-8000-00805f9b34fb");
        public static final UUID HID_PROTOCOL_MODE = 
                UUID.fromString("00002A4E-0000-1000-8000-00805f9b34fb");
        public static final UUID HID_BOOT_MOUSE_INPUT_REPORT = 
                UUID.fromString("00002A33-0000-1000-8000-00805f9b34fb");
        
        // Descriptor UUIDs
        public static final UUID CLIENT_CONFIG = 
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); // CCCD
        public static final UUID REPORT_REFERENCE = 
                UUID.fromString("00002908-0000-1000-8000-00805f9b34fb");
    }
    
    /**
     * Protocol-related constants.
     */
    public static class Protocol {
        // Protocol Modes
        public static final byte MODE_BOOT = 0x00;
        public static final byte MODE_REPORT = 0x01;
        
        // HID Information values
        // [bcdHID, bCountryCode, flags]
        // bcdHID: 0x0111 (HID version 1.11)
        // bCountryCode: 0x00 (Not localized)
        // flags: 0x03 (Remote wake + normally connectable)
        public static final byte[] HID_INFORMATION = {0x11, 0x01, 0x00, 0x03};
    }
    
    /**
     * Mouse-related constants.
     */
    public static class Mouse {
        // Mouse button constants
        public static final int BUTTON_LEFT = 0x01;
        public static final int BUTTON_RIGHT = 0x02;
        public static final int BUTTON_MIDDLE = 0x04;
        
        // Standard Mouse report map (only mouse functionality)
        // 4-byte mouse report: [buttons, x, y, wheel] without report ID
        public static final byte[] REPORT_MAP = new byte[] {
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
            // INPUT (Const,Array,Abs) - Padding (matches the sample)
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
            
            // Vertical wheel
            // USAGE (Wheel)
            (byte)0x09, (byte)0x38,
            // LOGICAL_MINIMUM (-127)
            (byte)0x15, (byte)0x81,
            // LOGICAL_MAXIMUM (127)
            (byte)0x25, (byte)0x7F,
            // REPORT_SIZE (8)
            (byte)0x75, (byte)0x08,
            // REPORT_COUNT (1)
            (byte)0x95, (byte)0x01,
            // INPUT (Data,Var,Rel)
            (byte)0x81, (byte)0x06,
            
            // END_COLLECTION (Physical)
            (byte)0xC0,
            // END_COLLECTION (Application)
            (byte)0xC0
        };
    }
    
    /**
     * Media control-related constants.
     */
    public static class Media {
        // Media control button constants
        public static final int BUTTON_PLAY_PAUSE = 0x01;
        public static final int BUTTON_NEXT_TRACK = 0x02;
        public static final int BUTTON_PREVIOUS_TRACK = 0x04;
        public static final int BUTTON_VOLUME_UP = 0x08;
        public static final int BUTTON_VOLUME_DOWN = 0x10;
        public static final int BUTTON_MUTE = 0x20;
    }
    
    /**
     * Keyboard-related constants.
     */
    public static class Keyboard {
        // Keyboard modifiers
        public static final int MOD_LCTRL = 0x01;
        public static final int MOD_LSHIFT = 0x02;
        public static final int MOD_LALT = 0x04;
        public static final int MOD_LMETA = 0x08;
        public static final int MOD_RCTRL = 0x10;
        public static final int MOD_RSHIFT = 0x20;
        public static final int MOD_RALT = 0x40;
        public static final int MOD_RMETA = 0x80;
        
        // HID keyboard key codes
        public static final byte KEY_NONE = 0x00;
        
        // Letters
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
        
        // Numbers
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
        
        // Special keys
        public static final byte KEY_ENTER = 0x28;
        public static final byte KEY_ESCAPE = 0x29;
        public static final byte KEY_BACKSPACE = 0x2A;
        public static final byte KEY_TAB = 0x2B;
        public static final byte KEY_SPACE = 0x2C;
        public static final byte KEY_MINUS = 0x2D;
        public static final byte KEY_EQUALS = 0x2E;
        public static final byte KEY_BRACKET_LEFT = 0x2F;
        public static final byte KEY_BRACKET_RIGHT = 0x30;
        public static final byte KEY_BACKSLASH = 0x31;
        public static final byte KEY_SEMICOLON = 0x33;
        public static final byte KEY_APOSTROPHE = 0x34;
        public static final byte KEY_GRAVE = 0x35;
        public static final byte KEY_COMMA = 0x36;
        public static final byte KEY_PERIOD = 0x37;
        public static final byte KEY_SLASH = 0x38;
        
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
        
        // Navigation keys
        public static final byte KEY_HOME = 0x4A;
        public static final byte KEY_PAGE_UP = 0x4B;
        public static final byte KEY_DELETE = 0x4C;
        public static final byte KEY_END = 0x4D;
        public static final byte KEY_PAGE_DOWN = 0x4E;
        public static final byte KEY_RIGHT = 0x4F;
        public static final byte KEY_LEFT = 0x50;
        public static final byte KEY_DOWN = 0x51;
        public static final byte KEY_UP = 0x52;
    }
    
    /**
     * Combined report maps and structures.
     */
    public static class Combined {
        // Combined HID Report Map descriptor for consumer control (media player), mouse, and keyboard
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
            (byte)0xC0,
            
            // === Third part: Keyboard Controls ===
            // USAGE_PAGE (Generic Desktop)
            (byte)0x05, (byte)0x01,
            // USAGE (Keyboard)
            (byte)0x09, (byte)0x06,
            // COLLECTION (Application)
            (byte)0xA1, (byte)0x01,
            
            // Modifier keys (8 bits)
            // USAGE_PAGE (Keyboard/Keypad)
            (byte)0x05, (byte)0x07,
            // USAGE_MINIMUM (Keyboard Left Control)
            (byte)0x19, (byte)0xE0,
            // USAGE_MAXIMUM (Keyboard Right GUI)
            (byte)0x29, (byte)0xE7,
            // LOGICAL_MINIMUM (0)
            (byte)0x15, (byte)0x00,
            // LOGICAL_MAXIMUM (1)
            (byte)0x25, (byte)0x01,
            // REPORT_SIZE (1)
            (byte)0x75, (byte)0x01,
            // REPORT_COUNT (8)
            (byte)0x95, (byte)0x08,
            // INPUT (Data,Var,Abs)
            (byte)0x81, (byte)0x02,
            
            // Reserved byte
            // REPORT_COUNT (1)
            (byte)0x95, (byte)0x01,
            // REPORT_SIZE (8)
            (byte)0x75, (byte)0x08,
            // INPUT (Const,Var,Abs)
            (byte)0x81, (byte)0x01,
            
            // LED output (5 bits)
            // REPORT_COUNT (5)
            (byte)0x95, (byte)0x05,
            // REPORT_SIZE (1)
            (byte)0x75, (byte)0x01,
            // USAGE_PAGE (LEDs)
            (byte)0x05, (byte)0x08,
            // USAGE_MINIMUM (Num Lock)
            (byte)0x19, (byte)0x01,
            // USAGE_MAXIMUM (Kana)
            (byte)0x29, (byte)0x05,
            // OUTPUT (Data,Var,Abs)
            (byte)0x91, (byte)0x02,
            
            // LED output padding (3 bits)
            // REPORT_COUNT (1)
            (byte)0x95, (byte)0x01,
            // REPORT_SIZE (3)
            (byte)0x75, (byte)0x03,
            // OUTPUT (Const,Var,Abs)
            (byte)0x91, (byte)0x01,
            
            // Key arrays (6 keys)
            // REPORT_COUNT (6)
            (byte)0x95, (byte)0x06,
            // REPORT_SIZE (8)
            (byte)0x75, (byte)0x08,
            // LOGICAL_MINIMUM (0)
            (byte)0x15, (byte)0x00,
            // LOGICAL_MAXIMUM (255)
            (byte)0x25, (byte)0xFF,
            // USAGE_PAGE (Keyboard/Keypad)
            (byte)0x05, (byte)0x07,
            // USAGE_MINIMUM (Reserved)
            (byte)0x19, (byte)0x00,
            // USAGE_MAXIMUM (Keyboard Application)
            (byte)0x29, (byte)0xFF,
            // INPUT (Data,Array,Abs)
            (byte)0x81, (byte)0x00,
            
            // END_COLLECTION (Keyboard Application)
            (byte)0xC0
        };
    }
}
