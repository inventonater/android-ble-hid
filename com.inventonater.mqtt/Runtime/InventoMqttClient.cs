using System;
using Best.MQTT;
using Best.MQTT.Packets;
using Best.MQTT.Packets.Builders;
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

        public MqttTopic(InventoMqttClient client, string topic, QoSLevels qos = QoSLevels.AtLeastOnceDelivery, bool retain = false)
        {
            _client = client;
            _retain = retain;
            _qos = qos;
            _topic = topic;
        }

        public void Publish(TPayload payload) => _client.Publish(_topic, JsonConvert.SerializeObject(payload), _qos, _retain);
        public void Publish(TPayload payload, QoSLevels qos) => _client.Publish(_topic, JsonConvert.SerializeObject(payload), qos, _retain);
        public void Subscribe(Action<TPayload> handler) => _client.Subscribe(_topic, handler);
        public void Unsubscribe(Action<TPayload> handler) => _client.Unsubscribe(_topic, handler);
    }

    public class InventoMqttClient : MonoBehaviour
    {
        [SerializeField] private string host = "192.168.10.7";
        [SerializeField] private int port = 1883;
        [SerializeField] private string userName = "inventonater";
        [SerializeField] private string password = "asdfasdf";
        [SerializeField] private bool verbose = true;

        // [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.BeforeSceneLoad)]
        // private static void StaticReload() => _client = null;
        private MQTTClient _client;
        private QoSLevels qosLevels = QoSLevels.ExactlyOnceDelivery;

        private ConnectionOptions ConnectionOptions => new()
        {
            Host = host,
            Port = port,
            Transport = SupportedTransports.TCP,
            UseTLS = false,
            Path = "/mqtt",
            ProtocolVersion = SupportedProtocolVersions.MQTT_5_0,
        };

        public async void Awake()
        {
            _client = new MQTTClient(ConnectionOptions);
            _client.OnConnected += HandleConnected;
            _client.OnError += HandleError;
            _client.OnDisconnect += HandleDisconnected;
            _client.BeginConnect(ConnectPacketBuilderCallback);
        }

        private void HandleConnected(MQTTClient client) => Debug.Log($"MqttClient Connected to MQTT: {client.State}");
        private void HandleDisconnected(MQTTClient client, DisconnectReasonCodes code, string reason) => Debug.Log($"MqttClient Disconnected: {reason}");
        private void HandleError(MQTTClient client, string reason) => Debug.LogError($"MqttClient MQTT Error: {reason}");

        private ConnectPacketBuilder ConnectPacketBuilderCallback(MQTTClient client, ConnectPacketBuilder builder)
        {
            var session = SessionHelper.HasAny(client.Options.Host) ? SessionHelper.Get(client.Options.Host) : SessionHelper.CreateNullSession(client.Options.Host);
            builder.WithSession(session);
            if (!string.IsNullOrEmpty(userName)) builder.WithUserName(userName);
            if (!string.IsNullOrEmpty(password)) builder.WithPassword(password);
            builder.WithKeepAlive(60);
            return builder;
        }

        public void Publish(string topic, string message, QoSLevels qos = QoSLevels.AtLeastOnceDelivery, bool retain = false)
        {
            _client.CreateApplicationMessageBuilder(topic)
                .WithQoS(qos)
                .WithRetain(retain)
                .WithPayload(message)
                .BeginPublish();
            Debug.Log($"{topic} {message}");
        }

        public void Subscribe<TPayload>(string subscriptionTopic, Action<TPayload> callback)
        {
            _client.CreateSubscriptionBuilder(subscriptionTopic).WithMessageCallback((client, topic, topicName, message) =>
            {
                try
                {
                    string payload = System.Text.Encoding.UTF8.GetString(message.Payload.Data, message.Payload.Offset, message.Payload.Count);
                    if (verbose) LoggingManager.Instance.Log($"{topic} {payload}");
                    callback(JsonConvert.DeserializeObject<TPayload>(payload));
                }
                catch (Exception ex) { Debug.LogError($"Failed to process message for topic {subscriptionTopic}: {ex.Message}"); }
            }).BeginSubscribe();
        }

        public void Unsubscribe<TPayload>(string topic, Action<TPayload> handler)
        {
            Debug.Log("TODO confirm double subscription, unsubscribe works properly?");
            _client.CreateUnsubscribePacketBuilder(topic).BeginUnsubscribe();
        }

        private void OnDestroy()
        {
            Debug.Log($"MqttClient OnDestory");
            _client?.CreateDisconnectPacketBuilder().BeginDisconnect();
        }
    }
}
