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
            if (!_manager.ConfirmIsInitialized()) return false;
            
            try
            {
                return _manager.BleInitializer.Call<bool>("disconnect");
            }
            catch (Exception e)
            {
                Debug.LogError($"Error disconnecting: {e.Message}");
                _manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_GENERAL_ERROR, $"Failed to disconnect: {e.Message}");
                return false;
            }
        }
    }
}
