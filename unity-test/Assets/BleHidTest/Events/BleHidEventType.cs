using System;

namespace BleHid.Events
{
    /// <summary>
    /// Enum defining all possible event types in the BLE HID system.
    /// Used for event identification and routing.
    /// </summary>
    public enum BleHidEventType
    {
        /// <summary>Device pairing requested</summary>
        PairingRequested,
        
        /// <summary>Device successfully connected</summary>
        DeviceConnected,
        
        /// <summary>Device disconnected</summary>
        DeviceDisconnected,
        
        /// <summary>Pairing with device failed</summary>
        PairingFailed,
        
        /// <summary>Advertising state changed</summary>
        AdvertisingStateChanged,
        
        // System events
        
        /// <summary>Initialization completed successfully</summary>
        InitializationCompleted,
        
        /// <summary>Initialization failed</summary>
        InitializationFailed,
        
        // Connection events
        
        /// <summary>Connection to device lost</summary>
        ConnectionLost,
        
        /// <summary>Reconnection attempt started</summary>
        ReconnectionStarted,
        
        /// <summary>Reconnection attempt succeeded</summary>
        ReconnectionSucceeded,
        
        /// <summary>Reconnection attempt failed</summary>
        ReconnectionFailed,
        
        // Command execution events
        
        /// <summary>Command executed successfully</summary>
        CommandSucceeded,
        
        /// <summary>Command execution failed</summary>
        CommandFailed
    }
}
