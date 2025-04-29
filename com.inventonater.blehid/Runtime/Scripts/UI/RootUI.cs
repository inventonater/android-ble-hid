using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class RootUI : MonoBehaviour
    {
        private static LoggingManager Logger => LoggingManager.Instance;

        private StatusUI _statusUI;
        private PermissionsUI _permissionsUI;
        private BleHidManager _bleHidManager;
        private SectionGroup _sectionGroup;

        private void Start()
        {
            _bleHidManager = BleHidManager.Instance;
            var connectionBridge = _bleHidManager.ConnectionBridge;
            var mouseBridge = _bleHidManager.BleBridge.Mouse;
            var bleHidPermissionHandler = _bleHidManager.BleBridge.Permissions;
            var accessibilityServiceBridge = _bleHidManager.BleBridge.AccessibilityService;

            var javaBroadcaster = FindFirstObjectByType<JavaBroadcaster>();
            javaBroadcaster.OnInitializeComplete += (success, message) =>
            {
                _statusUI.SetInitialized(success);
                if (success) Logger.Log("BLE HID initialized successfully: " + message);
                else Logger.Error("BLE HID initialization failed: " + message);
            };
            javaBroadcaster.OnAdvertisingStateChanged += (advertising, message) =>
            {
                if (advertising) Logger.Log("BLE advertising started: " + message);
                else Logger.Log("BLE advertising stopped: " + message);
            };
            javaBroadcaster.OnConnectionStateChanged += (connected, deviceName, deviceAddress) =>
            {
                if (connected) Logger.Log("Device connected: " + deviceName + " (" + deviceAddress + ")");
                else Logger.Log("Device disconnected");
            };
            javaBroadcaster.OnPairingStateChanged += (status, deviceAddress) =>
                Logger.Log("Pairing state changed: " + status + (deviceAddress != null ? " (" + deviceAddress + ")" : ""));
            javaBroadcaster.OnError += (errorCode, errorMessage) => Logger.Log("Error " + errorCode + ": " + errorMessage);
            javaBroadcaster.OnDebugLog += message => Logger.Log("Debug: " + message);

            _statusUI = new StatusUI(connectionBridge);
            _permissionsUI = new PermissionsUI(bleHidPermissionHandler, accessibilityServiceBridge);

            var debug = new SectionGroup("Debug", new List<SectionUI>()
            {
                new MediaDeviceUI(),
                new MouseDeviceUI(mouseBridge),
                new KeyboardUI(),
                new AccessibilityUI(accessibilityServiceBridge),
            });

            var connectivity = new SectionGroup("Connectivity", new List<SectionUI>()
            {
                new ConnectionUI(connectionBridge, javaBroadcaster),
                new IdentityUI(connectionBridge)
            });

            var sections = new List<SectionUI>
            {
                debug,
                connectivity,
            };
            _sectionGroup = new SectionGroup("Main", sections);
        }

        private void Update()
        {
            _sectionGroup.Update();
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
            else { _sectionGroup.DrawUI(); }

            Logger.DrawLogUI();
            GUILayout.EndArea();
        }
    }
}
