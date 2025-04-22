using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Primary control panel interface for BLE HID functionality.
    /// This component manages all UI components and their interactions,
    /// organizing features (media, mouse, keyboard, local) into a modular
    /// component-based system using Unity's OnGUI for reliable touch input handling.
    /// </summary>
    public class BleHidControlPanel : MonoBehaviour
    {
        private BleHidManager bleHidManager;

        // Dictionary to map tab names to their corresponding components
        private readonly List<UIComponent> tabComponents = new();
        void AddTab(UIComponent component) => tabComponents.Add(component);
        private int currentTabIndex = 0;
        private UIComponent currentTabComponent => tabComponents[currentTabIndex];
        private IEnumerable<string> tabNames => tabComponents.Select(t => t.TabName);
        private bool isInitialized = false;

        private static bool IsEditorMode => Application.isEditor;
        private static LoggingManager Logger => LoggingManager.Instance;
        private StatusComponent statusComponent;
        private MediaControlsComponent mediaComponent;
        private MouseControlsComponent mouseComponent;
        private KeyboardControlsComponent keyboardComponent;
        private LocalControlComponent localComponent;
        private ErrorHandlingComponent errorComponent;
        private ConnectionParametersComponent connectionParametersComponent;
        private IdentityManagerComponent identityComponent;

        private Vector2 localTabScrollPosition = Vector2.zero;
        private float nextPermissionCheckTime = 0f;

        private void Start()
        {
            // Check if running in the Unity Editor
            if (IsEditorMode) isInitialized = true; // Auto-initialize in editor

            bleHidManager = FindFirstObjectByType<BleHidManager>();
            bleHidManager.BleEventSystem.OnInitializeComplete += OnInitializeComplete;
            bleHidManager.BleEventSystem.OnAdvertisingStateChanged += OnAdvertisingStateChanged;
            bleHidManager.BleEventSystem.OnConnectionStateChanged += OnConnectionStateChanged;
            bleHidManager.BleEventSystem.OnPairingStateChanged += OnPairingStateChanged;
            bleHidManager.BleEventSystem.OnError += OnError;
            bleHidManager.BleEventSystem.OnDebugLog += OnDebugLog;

            statusComponent = new StatusComponent();
            statusComponent.SetInitialized(isInitialized);
            
            // Initialize error component first to ensure accessibility UI appears from startup
            errorComponent = new ErrorHandlingComponent(this);

            mediaComponent = new MediaControlsComponent();
            mouseComponent = new MouseControlsComponent();
            keyboardComponent = new KeyboardControlsComponent();
            localComponent = new LocalControlComponent(this);
            connectionParametersComponent = new ConnectionParametersComponent();
            identityComponent = new IdentityManagerComponent();

            AddTab(mediaComponent);
            AddTab(mouseComponent);
            AddTab(keyboardComponent);
            AddTab(localComponent);
            AddTab(connectionParametersComponent);
            AddTab(identityComponent);

            // Start initialization process
            StartCoroutine(bleHidManager.BleInitializer.Initialize());
            Logger.AddLogEntry("Starting BLE HID initialization...");
            
            // Check all permissions - accessibility status is already being checked in the constructor
            errorComponent.CheckMissingPermissions();
            errorComponent.CheckNotificationPermissionStatus();
            
            // Perform an extra check to ensure the accessibility service status is detected
            // This helps on devices where the first check might not be reliable
            StartCoroutine(DelayedAccessibilityCheck(1.0f));
        }
        
        /// <summary>
        /// Perform a delayed check of the accessibility service status
        /// </summary>
        private IEnumerator DelayedAccessibilityCheck(float delaySeconds)
        {
            yield return new WaitForSeconds(delaySeconds);
            errorComponent.CheckAccessibilityServiceStatus();
            Logger.AddLogEntry("Performing startup accessibility service check");
        }

        private void Update()
        {
            currentTabComponent.Update();
            PerformPeriodicPermissionChecks();
        }

        private const float PERMISSION_CHECK_INTERVAL = 3.0f; // Check every 3 seconds

        private void PerformPeriodicPermissionChecks()
        {
            // Check if we need to check permissions
            if (!errorComponent.HasPermissionError &&
                !errorComponent.HasAccessibilityError &&
                !errorComponent.HasNotificationPermissionError) return;
                
            if (Time.time < nextPermissionCheckTime) return;

            // Schedule next check
            nextPermissionCheckTime = Time.time + PERMISSION_CHECK_INTERVAL;

            // Check permissions
            if (errorComponent.HasPermissionError)
            {
                errorComponent.CheckMissingPermissions();
                LoggingManager.Instance.AddLogEntry("Periodic permission check");
            }

            // Check accessibility service
            if (errorComponent.HasAccessibilityError)
            {
                errorComponent.CheckAccessibilityServiceStatus();
                LoggingManager.Instance.AddLogEntry("Periodic accessibility check");
            }
            
            // Check notification permission
            if (errorComponent.HasNotificationPermissionError)
            {
                errorComponent.CheckNotificationPermissionStatus();
                LoggingManager.Instance.AddLogEntry("Periodic notification permission check");
            }
        }

        private void OnGUI()
        {
            SetupGUIStyles();
            DrawLayoutArea();
        }

        private void SetupGUIStyles()
        {
            // Set up GUI style for better touch targets
            GUI.skin.button.fontSize = 24;
            GUI.skin.label.fontSize = 20;
            GUI.skin.textField.fontSize = 20;
            GUI.skin.box.fontSize = 20;
        }

        private void DrawLayoutArea()
        {
            // Adjust layout to fill the screen
            float padding = 10;
            Rect layoutArea = new Rect(padding, padding, Screen.width - padding * 2, Screen.height - padding * 2);
            GUILayout.BeginArea(layoutArea);

            DrawErrorWarnings();
            DrawStatusArea();

            // If we have a permission error or accessibility error, don't show the rest of the UI
            if (HasCriticalErrors())
            {
                GUILayout.EndArea();
                return;
            }

            var newTabIndex = GUILayout.Toolbar(currentTabIndex, tabNames.ToArray(), GUILayout.Height(60));
            if (newTabIndex != currentTabIndex)
            {
                currentTabComponent.ComponentHidden();
                Logger.AddLogEntry($"Tab '{currentTabComponent.TabName}' deactivated");

                currentTabIndex = newTabIndex;

                currentTabComponent.ComponentShown();
                Logger.AddLogEntry($"Tab '{currentTabComponent.TabName}' activated");
            }

            if (currentTabComponent.TabName == LocalControlComponent.Name) GUILayout.BeginVertical(GUI.skin.box); // No fixed height for Local tab
            else GUILayout.BeginVertical(GUI.skin.box, GUILayout.Height(Screen.height * 0.45f));

            if (bleHidManager != null && (isInitialized || IsEditorMode)) DrawSelectedTab();

            GUILayout.EndVertical();

            Logger.DrawLogUI();
            GUILayout.EndArea();
        }

        private void DrawErrorWarnings()
        {
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
            
            // Notification permission error - show at the top but don't block functionality
            if (errorComponent.HasNotificationPermissionError)
            {
                errorComponent.DrawNotificationPermissionErrorUI();
                GUILayout.Space(20);
            }
        }

        private void DrawStatusArea()
        {
            // Status area
            statusComponent.DrawUI();
        }

        private bool HasCriticalErrors()
        {
            // Only treat regular permissions as blocking errors
            // Accessibility errors are shown at the top but don't block the UI completely
            return errorComponent.HasPermissionError;
        }

        private void DrawSelectedTab()
        {
            switch (currentTabIndex)
            {
                case 0: // Media tab
                    DrawMediaTab();
                    break;
                case 1: // Mouse tab
                    DrawMouseTab();
                    break;
                case 2: // Keyboard tab
                    DrawKeyboardTab();
                    break;
                case 3: // Local Control tab
                    DrawLocalControlTab();
                    break;
                case 4: // Connection Parameters tab
                    DrawConnectionParametersTab();
                    break;
                case 5: // Identity tab
                    DrawIdentityTab();
                    break;
            }
        }

        private void DrawTab(UIComponent tab)
        {
            GUI.enabled = bleHidManager.IsConnected || IsEditorMode;
            tab.DrawUI();
            GUI.enabled = true;
        }

        private void DrawMediaTab()
        {
            // Remote BLE controls need a connected device
            GUI.enabled = bleHidManager.IsConnected || IsEditorMode;
            mediaComponent.DrawUI();
            GUI.enabled = true;
        }

        private void DrawMouseTab()
        {
            // Remote BLE controls need a connected device
            GUI.enabled = bleHidManager.IsConnected || IsEditorMode;
            mouseComponent.DrawUI();
            GUI.enabled = true;
        }

        private void DrawKeyboardTab()
        {
            // Remote BLE controls need a connected device
            GUI.enabled = bleHidManager.IsConnected || IsEditorMode;
            keyboardComponent.DrawUI();
            GUI.enabled = true;
        }

        private void DrawLocalControlTab()
        {
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
        }

        private void DrawConnectionParametersTab()
        {
            // Connection parameters need a connected device
            GUI.enabled = bleHidManager.IsConnected || IsEditorMode;
            connectionParametersComponent.DrawUI();
            GUI.enabled = true;
        }
        
        private void DrawIdentityTab()
        {
            // Identity management is always enabled
            GUI.enabled = true;
            identityComponent.DrawUI();
        }

        private void OnInitializeComplete(bool success, string message)
        {
            isInitialized = success;
            statusComponent.SetInitialized(success);

            if (success) { Logger.AddLogEntry("BLE HID initialized successfully: " + message); }
            else
            {
                Logger.AddLogEntry("BLE HID initialization failed: " + message);

                // Check if this is a permission error
                if (message.Contains("permission")) { errorComponent.SetPermissionError(message); }
            }
        }

        private void OnAdvertisingStateChanged(bool advertising, string message)
        {
            if (advertising) Logger.AddLogEntry("BLE advertising started: " + message);
            else Logger.AddLogEntry("BLE advertising stopped: " + message);
        }

        private void OnConnectionStateChanged(bool connected, string deviceName, string deviceAddress)
        {
            if (connected) Logger.AddLogEntry("Device connected: " + deviceName + " (" + deviceAddress + ")");
            else Logger.AddLogEntry("Device disconnected");
        }

        private void OnPairingStateChanged(string status, string deviceAddress)
        {
            Logger.AddLogEntry("Pairing state changed: " + status + (deviceAddress != null ? " (" + deviceAddress + ")" : ""));
        }

        private void OnError(int errorCode, string errorMessage)
        {
            switch (errorCode)
            {
                case BleHidConstants.ERROR_PERMISSIONS_NOT_GRANTED:
                    errorComponent.SetPermissionError(errorMessage);
                    break;
                case BleHidConstants.ERROR_ACCESSIBILITY_NOT_ENABLED:
                    errorComponent.SetAccessibilityError(true);
                    Logger.AddLogEntry("Accessibility error: " + errorMessage);
                    errorComponent.CheckAccessibilityServiceStatus();
                    break;
                case BleHidConstants.ERROR_NOTIFICATION_PERMISSION_NOT_GRANTED:
                    errorComponent.SetNotificationPermissionError(true, errorMessage);
                    Logger.AddLogEntry("Notification permission error: " + errorMessage);
                    errorComponent.CheckNotificationPermissionStatus();
                    break;
                default:
                    Logger.AddLogEntry("Error " + errorCode + ": " + errorMessage);
                    break;
            }
        }

        private void OnDebugLog(string message) => Logger.AddLogEntry("Debug: " + message);
    }
}
