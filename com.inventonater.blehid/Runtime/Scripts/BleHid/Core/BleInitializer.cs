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
        public bool IsInitialized { get; private set; }

        static readonly ProfilerMarker _marker = new("BleHid.BleInitializer.BridgeInstance.Call");
        private readonly bool _verbose = true;

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

        public BleInitializer(BleHidManager manager) => _manager = manager;

        /// <summary>
        /// Initialize the BLE HID functionality.
        /// </summary>
        /// <returns>A coroutine that can be used with StartCoroutine.</returns>
        public void Initialize()
        {
            Application.runInBackground = true;
            if (Application.isEditor)
            {
                LoggingManager.Instance.AddLogEntry("BLE HID is only supported on Android");
                return;
            }

            try
            {
                BleHidPermissionHandler.RequestAllPermissions();

                AndroidJavaClass bridgeClass = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityBridge");
                BridgeInstance = bridgeClass.CallStatic<AndroidJavaObject>("getInstance");

                Debug.Log("Successfully connected to com.inventonater.blehid.unity.BleHidUnityBridge");

                IsInitialized = BridgeInstance.Call<bool>("initialize", _manager.gameObject.name);

                // Initialize the foreground service manager
                // manager.ForegroundServiceManager.Initialize(bridgeInstance);

                if (!IsInitialized)
                {
                    string message = "Failed to initialize BLE HID plugin";
                    LoggingManager.Instance.AddLogError(message);
                    _manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_INITIALIZATION_FAILED, message);
                    _manager.BleEventSystem.OnInitializeComplete?.Invoke(false, message);
                    return;
                }

                _manager.IdentityManager.InitializeIdentity();
            }
            catch (Exception e)
            {
                string message = "Exception during initialization: " + e.Message;
                LoggingManager.Instance.AddLogError(message);
                _manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_INITIALIZATION_FAILED, message);
                _manager.BleEventSystem.OnInitializeComplete?.Invoke(false, message);
            }
        }


        public void Close()
        {
            if (BridgeInstance != null)
            {
                try { BridgeInstance.Call("close"); }
                catch (Exception e) { Debug.LogException(e); }

                BridgeInstance.Dispose();
                BridgeInstance = null;
            }

            _manager.IsAdvertising = false;
            _manager.IsConnected = false;
            _manager.ConnectedDeviceName = null;
            _manager.ConnectedDeviceAddress = null;

            Debug.Log("BleHidManager closed");
        }
    }
}
