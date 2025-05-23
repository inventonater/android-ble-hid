using System;
using Best.MQTT.Packets;

namespace Inventonater
{
    [Serializable]
    public class MqttTopicDeviceActiveCommand : MqttTopic<RemoteDevice>
    {
        public const string DefaultTopic = "device/active/command";
        public const QoSLevels DefaultQos = QoSLevels.ExactlyOnceDelivery;

        public MqttTopicDeviceActiveCommand(InventoMqttClient mqttClient) : base(mqttClient, DefaultTopic, DefaultQos, retain: false)
        {
        }
    }
}
