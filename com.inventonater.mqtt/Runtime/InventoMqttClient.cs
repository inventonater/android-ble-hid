using System;
using Best.MQTT;
using Best.MQTT.Packets;
using Best.MQTT.Packets.Builders;
using Newtonsoft.Json;
using UnityEngine;

namespace Inventonater
{
    public class InventoMqttClient : MonoBehaviour
    {
        public enum ConnectionStatus
        {
            Disconnected,
            Connecting,
            Connected,
            Error
        }

        public class MqttMessage
        {
            public string Topic { get; set; }
            public string Payload { get; set; }
            public DateTime Timestamp { get; set; }
        }

        public ConnectionStatus Status { get; private set; } = ConnectionStatus.Disconnected;
        public MqttMessage LastReceivedMessage { get; private set; }
        [SerializeField] private string host = "";
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
            Status = ConnectionStatus.Connecting;
            try
            {
                if (string.IsNullOrEmpty(host))
                {
                    host = await HomeAssistantDiscovery.DiscoverHomeAssistantIpAsync();
                    if(string.IsNullOrEmpty(host)) 
                    {
                        Debug.LogError("Could not find MQTT host IP address");
                        Status = ConnectionStatus.Error;
                    }
                }

                _client = new MQTTClient(ConnectionOptions);
                _client.OnConnected += HandleConnected;
                _client.OnError += HandleError;
                _client.OnDisconnect += HandleDisconnected;
                _client.BeginConnect(ConnectPacketBuilderCallback);
            }
            catch (Exception e)
            {
                Status = ConnectionStatus.Error;
                LoggingManager.Instance.Exception(e);
            }
        }

        private Action _whenConnected = delegate{};

        public event Action WhenConnected
        {
            add
            {
                _whenConnected += value;
                if (_client is { State: ClientStates.Connected }) value();
            }
            remove => _whenConnected -= value;
        }

        public event Action WhenDisconnected = delegate { };

        private void HandleConnected(MQTTClient client)
        {
            Debug.Log($"MqttClient Connected to MQTT: {client.State}");
            Status = ConnectionStatus.Connected;
            _whenConnected();
        }

        private void HandleDisconnected(MQTTClient client, DisconnectReasonCodes code, string reason)
        {
            Debug.Log($"MqttClient Disconnected: {reason}");
            Status = ConnectionStatus.Disconnected;
            WhenDisconnected();
        }

        private void HandleError(MQTTClient client, string reason)
        {
            Debug.LogError($"MqttClient MQTT Error: {reason}");
            Status = ConnectionStatus.Error;
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

        public void Publish(string topic, string message, QoSLevels qos = QoSLevels.AtLeastOnceDelivery, bool retain = false)
        {
            string logMessage = $"Publish: {(retain ? "retain=true" : "")} {topic} {message}";
            if (_client == null)
            {
                LoggingManager.Instance.Error($"MQTT Client not ready for {logMessage}");
                return;
            }

            if (_client.State != ClientStates.Connected)
            {
                LoggingManager.Instance.Error($"Not Connected: {logMessage}");
                return;
            }
            _client.CreateApplicationMessageBuilder(topic)
                .WithQoS(qos)
                .WithRetain(retain)
                .WithPayload(message)
                .BeginPublish();

            if(verbose) Debug.Log(logMessage);
        }

        public void Subscribe<TPayload>(string subscriptionTopic, Action<TPayload> callback)
        {
            if (_client == null)
            {
                LoggingManager.Instance.Error($"MQTT Client not ready for Subscribe {subscriptionTopic}");
                return;
            }

            LoggingManager.Instance.Log($"Subscribing: {subscriptionTopic}");

            _client.CreateSubscriptionBuilder(subscriptionTopic).WithMessageCallback((client, topic, topicName, message) =>
            {
                string payload = System.Text.Encoding.UTF8.GetString(message.Payload.Data, message.Payload.Offset, message.Payload.Count);
                
                // Store the last received message
                LastReceivedMessage = new MqttMessage
                {
                    Topic = topicName,
                    Payload = payload,
                    Timestamp = DateTime.Now
                };
                
                if (verbose) LoggingManager.Instance.Log($"{topic} {payload}");
                TPayload result = default;
                bool deserializationSuccessful = false;

                try
                {
                    result = JsonConvert.DeserializeObject<TPayload>(payload);
                    deserializationSuccessful = true;
                }
                catch (Exception ex)
                {
                    LoggingManager.Instance.Error($"Failed to deserialize message for topic {subscriptionTopic} {payload} \n{ex.Message}");
                    if (ex.InnerException != null) LoggingManager.Instance.Error($"Inner Exception: {ex.InnerException.Message}");
                }

                if (!deserializationSuccessful)
                {
                    LoggingManager.Instance.Error($"Subscribing FAILED: {subscriptionTopic}");
                    return;
                }

                try
                {
                    callback(result);
                }
                catch (Exception callbackEx)
                {
                    LoggingManager.Instance.Error($"Error executing callback for topic {subscriptionTopic}. \n{callbackEx.Message}\n{callbackEx.StackTrace}");
                    if (callbackEx.InnerException != null) LoggingManager.Instance.Error($"Inner Exception: {callbackEx.InnerException.Message}");
                }
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
