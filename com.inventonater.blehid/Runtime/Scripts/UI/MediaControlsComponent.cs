using UnityEngine;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// UI component for media controls (play/pause, volume, etc.)
    /// </summary>
    public class MediaControlsComponent : UIComponent
    {
        public override void DrawUI()
        {
            // Media control buttons row 1
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Previous", GUILayout.Height(80)))
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Previous track pressed");
                else
                    BleHidManager.PreviousTrack();
            }

            if (GUILayout.Button("Play/Pause", GUILayout.Height(80)))
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Play/Pause pressed");
                else
                    BleHidManager.PlayPause();
            }

            if (GUILayout.Button("Next", GUILayout.Height(80)))
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Next track pressed");
                else
                    BleHidManager.NextTrack();
            }
            GUILayout.EndHorizontal();

            // Media control buttons row 2
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Volume Down", GUILayout.Height(80)))
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Volume down pressed");
                else
                    BleHidManager.VolumeDown();
            }

            if (GUILayout.Button("Mute", GUILayout.Height(80)))
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Mute pressed");
                else
                    BleHidManager.Mute();
            }

            if (GUILayout.Button("Volume Up", GUILayout.Height(80)))
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Volume up pressed");
                else
                    BleHidManager.VolumeUp();
            }
            GUILayout.EndHorizontal();
        }
    }
}
