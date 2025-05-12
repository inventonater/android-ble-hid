using System;
using UnityEngine;

namespace Inventonater
{
    [Serializable]
    public class MqttBridge
    {
        [SerializeField] private InventoMqttClient _mqttClient;
        [SerializeField] private MqttSpotifyBridge spotifyBridge;
        [SerializeField] private MqttLightsBridge lightsBridge;
        [SerializeField] private MqttChromecastBridge chromecastBridge;
        [SerializeField] private MqttShellBridge shellBridge;

        public MqttSpotifyBridge SpotifyBridge => spotifyBridge;
        public MqttLightsBridge LightsBridge => lightsBridge;
        public MqttChromecastBridge ChromecastBridge => chromecastBridge;
        public MqttShellBridge ShellBridge => shellBridge;

        public MqttBridge(InventoMqttClient mqttClient)
        {
            _mqttClient = mqttClient;
            spotifyBridge = new MqttSpotifyBridge(_mqttClient);
            lightsBridge = new MqttLightsBridge(_mqttClient);
            chromecastBridge = new MqttChromecastBridge(_mqttClient);
            shellBridge = new MqttShellBridge(_mqttClient);
        }
    }
}
