using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Manages Bluetooth Low Energy advertising functions for the BLE HID service.
    /// Handles starting, stopping, and monitoring the advertising state.
    /// </summary>
    public class BleHidAdvertisingManager
    {
        /// <summary>
        /// Event that is triggered when advertising state changes
        /// </summary>
        public event BleHidCallbackHandler.AdvertisingStateChangedHandler OnAdvertisingStateChanged;
        
        /// <summary>
        /// Event that is triggered when there's an error with advertising
        /// </summary>
        public event BleHidCallbackHandler.ErrorHandler OnError;
        
        /// <summary>
        /// Gets whether the device is currently advertising
        /// </summary>
        public bool IsAdvertising { get; private set; }
        
        /// <summary>
        /// Gets the current transmit power level
        /// </summary>
        public int TxPowerLevel { get; private set; }
        
        /// <summary>
        /// Reference to the bridge instance for native method calls
        /// </summary>
        private AndroidJavaObject bridgeInstance;
        
        /// <summary>
        /// Constructor requiring the bridge instance for communication
        /// </summary>
        /// <param name="bridgeInstance">The Java bridge instance for the native plugin</param>
        public BleHidAdvertisingManager(AndroidJavaObject bridgeInstance)
        {
            this.bridgeInstance = bridgeInstance;
            IsAdvertising = false;
        }
        
        /// <summary>
        /// Update the advertising state based on native plugin information
        /// </summary>
        /// <param name="advertising">The new advertising state</param>
        /// <param name="message">Additional information about the state change</param>
        internal void UpdateAdvertisingState(bool advertising, string message)
        {
            IsAdvertising = advertising;
            OnAdvertisingStateChanged?.Invoke(advertising, message);
        }
        
        /// <summary>
        /// Start BLE advertising to make this device discoverable.
        /// </summary>
        /// <returns>True if advertising was started successfully, false otherwise.</returns>
        public bool StartAdvertising()
        {
            try
            {
                Debug.Log("BleHidAdvertisingManager: Attempting to start advertising...");

                // Verify Bluetooth is enabled
                string errorMsg;
                if (!BleHidEnvironmentChecker.IsBluetoothEnabled(out errorMsg))
                {
                    Debug.LogError("BleHidAdvertisingManager: " + errorMsg);
                    OnError?.Invoke(BleHidConstants.ERROR_BLUETOOTH_DISABLED, errorMsg);
                    return false;
                }

                // Verify device supports advertising
                if (!BleHidEnvironmentChecker.SupportsBleAdvertising(out errorMsg))
                {
                    Debug.LogError("BleHidAdvertisingManager: " + errorMsg);
                    OnError?.Invoke(BleHidConstants.ERROR_PERIPHERAL_NOT_SUPPORTED, errorMsg);
                    return false;
                }

                // Add extra debug info
                try
                {
                    // Use a simple toString() to get some information about the bridge instance
                    string instanceInfo = bridgeInstance.Call<string>("toString");
                    Debug.Log("BleHidAdvertisingManager: Using bridgeInstance: " + instanceInfo);
                }
                catch (Exception debugEx) { Debug.LogWarning("BleHidAdvertisingManager: Could not get bridge instance info: " + debugEx.Message); }

                bool result = bridgeInstance.Call<bool>("startAdvertising");
                Debug.Log("BleHidAdvertisingManager: StartAdvertising call result: " + result);

                // Verify advertising state
                try
                {
                    bool isAdvertising = bridgeInstance.Call<bool>("isAdvertising");
                    Debug.Log("BleHidAdvertisingManager: isAdvertising check after call: " + isAdvertising);
                }
                catch (Exception verifyEx) { Debug.LogWarning("BleHidAdvertisingManager: Could not verify advertising state: " + verifyEx.Message); }

                return result;
            }
            catch (Exception e)
            {
                string message = "Exception starting advertising: " + e.Message;
                Debug.LogError(message);
                Debug.LogException(e);
                OnError?.Invoke(BleHidConstants.ERROR_ADVERTISING_FAILED, message);
                return false;
            }
        }

        /// <summary>
        /// Stop BLE advertising.
        /// </summary>
        public void StopAdvertising()
        {
            try 
            { 
                bridgeInstance.Call("stopAdvertising");
            }
            catch (Exception e)
            {
                string message = "Exception stopping advertising: " + e.Message;
                Debug.LogException(e);
                OnError?.Invoke(BleHidConstants.ERROR_ADVERTISING_FAILED, message);
            }
        }

        /// <summary>
        /// Get current advertising state directly from the native plugin.
        /// </summary>
        /// <returns>True if advertising is active, false otherwise.</returns>
        public bool GetAdvertisingState()
        {
            try 
            { 
                return bridgeInstance.Call<bool>("isAdvertising"); 
            }
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
            if (level < 0 || level > 2)
            {
                string message = "Invalid TX power level: " + level + ". Must be between 0 and 2.";
                Debug.LogError(message);
                OnError?.Invoke(BleHidConstants.ERROR_INVALID_PARAMETER, message);
                return false;
            }

            try 
            {
                bool result = bridgeInstance.Call<bool>("setTransmitPowerLevel", level);
                if (result)
                {
                    TxPowerLevel = level;
                }
                return result;
            }
            catch (Exception e)
            {
                string message = "Exception setting TX power level: " + e.Message;
                Debug.LogException(e);
                OnError?.Invoke(BleHidConstants.ERROR_INVALID_PARAMETER, message);
                return false;
            }
        }
    }
}
