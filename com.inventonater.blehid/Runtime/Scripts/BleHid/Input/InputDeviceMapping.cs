using System;
using System.Collections.Generic;
using System.IO;
using UnityEngine;

namespace Inventonater.BleHid
{
    [DefaultExecutionOrder(ExecutionOrder.Process)]
    public class InputDeviceMapping : MonoBehaviour
    {
        [SerializeField] private string _configurationPath = "";
        [SerializeField] private bool _loadDefaultConfigOnStart = true;
        
        public MousePositionFilter MousePositionFilter { get; private set; }

        public readonly Dictionary<BleHidButtonEvent, Action> ButtonMapping = new();
        public readonly Dictionary<BleHidDirection, Action> DirectionMapping = new();
        private readonly List<IAxisMapping> _axisMappings = new();

        [SerializeField] private BleHidButtonEvent _pendingButtonEvent;
        [SerializeField] private BleHidDirection _pendingDirection;

        private MappingConfiguration _currentConfig;
        private ActionResolver _actionResolver;
        private AxisMappingFactory _axisMappingFactory;
        private MappingConfigurationManager _configManager;

        private BleBridge BleBridge => BleHidManager.Instance.BleBridge;
        private MouseBridge Mouse => BleBridge.Mouse;
        private KeyboardBridge Keyboard => BleBridge.Keyboard;
        private MediaBridge Media => BleBridge.Media;

        private void Awake()
        {
            _actionResolver = new ActionResolver(BleBridge);
            _axisMappingFactory = new AxisMappingFactory(BleBridge, _actionResolver);
            _configManager = new MappingConfigurationManager();
            
            if (_loadDefaultConfigOnStart)
            {
                if (string.IsNullOrEmpty(_configurationPath))
                {
                    _configurationPath = _configManager.GetDefaultConfigPath();
                }
                
                LoadConfiguration(_configurationPath);
            }
        }
        
        public void LoadConfiguration(string path)
        {
            try
            {
                if (File.Exists(path))
                {
                    _currentConfig = _configManager.LoadConfiguration(path);
                }
                else
                {
                    _currentConfig = _configManager.CreateDefaultConfiguration();
                    _configManager.SaveConfiguration(_currentConfig, path);
                }
                
                ApplyConfiguration(_currentConfig);
                LoggingManager.Instance.AddLogEntry($"Loaded input configuration: {_currentConfig.Name}");
            }
            catch (Exception e)
            {
                Debug.LogError($"Failed to load configuration: {e.Message}");
                
                // Fall back to default configuration
                _currentConfig = _configManager.CreateDefaultConfiguration();
                ApplyConfiguration(_currentConfig);
            }
        }
        
        public void SaveConfiguration(string path)
        {
            if (_currentConfig != null)
            {
                _configManager.SaveConfiguration(_currentConfig, path);
            }
        }

        public void ApplyConfiguration(MappingConfiguration config)
        {
            // Clear existing mappings
            ButtonMapping.Clear();
            DirectionMapping.Clear();
            _axisMappings.Clear();
            
            // Apply button mappings
            foreach (var mapping in config.ButtonMappings)
            {
                ApplyButtonMapping(mapping);
            }
            
            // Apply direction mappings
            foreach (var mapping in config.DirectionMappings)
            {
                ApplyDirectionMapping(mapping);
            }
            
            // Apply axis mappings
            foreach (var mapping in config.AxisMappings)
            {
                ApplyAxisMapping(mapping);
            }
        }
        
        private void ApplyButtonMapping(ButtonMappingEntry mapping)
        {
            // Parse input event (e.g., "Primary.Press")
            string[] parts = mapping.InputEvent.Split('.');
            if (parts.Length != 2) return;
            
            if (!Enum.TryParse<BleHidButtonEvent.Id>(parts[0], out var buttonId)) return;
            if (!Enum.TryParse<BleHidButtonEvent.Action>(parts[1], out var buttonAction)) return;
            
            var buttonEvent = new BleHidButtonEvent(buttonId, buttonAction);
            var parameters = new Dictionary<string, object>();
            
            // Add any additional parameters
            if (!string.IsNullOrEmpty(mapping.KeyCode))
                parameters["keyCode"] = mapping.KeyCode;
            
            // Resolve and register the action
            ButtonMapping[buttonEvent] = _actionResolver.ResolveAction(mapping.Action, parameters);
        }
        
        private void ApplyDirectionMapping(DirectionMappingEntry mapping)
        {
            if (!Enum.TryParse<BleHidDirection>(mapping.InputDirection, out var direction)) return;
            
            var parameters = new Dictionary<string, object>();
            
            // Add any additional parameters
            if (!string.IsNullOrEmpty(mapping.KeyCode))
                parameters["keyCode"] = mapping.KeyCode;
            
            // Resolve and register the action
            DirectionMapping[direction] = _actionResolver.ResolveAction(mapping.Action, parameters);
        }
        
        private void ApplyAxisMapping(AxisMappingEntry mapping)
        {
            var axisMapping = _axisMappingFactory.CreateAxisMapping(mapping);
            if (axisMapping != null)
            {
                _axisMappings.Add(axisMapping);
                
                // Store reference to MousePositionFilter if it's that type
                if (axisMapping is MousePositionFilter mouseFilter)
                {
                    MousePositionFilter = mouseFilter;
                }
            }
        }

        public void SetDirection(BleHidDirection direction) => _pendingDirection = direction;
        public void SetButtonEvent(BleHidButtonEvent buttonEvent) => _pendingButtonEvent = buttonEvent;

        public void SetPosition(Vector3 absolutePosition)
        {
            foreach (var axisMapping in _axisMappings) axisMapping.SetValue(absolutePosition);
        }

        public void ResetPosition()
        {
            foreach (var axisMapping in _axisMappings) axisMapping.ResetPosition();
        }

        // ExecutionOrder Process
        private void Update()
        {
            if (_pendingButtonEvent != BleHidButtonEvent.None && ButtonMapping.TryGetValue(_pendingButtonEvent, out var buttonAction))
            {
                buttonAction();
                _pendingButtonEvent = BleHidButtonEvent.None;
            }

            if (_pendingDirection != BleHidDirection.None && DirectionMapping.TryGetValue(_pendingDirection, out var directionAction))
            {
                directionAction();
                _pendingDirection = BleHidDirection.None;
            }

            foreach (var axisMapping in _axisMappings) axisMapping.Update(Time.time);
        }
    }
}
