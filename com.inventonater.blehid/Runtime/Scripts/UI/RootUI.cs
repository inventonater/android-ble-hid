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
    public class RootUI : MonoBehaviour
    {
        private BleHidManager bleHidManager;

        // Dictionary to map tab names to their corresponding components
        private readonly List<SectionUI> tabComponents = new();
        void AddTab(SectionUI component) => tabComponents.Add(component);
        private int currentTabIndex = 0;
        private SectionUI CurrentTab => tabComponents[currentTabIndex];
        private IEnumerable<string> tabNames => tabComponents.Select(t => t.TabName);
        private bool isInitialized = false;

        private static bool IsEditorMode => Application.isEditor;
        private static LoggingManager Logger => LoggingManager.Instance;
        private StatusUI _statusUI;
        private MediaDeviceUI _media;
        private MouseDeviceUI _mouse;
        private KeyboardUI _keyboard;
        private AccessibilityUI _local;
        private PermissionsUI permissionsUI;
        private ConnectionUI _connectionUI;
        private IdentityUI _identity;
        private Vector2 localTabScrollPosition = Vector2.zero;

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

            _statusUI = new StatusUI();
            _statusUI.SetInitialized(isInitialized);
            
            // Initialize error component first to ensure accessibility UI appears from startup
            permissionsUI = new PermissionsUI(this);

            _media = new MediaDeviceUI();
            _mouse = new MouseDeviceUI();
            _keyboard = new KeyboardUI();
            _local = new AccessibilityUI(this);
            _connectionUI = new ConnectionUI();
            _identity = new IdentityUI();

            AddTab(_media);
            AddTab(_mouse);
            AddTab(_keyboard);
            AddTab(_local);
            AddTab(_connectionUI);
            AddTab(_identity);

            // Start initialization process
            StartCoroutine(bleHidManager.BleInitializer.Initialize());
            Logger.AddLogEntry("Starting BLE HID initialization...");

            permissionsUI.InitialCheck();
        }

        private void Update()
        {
            permissionsUI.Update();
            CurrentTab.Update();
        }

        private void OnGUI()
        {
            UIHelper.SetupGUIStyles();

            // Adjust layout to fill the screen
            float padding = 10;
            Rect layoutArea = new Rect(padding, padding, Screen.width - padding * 2, Screen.height - padding * 2);
            GUILayout.BeginArea(layoutArea);

            permissionsUI.DrawErrorWarnings();
            _statusUI.DrawUI();

            // If we have a permission error or accessibility error, don't show the rest of the UI
            if (permissionsUI.HasCriticalErrors())
            {
                GUILayout.EndArea();
                return;
            }

            var newTabIndex = GUILayout.Toolbar(currentTabIndex, tabNames.ToArray(), GUILayout.Height(60));
            if (newTabIndex != currentTabIndex)
            {
                CurrentTab.Hidden();
                Logger.AddLogEntry($"Tab '{CurrentTab.TabName}' deactivated");

                currentTabIndex = newTabIndex;

                CurrentTab.Shown();
                Logger.AddLogEntry($"Tab '{CurrentTab.TabName}' activated");
            }

            if (CurrentTab.TabName == AccessibilityUI.Name) GUILayout.BeginVertical(GUI.skin.box); // No fixed height for Local tab
            else GUILayout.BeginVertical(GUI.skin.box, GUILayout.Height(Screen.height * 0.45f));

            if (bleHidManager != null && (isInitialized || IsEditorMode)) DrawSelectedTab();

            GUILayout.EndVertical();

            Logger.DrawLogUI();
            GUILayout.EndArea();
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

        private void DrawTab(SectionUI tab)
        {
            GUI.enabled = bleHidManager.IsConnected || IsEditorMode;
            tab.DrawUI();
            GUI.enabled = true;
        }

        private void DrawMediaTab()
        {
            // Remote BLE controls need a connected device
            GUI.enabled = bleHidManager.IsConnected || IsEditorMode;
            _media.DrawUI();
            GUI.enabled = true;
        }

        private void DrawMouseTab()
        {
            // Remote BLE controls need a connected device
            GUI.enabled = bleHidManager.IsConnected || IsEditorMode;
            _mouse.DrawUI();
            GUI.enabled = true;
        }

        private void DrawKeyboardTab()
        {
            // Remote BLE controls need a connected device
            GUI.enabled = bleHidManager.IsConnected || IsEditorMode;
            _keyboard.DrawUI();
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

            _local.DrawUI();

            GUILayout.EndScrollView();
        }

        private void DrawConnectionParametersTab()
        {
            // Connection parameters need a connected device
            GUI.enabled = bleHidManager.IsConnected || IsEditorMode;
            _connectionUI.DrawUI();
            GUI.enabled = true;
        }
        
        private void DrawIdentityTab()
        {
            // Identity management is always enabled
            GUI.enabled = true;
            _identity.DrawUI();
        }

        private void OnInitializeComplete(bool success, string message)
        {
            isInitialized = success;
            _statusUI.SetInitialized(success);

            if (success) { Logger.AddLogEntry("BLE HID initialized successfully: " + message); }
            else
            {
                Logger.AddLogEntry("BLE HID initialization failed: " + message);

                // Check if this is a permission error
                if (message.Contains("permission")) { permissionsUI.SetPermissionError(message); }
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
                    permissionsUI.SetPermissionError(errorMessage);
                    break;
                case BleHidConstants.ERROR_ACCESSIBILITY_NOT_ENABLED:
                    permissionsUI.SetAccessibilityError(true);
                    Logger.AddLogEntry("Accessibility error: " + errorMessage);
                    permissionsUI.CheckAccessibilityServiceStatus();
                    break;
                case BleHidConstants.ERROR_NOTIFICATION_PERMISSION_NOT_GRANTED:
                    permissionsUI.SetNotificationPermissionError(true, errorMessage);
                    Logger.AddLogEntry("Notification permission error: " + errorMessage);
                    permissionsUI.CheckNotificationPermissionStatus();
                    break;
                default:
                    Logger.AddLogEntry("Error " + errorCode + ": " + errorMessage);
                    break;
            }
        }

        private void OnDebugLog(string message) => Logger.AddLogEntry("Debug: " + message);
    }
}
