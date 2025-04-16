using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Main Unity interface for BLE HID functionality.
    /// This class provides methods to control Bluetooth HID emulation for keyboard, mouse, and media control.
    /// It acts as a facade for the specialized manager components that handle different aspects of the system.
    /// </summary>
    public class BleHidManager : MonoBehaviour
    {
        // Public properties - these delegate to the appropriate managers
        public bool IsInitialized { get; internal set; }
        public string LastErrorMessage { get; internal set; }
        public int LastErrorCode { get; internal set; }
        internal AndroidJavaObject BridgeInstance => bridgeInstance;
        
        // Singleton pattern 
        public static BleHidManager Instance { get; private set; }
        
        // Component managers
        private BleHidConnectionManager connectionManager;
        private BleHidAdvertisingManager advertisingManager;
        private BleHidInputManager inputManager;
        private BleHidMediaManager mediaManager;
        private BleHidForegroundServiceManager foregroundServiceManager;
        
        // Connection state properties - delegate to connection manager
        public bool IsConnected => connectionManager?.IsConnected ?? false;
        public string ConnectedDeviceName => connectionManager?.ConnectedDeviceName;
        public string ConnectedDeviceAddress => connectionManager?.ConnectedDeviceAddress;
        public int ConnectionInterval => connectionManager?.ConnectionInterval ?? 0;
        public int SlaveLatency => connectionManager?.SlaveLatency ?? 0;
        public int SupervisionTimeout => connectionManager?.SupervisionTimeout ?? 0;
        public int MtuSize => connectionManager?.MtuSize ?? 0;
        public int Rssi => connectionManager?.Rssi ?? 0;
        
        // Advertising state properties - delegate to advertising manager
        public bool IsAdvertising => advertisingManager?.IsAdvertising ?? false;
        public int TxPowerLevel => advertisingManager?.TxPowerLevel ?? 0;
        
        // Input properties
        public MouseInputProcessor MouseInputProcessor => inputManager?.MouseInputProcessor;

        // Event declarations that will be forwarded to/from the appropriate manager
        public event BleHidCallbackHandler.InitializeCompleteHandler OnInitializeComplete;
        public event BleHidCallbackHandler.AdvertisingStateChangedHandler OnAdvertisingStateChanged;
        public event BleHidCallbackHandler.ConnectionStateChangedHandler OnConnectionStateChanged;
        public event BleHidCallbackHandler.PairingStateChangedHandler OnPairingStateChanged;
        public event BleHidCallbackHandler.ConnectionParametersChangedHandler OnConnectionParametersChanged;
        public event BleHidCallbackHandler.RssiReadHandler OnRssiRead;
        public event BleHidCallbackHandler.ConnectionParameterRequestCompleteHandler OnConnectionParameterRequestComplete;
        public event BleHidCallbackHandler.ErrorHandler OnError;
        public event BleHidCallbackHandler.DebugLogHandler OnDebugLog;

        // Fields to track state
        private bool isInitializing = false;
        private AndroidJavaObject bridgeInstance = null;
        private BleHidCallbackHandler callbackHandler;

        private void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }

            Instance = this;
            DontDestroyOnLoad(gameObject);

            // Create the callback handler
            callbackHandler = new BleHidCallbackHandler(this);

            // Forward events from callback handler
            SetupCallbackHandlers();

            Debug.Log("BleHidManager initialized");
        }

        private void SetupCallbackHandlers()
        {
            // Setup main callback handlers that forward to the specialized components
            callbackHandler.OnInitializeComplete += (success, message) => OnInitializeComplete?.Invoke(success, message);
            
            callbackHandler.OnAdvertisingStateChanged += (advertising, message) => {
                if (advertisingManager != null) {
                    advertisingManager.UpdateAdvertisingState(advertising, message);
                }
                OnAdvertisingStateChanged?.Invoke(advertising, message);
            };
            
            callbackHandler.OnConnectionStateChanged += (connected, deviceName, deviceAddress) => {
                if (connectionManager != null) {
                    connectionManager.UpdateConnectionState(connected, deviceName, deviceAddress);
                }
                OnConnectionStateChanged?.Invoke(connected, deviceName, deviceAddress);
            };
            
            callbackHandler.OnPairingStateChanged += (status, deviceAddress) => {
                if (connectionManager != null) {
                    connectionManager.UpdatePairingState(status, deviceAddress);
                }
                OnPairingStateChanged?.Invoke(status, deviceAddress);
            };
            
            callbackHandler.OnConnectionParametersChanged += (interval, latency, timeout, mtu) => {
                if (connectionManager != null) {
                    connectionManager.UpdateConnectionParameters(interval, latency, timeout, mtu);
                }
                OnConnectionParametersChanged?.Invoke(interval, latency, timeout, mtu);
            };
            
            callbackHandler.OnRssiRead += (rssi) => {
                if (connectionManager != null) {
                    connectionManager.UpdateRssi(rssi);
                }
                OnRssiRead?.Invoke(rssi);
            };
            
            callbackHandler.OnConnectionParameterRequestComplete += (paramName, success, actualValue) => {
                if (connectionManager != null) {
                    connectionManager.UpdateConnectionParameterRequestComplete(paramName, success, actualValue);
                }
                OnConnectionParameterRequestComplete?.Invoke(paramName, success, actualValue);
            };
            
            callbackHandler.OnError += (errorCode, errorMessage) => {
                LastErrorCode = errorCode;
                LastErrorMessage = errorMessage;
                OnError?.Invoke(errorCode, errorMessage);
            };
            
            callbackHandler.OnDebugLog += (message) => OnDebugLog?.Invoke(message);
        }

        private void OnDestroy()
        {
            Close();
        }

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

            if (IsInitialized)
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
                OnError?.Invoke(BleHidConstants.ERROR_PERIPHERAL_NOT_SUPPORTED, message);
                OnInitializeComplete?.Invoke(false, message);
                isInitializing = false;
                yield break;
            }

            // Check if plugins are loaded
            string errorMsg;
            if (!BleHidEnvironmentChecker.VerifyPluginsLoaded(out errorMsg))
            {
                Debug.LogError(errorMsg);
                OnError?.Invoke(BleHidConstants.ERROR_INITIALIZATION_FAILED, errorMsg);
                OnInitializeComplete?.Invoke(false, errorMsg);
                isInitializing = false;
                yield break;
            }

            // Request runtime permissions for Android 12+ (API level 31+)
            if (Application.platform == RuntimePlatform.Android)
            {
                Debug.Log("Checking Android version for permissions...");

                // Get Android version
                int sdkInt = BleHidPermissionHandler.GetAndroidSDKVersion();
                Debug.Log($"Android SDK version: {sdkInt}");

                // For Android 12+ (API 31+)
                if (sdkInt >= 31)
                {
                    yield return StartCoroutine(BleHidPermissionHandler.RequestBluetoothPermissions());

                    // Check if required permissions were granted
                    if (!BleHidPermissionHandler.CheckBluetoothPermissions())
                    {
                        string message = "Bluetooth permissions not granted";
                        Debug.LogError(message);
                        OnError?.Invoke(BleHidConstants.ERROR_PERMISSIONS_NOT_GRANTED, message);
                        OnInitializeComplete?.Invoke(false, message);
                        isInitializing = false;
                        yield break;
                    }

                    Debug.Log("Bluetooth permissions granted");
                }
            }

            // Create bridge and initialize
            bool initResult = false;

            try
            {
                // Only try the new namespace - no fallback
                Debug.Log("Connecting to com.inventonater.blehid.unity namespace...");
                AndroidJavaClass bridgeClass = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityBridge");
                bridgeInstance = bridgeClass.CallStatic<AndroidJavaObject>("getInstance");
                Debug.Log("Successfully connected to com.inventonater.blehid.unity.BleHidUnityBridge");

                // Verify the bridge interface
                if (!BleHidEnvironmentChecker.VerifyBridgeInterface(bridgeInstance, out errorMsg)) { throw new Exception(errorMsg); }

                // Initialize the bridge with this GameObject's name for callbacks
                initResult = bridgeInstance.Call<bool>("initialize", gameObject.name);

                if (!initResult)
                {
                    string message = "Failed to initialize BLE HID plugin";
                    Debug.LogError(message);
                    OnError?.Invoke(BleHidConstants.ERROR_INITIALIZATION_FAILED, message);
                    OnInitializeComplete?.Invoke(false, message);
                    isInitializing = false;
                    yield break;
                }
                
                // Initialize the specialized managers
                InitializeManagers();
            }
            catch (Exception e)
            {
                string message = "Exception during initialization: " + e.Message;
                Debug.LogException(e);
                OnError?.Invoke(BleHidConstants.ERROR_INITIALIZATION_FAILED, message);
                OnInitializeComplete?.Invoke(false, message);
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

                while (!IsInitialized && (Time.time - startTime) < timeout) { yield return null; }

                if (!IsInitialized)
                {
                    string message = "BLE HID initialization timed out";
                    Debug.LogError(message);
                    OnError?.Invoke(BleHidConstants.ERROR_INITIALIZATION_FAILED, message);
                    OnInitializeComplete?.Invoke(false, message);
                }
            }

            isInitializing = false;
        }
        
        /// <summary>
        /// Initialize the specialized manager components
        /// </summary>
        private void InitializeManagers()
        {
            if (bridgeInstance == null)
            {
                Debug.LogError("Cannot initialize managers: bridge instance is null");
                return;
            }
            
            // Create the managers
            connectionManager = new BleHidConnectionManager(bridgeInstance);
            advertisingManager = new BleHidAdvertisingManager(bridgeInstance);
            
            // Connection manager must be created first as other managers depend on it
            inputManager = new BleHidInputManager(bridgeInstance, connectionManager);
            mediaManager = new BleHidMediaManager(bridgeInstance, connectionManager);
            foregroundServiceManager = new BleHidForegroundServiceManager(bridgeInstance, this);
            
            // Forward error events from managers to the main error handler
            connectionManager.OnError += (code, message) => OnError?.Invoke(code, message);
            advertisingManager.OnError += (code, message) => OnError?.Invoke(code, message);
            inputManager.OnError += (code, message) => OnError?.Invoke(code, message);
            mediaManager.OnError += (code, message) => OnError?.Invoke(code, message);
            foregroundServiceManager.OnError += (code, message) => OnError?.Invoke(code, message);
        }

        /// <summary>
        /// Run diagnostic checks and return a comprehensive report of the system state.
        /// </summary>
        /// <returns>A string containing the diagnostic information.</returns>
        public string RunEnvironmentDiagnostics()
        {
            return BleHidEnvironmentChecker.RunEnvironmentDiagnostics(this);
        }
        
        // --- Advertising methods forwarded to AdvertisingManager ---

        /// <summary>
        /// Start BLE advertising to make this device discoverable.
        /// </summary>
        /// <returns>True if advertising was started successfully, false otherwise.</returns>
        public bool StartAdvertising()
        {
            if (!ConfirmIsInitialized()) return false;
            return advertisingManager.StartAdvertising();
        }

        /// <summary>
        /// Stop BLE advertising.
        /// </summary>
        public void StopAdvertising()
        {
            if (!ConfirmIsInitialized()) return;
            advertisingManager.StopAdvertising();
        }

        /// <summary>
        /// Get current advertising state.
        /// </summary>
        /// <returns>True if advertising is active, false otherwise.</returns>
        public bool GetAdvertisingState()
        {
            if (!ConfirmIsInitialized()) return false;
            return advertisingManager.GetAdvertisingState();
        }
        
        /// <summary>
        /// Sets the transmit power level for advertising.
        /// Higher power increases range but consumes more battery.
        /// </summary>
        /// <param name="level">The power level (0=LOW, 1=MEDIUM, 2=HIGH)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool SetTransmitPowerLevel(int level)
        {
            if (!ConfirmIsInitialized()) return false;
            return advertisingManager.SetTransmitPowerLevel(level);
        }
        
        // --- Keyboard input methods forwarded to InputManager ---

        /// <summary>
        /// Send a keyboard key press and release.
        /// </summary>
        /// <param name="keyCode">HID key code (see BleHidConstants)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool SendKey(byte keyCode)
        {
            if (!ConfirmIsConnected()) return false;
            return inputManager.SendKey(keyCode);
        }

        /// <summary>
        /// Send a keyboard key with modifier keys.
        /// </summary>
        /// <param name="keyCode">HID key code (see BleHidConstants)</param>
        /// <param name="modifiers">Modifier key bit flags (see BleHidConstants)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool SendKeyWithModifiers(byte keyCode, byte modifiers)
        {
            if (!ConfirmIsConnected()) return false;
            return inputManager.SendKeyWithModifiers(keyCode, modifiers);
        }

        /// <summary>
        /// Type a string of text.
        /// </summary>
        /// <param name="text">The text to type</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool TypeText(string text)
        {
            if (!ConfirmIsConnected()) return false;
            return inputManager.TypeText(text);
        }
        
        /// <summary>
        /// Click a mouse button.
        /// </summary>
        /// <param name="button">Button to click (0=left, 1=right, 2=middle)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool ClickMouseButton(int button)
        {
            if (!ConfirmIsConnected()) return false;
            return inputManager.ClickMouseButton(button);
        }
        
        // --- Media control methods forwarded to MediaManager ---

        /// <summary>
        /// Send media play/pause command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool PlayPause()
        {
            if (!ConfirmIsConnected()) return false;
            return mediaManager.PlayPause();
        }

        /// <summary>
        /// Send media next track command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool NextTrack()
        {
            if (!ConfirmIsConnected()) return false;
            return mediaManager.NextTrack();
        }

        /// <summary>
        /// Send media previous track command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool PreviousTrack()
        {
            if (!ConfirmIsConnected()) return false;
            return mediaManager.PreviousTrack();
        }

        /// <summary>
        /// Send media volume up command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool VolumeUp()
        {
            if (!ConfirmIsConnected()) return false;
            return mediaManager.VolumeUp();
        }

        /// <summary>
        /// Send media volume down command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool VolumeDown()
        {
            if (!ConfirmIsConnected()) return false;
            return mediaManager.VolumeDown();
        }

        /// <summary>
        /// Send media mute command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool Mute()
        {
            if (!ConfirmIsConnected()) return false;
            return mediaManager.Mute();
        }
        
        // --- Connection methods forwarded to ConnectionManager ---

        /// <summary>
        /// Request a change in connection priority.
        /// Connection priority affects latency and power consumption.
        /// </summary>
        /// <param name="priority">The priority to request (0=HIGH, 1=BALANCED, 2=LOW_POWER)</param>
        /// <returns>True if the request was sent, false otherwise.</returns>
        public bool RequestConnectionPriority(int priority)
        {
            if (!ConfirmIsConnected()) return false;
            return connectionManager.RequestConnectionPriority(priority);
        }

        /// <summary>
        /// Request a change in MTU (Maximum Transmission Unit) size.
        /// Larger MTU sizes can improve throughput.
        /// </summary>
        /// <param name="mtu">The MTU size to request (23-517 bytes)</param>
        /// <returns>True if the request was sent, false otherwise.</returns>
        public bool RequestMtu(int mtu)
        {
            if (!ConfirmIsConnected()) return false;
            return connectionManager.RequestMtu(mtu);
        }

        /// <summary>
        /// Reads the current RSSI (signal strength) value.
        /// </summary>
        /// <returns>True if the read request was sent, false otherwise.</returns>
        public bool ReadRssi()
        {
            if (!ConfirmIsConnected()) return false;
            return connectionManager.ReadRssi();
        }

        /// <summary>
        /// Gets all connection parameters as a dictionary.
        /// </summary>
        /// <returns>Dictionary of parameter names to values, or null if not connected.</returns>
        public Dictionary<string, string> GetConnectionParameters()
        {
            if (!ConfirmIsConnected()) return null;
            return connectionManager.GetConnectionParameters();
        }
        
        // --- Foreground service methods forwarded to ForegroundServiceManager ---

        /// <summary>
        /// Start the foreground service to keep accessibility service alive.
        /// </summary>
        /// <returns>True if the service start request was sent successfully.</returns>
        public bool StartForegroundService()
        {
            return foregroundServiceManager.StartForegroundService();
        }

        /// <summary>
        /// Stop the foreground service when it's no longer needed.
        /// </summary>
        /// <returns>True if the service stop request was sent successfully.</returns>
        public bool StopForegroundService()
        {
            return foregroundServiceManager.StopForegroundService();
        }

        /// <summary>
        /// Get diagnostic information from the plugin.
        /// </summary>
        /// <returns>A string with diagnostic information.</returns>
        public string GetDiagnosticInfo()
        {
            if (!ConfirmIsInitialized()) return "Not initialized";

            try { return bridgeInstance.Call<string>("getDiagnosticInfo"); }
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
            if (bridgeInstance != null)
            {
                try { bridgeInstance.Call("close"); }
                catch (Exception e) { Debug.LogException(e); }

                bridgeInstance.Dispose();
                bridgeInstance = null;
            }

            IsInitialized = false;

            Debug.Log("BleHidManager closed");
        }

        // --- Callback handling methods ---

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
        
        // --- Validation helper methods ---

        /// <summary>
        /// Confirm that the plugin is initialized before attempting operations
        /// </summary>
        private bool ConfirmIsInitialized()
        {
            if (IsInitialized && bridgeInstance != null) return true;

            string message = "BLE HID plugin not initialized";
            Debug.LogError(message);
            OnError?.Invoke(BleHidConstants.ERROR_NOT_INITIALIZED, message);
            return false;
        }

        /// <summary>
        /// Confirm that a device is connected before attempting operations that require a connection
        /// </summary>
        private bool ConfirmIsConnected()
        {
            if (!ConfirmIsInitialized()) return false;
            if (connectionManager.IsConnected) return true;

            string message = "No BLE device connected";
            Debug.LogError(message);
            OnError?.Invoke(BleHidConstants.ERROR_NOT_CONNECTED, message);
            return false;
        }
    }
}
