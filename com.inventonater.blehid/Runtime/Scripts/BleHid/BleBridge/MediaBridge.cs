using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    [Serializable]
    public class MediaBridge
    {
        private JavaBridge _java;
        public MediaBridge(JavaBridge java) => _java = java;

        public void PlayPause() => _java.Call("playPause");
        public void NextTrack() => _java.Call("nextTrack");
        public void PreviousTrack() => _java.Call("previousTrack");
        public void VolumeUp() => _java.Call("volumeUp");
        public void VolumeDown() => _java.Call("volumeDown");
        public void Mute() => _java.Call("mute");
    }
}
