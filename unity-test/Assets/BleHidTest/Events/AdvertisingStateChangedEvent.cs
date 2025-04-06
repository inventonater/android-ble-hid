using System;

namespace BleHid.Events
{
    /// <summary>
    /// Event fired when the advertising state changes.
    /// </summary>
    public class AdvertisingStateChangedEvent : BleHidEvent
    {
        /// <summary>
        /// Whether the device is currently advertising
        /// </summary>
        public bool IsAdvertising { get; private set; }
        
        /// <summary>
        /// Creates a new advertising state changed event.
        /// </summary>
        /// <param name="isAdvertising">Whether the device is currently advertising</param>
        /// <param name="eventId">ID of the event</param>
        /// <param name="javaTimestamp">Java timestamp (milliseconds since epoch)</param>
        public AdvertisingStateChangedEvent(bool isAdvertising, Guid eventId, long javaTimestamp)
            : base(BleHidEventType.AdvertisingStateChanged, eventId, javaTimestamp)
        {
            IsAdvertising = isAdvertising;
        }
        
        /// <summary>
        /// Creates a new advertising state changed event.
        /// For events created on the Unity side.
        /// </summary>
        /// <param name="isAdvertising">Whether the device is currently advertising</param>
        public AdvertisingStateChangedEvent(bool isAdvertising)
            : base(BleHidEventType.AdvertisingStateChanged)
        {
            IsAdvertising = isAdvertising;
        }
        
        /// <summary>
        /// Gets a string representation of this event.
        /// </summary>
        public override string ToString()
        {
            return $"{base.ToString()}, isAdvertising={IsAdvertising}";
        }
    }
}
