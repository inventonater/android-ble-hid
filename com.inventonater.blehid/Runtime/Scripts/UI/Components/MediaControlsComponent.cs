using UnityEngine;
using System;
using Inventonater.BleHid;

namespace Inventonater.BleHid
{
    /// <summary>
    /// UI component for media controls (play/pause, volume, etc.)
    /// </summary>
    public class MediaControlsComponent : UIComponent
    {
        public const string Name = "Media";
        public override string TabName => Name;
        private MediaBridge Media => BleHidManager.BleBridge.Media;

        public override void ComponentShown() { }
        public override void ComponentHidden() { }
        public override void Update(){}

        public override void DrawUI()
        {
            UIHelper.BeginSection("Media Controls");
            
            // Media control buttons row 1 - using the new ActionButtonRow helper
            string[] playbackLabels = { "Previous", "Play/Pause", "Next" };
            Action[] playbackActions = {
                () => Media.PreviousTrack(),
                () => Media.PlayPause(),
                () => Media.NextTrack()
            };
            string[] playbackMessages = {
                "Previous track pressed",
                "Play/Pause pressed",
                "Next track pressed"
            };
            
            UIHelper.ActionButtonRow(
                playbackLabels,
                playbackActions,
                playbackMessages,
                UIHelper.LargeButtonOptions);
            
            // Media control buttons row 2 - using the new ActionButtonRow helper
            string[] volumeLabels = { "Volume Down", "Mute", "Volume Up" };
            Action[] volumeActions = {
                () => Media.VolumeDown(),
                () => Media.Mute(),
                () => Media.VolumeUp()
            };
            string[] volumeMessages = {
                "Volume down pressed",
                "Mute pressed",
                "Volume up pressed"
            };
            
            UIHelper.ActionButtonRow(
                volumeLabels,
                volumeActions,
                volumeMessages,
                UIHelper.LargeButtonOptions);
                
            UIHelper.EndSection();
        }
    }
}
