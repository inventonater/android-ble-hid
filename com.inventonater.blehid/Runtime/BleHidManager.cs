using System;
using System.Collections;
using System.Runtime.InteropServices;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Main Unity interface for BLE HID functionality.
    /// This class provides methods to control Bluetooth HID emulation for keyboard, mouse, and media control.
    /// </summary>
    public class BleHidManager : MonoBehaviour
    {
        #region Events
        // Define delegate types for events
        public delegate void InitializeCompleteHandler(bool success, string message);
        public delegate void AdvertisingStateChangedHandler(bool advertising, string message);
        public delegate void ConnectionStateChangedHandler(bool connected, string deviceName, string deviceAddress);
        public delegate void PairingStateChangedHandler(string status, string deviceAddress);
        public delegate void ErrorHandler(int errorCode, string errorMessage);
        public delegate void DebugLogHandler(string message);
        
        // Event declarations
        public event InitializeCompleteHandler OnInitializeComplete;
        public event AdvertisingStateChangedHandler OnAdvertisingStateChanged;
        public event ConnectionStateChangedHandler OnConnectionStateChanged;
        public event PairingStateChangedHandler OnPairingStateChanged;
        public event ErrorHandler OnError;
        public event DebugLogHandler OnDebugLog;
        #endregion
        
        #region Properties
        /// <summary>
        /// Whether the BLE HID manager is initialized.
        /// </summary>
        public bool IsInitialized { get; private set; }
        
        /// <summary>
        /// Whether BLE advertising is currently active.
        /// </summary>
        public bool IsAdvertising { get; private set; }
        
        /// <summary>
        /// Whether a device is connected.
        /// </summary>
        public bool IsConnected { get; private set; }
        
        /// <summary>
        /// Name of the connected device, if any.
        /// </summary>
        public string ConnectedDeviceName { get; private set; }
        
        /// <summary>
        /// Address of the connected device, if any.
        /// </summary>
        public string ConnectedDeviceAddress { get; private set; }
        
        /// <summary>
        /// Last error message.
        /// </summary>
        public string LastErrorMessage { get; private set; }
        
        /// <summary>
        /// Last error code.
        /// </summary>
        public int LastErrorCode { get; private set; }
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
            
            Debug.Log("BleHidManager initialized");
        }
        #endregion
        
        #region Private Fields
        // Boolean fields to track state
        private bool isInitializing = false;
        private AndroidJavaObject bridgeInstance = null;
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
            if (Application.platform != RuntimePlatformSupportAndroid())
            {
                string message = "BLE HID is only supported on Android";
                Debug.LogWarning(message);
                OnError?.Invoke(BleHidConstants.ERROR_PERIPHERAL_NOT_SUPPORTED, message);
                OnInitializeComplete?.Invoke(false, message);
                isInitializing = false;
                yield break;
            }
            
            // Request runtime permissions for Android 12+ (API level 31+)
            if (Application.platform == RuntimePlatform.Android)
            {
                Debug.Log("Requesting Bluetooth permissions...");
                
                // Get Android version
                AndroidJavaClass versionClass = new AndroidJavaClass("android.os.Build$VERSION");
                int sdkInt = versionClass.GetStatic<int>("SDK_INT");
                Debug.Log($"Android SDK version: {sdkInt}");
                
                // For Android 12+ (API 31+)
                if (sdkInt >= 31)
                {
                    yield return StartCoroutine(RequestBluetoothPermissions());
                    
                    // Check if required permissions were granted
                    if (!CheckBluetoothPermissions())
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
                // Get the Unity activity
                AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
                AndroidJavaObject activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
                
                // Try to get the BleHidUnityBridge instance from the new namespace
                try
                {
                    Debug.Log("Attempting to use com.inventonater.blehid.unity namespace...");
                    AndroidJavaClass bridgeClass = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityBridge");
                    bridgeInstance = bridgeClass.CallStatic<AndroidJavaObject>("getInstance");
                    Debug.Log("Successfully connected to com.inventonater.blehid.unity.BleHidUnityBridge");
                }
                catch (Exception ex)
                {
                    // If that fails, try the old namespace as a fallback
                    Debug.LogWarning("Failed to connect to com.inventonater.blehid.unity namespace: " + ex.Message);
                    Debug.LogWarning("Attempting fallback to com.example.blehid.unity namespace...");
                    
                    try
                    {
                        AndroidJavaClass bridgeClass = new AndroidJavaClass("com.example.blehid.unity.BleHidUnityBridge");
                        bridgeInstance = bridgeClass.CallStatic<AndroidJavaObject>("getInstance");
                        Debug.Log("Successfully connected to com.example.blehid.unity.BleHidUnityBridge");
                    }
                    catch (Exception fallbackEx)
                    {
                        throw new Exception("Could not connect to either namespace (new or old): " + fallbackEx.Message);
                    }
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
        /// Start BLE advertising to make this device discoverable.
        /// </summary>
        /// <returns>True if advertising was started successfully, false otherwise.</returns>
        /// <summary>
        /// Run diagnostic checks and return a comprehensive report of the system state.
        /// </summary>
        /// <returns>A string containing the diagnostic information.</returns>
        public string RunEnvironmentDiagnostics()
        {
            System.Text.StringBuilder report = new System.Text.StringBuilder();
            
            report.AppendLine("===== BLE HID Environment Diagnostics =====");
            report.AppendLine("Date/Time: " + System.DateTime.Now.ToString());
            report.AppendLine("Platform: " + Application.platform);
            report.AppendLine("Unity Version: " + Application.unityVersion);
            
            if (Application.platform != RuntimePlatform.Android)
            {
                report.AppendLine("STATUS: UNSUPPORTED PLATFORM - Android required");
                return report.ToString();
            }
            
            // Android version check
            try
            {
                AndroidJavaClass versionClass = new AndroidJavaClass("android.os.Build$VERSION");
                int sdkInt = versionClass.GetStatic<int>("SDK_INT");
                string release = versionClass.GetStatic<string>("RELEASE");
                report.AppendLine($"Android Version: {release} (API {sdkInt})");
            }
            catch (Exception e)
            {
                report.AppendLine("Failed to get Android version: " + e.Message);
            }
            
            // Plugin load check
            string errorMsg;
            bool pluginsLoaded = VerifyPluginsLoaded(out errorMsg);
            report.AppendLine("Plugins Loaded: " + (pluginsLoaded ? "YES" : "NO"));
            if (!pluginsLoaded)
            {
                report.AppendLine("Plugin Error: " + errorMsg);
            }
            
            // Bluetooth enabled check
            bool bluetoothEnabled = IsBluetoothEnabled(out errorMsg);
            report.AppendLine("Bluetooth Enabled: " + (bluetoothEnabled ? "YES" : "NO"));
            if (!bluetoothEnabled)
            {
                report.AppendLine("Bluetooth Error: " + errorMsg);
            }
            
            // BLE advertising support check
            bool advertisingSupported = SupportsBleAdvertising(out errorMsg);
            report.AppendLine("BLE Advertising Support: " + (advertisingSupported ? "YES" : "NO"));
            if (!advertisingSupported)
            {
                report.AppendLine("Advertising Error: " + errorMsg);
            }
            
            // Permissions check
            if (Application.platform == RuntimePlatform.Android)
            {
                AndroidJavaClass versionClass = new AndroidJavaClass("android.os.Build$VERSION");
                int sdkInt = versionClass.GetStatic<int>("SDK_INT");
                
                if (sdkInt >= 31) // Android 12+
                {
                    bool hasConnectPermission = HasUserAuthorizedPermission("android.permission.BLUETOOTH_CONNECT");
                    bool hasScanPermission = HasUserAuthorizedPermission("android.permission.BLUETOOTH_SCAN");
                    bool hasAdvertisePermission = HasUserAuthorizedPermission("android.permission.BLUETOOTH_ADVERTISE");
                    
                    report.AppendLine("Permission BLUETOOTH_CONNECT: " + (hasConnectPermission ? "GRANTED" : "DENIED"));
                    report.AppendLine("Permission BLUETOOTH_SCAN: " + (hasScanPermission ? "GRANTED" : "DENIED"));
                    report.AppendLine("Permission BLUETOOTH_ADVERTISE: " + (hasAdvertisePermission ? "GRANTED" : "DENIED"));
                }
                else // Older Android
                {
                    report.AppendLine("Permissions: Not applicable for Android API " + sdkInt);
                }
            }
            
            // Bridge instance check
            if (bridgeInstance != null)
            {
                report.AppendLine("Bridge Instance: PRESENT");
                
                bool bridgeValid = VerifyBridgeInterface(bridgeInstance, out errorMsg);
                report.AppendLine("Bridge Interface Valid: " + (bridgeValid ? "YES" : "NO"));
                if (!bridgeValid)
                {
                    report.AppendLine("Bridge Error: " + errorMsg);
                }
                
                report.AppendLine("Initialized: " + IsInitialized);
                report.AppendLine("Advertising: " + IsAdvertising);
                report.AppendLine("Connected: " + IsConnected);
                
                if (IsConnected)
                {
                    report.AppendLine("Connected Device: " + ConnectedDeviceName + " (" + ConnectedDeviceAddress + ")");
                }
            }
            else
            {
                report.AppendLine("Bridge Instance: NOT PRESENT");
            }
            
            report.AppendLine("Last Error Code: " + LastErrorCode);
            report.AppendLine("Last Error Message: " + LastErrorMessage);
            
            report.AppendLine("===== End of Diagnostics =====");
            
            return report.ToString();
        }
        
        public bool StartAdvertising()
        {
            if (!CheckInitialized()) return false;
            
            try
            {
                Debug.Log("BleHidManager: Attempting to start advertising...");
                
                // Verify Bluetooth is enabled
                string errorMsg;
                if (!IsBluetoothEnabled(out errorMsg))
                {
                    Debug.LogError("BleHidManager: " + errorMsg);
                    OnError?.Invoke(BleHidConstants.ERROR_BLUETOOTH_DISABLED, errorMsg);
                    return false;
                }
                
                // Verify device supports advertising
                if (!SupportsBleAdvertising(out errorMsg))
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
            string[] parts = message.Split(new char[] { ':' }, 2);
            bool success = bool.Parse(parts[0]);
            string msg = parts.Length > 1 ? parts[1] : "";
            
            IsInitialized = success;
            
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
            
            IsAdvertising = advertising;
            
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
            
            IsConnected = connected;
            ConnectedDeviceName = deviceName;
            ConnectedDeviceAddress = deviceAddress;
            
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
            
            LastErrorCode = errorCode;
            LastErrorMessage = errorMessage;
            
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
        #endregion
        
        #region Environment Checks
        
        /// <summary>
        /// Verifies that plugins with the necessary functionality are loaded.
        /// Checks both the new and old namespaces.
        /// </summary>
        private bool VerifyPluginsLoaded(out string errorMsg)
        {
            errorMsg = "";
            
            if (Application.platform != RuntimePlatform.Android)
            {
                errorMsg = "BLE HID is only supported on Android";
                return false;
            }
            
            try
            {
                // First try the newer namespace
                AndroidJavaClass test = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityBridge");
                Debug.Log("Found plugin with new namespace (com.inventonater.blehid.unity)");
                return true;
            }
            catch (Exception ex1)
            {
                Debug.LogWarning("New namespace (com.inventonater.blehid.unity) not found: " + ex1.Message);
                
                try
                {
                    // Then try the older namespace
                    AndroidJavaClass test = new AndroidJavaClass("com.example.blehid.unity.BleHidUnityBridge");
                    Debug.Log("Found plugin with legacy namespace (com.example.blehid.unity)");
                    return true;
                }
                catch (Exception ex2)
                {
                    errorMsg = "BLE HID plugins not found in either new or legacy namespace";
                    Debug.LogError(errorMsg + ": " + ex2.Message);
                    return false;
                }
            }
        }
        
        /// <summary>
        /// Verify that the bridge interface is valid and responsive.
        /// </summary>
        private bool VerifyBridgeInterface(AndroidJavaObject bridge, out string errorMsg)
        {
            errorMsg = "";
            
            if (bridge == null)
            {
                errorMsg = "Bridge instance is null";
                return false;
            }
            
            try
            {
                // Call a method that shouldn't have side effects
                string result = bridge.Call<string>("toString");
                Debug.Log("Bridge interface verified: " + result);
                return true;
            }
            catch (Exception e)
            {
                errorMsg = "Bridge interface verification failed: " + e.Message;
                Debug.LogError(errorMsg);
                return false;
            }
        }
        
        /// <summary>
        /// Check if Bluetooth is enabled on the device.
        /// </summary>
        private bool IsBluetoothEnabled(out string errorMsg)
        {
            errorMsg = "";
            
            if (Application.platform != RuntimePlatform.Android)
            {
                errorMsg = "Bluetooth check only works on Android";
                return false;
            }
            
            try
            {
                AndroidJavaClass bluetoothAdapter = new AndroidJavaClass("android.bluetooth.BluetoothAdapter");
                AndroidJavaObject defaultAdapter = bluetoothAdapter.CallStatic<AndroidJavaObject>("getDefaultAdapter");
                
                if (defaultAdapter == null)
                {
                    errorMsg = "Bluetooth not supported on this device";
                    return false;
                }
                
                bool isEnabled = defaultAdapter.Call<bool>("isEnabled");
                if (!isEnabled)
                {
                    errorMsg = "Bluetooth is turned off";
                }
                return isEnabled;
            }
            catch (Exception e)
            {
                errorMsg = "Failed to check Bluetooth state: " + e.Message;
                Debug.LogError(errorMsg);
                return false;
            }
        }
        
        /// <summary>
        /// Check if the device supports BLE peripheral/advertising functionality.
        /// Not all Android devices can act as a BLE peripheral.
        /// </summary>
        private bool SupportsBleAdvertising(out string errorMsg)
        {
            errorMsg = "";
            
            if (Application.platform != RuntimePlatform.Android)
            {
                errorMsg = "BLE advertising check only works on Android";
                return false;
            }
            
            try
            {
                AndroidJavaClass bluetoothAdapter = new AndroidJavaClass("android.bluetooth.BluetoothAdapter");
                AndroidJavaObject defaultAdapter = bluetoothAdapter.CallStatic<AndroidJavaObject>("getDefaultAdapter");
                
                if (defaultAdapter == null)
                {
                    errorMsg = "Bluetooth not supported on this device";
                    return false;
                }
                
                // On some devices/Android versions this method might not exist, so we handle that case
                try 
                {
                    bool isSupported = defaultAdapter.Call<bool>("isMultipleAdvertisementSupported");
                    if (!isSupported)
                    {
                        errorMsg = "This device does not support BLE advertising";
                    }
                    return isSupported;
                }
                catch (Exception innerEx)
                {
                    // If the method doesn't exist, we can't be sure - use a different approach
                    Debug.LogWarning("Could not check BLE advertising support directly: " + innerEx.Message);
                    
                    // Check Android version as a fallback - M (23) and above generally support it
                    AndroidJavaClass buildVersion = new AndroidJavaClass("android.os.Build$VERSION");
                    int sdkInt = buildVersion.GetStatic<int>("SDK_INT");
                    
                    if (sdkInt < 23)
                    {
                        errorMsg = "BLE advertising likely not supported on Android " + sdkInt;
                        return false;
                    }
                    
                    // Can't be certain, but newer devices typically support it
                    Debug.Log("Could not definitively check BLE advertising support, but likely supported on Android " + sdkInt);
                    return true;
                }
            }
            catch (Exception e)
            {
                errorMsg = "Failed to check BLE advertising support: " + e.Message;
                Debug.LogError(errorMsg);
                return false;
            }
        }
        
        #endregion
        
        #region Helper Methods
        private RuntimePlatform RuntimePlatformSupportAndroid()
        {
            return RuntimePlatform.Android;
        }
        
        /// <summary>
        /// Request Bluetooth permissions required for Android 12+ (API 31+)
        /// </summary>
        private IEnumerator RequestBluetoothPermissions()
        {
            Debug.Log("Requesting Android 12+ Bluetooth permissions");
            
            // Request BLUETOOTH_CONNECT permission
            yield return RequestAndroidPermission("android.permission.BLUETOOTH_CONNECT");
            
            // Request BLUETOOTH_SCAN permission
            yield return RequestAndroidPermission("android.permission.BLUETOOTH_SCAN");
            
            // Request BLUETOOTH_ADVERTISE permission
            yield return RequestAndroidPermission("android.permission.BLUETOOTH_ADVERTISE");
            
            // Give a small delay to allow the permission requests to complete
            yield return new WaitForSeconds(0.5f);
        }
        
        /// <summary>
        /// Request a specific Android permission using Unity's Permission API
        /// </summary>
        private IEnumerator RequestAndroidPermission(string permission)
        {
            if (Application.platform != RuntimePlatform.Android)
                yield break;
                
            Debug.Log($"Requesting permission: {permission}");
            
            // Use Unity's permission system to check/request
            if (!HasUserAuthorizedPermission(permission))
            {
                Debug.Log($"Requesting permission: {permission}");
                RequestUserPermission(permission);
                
                // Wait briefly to allow the permission dialog to show and be handled
                yield return new WaitForSeconds(0.5f);
            }
            else
            {
                Debug.Log($"Permission already granted: {permission}");
            }
        }
        
        /// <summary>
        /// Check if the user has authorized the specified permission
        /// </summary>
        private bool HasUserAuthorizedPermission(string permission)
        {
            if (Application.platform != RuntimePlatform.Android)
                return true;
                
            AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            AndroidJavaClass compatClass = new AndroidJavaClass("androidx.core.content.ContextCompat");
            AndroidJavaClass permissionClass = new AndroidJavaClass("android.content.pm.PackageManager");
            int granted = permissionClass.GetStatic<int>("PERMISSION_GRANTED");
            
            int result = compatClass.CallStatic<int>("checkSelfPermission", currentActivity, permission);
            return result == granted;
        }
        
        /// <summary>
        /// Request the specified permission from the user
        /// </summary>
        private void RequestUserPermission(string permission)
        {
            if (Application.platform != RuntimePlatform.Android)
                return;
                
            try
            {
                AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
                AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
                AndroidJavaClass compatClass = new AndroidJavaClass("androidx.core.app.ActivityCompat");
                
                // Request permission - this will show the permission dialog
                compatClass.CallStatic("requestPermissions", currentActivity, new string[] { permission }, 0);
            }
            catch (Exception e)
            {
                Debug.LogError($"Error requesting permission {permission}: {e.Message}");
            }
        }
        
        /// <summary>
        /// Check if all required Bluetooth permissions are granted
        /// </summary>
        private bool CheckBluetoothPermissions()
        {
            if (Application.platform != RuntimePlatform.Android)
                return true;
                
            AndroidJavaClass versionClass = new AndroidJavaClass("android.os.Build$VERSION");
            int sdkInt = versionClass.GetStatic<int>("SDK_INT");
                
            // For Android 12+ (API 31+) we need these permissions
            if (sdkInt >= 31)
            {
                bool hasConnectPermission = HasUserAuthorizedPermission("android.permission.BLUETOOTH_CONNECT");
                bool hasScanPermission = HasUserAuthorizedPermission("android.permission.BLUETOOTH_SCAN");
                bool hasAdvertisePermission = HasUserAuthorizedPermission("android.permission.BLUETOOTH_ADVERTISE");
                
                Debug.Log($"Permissions: BLUETOOTH_CONNECT={hasConnectPermission}, BLUETOOTH_SCAN={hasScanPermission}, BLUETOOTH_ADVERTISE={hasAdvertisePermission}");
                
                return hasConnectPermission && hasScanPermission && hasAdvertisePermission;
            }
            
            // For older Android versions we check the legacy permissions
            return true; // These should be granted at install time pre-Android 12
        }
        
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
