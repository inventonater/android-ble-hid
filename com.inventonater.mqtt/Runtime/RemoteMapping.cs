using System;
using Newtonsoft.Json;
using UnityEngine;

namespace Inventonater
{
    [Serializable]
    public class RemoteMapping
    {
        // These fields will be serialized
        [SerializeField] private string id;
        [SerializeField] private string name;
        [SerializeField] private string category;
        [SerializeField] private bool isConnected;

        // This field should not be serialized
        [JsonIgnore]
        [SerializeField] private Texture2D image;

        // Default constructor for JSON.NET
        public RemoteMapping()
        {
            // Default values
            Id = string.Empty;
            Name = string.Empty;
            Category = nameof(DeviceCategory.Other);
            IsConnected = false;
            Image = null;
        }

        // Constructor for creating from InputDeviceMapping
        public RemoteMapping(InputBinding inputBinding)
        {
            Id = GetDeviceIdFromMapping(inputBinding);
            Name = inputBinding.Name;
            Category = DetermineCategory(inputBinding).ToString();
            IsConnected = false;
            Image = null;
        }

        // Constructor with all parameters
        public RemoteMapping(string id, string name, string category, bool isConnected, Texture2D image = null)
        {
            Id = id;
            Name = name;
            Category = category;
            IsConnected = isConnected;
            Image = image ?? Texture2D.grayTexture;
        }

        // Properties with public getters and setters for serialization
        [JsonProperty("id")]
        public string Id
        {
            get => id;
            set => id = value;
        }

        [JsonProperty("name")]
        public string Name
        {
            get => name;
            set => name = value;
        }

        [JsonProperty("category")]
        public string Category
        {
            get => category;
            set => category = value;
        }

        [JsonProperty("isConnected")]
        public bool IsConnected
        {
            get => isConnected;
            set => isConnected = value;
        }

        [JsonIgnore]
        public Texture2D Image
        {
            get => image;
            set => image = value;
        }

        public override string ToString() => $"{Name} ({Category})";

        public static string GetDeviceIdFromMapping(InputBinding mapping) => $"device-{mapping.Name.ToLower().Replace(" ", "-")}";

        private DeviceCategory DetermineCategory(InputBinding mapping)
        {
            string name = mapping.Name.ToLower();
            if (name.Contains("light")) return DeviceCategory.Lights;
            if (name.Contains("tv") || name.Contains("chromecast")) return DeviceCategory.SmartTV;
            if (name.Contains("speaker")) return DeviceCategory.SmartSpeaker;
            if (name.Contains("media")) return DeviceCategory.MediaPlayer;
            if (name.Contains("pc") || name.Contains("computer")) return DeviceCategory.PC;
            return DeviceCategory.Other;
        }

        public enum DeviceCategory
        {
            SmartTV,
            SmartSpeaker,
            PC,
            Lights,
            MediaPlayer,
            Other
        }
    }
}
