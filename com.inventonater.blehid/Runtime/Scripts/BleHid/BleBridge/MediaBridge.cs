using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    [Serializable]
    public class MediaBridge
    {
        private JavaBridge _java;
        public MediaBridge(JavaBridge java) => _java = java;

        [MappableAction(id: EInputAction.MediaPlayPause, displayName: "Play/Pause", description: "Toggle media playback between play and pause states")]
        public void PlayPause() => _java.Call("playPause");
        
        [MappableAction(id: EInputAction.MediaNextTrack, displayName: "Next Track", description: "Skip to the next track")]
        public void NextTrack() => _java.Call("nextTrack");
        
        [MappableAction(id: EInputAction.MediaPreviousTrack, displayName: "Previous Track", description: "Go back to the previous track")]
        public void PreviousTrack() => _java.Call("previousTrack");
        
        [MappableAction(id: EInputAction.MediaVolumeUp, displayName: "Volume Up", description: "Increase the volume")]
        public void VolumeUp() => _java.Call("volumeUp");
        
        [MappableAction(id: EInputAction.MediaVolumeDown, displayName: "Volume Down", description: "Decrease the volume")]
        public void VolumeDown() => _java.Call("volumeDown");
        
        [MappableAction(id: EInputAction.MediaMute, displayName: "Mute", description: "Mute or unmute the audio")]
        public void Mute() => _java.Call("mute");
    }
}
