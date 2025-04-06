using System;
using System.Collections;
using UnityEngine;
using UnityEngine.UI;


namespace BleHid
{
    /// <summary>
    /// Demo controller for BLE HID functionality.
    /// Provides a simple UI to demonstrate media and mouse control capabilities.
    /// Includes error recovery and graceful degradation features.
    /// </summary>
    public class BleHidDemoController : MonoBehaviour
    {
        [Header("UI References")]
        [SerializeField] public Button initButton;
        [SerializeField] public Button advertiseButton;
        [SerializeField] public Text statusText;
        [SerializeField] public Image connectionIndicator;

        [Header("Media Controls")]
        [SerializeField] public Button playPauseButton;
        [SerializeField] public Button nextTrackButton;
        [SerializeField] public Button prevTrackButton;
        [SerializeField] public Button volumeUpButton;
        [SerializeField] public Button volumeDownButton;
        [SerializeField] public Button muteButton;

        [Header("Mouse Controls")]
        [SerializeField] public MouseTouchpad mouseTouchpad; // Custom touchpad component for mouse movement
        [SerializeField] public Button leftClickButton;
        [SerializeField] public Button rightClickButton;
        [SerializeField] public Slider scrollSlider;

        [Header("Settings")]
        [SerializeField] public float mouseSensitivity = 5f;
        
        [Header("Error Recovery")]
        [SerializeField] public bool enableAutoRecovery = true;
        [SerializeField] public float reconnectDelay = 3.0f;
        [SerializeField] public int maxReconnectAttempts = 3;
        [SerializeField] public Button retryButton; // Optional retry button for manual recovery
        [SerializeField] public Button settingsButton; // Optional settings button to open system settings

        // Reference to BLE HID manager
        [HideInInspector] public BleHidManager bleManager;
        
        // Flag to control auto-initialization
        [Header("Initialization")]
        [Tooltip("If true, will automatically initialize the BleHidPlugin")]
        public bool autoInitialize = false;
        
        // Error recovery state
        private bool _recoveryInProgress = false;
        private int _recoveryAttempts = 0;
        private float _lastErrorTime = 0f;
        private string _lastErrorMessage = "";
        
        // Feature availability flags (for graceful degradation)
        private bool _mediaControlsAvailable = false;
        private bool _mouseControlsAvailable = false;
        private bool _bluetoothEnabled = false;
        private BleHidManager.InitState _lastKnownState = BleHidManager.InitState.NotInitialized;

        private void Start()
        {
            // Get reference to BleHidManager singleton instance
            if (bleManager == null)
            {
                bleManager = BleHidManager.Instance;
            }
            
            // Pass UI references to the manager using public methods
            if (statusText != null)
            {
                bleManager.SetStatusText(statusText);
            }
            
            if (connectionIndicator != null)
            {
                bleManager.SetConnectionIndicator(connectionIndicator);
            }

            // Setup touchpad mouse control
            if (mouseTouchpad != null)
            {
                // Subscribe to the touchpad's mouse move event
                mouseTouchpad.OnMouseMove += HandleMouseMovement;
            }

            // Setup button listeners including recovery buttons
            SetupButtons();

            // Subscribe to connection events
            bleManager.OnConnected += OnDeviceConnected;
            bleManager.OnDisconnected += OnDeviceDisconnected;
            
            // Check Bluetooth status initially
            if (Application.platform == RuntimePlatform.Android)
            {
                _bluetoothEnabled = CheckBluetoothEnabled();
            }

            // Initialize automatically if desired
            if (autoInitialize)
            {
                InitializeWithErrorHandling();
            }
            
            // Start a periodic feature check to handle changing conditions
            InvokeRepeating("CheckFeatureAvailability", 2.0f, 5.0f);
        }
        
        /// <summary>
        /// Initialize the BLE functionality with error handling
        /// </summary>
        private void InitializeWithErrorHandling()
        {
            if (bleManager == null)
            {
                Debug.LogError("[BleHidDemoController] BleHidManager reference is missing");
                UpdateStatusWithError("BLE manager not found");
                return;
            }
            
            bool result = bleManager.InitializePlugin();
            
            if (!result)
            {
                // Check what state we're in to provide better user feedback
                _lastKnownState = bleManager.CurrentInitState;
                
                switch (_lastKnownState)
                {
                    case BleHidManager.InitState.WaitingForPermissions:
                        Debug.Log("[BleHidDemoController] Waiting for permissions...");
                        break;
                        
                    case BleHidManager.InitState.InitializationFailed:
                        UpdateStatusWithError("Initialization failed");
                        if (enableAutoRecovery)
                        {
                            StartCoroutine(AttemptRecovery());
                        }
                        break;
                        
                    case BleHidManager.InitState.Unsupported:
                        UpdateStatusWithError("BLE not supported on this device");
                        // Can't recover from hardware limitation - disable features gracefully
                        GracefullyDegradeUnsupportedFeatures();
                        break;
                }
            }
            else
            {
                // Initialization succeeded, update UI appropriately
                SetControlButtonsInteractable(bleManager.IsInitialized() && bleManager.IsBlePeripheralSupported());
                CheckFeatureAvailability();
            }
        }

        private void SetupButtons()
        {
            // System buttons
            if (initButton != null)
            {
                initButton.onClick.AddListener(() =>
                {
                    if (statusText != null)
                    {
                        statusText.text = "Initializing...";
                    }
                    
                    // Reset recovery state
                    _recoveryInProgress = false;
                    _recoveryAttempts = 0;
                    
                    // Try to initialize with error handling
                    InitializeWithErrorHandling();
                });
            }

            if (advertiseButton != null)
            {
                advertiseButton.onClick.AddListener(() =>
                {
                    try
                    {
                        if (bleManager.IsAdvertising())
                        {
                            bleManager.StopAdvertising();
                            advertiseButton.GetComponentInChildren<Text>().text = "Start Advertising";
                        }
                        else
                        {
                            // Check Bluetooth status before attempting to advertise
                            if (!_bluetoothEnabled)
                            {
                                _bluetoothEnabled = CheckBluetoothEnabled();
                                if (!_bluetoothEnabled)
                                {
                                    UpdateStatusWithError("Bluetooth is disabled. Please enable Bluetooth.");
                                    if (settingsButton != null) settingsButton.gameObject.SetActive(true);
                                    return;
                                }
                            }
                            
                            bool success = bleManager.StartAdvertising();
                            if (success)
                            {
                                advertiseButton.GetComponentInChildren<Text>().text = "Stop Advertising";
                            }
                            else
                            {
                                UpdateStatusWithError("Failed to start advertising");
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        Debug.LogError("[BleHidDemoController] Error in advertise button handler: " + e.Message);
                        UpdateStatusWithError("Advertising error: " + e.Message);
                    }
                });
            }

            // Add error recovery buttons
            if (retryButton != null)
            {
                // Hide initially
                retryButton.gameObject.SetActive(false);
                
                // Set retry handler
                retryButton.onClick.AddListener(() =>
                {
                    retryButton.gameObject.SetActive(false);
                    if (settingsButton != null) settingsButton.gameObject.SetActive(false);
                    
                    if (statusText != null)
                    {
                        statusText.text = "Retrying...";
                    }
                    
                    // Reset recovery state
                    _recoveryAttempts = 0;
                    _recoveryInProgress = false;
                    
                    // Try again
                    InitializeWithErrorHandling();
                });
            }
            
            if (settingsButton != null)
            {
                // Hide initially
                settingsButton.gameObject.SetActive(false);
                
                // Set settings handler to open system settings
                settingsButton.onClick.AddListener(() =>
                {
                    OpenBluetoothSettings();
                });
            }

            // Add safe wrappers around control buttons to catch exceptions
            
            // Media control buttons
            if (playPauseButton != null)
                playPauseButton.onClick.AddListener(() => SafeExecuteAction(bleManager.PlayPause, "play/pause"));

            if (nextTrackButton != null)
                nextTrackButton.onClick.AddListener(() => SafeExecuteAction(bleManager.NextTrack, "next track"));

            if (prevTrackButton != null)
                prevTrackButton.onClick.AddListener(() => SafeExecuteAction(bleManager.PreviousTrack, "previous track"));

            if (volumeUpButton != null)
                volumeUpButton.onClick.AddListener(() => SafeExecuteAction(bleManager.VolumeUp, "volume up"));

            if (volumeDownButton != null)
                volumeDownButton.onClick.AddListener(() => SafeExecuteAction(bleManager.VolumeDown, "volume down"));

            if (muteButton != null)
                muteButton.onClick.AddListener(() => SafeExecuteAction(bleManager.Mute, "mute"));

            // Mouse control buttons
            if (leftClickButton != null)
                leftClickButton.onClick.AddListener(() => SafeExecuteAction(() => bleManager.ClickMouseButton(BleHidManager.HidConstants.BUTTON_LEFT), "left click"));

            if (rightClickButton != null)
                rightClickButton.onClick.AddListener(() => SafeExecuteAction(() => bleManager.ClickMouseButton(BleHidManager.HidConstants.BUTTON_RIGHT), "right click"));

            // Set all media and mouse control buttons initially disabled
            SetControlButtonsInteractable(false);
        }
        
        /// <summary>
        /// Safely executes an action with error handling
        /// </summary>
        private void SafeExecuteAction(Func<bool> action, string actionName)
        {
            try
            {
                bool result = action();
                if (!result)
                {
                    Debug.LogWarning($"[BleHidDemoController] {actionName} command failed");
                    
                    // If we're connected but command failed, there might be an issue with the connection
                    if (bleManager.IsConnected())
                    {
                        HandlePossibleConnectionIssue();
                    }
                }
            }
            catch (Exception e)
            {
                Debug.LogError($"[BleHidDemoController] Error executing {actionName}: {e.Message}");
                UpdateStatusWithError($"Error: {e.Message}");
                HandlePossibleConnectionIssue();
            }
        }

        private void Update()
        {
            // Check for state changes if BleHidManager is available
            if (bleManager != null && _lastKnownState != bleManager.CurrentInitState)
            {
                _lastKnownState = bleManager.CurrentInitState;
                
                if (_lastKnownState == BleHidManager.InitState.InitializedSuccessfully && !_recoveryInProgress)
                {
                    // Successful initialization - hide recovery buttons
                    if (retryButton != null) retryButton.gameObject.SetActive(false);
                    if (settingsButton != null) settingsButton.gameObject.SetActive(false);
                    
                    // Update feature availability
                    CheckFeatureAvailability();
                }
            }
            
            // Mouse touchpad input is handled via events

            // Handle scroll wheel input if available and connected
            if (scrollSlider != null && bleManager != null && bleManager.IsConnected() && _mouseControlsAvailable)
            {
                try
                {
                    // Map slider value (0-1) to scroll range (-127 to 127)
                    float normalizedValue = (scrollSlider.value - 0.5f) * 2f; // -1 to 1
                    int scrollAmount = Mathf.RoundToInt(normalizedValue * 127);
    
                    // Only send scroll if there's actual input
                    if (Mathf.Abs(scrollAmount) > 5)
                    {
                        bleManager.ScrollMouseWheel(scrollAmount);
    
                        // Reset slider to center after use
                        scrollSlider.value = 0.5f;
                    }
                }
                catch (Exception e)
                {
                    // Silent fail for continuous operations - just log the error
                    Debug.LogWarning("[BleHidDemoController] Scroll wheel error: " + e.Message);
                }
            }
        }

        private void OnDeviceConnected()
        {
            // Reset recovery state since we're now connected
            _recoveryInProgress = false;
            _recoveryAttempts = 0;
            
            // Hide recovery UI
            if (retryButton != null) retryButton.gameObject.SetActive(false);
            if (settingsButton != null) settingsButton.gameObject.SetActive(false);
            
            // Update feature availability
            CheckFeatureAvailability();
            
            // Enable appropriate controls
            SetControlButtonsInteractable(true);

            if (advertiseButton != null)
            {
                advertiseButton.interactable = false;
                advertiseButton.GetComponentInChildren<Text>().text = "Connected";
            }
            
            Debug.Log("[BleHidDemoController] Connected to device: " + bleManager.GetConnectedDeviceAddress());
        }

        private void OnDeviceDisconnected()
        {
            // Disable control buttons when disconnected
            SetControlButtonsInteractable(false);

            if (advertiseButton != null)
            {
                advertiseButton.interactable = true;
                advertiseButton.GetComponentInChildren<Text>().text = "Start Advertising";
            }
            
            Debug.Log("[BleHidDemoController] Device disconnected");
            
            // Check if we should attempt auto-reconnect
            if (enableAutoRecovery && !_recoveryInProgress)
            {
                StartCoroutine(AttemptReconnect());
            }
        }
        
        /// <summary>
        /// Attempt to reconnect after disconnection
        /// </summary>
        private IEnumerator AttemptReconnect()
        {
            if (_recoveryInProgress) yield break;
            
            _recoveryInProgress = true;
            _recoveryAttempts = 0;
            
            if (statusText != null)
            {
                statusText.text = "Disconnected. Attempting to reconnect...";
            }
            
            // Wait a moment before trying to reconnect
            yield return new WaitForSeconds(reconnectDelay);
            
            while (_recoveryAttempts < maxReconnectAttempts)
            {
                _recoveryAttempts++;
                
                if (statusText != null)
                {
                    statusText.text = $"Reconnection attempt {_recoveryAttempts}/{maxReconnectAttempts}...";
                }
                
                // First check if Bluetooth is still enabled
                if (!CheckBluetoothEnabled())
                {
                    UpdateStatusWithError("Bluetooth is disabled. Please enable Bluetooth.");
                    if (settingsButton != null) settingsButton.gameObject.SetActive(true);
                    break;
                }
                
                // Try to start advertising again
                bool result = bleManager.StartAdvertising();
                
                if (result)
                {
                    if (advertiseButton != null)
                    {
                        advertiseButton.GetComponentInChildren<Text>().text = "Stop Advertising";
                    }
                    
                    if (statusText != null)
                    {
                        statusText.text = "Advertising... Waiting for connection";
                    }
                    
                    // Success - we're advertising again
                    Debug.Log("[BleHidDemoController] Auto-reconnect: Started advertising");
                    _recoveryInProgress = false;
                    yield break;
                }
                
                // Wait before trying again
                yield return new WaitForSeconds(reconnectDelay);
            }
            
            // If we get here, reconnection failed
            if (_recoveryAttempts >= maxReconnectAttempts)
            {
                UpdateStatusWithError("Reconnection failed after multiple attempts");
                // Show manual retry button
                if (retryButton != null) retryButton.gameObject.SetActive(true);
            }
            
            _recoveryInProgress = false;
        }

        private void SetControlButtonsInteractable(bool interactable)
        {
            // Set media buttons interactable state - based on connection and feature availability
            bool mediaEnabled = interactable && _mediaControlsAvailable;
            if (playPauseButton != null) playPauseButton.interactable = mediaEnabled;
            if (nextTrackButton != null) nextTrackButton.interactable = mediaEnabled;
            if (prevTrackButton != null) prevTrackButton.interactable = mediaEnabled;
            if (volumeUpButton != null) volumeUpButton.interactable = mediaEnabled;
            if (volumeDownButton != null) volumeDownButton.interactable = mediaEnabled;
            if (muteButton != null) muteButton.interactable = mediaEnabled;

            // Set mouse buttons interactable state - based on connection and feature availability
            bool mouseEnabled = interactable && _mouseControlsAvailable;
            if (leftClickButton != null) leftClickButton.interactable = mouseEnabled;
            if (rightClickButton != null) rightClickButton.interactable = mouseEnabled;
            if (scrollSlider != null) scrollSlider.interactable = mouseEnabled;
            
            // Enable/disable touchpad based on connection status and mouse availability
            if (mouseTouchpad != null)
            {
                mouseTouchpad.enabled = mouseEnabled;
                
                // Update visual feedback on touchpad
                if (mouseTouchpad.touchpadBackground != null)
                {
                    Color bgColor = mouseTouchpad.touchpadBackground.color;
                    bgColor.a = mouseEnabled ? 0.5f : 0.2f; // Fade out when disabled
                    mouseTouchpad.touchpadBackground.color = bgColor;
                }
            }
        }

        /// <summary>
        /// Handle mouse movement from the touchpad
        /// </summary>
        private void HandleMouseMovement(Vector2 movementDelta)
        {
            if (bleManager != null && bleManager.IsConnected() && _mouseControlsAvailable)
            {
                try
                {
                    // Apply sensitivity and send movement to BLE HID manager
                    bleManager.MoveMouse(movementDelta, mouseSensitivity);
                }
                catch (Exception e)
                {
                    // Silent failure for continuous operations - just log the error
                    Debug.LogWarning("[BleHidDemoController] Mouse movement error: " + e.Message);
                }
            }
        }
        
        /// <summary>
        /// Attempt to recover from initialization failure
        /// </summary>
        private IEnumerator AttemptRecovery()
        {
            if (_recoveryInProgress) yield break;
            
            _recoveryInProgress = true;
            _recoveryAttempts = 0;
            
            while (_recoveryAttempts < maxReconnectAttempts)
            {
                _recoveryAttempts++;
                
                if (statusText != null)
                {
                    statusText.text = $"Recovery attempt {_recoveryAttempts}/{maxReconnectAttempts}...";
                }
                
                // Check if Bluetooth is enabled
                if (!CheckBluetoothEnabled())
                {
                    UpdateStatusWithError("Bluetooth is disabled. Please enable Bluetooth.");
                    if (settingsButton != null) settingsButton.gameObject.SetActive(true);
                    break;
                }
                
                // Try initialization again
                bool result = bleManager.InitializePlugin();
                
                if (result)
                {
                    // Check if BLE is supported
                    if (bleManager.IsBlePeripheralSupported())
                    {
                        // Success - initialization worked
                        SetControlButtonsInteractable(true);
                        if (statusText != null) statusText.text = "Recovered successfully";
                        Debug.Log("[BleHidDemoController] Auto-recovery successful");
                        _recoveryInProgress = false;
                        
                        // Hide recovery buttons
                        if (retryButton != null) retryButton.gameObject.SetActive(false);
                        if (settingsButton != null) settingsButton.gameObject.SetActive(false);
                        
                        yield break;
                    }
                }
                
                // Wait before trying again
                yield return new WaitForSeconds(reconnectDelay);
            }
            
            // If we get here, recovery failed
            if (_recoveryAttempts >= maxReconnectAttempts)
            {
                UpdateStatusWithError("Recovery failed after multiple attempts");
                // Show manual retry button
                if (retryButton != null) retryButton.gameObject.SetActive(true);
            }
            
            _recoveryInProgress = false;
        }
        
        /// <summary>
        /// Check if features are available based on current state
        /// </summary>
        private void CheckFeatureAvailability()
        {
            bool isInitialized = bleManager != null && bleManager.IsInitialized();
            bool isSupported = isInitialized && bleManager.IsBlePeripheralSupported();
            bool isConnected = isSupported && bleManager.IsConnected();
            
            // Check Bluetooth status
            _bluetoothEnabled = CheckBluetoothEnabled();
            
            // Update feature flags - all features require connection
            _mediaControlsAvailable = isConnected;
            _mouseControlsAvailable = isConnected;
            
            // Update UI based on new feature availability
            SetControlButtonsInteractable(isConnected);
            
            Debug.Log($"[BleHidDemoController] Feature Check - Media: {_mediaControlsAvailable}, Mouse: {_mouseControlsAvailable}, BT: {_bluetoothEnabled}");
        }
        
        /// <summary>
        /// Handle a potential connection issue with error recovery
        /// </summary>
        private void HandlePossibleConnectionIssue()
        {
            // If we're getting command failures while connected,
            // there might be connection issues
            if (bleManager != null && bleManager.IsConnected())
            {
                bool isReallyConnected = bleManager.IsConnected();
                
                if (!isReallyConnected)
                {
                    // Connection state is inconsistent - force a disconnect and try to recover
                    Debug.LogWarning("[BleHidDemoController] Detected inconsistent connection state");
                    OnDeviceDisconnected();
                    
                    // Auto-recovery will be triggered by OnDeviceDisconnected if enabled
                }
                else
                {
                    // The connection test says we're connected, but commands are failing
                    // This could be a temporary blip or an issue with the specific command
                    Debug.LogWarning("[BleHidDemoController] Command failures while connected");
                    
                    // Record error time and count for potential future recovery
                    _lastErrorTime = Time.time;
                }
            }
        }
        
        /// <summary>
        /// Update status text with error message and visual indicators
        /// </summary>
        private void UpdateStatusWithError(string errorMessage)
        {
            if (statusText != null)
            {
                statusText.text = errorMessage;
            }
            
            _lastErrorMessage = errorMessage;
            _lastErrorTime = Time.time;
            
            Debug.LogError("[BleHidDemoController] Error: " + errorMessage);
            
            // Show recovery buttons based on error type
            if (errorMessage.Contains("Bluetooth is disabled") && settingsButton != null)
            {
                settingsButton.gameObject.SetActive(true);
            }
            
            if (retryButton != null && !retryButton.gameObject.activeSelf && 
                !errorMessage.Contains("not supported"))  // Don't show retry for unsupported hardware
            {
                retryButton.gameObject.SetActive(true);
            }
        }
        
        /// <summary>
        /// Check if Bluetooth is enabled on the device
        /// </summary>
        private bool CheckBluetoothEnabled()
        {
            if (Application.platform != RuntimePlatform.Android)
                return true;  // Assume enabled on non-Android platforms
                
            if (bleManager != null)
            {
                return bleManager.IsBluetoothEnabled();
            }
            
            return false;
        }
        
        /// <summary>
        /// Open Android's Bluetooth settings page
        /// </summary>
        private void OpenBluetoothSettings()
        {
            if (Application.platform != RuntimePlatform.Android)
                return;
                
            try
            {
                AndroidJavaObject activity = new AndroidJavaClass("com.unity3d.player.UnityPlayer")
                    .GetStatic<AndroidJavaObject>("currentActivity");
                
                AndroidJavaObject intent = new AndroidJavaObject("android.content.Intent",
                    "android.settings.BLUETOOTH_SETTINGS");
                
                activity.Call("startActivity", intent);
                
                Debug.Log("[BleHidDemoController] Opened Bluetooth settings");
            }
            catch (Exception e)
            {
                Debug.LogError("[BleHidDemoController] Failed to open Bluetooth settings: " + e.Message);
                UpdateStatusWithError("Could not open Bluetooth settings");
            }
        }
        
        /// <summary>
        /// Gracefully degrade experience when BLE features not supported
        /// </summary>
        private void GracefullyDegradeUnsupportedFeatures()
        {
            Debug.Log("[BleHidDemoController] Gracefully degrading unsupported features");
            
            // Mark all features as unavailable
            _mediaControlsAvailable = false;
            _mouseControlsAvailable = false;
            
            // Update UI to reflect unavailable features
            SetControlButtonsInteractable(false);
            
            // Disable advertising button
            if (advertiseButton != null)
            {
                advertiseButton.interactable = false;
                advertiseButton.GetComponentInChildren<Text>().text = "Not Supported";
            }
            
            // Visual indication of which features are unavailable
            if (mouseTouchpad != null && mouseTouchpad.touchpadBackground != null)
            {
                Color bgColor = mouseTouchpad.touchpadBackground.color;
                bgColor.a = 0.1f; // Very faded out to indicate unavailable
                mouseTouchpad.touchpadBackground.color = bgColor;
            }
            
            if (statusText != null)
            {
                statusText.text = "BLE peripheral mode not supported on this device";
            }
        }

        private void OnDestroy()
        {
            // Cancel any repeating invokes
            CancelInvoke();
            
            // Clean up event subscriptions
            if (bleManager != null)
            {
                bleManager.OnConnected -= OnDeviceConnected;
                bleManager.OnDisconnected -= OnDeviceDisconnected;
            }

            // Unsubscribe from touchpad events
            if (mouseTouchpad != null)
            {
                mouseTouchpad.OnMouseMove -= HandleMouseMovement;
            }
        }
    }
}
