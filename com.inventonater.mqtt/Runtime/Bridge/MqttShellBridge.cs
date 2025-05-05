using System;
using Cysharp.Threading.Tasks;

namespace Inventonater
{
    [Serializable]
    public class MqttShellBridge
    {
        [MappableAction(id: EInputAction.PrimaryPress)] public void PrimaryPress() => Publish(InputEvent.PrimaryPress);
        [MappableAction(id: EInputAction.PrimaryRelease)] public void PrimaryRelease() => Publish(InputEvent.PrimaryRelease);
        [MappableAction(id: EInputAction.SecondaryPress)] public void SecondaryPress() => Publish(InputEvent.SecondaryPress);
        [MappableAction(id: EInputAction.SecondaryRelease)] public void SecondaryRelease() => Publish(InputEvent.SecondaryRelease);
        [MappableAction(id: EInputAction.TertiaryPress)] public void TertiaryPress() => Publish(InputEvent.TertiaryPress);
        [MappableAction(id: EInputAction.TertiaryRelease)] public void TertiaryRelease() => Publish(InputEvent.TertiaryRelease);

        [MappableAction(id: EInputAction.Up)] public void Up() => Publish(InputEvent.Up);
        [MappableAction(id: EInputAction.Down)] public void Down() => Publish(InputEvent.Down);
        [MappableAction(id: EInputAction.Left)] public void Left() => Publish(InputEvent.Left);
        [MappableAction(id: EInputAction.Right)] public void Right() => Publish(InputEvent.Right);

        [MappableAction(id: EInputAction.Chirp)]
        public async UniTask Chirp()
        {
            await UniTask.Yield();
            LoggingManager.Instance.Log("Shell.Chirp");
        }

        private void Publish(InputEvent inputEvent)
        {
            _entity.Publish(new MqttShellEntity.Command(inputEvent: inputEvent));
        }

        private MqttShellEntity _entity = new("shell/nav");

        public class MqttShellEntity : InventoMqttClient.MqttEntity<MqttShellEntity.Command>
        {
            [Serializable]
            public class Command
            {
                public InputEvent inputEvent;
                public Command(InputEvent inputEvent)
                {
                    this.inputEvent = inputEvent;
                }
            }
            private readonly Command _value;

            public MqttShellEntity(string commandTopic) : base(commandTopic) { }
        }
    }
}
