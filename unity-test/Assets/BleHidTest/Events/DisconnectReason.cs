namespace BleHid.Events
{
    /// <summary>
    /// Reasons why a device might disconnect
    /// </summary>
    public enum DisconnectReason
    {
        /// <summary>Normal disconnection initiated by user or device</summary>
        NormalDisconnect,
        
        /// <summary>Connection timed out</summary>
        ConnectionTimeout,
        
        /// <summary>Connection lost due to signal or other issues</summary>
        ConnectionLost,
        
        /// <summary>Remote device terminated the connection</summary>
        RemoteDeviceTerminated,
        
        /// <summary>Local host terminated the connection</summary>
        LocalHostTerminated,
        
        /// <summary>Disconnection for unknown reasons</summary>
        Unknown
    }
}
