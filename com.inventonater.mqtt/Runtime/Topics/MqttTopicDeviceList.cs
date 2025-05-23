using System;
using System.Collections.Generic;
using System.Linq;
using Best.MQTT.Packets;

namespace Inventonater
{
    [Serializable]
    public class MqttTopicDeviceList : MqttTopic<RemoteDevice[]>
    {
        public const string DefaultTopic = "device/list/state";
        public const QoSLevels DefaultQos = QoSLevels.ExactlyOnceDelivery;

        public MqttTopicDeviceList(InventoMqttClient mqttClient) : base(mqttClient, DefaultTopic, DefaultQos, retain: true)
        {
        }
    }
}
