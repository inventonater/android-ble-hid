using System;
using System.Collections.Generic;
using System.IO;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class InputDeviceLoader
    {
        private readonly ActionResolver _actionResolver;
        private readonly AxisMappingFactory _axisMappingFactory;
        private readonly MappingConfigurationManager _configManager;

        public InputDeviceLoader(MappingConfigurationManager mappingConfigurationManager, BleBridge bleBridge)
        {
            _configManager = mappingConfigurationManager;
            _actionResolver = new ActionResolver(bleBridge);
            _axisMappingFactory = new AxisMappingFactory(bleBridge, _actionResolver);
        }

        public void SaveConfiguration(MappingConfiguration config, string path) => _configManager.SaveConfiguration(config, path);

        public MappingConfiguration LoadConfiguration(string path)
        {
            try
            {
                if (!File.Exists(path)) throw new FileNotFoundException("Configuration file not found", path);
                return _configManager.LoadConfiguration(path);
            }
            catch (Exception e)
            {
                Debug.LogError($"Failed to load configuration: {e.Message}");
            }

            return _configManager.CreateDefaultConfiguration();
        }

        public InputDeviceMapping LoadConfiguration(MappingConfiguration config)
        {
            var newMapping = new InputDeviceMapping(config.Name);
            foreach (var buttonMapping in config.ButtonMappings) ApplyButtonMapping(newMapping, buttonMapping);
            foreach (var directionMapping in config.DirectionMappings) ApplyDirectionMapping(newMapping, directionMapping);
            foreach (var axisMapping in config.AxisMappings) ApplyAxisMapping(newMapping, axisMapping);
            LoggingManager.Instance.Log($"Loaded input configuration: {config.Name}");
            return newMapping;
        }

        private void ApplyButtonMapping(InputDeviceMapping newMapping, ButtonMappingEntry mapping)
        {
            string[] parts = mapping.InputEvent.Split('.');
            if (parts.Length != 2) return;

            if (!Enum.TryParse<BleHidButtonEvent.Id>(parts[0], out var buttonId)) return;
            if (!Enum.TryParse<BleHidButtonEvent.Action>(parts[1], out var buttonAction)) return;

            var buttonEvent = new BleHidButtonEvent(buttonId, buttonAction);
            var parameters = new Dictionary<string, object>();

            if (!string.IsNullOrEmpty(mapping.KeyCode)) parameters["keyCode"] = mapping.KeyCode;

            newMapping.Add(buttonEvent, _actionResolver.ResolveAction(mapping.Action, parameters));
        }

        private void ApplyDirectionMapping(InputDeviceMapping newMapping, DirectionMappingEntry mapping)
        {
            if (!Enum.TryParse<BleHidDirection>(mapping.InputDirection, out var direction)) return;

            var parameters = new Dictionary<string, object>();
            if (!string.IsNullOrEmpty(mapping.KeyCode)) parameters["keyCode"] = mapping.KeyCode;

            newMapping.Add(direction, _actionResolver.ResolveAction(mapping.Action, parameters));
        }

        private void ApplyAxisMapping(InputDeviceMapping newMapping, AxisMappingEntry mapping)
        {
            IAxisMapping axisMapping = _axisMappingFactory.CreateAxisMapping(mapping);
            if (axisMapping != null) newMapping.Add(axisMapping);
        }
    }
}
