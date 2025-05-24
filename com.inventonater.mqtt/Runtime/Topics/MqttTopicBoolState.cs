using System;
using Best.MQTT.Packets;

namespace Inventonater
{
    [Serializable]
    public class MqttTopicBoolState : MqttTopic<bool>
    {
        public const QoSLevels DefaultQos = QoSLevels.ExactlyOnceDelivery;

        public MqttTopicBoolState(InventoMqttClient mqttClient, string topic) : base(mqttClient, topic, DefaultQos, retain: true)
        {
        }
    }
}