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
            GUILayout.BeginVertical(GUI.skin.box);
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

            if (BleHidManager != null && (isInitialized || IsEditorMode))
            {
                if (GUILayout.Button(BleHidManager.IsAdvertising ? "Stop Advertising" : "Start Advertising", GUILayout.Height(60)))
                {
                    if (IsEditorMode)
                    {
                        Logger.AddLogEntry("Advertising toggle (not functional in editor)");
                    }
                    else
                    {
                        if (BleHidManager.IsAdvertising)
                            BleHidManager.StopAdvertising();
                        else
                            BleHidManager.StartAdvertising();
                    }
                }
            }
            else
            {
                GUI.enabled = false;
                GUILayout.Button("Start Advertising", GUILayout.Height(60));
                GUI.enabled = true;
            }
            GUILayout.EndVertical();
        }
    }
}
