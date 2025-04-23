using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    [Serializable]
    public class MediaBridge
    {
        private JavaBridge _java;
        public MediaBridge(JavaBridge java) => _java = java;

        public bool PlayPause() => _java.Call<bool>("playPause");
        public bool NextTrack() => _java.Call<bool>("nextTrack");
        public bool PreviousTrack() => _java.Call<bool>("previousTrack");
        public bool VolumeUp() => _java.Call<bool>("volumeUp");
        public bool VolumeDown() => _java.Call<bool>("volumeDown");
        public bool Mute() => _java.Call<bool>("mute");
    }
}
