using System;
using Newtonsoft.Json;
using UnityEngine;

namespace Inventonater
{
    [Serializable]
    public class RemoteMapping
    {
        public readonly string name;
        public readonly string description;
        public bool isConnected;

        [JsonConstructor]
        public RemoteMapping(string name, string description, bool isConnected, Texture2D image = null)
        {
            this.name = name;
            this.description = description;
            this.isConnected = true;
        }

        public RemoteMapping(InputBinding binding) : this(binding.Name, binding.Description, true)
        {
        }
    }
}
