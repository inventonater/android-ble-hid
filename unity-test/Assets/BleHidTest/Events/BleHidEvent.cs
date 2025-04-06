using System;
using UnityEngine;

namespace BleHid.Events
{
    /// <summary>
    /// Base class for all BLE HID events received from the native plugin.
    /// </summary>
    public abstract class BleHidEvent
    {
        /// <summary>
        /// Unique identifier for the event instance
        /// </summary>
        public Guid EventId { get; private set; }
        
        /// <summary>
        /// Type of the event for routing purposes
        /// </summary>
        public BleHidEventType EventType { get; private set; }
        
        /// <summary>
        /// Timestamp when the event was created (as reported by Java side)
        /// </summary>
        public long JavaTimestamp { get; private set; }
        
        /// <summary>
        /// Timestamp when the event was received in Unity
        /// </summary>
        public DateTime UnityTimestamp { get; private set; }
        
        /// <summary>
        /// Creates a new BLE HID event with the specified type.
        /// </summary>
        /// <param name="eventType">Type of the event</param>
        /// <param name="eventId">ID of the event (from Java side)</param>
        /// <param name="javaTimestamp">Java timestamp (milliseconds since epoch)</param>
        protected BleHidEvent(BleHidEventType eventType, Guid eventId, long javaTimestamp)
        {
            EventType = eventType;
            EventId = eventId;
            JavaTimestamp = javaTimestamp;
            UnityTimestamp = DateTime.Now;
        }
        
        /// <summary>
        /// Creates a new BLE HID event with the specified type.
        /// For events created on the Unity side.
        /// </summary>
        /// <param name="eventType">Type of the event</param>
        protected BleHidEvent(BleHidEventType eventType)
        {
            EventType = eventType;
            EventId = Guid.NewGuid();
            JavaTimestamp = DateTimeOffset.Now.ToUnixTimeMilliseconds();
            UnityTimestamp = DateTime.Now;
        }
        
        /// <summary>
        /// Gets a string representation of this event.
        /// </summary>
        public override string ToString()
        {
            return $"{GetType().Name}[id={EventId}, type={EventType}, javaTime={JavaTimestamp}, unityTime={UnityTimestamp}]";
        }
    }
}
