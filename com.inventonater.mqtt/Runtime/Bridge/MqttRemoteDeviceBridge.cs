using System;
using Best.MQTT.Packets;

namespace Inventonater
{
    [Serializable]
    public class MqttRemoteDeviceBridge : MqttTopic<EActiveDeviceType>
    {
        public const string DefaultTopic = "devices/active";
        public const QoSLevels DefaultQos = QoSLevels.ExactlyOnceDelivery;

        public MqttRemoteDeviceBridge(InventoMqttClient mqttClient) : base(mqttClient, DefaultTopic, DefaultQos)
        {
        }
    }
}
