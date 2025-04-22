using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Utility class for serializing and deserializing filter objects
    /// </summary>
    public static class FilterSerializer
    {
        /// <summary>
        /// Serialize a filter to JSON string
        /// </summary>
        /// <param name="filter">The filter to serialize</param>
        /// <returns>JSON string representation of the filter</returns>
        public static string Serialize(IInputFilter filter)
        {
            if (filter == null)
                return null;
                
            // Include type information in the serialized JSON
            var settings = new JsonSerializerSettings
            {
                TypeNameHandling = TypeNameHandling.All,
                Formatting = Formatting.Indented
            };
            
            try
            {
                return JsonConvert.SerializeObject(filter, settings);
            }
            catch (Exception ex)
            {
                Debug.LogError($"Error serializing filter: {ex.Message}");
                return null;
            }
        }
        
        /// <summary>
        /// Deserialize a JSON string to a filter object
        /// </summary>
        /// <param name="json">JSON string representation of the filter</param>
        /// <returns>Deserialized filter object</returns>
        public static IInputFilter Deserialize(string json)
        {
            if (string.IsNullOrEmpty(json))
                return null;
                
            var settings = new JsonSerializerSettings
            {
                TypeNameHandling = TypeNameHandling.All
            };
            
            try
            {
                return JsonConvert.DeserializeObject<IInputFilter>(json, settings);
            }
            catch (Exception ex)
            {
                Debug.LogError($"Error deserializing filter: {ex.Message}");
                return null;
            }
        }
        
        /// <summary>
        /// Apply settings from a dictionary to a filter
        /// </summary>
        /// <param name="filter">The filter to apply settings to</param>
        /// <param name="settings">Dictionary of settings</param>
        public static void ApplySettings(IInputFilter filter, Dictionary<string, object> settings)
        {
            if (filter == null || settings == null || settings.Count == 0)
                return;
                
            try
            {
                // Convert settings to JSON
                string settingsJson = JsonConvert.SerializeObject(settings);
                
                // Apply settings to the filter
                var jsonSettings = new JsonSerializerSettings
                {
                    ObjectCreationHandling = ObjectCreationHandling.Replace
                };
                
                JsonConvert.PopulateObject(settingsJson, filter, jsonSettings);
            }
            catch (Exception ex)
            {
                Debug.LogError($"Error applying settings to filter: {ex.Message}");
            }
        }
        
        /// <summary>
        /// Create a filter from a type and settings dictionary
        /// </summary>
        /// <param name="type">Type of filter to create</param>
        /// <param name="settings">Dictionary of settings</param>
        /// <returns>Configured filter instance</returns>
        public static IInputFilter CreateFilterFromSettings(
            InputFilterFactory.FilterType type, 
            Dictionary<string, object> settings)
        {
            // Create a default filter of the specified type
            var filter = InputFilterFactory.CreateFilter(type);
            
            if (filter == null)
                return null;
                
            // Apply settings if available
            ApplySettings(filter, settings);
            
            return filter;
        }
    }
}
