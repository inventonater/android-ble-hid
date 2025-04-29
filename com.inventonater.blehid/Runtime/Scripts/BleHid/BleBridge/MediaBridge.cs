using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    [Serializable]
    public class MediaBridge
    {
        private JavaBridge _java;
        public MediaBridge(JavaBridge java) => _java = java;

        public void PlayPause() => _java.Call<bool>("playPause");
        public void NextTrack() => _java.Call<bool>("nextTrack");
        public void PreviousTrack() => _java.Call<bool>("previousTrack");
        public void VolumeUp() => _java.Call<bool>("volumeUp");
        public void VolumeDown() => _java.Call<bool>("volumeDown");
        public void Mute() => _java.Call<bool>("mute");
    }
}
