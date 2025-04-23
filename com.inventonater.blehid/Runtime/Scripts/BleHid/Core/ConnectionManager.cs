using System;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Manages BLE connection parameters and connection-related functionality.
    /// </summary>
    public class ConnectionManager
    {
        private BleHidManager manager;

        public ConnectionManager(BleHidManager manager)
        {
            this.manager = manager;
        }

        /// <summary>
        /// Request a change in connection priority.
        /// Connection priority affects latency and power consumption.
        /// </summary>
        /// <param name="priority">The priority to request (0=HIGH, 1=BALANCED, 2=LOW_POWER)</param>
        /// <returns>True if the request was sent, false otherwise.</returns>
        public bool RequestConnectionPriority(int priority)
        {
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.Call<bool>("requestConnectionPriority", priority); }
            catch (Exception e)
            {
                LoggingManager.Instance.AddLogError("Exception requesting connection priority: " + e.Message);
                return false;
            }
        }

        /// <summary>
        /// Request a change in MTU (Maximum Transmission Unit) size.
        /// Larger MTU sizes can improve throughput.
        /// </summary>
        /// <param name="mtu">The MTU size to request (23-517 bytes)</param>
        /// <returns>True if the request was sent, false otherwise.</returns>
        public bool RequestMtu(int mtu)
        {
            if (!manager.ConfirmIsConnected()) return false;

            if (mtu < 23 || mtu > 517)
            {
                LoggingManager.Instance.AddLogError("Invalid MTU size: " + mtu + ". Must be between 23 and 517.");
                return false;
            }

            try { return manager.BleInitializer.Call<bool>("requestMtu", mtu); }
            catch (Exception e)
            {
                LoggingManager.Instance.AddLogError("Exception requesting MTU: " + e.Message);
                return false;
            }
        }

        /// <summary>
        /// Reads the current RSSI (signal strength) value.
        /// </summary>
        /// <returns>True if the read request was sent, false otherwise.</returns>
        public bool ReadRssi()
        {
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.Call<bool>("readRssi"); }
            catch (Exception e)
            {
                LoggingManager.Instance.AddLogError("Exception reading RSSI: " + e.Message);
                return false;
            }
        }

        /// <summary>
        /// Gets all connection parameters as a dictionary.
        /// </summary>
        /// <returns>Dictionary of parameter names to values, or null if not connected.</returns>
        public Dictionary<string, string> GetConnectionParameters()
        {
            if (!manager.ConfirmIsConnected()) return null;

            try
            {
                AndroidJavaObject parametersMap = manager.BleInitializer.Call<AndroidJavaObject>("getConnectionParameters");
                if (parametersMap == null) { return null; }

                Dictionary<string, string> result = new Dictionary<string, string>();

                // Convert Java Map to C# Dictionary
                using (AndroidJavaObject entrySet = parametersMap.Call<AndroidJavaObject>("entrySet"))
                using (AndroidJavaObject iterator = entrySet.Call<AndroidJavaObject>("iterator"))
                {
                    while (iterator.Call<bool>("hasNext"))
                    {
                        using (AndroidJavaObject entry = iterator.Call<AndroidJavaObject>("next"))
                        {
                            string key = entry.Call<AndroidJavaObject>("getKey").Call<string>("toString");
                            string value = entry.Call<AndroidJavaObject>("getValue").Call<string>("toString");
                            result[key] = value;
                        }
                    }
                }

                return result;
            }
            catch (Exception e)
            {
                LoggingManager.Instance.AddLogError("Exception getting connection parameters: " + e.Message);
                return null;
            }
        }
        
        /// <summary>
        /// Disconnects from the currently connected device.
        /// </summary>
        /// <returns>True if the disconnect command was sent successfully, false otherwise.</returns>
        public bool Disconnect()
        {
            if (!manager.IsConnected) return true; // Already disconnected
            
            try 
            { 
                bool success = manager.BleInitializer.Call<bool>("disconnect");
                if (success)
                {
                    Debug.Log("Disconnect request sent successfully");
                }
                return success;
            }
            catch (Exception e)
            {
                LoggingManager.Instance.AddLogError("Exception disconnecting: " + e.Message);
                return false;
            }
        }
    }
}
