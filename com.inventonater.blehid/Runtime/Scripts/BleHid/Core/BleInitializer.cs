using System;
using System.Collections;
using System.Linq;
using Unity.Profiling;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Handles initialization of the BLE HID functionality, including permissions and environment checks.
    /// </summary>
    public class BleInitializer
    {
        private readonly BleHidManager _manager;
        public AndroidJavaObject BridgeInstance { get; private set; }
        static readonly ProfilerMarker _marker = new("BleHid.BleInitializer.BridgeInstance.Call");

        private bool _verbose = true;

        public void Call(string methodName, params object[] args)
        {
            using var profilerMarker = _marker.Auto();

            if (_verbose) LoggingManager.Instance.AddLogEntry($" -- {methodName} {string.Join(", ", args)}");
            if (Application.isEditor) return;

            BridgeInstance.Call(methodName, args);
        }

        public T Call<T>(string methodName, params object[] args)
        {
            using var profilerMarker = _marker.Auto();

            if (_verbose) LoggingManager.Instance.AddLogEntry($" -- {methodName} {string.Join(", ", args)}");
            if (Application.isEditor) return default;

            return BridgeInstance.Call<T>(methodName, args);
        }

        private bool isInitializing = false;
        public BleInitializer(BleHidManager manager) => _manager = manager;

        /// <summary>
        /// Initialize the BLE HID functionality.
        /// </summary>
        /// <returns>A coroutine that can be used with StartCoroutine.</returns>
        public IEnumerator Initialize()
        {
            if (isInitializing)
            {
                Debug.LogWarning("BleHidManager: Already initializing");
                yield break;
            }

            if (_manager.IsInitialized)
            {
                Debug.LogWarning("BleHidManager: Already initialized");
                yield break;
            }

            isInitializing = true;
            Debug.Log("BleHidManager: Initializing...");

            Application.runInBackground = true;

            if (Application.isEditor)
            {
                string message = "BLE HID is only supported on Android";
                LoggingManager.Instance.AddLogEntry(message);
                _manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_PERIPHERAL_NOT_SUPPORTED, message);
                _manager.BleEventSystem.OnInitializeComplete(false, message);
                isInitializing = false;
                yield break;
            }

            yield return _manager.StartCoroutine(BleHidPermissionHandler.RequestPermissionsAndWait());

            // Check if required permissions were granted
            if (!BleHidPermissionHandler.CheckBluetoothPermissions())
            {
                string message = "Bluetooth permissions not granted";
                Debug.LogError(message);
                _manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_PERMISSIONS_NOT_GRANTED, message);
                _manager.BleEventSystem.OnInitializeComplete?.Invoke(false, message);
                isInitializing = false;
                yield break;
            }
            Debug.Log("Bluetooth permissions granted");

            bool initResult = false;

            try
            {
                // Only try the new namespace - no fallback
                Debug.Log("Connecting to com.inventonater.blehid.unity namespace...");
                AndroidJavaClass bridgeClass = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityBridge");
                BridgeInstance = bridgeClass.CallStatic<AndroidJavaObject>("getInstance");
                Debug.Log("Successfully connected to com.inventonater.blehid.unity.BleHidUnityBridge");

                // Initialize the bridge with this GameObject's name for callbacks
                initResult = BridgeInstance.Call<bool>("initialize", _manager.gameObject.name);

                // Initialize the foreground service manager
                // manager.ForegroundServiceManager.Initialize(bridgeInstance);

                if (!initResult)
                {
                    string message = "Failed to initialize BLE HID plugin";
                    Debug.LogError(message);
                    _manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_INITIALIZATION_FAILED, message);
                    _manager.BleEventSystem.OnInitializeComplete?.Invoke(false, message);
                    isInitializing = false;
                    yield break;
                }
            }
            catch (Exception e)
            {
                string message = "Exception during initialization: " + e.Message;
                Debug.LogException(e);
                _manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_INITIALIZATION_FAILED, message);
                _manager.BleEventSystem.OnInitializeComplete?.Invoke(false, message);
                isInitializing = false;
                yield break;
            }

            // If we get here, the bridge was initialized successfully, but we still need to wait
            // for the callback to set IsInitialized
            if (initResult)
            {
                // Wait for initialization to complete via callback
                float timeout = 5.0f; // 5 seconds timeout
                float startTime = Time.time;

                while (!_manager.IsInitialized && (Time.time - startTime) < timeout) { yield return null; }

                if (!_manager.IsInitialized)
                {
                    string message = "BLE HID initialization timed out";
                    Debug.LogError(message);
                    _manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_INITIALIZATION_FAILED, message);
                    _manager.BleEventSystem.OnInitializeComplete?.Invoke(false, message);
                }
                else
                {
                    // Initialize device identity for consistent recognition across app restarts
                    InitializeDeviceIdentity();
                }
            }

            isInitializing = false;
        }

        /// <summary>
        /// Initialize the device identity to ensure consistent recognition across app restarts.
        /// </summary>
        private void InitializeDeviceIdentity()
        {
            try
            {
                bool identitySet = _manager.IdentityManager.InitializeIdentity();
                if (identitySet) { Debug.Log("Device identity initialized successfully"); }
                else { Debug.LogWarning("Failed to initialize device identity"); }
            }
            catch (Exception e)
            {
                Debug.LogError($"Error initializing device identity: {e.Message}");
                // Don't fail BLE initialization if identity fails - it's a non-critical feature
            }
        }


        /// <summary>
        /// Close the plugin and release all resources.
        /// </summary>
        public void Close()
        {
            if (BridgeInstance != null)
            {
                try { BridgeInstance.Call("close"); }
                catch (Exception e) { Debug.LogException(e); }

                BridgeInstance.Dispose();
                BridgeInstance = null;
            }

            _manager.IsInitialized = false;
            _manager.IsAdvertising = false;
            _manager.IsConnected = false;
            _manager.ConnectedDeviceName = null;
            _manager.ConnectedDeviceAddress = null;

            Debug.Log("BleHidManager closed");
        }
    }
}
