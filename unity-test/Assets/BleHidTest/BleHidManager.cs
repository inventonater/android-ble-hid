using System;
using System.Collections;
using System.Runtime.InteropServices;
using UnityEngine;

namespace BleHid
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
            
            // Create bridge and initialize
            bool initResult = false;
            
            try
            {
                // Get the Unity activity
                AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
                AndroidJavaObject activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
                
                // Get the BleHidUnityBridge instance
                AndroidJavaClass bridgeClass = new AndroidJavaClass("com.example.blehid.unity.BleHidUnityBridge");
                bridgeInstance = bridgeClass.CallStatic<AndroidJavaObject>("getInstance");
                
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
        public bool StartAdvertising()
        {
            if (!CheckInitialized()) return false;
            
            try
            {
                bool result = bridgeInstance.Call<bool>("startAdvertising");
                return result;
            }
            catch (Exception e)
            {
                string message = "Exception starting advertising: " + e.Message;
                Debug.LogException(e);
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
        
        #region Helper Methods
        private RuntimePlatform RuntimePlatformSupportAndroid()
        {
            return RuntimePlatform.Android;
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
