using System;

namespace Inventonater
{
    [Serializable]
    public class MediaPayload
    {
        public readonly string key;
        public MediaPayload(string msg) => key = msg;
        public static readonly MediaPayload PlayToggle = new MediaPayload("PLAY_TOGGLE");
        public static readonly MediaPayload Next = new MediaPayload("NEXT");
        public static readonly MediaPayload Previous = new MediaPayload("PREVIOUS");
        public static readonly MediaPayload VolumeUp = new MediaPayload("VOLUME_UP");
        public static readonly MediaPayload VolumeDown = new MediaPayload("VOLUME_DOWN");
        public static readonly MediaPayload MuteOn = new MediaPayload("MUTE_ON");
        public static readonly MediaPayload MuteOff = new MediaPayload("MUTE_OFF");
    }
}
