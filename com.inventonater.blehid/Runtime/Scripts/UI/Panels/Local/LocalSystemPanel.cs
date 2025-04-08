using System;
using UnityEngine;

namespace Inventonater.BleHid.UI.Panels.Local
{
    /// <summary>
    /// Panel for local system control functionality.
    /// Primarily handles accessibility service setup and status.
    /// </summary>
    public class LocalSystemPanel : BaseBleHidPanel
    {
        private BleHidLocalControl localControl;
        private bool accessibilityServiceEnabled = false;
        
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
        
        public override void OnActivate()
        {
            base.OnActivate();
            
            // Check accessibility status when panel is activated
            CheckAccessibilityStatus();
        }
        
        /// <summary>
        /// Check if the accessibility service is enabled
        /// </summary>
        private void CheckAccessibilityStatus()
        {
            #if UNITY_EDITOR
            // In editor mode, pretend it's enabled
            accessibilityServiceEnabled = true;
            #else
            // Check actual status
            if (localControl != null)
            {
                try
                {
                    accessibilityServiceEnabled = localControl.IsAccessibilityServiceEnabled();
                }
                catch (Exception e)
                {
                    logger.LogError($"Error checking accessibility status: {e.Message}");
                    accessibilityServiceEnabled = false;
                }
            }
            else
            {
                accessibilityServiceEnabled = false;
            }
            #endif
        }
        
        protected override void DrawPanelContent()
        {
            GUILayout.Label("System Settings", titleStyle);
            GUILayout.Space(10);
            
            #if UNITY_EDITOR
            bool isEditorMode = true;
            #else
            bool isEditorMode = false;
            #endif
            
            // Main area - focused on Accessibility Service status
            if (!isEditorMode)
            {
                DrawAccessibilityStatus();
                
                // Only show this message if accessibility is enabled
                if (accessibilityServiceEnabled)
                {
                    GUILayout.Space(20);
                    
                    GUILayout.BeginVertical(GUI.skin.box, GUILayout.ExpandHeight(true));
                    GUILayout.Label("System Control Ready", subtitleStyle);
                    GUILayout.Label("The app now has permission to control system functions.");
                    GUILayout.Label("You can use the Media, Navigation, and Touch panels to control your device.");
                    GUILayout.EndVertical();
                }
            }
            else
            {
                // Editor mode message
                GUILayout.BeginVertical(GUI.skin.box);
                GUILayout.Label("System Control (Editor Mode)", subtitleStyle);
                GUILayout.Label("System controls require running on an Android device");
                GUILayout.Label("with the Accessibility Service enabled.");
                GUILayout.EndVertical();
            }
        }
        
        /// <summary>
        /// Displays the accessibility service status and provides a button to enable it if needed.
        /// </summary>
        private void DrawAccessibilityStatus()
        {
            GUILayout.BeginVertical(GUI.skin.box);
            
            // Setup styles
            GUIStyle statusStyle = new GUIStyle(GUI.skin.label);
            statusStyle.fontSize = 14;
            statusStyle.fontStyle = FontStyle.Bold;
            
            if (accessibilityServiceEnabled)
            {
                // Accessibility service is enabled - show status
                statusStyle.normal.textColor = Color.green;
                GUILayout.Label("✓ Accessibility Service: ENABLED", statusStyle);
                GUILayout.Label("System controls can now be used");
            }
            else
            {
                // Accessibility service is disabled - show status and enable button
                statusStyle.normal.textColor = Color.red;
                GUILayout.Label("✗ Accessibility Service: DISABLED", statusStyle);
                GUILayout.Label("System controls require Accessibility Service permissions");
                
                // Add enable button with distinctive style
                GUIStyle enableStyle = new GUIStyle(GUI.skin.button);
                enableStyle.fontStyle = FontStyle.Bold;
                
                if (GUILayout.Button("ENABLE ACCESSIBILITY SERVICE", enableStyle, GUILayout.Height(50)))
                {
                    if (localControl != null)
                    {
                        localControl.OpenAccessibilitySettings();
                        logger.Log("Opening Accessibility Settings...");
                    }
                    else
                    {
                        logger.LogError("LocalControl not available");
                    }
                }
            }
            
            GUILayout.EndVertical();
        }
        
    }
}
