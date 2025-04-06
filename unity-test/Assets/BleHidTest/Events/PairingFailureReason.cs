namespace BleHid.Events
{
    /// <summary>
    /// Reasons why pairing might fail
    /// </summary>
    public enum PairingFailureReason
    {
        /// <summary>Pairing was cancelled by user or device</summary>
        Cancelled,
        
        /// <summary>Pairing timed out</summary>
        Timeout,
        
        /// <summary>Authentication failed (incorrect PIN, etc)</summary>
        AuthenticationFailed,
        
        /// <summary>Protocol error during pairing</summary>
        ProtocolError,
        
        /// <summary>Pairing was rejected by remote device</summary>
        RejectedByRemote,
        
        /// <summary>Failure for unknown reasons</summary>
        Unknown
    }
}
