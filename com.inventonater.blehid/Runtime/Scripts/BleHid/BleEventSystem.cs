using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Handles events for the BLE HID system, processing callbacks from the native plugin.
    /// </summary>
    public class BleEventSystem : MonoBehaviour
    {
        private BleHidManager manager;
        private BleHidCallbackHandler callbackHandler;

        // Event declarations that will be forwarded from the callback handler
        public BleHidCallbackHandler.InitializeCompleteHandler OnInitializeComplete;
        public BleHidCallbackHandler.AdvertisingStateChangedHandler OnAdvertisingStateChanged;
        public BleHidCallbackHandler.ConnectionStateChangedHandler OnConnectionStateChanged;
        public BleHidCallbackHandler.PairingStateChangedHandler OnPairingStateChanged;
        public BleHidCallbackHandler.ConnectionParametersChangedHandler OnConnectionParametersChanged;
        public BleHidCallbackHandler.RssiReadHandler OnRssiRead;
        public BleHidCallbackHandler.ConnectionParameterRequestCompleteHandler OnConnectionParameterRequestComplete;
        public BleHidCallbackHandler.ErrorHandler OnError;
        public BleHidCallbackHandler.DebugLogHandler OnDebugLog;

        public void Awake()
        {
            this.manager = BleHidManager.Instance;
            
            // Create the callback handler
            callbackHandler = new BleHidCallbackHandler(manager);

            // Forward events
            callbackHandler.OnInitializeComplete += (success, message) => OnInitializeComplete?.Invoke(success, message);
            callbackHandler.OnAdvertisingStateChanged += (advertising, message) => OnAdvertisingStateChanged?.Invoke(advertising, message);
            callbackHandler.OnConnectionStateChanged += (connected, deviceName, deviceAddress) => OnConnectionStateChanged?.Invoke(connected, deviceName, deviceAddress);
            callbackHandler.OnPairingStateChanged += (status, deviceAddress) => OnPairingStateChanged?.Invoke(status, deviceAddress);
            callbackHandler.OnConnectionParametersChanged += (interval, latency, timeout, mtu) =>
            {
                manager.ConnectionInterval = interval;
                manager.SlaveLatency = latency;
                manager.SupervisionTimeout = timeout;
                manager.MtuSize = mtu;
                OnConnectionParametersChanged?.Invoke(interval, latency, timeout, mtu);
            };
            callbackHandler.OnRssiRead += (rssi) =>
            {
                manager.Rssi = rssi;
                OnRssiRead?.Invoke(rssi);
            };
            callbackHandler.OnConnectionParameterRequestComplete += (paramName, success, actualValue) =>
                OnConnectionParameterRequestComplete?.Invoke(paramName, success, actualValue);
            callbackHandler.OnError += (errorCode, errorMessage) => OnError?.Invoke(errorCode, errorMessage);
            callbackHandler.OnDebugLog += (message) => OnDebugLog?.Invoke(message);
        }

        /// <summary>
        /// Called when initialization is complete.
        /// </summary>
        public void HandleInitializeComplete(string message)
        {
            callbackHandler.HandleInitializeComplete(message);
        }

        /// <summary>
        /// Called when the advertising state changes.
        /// </summary>
        public void HandleAdvertisingStateChanged(string message)
        {
            callbackHandler.HandleAdvertisingStateChanged(message);
        }

        /// <summary>
        /// Called when the connection state changes.
        /// </summary>
        public void HandleConnectionStateChanged(string message)
        {
            callbackHandler.HandleConnectionStateChanged(message);
        }

        /// <summary>
        /// Called when the pairing state changes.
        /// </summary>
        public void HandlePairingStateChanged(string message)
        {
            callbackHandler.HandlePairingStateChanged(message);
        }

        /// <summary>
        /// Called when an error occurs.
        /// </summary>
        public void HandleError(string message)
        {
            callbackHandler.HandleError(message);
        }

        /// <summary>
        /// Called for debug log messages.
        /// </summary>
        public void HandleDebugLog(string message)
        {
            callbackHandler.HandleDebugLog(message);
        }

        /// <summary>
        /// Called when connection parameters are updated.
        /// </summary>
        public void HandleConnectionParametersChanged(string message)
        {
            callbackHandler.HandleConnectionParametersChanged(message);
        }

        /// <summary>
        /// Called when RSSI is read.
        /// </summary>
        public void HandleRssiRead(string message)
        {
            callbackHandler.HandleRssiRead(message);
        }

        /// <summary>
        /// Called when a connection parameter change request is completed.
        /// </summary>
        public void HandleConnectionParameterRequestComplete(string message)
        {
            callbackHandler.HandleConnectionParameterRequestComplete(message);
        }
    }
}
