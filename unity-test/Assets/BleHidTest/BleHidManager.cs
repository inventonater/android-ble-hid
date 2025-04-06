using UnityEngine;
using System;
using System.Collections;
using UnityEngine.UI;

namespace BleHid
{
    /// <summary>
    /// Unity wrapper for the Android BLE HID functionality.
    /// Provides a simple interface for controlling media playback and mouse movement via BLE.
    /// </summary>
    public class BleHidManager : MonoBehaviour
    {
        // Constants from HidMediaConstants.java
        public static class HidConstants
        {
            // Media button constants
            public const int BUTTON_PLAY_PAUSE = 0x01;
            public const int BUTTON_NEXT_TRACK = 0x02;
            public const int BUTTON_PREVIOUS_TRACK = 0x04;
            public const int BUTTON_VOLUME_UP = 0x08;
            public const int BUTTON_VOLUME_DOWN = 0x10;
            public const int BUTTON_MUTE = 0x20;

            // Mouse button constants
            public const int BUTTON_LEFT = 0x01;
            public const int BUTTON_RIGHT = 0x02;
            public const int BUTTON_MIDDLE = 0x04;
        }

        // Inspector-configurable fields
        [Header("Status UI")]
        [SerializeField] private Text statusText;
        [SerializeField] private Image connectionIndicator;
        [SerializeField] private Color connectedColor = Color.green;
        [SerializeField] private Color disconnectedColor = Color.red;
        [SerializeField] private Color notSupportedColor = Color.gray;
        
        // Public methods to set UI references
        public void SetStatusText(Text text)
        {
            statusText = text;
            if (statusText != null && _isInitialized)
            {
                UpdateStatusText(_isConnected ? "Connected" : (_isAdvertising ? "Advertising..." : "Ready - Not advertising"));
            }
        }
        
        public void SetConnectionIndicator(Image indicator)
        {
            connectionIndicator = indicator;
            if (connectionIndicator != null)
            {
                UpdateConnectionUI();
            }
        }

        // Events
        public event Action OnConnected;
        public event Action OnDisconnected;
        public event Action<bool> OnConnectionStateChanged;
        public event Action<string, int> WhenPairingRequested;

        // Private fields
        private AndroidJavaObject _activity;
        private AndroidJavaClass _pluginClass;
        private bool _isInitialized = false;
        private bool _isConnected = false;
        private bool _isBleSupported = false;
        private bool _isAdvertising = false;

        // Singleton pattern
        private static BleHidManager _instance;
        public static BleHidManager Instance
        {
            get
            {
                if (_instance == null)
                {
                    _instance = FindObjectOfType<BleHidManager>();
                    if (_instance == null)
                    {
                        GameObject go = new GameObject("BleHidManager");
                        _instance = go.AddComponent<BleHidManager>();
                        DontDestroyOnLoad(go);
                    }
                }
                return _instance;
            }
        }

        private void Awake()
        {
            if (_instance != null && _instance != this)
            {
                Destroy(gameObject);
                return;
            }

            _instance = this;
            DontDestroyOnLoad(gameObject);
        }

        private void Start()
        {
            InitializePlugin();
        }

        private void Update()
        {
            // If we're initialized, check the connection status periodically
            if (_isInitialized)
            {
                bool newConnectionState = IsConnected();
                if (newConnectionState != _isConnected)
                {
                    _isConnected = newConnectionState;
                    UpdateConnectionUI();

                    if (_isConnected)
                    {
                        if (OnConnected != null) OnConnected();
                    }
                    else
                    {
                        if (OnDisconnected != null) OnDisconnected();
                    }

                    if (OnConnectionStateChanged != null)
                    {
                        OnConnectionStateChanged(_isConnected);
                    }
                }
            }
        }

        private void OnDestroy()
        {
            ClosePlugin();
        }

        private void OnApplicationPause(bool pause)
        {
            if (pause)
            {
                // If application is paused, stop advertising
                if (_isAdvertising)
                {
                    StopAdvertising();
                }
            }
            else
            {
                // When returning from pause, restart advertising if we were advertising before
                if (_isInitialized && !_isConnected && _isAdvertising)
                {
                    StartAdvertising();
                }
            }
        }

        // Android 12+ permission strings
        private readonly string[] _requiredPermissions = new string[]
        {
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_ADVERTISE",
            "android.permission.BLUETOOTH_CONNECT"
        };
        
        // Permission handling
        private bool _permissionsRequested = false;
        private bool _permissionsGranted = false;
        
        // Service availability flags
        private bool _isMediaServiceAvailable = true;  // Media service always available in our implementation
        private bool _isMouseServiceAvailable = true;  // Mouse functionality is included in the Media service
        private bool _isKeyboardServiceAvailable = false; // Keyboard service might not be available

        /// <summary>
        /// Initializes the BLE HID plugin.
        /// </summary>
        /// <returns>True if initialization was successful, false otherwise.</returns>
        public bool InitializePlugin()
        {
            if (_isInitialized)
            {
                Debug.Log("[BleHidManager] Already initialized");
                return true;
            }

            try
            {
                if (Application.platform != RuntimePlatform.Android)
                {
                    Debug.LogWarning("[BleHidManager] Not running on Android, BLE HID functionality disabled");
                    UpdateStatusText("Not running on Android");
                    UpdateConnectionUI();
                    return false;
                }

                // Get the Unity Activity
                AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
                _activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
                
                // Check and request permissions for Android 12+
                if (Build.VERSION.SDK_INT >= 31) // Android 12 (SDK 31+)
                {
                    if (!CheckAndRequestPermissions())
                    {
                        UpdateStatusText("Waiting for permissions...");
                        return false;
                    }
                }

                // Get the plugin class
                _pluginClass = new AndroidJavaClass("com.example.blehid.unity.BleHidPlugin");

                // Register the callback handler
                AndroidJavaClass callbackClass = new AndroidJavaClass("com.example.blehid.unity.UnityCallback");
                callbackClass.CallStatic("setUnityGameObject", gameObject.name);

                // Initialize the plugin
                bool result = _pluginClass.CallStatic<bool>("initialize", _activity);
                if (result)
                {
                    Debug.Log("[BleHidManager] Initialized successfully");
                    _isInitialized = true;

                    // Check if BLE peripheral mode is supported
                    _isBleSupported = _pluginClass.CallStatic<bool>("isBlePeripheralSupported");
                    if (!_isBleSupported)
                    {
                        Debug.LogWarning("[BleHidManager] BLE peripheral mode not supported on this device");
                        UpdateStatusText("BLE peripheral not supported");
                    }
                    else
                    {
                        // Log available services for debugging
                        Debug.Log("[BleHidManager] Media service available: " + _isMediaServiceAvailable);
                        Debug.Log("[BleHidManager] Mouse functionality available: " + _isMouseServiceAvailable);
                        Debug.Log("[BleHidManager] Keyboard service available: " + _isKeyboardServiceAvailable);
                        
                        UpdateStatusText("Ready - Not advertising");
                    }
                }
                else
                {
                    Debug.LogError("[BleHidManager] Failed to initialize");
                    UpdateStatusText("Initialization failed");
                }

                // Set up the Java to Unity callback
                _pluginClass.CallStatic("setCallback", callbackClass);

                UpdateConnectionUI();
                return result;
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error initializing plugin: " + e.Message);
                UpdateStatusText("Error: " + e.Message);
                return false;
            }
        }
        
        /// <summary>
        /// Checks and requests required Android permissions
        /// </summary>
        /// <returns>True if all permissions are granted, false otherwise</returns>
        private bool CheckAndRequestPermissions()
        {
            // Skip if not on Android
            if (Application.platform != RuntimePlatform.Android)
                return true;
                
            try
            {
                // Get the current Android API level
                AndroidJavaClass buildVersionClass = new AndroidJavaClass("android.os.Build$VERSION");
                int sdkInt = buildVersionClass.GetStatic<int>("SDK_INT");
                
                // For Android 12+ we need the new Bluetooth permissions
                if (sdkInt >= 31)
                {
                    // Check if all required permissions are already granted
                    bool allPermissionsGranted = true;
                    foreach (string permission in _requiredPermissions)
                    {
                        int permissionStatus = _activity.Call<int>("checkSelfPermission", permission);
                        if (permissionStatus != 0) // 0 is PERMISSION_GRANTED
                        {
                            allPermissionsGranted = false;
                            break;
                        }
                    }
                    
                    if (allPermissionsGranted)
                    {
                        Debug.Log("[BleHidManager] All permissions already granted");
                        _permissionsGranted = true;
                        return true;
                    }
                    
                    // If permissions aren't granted and we haven't requested them yet
                    if (!_permissionsRequested)
                    {
                        Debug.Log("[BleHidManager] Requesting permissions...");
                        _activity.Call("requestPermissions", _requiredPermissions, 1);
                        _permissionsRequested = true;
                        StartCoroutine(CheckPermissionsAfterDelay());
                        return false;
                    }
                    
                    return _permissionsGranted;
                }
                
                // For older Android versions, permissions are granted at install time
                return true;
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error checking permissions: " + e.Message);
                return false;
            }
        }
        
        /// <summary>
        /// Coroutine to check permissions after a delay to let the user respond
        /// </summary>
        private IEnumerator CheckPermissionsAfterDelay()
        {
            // Wait a bit for user to respond to permission dialogs
            yield return new WaitForSeconds(1.0f);
            
            // Check if permissions have been granted
            bool allGranted = true;
            foreach (string permission in _requiredPermissions)
            {
                int permissionStatus = _activity.Call<int>("checkSelfPermission", permission);
                if (permissionStatus != 0) // 0 is PERMISSION_GRANTED
                {
                    allGranted = false;
                    break;
                }
            }
            
            _permissionsGranted = allGranted;
            
            if (_permissionsGranted)
            {
                Debug.Log("[BleHidManager] All permissions granted");
                UpdateStatusText("Permissions granted, initializing...");
                
                // Try to initialize again now that we have permissions
                InitializePlugin();
            }
            else
            {
                Debug.LogWarning("[BleHidManager] Not all permissions were granted");
                UpdateStatusText("Bluetooth permissions denied");
            }
        }

        /// <summary>
        /// Starts advertising the BLE HID device.
        /// </summary>
        /// <returns>True if advertising started successfully, false otherwise.</returns>
        public bool StartAdvertising()
        {
            if (!_isInitialized)
            {
                Debug.LogError("[BleHidManager] Not initialized");
                return false;
            }

            if (!_isBleSupported)
            {
                Debug.LogWarning("[BleHidManager] BLE peripheral mode not supported");
                return false;
            }

            if (_isConnected)
            {
                Debug.Log("[BleHidManager] Already connected, no need to advertise");
                return true;
            }

            try
            {
                bool result = _pluginClass.CallStatic<bool>("startAdvertising");
                if (result)
                {
                    Debug.Log("[BleHidManager] Advertising started");
                    UpdateStatusText("Advertising...");
                    _isAdvertising = true;
                }
                else
                {
                    Debug.LogError("[BleHidManager] Failed to start advertising");
                    UpdateStatusText("Failed to start advertising");
                }
                return result;
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error starting advertising: " + e.Message);
                return false;
            }
        }

        /// <summary>
        /// Stops advertising the BLE HID device.
        /// </summary>
        public void StopAdvertising()
        {
            if (!_isInitialized)
            {
                Debug.LogError("[BleHidManager] Not initialized");
                return;
            }

            if (!_isAdvertising)
            {
                return;
            }

            try
            {
                _pluginClass.CallStatic("stopAdvertising");
                Debug.Log("[BleHidManager] Advertising stopped");
                UpdateStatusText("Advertising stopped");
                _isAdvertising = false;
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error stopping advertising: " + e.Message);
            }
        }

        /// <summary>
        /// Checks if the device is connected to a host.
        /// </summary>
        /// <returns>True if connected, false otherwise.</returns>
        public bool IsConnected()
        {
            if (!_isInitialized || !_isBleSupported)
            {
                return false;
            }

            try
            {
                return _pluginClass.CallStatic<bool>("isConnected");
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error checking connection: " + e.Message);
                return false;
            }
        }

        /// <summary>
        /// Gets the address of the connected device.
        /// </summary>
        /// <returns>The MAC address of the connected device, or null if not connected.</returns>
        public string GetConnectedDeviceAddress()
        {
            if (!_isInitialized || !_isBleSupported || !_isConnected)
            {
                return null;
            }

            try
            {
                return _pluginClass.CallStatic<string>("getConnectedDeviceAddress");
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error getting connected device address: " + e.Message);
                return null;
            }
        }

        /// <summary>
        /// Releases resources when the plugin is no longer needed.
        /// </summary>
        public void ClosePlugin()
        {
            if (!_isInitialized)
            {
                return;
            }

            try
            {
                _pluginClass.CallStatic("close");
                Debug.Log("[BleHidManager] Plugin closed");
                _isInitialized = false;
                _isConnected = false;
                _isAdvertising = false;
                UpdateStatusText("Plugin closed");
                UpdateConnectionUI();
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error closing plugin: " + e.Message);
            }
        }

        // Media Control Methods

        /// <summary>
        /// Sends play/pause media control command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool PlayPause()
        {
            if (!CheckConnection()) return false;
            
            try
            {
                bool result = _pluginClass.CallStatic<bool>("playPause");
                if (!result)
                {
                    Debug.LogError("[BleHidManager] Failed to send play/pause command");
                }
                return result;
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error sending play/pause command: " + e.Message);
                return false;
            }
        }

        /// <summary>
        /// Sends next track media control command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool NextTrack()
        {
            if (!CheckConnection()) return false;
            
            try
            {
                bool result = _pluginClass.CallStatic<bool>("nextTrack");
                if (!result)
                {
                    Debug.LogError("[BleHidManager] Failed to send next track command");
                }
                return result;
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error sending next track command: " + e.Message);
                return false;
            }
        }

        /// <summary>
        /// Sends previous track media control command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool PreviousTrack()
        {
            if (!CheckConnection()) return false;
            
            try
            {
                bool result = _pluginClass.CallStatic<bool>("previousTrack");
                if (!result)
                {
                    Debug.LogError("[BleHidManager] Failed to send previous track command");
                }
                return result;
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error sending previous track command: " + e.Message);
                return false;
            }
        }

        /// <summary>
        /// Sends volume up media control command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool VolumeUp()
        {
            if (!CheckConnection()) return false;
            
            try
            {
                bool result = _pluginClass.CallStatic<bool>("volumeUp");
                if (!result)
                {
                    Debug.LogError("[BleHidManager] Failed to send volume up command");
                }
                return result;
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error sending volume up command: " + e.Message);
                return false;
            }
        }

        /// <summary>
        /// Sends volume down media control command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool VolumeDown()
        {
            if (!CheckConnection()) return false;
            
            try
            {
                bool result = _pluginClass.CallStatic<bool>("volumeDown");
                if (!result)
                {
                    Debug.LogError("[BleHidManager] Failed to send volume down command");
                }
                return result;
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error sending volume down command: " + e.Message);
                return false;
            }
        }

        /// <summary>
        /// Sends mute media control command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool Mute()
        {
            if (!CheckConnection()) return false;
            
            try
            {
                bool result = _pluginClass.CallStatic<bool>("mute");
                if (!result)
                {
                    Debug.LogError("[BleHidManager] Failed to send mute command");
                }
                return result;
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error sending mute command: " + e.Message);
                return false;
            }
        }

        // Mouse Control Methods

        /// <summary>
        /// Moves the mouse pointer by the specified amount.
        /// </summary>
        /// <param name="x">X-axis movement (-127 to 127)</param>
        /// <param name="y">Y-axis movement (-127 to 127)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool MoveMouse(int x, int y)
        {
            if (!CheckConnection()) return false;
            
            try
            {
                bool result = _pluginClass.CallStatic<bool>("moveMouse", x, y);
                if (!result)
                {
                    Debug.LogError("[BleHidManager] Failed to move mouse");
                }
                return result;
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error moving mouse: " + e.Message);
                return false;
            }
        }

        /// <summary>
        /// Moves the mouse using Vector2 input. Useful for joystick input.
        /// </summary>
        /// <param name="movement">Vector2 representing mouse movement</param>
        /// <param name="sensitivity">Sensitivity multiplier</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool MoveMouse(Vector2 movement, float sensitivity = 1.0f)
        {
            int x = Mathf.Clamp(Mathf.RoundToInt(movement.x * sensitivity), -127, 127);
            int y = Mathf.Clamp(Mathf.RoundToInt(movement.y * sensitivity), -127, 127);
            return MoveMouse(x, y);
        }

        /// <summary>
        /// Presses a mouse button.
        /// </summary>
        /// <param name="button">The button to press (HidConstants.BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool PressMouseButton(int button)
        {
            if (!CheckConnection()) return false;
            
            try
            {
                bool result = _pluginClass.CallStatic<bool>("pressMouseButton", button);
                if (!result)
                {
                    Debug.LogError("[BleHidManager] Failed to press mouse button");
                }
                return result;
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error pressing mouse button: " + e.Message);
                return false;
            }
        }

        /// <summary>
        /// Releases all mouse buttons.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool ReleaseMouseButtons()
        {
            if (!CheckConnection()) return false;
            
            try
            {
                bool result = _pluginClass.CallStatic<bool>("releaseMouseButtons");
                if (!result)
                {
                    Debug.LogError("[BleHidManager] Failed to release mouse buttons");
                }
                return result;
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error releasing mouse buttons: " + e.Message);
                return false;
            }
        }

        /// <summary>
        /// Performs a click with the specified button.
        /// </summary>
        /// <param name="button">The button to click (HidConstants.BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool ClickMouseButton(int button)
        {
            if (!CheckConnection()) return false;
            
            try
            {
                bool result = _pluginClass.CallStatic<bool>("clickMouseButton", button);
                if (!result)
                {
                    Debug.LogError("[BleHidManager] Failed to click mouse button");
                }
                return result;
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error clicking mouse button: " + e.Message);
                return false;
            }
        }

        /// <summary>
        /// Scrolls the mouse wheel.
        /// </summary>
        /// <param name="amount">The scroll amount (-127 to 127)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool ScrollMouseWheel(int amount)
        {
            if (!CheckConnection()) return false;
            
            try
            {
                bool result = _pluginClass.CallStatic<bool>("scrollMouseWheel", amount);
                if (!result)
                {
                    Debug.LogError("[BleHidManager] Failed to scroll mouse wheel");
                }
                return result;
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error scrolling mouse wheel: " + e.Message);
                return false;
            }
        }

        // Combined Media and Mouse Control

        /// <summary>
        /// Sends a combined media and mouse report.
        /// </summary>
        /// <param name="mediaButtons">Media button flags (HidConstants.BUTTON_PLAY_PAUSE, etc.)</param>
        /// <param name="mouseButtons">Mouse button flags (HidConstants.BUTTON_LEFT, etc.)</param>
        /// <param name="x">X-axis movement (-127 to 127)</param>
        /// <param name="y">Y-axis movement (-127 to 127)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool SendCombinedReport(int mediaButtons, int mouseButtons, int x, int y)
        {
            if (!CheckConnection()) return false;
            
            try
            {
                bool result = _pluginClass.CallStatic<bool>("sendCombinedReport", mediaButtons, mouseButtons, x, y);
                if (!result)
                {
                    Debug.LogError("[BleHidManager] Failed to send combined report");
                }
                return result;
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidManager] Error sending combined report: " + e.Message);
                return false;
            }
        }

        // Helper methods

        /// <summary>
        /// Checks if BLE peripheral mode is supported on the device.
        /// </summary>
        /// <returns>True if supported, false otherwise.</returns>
        public bool IsBlePeripheralSupported()
        {
            return _isBleSupported;
        }

        /// <summary>
        /// Checks if the plugin is initialized.
        /// </summary>
        /// <returns>True if initialized, false otherwise.</returns>
        public bool IsInitialized()
        {
            return _isInitialized;
        }

        /// <summary>
        /// Checks if the device is currently advertising.
        /// </summary>
        /// <returns>True if advertising, false otherwise.</returns>
        public bool IsAdvertising()
        {
            return _isAdvertising;
        }

        // Unity callback methods - these are called from Java

        /// <summary>
        /// Called when a pairing request is received.
        /// </summary>
        public void OnPairingRequested(string address, int variant)
        {
            Debug.Log("[BleHidManager] Pairing requested from: " + address + ", variant: " + variant);
            if (WhenPairingRequested != null)
            {
                WhenPairingRequested(address, variant);
            }
        }

        /// <summary>
        /// Called when a device connects.
        /// </summary>
        public void OnDeviceConnected(string address)
        {
            Debug.Log("[BleHidManager] Device connected: " + address);
            _isConnected = true;
            _isAdvertising = false;
            UpdateStatusText("Connected to: " + address);
            UpdateConnectionUI();

            if (OnConnected != null)
            {
                OnConnected();
            }

            if (OnConnectionStateChanged != null)
            {
                OnConnectionStateChanged(true);
            }
        }

        /// <summary>
        /// Called when pairing fails.
        /// </summary>
        public void OnPairingFailed(string address)
        {
            Debug.Log("[BleHidManager] Pairing failed with: " + address);
            UpdateStatusText("Pairing failed");
        }

        // Private helper methods

        private bool CheckConnection()
        {
            if (!_isInitialized)
            {
                Debug.LogError("[BleHidManager] Not initialized");
                return false;
            }

            if (!_isBleSupported)
            {
                Debug.LogWarning("[BleHidManager] BLE peripheral mode not supported");
                return false;
            }

            if (!_isConnected)
            {
                Debug.LogWarning("[BleHidManager] Not connected to a host");
                return false;
            }

            return true;
        }

        private void UpdateStatusText(string message)
        {
            if (statusText != null)
            {
                statusText.text = message;
            }
        }

        private void UpdateConnectionUI()
        {
            if (connectionIndicator != null)
            {
                if (!_isInitialized || !_isBleSupported)
                {
                    connectionIndicator.color = notSupportedColor;
                }
                else if (_isConnected)
                {
                    connectionIndicator.color = connectedColor;
                }
                else
                {
                    connectionIndicator.color = disconnectedColor;
                }
            }
        }
    }
}
