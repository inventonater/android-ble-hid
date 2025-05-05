using System.Collections.Generic;
using UnityEngine;

namespace Inventonater
{
    public class IdentityUI : SectionUI
    {
        public ConnectionBridge ConnectionBridge { get; }
        public const string Name = "Identity";
        public override string TabName => Name;

        private string _deviceUuid = string.Empty;
        private string _creationDate = string.Empty;
        private string _deviceName = string.Empty;
        private string _newDeviceName = string.Empty;
        private List<Dictionary<string, string>> _pairedDevices = new();
        private Vector2 _deviceListScrollPosition = Vector2.zero;
        private string _deviceToForget = string.Empty;

        public IdentityUI(ConnectionBridge connectionBridge) => ConnectionBridge = connectionBridge;

        private float _lastRefresh;
        public override void Update()
        {
            if (Time.time < _lastRefresh + 3) return;
            _lastRefresh = Time.time;

            _pairedDevices = ConnectionBridge.GetBondedDevices();
            _deviceUuid = ConnectionBridge.GetOrCreateDeviceUuid();

            string creationDate = ConnectionBridge.GetIdentityCreationDate();
            if (creationDate == "Unknown") _creationDate = "Creation date: Unknown";
            else
            {
                try
                {
                    System.DateTime dt = System.DateTime.Parse(creationDate);
                    _creationDate = "Created: " + dt.ToString("g");
                }
                catch { _creationDate = "Created: " + creationDate; }
            }

            _deviceName = ConnectionBridge.GetDeviceName();
            _newDeviceName = _deviceName;
        }

        public override void DrawUI()
        {
            UIHelper.BeginSection("Device Identity");
            DeviceNameSection();
            GUILayout.Space(10);
            ResetIdentitySection();
            GUILayout.Space(10);
            PairedDevicesSection();
            UIHelper.EndSection();
        }

        private void DeviceNameSection()
        {
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Device UUID:", UIHelper.BoldStyle);
            GUILayout.Label(_deviceUuid, GUILayout.ExpandWidth(true));
            if (!string.IsNullOrEmpty(_creationDate)) GUILayout.Label(_creationDate);
            GUILayout.EndVertical();

            GUILayout.Space(10);

            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Device Name:", UIHelper.BoldStyle);

            // Create a custom style for the text field with larger height
            GUIStyle largeTextFieldStyle = new GUIStyle(GUI.skin.textField);
            largeTextFieldStyle.fontSize = GUI.skin.textField.fontSize;
            largeTextFieldStyle.fixedHeight = 60f; // Match standard button height

            {
                GUILayout.BeginHorizontal();
                _newDeviceName = GUILayout.TextField(_newDeviceName, largeTextFieldStyle, GUILayout.ExpandWidth(true));
                if (GUILayout.Button("Save", GUILayout.Width(100), GUILayout.Height(60)))
                {
                    string newName = _newDeviceName.Trim();
                    if (string.IsNullOrEmpty(newName))
                    {
                        // Revert to existing name
                        _newDeviceName = _deviceName;
                    }
                    else
                    {
                        bool success = ConnectionBridge.SetDeviceName(newName);
                        if (success) _deviceName = newName;
                    }
                }

                GUILayout.EndHorizontal();
            }

            GUILayout.EndVertical();
        }

        private void ResetIdentitySection()
        {
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Reset Device Identity:", UIHelper.BoldStyle);
            GUILayout.Label("This will generate a new UUID and require re-pairing with all devices.", new GUIStyle(GUI.skin.label) { wordWrap = true });
            if (GUILayout.Button("Reset Identity", GUILayout.Height(50))) ConnectionBridge.ResetIdentity();
            GUILayout.EndVertical();
        }

        private void PairedDevicesSection()
        {
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Paired Previously on this Phone:", UIHelper.BoldStyle);

            if (_pairedDevices.Count == 0) { GUILayout.Label("No paired devices found."); }
            else
            {
                // Calculate a better height for the scroll view - make it proportional to screen height
                float scrollViewHeight = Mathf.Max(Screen.height * 0.3f, 300);

                // Create a custom style for the vertical scrollbar - twice as wide for better touch
                GUIStyle wideVerticalScrollbarStyle = new GUIStyle(GUI.skin.verticalScrollbar);
                wideVerticalScrollbarStyle.fixedWidth = 80f; // Double the previous width (was 40f)

                // Also need to make the thumb (slider part) wider to be more touch-friendly
                GUIStyle wideThumbStyle = new GUIStyle(GUI.skin.verticalScrollbarThumb);
                wideThumbStyle.fixedWidth = 70f; // Make the thumb nearly as wide as the scrollbar

                // Store the original thumb style to restore later
                GUIStyle originalThumbStyle = GUI.skin.verticalScrollbarThumb;

                // Apply our custom thumb style
                GUI.skin.verticalScrollbarThumb = wideThumbStyle;

                // Use a scroll view with custom vertical scrollbar style
                // alwaysShowVertical=true so we have a scrollbar visual indicator
                // alwaysShowHorizontal=false to eliminate the warnings
                _deviceListScrollPosition = GUILayout.BeginScrollView(
                    _deviceListScrollPosition,
                    false, // Never show horizontal scrollbar
                    true, // Always show vertical scrollbar
                    GUI.skin.scrollView, // Default scroll view style
                    wideVerticalScrollbarStyle, // Custom wider vertical scrollbar
                    GUIStyle.none, // No horizontal scrollbar style to avoid warnings
                    GUILayout.Height(scrollViewHeight)
                );

                foreach (var device in _pairedDevices)
                {
                    if (!device.TryGetValue("address", out var address)) continue;
                    if (!device.TryGetValue("name", out var name)) continue;

                    device.TryGetValue("type", out var type);

                    GUILayout.BeginVertical(GUI.skin.box);
                    GUILayout.Label(name, UIHelper.BoldStyle);
                    GUILayout.Label($"Address: {address}");
                    if (!string.IsNullOrEmpty(type)) { GUILayout.Label($"Type: {type}"); }

                    if (GUILayout.Button("Forget Device", GUILayout.Height(30)))
                    {
                        _deviceToForget = address;
                        ConnectionBridge.RemoveBond(_deviceToForget);
                    }

                    GUILayout.EndVertical();
                }

                GUILayout.EndScrollView();

                // Restore the original thumb style
                GUI.skin.verticalScrollbarThumb = originalThumbStyle;
            }

            GUILayout.EndVertical();
        }
    }
}
