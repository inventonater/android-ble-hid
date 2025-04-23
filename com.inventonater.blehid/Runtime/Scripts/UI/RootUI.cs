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
        private static LoggingManager Logger => LoggingManager.Instance;

        private readonly List<SectionUI> _tabComponents = new();
        private string[] _activeTabNames;
        void AddTab(SectionUI component)
        {
            _tabComponents.Add(component);
            _activeTabNames = _tabComponents.Select(t => t.TabName).ToArray();
        }

        private int _currentTabIndex = 0;
        private SectionUI CurrentTab => _tabComponents[_currentTabIndex];
        private StatusUI _statusUI;
        private PermissionsUI _permissionsUI;
        private Vector2 _localTabScrollPosition = Vector2.zero;

        private void Start()
        {
            var eventSystem = FindFirstObjectByType<BleEventSystem>();
            eventSystem.OnInitializeComplete += OnInitializeComplete;
            eventSystem.OnAdvertisingStateChanged += OnAdvertisingStateChanged;
            eventSystem.OnConnectionStateChanged += OnConnectionStateChanged;
            eventSystem.OnPairingStateChanged += OnPairingStateChanged;
            eventSystem.OnError += OnError;
            eventSystem.OnDebugLog += OnDebugLog;

            _statusUI = new StatusUI();
            _permissionsUI = new PermissionsUI(this);

            AddTab(new MediaDeviceUI());
            AddTab(new MouseDeviceUI());
            AddTab(new KeyboardUI());
            AddTab(new AccessibilityUI(this));
            AddTab(new ConnectionUI());
            AddTab(new IdentityUI());

            StartCoroutine(BleHidManager.Instance.BleInitializer.Initialize());
            Logger.AddLogEntry("Starting BLE HID initialization...");

            _permissionsUI.InitialCheck();
        }

        private void Update()
        {
            _permissionsUI.Update();
            CurrentTab.Update();
        }

        private void OnGUI()
        {
            UIHelper.SetupGUIStyles();

            // Adjust layout to fill the screen
            float padding = 10;
            Rect layoutArea = new Rect(padding, padding, Screen.width - padding * 2, Screen.height - padding * 2);
            GUILayout.BeginArea(layoutArea);

            _permissionsUI.DrawErrorWarnings();
            _statusUI.DrawUI();

            // If we have a permission error or accessibility error, don't show the rest of the UI
            if (_permissionsUI.HasCriticalErrors())
            {
                GUILayout.EndArea();
                return;
            }

            var newTabIndex = GUILayout.Toolbar(_currentTabIndex, _activeTabNames, GUILayout.Height(60));
            if (newTabIndex != _currentTabIndex)
            {
                CurrentTab.Hidden();
                Logger.AddLogEntry($"Tab '{CurrentTab.TabName}' deactivated");

                _currentTabIndex = newTabIndex;

                CurrentTab.Shown();
                Logger.AddLogEntry($"Tab '{CurrentTab.TabName}' activated");
            }

            if (CurrentTab.TabName == AccessibilityUI.Name) GUILayout.BeginVertical(GUI.skin.box); // No fixed height for Local tab
            else GUILayout.BeginVertical(GUI.skin.box, GUILayout.Height(Screen.height * 0.45f));

            DrawTabWithScroll(CurrentTab);

            GUILayout.EndVertical();

            Logger.DrawLogUI();
            GUILayout.EndArea();
        }

        private void DrawTab(SectionUI tab)
        {
            GUI.enabled = true;
            tab.DrawUI();
            GUI.enabled = true;
        }

        private void DrawTabWithScroll(SectionUI tab)
        {
            GUI.enabled = true;

            float viewHeight = Screen.height * 0.45f; // Maintain consistent view height
            _localTabScrollPosition = GUILayout.BeginScrollView(_localTabScrollPosition, GUILayout.MinHeight(viewHeight), GUILayout.ExpandHeight(true));

            tab.DrawUI();

            GUILayout.EndScrollView();

            GUI.enabled = true;
        }

        private void OnInitializeComplete(bool success, string message)
        {
            _statusUI.SetInitialized(success);

            if (success) { Logger.AddLogEntry("BLE HID initialized successfully: " + message); }
            else
            {
                Logger.AddLogEntry("BLE HID initialization failed: " + message);

                // Check if this is a permission error
                if (message.Contains("permission")) { _permissionsUI.SetPermissionError(message); }
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
                    _permissionsUI.SetPermissionError(errorMessage);
                    break;
                case BleHidConstants.ERROR_ACCESSIBILITY_NOT_ENABLED:
                    _permissionsUI.SetAccessibilityError(true);
                    Logger.AddLogEntry("Accessibility error: " + errorMessage);
                    _permissionsUI.CheckAccessibilityServiceStatus();
                    break;
                default:
                    Logger.AddLogEntry("Error " + errorCode + ": " + errorMessage);
                    break;
            }
        }

        private void OnDebugLog(string message) => Logger.AddLogEntry("Debug: " + message);
    }
}
