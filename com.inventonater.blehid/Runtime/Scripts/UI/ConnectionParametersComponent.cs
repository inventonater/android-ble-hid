using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// UI Component for displaying and controlling BLE connection parameters
    /// </summary>
    public class ConnectionParametersComponent : UIComponent
    {
        private int requestedMtu = 23;
        private string statusMessage = "";
        private Color statusColor = Color.white;
        
        // Connection parameter info
        private string connectionInterval = "--";
        private string slaveLatency = "--";
        private string supervisionTimeout = "--";
        private string mtuSize = "--";
        private string rssi = "--";
        private Color rssiColor = Color.white;
        
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
            GUILayout.Label("Connection Interval: " + connectionInterval + " ms");
            GUILayout.Label("Slave Latency: " + slaveLatency);
            GUILayout.Label("Supervision Timeout: " + supervisionTimeout + " ms");
            
            // RSSI with color
            GUIStyle rssiStyle = new GUIStyle(GUI.skin.label);
            rssiStyle.normal.textColor = rssiColor;
            GUILayout.Label("RSSI: " + rssi + " dBm", rssiStyle);
            
            GUILayout.Label("MTU Size: " + mtuSize + " bytes");
            GUILayout.EndVertical();
            
            GUILayout.Space(10);
            
            // Connection Priority section
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Connection Priority:", boldStyle);
            
            GUI.enabled = connected || IsEditorMode;
            
            if (ActionButton("High Priority (Low Latency)", 
                    () => RequestConnectionPriority(0),
                    "Request high priority connection", 
                    UIHelper.StandardButtonOptions))
            {
                SetStatus("Requesting HIGH priority...", Color.yellow);
            }
            
            if (ActionButton("Balanced", 
                    () => RequestConnectionPriority(1),
                    "Request balanced connection priority", 
                    UIHelper.StandardButtonOptions))
            {
                SetStatus("Requesting BALANCED priority...", Color.yellow);
            }
            
            if (ActionButton("Low Power", 
                    () => RequestConnectionPriority(2),
                    "Request low power connection priority", 
                    UIHelper.StandardButtonOptions))
            {
                SetStatus("Requesting LOW POWER priority...", Color.yellow);
            }
            
            GUI.enabled = true;
            GUILayout.EndVertical();
            
            GUILayout.Space(10);
            
            // MTU Size section
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("MTU Size:", boldStyle);
            
            GUI.enabled = connected || IsEditorMode;
            
            GUILayout.BeginHorizontal();
            GUILayout.Label("23", GUILayout.Width(30));
            requestedMtu = Mathf.RoundToInt(GUILayout.HorizontalSlider(requestedMtu, 23, 517));
            GUILayout.Label("517", GUILayout.Width(30));
            GUILayout.EndHorizontal();
            
            GUILayout.Label("Requested MTU: " + requestedMtu + " bytes");
            
            if (ActionButton("Request MTU Size", 
                    () => RequestMtu(),
                    "Request new MTU size", 
                    UIHelper.StandardButtonOptions))
            {
                SetStatus($"Requesting MTU {requestedMtu}...", Color.yellow);
            }
            
            GUI.enabled = true;
            GUILayout.EndVertical();
            
            GUILayout.Space(10);
            
            // Transmit Power section
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Transmit Power:", boldStyle);
            
            GUI.enabled = initialized || IsEditorMode;
            
            if (ActionButton("Low Power", 
                    () => SetTransmitPowerLevel(0),
                    "Set low transmit power", 
                    UIHelper.StandardButtonOptions))
            {
                SetStatus("Setting TX power to LOW...", Color.yellow);
            }
            
            if (ActionButton("Medium Power", 
                    () => SetTransmitPowerLevel(1),
                    "Set medium transmit power", 
                    UIHelper.StandardButtonOptions))
            {
                SetStatus("Setting TX power to MEDIUM...", Color.yellow);
            }
            
            if (ActionButton("High Power", 
                    () => SetTransmitPowerLevel(2),
                    "Set high transmit power", 
                    UIHelper.StandardButtonOptions))
            {
                SetStatus("Setting TX power to HIGH...", Color.yellow);
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
            
            SetStatus("RSSI read successfully", Color.green);
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
