using System;
using Inventonater;
using UnityEngine;

namespace Remotes
{
    [Serializable]
    public class MqttRemoteDeviceClient
    {
        [SerializeField] private MqttTopicDeviceActiveState _activeState;
        [SerializeField] private MqttTopicDeviceActiveCommand _activeCommand;
        [SerializeField] private MqttTopicDeviceList deviceList;
        private readonly InventoMqttClient _mqttClient;

        [SerializeField] private RemoteDevice active;
        public RemoteDevice Active => active;

        [SerializeField] private RemoteDevice[] list = Array.Empty<RemoteDevice>();
        public RemoteDevice[] List => list;

        public event Action<RemoteDevice> WhenActiveChanged = delegate { };
        public event Action<RemoteDevice[]> WhenListChanged = delegate { };
        public void RequestActive(RemoteDevice remoteDevice) => _activeCommand.Publish(remoteDevice);

        public MqttRemoteDeviceClient(InventoMqttClient mqttClient)
        {
            _mqttClient = mqttClient;
            _mqttClient.WhenConnected += HandleMqttConnected;
        }

        private void HandleMqttConnected()
        {
            deviceList = new (_mqttClient);
            deviceList.WhenMessageReceived += list =>
            {
                this.list = list;
                WhenListChanged(this.list);
            };

            _activeState = new(_mqttClient);
            _activeState.WhenMessageReceived += remoteDevice =>
            {
                active = remoteDevice;
                WhenActiveChanged(remoteDevice);
            };

            _activeCommand = new MqttTopicDeviceActiveCommand(_mqttClient);
        }
    }
}
