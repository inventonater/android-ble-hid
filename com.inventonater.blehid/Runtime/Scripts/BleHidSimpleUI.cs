using System;
using System.Collections;
using UnityEngine;
using Inventonater.BleHid.UI;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Simple immediate mode UI for controlling BLE HID functionality.
    /// This script combines features (media, mouse, keyboard, local) into a modular component-based system
    /// using Unity's OnGUI system for reliable touch input handling.
    /// </summary>
    public class BleHidSimpleUI : MonoBehaviour
    {
    private BleHidManager bleHidManager;
    private int currentTab = 0;
    private string[] tabNames = new string[] { "Media", "Mouse", "Keyboard", "Local", "Connection" };
    private bool isInitialized = false;
    
    // Flag to enable UI in editor even without full BLE functionality
    private bool isEditorMode = false;
    
    // UI Components
    private LoggingManager logger;
    private StatusComponent statusComponent;
    private MediaControlsComponent mediaComponent;
    private MouseControlsComponent mouseComponent;
    private KeyboardControlsComponent keyboardComponent;
    private LocalControlComponent localComponent;
    private ErrorHandlingComponent errorComponent;
    private ConnectionParametersComponent connectionParametersComponent;
    
    // Scroll position for Local tab
    private Vector2 localTabScrollPosition = Vector2.zero;
    
    // Permission checking
    private float nextPermissionCheckTime = 0f;
    private const float PERMISSION_CHECK_INTERVAL = 3.0f; // Check every 3 seconds
    
    // Application focus tracking
    private bool wasInBackground = false;
        
        // Track if we've attempted to initialize local control
        private bool localControlInitialized = false;

        private void Start()
        {
            // Check if running in the Unity Editor
            #if UNITY_EDITOR
            isEditorMode = true;
            isInitialized = true; // Auto-initialize in editor
            #endif

            // Create and initialize managers
            InitializeManagers();
            
            // Initialize UI components
            InitializeUIComponents();
            
            // Register for events
            RegisterEvents();
            
            // Initialize BLE HID
            StartCoroutine(bleHidManager.Initialize());
            
            // Initialize BleHidLocalControl for Android
            #if UNITY_ANDROID && !UNITY_EDITOR
            if (localComponent != null)
            {
                localComponent.SetMonoBehaviourOwner(this);
            }
            #endif

            // Add log message
            logger.AddLogEntry("Starting BLE HID initialization...");
            
            // Check permissions and accessibility service on startup (Android only)
            #if UNITY_ANDROID && !UNITY_EDITOR
            errorComponent.CheckMissingPermissions();
            errorComponent.CheckAccessibilityServiceStatus();
            #endif
        }

        private void InitializeManagers()
        {
            // Create logging manager
            logger = new LoggingManager();
            
            // Create BleHidManager
            GameObject managerObj = new GameObject("BleHidManager");
            bleHidManager = managerObj.AddComponent<BleHidManager>();
            
            if (isEditorMode)
            {
                logger.AddLogEntry("Running in Editor mode - BLE functionality limited");
            }
        }
        
        private void InitializeUIComponents()
        {
            // Create components
            statusComponent = new StatusComponent();
            mediaComponent = new MediaControlsComponent();
            mouseComponent = new MouseControlsComponent();
            keyboardComponent = new KeyboardControlsComponent();
            localComponent = new LocalControlComponent();
            errorComponent = new ErrorHandlingComponent();
            connectionParametersComponent = new ConnectionParametersComponent();
            
            // Initialize components
            statusComponent.Initialize(bleHidManager, logger, isEditorMode);
            mediaComponent.Initialize(bleHidManager, logger, isEditorMode);
            mouseComponent.Initialize(bleHidManager, logger, isEditorMode);
            keyboardComponent.Initialize(bleHidManager, logger, isEditorMode);
            localComponent.Initialize(bleHidManager, logger, isEditorMode);
            errorComponent.Initialize(bleHidManager, logger, isEditorMode);
            connectionParametersComponent.Initialize(bleHidManager, logger, isEditorMode);
            
            // Additional setup for components that need MonoBehaviour reference
            errorComponent.SetMonoBehaviourOwner(this);
            localComponent.SetMonoBehaviourOwner(this);
            
            // Set initial state
            statusComponent.SetInitialized(isInitialized);
        }
        
        private void RegisterEvents()
        {
            if (bleHidManager != null)
            {
                bleHidManager.OnInitializeComplete += OnInitializeComplete;
                bleHidManager.OnAdvertisingStateChanged += OnAdvertisingStateChanged;
                bleHidManager.OnConnectionStateChanged += OnConnectionStateChanged;
                bleHidManager.OnPairingStateChanged += OnPairingStateChanged;
                bleHidManager.OnError += OnError;
                bleHidManager.OnDebugLog += OnDebugLog;
            }
        }

        private void Update()
        {
            // Handle update logic in the active component
            if (currentTab == 1) // Mouse tab
            {
                mouseComponent.Update();
            }
            
            // Periodic permission checking if needed
            #if UNITY_ANDROID && !UNITY_EDITOR
            // Check if we need to check permissions
            if ((errorComponent.HasPermissionError || errorComponent.HasAccessibilityError) && 
                Time.time >= nextPermissionCheckTime)
            {
                // Schedule next check
                nextPermissionCheckTime = Time.time + PERMISSION_CHECK_INTERVAL;
                
                // Check permissions
                if (errorComponent.HasPermissionError)
                {
                    errorComponent.CheckMissingPermissions();
                    logger.AddLogEntry("Periodic permission check");
                }
                
                // Check accessibility service
                if (errorComponent.HasAccessibilityError)
                {
                    errorComponent.CheckAccessibilityServiceStatus();
                    logger.AddLogEntry("Periodic accessibility check");
                }
            }
            #endif
        }

        private void OnDestroy()
        {
            // Unregister events to prevent memory leaks
            if (bleHidManager != null)
            {
                bleHidManager.OnInitializeComplete -= OnInitializeComplete;
                bleHidManager.OnAdvertisingStateChanged -= OnAdvertisingStateChanged;
                bleHidManager.OnConnectionStateChanged -= OnConnectionStateChanged;
                bleHidManager.OnPairingStateChanged -= OnPairingStateChanged;
                bleHidManager.OnError -= OnError;
                bleHidManager.OnDebugLog -= OnDebugLog;
            }
        }

        private void OnGUI()
        {
            // Set up GUI style for better touch targets
            GUI.skin.button.fontSize = 24;
            GUI.skin.label.fontSize = 20;
            GUI.skin.textField.fontSize = 20;
            GUI.skin.box.fontSize = 20;

            // Adjust layout to fill the screen
            float padding = 10;
            Rect layoutArea = new Rect(padding, padding, Screen.width - padding * 2, Screen.height - padding * 2);
            GUILayout.BeginArea(layoutArea);

            // Permission error warning - show at the top with a red background
            if (errorComponent.HasPermissionError)
            {
                errorComponent.DrawPermissionErrorUI();
                GUILayout.Space(20);
            }
            
            // Accessibility error - always show full UI at the top with other permissions
            if (errorComponent.HasAccessibilityError)
            {
                errorComponent.DrawAccessibilityErrorUI(true); // Show full UI with button
                GUILayout.Space(20);
            }

            // Status area
            statusComponent.DrawUI();

            // If we have a permission error or accessibility error, don't show the rest of the UI
            // Treat accessibility service as a critical requirement like other permissions
            if (errorComponent.HasPermissionError || errorComponent.HasAccessibilityError)
            {
                GUILayout.EndArea();
                return;
            }

            // Tab selection
            currentTab = GUILayout.Toolbar(currentTab, tabNames, GUILayout.Height(60));

            // Tab content - use flexible height for Local tab
            if (currentTab == 3) // Local tab
            {
                GUILayout.BeginVertical(GUI.skin.box); // No fixed height for Local tab
            }
            else
            {
                GUILayout.BeginVertical(GUI.skin.box, GUILayout.Height(Screen.height * 0.45f));
            }

            // Check if BLE HID is initialized and a device is connected (or in editor mode)
            if (bleHidManager != null && (isInitialized || isEditorMode))
            {
                switch (currentTab)
                {
                    case 0: // Media tab
                        // Remote BLE controls need a connected device
                        GUI.enabled = bleHidManager.IsConnected || isEditorMode;
                        mediaComponent.DrawUI();
                        GUI.enabled = true;
                        break;
                    case 1: // Mouse tab
                        // Remote BLE controls need a connected device
                        GUI.enabled = bleHidManager.IsConnected || isEditorMode;
                        mouseComponent.DrawUI();
                        GUI.enabled = true;
                        break;
                    case 2: // Keyboard tab
                        // Remote BLE controls need a connected device
                        GUI.enabled = bleHidManager.IsConnected || isEditorMode;
                        keyboardComponent.DrawUI();
                        GUI.enabled = true;
                        break;
                    case 3: // Local Control tab
                        // Local controls always enabled since they don't rely on a BLE connection
                        GUI.enabled = true;
                        
                        // Always show the Local Control UI
                        // The accessibility error is now displayed at the top of the screen
                        
                        // Wrap local controls in a scroll view
                        float viewHeight = Screen.height * 0.45f; // Maintain consistent view height
                        localTabScrollPosition = GUILayout.BeginScrollView(
                            localTabScrollPosition, 
                            GUILayout.MinHeight(viewHeight), 
                            GUILayout.ExpandHeight(true)
                        );
                        
                        localComponent.DrawUI();
                        
                        GUILayout.EndScrollView();
                        break;
                    case 4: // Connection Parameters tab
                        // Connection parameters need a connected device
                        GUI.enabled = bleHidManager.IsConnected || isEditorMode;
                        connectionParametersComponent.DrawUI();
                        GUI.enabled = true;
                        break;
                }
            }
            else
            {
                GUILayout.Label("Initializing BLE HID...");
            }

            GUILayout.EndVertical();

            // Log area
            logger.DrawLogUI();

            GUILayout.EndArea();
        }

        private void OnInitializeComplete(bool success, string message)
        {
            isInitialized = success;
            statusComponent.SetInitialized(success);

            if (success)
            {
                logger.AddLogEntry("BLE HID initialized successfully: " + message);
            }
            else
            {
                logger.AddLogEntry("BLE HID initialization failed: " + message);
                
                // Check if this is a permission error
                if (message.Contains("permission"))
                {
                    errorComponent.SetPermissionError(message);
                }
            }
        }

        private void OnAdvertisingStateChanged(bool advertising, string message)
        {
            if (advertising)
            {
                logger.AddLogEntry("BLE advertising started: " + message);
            }
            else
            {
                logger.AddLogEntry("BLE advertising stopped: " + message);
            }
        }

        private void OnConnectionStateChanged(bool connected, string deviceName, string deviceAddress)
        {
            if (connected)
            {
                logger.AddLogEntry("Device connected: " + deviceName + " (" + deviceAddress + ")");
            }
            else
            {
                logger.AddLogEntry("Device disconnected");
            }
        }

        private void OnPairingStateChanged(string status, string deviceAddress)
        {
            logger.AddLogEntry("Pairing state changed: " + status + (deviceAddress != null ? " (" + deviceAddress + ")" : ""));
        }

        private void OnError(int errorCode, string errorMessage)
        {
            // Check for permission error
            if (errorCode == BleHidConstants.ERROR_PERMISSIONS_NOT_GRANTED)
            {
                errorComponent.SetPermissionError(errorMessage);
            }
            else if (errorCode == BleHidConstants.ERROR_ACCESSIBILITY_NOT_ENABLED)
            {
                errorComponent.SetAccessibilityError(true);
                logger.AddLogEntry("Accessibility error: " + errorMessage);
                
                // Check accessibility service status
                errorComponent.CheckAccessibilityServiceStatus();
            }
            else
            {
                logger.AddLogEntry("Error " + errorCode + ": " + errorMessage);
            }
        }

        private void OnDebugLog(string message)
        {
            logger.AddLogEntry("Debug: " + message);
        }
        
        // Handle application focus and pause to detect when user returns from Android settings
        private void OnApplicationFocus(bool hasFocus)
        {
            if (hasFocus && wasInBackground)
            {
                // App has regained focus after being in background
                logger.AddLogEntry("Application regained focus");
                wasInBackground = false;
                
                #if UNITY_ANDROID && !UNITY_EDITOR
                // Check if we need to reinitialize the local control
                StartCoroutine(HandleApplicationFocusGained());
                #endif
            }
        }
        
        private void OnApplicationPause(bool isPaused)
        {
            // App was paused (e.g., user went to settings)
            if (isPaused)
            {
                logger.AddLogEntry("Application paused");
                wasInBackground = true;
            }
        }
        
        // Handle app returning from background (e.g., returning from accessibility settings)
        private IEnumerator HandleApplicationFocusGained()
        {
            // Wait a short delay for Android to settle
            yield return new WaitForSeconds(0.5f);
            
            logger.AddLogEntry("Checking accessibility status after focus gained");
            
            // Use the direct check method to see if accessibility service was enabled
            bool isAccessibilityEnabled = BleHidLocalControl.CheckAccessibilityServiceEnabledDirect();
            logger.AddLogEntry($"Direct accessibility check: {(isAccessibilityEnabled ? "ENABLED" : "NOT ENABLED")}");
            
            // Update UI if accessibility status has changed
            if (isAccessibilityEnabled && errorComponent.HasAccessibilityError)
            {
                logger.AddLogEntry("Accessibility service was enabled in settings, updating UI");
                errorComponent.SetAccessibilityError(false);
            }
            else if (!isAccessibilityEnabled && errorComponent.HasAccessibilityError)
            {
                // Still not enabled, reinitialize local control and recheck
                logger.AddLogEntry("Reinitializing local control after returning from settings");
                StartCoroutine(BleHidLocalControl.ReinitializeAfterFocusGained(this));
                
                // Extra delay and then check accessibility status again
                yield return new WaitForSeconds(1.0f);
                errorComponent.CheckAccessibilityServiceStatus();
            }
            
            // Re-check permissions too
            if (errorComponent.HasPermissionError)
            {
                logger.AddLogEntry("Checking permissions after focus gained");
                errorComponent.CheckMissingPermissions();
            }
        }
    }
}
