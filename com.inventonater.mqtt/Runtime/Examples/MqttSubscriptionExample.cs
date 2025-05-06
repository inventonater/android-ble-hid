using System;
using UnityEngine;

namespace Inventonater
{
    /// <summary>
    /// Example class demonstrating how to use MqttEntity with both publishing and subscribing
    /// using the native MQTT subscription system
    /// </summary>
    public class MqttSubscriptionExample : MonoBehaviour
    {
        // Define a message type for our MQTT communication
        [Serializable]
        public class TemperatureMessage
        {
            public float temperature;
            public string unit = "C";
            public string deviceId;
            public long timestamp;
        }

        // Create an MqttEntity for temperature data
        private InventoMqttClient.MqttEntity<TemperatureMessage> _temperatureEntity;
        
        // Create another entity for a different topic to demonstrate multiple subscriptions
        private InventoMqttClient.MqttEntity<HumidityMessage> _humidityEntity;

        [Serializable]
        public class HumidityMessage
        {
            public float humidity;
            public string deviceId;
            public long timestamp;
        }

        void Start()
        {
            // Initialize the temperature entity
            _temperatureEntity = new InventoMqttClient.MqttEntity<TemperatureMessage>("sensors/temperature");
            
            // Subscribe to temperature messages
            _temperatureEntity.Subscribe(OnTemperatureMessageReceived);
            
            // Initialize the humidity entity
            _humidityEntity = new InventoMqttClient.MqttEntity<HumidityMessage>("sensors/humidity");
            
            // Subscribe to humidity messages
            _humidityEntity.Subscribe(OnHumidityMessageReceived);
            
            // Publish test messages
            PublishTemperature();
            PublishHumidity();
        }

        void OnDestroy()
        {
            // Always unsubscribe when done to prevent memory leaks
            if (_temperatureEntity != null)
                _temperatureEntity.Unsubscribe(OnTemperatureMessageReceived);
                
            if (_humidityEntity != null)
                _humidityEntity.Unsubscribe(OnHumidityMessageReceived);
        }

        // Handler for received temperature messages
        private void OnTemperatureMessageReceived(TemperatureMessage message)
        {
            Debug.Log($"Received temperature: {message.temperature}{message.unit} from device {message.deviceId} at {DateTimeOffset.FromUnixTimeMilliseconds(message.timestamp).DateTime}");
            
            // You can process the message here
            // For example, update UI, trigger events, etc.
        }
        
        // Handler for received humidity messages
        private void OnHumidityMessageReceived(HumidityMessage message)
        {
            Debug.Log($"Received humidity: {message.humidity}% from device {message.deviceId} at {DateTimeOffset.FromUnixTimeMilliseconds(message.timestamp).DateTime}");
            
            // Process humidity data
        }

        // Example method to publish a temperature reading
        public void PublishTemperature()
        {
            var message = new TemperatureMessage
            {
                temperature = 22.5f,
                deviceId = SystemInfo.deviceUniqueIdentifier,
                timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
            };
            
            _temperatureEntity.Publish(message);
        }
        
        // Example method to publish a humidity reading
        public void PublishHumidity()
        {
            var message = new HumidityMessage
            {
                humidity = 45.2f,
                deviceId = SystemInfo.deviceUniqueIdentifier,
                timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
            };
            
            _humidityEntity.Publish(message);
        }

        // Example showing multiple subscribers to the same topic
        public void AddAnotherTemperatureSubscriber()
        {
            _temperatureEntity.Subscribe(message => {
                Debug.Log($"Second subscriber received: {message.temperature}{message.unit}");
            });
        }
    }
}
