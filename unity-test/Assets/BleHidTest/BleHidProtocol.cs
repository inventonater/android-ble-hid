using UnityEngine;

namespace BleHid
{
    /// <summary>
    /// Defines a formal protocol for communication between C# and Java in the BLE HID system.
    /// This class establishes a versioned contract with standard status codes, operation types,
    /// and command identifiers to ensure consistent interface behavior across language boundaries.
    /// </summary>
    public static class BleHidProtocol
    {
        /// <summary>
        /// Current protocol version - increment when making breaking changes to the interface
        /// </summary>
        public const int ProtocolVersion = 1;
        
        /// <summary>
        /// Status codes for operations
        /// </summary>
        public static class Status
        {
            public const int Success = 0;
            public const int NotInitialized = 1;
            public const int NotConnected = 2;
            public const int BluetoothError = 3;
            public const int InvalidParameter = 4;
            public const int OperationFailed = 5;
            public const int PermissionDenied = 6;
            public const int Timeout = 7;
            public const int UnknownError = 99;
        }
        
        /// <summary>
        /// Operation types for command categorization
        /// </summary>
        public static class Operation
        {
            public const int System = 1;       // System-related commands (init, advertising, etc.)
            public const int Media = 2;        // Media control commands
            public const int Mouse = 3;        // Mouse control commands
            public const int Keyboard = 4;     // Keyboard control commands
            public const int Combined = 5;     // Combined (media + mouse) commands
        }
        
        /// <summary>
        /// System state flags - used to verify system state across boundaries
        /// </summary>
        public static class State
        {
            public const byte Initialized = 0x01;
            public const byte Connected = 0x02;
            public const byte Advertising = 0x04;
            public const byte PeripheralSupported = 0x08;
        }
        
        /// <summary>
        /// Command identifiers - used to identify specific commands in batched operations
        /// </summary>
        public static class Command
        {
            // System commands (1xx)
            public const int Initialize = 101;
            public const int StartAdvertising = 102;
            public const int StopAdvertising = 103;
            public const int CheckConnection = 104;
            public const int GetDeviceAddress = 105;
            public const int Close = 106;
            
            // Media commands (2xx)
            public const int PlayPause = 201;
            public const int NextTrack = 202;
            public const int PreviousTrack = 203;
            public const int VolumeUp = 204;
            public const int VolumeDown = 205;
            public const int Mute = 206;
            
            // Mouse commands (3xx)
            public const int MoveMouse = 301;
            public const int PressMouseButton = 302;
            public const int ReleaseMouseButtons = 303;
            public const int ClickMouseButton = 304;
            public const int ScrollMouseWheel = 305;
            
            // Combined commands (5xx)
            public const int SendCombinedReport = 501;
        }
        
        /// <summary>
        /// Mouse button constants - duplicated from HidConstants for convenience
        /// </summary>
        public static class MouseButton
        {
            public const int Left = 0x01;
            public const int Right = 0x02;
            public const int Middle = 0x04;
        }
        
        /// <summary>
        /// Media button constants - duplicated from HidConstants for convenience
        /// </summary>
        public static class MediaButton
        {
            public const int PlayPause = 0x01;
            public const int NextTrack = 0x02;
            public const int PreviousTrack = 0x04;
            public const int VolumeUp = 0x08;
            public const int VolumeDown = 0x10;
            public const int Mute = 0x20;
        }

        /// <summary>
        /// Validates that a protocol version is compatible with the current version
        /// </summary>
        /// <param name="version">Version to check</param>
        /// <returns>True if compatible, false otherwise</returns>
        public static bool IsCompatibleVersion(int version)
        {
            // For now, we only support exact version match
            // In the future, this could be enhanced to support backward compatibility
            return version == ProtocolVersion;
        }

        /// <summary>
        /// Gets the string representation of a status code
        /// </summary>
        /// <param name="statusCode">Status code to convert</param>
        /// <returns>String representation of the status code</returns>
        public static string GetStatusString(int statusCode)
        {
            switch (statusCode)
            {
                case Status.Success: return "Success";
                case Status.NotInitialized: return "Not Initialized";
                case Status.NotConnected: return "Not Connected";
                case Status.BluetoothError: return "Bluetooth Error";
                case Status.InvalidParameter: return "Invalid Parameter";
                case Status.OperationFailed: return "Operation Failed";
                case Status.PermissionDenied: return "Permission Denied";
                case Status.Timeout: return "Timeout";
                case Status.UnknownError: return "Unknown Error";
                default: return "Status Code: " + statusCode;
            }
        }
    }
}
