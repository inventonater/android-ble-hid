using UnityEngine;
using System;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// UI component for media controls (play/pause, volume, etc.)
    /// </summary>
    public class MediaControlsComponent : UIComponent
    {
        public override void DrawUI()
        {
            UIHelper.BeginSection("Media Controls");
            
            // Media control buttons row 1 - using the new ActionButtonRow helper
            string[] playbackLabels = { "Previous", "Play/Pause", "Next" };
            Action[] playbackActions = {
                () => BleHidManager.PreviousTrack(),
                () => BleHidManager.PlayPause(),
                () => BleHidManager.NextTrack()
            };
            string[] playbackMessages = {
                "Previous track pressed",
                "Play/Pause pressed",
                "Next track pressed"
            };
            
            UIHelper.ActionButtonRow(
                playbackLabels,
                playbackActions,
                IsEditorMode,
                Logger,
                playbackMessages,
                UIHelper.LargeButtonOptions);
            
            // Media control buttons row 2 - using the new ActionButtonRow helper
            string[] volumeLabels = { "Volume Down", "Mute", "Volume Up" };
            Action[] volumeActions = {
                () => BleHidManager.VolumeDown(),
                () => BleHidManager.Mute(),
                () => BleHidManager.VolumeUp()
            };
            string[] volumeMessages = {
                "Volume down pressed",
                "Mute pressed",
                "Volume up pressed"
            };
            
            UIHelper.ActionButtonRow(
                volumeLabels,
                volumeActions,
                IsEditorMode,
                Logger,
                volumeMessages,
                UIHelper.LargeButtonOptions);
                
            UIHelper.EndSection();
        }
    }
}
