using System;
using System.Collections.Generic;
using Best.MQTT.Packets;
using Cysharp.Threading.Tasks;
using UnityEngine;
using Random = UnityEngine.Random;

namespace Inventonater
{
    [Serializable]
    public class MqttLightsBridge
    {
        [MappableAction(id: EInputAction.Back, displayName: "Play/Pause", description: "Toggle media playback between play and pause states")]
        public void Back() => RandomizeAndPublish();

        [MappableAction(id: EInputAction.Right, displayName: "Next Light", description: "Switch to the next light in the sequence")]
        public void Right() => CycleLight();

        [MappableAction(id: EInputAction.Left, displayName: "Previous Light", description: "Switch to the previous light in the sequence")]
        public void Left() => CycleLight(-1);

        [MappableAction(id: EInputAction.Up, displayName: "Brightness Up", description: "Increase brightness")]
        public void Up() => SetBrightness(255);

        [MappableAction(id: EInputAction.Down, displayName: "Brightness Down", description: "Decrease brightness")]
        public void Down() => SetBrightness(0);
        
        [MappableAction(id: EInputAction.Chirp)]
        public async UniTask Chirp()
        {
            const int blinkOffMs = 110;
            const int blinkOnMs = 180;
            CurrentLight.SetStatusOff();
            await UniTask.Delay(blinkOffMs);
            CurrentLight.SetStatusOn();
            await UniTask.Delay(blinkOnMs);
            CurrentLight.SetStatusOff();
            await UniTask.Delay(blinkOffMs);
            CurrentLight.SetStatusOn();
        }

        private List<MqttLightTopic> _mqttLights;
        private int _currentLightIndex;

        public MqttLightsBridge(InventoMqttClient mqttClient)
        {
            _mqttLights = new()
            {
                new(mqttClient, "kauf-bulb-302bb8/light/kauf-bulb-302bb8/command"),
                new(mqttClient, "kauf-bulb-302a9e/light/kauf-bulb-302a9e/command")
            };
        }

        private MqttLightTopic CurrentLight => _mqttLights[_currentLightIndex];

        public class MqttLightTopic : MqttTopic<MqttLightTopic.Payload>
        {
            [Serializable]
            public class Payload
            {
                public string state = "ON";
                public byte brightness = 255;
                public Color32 color = new Color32(255, 255, 255, 255);
            }

            private readonly Payload _value = new();

            public MqttLightTopic(InventoMqttClient client, string commandTopic) : base(client, commandTopic)
            {
            }

            public void SetColor(Color32 color)
            {
                _value.color = color;
                Publish(_value);
            }

            public void SetBrightness(int brightness, QoSLevels qos = QoSLevels.AtLeastOnceDelivery)
            {
                _value.state = "ON";
                _value.brightness = (byte)Mathf.Clamp(brightness, 0, 255);
                Publish(_value, qos);
            }

            public void IncrementBrightness(int increment)
            {
                SetBrightness(_value.brightness + increment, QoSLevels.AtMostOnceDelivery);
            }

            public void SetStatusOff()
            {
                _value.state = "OFF";
                Publish(_value);
            }

            public void SetStatusOn()
            {
                _value.state = "ON";
                Publish(_value);
            }
        }

        private static Color32 RandomColor() => new((byte)Random.Range(0, 255), (byte)Random.Range(0, 255), (byte)Random.Range(0, 255), 255);

        public void CycleLight(int direction = 1)
        {
            _currentLightIndex = (_currentLightIndex + direction + _mqttLights.Count) % _mqttLights.Count;
            Chirp().Forget();
        }

        public void IncrementBrightness(int increment) => CurrentLight.IncrementBrightness(increment);
        public void SetBrightness(int brightness) => CurrentLight.SetBrightness(brightness);

        public void RandomizeAndPublish() => CurrentLight.SetColor(RandomColor());
    }
}
