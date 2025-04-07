using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Handles callbacks from the Java/Android native code.
    /// This class processes messages from the native plugin and converts them
    /// into C# events that can be consumed by Unity scripts.
    /// </summary>
    public class BleHidCallbackHandler
    {
        // Event delegate types
        public delegate void InitializeCompleteHandler(bool success, string message);
        public delegate void AdvertisingStateChangedHandler(bool advertising, string message);
        public delegate void ConnectionStateChangedHandler(bool connected, string deviceName, string deviceAddress);
        public delegate void PairingStateChangedHandler(string status, string deviceAddress);
        public delegate void ErrorHandler(int errorCode, string errorMessage);
        public delegate void DebugLogHandler(string message);
        
        // Events that can be subscribed to
        public event InitializeCompleteHandler OnInitializeComplete;
        public event AdvertisingStateChangedHandler OnAdvertisingStateChanged;
        public event ConnectionStateChangedHandler OnConnectionStateChanged;
        public event PairingStateChangedHandler OnPairingStateChanged;
        public event ErrorHandler OnError;
        public event DebugLogHandler OnDebugLog;
        
        // Reference to the main manager to update its state
        private BleHidManager manager;
        
        /// <summary>
        /// Creates a new callback handler associated with a manager instance.
        /// </summary>
        /// <param name="manager">The BleHidManager instance to update</param>
        public BleHidCallbackHandler(BleHidManager manager)
        {
            this.manager = manager;
        }
        
        /// <summary>
        /// Called when initialization is complete.
        /// </summary>
        public void HandleInitializeComplete(string message)
        {
            string[] parts = message.Split(new char[] { ':' }, 2);
            bool success = bool.Parse(parts[0]);
            string msg = parts.Length > 1 ? parts[1] : "";
            
            manager.IsInitialized = success;
            
            if (success)
            {
                Debug.Log("BLE HID initialized successfully: " + msg);
            }
            else
            {
                Debug.LogError("BLE HID initialization failed: " + msg);
            }
            
            OnInitializeComplete?.Invoke(success, msg);
        }
        
        /// <summary>
        /// Called when the advertising state changes.
        /// </summary>
        public void HandleAdvertisingStateChanged(string message)
        {
            string[] parts = message.Split(new char[] { ':' }, 2);
            bool advertising = bool.Parse(parts[0]);
            string msg = parts.Length > 1 ? parts[1] : "";
            
            manager.IsAdvertising = advertising;
            
            if (advertising)
            {
                Debug.Log("BLE advertising started: " + msg);
            }
            else
            {
                Debug.Log("BLE advertising stopped: " + msg);
            }
            
            OnAdvertisingStateChanged?.Invoke(advertising, msg);
        }
        
        /// <summary>
        /// Called when the connection state changes.
        /// </summary>
        public void HandleConnectionStateChanged(string message)
        {
            string[] parts = message.Split(':');
            bool connected = bool.Parse(parts[0]);
            string deviceName = null;
            string deviceAddress = null;
            
            if (connected && parts.Length >= 3)
            {
                deviceName = parts[1];
                deviceAddress = parts[2];
            }
            
            manager.IsConnected = connected;
            manager.ConnectedDeviceName = deviceName;
            manager.ConnectedDeviceAddress = deviceAddress;
            
            if (connected)
            {
                Debug.Log($"BLE device connected: {deviceName} ({deviceAddress})");
            }
            else
            {
                Debug.Log("BLE device disconnected");
            }
            
            OnConnectionStateChanged?.Invoke(connected, deviceName, deviceAddress);
        }
        
        /// <summary>
        /// Called when the pairing state changes.
        /// </summary>
        public void HandlePairingStateChanged(string message)
        {
            string[] parts = message.Split(':');
            string status = parts[0];
            string deviceAddress = parts.Length > 1 ? parts[1] : null;
            
            Debug.Log($"BLE pairing state changed: {status}" + (deviceAddress != null ? $" ({deviceAddress})" : ""));
            
            OnPairingStateChanged?.Invoke(status, deviceAddress);
        }
        
        /// <summary>
        /// Called when an error occurs.
        /// </summary>
        public void HandleError(string message)
        {
            string[] parts = message.Split(new char[] { ':' }, 2);
            int errorCode = int.Parse(parts[0]);
            string errorMessage = parts.Length > 1 ? parts[1] : "";
            
            manager.LastErrorCode = errorCode;
            manager.LastErrorMessage = errorMessage;
            
            Debug.LogError($"BLE HID error {errorCode}: {errorMessage}");
            
            OnError?.Invoke(errorCode, errorMessage);
        }
        
        /// <summary>
        /// Called for debug log messages.
        /// </summary>
        public void HandleDebugLog(string message)
        {
            Debug.Log("BLE HID [Debug]: " + message);
            
            OnDebugLog?.Invoke(message);
        }
    }
}
