using System;
using UnityEngine;

namespace Inventonater.BleHid.UI.Panels.Local
{
    /// <summary>
    /// Panel for local system control functionality.
    /// Provides system-level controls like quick settings toggles and notifications.
    /// </summary>
    public class LocalSystemPanel : BaseBleHidPanel
    {
        private BleHidLocalControl localControl;
        
        // Toggle states for system settings (doesn't actually reflect real state yet)
        private bool wifiEnabled = true;
        private bool bluetoothEnabled = true;
        private bool flashlightEnabled = false;
        private bool airplaneModeEnabled = false;
        private bool darkModeEnabled = false;
        
        public override bool RequiresConnectedDevice => false;
        
        public override void Initialize(BleHidManager manager)
        {
            base.Initialize(manager);
            
            // Get reference to the local control instance
            try
            {
                localControl = BleHidLocalControl.Instance;
            }
            catch (Exception e)
            {
                logger.LogError($"Failed to get LocalControl instance: {e.Message}");
            }
        }
        
        protected override void DrawPanelContent()
        {
            GUILayout.Label("System Controls", titleStyle);
            
            #if UNITY_EDITOR
            bool isEditorMode = true;
            #else
            bool isEditorMode = false;
            #endif
            
            // Quick Settings Toggles
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Quick Settings", subtitleStyle);
            
            // Quick Settings Row 1
            GUILayout.BeginHorizontal();
            DrawToggleButton("WiFi", ref wifiEnabled, GUILayout.Height(60));
            DrawToggleButton("Bluetooth", ref bluetoothEnabled, GUILayout.Height(60));
            DrawToggleButton("Flashlight", ref flashlightEnabled, GUILayout.Height(60));
            GUILayout.EndHorizontal();
            
            // Quick Settings Row 2
            GUILayout.BeginHorizontal();
            DrawToggleButton("Airplane Mode", ref airplaneModeEnabled, GUILayout.Height(60));
            DrawToggleButton("Dark Mode", ref darkModeEnabled, GUILayout.Height(60));
            
            if (GUILayout.Button("Auto-Rotate", GUILayout.Height(60)))
            {
                logger.Log("Auto-rotate toggle (not implemented)");
            }
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            // Notification Actions
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Notifications", subtitleStyle);
            
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Pull Down Notifications", GUILayout.Height(60)))
            {
                logger.Log("Pull down notifications (not implemented)");
            }
            
            if (GUILayout.Button("Clear All Notifications", GUILayout.Height(60)))
            {
                logger.Log("Clear all notifications (not implemented)");
            }
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            // System Controls
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("System Actions", subtitleStyle);
            
            // System Controls Row 1
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Screenshot", GUILayout.Height(60)))
            {
                logger.Log("Screenshot capture (not implemented)");
            }
            
            if (GUILayout.Button("Power Dialog", GUILayout.Height(60)))
            {
                logger.Log("Power dialog (not implemented)");
            }
            GUILayout.EndHorizontal();
            
            // System Controls Row 2
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Split Screen", GUILayout.Height(60)))
            {
                logger.Log("Split screen toggle (not implemented)");
            }
            
            if (GUILayout.Button("One-handed Mode", GUILayout.Height(60)))
            {
                logger.Log("One-handed mode toggle (not implemented)");
            }
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            // App Shortcuts
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("App Shortcuts", subtitleStyle);
            
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Settings", GUILayout.Height(60)))
            {
                logger.Log("Open Settings app (not implemented)");
            }
            
            if (GUILayout.Button("Camera", GUILayout.Height(60)))
            {
                logger.Log("Open Camera app (not implemented)");
            }
            
            if (GUILayout.Button("Quick Launch", GUILayout.Height(60)))
            {
                logger.Log("Quick launch app selector (not implemented)");
            }
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
        }
        
        /// <summary>
        /// Draw a toggle button that changes state when clicked.
        /// </summary>
        private void DrawToggleButton(string text, ref bool state, params GUILayoutOption[] options)
        {
            // Change button style based on state
            GUIStyle toggleStyle = new GUIStyle(GUI.skin.button);
            if (state)
            {
                toggleStyle.normal.background = MakeColorTexture(new Color(0.2f, 0.7f, 0.2f, 1.0f));
                toggleStyle.normal.textColor = Color.white;
            }
            
            if (GUILayout.Button(text + (state ? ": ON" : ": OFF"), toggleStyle, options))
            {
                // Toggle state
                state = !state;
                logger.Log($"{text} toggled to {(state ? "ON" : "OFF")} (not implemented)");
            }
        }
    }
}
