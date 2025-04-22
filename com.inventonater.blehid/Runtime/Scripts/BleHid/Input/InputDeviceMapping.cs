using System;
using System.Collections.Generic;
using System.IO;
using Unity.Profiling;
using UnityEngine;

namespace Inventonater.BleHid
{
    [DefaultExecutionOrder(ExecutionOrder.Process)]
    public class InputDeviceMapping : MonoBehaviour
    {
        [SerializeField] private string _configurationPath = "";
        [SerializeField] private bool _loadDefaultConfigOnStart = false;
        
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
                LoggingManager.Instance.AddLogEntry("Loading configuration...");

                if (string.IsNullOrEmpty(_configurationPath))
                {
                    _configurationPath = _configManager.GetDefaultConfigPath();
                }
                
                LoadConfiguration(_configurationPath);
            }
            else
            {
                LoggingManager.Instance.AddLogEntry("Loading hardcoded configuration");
                AddPressRelease(BleHidButtonEvent.Id.Primary, 0);
                AddPressRelease(BleHidButtonEvent.Id.Secondary, 1);
                AddDirection(BleHidDirection.Up, BleHidConstants.KEY_UP);
                AddDirection(BleHidDirection.Right, BleHidConstants.KEY_RIGHT);
                AddDirection(BleHidDirection.Down, BleHidConstants.KEY_DOWN);
                AddDirection(BleHidDirection.Left, BleHidConstants.KEY_LEFT);
                _axisMappings.Add(MousePositionFilter = new MousePositionFilter(Mouse));
                _axisMappings.Add(new AxisMappingIncremental(BleHidAxis.Z, () => Media.VolumeUp(), () => Media.VolumeDown()));
            }
        }
        private void AddDirection(BleHidDirection dir, byte hidConstant) => DirectionMapping.Add(dir, () => Keyboard.SendKey(hidConstant));

        public void AddPressRelease(BleHidButtonEvent.Id button, int mouseButtonId)
        {
            ButtonMapping.Add(new BleHidButtonEvent(button, BleHidButtonEvent.Action.Press), () => Mouse.PressMouseButton(mouseButtonId));
            ButtonMapping.Add(new BleHidButtonEvent(button, BleHidButtonEvent.Action.Release), () => Mouse.ReleaseMouseButton(mouseButtonId));
        }
        public void AddTap(BleHidButtonEvent.Id button, byte hidConstant) =>
            ButtonMapping.Add(new BleHidButtonEvent(button, BleHidButtonEvent.Action.Tap), () => Keyboard.SendKey(hidConstant));

        public void AddDoubleTap(BleHidButtonEvent.Id button, byte hidConstant) =>
            ButtonMapping.Add(new BleHidButtonEvent(button, BleHidButtonEvent.Action.DoubleTap), () => Keyboard.SendKey(hidConstant));
        
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

        static readonly ProfilerMarker _profileMarkerButtonEvent = new("BleHid.InputDeviceMapping.Update.ButtonEvent");
        static readonly ProfilerMarker _profileMarkerDirection = new("BleHid.InputDeviceMapping.Update.DirectionMapping");
        static readonly ProfilerMarker _profileMarkerAxis = new("BleHid.InputDeviceMapping.Update.AxisMapping");


        // ExecutionOrder Process
        private void Update()
        {
            using (_profileMarkerButtonEvent.Auto())
            {
                if (_pendingButtonEvent != BleHidButtonEvent.None && ButtonMapping.TryGetValue(_pendingButtonEvent, out var buttonAction))
                {
                    buttonAction();
                    _pendingButtonEvent = BleHidButtonEvent.None;
                }
            }

            using (_profileMarkerDirection.Auto())
            {
                if (_pendingDirection != BleHidDirection.None && DirectionMapping.TryGetValue(_pendingDirection, out var directionAction))
                {
                    directionAction();
                    _pendingDirection = BleHidDirection.None;
                }
            }

            using (_profileMarkerAxis.Auto())
            {
                foreach (var axisMapping in _axisMappings) axisMapping.Update(Time.time);
            }
        }
    }
}
