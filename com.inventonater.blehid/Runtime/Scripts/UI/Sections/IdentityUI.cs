using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// UI component for managing BLE peripheral device identity.
    /// Allows viewing and changing the device name and resetting the identity UUID.
    /// </summary>
    public class IdentityUI : SectionUI
    {
        public ConnectionBridge ConnectionBridge { get; }
        public const string Name = "Identity";
        public override string TabName => Name;

        private string _deviceUuid = string.Empty;
        private string _creationDate = string.Empty;
        private string _deviceName = string.Empty;
        private string _newDeviceName = string.Empty;
        private List<Dictionary<string, string>> _pairedDevices = new List<Dictionary<string, string>>();
        private Vector2 _deviceListScrollPosition = Vector2.zero;
        private string _statusMessage = string.Empty;
        private Color _statusColor = Color.white;
        private bool _showConfirmResetDialog = false;
        private bool _showConfirmForgetDialog = false;
        private bool _showConfirmDisconnectDialog = false;
        private string _deviceToForget = string.Empty;
        private string _deviceToForgetName = string.Empty;

        public IdentityUI(ConnectionBridge connectionBridge)
        {
            ConnectionBridge = connectionBridge;
            RefreshIdentityDisplay();
            RefreshPairedDevices();
        }

        public override void Shown()
        {
            RefreshIdentityDisplay();
            RefreshPairedDevices();
        }

        public override void Hidden()
        {
            // Clear any pending dialogs
            _showConfirmResetDialog = false;
            _showConfirmForgetDialog = false;
            _showConfirmDisconnectDialog = false;
        }
        
        /// <summary>
        /// Disconnects from the currently connected device.
        /// </summary>
        private void DisconnectDevice()
        {
            bool success = BleHidManager.BleBridge.Connection.Disconnect();
            if (success)
            {
                Debug.Log("Disconnect command sent successfully");
                ShowNotification("Disconnecting from device...");
            }
            else
            {
                Debug.LogError("Failed to disconnect");
                ShowNotification("Failed to disconnect from device", isError: true);
            }
        }

        public override void Update()
        {
        }

        public override void DrawUI()
        {
            GUIStyle boldStyle = new GUIStyle(GUI.skin.label) { fontStyle = FontStyle.Bold };
            
            UIHelper.BeginSection("Device Identity");
            
            // Connected Device Section (shown only when a device is connected)
            if (ConnectionBridge.IsConnected && !string.IsNullOrEmpty(ConnectionBridge.ConnectedDeviceAddress))
            {
                GUILayout.BeginVertical(new GUIStyle(GUI.skin.box) 
                { 
                    normal = { background = UIHelper.MakeColorTexture(new Color(0.0f, 0.5f, 0.0f, 0.2f)) } 
                });
                GUILayout.BeginHorizontal();
                GUILayout.Label("Connected Device:", boldStyle, GUILayout.Width(150));
                GUILayout.Label(ConnectionBridge.ConnectedDeviceName ?? "Unknown Device",
                    new GUIStyle(GUI.skin.label) { fontStyle = FontStyle.Bold, normal = { textColor = Color.green } });
                GUILayout.EndHorizontal();
                
                GUILayout.BeginHorizontal();
                GUILayout.Label("Address:", GUILayout.Width(150));
                GUILayout.Label(ConnectionBridge.ConnectedDeviceAddress);
                GUILayout.EndHorizontal();
                
                if (GUILayout.Button("Disconnect", GUILayout.Height(50)))
                {
                    _showConfirmDisconnectDialog = true;
                }
                GUILayout.EndVertical();
                
                GUILayout.Space(10);
            }
            
            // Device UUID
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Device UUID:", boldStyle);
            GUILayout.Label(_deviceUuid, GUILayout.ExpandWidth(true));
            
            // Creation date
            if (!string.IsNullOrEmpty(_creationDate))
            {
                GUILayout.Label(_creationDate);
            }
            GUILayout.EndVertical();
            
            GUILayout.Space(10);
            
            // Device Name
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Device Name:", boldStyle);
            
            // Create a custom style for the text field with larger height
            GUIStyle largeTextFieldStyle = new GUIStyle(GUI.skin.textField);
            largeTextFieldStyle.fontSize = GUI.skin.textField.fontSize;
            largeTextFieldStyle.fixedHeight = 60f; // Match standard button height
            
            GUILayout.BeginHorizontal();
            _newDeviceName = GUILayout.TextField(_newDeviceName, largeTextFieldStyle, GUILayout.ExpandWidth(true));
            if (GUILayout.Button("Save", GUILayout.Width(100), GUILayout.Height(60)))
            {
                SaveDeviceName();
            }
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            GUILayout.Space(10);
            
            // Reset Identity
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Reset Device Identity:", boldStyle);
            GUILayout.Label("This will generate a new UUID and require re-pairing with all devices.", 
                new GUIStyle(GUI.skin.label) { wordWrap = true });
            
            if (GUILayout.Button("Reset Identity", GUILayout.Height(50)))
            {
                _showConfirmResetDialog = true;
            }
            GUILayout.EndVertical();
            
            GUILayout.Space(10);
            
            // Paired Devices
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Paired Previously on this Phone:", boldStyle);
            
            if (_pairedDevices.Count == 0)
            {
                GUILayout.Label("No paired devices found.");
            }
            else
            {
                GUILayout.BeginHorizontal();
                if (GUILayout.Button("Refresh", GUILayout.Width(100), GUILayout.Height(30)))
                {
                    RefreshPairedDevices();
                }
                GUILayout.EndHorizontal();
                
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
                    false,  // Never show horizontal scrollbar
                    true,   // Always show vertical scrollbar
                    GUI.skin.scrollView, // Default scroll view style
                    wideVerticalScrollbarStyle, // Custom wider vertical scrollbar
                    GUIStyle.none, // No horizontal scrollbar style to avoid warnings
                    GUILayout.Height(scrollViewHeight)
                );
                
                foreach (var device in _pairedDevices)
                {
                    if (!device.TryGetValue("address", out string address) || 
                        !device.TryGetValue("name", out string name))
                        continue;
                        
                    device.TryGetValue("type", out string type);
                    
                    GUILayout.BeginVertical(GUI.skin.box);
                    GUILayout.Label(name, boldStyle);
                    GUILayout.Label($"Address: {address}");
                    if (!string.IsNullOrEmpty(type))
                    {
                        GUILayout.Label($"Type: {type}");
                    }
                    
                    if (GUILayout.Button("Forget Device", GUILayout.Height(30)))
                    {
                        _deviceToForget = address;
                        _deviceToForgetName = name;
                        _showConfirmForgetDialog = true;
                    }
                    GUILayout.EndVertical();
                }
                
                GUILayout.EndScrollView();
                
                // Restore the original thumb style 
                GUI.skin.verticalScrollbarThumb = originalThumbStyle;
            }
            
            GUILayout.EndVertical();
            
            // Status message if any
            if (!string.IsNullOrEmpty(_statusMessage))
            {
                GUIStyle statusStyle = new GUIStyle(GUI.skin.label)
                {
                    normal = { textColor = _statusColor },
                    alignment = TextAnchor.MiddleCenter,
                    fontStyle = FontStyle.Bold
                };
                
                GUILayout.Space(10);
                GUILayout.Label(_statusMessage, statusStyle);
            }
            
            UIHelper.EndSection();
            
            // Draw confirmation dialogs if needed
            DrawConfirmationDialogs();
        }
        
        private void DrawConfirmationDialogs()
        {
            // Reset Identity confirmation dialog
            if (_showConfirmResetDialog)
            {
                // Create a semi-transparent overlay for the dialog background
                GUI.Box(new Rect(0, 0, Screen.width, Screen.height), GUIContent.none, 
                    new GUIStyle(){ normal = { background = UIHelper.MakeColorTexture(new Color(0,0,0,0.5f)) } });
                
                // Center the dialog
                float dialogWidth = 450;
                float dialogHeight = 200;
                Rect dialogRect = new Rect(
                    (Screen.width - dialogWidth) / 2,
                    (Screen.height - dialogHeight) / 2,
                    dialogWidth, 
                    dialogHeight);
                
                GUILayout.BeginArea(dialogRect, GUI.skin.box);
                
                GUILayout.BeginVertical();
                GUILayout.Label("Reset Device Identity", 
                    new GUIStyle(GUI.skin.label) { fontSize = 22, fontStyle = FontStyle.Bold, alignment = TextAnchor.MiddleCenter });
                
                GUILayout.Space(10);
                
                GUILayout.Label("This will generate a new device identity and require re-pairing with all devices. Continue?", 
                    new GUIStyle(GUI.skin.label) { wordWrap = true });
                
                GUILayout.Space(20);
                
                GUILayout.BeginHorizontal();
                if (GUILayout.Button("Cancel", GUILayout.Height(50)))
                {
                    _showConfirmResetDialog = false;
                }
                
                if (GUILayout.Button("Reset Identity", GUILayout.Height(50)))
                {
                    _showConfirmResetDialog = false;
                    ResetIdentity();
                }
                GUILayout.EndHorizontal();
                
                GUILayout.EndVertical();
                GUILayout.EndArea();
            }
            
            // Disconnect confirmation dialog
            if (_showConfirmDisconnectDialog)
            {
                // Create a semi-transparent overlay
                GUI.Box(new Rect(0, 0, Screen.width, Screen.height), GUIContent.none, 
                    new GUIStyle(){ normal = { background = UIHelper.MakeColorTexture(new Color(0,0,0,0.5f)) } });
                
                // Center the dialog
                float dialogWidth = 450;
                float dialogHeight = 200;
                Rect dialogRect = new Rect(
                    (Screen.width - dialogWidth) / 2,
                    (Screen.height - dialogHeight) / 2,
                    dialogWidth, 
                    dialogHeight);
                
                GUILayout.BeginArea(dialogRect, GUI.skin.box);
                
                GUILayout.BeginVertical();
                GUILayout.Label("Disconnect Device", 
                    new GUIStyle(GUI.skin.label) { fontSize = 22, fontStyle = FontStyle.Bold, alignment = TextAnchor.MiddleCenter });
                
                GUILayout.Space(10);
                
                GUILayout.Label($"Disconnect from '{ConnectionBridge.ConnectedDeviceName}'?",
                    new GUIStyle(GUI.skin.label) { wordWrap = true });
                
                GUILayout.Space(20);
                
                GUILayout.BeginHorizontal();
                if (GUILayout.Button("Cancel", GUILayout.Height(50)))
                {
                    _showConfirmDisconnectDialog = false;
                }
                
                if (GUILayout.Button("Disconnect", GUILayout.Height(50)))
                {
                    _showConfirmDisconnectDialog = false;
                    DisconnectDevice();
                }
                GUILayout.EndHorizontal();
                
                GUILayout.EndVertical();
                GUILayout.EndArea();
            }
            
            // Forget Device confirmation dialog
            if (_showConfirmForgetDialog)
            {
                // Create a semi-transparent overlay
                GUI.Box(new Rect(0, 0, Screen.width, Screen.height), GUIContent.none, 
                    new GUIStyle(){ normal = { background = UIHelper.MakeColorTexture(new Color(0,0,0,0.5f)) } });
                
                // Center the dialog
                float dialogWidth = 450;
                float dialogHeight = 200;
                Rect dialogRect = new Rect(
                    (Screen.width - dialogWidth) / 2,
                    (Screen.height - dialogHeight) / 2,
                    dialogWidth, 
                    dialogHeight);
                
                GUILayout.BeginArea(dialogRect, GUI.skin.box);
                
                GUILayout.BeginVertical();
                GUILayout.Label("Forget Device", 
                    new GUIStyle(GUI.skin.label) { fontSize = 22, fontStyle = FontStyle.Bold, alignment = TextAnchor.MiddleCenter });
                
                GUILayout.Space(10);
                
                GUILayout.Label($"Are you sure you want to forget device '{_deviceToForgetName}'?\nYou will need to re-pair to use it again.", 
                    new GUIStyle(GUI.skin.label) { wordWrap = true });
                
                GUILayout.Space(20);
                
                GUILayout.BeginHorizontal();
                if (GUILayout.Button("Cancel", GUILayout.Height(50)))
                {
                    _showConfirmForgetDialog = false;
                }
                
                if (GUILayout.Button("Forget Device", GUILayout.Height(50)))
                {
                    _showConfirmForgetDialog = false;
                    ForgetDevice(_deviceToForget, _deviceToForgetName);
                }
                GUILayout.EndHorizontal();
                
                GUILayout.EndVertical();
                GUILayout.EndArea();
            }
        }

        private void RefreshIdentityDisplay()
        {
            if (BleHidManager == null || BleHidManager.IdentityManager == null)
                return;

            // Get the UUID
            _deviceUuid = BleHidManager.IdentityManager.GetOrCreateDeviceUuid();

            // Format creation date
            string creationDate = BleHidManager.IdentityManager.GetIdentityCreationDate();
            if (creationDate == "Unknown")
                _creationDate = "Creation date: Unknown";
            else
            {
                try
                {
                    System.DateTime dt = System.DateTime.Parse(creationDate);
                    _creationDate = "Created: " + dt.ToString("g");
                }
                catch
                {
                    _creationDate = "Created: " + creationDate;
                }
            }

            // Get current device name
            _deviceName = BleHidManager.IdentityManager.GetDeviceName();
            _newDeviceName = _deviceName;
        }

        private void SaveDeviceName()
        {
            if (BleHidManager == null || BleHidManager.IdentityManager == null)
                return;

            string newName = _newDeviceName.Trim();
            if (string.IsNullOrEmpty(newName))
            {
                // Revert to existing name
                _newDeviceName = _deviceName;
                return;
            }

            // Set new device name
            bool success = BleHidManager.IdentityManager.SetDeviceName(newName);
            if (success)
            {
                _deviceName = newName;
                Debug.Log($"Device name changed to: {newName}");
                ShowNotification($"Device name changed to: {newName}");
            }
            else
            {
                Debug.LogError("Failed to set device name");
                ShowNotification("Failed to set device name", isError: true);
            }
        }

        private void ResetIdentity()
        {
            if (BleHidManager == null || BleHidManager.IdentityManager == null)
                return;

            bool success = BleHidManager.IdentityManager.ResetIdentity();
            if (success)
            {
                RefreshIdentityDisplay();
                RefreshPairedDevices(); // Pairs are likely invalidated
                
                Debug.Log("Device identity reset successfully");
                ShowNotification("Device identity reset successfully");
            }
            else
            {
                Debug.LogError("Failed to reset device identity");
                ShowNotification("Failed to reset device identity", isError: true);
            }
        }

        private void RefreshPairedDevices()
        {
            if (BleHidManager == null || BleHidManager.IdentityManager == null)
                return;

            // Get paired devices
            _pairedDevices = BleHidManager.IdentityManager.GetBondedDevices();
            
            if (_pairedDevices.Count > 0)
            {
                ShowNotification($"Found {_pairedDevices.Count} paired device(s)");
            }
        }

        private void ForgetDevice(string address, string name)
        {
            if (BleHidManager == null || BleHidManager.IdentityManager == null)
                return;

            bool success = BleHidManager.IdentityManager.ForgetDevice(address);
            if (success)
            {
                // Refresh the device list
                RefreshPairedDevices();
                
                Debug.Log($"Forgot device: {name} ({address})");
                ShowNotification($"Forgot device: {name}");
            }
            else
            {
                Debug.LogError($"Failed to forget device: {name} ({address})");
                ShowNotification($"Failed to forget device: {name}", isError: true);
            }
        }

        private void ShowNotification(string message, bool isError = false)
        {
            _statusMessage = message;
            _statusColor = isError ? Color.red : Color.green;
            Logger.AddLogEntry(isError ? "ERROR: " + message : message);

            // Clear the status message after 3 seconds
            clearStatusAfterDelay();
        }
        
        private async void clearStatusAfterDelay()
        {
            // Mock async delay since we can't use coroutines in a non-MonoBehaviour
            await System.Threading.Tasks.Task.Delay(3000);
            _statusMessage = string.Empty;
        }
    }
}
