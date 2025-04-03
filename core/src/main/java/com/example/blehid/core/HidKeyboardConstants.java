package com.example.blehid.core;

import java.util.UUID;

/**
 * Constants for HID Keyboard functionality.
 * Contains key codes, report descriptors, and other constants.
 */
public class HidKeyboardConstants {
    // Use the standard UUIDs defined in HidMouseConstants
    
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
    
    // HID Report Map section for a keyboard
    // This is the keyboard portion only, not the full report map
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
