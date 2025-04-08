using UnityEngine;

namespace Inventonater.BleHid.UI.Panels.Remote
{
    /// <summary>
    /// Panel for remote media control functionality over BLE HID.
    /// </summary>
    public class RemoteMediaPanel : BaseBleHidPanel
    {
        public override bool RequiresConnectedDevice => true;
        
        protected override void DrawPanelContent()
        {
            GUILayout.Label("Media Controls", titleStyle);
            
            #if UNITY_EDITOR
            bool isEditorMode = true;
            #else
            bool isEditorMode = false;
            #endif
            
            // Basic media controls section
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Playback Controls", subtitleStyle);
            
            // Media controls row 1
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Previous", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Previous track pressed");
                }
                else if (bleHidManager != null)
                {
                    bleHidManager.PreviousTrack();
                    logger.Log("Sent previous track command");
                }
            }
            
            if (GUILayout.Button("Play/Pause", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Play/Pause pressed");
                }
                else if (bleHidManager != null)
                {
                    bleHidManager.PlayPause();
                    logger.Log("Sent play/pause command");
                }
            }
            
            if (GUILayout.Button("Next", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Next track pressed");
                }
                else if (bleHidManager != null)
                {
                    bleHidManager.NextTrack();
                    logger.Log("Sent next track command");
                }
            }
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            // Volume controls section
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Volume Controls", subtitleStyle);
            
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Volume Down", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Volume down pressed");
                }
                else if (bleHidManager != null)
                {
                    bleHidManager.VolumeDown();
                    logger.Log("Sent volume down command");
                }
            }
            
            if (GUILayout.Button("Mute", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Mute pressed");
                }
                else if (bleHidManager != null)
                {
                    bleHidManager.Mute();
                    logger.Log("Sent mute command");
                }
            }
            
            if (GUILayout.Button("Volume Up", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Volume up pressed");
                }
                else if (bleHidManager != null)
                {
                    bleHidManager.VolumeUp();
                    logger.Log("Sent volume up command");
                }
            }
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            // Advanced media controls
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Advanced Controls", subtitleStyle);
            
            // Function key row (F1-F6)
            GUILayout.BeginHorizontal();
            for (int i = 1; i <= 6; i++)
            {
                if (GUILayout.Button($"F{i}", GUILayout.Height(50)))
                {
                    if (isEditorMode)
                    {
                        logger.Log($"F{i} key pressed");
                    }
                    else if (bleHidManager != null)
                    {
                        byte keyCode = GetFunctionKeyCode(i);
                        if (keyCode > 0)
                        {
                            bleHidManager.SendKey(keyCode);
                            logger.Log($"Sent F{i} key command");
                        }
                    }
                }
            }
            GUILayout.EndHorizontal();
            
            // Function key row (F7-F12)
            GUILayout.BeginHorizontal();
            for (int i = 7; i <= 12; i++)
            {
                if (GUILayout.Button($"F{i}", GUILayout.Height(50)))
                {
                    if (isEditorMode)
                    {
                        logger.Log($"F{i} key pressed");
                    }
                    else if (bleHidManager != null)
                    {
                        byte keyCode = GetFunctionKeyCode(i);
                        if (keyCode > 0)
                        {
                            bleHidManager.SendKey(keyCode);
                            logger.Log($"Sent F{i} key command");
                        }
                    }
                }
            }
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
        }
        
        /// <summary>
        /// Get the key code for a function key.
        /// </summary>
        /// <param name="number">The function key number (1-12).</param>
        /// <returns>The key code for the function key.</returns>
        private byte GetFunctionKeyCode(int number)
        {
            switch (number)
            {
                case 1: return BleHidConstants.KEY_F1;
                case 2: return BleHidConstants.KEY_F2;
                case 3: return BleHidConstants.KEY_F3;
                case 4: return BleHidConstants.KEY_F4;
                case 5: return BleHidConstants.KEY_F5;
                case 6: return BleHidConstants.KEY_F6;
                case 7: return BleHidConstants.KEY_F7;
                case 8: return BleHidConstants.KEY_F8;
                case 9: return BleHidConstants.KEY_F9;
                case 10: return BleHidConstants.KEY_F10;
                case 11: return BleHidConstants.KEY_F11;
                case 12: return BleHidConstants.KEY_F12;
                default: return 0;
            }
        }
    }
}
