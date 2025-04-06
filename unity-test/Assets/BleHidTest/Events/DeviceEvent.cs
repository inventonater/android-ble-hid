using System;

namespace BleHid.Events
{
    /// <summary>
    /// Base class for all device-related events.
    /// Provides common device properties.
    /// </summary>
    public abstract class DeviceEvent : BleHidEvent
    {
        /// <summary>
        /// MAC address of the device
        /// </summary>
        public string DeviceAddress { get; private set; }
        
        /// <summary>
        /// Name of the device, if available
        /// </summary>
        public string DeviceName { get; private set; }
        
        /// <summary>
        /// Creates a new device event.
        /// </summary>
        /// <param name="eventType">Type of this event</param>
        /// <param name="deviceAddress">MAC address of the device</param>
        /// <param name="deviceName">Name of the device, if available</param>
        /// <param name="eventId">ID of the event (from Java side)</param>
        /// <param name="javaTimestamp">Java timestamp (milliseconds since epoch)</param>
        protected DeviceEvent(BleHidEventType eventType, string deviceAddress, string deviceName, Guid eventId, long javaTimestamp)
            : base(eventType, eventId, javaTimestamp)
        {
            DeviceAddress = deviceAddress;
            DeviceName = deviceName;
        }
        
        /// <summary>
        /// Creates a new device event.
        /// For events created on the Unity side.
        /// </summary>
        /// <param name="eventType">Type of this event</param>
        /// <param name="deviceAddress">MAC address of the device</param>
        /// <param name="deviceName">Name of the device, if available</param>
        protected DeviceEvent(BleHidEventType eventType, string deviceAddress, string deviceName = null)
            : base(eventType)
        {
            DeviceAddress = deviceAddress;
            DeviceName = deviceName;
        }
        
        /// <summary>
        /// Gets a string representation of this event.
        /// </summary>
        public override string ToString()
        {
            return $"{base.ToString()}, deviceAddress={DeviceAddress}, deviceName={DeviceName}";
        }
    }
}
