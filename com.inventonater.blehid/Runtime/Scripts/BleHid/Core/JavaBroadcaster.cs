using UnityEngine;
using UnityEngine.Scripting;

namespace Inventonater
{
    /// <summary>
    /// Handles events for the BLE HID system, processing callbacks from the native plugin.
    /// </summary>
    public class JavaBroadcaster : MonoBehaviour
    {
        public delegate void InitializeCompleteHandler(bool success, string message);
        public delegate void AdvertisingStateChangedHandler(bool advertising, string message);
        public delegate void ConnectionStateChangedHandler(bool connected, string deviceName, string deviceAddress);
        public delegate void PairingStateChangedHandler(string status, string deviceAddress);
        public delegate void ConnectionParametersChangedHandler(int interval, int latency, int timeout, int mtu);
        public delegate void RssiReadHandler(int rssi);
        public delegate void ConnectionParameterRequestCompleteHandler(string parameterName, bool success, string actualValue);
        public delegate void ErrorHandler(int errorCode, string errorMessage);
        public delegate void DebugLogHandler(string message);
        public delegate void PipModeChangedHandler(bool isInPipMode);

        public event InitializeCompleteHandler OnInitializeComplete = delegate { };
        public event AdvertisingStateChangedHandler OnAdvertisingStateChanged = delegate { };
        public event ConnectionStateChangedHandler OnConnectionStateChanged = delegate { };
        public event PairingStateChangedHandler OnPairingStateChanged = delegate { };
        public event ConnectionParametersChangedHandler OnConnectionParametersChanged = delegate { };
        public event RssiReadHandler OnRssiRead = delegate { };
        public event ConnectionParameterRequestCompleteHandler OnConnectionParameterRequestComplete = delegate { };
        public event ErrorHandler OnError = delegate { };
        public event DebugLogHandler OnDebugLog = delegate { };
        public event PipModeChangedHandler OnPipModeChanged = delegate { };

        [Preserve]
        public void HandleInitializeComplete(string message)
        {
            string[] parts = message.Split(new[] { ':' }, 2);
            bool success = bool.Parse(parts[0]);
            string msg = parts.Length > 1 ? parts[1] : "";

            if (success) Debug.Log("BLE HID initialized successfully: " + msg);
            else Debug.LogError("BLE HID initialization failed: " + msg);

            OnInitializeComplete?.Invoke(success, msg);
        }

        [Preserve]
        public void HandleAdvertisingStateChanged(string message)
        {
            string[] parts = message.Split(new char[] { ':' }, 2);
            bool advertising = bool.Parse(parts[0]);
            string msg = parts.Length > 1 ? parts[1] : "";

            if (advertising) Debug.Log("BLE advertising started: " + msg);
            else Debug.Log("BLE advertising stopped: " + msg);

            OnAdvertisingStateChanged?.Invoke(advertising, msg);
        }

        [Preserve]
        public void HandleConnectionStateChanged(string message)
        {
            string[] parts = message.Split(':');
            bool connected = bool.Parse(parts[0]);
            string deviceName = parts.Length > 1 ? parts[1] : "";
            string deviceAddress = parts.Length > 2 ? parts[2] : "";

            if (connected) Debug.Log($"BLE device connected: {deviceName} ({deviceAddress})");
            else Debug.Log("BLE device disconnected");

            OnConnectionStateChanged?.Invoke(connected, deviceName, deviceAddress);
        }

        [Preserve]
        public void HandlePairingStateChanged(string message)
        {
            string[] parts = message.Split(':');
            string status = parts[0];
            string deviceAddress = parts.Length > 1 ? parts[1] : null;

            Debug.Log($"BLE pairing state changed: {status}" + (deviceAddress != null ? $" ({deviceAddress})" : ""));
            OnPairingStateChanged?.Invoke(status, deviceAddress);
        }

        [Preserve]
        public void HandleError(string message)
        {
            string[] parts = message.Split(new[] { ':' }, 2);
            int errorCode = int.Parse(parts[0]);
            string errorMessage = parts.Length > 1 ? parts[1] : "";
            OnError?.Invoke(errorCode, errorMessage);
            LoggingManager.Instance.Error($"BLE HID error {errorCode}: {errorMessage}");
        }

        [Preserve]
        public void HandleDebugLog(string message)
        {
            Debug.Log("BLE HID [Debug]: " + message);
            OnDebugLog?.Invoke(message);
        }

        [Preserve]
        public void HandleConnectionParametersChanged(string message)
        {
            string[] parts = message.Split(':');
            if (parts.Length < 4) return;
            int interval = int.Parse(parts[0]);
            int latency = int.Parse(parts[1]);
            int timeout = int.Parse(parts[2]);
            int mtu = int.Parse(parts[3]);

            Debug.Log($"Connection parameters changed: interval={interval}ms, latency={latency}, timeout={timeout}ms, MTU={mtu}");

            OnConnectionParametersChanged?.Invoke(interval, latency, timeout, mtu);
        }

        [Preserve]
        public void HandleRssiRead(string message)
        {
            int rssi = int.Parse(message);
            OnRssiRead?.Invoke(rssi);
        }

        [Preserve]
        public void HandleConnectionParameterRequestComplete(string message)
        {
            string[] parts = message.Split(new char[] { ':' }, 3);
            if (parts.Length < 3) return;
            string parameterName = parts[0];
            bool success = bool.Parse(parts[1]);
            string actualValue = parts[2];

            Debug.Log($"Parameter request complete: {parameterName}, success={success}, actual={actualValue}");
            OnConnectionParameterRequestComplete?.Invoke(parameterName, success, actualValue);
        }

        [Preserve]
        public void HandlePipModeChanged(string message)
        {
            bool isInPipMode = bool.Parse(message);
            Debug.Log("BLE HID PiP mode changed: " + (isInPipMode ? "Enter PiP" : "Exit PiP"));
            OnPipModeChanged?.Invoke(isInPipMode);
        }
    }
}
