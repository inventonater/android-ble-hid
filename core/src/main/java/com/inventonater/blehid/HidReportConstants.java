package com.inventonater.blehid;

/**
 * Constants for HID reports.
 * This class defines constants for report IDs, report sizes, and other report-related values.
 */
public class HidReportConstants {
    // Report IDs
    public static final byte REPORT_ID_NONE = 0;
    public static final byte REPORT_ID_KEYBOARD = 1;
    public static final byte REPORT_ID_MOUSE = 2;
    public static final byte REPORT_ID_CONSUMER = 3;
    
    // Report sizes
    public static final int MOUSE_REPORT_SIZE = 4;     // buttons(1) + x(1) + y(1) + wheel(1)
    public static final int KEYBOARD_REPORT_SIZE = 8;  // modifiers(1) + reserved(1) + keys(6)
    public static final int CONSUMER_REPORT_SIZE = 2;  // 16-bit consumer control value
    
    // Mouse button masks
    public static final byte MOUSE_BUTTON_LEFT = 0x01;
    public static final byte MOUSE_BUTTON_RIGHT = 0x02;
    public static final byte MOUSE_BUTTON_MIDDLE = 0x04;
    
    // Keyboard modifier masks
    public static final byte KEYBOARD_MODIFIER_NONE = 0x00;
    public static final byte KEYBOARD_MODIFIER_LEFT_CTRL = 0x01;
    public static final byte KEYBOARD_MODIFIER_LEFT_SHIFT = 0x02;
    public static final byte KEYBOARD_MODIFIER_LEFT_ALT = 0x04;
    public static final byte KEYBOARD_MODIFIER_LEFT_GUI = 0x08;
    public static final byte KEYBOARD_MODIFIER_RIGHT_CTRL = 0x10;
    public static final byte KEYBOARD_MODIFIER_RIGHT_SHIFT = 0x20;
    public static final byte KEYBOARD_MODIFIER_RIGHT_ALT = 0x40;
    public static final int KEYBOARD_MODIFIER_RIGHT_GUI = 0x80;
    
    // Keyboard key codes (USB HID Usage Tables 1.12)
    public static final byte KEY_NONE = 0x00;
    public static final byte KEY_A = 0x04;
    public static final byte KEY_B = 0x05;
    public static final byte KEY_C = 0x06;
    public static final byte KEY_Z = 0x1d;
    public static final byte KEY_1 = 0x1e;
    public static final byte KEY_2 = 0x1f;
    public static final byte KEY_3 = 0x20;
    public static final byte KEY_4 = 0x21;
    public static final byte KEY_5 = 0x22;
    public static final byte KEY_6 = 0x23;
    public static final byte KEY_7 = 0x24;
    public static final byte KEY_8 = 0x25;
    public static final byte KEY_9 = 0x26;
    public static final byte KEY_0 = 0x27;
    public static final byte KEY_ENTER = 0x28;
    public static final byte KEY_ESCAPE = 0x29;
    public static final byte KEY_BACKSPACE = 0x2a;
    public static final byte KEY_TAB = 0x2b;
    public static final byte KEY_SPACE = 0x2c;
    public static final byte KEY_MINUS = 0x2d;
    public static final byte KEY_EQUAL = 0x2e;
    public static final byte KEY_LEFT_BRACKET = 0x2f;
    public static final byte KEY_RIGHT_BRACKET = 0x30;
    public static final byte KEY_BACKSLASH = 0x31;
    public static final byte KEY_SEMICOLON = 0x33;
    public static final byte KEY_APOSTROPHE = 0x34;
    public static final byte KEY_GRAVE = 0x35;
    public static final byte KEY_COMMA = 0x36;
    public static final byte KEY_PERIOD = 0x37;
    public static final byte KEY_SLASH = 0x38;
    public static final byte KEY_CAPS_LOCK = 0x39;
    public static final byte KEY_F1 = 0x3a;
    public static final byte KEY_F12 = 0x45;
    public static final byte KEY_PRINT_SCREEN = 0x46;
    public static final byte KEY_SCROLL_LOCK = 0x47;
    public static final byte KEY_PAUSE = 0x48;
    public static final byte KEY_INSERT = 0x49;
    public static final byte KEY_HOME = 0x4a;
    public static final byte KEY_PAGE_UP = 0x4b;
    public static final byte KEY_DELETE = 0x4c;
    public static final byte KEY_END = 0x4d;
    public static final byte KEY_PAGE_DOWN = 0x4e;
    public static final byte KEY_RIGHT_ARROW = 0x4f;
    public static final byte KEY_LEFT_ARROW = 0x50;
    public static final byte KEY_DOWN_ARROW = 0x51;
    public static final byte KEY_UP_ARROW = 0x52;
    
    // Consumer control codes (USB HID Usage Tables 1.12)
    // These are the 16-bit usage codes for common media controls
    public static final short CONSUMER_USAGE_PLAY_PAUSE = 0x00CD;
    public static final short CONSUMER_USAGE_SCAN_NEXT = 0x00B5;
    public static final short CONSUMER_USAGE_SCAN_PREVIOUS = 0x00B6;
    public static final short CONSUMER_USAGE_STOP = 0x00B7;
    public static final short CONSUMER_USAGE_VOLUME_UP = 0x00E9;
    public static final short CONSUMER_USAGE_VOLUME_DOWN = 0x00EA;
    public static final short CONSUMER_USAGE_MUTE = 0x00E2;
    public static final short CONSUMER_USAGE_POWER = 0x0030;
    public static final short CONSUMER_USAGE_MENU = 0x0040;
    public static final short CONSUMER_USAGE_RECORD = 0x00B2;
    public static final short CONSUMER_USAGE_FAST_FORWARD = 0x00B3;
    public static final short CONSUMER_USAGE_REWIND = 0x00B4;
}
