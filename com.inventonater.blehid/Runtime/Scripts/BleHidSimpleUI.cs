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
        private string[] tabNames = new string[] { "Media", "Mouse", "Keyboard", "Local" };
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
            
            // Check permissions on startup (Android only)
            #if UNITY_ANDROID && !UNITY_EDITOR
            errorComponent.CheckMissingPermissions();
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
            
            // Initialize components
            statusComponent.Initialize(bleHidManager, logger, isEditorMode);
            mediaComponent.Initialize(bleHidManager, logger, isEditorMode);
            mouseComponent.Initialize(bleHidManager, logger, isEditorMode);
            keyboardComponent.Initialize(bleHidManager, logger, isEditorMode);
            localComponent.Initialize(bleHidManager, logger, isEditorMode);
            errorComponent.Initialize(bleHidManager, logger, isEditorMode);
            
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
            
            // Accessibility error - show if we're not in the Local tab
            if (errorComponent.HasAccessibilityError && currentTab != 3)
            {
                errorComponent.DrawAccessibilityErrorUI(false); // Simple notification when not on Local tab
                GUILayout.Space(20);
            }

            // Status area
            statusComponent.DrawUI();

            // If we have a permission error, don't show the rest of the UI
            if (errorComponent.HasPermissionError)
            {
                GUILayout.EndArea();
                return;
            }

            // Tab selection
            currentTab = GUILayout.Toolbar(currentTab, tabNames, GUILayout.Height(60));

            // Tab content
            GUILayout.BeginVertical(GUI.skin.box, GUILayout.Height(Screen.height * 0.45f));

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
                        
                        // If we have an accessibility error, show that UI
                        if (errorComponent.HasAccessibilityError && !isEditorMode)
                        {
                            errorComponent.DrawAccessibilityErrorUI(true);
                        }
                        else
                        {
                            localComponent.DrawUI();
                        }
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
    }
}
