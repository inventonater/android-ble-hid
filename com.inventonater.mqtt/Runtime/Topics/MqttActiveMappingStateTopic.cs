using System;
using Best.MQTT.Packets;

namespace Inventonater
{
    [Serializable]
    public class MqttActiveMappingStateTopic : MqttTopic<RemoteMapping>
    {
        public const string DefaultTopic = "mappings/active/state";
        public const QoSLevels DefaultQos = QoSLevels.ExactlyOnceDelivery;

        public MqttActiveMappingStateTopic(InventoMqttClient mqttClient) : base(mqttClient, DefaultTopic, DefaultQos, retain: false)
        {
        }
    }
}
