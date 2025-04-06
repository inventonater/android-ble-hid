namespace BleHid.Events
{
    /// <summary>
    /// Interface for objects that can listen for BLE HID events.
    /// </summary>
    public interface IBleHidEventListener
    {
        /// <summary>
        /// Called when a BLE HID event occurs.
        /// </summary>
        /// <param name="event">The event that occurred</param>
        void OnEventReceived(BleHidEvent @event);
    }
}
