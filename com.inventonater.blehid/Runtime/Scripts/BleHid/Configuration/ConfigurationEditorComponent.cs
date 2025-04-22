using System.Collections.Generic;
using UnityEngine;
using System.IO;

namespace Inventonater.BleHid
{
    /// <summary>
    /// UI Component for editing BleHid configurations
    /// </summary>
    public class ConfigurationEditorComponent : MonoBehaviour
    {
        [SerializeField] private BleHidManager _bleHidManager;
        
        private MappingConfigurationManager _configManager;
        private MappingConfiguration _currentConfig;
        private string _configPath;
        private Vector2 _scrollPosition;
        private bool _showButtonMappings = true;
        private bool _showDirectionMappings = true;
        private bool _showAxisMappings = true;
        private string _configName = "Default Configuration";
        private string _configDescription = "Default input mapping configuration";
        
        // GUI Styles
        private GUIStyle _headerStyle;
        private GUIStyle _subHeaderStyle;
        private GUIStyle _toggleHeaderStyle;
        
        private void Start()
        {
            _configManager = new MappingConfigurationManager();
            _configPath = _configManager.GetDefaultConfigPath();
            LoadConfiguration();
            
            // Initialize styles
            InitializeStyles();
        }
        
        private void InitializeStyles()
        {
            _headerStyle = new GUIStyle(GUI.skin.label);
            _headerStyle.fontSize = GUI.skin.label.fontSize + 4;
            _headerStyle.fontStyle = FontStyle.Bold;
            _headerStyle.alignment = TextAnchor.MiddleLeft;
            _headerStyle.margin = new RectOffset(5, 5, 10, 10);
            
            _subHeaderStyle = new GUIStyle(GUI.skin.label);
            _subHeaderStyle.fontSize = GUI.skin.label.fontSize + 2;
            _subHeaderStyle.fontStyle = FontStyle.Bold;
            _subHeaderStyle.alignment = TextAnchor.MiddleLeft;
            _subHeaderStyle.margin = new RectOffset(5, 5, 5, 5);
            
            _toggleHeaderStyle = new GUIStyle(GUI.skin.toggle);
            _toggleHeaderStyle.fontSize = GUI.skin.toggle.fontSize + 2;
            _toggleHeaderStyle.fontStyle = FontStyle.Bold;
            _toggleHeaderStyle.margin = new RectOffset(5, 5, 5, 5);
        }
        
        public void OnGUI()
        {
            GUILayout.Label("BleHid Configuration Editor", _headerStyle);
            
            // Configuration file path
            GUILayout.BeginHorizontal();
            GUILayout.Label("Config Path:", GUILayout.Width(100));
            _configPath = GUILayout.TextField(_configPath, GUILayout.ExpandWidth(true));
            if (GUILayout.Button("Load", GUILayout.Width(60)))
            {
                LoadConfiguration();
            }
            if (GUILayout.Button("Save", GUILayout.Width(60)))
            {
                SaveConfiguration();
            }
            GUILayout.EndHorizontal();
            
            if (_currentConfig == null)
            {
                GUILayout.Label("No configuration loaded.");
                return;
            }
            
            _scrollPosition = GUILayout.BeginScrollView(_scrollPosition);
            
            // Configuration metadata
            GUILayout.Space(10);
            GUILayout.Label("Configuration Properties", _subHeaderStyle);
            
            GUILayout.BeginHorizontal();
            GUILayout.Label("Name:", GUILayout.Width(100));
            _currentConfig.Name = GUILayout.TextField(_currentConfig.Name, GUILayout.ExpandWidth(true));
            GUILayout.EndHorizontal();
            
            GUILayout.BeginHorizontal();
            GUILayout.Label("Description:", GUILayout.Width(100));
            _currentConfig.Description = GUILayout.TextField(_currentConfig.Description, GUILayout.ExpandWidth(true));
            GUILayout.EndHorizontal();
            
            GUILayout.BeginHorizontal();
            GUILayout.Label("Version:", GUILayout.Width(100));
            _currentConfig.Version = GUILayout.TextField(_currentConfig.Version, GUILayout.ExpandWidth(true));
            GUILayout.EndHorizontal();
            
            // Button mappings
            GUILayout.Space(10);
            _showButtonMappings = GUILayout.Toggle(_showButtonMappings, "Button Mappings", _toggleHeaderStyle);
            if (_showButtonMappings)
            {
                DrawButtonMappings();
            }
            
            // Direction mappings
            GUILayout.Space(10);
            _showDirectionMappings = GUILayout.Toggle(_showDirectionMappings, "Direction Mappings", _toggleHeaderStyle);
            if (_showDirectionMappings)
            {
                DrawDirectionMappings();
            }
            
            // Axis mappings
            GUILayout.Space(10);
            _showAxisMappings = GUILayout.Toggle(_showAxisMappings, "Axis Mappings", _toggleHeaderStyle);
            if (_showAxisMappings)
            {
                DrawAxisMappings();
            }
            
            GUILayout.EndScrollView();
            
            // Apply configuration
            GUILayout.Space(10);
            if (GUILayout.Button("Apply Configuration"))
            {
                ApplyConfiguration();
            }
        }
        
        private void DrawButtonMappings()
        {
            for (int i = 0; i < _currentConfig.ButtonMappings.Count; i++)
            {
                var mapping = _currentConfig.ButtonMappings[i];
                GUILayout.BeginHorizontal();
                
                GUILayout.Label("Input Event:", GUILayout.Width(80));
                mapping.InputEvent = GUILayout.TextField(mapping.InputEvent, GUILayout.Width(120));
                
                GUILayout.Label("Action:", GUILayout.Width(50));
                mapping.Action = GUILayout.TextField(mapping.Action, GUILayout.Width(150));
                
                if (GUILayout.Button("X", GUILayout.Width(20)))
                {
                    _currentConfig.ButtonMappings.RemoveAt(i);
                    i--;
                    continue;
                }
                
                GUILayout.EndHorizontal();
            }
            
            if (GUILayout.Button("Add Button Mapping", GUILayout.Width(150)))
            {
                _currentConfig.ButtonMappings.Add(new ButtonMappingEntry());
            }
        }
        
        private void DrawDirectionMappings()
        {
            for (int i = 0; i < _currentConfig.DirectionMappings.Count; i++)
            {
                var mapping = _currentConfig.DirectionMappings[i];
                GUILayout.BeginHorizontal();
                
                GUILayout.Label("Direction:", GUILayout.Width(60));
                mapping.InputDirection = GUILayout.TextField(mapping.InputDirection, GUILayout.Width(80));
                
                GUILayout.Label("Action:", GUILayout.Width(50));
                mapping.Action = GUILayout.TextField(mapping.Action, GUILayout.Width(100));
                
                GUILayout.Label("Key:", GUILayout.Width(30));
                mapping.KeyCode = GUILayout.TextField(mapping.KeyCode, GUILayout.Width(80));
                
                if (GUILayout.Button("X", GUILayout.Width(20)))
                {
                    _currentConfig.DirectionMappings.RemoveAt(i);
                    i--;
                    continue;
                }
                
                GUILayout.EndHorizontal();
            }
            
            if (GUILayout.Button("Add Direction Mapping", GUILayout.Width(150)))
            {
                _currentConfig.DirectionMappings.Add(new DirectionMappingEntry());
            }
        }
        
        private void DrawAxisMappings()
        {
            for (int i = 0; i < _currentConfig.AxisMappings.Count; i++)
            {
                var mapping = _currentConfig.AxisMappings[i];
                GUILayout.BeginVertical(GUI.skin.box);
                
                GUILayout.BeginHorizontal();
                GUILayout.Label("Type:", GUILayout.Width(40));
                mapping.Type = GUILayout.TextField(mapping.Type, GUILayout.Width(80));
                
                GUILayout.Label("Axis:", GUILayout.Width(40));
                mapping.Axis = GUILayout.TextField(mapping.Axis, GUILayout.Width(40));
                
                if (GUILayout.Button("X", GUILayout.Width(20)))
                {
                    _currentConfig.AxisMappings.RemoveAt(i);
                    i--;
                    GUILayout.EndHorizontal();
                    GUILayout.EndVertical();
                    continue;
                }
                GUILayout.EndHorizontal();
                
                if (mapping.Type.ToLowerInvariant() == "mouse")
                {
                    DrawMouseAxisMapping(mapping);
                }
                else if (mapping.Type.ToLowerInvariant() == "incremental")
                {
                    DrawIncrementalAxisMapping(mapping);
                }
                
                GUILayout.EndVertical();
            }
            
            if (GUILayout.Button("Add Axis Mapping", GUILayout.Width(150)))
            {
                _currentConfig.AxisMappings.Add(new AxisMappingEntry
                {
                    Type = "Mouse",
                    Axis = "XY",
                    Settings = new Dictionary<string, object>(),
                    FilterSettings = new Dictionary<string, object>()
                });
            }
        }
        
        private void DrawMouseAxisMapping(AxisMappingEntry mapping)
        {
            // Ensure settings dictionaries exist
            if (mapping.Settings == null)
                mapping.Settings = new Dictionary<string, object>();
            
            if (mapping.FilterSettings == null)
                mapping.FilterSettings = new Dictionary<string, object>();
            
            // Get current values with defaults
            float hSensitivity = 3.0f;
            float vSensitivity = 3.0f;
            bool flipY = true;
            string filterType = "OneEuro";
            
            if (mapping.Settings.TryGetValue("horizontalSensitivity", out var hSensObj))
                hSensitivity = ConvertToFloat(hSensObj);
            
            if (mapping.Settings.TryGetValue("verticalSensitivity", out var vSensObj))
                vSensitivity = ConvertToFloat(vSensObj);
            
            if (mapping.Settings.TryGetValue("flipY", out var flipYObj) && flipYObj is bool flipYBool)
                flipY = flipYBool;
            
            if (mapping.Settings.TryGetValue("filter", out var filterObj) && filterObj is string filterStr)
                filterType = filterStr;
            
            // Draw settings
            GUILayout.BeginHorizontal();
            GUILayout.Label("H Sensitivity:", GUILayout.Width(80));
            hSensitivity = float.Parse(GUILayout.TextField(hSensitivity.ToString(), GUILayout.Width(50)));
            
            GUILayout.Label("V Sensitivity:", GUILayout.Width(80));
            vSensitivity = float.Parse(GUILayout.TextField(vSensitivity.ToString(), GUILayout.Width(50)));
            
            GUILayout.Label("Flip Y:", GUILayout.Width(50));
            flipY = GUILayout.Toggle(flipY, "", GUILayout.Width(20));
            GUILayout.EndHorizontal();
            
            GUILayout.BeginHorizontal();
            GUILayout.Label("Filter Type:", GUILayout.Width(80));
            filterType = GUILayout.TextField(filterType, GUILayout.Width(100));
            GUILayout.EndHorizontal();
            
            // Draw filter settings based on filter type
            if (filterType.ToLowerInvariant() == "oneeuro")
            {
                float beta = 0.007f;
                float minCutoff = 1.0f;
                
                if (mapping.FilterSettings.TryGetValue("beta", out var betaObj))
                    beta = ConvertToFloat(betaObj);
                
                if (mapping.FilterSettings.TryGetValue("minCutoff", out var minCutoffObj))
                    minCutoff = ConvertToFloat(minCutoffObj);
                
                GUILayout.BeginHorizontal();
                GUILayout.Label("Beta:", GUILayout.Width(80));
                beta = float.Parse(GUILayout.TextField(beta.ToString(), GUILayout.Width(50)));
                
                GUILayout.Label("Min Cutoff:", GUILayout.Width(80));
                minCutoff = float.Parse(GUILayout.TextField(minCutoff.ToString(), GUILayout.Width(50)));
                GUILayout.EndHorizontal();
                
                mapping.FilterSettings["beta"] = beta;
                mapping.FilterSettings["minCutoff"] = minCutoff;
            }
            else if (filterType.ToLowerInvariant() == "kalman")
            {
                float processNoise = 0.001f;
                float measurementNoise = 0.1f;
                
                if (mapping.FilterSettings.TryGetValue("processNoise", out var processNoiseObj))
                    processNoise = ConvertToFloat(processNoiseObj);
                
                if (mapping.FilterSettings.TryGetValue("measurementNoise", out var measurementNoiseObj))
                    measurementNoise = ConvertToFloat(measurementNoiseObj);
                
                GUILayout.BeginHorizontal();
                GUILayout.Label("Process Noise:", GUILayout.Width(100));
                processNoise = float.Parse(GUILayout.TextField(processNoise.ToString(), GUILayout.Width(50)));
                
                GUILayout.Label("Measurement Noise:", GUILayout.Width(120));
                measurementNoise = float.Parse(GUILayout.TextField(measurementNoise.ToString(), GUILayout.Width(50)));
                GUILayout.EndHorizontal();
                
                mapping.FilterSettings["processNoise"] = processNoise;
                mapping.FilterSettings["measurementNoise"] = measurementNoise;
            }
            
            // Update settings
            mapping.Settings["horizontalSensitivity"] = hSensitivity;
            mapping.Settings["verticalSensitivity"] = vSensitivity;
            mapping.Settings["flipY"] = flipY;
            mapping.Settings["filter"] = filterType;
            
            // Serialize filter button
            if (GUILayout.Button("Generate Serialized Filter", GUILayout.Width(180)))
            {
                GenerateSerializedFilter(mapping);
            }
        }
        
        private void DrawIncrementalAxisMapping(AxisMappingEntry mapping)
        {
            // Ensure settings dictionary exists
            if (mapping.Settings == null)
                mapping.Settings = new Dictionary<string, object>();
            
            // Get current values with defaults
            float interval = 0.02f;
            
            if (mapping.Settings.TryGetValue("interval", out var intervalObj))
                interval = ConvertToFloat(intervalObj);
            
            // Draw settings
            GUILayout.BeginHorizontal();
            GUILayout.Label("Increment Action:", GUILayout.Width(100));
            mapping.IncrementAction = GUILayout.TextField(mapping.IncrementAction ?? "", GUILayout.Width(150));
            GUILayout.EndHorizontal();
            
            GUILayout.BeginHorizontal();
            GUILayout.Label("Decrement Action:", GUILayout.Width(100));
            mapping.DecrementAction = GUILayout.TextField(mapping.DecrementAction ?? "", GUILayout.Width(150));
            GUILayout.EndHorizontal();
            
            GUILayout.BeginHorizontal();
            GUILayout.Label("Interval:", GUILayout.Width(100));
            interval = float.Parse(GUILayout.TextField(interval.ToString(), GUILayout.Width(50)));
            GUILayout.EndHorizontal();
            
            // Update settings
            mapping.Settings["interval"] = interval;
        }
        
        private void GenerateSerializedFilter(AxisMappingEntry mapping)
        {
            if (mapping.Settings.TryGetValue("filter", out var filterTypeObj) && 
                filterTypeObj is string filterTypeStr)
            {
                if (System.Enum.TryParse<InputFilterFactory.FilterType>(filterTypeStr, true, out var filterType))
                {
                    var filter = InputFilterFactory.CreateFilter(filterType);
                    if (filter != null && mapping.FilterSettings != null)
                    {
                        // Apply settings to the filter
                        FilterSerializer.ApplySettings(filter, mapping.FilterSettings);
                        
                        // Serialize the filter
                        mapping.SerializedFilter = FilterSerializer.Serialize(filter);
                        Debug.Log($"Generated serialized filter: {mapping.SerializedFilter}");
                    }
                }
            }
        }
        
        private float ConvertToFloat(object value)
        {
            if (value is float floatValue)
                return floatValue;
            if (value is double doubleValue)
                return (float)doubleValue;
            if (value is int intValue)
                return intValue;
            
            return 0f;
        }
        
        private void LoadConfiguration()
        {
            if (File.Exists(_configPath))
            {
                _currentConfig = _configManager.LoadConfiguration(_configPath);
                Debug.Log($"Loaded configuration from {_configPath}");
            }
            else
            {
                _currentConfig = _configManager.CreateDefaultConfiguration();
                Debug.Log("Created default configuration");
            }
        }
        
        private void SaveConfiguration()
        {
            if (_currentConfig != null)
            {
                _configManager.SaveConfiguration(_currentConfig, _configPath);
                Debug.Log($"Saved configuration to {_configPath}");
            }
        }
        
        private void ApplyConfiguration()
        {
            if (_currentConfig != null && _bleHidManager != null && _bleHidManager.InputRouter != null)
            {
                if (_bleHidManager.InputRouter.Mapping != null)
                {
                    _bleHidManager.InputRouter.Mapping.ApplyConfiguration(_currentConfig);
                    Debug.Log("Applied configuration to input router");
                }
                else
                {
                    Debug.LogWarning("Cannot apply configuration: InputRouter.Mapping is null");
                }
            }
            else
            {
                Debug.LogWarning("Cannot apply configuration: BleHidManager or InputRouter is null");
            }
        }
    }
}
