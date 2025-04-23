using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Handles BLE advertising functionality.
    /// </summary>
    public class BleAdvertiser
    {
        private BleHidManager manager;

        public BleAdvertiser(BleHidManager manager)
        {
            this.manager = manager;
        }

        /// <summary>
        /// Start BLE advertising to make this device discoverable.
        /// </summary>
        /// <returns>True if advertising was started successfully, false otherwise.</returns>
        public bool StartAdvertising()
        {
            if (!manager.ConfirmIsInitialized()) return false;

            try
            {
                Debug.Log("BleHidManager: Attempting to start advertising...");

                // Add extra debug info
                try
                {
                    // Use a simple toString() to get some information about the bridge instance
                    string instanceInfo = manager.Bridge.Call<string>("toString");
                    Debug.Log("BleHidManager: Using bridgeInstance: " + instanceInfo);
                }
                catch (Exception debugEx) { Debug.LogWarning("BleHidManager: Could not get bridge instance info: " + debugEx.Message); }

                bool result = manager.Bridge.Call<bool>("startAdvertising");
                Debug.Log("BleHidManager: StartAdvertising call result: " + result);

                // Verify advertising state
                try
                {
                    bool isAdvertising = manager.Bridge.Call<bool>("isAdvertising");
                    Debug.Log("BleHidManager: isAdvertising check after call: " + isAdvertising);
                }
                catch (Exception verifyEx) { Debug.LogWarning("BleHidManager: Could not verify advertising state: " + verifyEx.Message); }

                return result;
            }
            catch (Exception e)
            {
                LoggingManager.Instance.AddLogError("Exception starting advertising: " + e.Message);
                return false;
            }
        }

        /// <summary>
        /// Stop BLE advertising.
        /// </summary>
        public void StopAdvertising()
        {
            if (!manager.ConfirmIsInitialized()) return;

            try { manager.Bridge.Call("stopAdvertising"); }
            catch (Exception e)
            {
                LoggingManager.Instance.AddLogError("Exception stopping advertising: " + e.Message);
            }
        }

        /// <summary>
        /// Get current advertising state.
        /// </summary>
        /// <returns>True if advertising is active, false otherwise.</returns>
        public bool GetAdvertisingState()
        {
            if (!manager.ConfirmIsInitialized()) return false;

            try { return manager.Bridge.Call<bool>("isAdvertising"); }
            catch (Exception e)
            {
                LoggingManager.Instance.AddLogError(e.Message);
                return false;
            }
        }

        /// <summary>
        /// Sets the transmit power level for advertising.
        /// Higher power increases range but consumes more battery.
        /// </summary>
        /// <param name="level">The power level (0=LOW, 1=MEDIUM, 2=HIGH)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool SetTransmitPowerLevel(int level)
        {
            if (!manager.ConfirmIsInitialized()) return false;

            if (level < 0 || level > 2)
            {
                LoggingManager.Instance.AddLogError("Invalid TX power level: " + level + ". Must be between 0 and 2.");
                return false;
            }

            try { return manager.Bridge.Call<bool>("setTransmitPowerLevel", level); }
            catch (Exception e)
            {
                LoggingManager.Instance.AddLogError("Exception setting TX power level: " + e.Message);
                return false;
            }
        }
    }
}
