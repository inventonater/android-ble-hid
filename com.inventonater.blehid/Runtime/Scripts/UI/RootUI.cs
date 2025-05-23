using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace Inventonater
{
    public class RootUI : MonoBehaviour
    {
        private static LoggingManager Logger => LoggingManager.Instance;

        private StatusUI _statusUI;
        private PermissionsUI _permissionsUI;
        private BleHidClient _bleHidClient;
        private TabGroup _tabGroup;

        private void Start()
        {
            _bleHidClient = BleHidClient.Instance;
            var bindingSet = FindFirstObjectByType<BindingList>();
            var connectionBridge = _bleHidClient.ConnectionBridge;
            var mouseBridge = _bleHidClient.BleBridge.Mouse;
            var bleHidPermissionHandler = _bleHidClient.BleBridge.Permissions;
            var accessibilityServiceBridge = _bleHidClient.AccessibilityServiceBridge;

            var javaBroadcaster = FindFirstObjectByType<JavaBroadcaster>();
            javaBroadcaster.OnInitializeComplete += (success, message) =>
            {
                _statusUI.SetInitialized(success);
                Logger.Log($"BLE HID initialized {success}: {message}", isError: !success);
            };
            javaBroadcaster.OnAdvertisingStateChanged += (advertising, message) => Logger.Log($"BLE advertising {advertising}: " + message);
            javaBroadcaster.OnConnectionStateChanged += (connected, deviceName, deviceAddress) => Logger.Log($"Device connected {connected}: {deviceName} ({deviceAddress})");
            javaBroadcaster.OnPairingStateChanged += (status, deviceAddress) => Logger.Log($"Pairing state changed: {status} ({deviceAddress ?? ""})");
            javaBroadcaster.OnError += (errorCode, errorMessage) => Logger.Log("Error " + errorCode + ": " + errorMessage);
            javaBroadcaster.OnDebugLog += message => Logger.Log("Debug: " + message);

            _statusUI = new StatusUI(connectionBridge);
            _permissionsUI = new PermissionsUI(bleHidPermissionHandler, accessibilityServiceBridge);

            _tabGroup = new TabGroup("Main", new List<SectionUI>
            {
                new MappingSection(bindingSet),
                new TabGroup("Debug", new List<SectionUI>()
                {
                    new MediaDeviceUI(),
                    new MouseUI(mouseBridge),
                    new KeyboardUI(),
                    new AccessibilityUI(accessibilityServiceBridge),
                }),
                new TabGroup("Connectivity", new List<SectionUI>()
                {
                    new ConnectionUI(connectionBridge, javaBroadcaster),
                    new IdentityUI(connectionBridge)
                }),
            });
        }

        private void Update()
        {
            _tabGroup.Update();
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

            if (!_bleHidClient.IsInitialized)
            {
                GUILayout.Space(5);
                GUILayout.Label("BleHidManager Not Initialized");
                GUILayout.Space(5);
            }
            else { _tabGroup.DrawUI(); }

            Logger.DrawLogUI();
            GUILayout.EndArea();
        }
    }
}
