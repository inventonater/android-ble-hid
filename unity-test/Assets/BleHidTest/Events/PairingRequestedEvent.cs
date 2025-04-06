using System;

namespace BleHid.Events
{
    /// <summary>
    /// Event fired when a device requests pairing.
    /// </summary>
    public class PairingRequestedEvent : DeviceEvent
    {
        /// <summary>
        /// The pairing variant requested (PIN, passkey, etc.)
        /// </summary>
        public int PairingVariant { get; private set; }
        
        /// <summary>
        /// Creates a new pairing requested event.
        /// </summary>
        /// <param name="deviceAddress">MAC address of the device</param>
        /// <param name="pairingVariant">Pairing variant</param>
        /// <param name="eventId">ID of the event</param>
        /// <param name="javaTimestamp">Java timestamp (milliseconds since epoch)</param>
        public PairingRequestedEvent(string deviceAddress, int pairingVariant, Guid eventId, long javaTimestamp)
            : base(BleHidEventType.PairingRequested, deviceAddress, null, eventId, javaTimestamp)
        {
            PairingVariant = pairingVariant;
        }
        
        /// <summary>
        /// Creates a new pairing requested event.
        /// For events created on the Unity side.
        /// </summary>
        /// <param name="deviceAddress">MAC address of the device</param>
        /// <param name="pairingVariant">Pairing variant</param>
        public PairingRequestedEvent(string deviceAddress, int pairingVariant)
            : base(BleHidEventType.PairingRequested, deviceAddress)
        {
            PairingVariant = pairingVariant;
        }
        
        /// <summary>
        /// Gets a string representation of this event.
        /// </summary>
        public override string ToString()
        {
            return $"{base.ToString()}, pairingVariant={PairingVariant}";
        }
    }
}
