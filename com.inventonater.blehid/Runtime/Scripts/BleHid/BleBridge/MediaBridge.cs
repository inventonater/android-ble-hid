using System;
using Cysharp.Threading.Tasks;
using UnityEngine;

namespace Inventonater
{
    [Serializable]
    public class MediaBridge
    {
        private JavaBridge _java;
        public MediaBridge(JavaBridge java) => _java = java;

        [MappableAction(id: MappableActionId.PlayToggle, displayName: "Play/Pause", description: "Toggle media playback between play and pause states")]
        public void PlayPause() => _java.Call("playPause");
        
        [MappableAction(id: MappableActionId.NextTrack, displayName: "Next Track", description: "Skip to the next track")]
        public void NextTrack() => _java.Call("nextTrack");
        
        [MappableAction(id: MappableActionId.PreviousTrack, displayName: "Previous Track", description: "Go back to the previous track")]
        public void PreviousTrack() => _java.Call("previousTrack");
        
        [MappableAction(id: MappableActionId.VolumeUp, displayName: "Volume Up", description: "Increase the volume")]
        public void VolumeUp() => _java.Call("volumeUp");
        
        [MappableAction(id: MappableActionId.VolumeDown, displayName: "Volume Down", description: "Decrease the volume")]
        public void VolumeDown() => _java.Call("volumeDown");
        
        [MappableAction(id: MappableActionId.MuteToggle, displayName: "Mute", description: "Mute or unmute the audio")]
        public void Mute() => _java.Call("mute");

        [MappableAction(id: MappableActionId.Chirp)]
        public async UniTask Chirp()
        {
            const int blinkOffMs = 110;
            const int blinkOnMs = 180;
            Mute();
            await UniTask.Delay(blinkOffMs);
            Mute();
            await UniTask.Delay(blinkOnMs);
            Mute();
            await UniTask.Delay(blinkOffMs);
            Mute();
        }
    }
}
