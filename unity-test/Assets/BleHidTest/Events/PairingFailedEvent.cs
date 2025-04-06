using System;

namespace BleHid.Events
{
    /// <summary>
    /// Event fired when pairing with a device fails.
    /// </summary>
    public class PairingFailedEvent : DeviceEvent
    {
        /// <summary>
        /// The reason for the pairing failure
        /// </summary>
        public PairingFailureReason Reason { get; private set; }
        
        /// <summary>
        /// Creates a new pairing failed event.
        /// </summary>
        /// <param name="deviceAddress">MAC address of the device</param>
        /// <param name="reason">The reason for the pairing failure</param>
        /// <param name="eventId">ID of the event</param>
        /// <param name="javaTimestamp">Java timestamp (milliseconds since epoch)</param>
        public PairingFailedEvent(string deviceAddress, PairingFailureReason reason, Guid eventId, long javaTimestamp)
            : base(BleHidEventType.PairingFailed, deviceAddress, null, eventId, javaTimestamp)
        {
            Reason = reason;
        }
        
        /// <summary>
        /// Creates a new pairing failed event.
        /// For events created on the Unity side.
        /// </summary>
        /// <param name="deviceAddress">MAC address of the device</param>
        /// <param name="reason">The reason for the pairing failure</param>
        public PairingFailedEvent(string deviceAddress, PairingFailureReason reason)
            : base(BleHidEventType.PairingFailed, deviceAddress)
        {
            Reason = reason;
        }
        
        /// <summary>
        /// Gets a string representation of this event.
        /// </summary>
        public override string ToString()
        {
            return $"{base.ToString()}, reason={Reason}";
        }
    }
}
