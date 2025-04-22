using System;
using System.Collections.Generic;
using Newtonsoft.Json;

namespace Inventonater.BleHid
{
    [Serializable]
    public class MappingConfiguration
    {
        [JsonProperty("name")]
        public string Name { get; set; } = "Default Configuration";
        
        [JsonProperty("description")]
        public string Description { get; set; } = "Default input mapping configuration";
        
        [JsonProperty("version")]
        public string Version { get; set; } = "1.0";
        
        [JsonProperty("buttonMappings")]
        public List<ButtonMappingEntry> ButtonMappings { get; set; } = new List<ButtonMappingEntry>();
        
        [JsonProperty("directionMappings")]
        public List<DirectionMappingEntry> DirectionMappings { get; set; } = new List<DirectionMappingEntry>();
        
        [JsonProperty("axisMappings")]
        public List<AxisMappingEntry> AxisMappings { get; set; } = new List<AxisMappingEntry>();
    }
    
    [Serializable]
    public class ButtonMappingEntry
    {
        [JsonProperty("inputEvent")]
        public string InputEvent { get; set; }
        
        [JsonProperty("action")]
        public string Action { get; set; }
        
        [JsonProperty("keyCode")]
        public string KeyCode { get; set; }
    }
    
    [Serializable]
    public class DirectionMappingEntry
    {
        [JsonProperty("inputDirection")]
        public string InputDirection { get; set; }
        
        [JsonProperty("action")]
        public string Action { get; set; }
        
        [JsonProperty("keyCode")]
        public string KeyCode { get; set; }
    }
    
    [Serializable]
    public class AxisMappingEntry
    {
        [JsonProperty("type")]
        public string Type { get; set; }
        
        [JsonProperty("axis")]
        public string Axis { get; set; }
        
        [JsonProperty("incrementAction")]
        public string IncrementAction { get; set; }
        
        [JsonProperty("decrementAction")]
        public string DecrementAction { get; set; }
        
        [JsonProperty("settings")]
        public Dictionary<string, object> Settings { get; set; } = new Dictionary<string, object>();
        
        [JsonProperty("filterSettings", NullValueHandling = NullValueHandling.Ignore)]
        public Dictionary<string, object> FilterSettings { get; set; } = new Dictionary<string, object>();
        
        // New property to store serialized filter
        [JsonProperty("serializedFilter", NullValueHandling = NullValueHandling.Ignore)]
        public string SerializedFilter { get; set; }
    }
}
