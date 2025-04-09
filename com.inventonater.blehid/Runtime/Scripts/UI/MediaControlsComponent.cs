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
            
            // Use ActionButton helper for simplified button creation with editor mode handling
            ActionButton("Previous", 
                () => BleHidManager.PreviousTrack(), 
                "Previous track pressed", 
                new GUILayoutOption[] { GUILayout.Height(80) });
                
            ActionButton("Play/Pause", 
                () => BleHidManager.PlayPause(), 
                "Play/Pause pressed", 
                new GUILayoutOption[] { GUILayout.Height(80) });
                
            ActionButton("Next", 
                () => BleHidManager.NextTrack(), 
                "Next track pressed", 
                new GUILayoutOption[] { GUILayout.Height(80) });
                
            GUILayout.EndHorizontal();

            // Media control buttons row 2
            GUILayout.BeginHorizontal();
            
            ActionButton("Volume Down", 
                () => BleHidManager.VolumeDown(), 
                "Volume down pressed", 
                new GUILayoutOption[] { GUILayout.Height(80) });
                
            ActionButton("Mute", 
                () => BleHidManager.Mute(), 
                "Mute pressed", 
                new GUILayoutOption[] { GUILayout.Height(80) });
                
            ActionButton("Volume Up", 
                () => BleHidManager.VolumeUp(), 
                "Volume up pressed", 
                new GUILayoutOption[] { GUILayout.Height(80) });
                
            GUILayout.EndHorizontal();
        }
    }
}
