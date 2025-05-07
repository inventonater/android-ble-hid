using System;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using UnityEngine;

namespace Inventonater
{
    public class RemoteMappingConverter : JsonConverter<RemoteMapping>
    {
        public override RemoteMapping ReadJson(JsonReader reader, Type objectType, RemoteMapping existingValue, bool hasExistingValue, JsonSerializer serializer)
        {
            try
            {
                // Read the JSON object
                JObject jo = JObject.Load(reader);
                
                // Extract properties with error handling
                string id = ExtractStringProperty(jo, "id");
                string name = ExtractStringProperty(jo, "name");
                string category = ExtractCategoryProperty(jo);
                bool isConnected = ExtractBoolProperty(jo, "isConnected");
                
                // Create and return the RemoteMapping object
                return new RemoteMapping(id, name, category, isConnected);
            }
            catch (Exception ex)
            {
                Debug.LogError($"Error in RemoteMappingConverter.ReadJson: {ex.Message}");
                // Return a default object rather than throwing
                return new RemoteMapping();
            }
        }

        public override void WriteJson(JsonWriter writer, RemoteMapping value, JsonSerializer serializer)
        {
            try
            {
                writer.WriteStartObject();
                
                writer.WritePropertyName("id");
                writer.WriteValue(value.Id);
                
                writer.WritePropertyName("name");
                writer.WriteValue(value.Name);
                
                writer.WritePropertyName("category");
                writer.WriteValue(value.Category);
                
                writer.WritePropertyName("isConnected");
                writer.WriteValue(value.IsConnected);
                
                writer.WriteEndObject();
            }
            catch (Exception ex)
            {
                Debug.LogError($"Error in RemoteMappingConverter.WriteJson: {ex.Message}");
                // Continue without throwing
            }
        }
        
        private string ExtractStringProperty(JObject jo, string propertyName)
        {
            if (jo.TryGetValue(propertyName, out JToken token))
            {
                return token.ToString();
            }
            Debug.LogWarning($"Property '{propertyName}' not found in JSON");
            return string.Empty;
        }
        
        private bool ExtractBoolProperty(JObject jo, string propertyName)
        {
            if (jo.TryGetValue(propertyName, out JToken token))
            {
                if (token.Type == JTokenType.Boolean)
                {
                    return token.Value<bool>();
                }
                else if (token.Type == JTokenType.String)
                {
                    if (bool.TryParse(token.Value<string>(), out bool result))
                    {
                        return result;
                    }
                }
                else if (token.Type == JTokenType.Integer)
                {
                    return token.Value<int>() != 0;
                }
            }
            Debug.LogWarning($"Property '{propertyName}' not found or not convertible to bool");
            return false;
        }
        
        private string ExtractCategoryProperty(JObject jo)
        {
            if (jo.TryGetValue("category", out JToken token))
            {
                if (token.Type == JTokenType.Integer)
                {
                    int categoryValue = token.Value<int>();
                    // Map numeric values to string representation
                    switch (categoryValue)
                    {
                        case 0: return nameof(RemoteMapping.DeviceCategory.SmartTV);
                        case 1: return nameof(RemoteMapping.DeviceCategory.SmartSpeaker);
                        case 2: return nameof(RemoteMapping.DeviceCategory.PC);
                        case 3: return nameof(RemoteMapping.DeviceCategory.Lights);
                        case 4: return nameof(RemoteMapping.DeviceCategory.MediaPlayer);
                        case 5: return nameof(RemoteMapping.DeviceCategory.Other);
                        default: return nameof(RemoteMapping.DeviceCategory.Other);
                    }
                }
                else if (token.Type == JTokenType.String)
                {
                    return token.Value<string>();
                }
            }
            Debug.LogWarning("Property 'category' not found or not convertible");
            return nameof(RemoteMapping.DeviceCategory.Other);
        }
    }
}
