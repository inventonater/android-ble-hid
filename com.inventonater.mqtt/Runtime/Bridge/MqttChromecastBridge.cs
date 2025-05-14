using System;
using Cysharp.Threading.Tasks;

namespace Inventonater
{
    [Serializable]
    public class MqttChromecastBridge
    {
        private const string DefaultCommandTopic = "chromecast/dpad";

        [MappableAction(id: MappableActionId.Select)] public void Select() => Publish("DPAD_CENTER");
        [MappableAction(id: MappableActionId.Back)] public void Back() => Publish("BACK");
        [MappableAction(id: MappableActionId.Home)] public void Home() => Publish("HOME");

        [MappableAction(id: MappableActionId.Up)] public void Up() => Publish("DPAD_UP");
        [MappableAction(id: MappableActionId.Down)] public void Down() => Publish("DPAD_DOWN");
        [MappableAction(id: MappableActionId.Left)] public void Left() => Publish("DPAD_LEFT");
        [MappableAction(id: MappableActionId.Right)] public void Right() => Publish("DPAD_RIGHT");

        [MappableAction(id: MappableActionId.Chirp)]
        public async UniTask Chirp()
        {
            const int blinkOffMs = 400;
            Right();
            await UniTask.Delay(blinkOffMs);
            Left();
            await UniTask.Delay(blinkOffMs);
            Down();
            await UniTask.Delay(blinkOffMs);
            Up();
        }

        private void Publish(string dpadCenter) => _chromecast.Publish(new ChromecastDpadTopic.Payload(dpadCenter));

        private readonly ChromecastDpadTopic _chromecast;
        public MqttChromecastBridge(InventoMqttClient client, string commandTopic = DefaultCommandTopic)
        {
            _chromecast = new ChromecastDpadTopic(client, commandTopic);
        }

        public class ChromecastDpadTopic : MqttTopic<ChromecastDpadTopic.Payload>
        {
            [Serializable]
            public readonly struct Payload
            {
                public readonly string key;
                public Payload(string key) => this.key = key;
            }

            public ChromecastDpadTopic(InventoMqttClient client, string commandTopic) : base(client, commandTopic)
            {
            }
        }
    }
}
