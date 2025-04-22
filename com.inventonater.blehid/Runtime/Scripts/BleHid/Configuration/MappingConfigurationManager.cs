using System;
using System.IO;
using UnityEngine;
using Newtonsoft.Json;

namespace Inventonater.BleHid
{
    public class MappingConfigurationManager
    {
        public MappingConfiguration LoadConfiguration(string path)
        {
            try
            {
                string json = File.ReadAllText(path);
                var config = JsonConvert.DeserializeObject<MappingConfiguration>(json);
                
                // Deserialize any serialized filters
                if (config != null && config.AxisMappings != null)
                {
                    foreach (var axisMapping in config.AxisMappings)
                    {
                        if (!string.IsNullOrEmpty(axisMapping.SerializedFilter))
                        {
                            // The filter will be deserialized when needed in AxisMappingFactory
                            Debug.Log($"Found serialized filter for axis mapping: {axisMapping.Type}");
                        }
                    }
                }
                
                return config ?? CreateDefaultConfiguration();
            }
            catch (Exception e)
            {
                Debug.LogError($"Failed to load configuration from {path}: {e.Message}");
                return CreateDefaultConfiguration();
            }
        }
        
        public void SaveConfiguration(MappingConfiguration config, string path)
        {
            try
            {
                // Ensure directory exists
                string directory = Path.GetDirectoryName(path);
                if (!string.IsNullOrEmpty(directory) && !Directory.Exists(directory))
                {
                    Directory.CreateDirectory(directory);
                }
                
                // Serialize any filters in the configuration
                if (config.AxisMappings != null)
                {
                    foreach (var axisMapping in config.AxisMappings)
                    {
                        // If we have filter settings but no serialized filter, create a serialized version
                        if (axisMapping.FilterSettings != null && axisMapping.FilterSettings.Count > 0 && 
                            string.IsNullOrEmpty(axisMapping.SerializedFilter))
                        {
                            // Create a filter instance based on the settings
                            if (axisMapping.Settings != null && 
                                axisMapping.Settings.TryGetValue("filter", out var filterTypeObj) && 
                                filterTypeObj is string filterTypeStr)
                            {
                                if (Enum.TryParse<InputFilterFactory.FilterType>(filterTypeStr, true, out var filterType))
                                {
                                    var filter = InputFilterFactory.CreateFilter(filterType);
                                    if (filter != null)
                                    {
                                        // Apply settings to the filter
                                        FilterSerializer.ApplySettings(filter, axisMapping.FilterSettings);
                                        
                                        // Serialize the filter
                                        axisMapping.SerializedFilter = FilterSerializer.Serialize(filter);
                                    }
                                }
                            }
                        }
                    }
                }
                
                string json = JsonConvert.SerializeObject(config, Formatting.Indented);
                File.WriteAllText(path, json);
                Debug.Log($"Configuration saved to {path}");
            }
            catch (Exception e)
            {
                Debug.LogError($"Failed to save configuration to {path}: {e.Message}");
            }
        }
        
        public MappingConfiguration CreateDefaultConfiguration()
        {
            var config = new MappingConfiguration
            {
                Name = "Default Configuration",
                Description = "Standard mapping for mouse and keyboard control",
                Version = "1.0",
                ButtonMappings = new System.Collections.Generic.List<ButtonMappingEntry>
                {
                    new ButtonMappingEntry { InputEvent = "Primary.Press", Action = "Mouse.LeftButton.Press" },
                    new ButtonMappingEntry { InputEvent = "Primary.Release", Action = "Mouse.LeftButton.Release" },
                    new ButtonMappingEntry { InputEvent = "Secondary.Press", Action = "Mouse.RightButton.Press" },
                    new ButtonMappingEntry { InputEvent = "Secondary.Release", Action = "Mouse.RightButton.Release" }
                },
                DirectionMappings = new System.Collections.Generic.List<DirectionMappingEntry>
                {
                    new DirectionMappingEntry { InputDirection = "Up", Action = "Keyboard.Key", KeyCode = "UpArrow" },
                    new DirectionMappingEntry { InputDirection = "Down", Action = "Keyboard.Key", KeyCode = "DownArrow" },
                    new DirectionMappingEntry { InputDirection = "Left", Action = "Keyboard.Key", KeyCode = "LeftArrow" },
                    new DirectionMappingEntry { InputDirection = "Right", Action = "Keyboard.Key", KeyCode = "RightArrow" }
                },
                AxisMappings = new System.Collections.Generic.List<AxisMappingEntry>
                {
                    new AxisMappingEntry
                    {
                        Type = "Mouse",
                        Axis = "XY",
                        Settings = new System.Collections.Generic.Dictionary<string, object>
                        {
                            ["horizontalSensitivity"] = 3.0f,
                            ["verticalSensitivity"] = 3.0f,
                            ["flipY"] = true,
                            ["filter"] = "OneEuro"
                        },
                        FilterSettings = new System.Collections.Generic.Dictionary<string, object>
                        {
                            ["beta"] = 0.007f,
                            ["minCutoff"] = 1.0f
                        }
                    },
                    new AxisMappingEntry
                    {
                        Type = "Incremental",
                        Axis = "Z",
                        IncrementAction = "Media.VolumeUp",
                        DecrementAction = "Media.VolumeDown",
                        Settings = new System.Collections.Generic.Dictionary<string, object>
                        {
                            ["interval"] = 0.02f
                        }
                    }
                }
            };
            
            return config;
        }
        
        public string GetDefaultConfigPath()
        {
            return Path.Combine(Application.persistentDataPath, "DefaultMapping.json");
        }
    }
}
