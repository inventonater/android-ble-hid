using UnityEngine;
using System;

namespace Inventonater
{
    /// <summary>
    /// UI component for media controls (play/pause, volume, etc.)
    /// </summary>
    public class MediaDeviceUI : SectionUI
    {
        private readonly string[] _playbackLabels;
        private readonly Action[] _playbackActions;
        private readonly string[] _playbackMessages;
        private readonly string[] _volumeLabels;
        private readonly Action[] _volumeActions;
        private readonly string[] _volumeMessages;
        public const string Name = "Media";
        public override string TabName => Name;
        private MediaBridge Media => BleHidManager.Instance.BleBridge.Media;

        public MediaDeviceUI()
        {
            // Media control buttons row 1 - using the new ActionButtonRow helper
            _playbackLabels = new[] { "Previous", "Play/Pause", "Next" };
            _playbackActions = new Action[] { Media.PreviousTrack, Media.PlayPause, Media.NextTrack };
            _playbackMessages = new[] { "Previous track pressed", "Play/Pause pressed", "Next track pressed" };

            // Media control buttons row 2 - using the new ActionButtonRow helper
            _volumeLabels = new[] { "Volume Down", "Mute", "Volume Up" };
            _volumeActions = new Action[] { Media.VolumeDown, Media.Mute, Media.VolumeUp };
            _volumeMessages = new[] { "Volume down pressed", "Mute pressed", "Volume up pressed" };
        }

        public override void Update()
        {
        }

        public override void DrawUI()
        {
            UIHelper.BeginSection("Media Controls");
            UIHelper.ActionButtonRow(_playbackLabels, _playbackActions, _playbackMessages, UIHelper.LargeButtonOptions);
            UIHelper.ActionButtonRow(_volumeLabels, _volumeActions, _volumeMessages, UIHelper.LargeButtonOptions);
            UIHelper.EndSection();
        }
    }
}
