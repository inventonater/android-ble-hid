using System;
using Cysharp.Threading.Tasks;

namespace Inventonater
{
    [Serializable]
    public class MqttSpotifyBridge
    {
        [MappableAction(id: MappableActionId.PlayToggle, displayName: "Play/Pause", description: "Toggle media playback between play and pause states")]
        public void PlayPause() => _spotify.Call(MediaPayload.PlayToggle);

        [MappableAction(id: MappableActionId.NextTrack, displayName: "Next Track", description: "Skip to the next track")]
        public void NextTrack() => _spotify.Call(MediaPayload.Next);

        [MappableAction(id: MappableActionId.PreviousTrack, displayName: "Previous Track", description: "Go back to the previous track")]
        public void PreviousTrack() => _spotify.Call(MediaPayload.Previous);

        [MappableAction(id: MappableActionId.VolumeUp, displayName: "Volume Up", description: "Increase the volume")]
        public void VolumeUp() => _spotify.Call(MediaPayload.VolumeUp);

        [MappableAction(id: MappableActionId.VolumeDown, displayName: "Volume Down", description: "Decrease the volume")]
        public void VolumeDown() => _spotify.Call(MediaPayload.VolumeDown);

        // private bool _previousMute;
        // [MappableAction(id: EInputAction.MuteToggle, displayName: "Mute", description: "Mute or unmute the audio")]
        // public void Mute()
        // {
        //     _previousMute = !_previousMute;
        //     _spotify.Call(_previousMute ? "MUTE_ON" : "MUTE_OFF");
        // }

        [MappableAction(id: MappableActionId.Chirp)]
        public async UniTask Chirp()
        {
            const int blinkOffMs = 110;
            const int blinkOnMs = 180;
            // PlayPause();
            // await UniTask.Delay(blinkOffMs);
            // PlayPause();
            // await UniTask.Delay(blinkOnMs);
            // PlayPause();
            // await UniTask.Delay(blinkOffMs);
            // PlayPause();
        }

        private const string DefaultCommandTopic = "spotify/media/command";
        private readonly SpotifyCommandTopic _spotify;
        public MqttSpotifyBridge(InventoMqttClient client, string commandTopic = DefaultCommandTopic)
        {
            _spotify = new SpotifyCommandTopic(client, commandTopic);
        }

        public class SpotifyCommandTopic : MqttTopic<MediaPayload>
        {

            public SpotifyCommandTopic(InventoMqttClient client, string commandTopic) : base(client, commandTopic)
            {
            }

            public void Call(MediaPayload msg) => Publish(msg);
        }
    }
}
