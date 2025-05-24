using System;
using Best.MQTT.Packets;
using UnityEngine;

namespace Inventonater
{
    [Serializable]
    public class MqttTopicMessageIncomingCommand : MqttTopic<MqttTopicMessageIncomingCommand.Payload>
    {
        [Serializable]
        public class Payload
        {
            public string name;
            public string text;

            public Payload(string name, string text)
            {
                this.name = name;
                this.text = text;
            }
        }

        public const string DefaultTopic = "message/incoming/command";
        public const QoSLevels DefaultQos = QoSLevels.ExactlyOnceDelivery;

        public MqttTopicMessageIncomingCommand(InventoMqttClient mqttClient) : base(mqttClient, DefaultTopic, DefaultQos, retain: false)
        {
        }
    }
}
