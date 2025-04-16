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
            if (!manager.BleUtils.ConfirmIsInitialized()) return false;

            try
            {
                Debug.Log("BleHidManager: Attempting to start advertising...");

                // Verify Bluetooth is enabled
                string errorMsg;
                if (!BleHidEnvironmentChecker.IsBluetoothEnabled(out errorMsg))
                {
                    Debug.LogError("BleHidManager: " + errorMsg);
                    manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_BLUETOOTH_DISABLED, errorMsg);
                    return false;
                }

                // Verify device supports advertising
                if (!BleHidEnvironmentChecker.SupportsBleAdvertising(out errorMsg))
                {
                    Debug.LogError("BleHidManager: " + errorMsg);
                    manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_PERIPHERAL_NOT_SUPPORTED, errorMsg);
                    return false;
                }

                // Add extra debug info
                try
                {
                    // Use a simple toString() to get some information about the bridge instance
                    string instanceInfo = manager.BleInitializer.BridgeInstance.Call<string>("toString");
                    Debug.Log("BleHidManager: Using bridgeInstance: " + instanceInfo);
                }
                catch (Exception debugEx) { Debug.LogWarning("BleHidManager: Could not get bridge instance info: " + debugEx.Message); }

                bool result = manager.BleInitializer.BridgeInstance.Call<bool>("startAdvertising");
                Debug.Log("BleHidManager: StartAdvertising call result: " + result);

                // Verify advertising state
                try
                {
                    bool isAdvertising = manager.BleInitializer.BridgeInstance.Call<bool>("isAdvertising");
                    Debug.Log("BleHidManager: isAdvertising check after call: " + isAdvertising);
                }
                catch (Exception verifyEx) { Debug.LogWarning("BleHidManager: Could not verify advertising state: " + verifyEx.Message); }

                return result;
            }
            catch (Exception e)
            {
                string message = "Exception starting advertising: " + e.Message;
                Debug.LogError(message);
                Debug.LogException(e);
                manager.LastErrorMessage = message;
                manager.LastErrorCode = BleHidConstants.ERROR_ADVERTISING_FAILED;
                manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_ADVERTISING_FAILED, message);
                return false;
            }
        }

        /// <summary>
        /// Stop BLE advertising.
        /// </summary>
        public void StopAdvertising()
        {
            if (!manager.BleUtils.ConfirmIsInitialized()) return;

            try { manager.BleInitializer.BridgeInstance.Call("stopAdvertising"); }
            catch (Exception e)
            {
                string message = "Exception stopping advertising: " + e.Message;
                Debug.LogException(e);
                manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_ADVERTISING_FAILED, message);
            }
        }

        /// <summary>
        /// Get current advertising state.
        /// </summary>
        /// <returns>True if advertising is active, false otherwise.</returns>
        public bool GetAdvertisingState()
        {
            if (!manager.BleUtils.ConfirmIsInitialized()) return false;

            try { return manager.BleInitializer.BridgeInstance.Call<bool>("isAdvertising"); }
            catch (Exception e)
            {
                Debug.LogException(e);
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
            if (!manager.BleUtils.ConfirmIsInitialized()) return false;

            if (level < 0 || level > 2)
            {
                string message = "Invalid TX power level: " + level + ". Must be between 0 and 2.";
                Debug.LogError(message);
                manager.LastErrorMessage = message;
                manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_INVALID_PARAMETER, message);
                return false;
            }

            try { return manager.BleInitializer.BridgeInstance.Call<bool>("setTransmitPowerLevel", level); }
            catch (Exception e)
            {
                string message = "Exception setting TX power level: " + e.Message;
                Debug.LogException(e);
                manager.LastErrorMessage = message;
                manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_INVALID_PARAMETER, message);
                return false;
            }
        }
    }
}
