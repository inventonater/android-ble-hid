using System;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater
{
    public static class InputDeviceMappingExtensions
    {
        public static void AppendValue<T1, T2>(this Dictionary<T1, List<T2>> mapping, T1 key, T2 value)
        {
            if (!mapping.TryGetValue(key, out var actions))
            {
                actions = new List<T2>();
                mapping.Add(key, actions);
            }

            actions.Add(value);
        }
    }

    [DefaultExecutionOrder(ExecutionOrder.InputMapping)]
    public class InputDeviceMapping
    {
        private readonly Dictionary<InputEvent, List<EInputAction>> _buttonMapping = new();
        public IReadOnlyDictionary<InputEvent, List<EInputAction>> ButtonMapping => _buttonMapping;

        private readonly List<IAxisMapping> _axisMappings = new();
        public IReadOnlyList<IAxisMapping> AxisMappings => _axisMappings;

        public InputDeviceMapping(string name, ActionRegistry actionRegistry, List<(InputEvent, EInputAction)> map, List<IAxisMapping> axisMappings)
        {
            Name = name;
            Registry = actionRegistry;
            foreach (var entry in map) Add(entry.Item1, entry.Item2);
            foreach (var entry in axisMappings) Add(entry);
        }

        public string Name { get; }
        public ActionRegistry Registry { get; }
        public Action GetAction(EInputAction id) => Registry.GetAction(id);

        public void Add(InputEvent e, EInputAction a) => _buttonMapping.AppendValue(e, a);
        public void Add(IAxisMapping axisMapping) => _axisMappings.Add(axisMapping);
        public void AddAction(InputEvent inputEvent, EInputAction action)
        {
            _buttonMapping.AppendValue(inputEvent, action);
        }
        
        public void RemoveAction(InputEvent inputEvent, EInputAction action)
        {
            if (_buttonMapping.TryGetValue(inputEvent, out var actions))
            {
                actions.Remove(action);
                // Remove the key if no actions remain
                if (actions.Count == 0)
                {
                    _buttonMapping.Remove(inputEvent);
                }
            }
        }
    }
}
