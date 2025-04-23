using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Handles events for the BLE HID system, processing callbacks from the native plugin.
    /// </summary>
    public class BleEventSystem : MonoBehaviour
    {
        private BleHidCallbackHandler callbackHandler;

        public BleHidCallbackHandler.InitializeCompleteHandler OnInitializeComplete = delegate { };
        public BleHidCallbackHandler.AdvertisingStateChangedHandler OnAdvertisingStateChanged = delegate { };
        public BleHidCallbackHandler.ConnectionStateChangedHandler OnConnectionStateChanged = delegate { };
        public BleHidCallbackHandler.PairingStateChangedHandler OnPairingStateChanged = delegate { };
        public BleHidCallbackHandler.ConnectionParametersChangedHandler OnConnectionParametersChanged = delegate { };
        public BleHidCallbackHandler.RssiReadHandler OnRssiRead = delegate { };
        public BleHidCallbackHandler.ConnectionParameterRequestCompleteHandler OnConnectionParameterRequestComplete = delegate { };
        public BleHidCallbackHandler.ErrorHandler OnError = delegate { };
        public BleHidCallbackHandler.DebugLogHandler OnDebugLog = delegate { };
        public BleHidCallbackHandler.PipModeChangedHandler OnPipModeChanged = delegate { };

        public void Awake()
        {
            var manager = BleHidManager.Instance;

            // Create the callback handler
            callbackHandler = new BleHidCallbackHandler(manager);

            // Forward events
            callbackHandler.OnInitializeComplete += (success, message) => OnInitializeComplete?.Invoke(success, message);
            callbackHandler.OnAdvertisingStateChanged += (advertising, message) => OnAdvertisingStateChanged?.Invoke(advertising, message);
            callbackHandler.OnConnectionStateChanged += (connected, deviceName, deviceAddress) => OnConnectionStateChanged?.Invoke(connected, deviceName, deviceAddress);
            callbackHandler.OnPairingStateChanged += (status, deviceAddress) => OnPairingStateChanged?.Invoke(status, deviceAddress);
            callbackHandler.OnConnectionParametersChanged += (interval, latency, timeout, mtu) =>
            {
                manager.ConnectionBridge.ConnectionInterval = interval;
                manager.ConnectionBridge.SlaveLatency = latency;
                manager.ConnectionBridge.SupervisionTimeout = timeout;
                manager.ConnectionBridge.MtuSize = mtu;
                OnConnectionParametersChanged?.Invoke(interval, latency, timeout, mtu);
            };
            callbackHandler.OnRssiRead += (rssi) =>
            {
                manager.ConnectionBridge.Rssi = rssi;
                OnRssiRead?.Invoke(rssi);
            };
            callbackHandler.OnConnectionParameterRequestComplete += (paramName, success, actualValue) =>
                OnConnectionParameterRequestComplete?.Invoke(paramName, success, actualValue);
            callbackHandler.OnError += (errorCode, errorMessage) => OnError?.Invoke(errorCode, errorMessage);
            callbackHandler.OnDebugLog += (message) => OnDebugLog?.Invoke(message);
            callbackHandler.OnPipModeChanged += (isInPipMode) => OnPipModeChanged?.Invoke(isInPipMode);
        }

        public void HandleInitializeComplete(string message)
        {
            callbackHandler.HandleInitializeComplete(message);
        }

        public void HandleAdvertisingStateChanged(string message)
        {
            callbackHandler.HandleAdvertisingStateChanged(message);
        }

        public void HandleConnectionStateChanged(string message)
        {
            callbackHandler.HandleConnectionStateChanged(message);
        }

        public void HandlePairingStateChanged(string message)
        {
            callbackHandler.HandlePairingStateChanged(message);
        }

        public void HandleError(string message)
        {
            callbackHandler.HandleError(message);
        }

        public void HandleDebugLog(string message)
        {
            callbackHandler.HandleDebugLog(message);
        }

        public void HandleConnectionParametersChanged(string message)
        {
            callbackHandler.HandleConnectionParametersChanged(message);
        }

        public void HandleRssiRead(string message)
        {
            callbackHandler.HandleRssiRead(message);
        }

        public void HandleConnectionParameterRequestComplete(string message)
        {
            callbackHandler.HandleConnectionParameterRequestComplete(message);
        }

        public void HandlePipModeChanged(string message)
        {
            callbackHandler.HandlePipModeChanged(message);
        }
    }
}
