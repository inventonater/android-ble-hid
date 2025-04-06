using System;

namespace BleHid.Events
{
    /// <summary>
    /// Event fired when a device successfully connects.
    /// </summary>
    public class DeviceConnectedEvent : DeviceEvent
    {
        /// <summary>
        /// Creates a new device connected event.
        /// </summary>
        /// <param name="deviceAddress">MAC address of the device</param>
        /// <param name="eventId">ID of the event</param>
        /// <param name="javaTimestamp">Java timestamp (milliseconds since epoch)</param>
        public DeviceConnectedEvent(string deviceAddress, Guid eventId, long javaTimestamp)
            : base(BleHidEventType.DeviceConnected, deviceAddress, null, eventId, javaTimestamp)
        {
        }
        
        /// <summary>
        /// Creates a new device connected event.
        /// For events created on the Unity side.
        /// </summary>
        /// <param name="deviceAddress">MAC address of the device</param>
        public DeviceConnectedEvent(string deviceAddress)
            : base(BleHidEventType.DeviceConnected, deviceAddress)
        {
        }
    }
}
