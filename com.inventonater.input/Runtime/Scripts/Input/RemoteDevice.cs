using System;
using Newtonsoft.Json;
using UnityEngine;

namespace Inventonater
{
    [Serializable]
    public class RemoteDevice
    {
        [SerializeField] private string _name;
        [SerializeField] private string _description;
        [SerializeField] private bool _isConnected;

        public string Name => _name;
        public string Description => _description;
        public bool IsConnected => _isConnected;

        [JsonConstructor]
        public RemoteDevice(string name, string description, bool isConnected)
        {
            _name = name;
            _description = description;
            _isConnected = true;
        }

        public RemoteDevice(InputBinding binding) : this(binding.Name, binding.Description, true)
        {
        }
    }
}
