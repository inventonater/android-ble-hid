using System;
using Best.MQTT.Packets;

namespace Inventonater
{
    [Serializable]
    public class MqttActiveMappingCommandTopic : MqttTopic<RemoteMapping>
    {
        public const string DefaultTopic = "mappings/active/command";
        public const QoSLevels DefaultQos = QoSLevels.ExactlyOnceDelivery;

        public MqttActiveMappingCommandTopic(InventoMqttClient mqttClient) : base(mqttClient, DefaultTopic, DefaultQos, retain: false)
        {
        }
    }
}