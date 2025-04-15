using System;
using System.Collections;
using JetBrains.Annotations;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Main Unity interface for BLE HID functionality.
    /// This class provides methods to control Bluetooth HID emulation for keyboard, mouse, and media control.
    /// </summary>
    public class BleHidManager : MonoBehaviour
    {
        #region Properties
        /// <summary>
        /// Whether the BLE HID manager is initialized.
        /// </summary>
        public bool IsInitialized { get; internal set; }
        
        /// <summary>
        /// Whether BLE advertising is currently active.
        /// </summary>
        public bool IsAdvertising { get; internal set; }
        
        /// <summary>
        /// Whether a device is connected.
        /// </summary>
        public bool IsConnected { get; internal set; }
        
        /// <summary>
        /// Name of the connected device, if any.
        /// </summary>
        public string ConnectedDeviceName { get; internal set; }
        
        /// <summary>
        /// Address of the connected device, if any.
        /// </summary>
        public string ConnectedDeviceAddress { get; internal set; }
        
        /// <summary>
        /// Last error message.
        /// </summary>
        public string LastErrorMessage { get; internal set; }
        
        /// <summary>
        /// Last error code.
        /// </summary>
        public int LastErrorCode { get; internal set; }
        
        // Connection parameters
        /// <summary>
        /// Current connection interval in milliseconds.
        /// </summary>
        public int ConnectionInterval { get; internal set; }
        
        /// <summary>
        /// Current slave latency (number of connection events that can be skipped).
        /// </summary>
        public int SlaveLatency { get; internal set; }
        
        /// <summary>
        /// Current supervision timeout in milliseconds.
        /// </summary>
        public int SupervisionTimeout { get; internal set; }
        
        /// <summary>
        /// Current MTU size in bytes.
        /// </summary>
        public int MtuSize { get; internal set; }
        
        /// <summary>
        /// Current RSSI value in dBm.
        /// </summary>
        public int Rssi { get; internal set; }
        
        /// <summary>
        /// Current transmit power level (0=Low, 1=Medium, 2=High).
        /// </summary>
        public int TxPowerLevel { get; internal set; }
        
        /// <summary>
        /// The bridge instance used to communicate with Java.
        /// </summary>
        internal AndroidJavaObject BridgeInstance => bridgeInstance;
        #endregion
        
        #region Singleton
        /// <summary>
        /// Singleton instance of the BleHidManager.
        /// </summary>
        public static BleHidManager Instance { get; private set; }
        
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
            
            // Forward events
            callbackHandler.OnInitializeComplete += (success, message) => OnInitializeComplete?.Invoke(success, message);
            callbackHandler.OnAdvertisingStateChanged += (advertising, message) => OnAdvertisingStateChanged?.Invoke(advertising, message);
            callbackHandler.OnConnectionStateChanged += (connected, deviceName, deviceAddress) => OnConnectionStateChanged?.Invoke(connected, deviceName, deviceAddress);
            callbackHandler.OnPairingStateChanged += (status, deviceAddress) => OnPairingStateChanged?.Invoke(status, deviceAddress);
            callbackHandler.OnConnectionParametersChanged += (interval, latency, timeout, mtu) => {
                ConnectionInterval = interval;
                SlaveLatency = latency;
                SupervisionTimeout = timeout;
                MtuSize = mtu;
                OnConnectionParametersChanged?.Invoke(interval, latency, timeout, mtu);
            };
            callbackHandler.OnRssiRead += (rssi) => {
                Rssi = rssi;
                OnRssiRead?.Invoke(rssi);
            };
            callbackHandler.OnConnectionParameterRequestComplete += (paramName, success, actualValue) => 
                OnConnectionParameterRequestComplete?.Invoke(paramName, success, actualValue);
            callbackHandler.OnError += (errorCode, errorMessage) => OnError?.Invoke(errorCode, errorMessage);
            callbackHandler.OnDebugLog += (message) => OnDebugLog?.Invoke(message);
            
            Debug.Log("BleHidManager initialized");
        }
        #endregion
        
        #region Events
        // Event declarations that will be forwarded from the callback handler
        public event BleHidCallbackHandler.InitializeCompleteHandler OnInitializeComplete;
        public event BleHidCallbackHandler.AdvertisingStateChangedHandler OnAdvertisingStateChanged;
        public event BleHidCallbackHandler.ConnectionStateChangedHandler OnConnectionStateChanged;
        public event BleHidCallbackHandler.PairingStateChangedHandler OnPairingStateChanged;
        public event BleHidCallbackHandler.ConnectionParametersChangedHandler OnConnectionParametersChanged;
        public event BleHidCallbackHandler.RssiReadHandler OnRssiRead;
        public event BleHidCallbackHandler.ConnectionParameterRequestCompleteHandler OnConnectionParameterRequestComplete;
        public event BleHidCallbackHandler.ErrorHandler OnError;
        public event BleHidCallbackHandler.DebugLogHandler OnDebugLog;
        #endregion
        
        #region Private Fields
        // Fields to track state
        private bool isInitializing = false;
        private AndroidJavaObject bridgeInstance = null;
        private BleHidCallbackHandler callbackHandler;
        #endregion
        
        #region Unity Lifecycle
        private void OnDestroy()
        {
            Close();
        }
        #endregion
        
        #region Public Methods
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
                if (!BleHidEnvironmentChecker.VerifyBridgeInterface(bridgeInstance, out errorMsg))
                {
                    throw new Exception(errorMsg);
                }
                
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
                
                while (!IsInitialized && (Time.time - startTime) < timeout)
                {
                    yield return null;
                }
                
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
        /// Run diagnostic checks and return a comprehensive report of the system state.
        /// </summary>
        /// <returns>A string containing the diagnostic information.</returns>
        public string RunEnvironmentDiagnostics()
        {
            return BleHidEnvironmentChecker.RunEnvironmentDiagnostics(this);
        }
        
        /// <summary>
        /// Start BLE advertising to make this device discoverable.
        /// </summary>
        /// <returns>True if advertising was started successfully, false otherwise.</returns>
        public bool StartAdvertising()
        {
            if (!CheckInitialized()) return false;
            
            try
            {
                Debug.Log("BleHidManager: Attempting to start advertising...");
                
                // Verify Bluetooth is enabled
                string errorMsg;
                if (!BleHidEnvironmentChecker.IsBluetoothEnabled(out errorMsg))
                {
                    Debug.LogError("BleHidManager: " + errorMsg);
                    OnError?.Invoke(BleHidConstants.ERROR_BLUETOOTH_DISABLED, errorMsg);
                    return false;
                }
                
                // Verify device supports advertising
                if (!BleHidEnvironmentChecker.SupportsBleAdvertising(out errorMsg))
                {
                    Debug.LogError("BleHidManager: " + errorMsg);
                    OnError?.Invoke(BleHidConstants.ERROR_PERIPHERAL_NOT_SUPPORTED, errorMsg);
                    return false;
                }
                
                // Add extra debug info
                try {
                    // Use a simple toString() to get some information about the bridge instance
                    string instanceInfo = bridgeInstance.Call<string>("toString");
                    Debug.Log("BleHidManager: Using bridgeInstance: " + instanceInfo);
                } catch (Exception debugEx) {
                    Debug.LogWarning("BleHidManager: Could not get bridge instance info: " + debugEx.Message);
                }
                
                bool result = bridgeInstance.Call<bool>("startAdvertising");
                Debug.Log("BleHidManager: StartAdvertising call result: " + result);
                
                // Verify advertising state
                try {
                    bool isAdvertising = bridgeInstance.Call<bool>("isAdvertising");
                    Debug.Log("BleHidManager: isAdvertising check after call: " + isAdvertising);
                } catch (Exception verifyEx) {
                    Debug.LogWarning("BleHidManager: Could not verify advertising state: " + verifyEx.Message);
                }
                
                return result;
            }
            catch (Exception e)
            {
                string message = "Exception starting advertising: " + e.Message;
                Debug.LogError(message);
                Debug.LogException(e);
                LastErrorMessage = message;
                LastErrorCode = BleHidConstants.ERROR_ADVERTISING_FAILED;
                OnError?.Invoke(BleHidConstants.ERROR_ADVERTISING_FAILED, message);
                return false;
            }
        }
        
        /// <summary>
        /// Stop BLE advertising.
        /// </summary>
        public void StopAdvertising()
        {
            if (!CheckInitialized()) return;
            
            try
            {
                bridgeInstance.Call("stopAdvertising");
            }
            catch (Exception e)
            {
                string message = "Exception stopping advertising: " + e.Message;
                Debug.LogException(e);
                OnError?.Invoke(BleHidConstants.ERROR_ADVERTISING_FAILED, message);
            }
        }
        
        /// <summary>
        /// Get current advertising state.
        /// </summary>
        /// <returns>True if advertising is active, false otherwise.</returns>
        public bool GetAdvertisingState()
        {
            if (!CheckInitialized()) return false;
            
            try
            {
                return bridgeInstance.Call<bool>("isAdvertising");
            }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
        
        /// <summary>
        /// Send a keyboard key press and release.
        /// </summary>
        /// <param name="keyCode">HID key code (see BleHidConstants)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool SendKey(byte keyCode)
        {
            if (!CheckConnected()) return false;
            
            try
            {
                return bridgeInstance.Call<bool>("sendKey", (int)keyCode);
            }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
        
        /// <summary>
        /// Send a keyboard key with modifier keys.
        /// </summary>
        /// <param name="keyCode">HID key code (see BleHidConstants)</param>
        /// <param name="modifiers">Modifier key bit flags (see BleHidConstants)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool SendKeyWithModifiers(byte keyCode, byte modifiers)
        {
            if (!CheckConnected()) return false;
            
            try
            {
                return bridgeInstance.Call<bool>("sendKeyWithModifiers", (int)keyCode, (int)modifiers);
            }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
        
        /// <summary>
        /// Type a string of text.
        /// </summary>
        /// <param name="text">The text to type</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool TypeText(string text)
        {
            if (!CheckConnected()) return false;
            
            try
            {
                return bridgeInstance.Call<bool>("typeText", text);
            }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
        
        /// <summary>
        /// Send a mouse movement.
        /// </summary>
        /// <param name="deltaX">X-axis movement (-127 to 127)</param>
        /// <param name="deltaY">Y-axis movement (-127 to 127)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool MoveMouse(int deltaX, int deltaY)
        {
            if (!CheckConnected()) return false;
            
            try
            {
                return bridgeInstance.Call<bool>("moveMouse", deltaX, deltaY);
            }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
        
        /// <summary>
        /// Click a mouse button.
        /// </summary>
        /// <param name="button">Button to click (0=left, 1=right, 2=middle)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool ClickMouseButton(int button)
        {
            if (!CheckConnected()) return false;
            
            try
            {
                return bridgeInstance.Call<bool>("clickMouseButton", button);
            }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
        
        /// <summary>
        /// Send media play/pause command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool PlayPause()
        {
            if (!CheckConnected()) return false;
            
            try
            {
                return bridgeInstance.Call<bool>("playPause");
            }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
        
        /// <summary>
        /// Send media next track command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool NextTrack()
        {
            if (!CheckConnected()) return false;
            
            try
            {
                return bridgeInstance.Call<bool>("nextTrack");
            }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
        
        /// <summary>
        /// Send media previous track command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool PreviousTrack()
        {
            if (!CheckConnected()) return false;
            
            try
            {
                return bridgeInstance.Call<bool>("previousTrack");
            }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
        
        /// <summary>
        /// Send media volume up command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool VolumeUp()
        {
            if (!CheckConnected()) return false;
            
            try
            {
                return bridgeInstance.Call<bool>("volumeUp");
            }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
        
        /// <summary>
        /// Send media volume down command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool VolumeDown()
        {
            if (!CheckConnected()) return false;
            
            try
            {
                return bridgeInstance.Call<bool>("volumeDown");
            }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
        
        /// <summary>
        /// Send media mute command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool Mute()
        {
            if (!CheckConnected()) return false;
            
            try
            {
                return bridgeInstance.Call<bool>("mute");
            }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
        
        #region Connection Parameter Methods
        
        /// <summary>
        /// Request a change in connection priority.
        /// Connection priority affects latency and power consumption.
        /// </summary>
        /// <param name="priority">The priority to request (0=HIGH, 1=BALANCED, 2=LOW_POWER)</param>
        /// <returns>True if the request was sent, false otherwise.</returns>
        public bool RequestConnectionPriority(int priority)
        {
            if (!CheckConnected()) return false;
            
            try
            {
                return bridgeInstance.Call<bool>("requestConnectionPriority", priority);
            }
            catch (Exception e)
            {
                string message = "Exception requesting connection priority: " + e.Message;
                Debug.LogException(e);
                LastErrorMessage = message;
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
            if (!CheckConnected()) return false;
            
            if (mtu < 23 || mtu > 517)
            {
                string message = "Invalid MTU size: " + mtu + ". Must be between 23 and 517.";
                Debug.LogError(message);
                LastErrorMessage = message;
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
                LastErrorMessage = message;
                OnError?.Invoke(BleHidConstants.ERROR_NOT_CONNECTED, message);
                return false;
            }
        }
        
        /// <summary>
        /// Sets the transmit power level for advertising.
        /// Higher power increases range but consumes more battery.
        /// </summary>
        /// <param name="level">The power level (0=LOW, 1=MEDIUM, 2=HIGH)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool SetTransmitPowerLevel(int level)
        {
            if (!CheckInitialized()) return false;
            
            if (level < 0 || level > 2)
            {
                string message = "Invalid TX power level: " + level + ". Must be between 0 and 2.";
                Debug.LogError(message);
                LastErrorMessage = message;
                OnError?.Invoke(BleHidConstants.ERROR_INVALID_PARAMETER, message);
                return false;
            }
            
            try
            {
                return bridgeInstance.Call<bool>("setTransmitPowerLevel", level);
            }
            catch (Exception e)
            {
                string message = "Exception setting TX power level: " + e.Message;
                Debug.LogException(e);
                LastErrorMessage = message;
                OnError?.Invoke(BleHidConstants.ERROR_INVALID_PARAMETER, message);
                return false;
            }
        }
        
        /// <summary>
        /// Reads the current RSSI (signal strength) value.
        /// </summary>
        /// <returns>True if the read request was sent, false otherwise.</returns>
        public bool ReadRssi()
        {
            if (!CheckConnected()) return false;
            
            try
            {
                return bridgeInstance.Call<bool>("readRssi");
            }
            catch (Exception e)
            {
                string message = "Exception reading RSSI: " + e.Message;
                Debug.LogException(e);
                LastErrorMessage = message;
                OnError?.Invoke(BleHidConstants.ERROR_NOT_CONNECTED, message);
                return false;
            }
        }
        
        /// <summary>
        /// Gets all connection parameters as a dictionary.
        /// </summary>
        /// <returns>Dictionary of parameter names to values, or null if not connected.</returns>
        public System.Collections.Generic.Dictionary<string, string> GetConnectionParameters()
        {
            if (!CheckConnected()) return null;
            
            try
            {
                AndroidJavaObject parametersMap = bridgeInstance.Call<AndroidJavaObject>("getConnectionParameters");
                if (parametersMap == null)
                {
                    return null;
                }
                
                System.Collections.Generic.Dictionary<string, string> result = new System.Collections.Generic.Dictionary<string, string>();
                
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
                LastErrorMessage = message;
                return null;
            }
        }
        
        #endregion
        
        /// <summary>
        /// Get diagnostic information from the plugin.
        /// </summary>
        /// <returns>A string with diagnostic information.</returns>
        public string GetDiagnosticInfo()
        {
            if (!CheckInitialized()) return "Not initialized";
            
            try
            {
                return bridgeInstance.Call<string>("getDiagnosticInfo");
            }
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
                try
                {
                    bridgeInstance.Call("close");
                }
                catch (Exception e)
                {
                    Debug.LogException(e);
                }
                
                bridgeInstance.Dispose();
                bridgeInstance = null;
            }
            
            IsInitialized = false;
            IsAdvertising = false;
            IsConnected = false;
            ConnectedDeviceName = null;
            ConnectedDeviceAddress = null;
            
            Debug.Log("BleHidManager closed");
        }
        #endregion
        
        #region Callback Methods (Called from Java)
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
        #endregion
        
        #region Helper Methods
        private bool CheckInitialized()
        {
            if (!IsInitialized || bridgeInstance == null)
            {
                string message = "BLE HID plugin not initialized";
                Debug.LogError(message);
                OnError?.Invoke(BleHidConstants.ERROR_NOT_INITIALIZED, message);
                return false;
            }
            return true;
        }
        
        private bool CheckConnected()
        {
            if (!CheckInitialized()) return false;
            
            if (!IsConnected)
            {
                string message = "No BLE device connected";
                Debug.LogError(message);
                OnError?.Invoke(BleHidConstants.ERROR_NOT_CONNECTED, message);
                return false;
            }
            return true;
        }
        #endregion
    }
}
