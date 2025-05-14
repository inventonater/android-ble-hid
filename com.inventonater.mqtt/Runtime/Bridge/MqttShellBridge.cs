using System;
using Best.MQTT.Packets;
using Cysharp.Threading.Tasks;

namespace Inventonater
{
    [Serializable]
    public class MqttShellBridge : MqttTopic<ButtonEvent>
    {
        public const string DefaultTopic = "shell/nav";
        public const QoSLevels DefaultQos = QoSLevels.ExactlyOnceDelivery;
        public MqttShellBridge(InventoMqttClient mqttClient) : base(mqttClient, DefaultTopic, DefaultQos) { }

        [MappableAction(id: MappableActionId.PrimaryPress)] public void PrimaryPress() => Publish(ButtonEvent.PrimaryPress);
        [MappableAction(id: MappableActionId.PrimaryRelease)] public void PrimaryRelease() => Publish(ButtonEvent.PrimaryRelease);
        [MappableAction(id: MappableActionId.Select)] public void Select() => Publish(ButtonEvent.PrimarySingleTap);
        [MappableAction(id: MappableActionId.Back)] public void Back() => Publish(ButtonEvent.SecondarySingleTap);

        [MappableAction(id: MappableActionId.SecondaryPress)] public void SecondaryPress() => Publish(ButtonEvent.SecondaryPress);
        [MappableAction(id: MappableActionId.SecondaryRelease)] public void SecondaryRelease() => Publish(ButtonEvent.SecondaryRelease);
        [MappableAction(id: MappableActionId.TertiaryPress)] public void TertiaryPress() => Publish(ButtonEvent.TertiaryPress);
        [MappableAction(id: MappableActionId.TertiaryRelease)] public void TertiaryRelease() => Publish(ButtonEvent.TertiaryRelease);

        [MappableAction(id: MappableActionId.Up)] public void Up() => Publish(ButtonEvent.Up);
        [MappableAction(id: MappableActionId.Down)] public void Down() => Publish(ButtonEvent.Down);
        [MappableAction(id: MappableActionId.Left)] public void Left() => Publish(ButtonEvent.Left);
        [MappableAction(id: MappableActionId.Right)] public void Right() => Publish(ButtonEvent.Right);

        [MappableAction(id: MappableActionId.Chirp)]
        public async UniTask Chirp()
        {
            await UniTask.Yield();
            LoggingManager.Instance.Log("Shell.Chirp");
        }
    }
}
