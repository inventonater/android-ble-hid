using System;
using Best.MQTT.Packets;
using Newtonsoft.Json;
using UnityEngine;

namespace Inventonater
{
    [Serializable]
    public class MqttTopic<TPayload>
    {
        [SerializeField] private InventoMqttClient _client;
        [SerializeField] private string _topic;
        [SerializeField] private QoSLevels _qos;
        [SerializeField] private bool _retain;

        public event Action<TPayload> WhenMessageReceived = delegate { };
        private void Handler(TPayload payload) => WhenMessageReceived(payload);

        public MqttTopic(InventoMqttClient client, string topic, QoSLevels qos = QoSLevels.AtLeastOnceDelivery, bool retain = false)
        {
            _client = client;
            _retain = retain;
            _qos = qos;
            _topic = topic;
            _client.Subscribe<TPayload>(_topic, Handler);
        }

        ~MqttTopic()
        {
            if(_client != null) _client.Unsubscribe<TPayload>(_topic, Handler);
            _client = null;
            _topic = null;
            _qos = QoSLevels.AtLeastOnceDelivery;
            _topic = null;
        }

        public void Publish(TPayload payload) => Publish(payload, _qos);
        public void Publish(TPayload payload, QoSLevels qos) => _client.Publish(_topic, payload == null ? "" : JsonConvert.SerializeObject(payload), qos, _retain);
    }
}
