using System;
using Best.MQTT.Packets;

namespace Inventonater
{
    [Serializable]
    public class MqttTopicDeviceActiveState : MqttTopic<RemoteDevice>
    {
        public const string DefaultTopic = "device/active/state";
        public const QoSLevels DefaultQos = QoSLevels.ExactlyOnceDelivery;

        public MqttTopicDeviceActiveState(InventoMqttClient mqttClient) : base(mqttClient, DefaultTopic, DefaultQos, retain: true)
        {
        }
    }
}
