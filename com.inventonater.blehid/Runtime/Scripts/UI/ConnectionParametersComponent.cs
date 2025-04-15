using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// UI Component for displaying and controlling BLE connection parameters
    /// </summary>
    public class ConnectionParametersComponent : UIComponent
    {
        // Requested values
        private int requestedMtu = 512; // Default to maximum for best performance
        private int requestedConnectionPriority = 0; // Default to HIGH (0) for best performance
        private int requestedTxPowerLevel = 2; // Default to HIGH (2) for best signal strength
        
        // Track last applied values to avoid redundant requests
        private int lastAppliedMtu = 512;
        private int lastAppliedTxPowerLevel = 2;
        
        // Slider interaction tracking
        private bool isSliderBeingDragged = false;
        
        // Dropdown state tracking
        private bool connectionPriorityDropdownExpanded = false;
        private bool txPowerDropdownExpanded = false;
        
        // UI state
        private string statusMessage = "";
        private Color statusColor = Color.white;
        private string[] connectionPriorityNames = new string[] { "HIGH (Low Latency)", "BALANCED", "LOW POWER" };
        private string[] expectedIntervals = new string[] { "7.5-15ms", "30-50ms", "100-500ms" };
        private string[] txPowerLevelNames = new string[] { "LOW", "MEDIUM", "HIGH" };
        
        // Connection parameter info - actual values
        private string connectionInterval = "--";
        private string slaveLatency = "--";
        private string supervisionTimeout = "--";
        private string mtuSize = "--";
        private string rssi = "--";
        private Color rssiColor = Color.white;
        private Color intervalColor = Color.white;
        private Color mtuColor = Color.white;
        
        public override void Initialize(BleHidManager bleManager, LoggingManager logger, bool isEditorMode)
        {
            base.Initialize(bleManager, logger, isEditorMode);
            
            // Register to events
            if (BleHidManager != null)
            {
                BleHidManager.OnConnectionParametersChanged += HandleConnectionParametersChanged;
                BleHidManager.OnRssiRead += HandleRssiRead;
                BleHidManager.OnConnectionParameterRequestComplete += HandleConnectionParameterRequestComplete;
                BleHidManager.OnConnectionStateChanged += HandleConnectionStateChanged;
            }
            
            // Initialize with current values if connected
            UpdateValuesFromManager();
        }
        
        public override void DrawUI()
        {
            bool connected = BleHidManager != null && BleHidManager.IsConnected;
            bool initialized = BleHidManager != null && BleHidManager.IsInitialized;
            
            UIHelper.BeginSection("Connection Parameters");
            
            // Status message
            GUILayout.Label("Status: " + (string.IsNullOrEmpty(statusMessage) ? 
                (connected ? "Connected" : "Not Connected") : statusMessage));
            
            // Connection info section
            GUILayout.BeginVertical(GUI.skin.box);
            GUIStyle boldStyle = new GUIStyle(GUI.skin.label);
            boldStyle.fontStyle = FontStyle.Bold;
            GUILayout.Label("Connection Info:", boldStyle);
            
            // Connection interval with expected range
            GUIStyle intervalStyle = new GUIStyle(GUI.skin.label);
            intervalStyle.normal.textColor = intervalColor;
            string expectedRange = "";
            if (requestedConnectionPriority >= 0 && requestedConnectionPriority < expectedIntervals.Length)
                expectedRange = " (Expected: " + expectedIntervals[requestedConnectionPriority] + ")";
            GUILayout.Label("Connection Interval: " + connectionInterval + " ms" + expectedRange, intervalStyle);
            
            GUILayout.Label("Slave Latency: " + slaveLatency);
            GUILayout.Label("Supervision Timeout: " + supervisionTimeout + " ms");
            
            // RSSI with color and signal strength indicator
            GUIStyle rssiStyle = new GUIStyle(GUI.skin.label);
            rssiStyle.normal.textColor = rssiColor;
            string signalStrength = "";
            if (rssi != "--")
            {
                int rssiValue = int.Parse(rssi);
                if (rssiValue > -60)
                    signalStrength = " (Excellent)";
                else if (rssiValue > -70)
                    signalStrength = " (Good)";
                else if (rssiValue > -80)
                    signalStrength = " (Fair)";
                else
                    signalStrength = " (Poor)";
            }
            GUILayout.Label("RSSI: " + rssi + " dBm" + signalStrength, rssiStyle);
            
            // MTU Size with requested value
            GUIStyle mtuStyle = new GUIStyle(GUI.skin.label);
            mtuStyle.normal.textColor = mtuColor;
            GUILayout.Label("MTU Size: " + mtuSize + " bytes (Requested: " + requestedMtu + ")", mtuStyle);
            
            GUILayout.EndVertical();
            
            GUILayout.Space(10);
            
            // Connection Priority section with dropdown
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Connection Priority:", boldStyle);
            
            GUI.enabled = connected || IsEditorMode;
            
            GUILayout.BeginHorizontal();
            var dropDownLabel = GUILayout.Width(120);
            GUILayout.Label("Priority:", dropDownLabel);
            
            // Create a custom dropdown style
            GUIStyle dropdownStyle = new GUIStyle(GUI.skin.button);
            dropdownStyle.alignment = TextAnchor.MiddleLeft;
            dropdownStyle.padding = new RectOffset(10, 10, 5, 5);
            
            // Display current connection priority
            string currentPriority = "Unknown";
            if (requestedConnectionPriority >= 0 && requestedConnectionPriority < connectionPriorityNames.Length)
                currentPriority = connectionPriorityNames[requestedConnectionPriority];
            
            // Make dropdown button that toggles the expanded state
            if (GUILayout.Button(currentPriority, dropdownStyle, GUILayout.MinWidth(180)))
            {
                connectionPriorityDropdownExpanded = !connectionPriorityDropdownExpanded;
                // Close the other dropdown if it's open
                if (connectionPriorityDropdownExpanded)
                    txPowerDropdownExpanded = false;
            }
            
            // Dropdown arrow indicator
            GUILayout.Label(connectionPriorityDropdownExpanded ? "▲" : "▼", GUILayout.Width(20));
            GUILayout.EndHorizontal();
            
            // Show dropdown options if expanded
            if (connectionPriorityDropdownExpanded)
            {
                GUILayout.BeginVertical(GUI.skin.box);
                for (int i = 0; i < connectionPriorityNames.Length; i++)
                {
                    bool isSelected = (requestedConnectionPriority == i);
                    GUIStyle optionStyle = new GUIStyle(GUI.skin.button);
                    if (isSelected)
                    {
                        optionStyle.normal.textColor = Color.green;
                        optionStyle.fontStyle = FontStyle.Bold;
                    }
                    
                    if (GUILayout.Button(connectionPriorityNames[i], optionStyle))
                    {
                        requestedConnectionPriority = i;
                        RequestConnectionPriority(i);
                        connectionPriorityDropdownExpanded = false;
                    }
                }
                GUILayout.EndVertical();
            }

            GUI.enabled = true;
            GUILayout.EndVertical();
            
            GUILayout.Space(10);
            
            // MTU Size section with enhanced slider
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("MTU Size:", boldStyle);
            
            GUI.enabled = connected || IsEditorMode;
            
            // Track previous value to detect changes
            int previousMtu = requestedMtu;
            
            // Use the enhanced slider with labels
            requestedMtu = UIHelper.SliderWithLabels(
                "23", 
                requestedMtu, 
                23, 
                517, 
                "517", 
                "{0} bytes", 
                UIHelper.StandardSliderOptions
            );
            
            // Auto-apply when slider value changes
            if (previousMtu != requestedMtu && !isSliderBeingDragged)
            {
                // Only apply if value is different from last applied value
                if (requestedMtu != lastAppliedMtu)
                {
                    RequestMtu();
                    lastAppliedMtu = requestedMtu;
                    SetStatus($"Requesting MTU {requestedMtu}...", Color.yellow);
                }
            }
            
            // Track whether mouse button is pressed to detect end of slider drag
            if (Event.current.type == EventType.MouseDown && Event.current.button == 0)
            {
                isSliderBeingDragged = true;
            }
            else if (Event.current.type == EventType.MouseUp && Event.current.button == 0)
            {
                if (isSliderBeingDragged)
                {
                    isSliderBeingDragged = false;
                    // Apply on mouse up - this ensures we only send the request when user is done dragging
                    if (requestedMtu != lastAppliedMtu)
                    {
                        RequestMtu();
                        lastAppliedMtu = requestedMtu;
                        SetStatus($"Requesting MTU {requestedMtu}...", Color.yellow);
                    }
                }
            }
            
            // Add a descriptive note about MTU
            GUIStyle noteStyle = new GUIStyle(GUI.skin.label);
            noteStyle.fontSize = GUI.skin.label.fontSize - 2;
            noteStyle.wordWrap = true;
            GUILayout.Label("Higher MTU allows more data per packet. Changes are applied automatically when slider is released.", 
                noteStyle, GUILayout.Height(40));
            
            GUI.enabled = true;
            GUILayout.EndVertical();
            
            GUILayout.Space(10);
            
            // Transmit Power section with dropdown
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Transmit Power:", boldStyle);
            
            GUI.enabled = initialized || IsEditorMode;
            
            GUILayout.BeginHorizontal();
            GUILayout.Label("Level:", dropDownLabel);
            
            // Create a custom dropdown style
            GUIStyle txPowerDropdownStyle = new GUIStyle(GUI.skin.button);
            txPowerDropdownStyle.alignment = TextAnchor.MiddleLeft;
            txPowerDropdownStyle.padding = new RectOffset(10, 10, 5, 5);
            
            // Display current TX power level
            string currentLevel = "Unknown";
            if (requestedTxPowerLevel >= 0 && requestedTxPowerLevel < txPowerLevelNames.Length)
                currentLevel = txPowerLevelNames[requestedTxPowerLevel];
            
            // Make dropdown button that toggles the expanded state
            if (GUILayout.Button(currentLevel, txPowerDropdownStyle, GUILayout.MinWidth(120)))
            {
                txPowerDropdownExpanded = !txPowerDropdownExpanded;
                // Close the other dropdown if it's open
                if (txPowerDropdownExpanded)
                    connectionPriorityDropdownExpanded = false;
            }
            
            // Dropdown arrow indicator
            GUILayout.Label(txPowerDropdownExpanded ? "▲" : "▼", GUILayout.Width(20));
            GUILayout.EndHorizontal();
            
            // Show dropdown options if expanded
            if (txPowerDropdownExpanded)
            {
                GUILayout.BeginVertical(GUI.skin.box);
                for (int i = 0; i < txPowerLevelNames.Length; i++)
                {
                    bool isSelected = (requestedTxPowerLevel == i);
                    GUIStyle optionStyle = new GUIStyle(GUI.skin.button);
                    if (isSelected)
                    {
                        optionStyle.normal.textColor = Color.green;
                        optionStyle.fontStyle = FontStyle.Bold;
                    }
                    
                    if (GUILayout.Button(txPowerLevelNames[i], optionStyle))
                    {
                        requestedTxPowerLevel = i;
                        SetTransmitPowerLevel(i);
                        txPowerDropdownExpanded = false;
                    }
                }
                GUILayout.EndVertical();
            }

            GUI.enabled = true;
            GUILayout.EndVertical();
            
            GUILayout.Space(10);
            
            // Actions section
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Actions:", boldStyle);
            
            GUI.enabled = connected || IsEditorMode;
            
            if (ActionButton("Read RSSI", 
                    () => ReadRssi(),
                    "Read current RSSI value", 
                    UIHelper.StandardButtonOptions))
            {
                SetStatus("Reading RSSI...", Color.yellow);
            }
            
            if (ActionButton("Refresh Parameters", 
                    () => RefreshParameters(),
                    "Refresh connection parameters", 
                    UIHelper.StandardButtonOptions))
            {
                SetStatus("Refreshing parameters...", Color.yellow);
            }
            
            GUI.enabled = true;
            GUILayout.EndVertical();
            
            GUILayout.Space(10);
            
            // Documentation section
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Documentation:", boldStyle);
            
            if (ActionButton("Connection Parameters Help", 
                    () => ShowDocumentation(),
                    "Show documentation on connection parameters", 
                    UIHelper.StandardButtonOptions))
            {
                SetStatus("Opening Connection Parameters documentation...", Color.cyan);
            }
            
            GUILayout.EndVertical();
            
            UIHelper.EndSection();
        }
        
        private void SetStatus(string message, Color color)
        {
            statusMessage = message;
            statusColor = color;
            
            if (Logger != null)
            {
                Logger.AddLogEntry(message);
            }
        }
        
        private void UpdateValuesFromManager()
        {
            if (BleHidManager == null || !BleHidManager.IsConnected)
            {
                // Clear all parameter values
                connectionInterval = "--";
                slaveLatency = "--";
                supervisionTimeout = "--";
                rssi = "--";
                mtuSize = "--";
                return;
            }
            
            // Update parameter values
            connectionInterval = BleHidManager.ConnectionInterval.ToString();
            slaveLatency = BleHidManager.SlaveLatency.ToString();
            supervisionTimeout = BleHidManager.SupervisionTimeout.ToString();
            rssi = BleHidManager.Rssi.ToString();
            mtuSize = BleHidManager.MtuSize.ToString();
        }
        
        private void RequestConnectionPriority(int priority)
        {
            if (BleHidManager == null || !BleHidManager.IsConnected) return;
            
            BleHidManager.RequestConnectionPriority(priority);
        }
        
        private void RequestMtu()
        {
            if (BleHidManager == null || !BleHidManager.IsConnected) return;
            
            BleHidManager.RequestMtu(requestedMtu);
        }
        
        private void SetTransmitPowerLevel(int level)
        {
            if (BleHidManager == null || !BleHidManager.IsInitialized) return;
            
            BleHidManager.SetTransmitPowerLevel(level);
        }
        
        private void ReadRssi()
        {
            if (BleHidManager == null || !BleHidManager.IsConnected) return;
            
            BleHidManager.ReadRssi();
        }
        
        private void RefreshParameters()
        {
            if (BleHidManager == null || !BleHidManager.IsConnected) return;
            
            Dictionary<string, string> parameters = BleHidManager.GetConnectionParameters();
            if (parameters != null)
            {
                SetStatus("Parameters refreshed", Color.green);
                
                // Read RSSI as well
                ReadRssi();
                
                // Update values
                UpdateValuesFromManager();
            }
            else
            {
                SetStatus("Failed to get parameters", Color.red);
            }
        }
        
        private void ShowDocumentation()
        {
            string documentationPath = "Documentation/ConnectionParameters.md";
            
            try
            {
                #if UNITY_EDITOR
                // In the editor, try to open the file directly
                string fullPath = System.IO.Path.Combine(Application.dataPath, "..", documentationPath);
                fullPath = System.IO.Path.GetFullPath(fullPath);
                
                // Use OpenURL as a fallback
                Application.OpenURL("file://" + fullPath);
                
                SetStatus("Documentation opened in default application", Color.green);
                #elif UNITY_ANDROID
                // On Android, display helpful information
                SetStatus("Documentation available in package at: " + documentationPath, Color.green);
                
                // Show more detailed info in the log
                if (Logger != null)
                {
                    Logger.AddLogEntry("Connection Parameters documentation is available in the package at: " + documentationPath);
                    Logger.AddLogEntry("The documentation provides details about each parameter and recommended settings for different use cases.");
                }
                #else
                // For other platforms, just log where to find it
                SetStatus("Documentation available in package at: " + documentationPath, Color.green);
                #endif
            }
            catch (System.Exception e)
            {
                Debug.LogError("Error opening documentation: " + e.Message);
                SetStatus("Error opening documentation. See logs for details.", Color.red);
            }
        }
        
        // Event handlers
        
        private void HandleConnectionParametersChanged(int interval, int latency, int timeout, int mtu)
        {
            connectionInterval = interval.ToString();
            slaveLatency = latency.ToString();
            supervisionTimeout = timeout.ToString();
            mtuSize = mtu.ToString();
            
            // Color coding for connection interval based on requested priority
            if (requestedConnectionPriority == 0) // HIGH priority requested
            {
                if (interval <= 15) 
                    intervalColor = Color.green; // Good - within expected range for HIGH
                else if (interval <= 30)
                    intervalColor = Color.yellow; // OK - faster than BALANCED but not as fast as HIGH
                else
                    intervalColor = Color.red; // Bad - not getting low latency despite HIGH priority
            }
            else if (requestedConnectionPriority == 1) // BALANCED priority requested
            {
                if (interval >= 30 && interval <= 50)
                    intervalColor = Color.green; // Good - within expected range for BALANCED
                else if (interval < 30)
                    intervalColor = Color.green; // Good - actually got better than expected
                else
                    intervalColor = Color.yellow; // OK - higher than expected, but may be OK
            }
            else if (requestedConnectionPriority == 2) // LOW POWER priority requested
            {
                if (interval >= 100)
                    intervalColor = Color.green; // Good for power saving
                else
                    intervalColor = Color.yellow; // Not as power efficient as requested
            }
            
            // Color coding for MTU size
            if (mtu >= requestedMtu)
                mtuColor = Color.green; // Got requested MTU or better
            else if (mtu >= requestedMtu * 0.8f)
                mtuColor = Color.yellow; // Got close to requested MTU (80% or more)
            else
                mtuColor = Color.red; // Got significantly less than requested MTU
            
            SetStatus("Parameters updated", Color.green);
        }
        
        private void HandleRssiRead(int rssiValue)
        {
            rssi = rssiValue.ToString();
            
            // Color-code the RSSI value
            if (rssiValue > -60)
                rssiColor = Color.green;
            else if (rssiValue > -80)
                rssiColor = Color.yellow;
            else
                rssiColor = Color.red;
        }
        
        private void HandleConnectionParameterRequestComplete(string parameterName, bool success, string actualValue)
        {
            if (success)
            {
                SetStatus($"{parameterName} set to {actualValue}", Color.green);
            }
            else
            {
                SetStatus($"Failed to set {parameterName}", Color.red);
            }
            
            UpdateValuesFromManager();
        }
        
        private void HandleConnectionStateChanged(bool connected, string deviceName, string deviceAddress)
        {
            if (connected)
            {
                SetStatus($"Connected to {deviceName}", Color.green);
            }
            else
            {
                SetStatus("Disconnected", Color.red);
            }
            
            UpdateValuesFromManager();
        }
    }
}
