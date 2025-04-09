using UnityEngine;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// UI component for displaying BLE HID status and advertising controls
    /// </summary>
    public class StatusComponent : UIComponent
    {
        private bool isInitialized = false;
        
        public void SetInitialized(bool initialized)
        {
            isInitialized = initialized;
        }
        
        public override void DrawUI()
        {
            UIHelper.BeginSection("Connection Status");
            
            // Status display
            GUILayout.Label("Status: " + (isInitialized ? "Ready" : "Initializing..."));

            if (BleHidManager != null && BleHidManager.IsConnected)
            {
                GUILayout.Label("Connected to: " + BleHidManager.ConnectedDeviceName);
                GUILayout.Label("Device: " + BleHidManager.ConnectedDeviceName + " (" + BleHidManager.ConnectedDeviceAddress + ")");
            }
            else
            {
                GUILayout.Label("Not connected");

                if (IsEditorMode)
                {
                    GUILayout.Label("EDITOR MODE: UI visible but BLE functions disabled");
                }
            }

            // Advertising button
            if (BleHidManager != null && (isInitialized || IsEditorMode))
            {
                string buttonLabel = BleHidManager.IsAdvertising ? "Stop Advertising" : "Start Advertising";
                string editorMessage = "Advertising toggle (not functional in editor)";
                
                UIHelper.LoggingButton(
                    buttonLabel,
                    () => {
                        if (BleHidManager.IsAdvertising)
                            BleHidManager.StopAdvertising();
                        else
                            BleHidManager.StartAdvertising();
                    },
                    editorMessage,
                    IsEditorMode,
                    Logger,
                    UIHelper.StandardButtonOptions);
            }
            else
            {
                GUI.enabled = false;
                GUILayout.Button("Start Advertising", UIHelper.StandardButtonOptions);
                GUI.enabled = true;
            }
            
            UIHelper.EndSection();
        }
    }
}
