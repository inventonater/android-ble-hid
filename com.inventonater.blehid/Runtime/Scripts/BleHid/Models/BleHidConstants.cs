using UnityEngine;

namespace Inventonater
{
    /// <summary>
    /// Contains constants for the HID keyboard, mouse, media key codes, and options parameters.
    /// </summary>
    public static class BleHidConstants
    {
        // Options parameter constants - must match OptionsConstants.java in core module
        public static class OptionsParams
        {
            // Camera options
            public const string TapDelay = "tap_delay_ms";
            public const string ReturnDelay = "return_delay_ms";
            public const string ButtonX = "button_x_position";
            public const string ButtonY = "button_y_position";
            public const string AcceptDialogDelay = "accept_dialog_delay_ms";
            public const string AcceptXOffset = "accept_button_x_offset";
            public const string AcceptYOffset = "accept_button_y_offset";
            
            // Video options
            public const string VideoDuration = "video_duration_ms";
        }
        
        // Mouse buttons - using bit flags to match Android implementation
        // These values represent individual bits rather than indices:
        // 0x01 = 00000001, 0x02 = 00000010, 0x04 = 00000100
        public const int BUTTON_LEFT = 0x01;
        public const int BUTTON_RIGHT = 0x02;
        public const int BUTTON_MIDDLE = 0x04;
        
        // Keyboard modifier keys
        public const byte KEY_MOD_LCTRL = 0x01;
        public const byte KEY_MOD_LSHIFT = 0x02;
        public const byte KEY_MOD_LALT = 0x04;
        public const byte KEY_MOD_LMETA = 0x08;
        public const byte KEY_MOD_RCTRL = 0x10;
        public const byte KEY_MOD_RSHIFT = 0x20;
        public const byte KEY_MOD_RALT = 0x40;
        public const byte KEY_MOD_RMETA = 0x80;
        
        // Keyboard keys
        public const byte KEY_A = 0x04;
        public const byte KEY_B = 0x05;
        public const byte KEY_C = 0x06;
        public const byte KEY_D = 0x07;
        public const byte KEY_E = 0x08;
        public const byte KEY_F = 0x09;
        public const byte KEY_G = 0x0A;
        public const byte KEY_H = 0x0B;
        public const byte KEY_I = 0x0C;
        public const byte KEY_J = 0x0D;
        public const byte KEY_K = 0x0E;
        public const byte KEY_L = 0x0F;
        public const byte KEY_M = 0x10;
        public const byte KEY_N = 0x11;
        public const byte KEY_O = 0x12;
        public const byte KEY_P = 0x13;
        public const byte KEY_Q = 0x14;
        public const byte KEY_R = 0x15;
        public const byte KEY_S = 0x16;
        public const byte KEY_T = 0x17;
        public const byte KEY_U = 0x18;
        public const byte KEY_V = 0x19;
        public const byte KEY_W = 0x1A;
        public const byte KEY_X = 0x1B;
        public const byte KEY_Y = 0x1C;
        public const byte KEY_Z = 0x1D;
        public const byte KEY_1 = 0x1E;
        public const byte KEY_2 = 0x1F;
        public const byte KEY_3 = 0x20;
        public const byte KEY_4 = 0x21;
        public const byte KEY_5 = 0x22;
        public const byte KEY_6 = 0x23;
        public const byte KEY_7 = 0x24;
        public const byte KEY_8 = 0x25;
        public const byte KEY_9 = 0x26;
        public const byte KEY_0 = 0x27;
        public const byte KEY_RETURN = 0x28;  // Enter
        public const byte KEY_ESCAPE = 0x29;
        public const byte KEY_BACKSPACE = 0x2A;
        public const byte KEY_TAB = 0x2B;
        public const byte KEY_SPACE = 0x2C;
        public const byte KEY_MINUS = 0x2D;
        public const byte KEY_EQUAL = 0x2E;
        public const byte KEY_LEFT_BRACE = 0x2F;
        public const byte KEY_RIGHT_BRACE = 0x30;
        public const byte KEY_BACKSLASH = 0x31;
        public const byte KEY_SEMICOLON = 0x33;
        public const byte KEY_APOSTROPHE = 0x34;
        public const byte KEY_GRAVE = 0x35;
        public const byte KEY_COMMA = 0x36;
        public const byte KEY_DOT = 0x37;
        public const byte KEY_SLASH = 0x38;
        public const byte KEY_CAPS_LOCK = 0x39;
        
        // Function keys
        public const byte KEY_F1 = 0x3A;
        public const byte KEY_F2 = 0x3B;
        public const byte KEY_F3 = 0x3C;
        public const byte KEY_F4 = 0x3D;
        public const byte KEY_F5 = 0x3E;
        public const byte KEY_F6 = 0x3F;
        public const byte KEY_F7 = 0x40;
        public const byte KEY_F8 = 0x41;
        public const byte KEY_F9 = 0x42;
        public const byte KEY_F10 = 0x43;
        public const byte KEY_F11 = 0x44;
        public const byte KEY_F12 = 0x45;
        
        // Navigation keys
        public const byte KEY_PRINT_SCREEN = 0x46;
        public const byte KEY_SCROLL_LOCK = 0x47;
        public const byte KEY_PAUSE = 0x48;
        public const byte KEY_INSERT = 0x49;
        public const byte KEY_HOME = 0x4A;
        public const byte KEY_PAGE_UP = 0x4B;
        public const byte KEY_DELETE = 0x4C;
        public const byte KEY_END = 0x4D;
        public const byte KEY_PAGE_DOWN = 0x4E;
        public const byte KEY_RIGHT = 0x4F;
        public const byte KEY_LEFT = 0x50;
        public const byte KEY_DOWN = 0x51;
        public const byte KEY_UP = 0x52;
    }
}
