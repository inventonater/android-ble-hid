using System;
using Best.MQTT;
using Best.MQTT.Packets;
using Best.MQTT.Packets.Builders;
using Cysharp.Threading.Tasks;
using Newtonsoft.Json;
using UnityEngine;

namespace Inventonater
{
    public class InventoMqttClient : MonoBehaviour
    {
        [SerializeField] private string host = "192.168.10.7";
        [SerializeField] private int port = 1883;
        [SerializeField] private string userName = "inventonater";
        [SerializeField] private string password = "asdfasdf";

        [Serializable]
        public class MqttEntity<T>
        {
            [SerializeField] private string commandTopic;
            public MqttEntity(string commandTopic) => this.commandTopic = commandTopic;

            public void Publish(T payload) => Publish(commandTopic, JsonConvert.SerializeObject(payload));

            private void Publish(string topic, string message, QoSLevels qos = QoSLevels.AtLeastOnceDelivery)
            {
                if (_client == null)
                {
                    Debug.LogError($"No mqtt client has been initialized in InventoMqttClient for MqttEntity {commandTopic}");
                    return;
                }

                _client.CreateApplicationMessageBuilder(topic)
                    .WithQoS(qos)
                    .WithRetain(false)
                    .WithPayload(message)
                    .BeginPublish();
                Debug.Log($"{topic} {message}");
            }
        }

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.BeforeSceneLoad)]
        private static void StaticReload() => _client = null;
        private static MQTTClient _client;
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
            _client.OnStateChanged += HandleStateChanged;
            _client.BeginConnect(ConnectPacketBuilderCallback);
        }

        private void HandleConnected(MQTTClient client) => Debug.Log($"MqttClient Connected to MQTT: {client.State}");
        private void HandleDisconnected(MQTTClient client, DisconnectReasonCodes code, string reason) => Debug.Log($"MqttClient Disconnected: {reason}");
        private void HandleError(MQTTClient client, string reason) => Debug.LogError($"MqttClient MQTT Error: {reason}");

        private void HandleStateChanged(MQTTClient client, ClientStates oldState, ClientStates newState)
        {
            if (newState == ClientStates.Connected)
            {
                var subscribeTopic = "#";
                _client.CreateBulkSubscriptionBuilder()
                    .WithTopic(new SubscribeTopicBuilder(subscribeTopic)
                        // .WithMaximumQoS(qos)
                        // .WithAcknowledgementCallback(OnSubscriptionAcknowledgement)
                        .WithMessageCallback(OnApplicationMessage))
                    .BeginSubscribe();
                Debug.Log($"MqttClient Client state changed from {oldState} to {newState}");
            }
        }

        private ConnectPacketBuilder ConnectPacketBuilderCallback(MQTTClient client, ConnectPacketBuilder builder)
        {
            var session = SessionHelper.HasAny(client.Options.Host) ? SessionHelper.Get(client.Options.Host) : SessionHelper.CreateNullSession(client.Options.Host);
            builder.WithSession(session);
            if (!string.IsNullOrEmpty(userName)) builder.WithUserName(userName);
            if (!string.IsNullOrEmpty(password)) builder.WithPassword(password);
            builder.WithKeepAlive(60);
            return builder;
        }

        private void OnSubscriptionAcknowledgement(MQTTClient client, SubscriptionTopic topic, SubscribeAckReasonCodes reasonCode)
        {
            Debug.Log($"MqttClient OnSubscriptionAcknowledgement {topic} - {reasonCode}");
        }

        private void OnApplicationMessage(MQTTClient client, SubscriptionTopic topic, string topicName, ApplicationMessage applicationMessage)
        {
            // Debug.Log($"MqttClient OnApplicationMessage {topic} - {topicName} - {applicationMessage}");
        }

        private void OnDestroy()
        {
            Debug.Log($"MqttClient OnDestory");
            _client?.CreateDisconnectPacketBuilder().BeginDisconnect();
        }
    }
}
