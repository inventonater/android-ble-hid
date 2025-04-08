using System;
using UnityEngine;

namespace Inventonater.BleHid.UI.Panels.Local
{
    /// <summary>
    /// Panel for local navigation control functionality.
    /// Supports directional controls and system navigation.
    /// </summary>
    public class LocalNavigationPanel : BaseBleHidPanel
    {
        private BleHidLocalControl localControl;
        
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
            GUILayout.Label("Navigation Controls", titleStyle);
            
            #if UNITY_EDITOR
            bool isEditorMode = true;
            #else
            bool isEditorMode = false;
            #endif
            
            // System navigation buttons
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("System Navigation", subtitleStyle);
            
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Back", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Back button pressed");
                }
                else if (localControl != null)
                {
                    localControl.Navigate(BleHidLocalControl.NavigationDirection.Back);
                    logger.Log("Sent back command");
                }
            }
            
            if (GUILayout.Button("Home", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Home button pressed");
                }
                else if (localControl != null)
                {
                    localControl.Navigate(BleHidLocalControl.NavigationDirection.Home);
                    logger.Log("Sent home command");
                }
            }
            
            if (GUILayout.Button("Recents", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Recents button pressed");
                }
                else if (localControl != null)
                {
                    localControl.Navigate(BleHidLocalControl.NavigationDirection.Recents);
                    logger.Log("Sent recents command");
                }
            }
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            // Directional pad
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Directional Controls", subtitleStyle);
            
            // Up button row
            GUILayout.BeginHorizontal();
            GUILayout.FlexibleSpace();
            if (GUILayout.Button("Up", GUILayout.Height(60), GUILayout.Width(Screen.width / 3)))
            {
                if (isEditorMode)
                {
                    logger.Log("Up button pressed");
                }
                else if (localControl != null)
                {
                    localControl.Navigate(BleHidLocalControl.NavigationDirection.Up);
                    logger.Log("Sent up command");
                }
            }
            GUILayout.FlexibleSpace();
            GUILayout.EndHorizontal();
            
            // Left, Down, Right row
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Left", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Left button pressed");
                }
                else if (localControl != null)
                {
                    localControl.Navigate(BleHidLocalControl.NavigationDirection.Left);
                    logger.Log("Sent left command");
                }
            }
            
            if (GUILayout.Button("Down", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Down button pressed");
                }
                else if (localControl != null)
                {
                    localControl.Navigate(BleHidLocalControl.NavigationDirection.Down);
                    logger.Log("Sent down command");
                }
            }
            
            if (GUILayout.Button("Right", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Right button pressed");
                }
                else if (localControl != null)
                {
                    localControl.Navigate(BleHidLocalControl.NavigationDirection.Right);
                    logger.Log("Sent right command");
                }
            }
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            // Quick action buttons section
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Quick Actions", subtitleStyle);
            
            // Quick action row 1
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Split Screen", GUILayout.Height(50)))
            {
                logger.Log("Split screen toggle (not implemented)");
            }
            
            if (GUILayout.Button("Screenshot", GUILayout.Height(50)))
            {
                logger.Log("Screenshot capture (not implemented)");
            }
            GUILayout.EndHorizontal();
            
            // Quick action row 2
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Notifications", GUILayout.Height(50)))
            {
                logger.Log("Pull down notifications (not implemented)");
            }
            
            if (GUILayout.Button("Settings", GUILayout.Height(50)))
            {
                logger.Log("Open settings (not implemented)");
            }
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
        }
    }
}
