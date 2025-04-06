package com.example.blehid.unity;

/**
 * Defines a formal protocol for communication between Java and C# in the BLE HID system.
 * This class establishes a versioned contract with standard status codes, operation types,
 * and command identifiers to ensure consistent interface behavior across language boundaries.
 */
public class BleHidProtocol {
    
    /**
     * Current protocol version - increment when making breaking changes to the interface
     */
    public static final int PROTOCOL_VERSION = 1;
    
    /**
     * Status codes for operations
     */
    public static final class Status {
        public static final int SUCCESS = 0;
        public static final int NOT_INITIALIZED = 1;
        public static final int NOT_CONNECTED = 2;
        public static final int BLUETOOTH_ERROR = 3;
        public static final int INVALID_PARAMETER = 4;
        public static final int OPERATION_FAILED = 5;
        public static final int PERMISSION_DENIED = 6;
        public static final int TIMEOUT = 7;
        public static final int UNKNOWN_ERROR = 99;
    }
    
    /**
     * Operation types for command categorization
     */
    public static final class Operation {
        public static final int SYSTEM = 1;        // System-related commands (init, advertising, etc.)
        public static final int MEDIA = 2;         // Media control commands
        public static final int MOUSE = 3;         // Mouse control commands
        public static final int KEYBOARD = 4;      // Keyboard control commands
        public static final int COMBINED = 5;      // Combined (media + mouse) commands
    }
    
    /**
     * System state flags - used to verify system state across boundaries
     */
    public static final class State {
        public static final byte INITIALIZED = 0x01;
        public static final byte CONNECTED = 0x02;
        public static final byte ADVERTISING = 0x04;
        public static final byte PERIPHERAL_SUPPORTED = 0x08;
    }
    
    /**
     * Command identifiers - used to identify specific commands in batched operations
     */
    public static final class Command {
        // System commands (1xx)
        public static final int INITIALIZE = 101;
        public static final int START_ADVERTISING = 102;
        public static final int STOP_ADVERTISING = 103;
        public static final int CHECK_CONNECTION = 104;
        public static final int GET_DEVICE_ADDRESS = 105;
        public static final int CLOSE = 106;
        
        // Media commands (2xx)
        public static final int PLAY_PAUSE = 201;
        public static final int NEXT_TRACK = 202;
        public static final int PREVIOUS_TRACK = 203;
        public static final int VOLUME_UP = 204;
        public static final int VOLUME_DOWN = 205;
        public static final int MUTE = 206;
        
        // Mouse commands (3xx)
        public static final int MOVE_MOUSE = 301;
        public static final int PRESS_MOUSE_BUTTON = 302;
        public static final int RELEASE_MOUSE_BUTTONS = 303;
        public static final int CLICK_MOUSE_BUTTON = 304;
        public static final int SCROLL_MOUSE_WHEEL = 305;
        
        // Combined commands (5xx)
        public static final int SEND_COMBINED_REPORT = 501;
    }
    
    /**
     * Mouse button constants - duplicated from HidMouseConstants for convenience
     */
    public static final class MouseButton {
        public static final int LEFT = 0x01;
        public static final int RIGHT = 0x02;
        public static final int MIDDLE = 0x04;
    }
    
    /**
     * Media button constants - duplicated from HidMediaConstants for convenience
     */
    public static final class MediaButton {
        public static final int PLAY_PAUSE = 0x01;
        public static final int NEXT_TRACK = 0x02;
        public static final int PREVIOUS_TRACK = 0x04;
        public static final int VOLUME_UP = 0x08;
        public static final int VOLUME_DOWN = 0x10;
        public static final int MUTE = 0x20;
    }
}
