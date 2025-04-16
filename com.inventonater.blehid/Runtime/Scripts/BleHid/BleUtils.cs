using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Provides utility methods for BLE HID functionality.
    /// </summary>
    public class BleUtils
    {
        private BleHidManager manager;

        public BleUtils(BleHidManager manager)
        {
            this.manager = manager;
        }

        /// <summary>
        /// Confirms that the BLE HID system is initialized.
        /// </summary>
        /// <returns>True if initialized, false otherwise.</returns>
        public bool ConfirmIsInitialized()
        {
            if (manager.IsInitialized && manager.BleInitializer.BridgeInstance != null) return true;

            string message = "BLE HID plugin not initialized";
            Debug.LogError(message);
            manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_NOT_INITIALIZED, message);
            return false;
        }

        /// <summary>
        /// Confirms that a BLE device is connected.
        /// </summary>
        /// <returns>True if connected, false otherwise.</returns>
        public bool ConfirmIsConnected()
        {
            if (!ConfirmIsInitialized()) return false;
            if (manager.IsConnected) return true;

            string message = "No BLE device connected";
            Debug.LogError(message);
            manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_NOT_CONNECTED, message);
            return false;
        }
    }
}
