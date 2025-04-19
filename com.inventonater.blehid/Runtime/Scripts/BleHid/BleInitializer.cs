using System;
using System.Collections;
using System.Linq;
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

        private bool _verbose = true;
        public void Call(string methodName, params object[] args)
        {
            if(_verbose) LoggingManager.Instance.AddLogEntry($" -- {methodName} {string.Join(", ", args)}");
            if (Application.isEditor) return;

            BridgeInstance.Call(methodName, args);
        }

        public T Call<T>(string methodName, params object[] args)
        {
            if(_verbose) LoggingManager.Instance.AddLogEntry($" -- {methodName} {string.Join(", ", args)}");
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

            // Only run on Android
            if (Application.platform != RuntimePlatform.Android)
            {
                string message = "BLE HID is only supported on Android";
                Debug.LogWarning(message);
                _manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_PERIPHERAL_NOT_SUPPORTED, message);
                _manager.BleEventSystem.OnInitializeComplete?.Invoke(false, message);
                isInitializing = false;
                yield break;
            }

            // Check if plugins are loaded
            string errorMsg;
            if (!BleHidEnvironmentChecker.VerifyPluginsLoaded(out errorMsg))
            {
                Debug.LogError(errorMsg);
                _manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_INITIALIZATION_FAILED, errorMsg);
                _manager.BleEventSystem.OnInitializeComplete?.Invoke(false, errorMsg);
                isInitializing = false;
                yield break;
            }

            // Request runtime permissions for Android
            if (Application.platform == RuntimePlatform.Android)
            {
                Debug.Log("Checking Android version for permissions...");

                // Get Android version
                int sdkInt = GetAndroidSDKVersion();
                Debug.Log($"Android SDK version: {sdkInt}");

                // For Android 12+ (API 31+), request Bluetooth permissions
                if (sdkInt >= 31)
                {
                    yield return _manager.StartCoroutine(BleHidPermissionHandler.RequestBluetoothPermissions());

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
                }
                
                // For Android 13+ (API 33+), request notification permissions
                if (sdkInt >= 33)
                {
                    yield return _manager.StartCoroutine(BleHidPermissionHandler.RequestNotificationPermission());
                    
                    // Check if notification permission was granted
                    if (!BleHidPermissionHandler.CheckNotificationPermission())
                    {
                        // Just log a warning but don't fail initialization - notifications aren't critical
                        string message = "Notification permission not granted";
                        Debug.LogWarning(message);
                        _manager.BleEventSystem.OnDebugLog?.Invoke(message);
                        // We don't break here since notification permissions aren't critical for functionality
                    }
                    else
                    {
                        Debug.Log("Notification permission granted");
                    }
                }
            }

            // Create bridge and initialize
            bool initResult = false;

            try
            {
                // Only try the new namespace - no fallback
                Debug.Log("Connecting to com.inventonater.blehid.unity namespace...");
                AndroidJavaClass bridgeClass = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityBridge");
                BridgeInstance = bridgeClass.CallStatic<AndroidJavaObject>("getInstance");
                Debug.Log("Successfully connected to com.inventonater.blehid.unity.BleHidUnityBridge");

                // Verify the bridge interface
                if (!BleHidEnvironmentChecker.VerifyBridgeInterface(BridgeInstance, out errorMsg)) { throw new Exception(errorMsg); }

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
            }

            isInitializing = false;
        }

        /// <summary>
        /// Get the Android SDK version
        /// </summary>
        /// <returns>SDK version number, or -1 if not on Android</returns>
        public static int GetAndroidSDKVersion()
        {
            if (Application.platform != RuntimePlatform.Android) return -1;

            try
            {
                AndroidJavaClass versionClass = new AndroidJavaClass("android.os.Build$VERSION");
                return versionClass.GetStatic<int>("SDK_INT");
            }
            catch (Exception e)
            {
                Debug.LogError("Failed to get Android SDK version: " + e.Message);
                return -1;
            }
        }

        /// <summary>
        /// Run diagnostic checks and return a comprehensive report of the system state.
        /// </summary>
        /// <returns>A string containing the diagnostic information.</returns>
        public string RunEnvironmentDiagnostics()
        {
            return BleHidEnvironmentChecker.RunEnvironmentDiagnostics(_manager);
        }

        /// <summary>
        /// Get diagnostic information from the plugin.
        /// </summary>
        /// <returns>A string with diagnostic information.</returns>
        public string GetDiagnosticInfo()
        {
            if (!_manager.ConfirmIsInitialized()) return "Not initialized";

            try { return BridgeInstance.Call<string>("getDiagnosticInfo"); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return "Error getting diagnostic info: " + e.Message;
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
