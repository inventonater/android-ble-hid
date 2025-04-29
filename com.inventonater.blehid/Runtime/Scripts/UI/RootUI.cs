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
        private BleHidManager _bleHidManager;

        private void Start()
        {
            _bleHidManager = BleHidManager.Instance;
            var connectionBridge = _bleHidManager.ConnectionBridge;
            var mouseBridge = _bleHidManager.BleBridge.Mouse;
            var bleHidPermissionHandler = _bleHidManager.BleBridge.Permissions;
            var accessibilityServiceBridge = _bleHidManager.BleBridge.AccessibilityService;

            var javaBroadcaster = FindFirstObjectByType<JavaBroadcaster>();
            javaBroadcaster.OnInitializeComplete += OnInitializeComplete;
            javaBroadcaster.OnAdvertisingStateChanged += OnAdvertisingStateChanged;
            javaBroadcaster.OnConnectionStateChanged += OnConnectionStateChanged;
            javaBroadcaster.OnPairingStateChanged += OnPairingStateChanged;
            javaBroadcaster.OnError += OnError;
            javaBroadcaster.OnDebugLog += OnDebugLog;

            _statusUI = new StatusUI(connectionBridge);
            _permissionsUI = new PermissionsUI(bleHidPermissionHandler, accessibilityServiceBridge);

            AddTab(new MediaDeviceUI());
            AddTab(new MouseDeviceUI(mouseBridge));
            AddTab(new KeyboardUI());
            AddTab(new AccessibilityUI(accessibilityServiceBridge));
            AddTab(new ConnectionUI(connectionBridge, javaBroadcaster));
            AddTab(new IdentityUI(connectionBridge));
        }

        private void Update()
        {
            CurrentTab.Update();
        }

        private void OnGUI()
        {
            UIHelper.SetupGUIStyles();

            // Adjust layout to fill the screen
            float padding = 10;
            Rect layoutArea = new Rect(padding, padding, Screen.width - padding * 2, Screen.height - padding * 2);
            GUILayout.BeginArea(layoutArea);

            _permissionsUI.DrawIssues();
            _statusUI.DrawUI();

            if (!_bleHidManager.IsInitialized)
            {
                GUILayout.Space(5);
                GUILayout.Label("BleHidManager Not Initialized");
                GUILayout.Space(5);
            }
            else
            {
                var newTabIndex = GUILayout.Toolbar(_currentTabIndex, _activeTabNames, GUILayout.Height(60));
                if (newTabIndex != _currentTabIndex)
                {
                    CurrentTab.Hidden();
                    Logger.Log($"Tab '{CurrentTab.TabName}' deactivated");

                    _currentTabIndex = newTabIndex;

                    CurrentTab.Shown();
                    Logger.Log($"Tab '{CurrentTab.TabName}' activated");
                }

                if (CurrentTab.TabName == AccessibilityUI.Name) GUILayout.BeginVertical(GUI.skin.box); // No fixed height for Local tab
                else GUILayout.BeginVertical(GUI.skin.box, GUILayout.Height(Screen.height * 0.45f));
                DrawTabWithScroll(CurrentTab);
                GUILayout.EndVertical();
            }

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
            if (success) Logger.Log("BLE HID initialized successfully: " + message);
            else Logger.Error("BLE HID initialization failed: " + message);
        }

        private void OnAdvertisingStateChanged(bool advertising, string message)
        {
            if (advertising) Logger.Log("BLE advertising started: " + message);
            else Logger.Log("BLE advertising stopped: " + message);
        }

        private void OnConnectionStateChanged(bool connected, string deviceName, string deviceAddress)
        {
            if (connected) Logger.Log("Device connected: " + deviceName + " (" + deviceAddress + ")");
            else Logger.Log("Device disconnected");
        }

        private void OnPairingStateChanged(string status, string deviceAddress)
        {
            Logger.Log("Pairing state changed: " + status + (deviceAddress != null ? " (" + deviceAddress + ")" : ""));
        }

        private void OnError(int errorCode, string errorMessage)
        {
            Logger.Log("Error " + errorCode + ": " + errorMessage);
        }

        private void OnDebugLog(string message) => Logger.Log("Debug: " + message);
    }
}
