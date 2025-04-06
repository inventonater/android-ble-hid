using System;

namespace BleHid.Events
{
    /// <summary>
    /// Event fired when a device disconnects.
    /// </summary>
    public class DeviceDisconnectedEvent : DeviceEvent
    {
        /// <summary>
        /// The reason for the disconnection
        /// </summary>
        public DisconnectReason Reason { get; private set; }
        
        /// <summary>
        /// Creates a new device disconnected event.
        /// </summary>
        /// <param name="deviceAddress">MAC address of the device</param>
        /// <param name="reason">The reason for disconnection</param>
        /// <param name="eventId">ID of the event</param>
        /// <param name="javaTimestamp">Java timestamp (milliseconds since epoch)</param>
        public DeviceDisconnectedEvent(string deviceAddress, DisconnectReason reason, Guid eventId, long javaTimestamp)
            : base(BleHidEventType.DeviceDisconnected, deviceAddress, null, eventId, javaTimestamp)
        {
            Reason = reason;
        }
        
        /// <summary>
        /// Creates a new device disconnected event.
        /// For events created on the Unity side.
        /// </summary>
        /// <param name="deviceAddress">MAC address of the device</param>
        /// <param name="reason">The reason for disconnection</param>
        public DeviceDisconnectedEvent(string deviceAddress, DisconnectReason reason)
            : base(BleHidEventType.DeviceDisconnected, deviceAddress)
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
