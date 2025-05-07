using System;
using System.Collections.Generic;
using System.Linq;
using Best.MQTT.Packets;

namespace Inventonater
{
    [Serializable]
    public class MqttMappingsStateTopic : MqttTopic<RemoteMapping[]>
    {
        public const string DefaultTopic = "mappings/state";
        public const QoSLevels DefaultQos = QoSLevels.ExactlyOnceDelivery;

        public MqttMappingsStateTopic(InventoMqttClient mqttClient) : base(mqttClient, DefaultTopic, DefaultQos, retain: true)
        {
        }
    }
}
