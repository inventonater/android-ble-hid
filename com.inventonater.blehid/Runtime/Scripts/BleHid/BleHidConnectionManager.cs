using System;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Manages Bluetooth Low Energy connection functionality for the BLE HID service.
    /// Handles connection state, connection parameters, and RSSI readings.
    /// </summary>
    public class BleHidConnectionManager
    {
        /// <summary>
        /// Event that is triggered when connection state changes
        /// </summary>
        public event BleHidCallbackHandler.ConnectionStateChangedHandler OnConnectionStateChanged;
        
        /// <summary>
        /// Event that is triggered when pairing state changes
        /// </summary>
        public event BleHidCallbackHandler.PairingStateChangedHandler OnPairingStateChanged;
        
        /// <summary>
        /// Event that is triggered when connection parameters change
        /// </summary>
        public event BleHidCallbackHandler.ConnectionParametersChangedHandler OnConnectionParametersChanged;
        
        /// <summary>
        /// Event that is triggered when RSSI is read
        /// </summary>
        public event BleHidCallbackHandler.RssiReadHandler OnRssiRead;
        
        /// <summary>
        /// Event that is triggered when a connection parameter request completes
        /// </summary>
        public event BleHidCallbackHandler.ConnectionParameterRequestCompleteHandler OnConnectionParameterRequestComplete;
        
        /// <summary>
        /// Event that is triggered when there's an error with a connection operation
        /// </summary>
        public event BleHidCallbackHandler.ErrorHandler OnError;
        
        /// <summary>
        /// Gets whether a device is currently connected
        /// </summary>
        public bool IsConnected { get; private set; }
        
        /// <summary>
        /// Gets the name of the connected device, if any
        /// </summary>
        public string ConnectedDeviceName { get; private set; }
        
        /// <summary>
        /// Gets the address of the connected device, if any
        /// </summary>
        public string ConnectedDeviceAddress { get; private set; }
        
        /// <summary>
        /// Gets the connection interval in units of 1.25ms
        /// </summary>
        public int ConnectionInterval { get; private set; }
        
        /// <summary>
        /// Gets the slave latency (number of connection events that can be skipped)
        /// </summary>
        public int SlaveLatency { get; private set; }
        
        /// <summary>
        /// Gets the supervision timeout in units of 10ms
        /// </summary>
        public int SupervisionTimeout { get; private set; }
        
        /// <summary>
        /// Gets the MTU (Maximum Transmission Unit) size in bytes
        /// </summary>
        public int MtuSize { get; private set; }
        
        /// <summary>
        /// Gets the most recently read RSSI value
        /// </summary>
        public int Rssi { get; private set; }
        
        /// <summary>
        /// Reference to the bridge instance for native method calls
        /// </summary>
        private AndroidJavaObject bridgeInstance;
        
        /// <summary>
        /// Constructor requiring the bridge instance for communication
        /// </summary>
        /// <param name="bridgeInstance">The Java bridge instance for the native plugin</param>
        public BleHidConnectionManager(AndroidJavaObject bridgeInstance)
        {
            this.bridgeInstance = bridgeInstance;
            IsConnected = false;
            ConnectedDeviceName = null;
            ConnectedDeviceAddress = null;
        }
        
        /// <summary>
        /// Update the connection state based on native plugin information
        /// </summary>
        /// <param name="connected">The new connection state</param>
        /// <param name="deviceName">The name of the connected device</param>
        /// <param name="deviceAddress">The address of the connected device</param>
        internal void UpdateConnectionState(bool connected, string deviceName, string deviceAddress)
        {
            IsConnected = connected;
            ConnectedDeviceName = deviceName;
            ConnectedDeviceAddress = deviceAddress;
            OnConnectionStateChanged?.Invoke(connected, deviceName, deviceAddress);
        }
        
        /// <summary>
        /// Update pairing state based on native plugin information
        /// </summary>
        /// <param name="status">The pairing status</param>
        /// <param name="deviceAddress">The address of the paired device</param>
        internal void UpdatePairingState(string status, string deviceAddress)
        {
            OnPairingStateChanged?.Invoke(status, deviceAddress);
        }
        
        /// <summary>
        /// Update connection parameters based on native plugin information
        /// </summary>
        /// <param name="interval">The connection interval in units of 1.25ms</param>
        /// <param name="latency">The slave latency</param>
        /// <param name="timeout">The supervision timeout in units of 10ms</param>
        /// <param name="mtu">The MTU size in bytes</param>
        internal void UpdateConnectionParameters(int interval, int latency, int timeout, int mtu)
        {
            ConnectionInterval = interval;
            SlaveLatency = latency;
            SupervisionTimeout = timeout;
            MtuSize = mtu;
            OnConnectionParametersChanged?.Invoke(interval, latency, timeout, mtu);
        }
        
        /// <summary>
        /// Update RSSI value based on native plugin information
        /// </summary>
        /// <param name="rssi">The RSSI value</param>
        internal void UpdateRssi(int rssi)
        {
            Rssi = rssi;
            OnRssiRead?.Invoke(rssi);
        }
        
        /// <summary>
        /// Update connection parameter request result
        /// </summary>
        /// <param name="paramName">The parameter name</param>
        /// <param name="success">Whether the request was successful</param>
        /// <param name="actualValue">The actual value set</param>
        internal void UpdateConnectionParameterRequestComplete(string paramName, bool success, int actualValue)
        {
            OnConnectionParameterRequestComplete?.Invoke(paramName, success, actualValue);
        }
        
        /// <summary>
        /// Request a change in connection priority.
        /// Connection priority affects latency and power consumption.
        /// </summary>
        /// <param name="priority">The priority to request (0=HIGH, 1=BALANCED, 2=LOW_POWER)</param>
        /// <returns>True if the request was sent, false otherwise.</returns>
        public bool RequestConnectionPriority(int priority)
        {
            if (!IsConnected)
            {
                string message = "Cannot request connection priority when not connected";
                Debug.LogError(message);
                OnError?.Invoke(BleHidConstants.ERROR_NOT_CONNECTED, message);
                return false;
            }

            try 
            { 
                return bridgeInstance.Call<bool>("requestConnectionPriority", priority); 
            }
            catch (Exception e)
            {
                string message = "Exception requesting connection priority: " + e.Message;
                Debug.LogException(e);
                OnError?.Invoke(BleHidConstants.ERROR_NOT_CONNECTED, message);
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
            if (!IsConnected)
            {
                string message = "Cannot request MTU when not connected";
                Debug.LogError(message);
                OnError?.Invoke(BleHidConstants.ERROR_NOT_CONNECTED, message);
                return false;
            }

            if (mtu < 23 || mtu > 517)
            {
                string message = "Invalid MTU size: " + mtu + ". Must be between 23 and 517.";
                Debug.LogError(message);
                OnError?.Invoke(BleHidConstants.ERROR_INVALID_PARAMETER, message);
                return false;
            }

            try 
            { 
                return bridgeInstance.Call<bool>("requestMtu", mtu); 
            }
            catch (Exception e)
            {
                string message = "Exception requesting MTU: " + e.Message;
                Debug.LogException(e);
                OnError?.Invoke(BleHidConstants.ERROR_NOT_CONNECTED, message);
                return false;
            }
        }

        /// <summary>
        /// Reads the current RSSI (signal strength) value.
        /// </summary>
        /// <returns>True if the read request was sent, false otherwise.</returns>
        public bool ReadRssi()
        {
            if (!IsConnected)
            {
                string message = "Cannot read RSSI when not connected";
                Debug.LogError(message);
                OnError?.Invoke(BleHidConstants.ERROR_NOT_CONNECTED, message);
                return false;
            }

            try 
            { 
                return bridgeInstance.Call<bool>("readRssi"); 
            }
            catch (Exception e)
            {
                string message = "Exception reading RSSI: " + e.Message;
                Debug.LogException(e);
                OnError?.Invoke(BleHidConstants.ERROR_NOT_CONNECTED, message);
                return false;
            }
        }

        /// <summary>
        /// Gets all connection parameters as a dictionary.
        /// </summary>
        /// <returns>Dictionary of parameter names to values, or null if not connected.</returns>
        public Dictionary<string, string> GetConnectionParameters()
        {
            if (!IsConnected)
            {
                Debug.LogError("Cannot get connection parameters when not connected");
                return null;
            }

            try
            {
                AndroidJavaObject parametersMap = bridgeInstance.Call<AndroidJavaObject>("getConnectionParameters");
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
                string message = "Exception getting connection parameters: " + e.Message;
                Debug.LogException(e);
                return null;
            }
        }
    }
}
