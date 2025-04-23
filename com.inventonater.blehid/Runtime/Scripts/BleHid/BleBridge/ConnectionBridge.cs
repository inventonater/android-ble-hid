using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    [Serializable]
    public class ConnectionBridge
    {
        private readonly BleHidManager _manager;

        public ConnectionBridge(BleHidManager manager)
        {
            _manager = manager;
        }

        /// <summary>
        /// Disconnects from the currently connected device.
        /// </summary>
        /// <returns>True if disconnect was successful or already disconnected, false otherwise</returns>
        public bool Disconnect()
        {
            Debug.Log("[ConnectionBridge] Disconnect method called");
            
            if (!_manager.ConfirmIsInitialized()) 
            {
                Debug.LogError("[ConnectionBridge] Cannot disconnect - manager not initialized");
                return false;
            }
            
            if (!_manager.IsConnected)
            {
                Debug.Log("[ConnectionBridge] Already disconnected (IsConnected = false)");
                return true; // Already disconnected is a success case
            }
            
            try
            {
                Debug.Log($"[ConnectionBridge] Calling Java disconnect method via BleInitializer. Device: {_manager.ConnectedDeviceName} ({_manager.ConnectedDeviceAddress})");
                bool result = _manager.BleInitializer.Call<bool>("disconnect");
                Debug.Log($"[ConnectionBridge] Java disconnect call returned: {result}");
                return result;
            }
            catch (Exception e)
            {
                LoggingManager.Instance.AddLogError($"Failed to disconnect: {e.Message}");
                return false;
            }
        }
    }
}
