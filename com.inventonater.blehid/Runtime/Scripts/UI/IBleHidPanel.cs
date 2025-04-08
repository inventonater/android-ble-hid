using UnityEngine;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// Interface for all BLE HID UI panels.
    /// </summary>
    public interface IBleHidPanel
    {
        /// <summary>
        /// Draw the panel using Unity's OnGUI.
        /// </summary>
        void DrawPanel();

        /// <summary>
        /// Gets whether the panel requires a connected device to function.
        /// </summary>
        bool RequiresConnectedDevice { get; }

        /// <summary>
        /// Initialize the panel.
        /// </summary>
        /// <param name="manager">The BleHidManager instance.</param>
        void Initialize(BleHidManager manager);

        /// <summary>
        /// Called when the panel becomes active.
        /// </summary>
        void OnActivate();

        /// <summary>
        /// Called when the panel becomes inactive.
        /// </summary>
        void OnDeactivate();

        /// <summary>
        /// Process Update calls when this panel is active.
        /// </summary>
        void Update();
    }
}
