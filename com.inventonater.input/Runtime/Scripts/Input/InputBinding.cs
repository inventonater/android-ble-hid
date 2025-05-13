using System;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater
{
    [Serializable]
    public class ButtonMapEntry
    {
        public ButtonEvent ButtonEvent;
        public MappableActionId ActionId;

        public ButtonMapEntry(ButtonEvent buttonEvent, MappableActionId mappableActionId)
        {
            ButtonEvent = buttonEvent;
            ActionId = mappableActionId;
        }
    }

    [Serializable]
    public class InputMap
    {
        private List<ButtonMapEntry> _buttons;
        private List<IAxisMapping> _axis;

        public List<ButtonMapEntry> Buttons => _buttons;
        public List<IAxisMapping> Axis => _axis;

        public Action<ButtonMapEntry> WhenButtonMapEntryAdded = delegate { };
        public Action<ButtonMapEntry> WhenButtonMapEntryRemoved = delegate { };

        public Action<IAxisMapping> WhenAxisMapEntryAdded = delegate { };
        public Action<IAxisMapping> WhenAxisMapEntryRemoved = delegate { };

        public InputMap() : this(new List<ButtonMapEntry>(), new List<IAxisMapping>())
        {
        }

        public InputMap(List<ButtonMapEntry> buttons, List<IAxisMapping> axisMappings)
        {
            _buttons = buttons;
            _axis = axisMappings;
        }

        public void Add(ButtonEvent buttonEvent, MappableActionId mappableActionId) => Add(new ButtonMapEntry(buttonEvent, mappableActionId));

        public void Add(ButtonMapEntry buttonMapEntry)
        {
            _buttons.Add(buttonMapEntry);
            WhenButtonMapEntryAdded(buttonMapEntry);
        }

        public void Remove(ButtonEvent buttonEvent, MappableActionId mappableActionId) => Remove(new ButtonMapEntry(buttonEvent, mappableActionId));

        public void Remove(ButtonMapEntry buttonMapEntry)
        {
            _buttons.Remove(buttonMapEntry);
            WhenButtonMapEntryRemoved(buttonMapEntry);
        }

        public void Add(IAxisMapping axisMapping)
        {
            _axis.Add(axisMapping);
            WhenAxisMapEntryAdded(axisMapping);
        }

        public void Remove(IAxisMapping axisMapping)
        {
            _axis.Remove(axisMapping);
            WhenAxisMapEntryRemoved(axisMapping);
        }
    }

    public delegate void MouseMoveActionDelegate(Vector2 delta);

    [Serializable, DefaultExecutionOrder(ExecutionOrder.InputMapping)]
    public class InputBinding
    {
        public string Name { get; }
        public InputMap Map { get; }

        [SerializeField] private MappableActionRegistry _registry;
        public MappableActionRegistry Registry => _registry;

        public InputBinding(string name, MappableActionRegistry registry, InputMap map)
        {
            _registry = registry;
            Name = name;
            Map = map;
        }

        public void Invoke(ButtonEvent buttonEvent)
        {
            foreach (var buttonMapEntry in Map.Buttons)
            {
                if (buttonMapEntry.ButtonEvent != buttonEvent) continue;

                var mappableActionId = buttonMapEntry.ActionId;
                try
                {
                    if (_registry.TryGetMappedAction(mappableActionId, out var mappedActions))
                    {
                        mappedActions.Invoke();
                    }
                }
                catch (Exception e) { LoggingManager.Instance.Exception(e); }
            }

            foreach (var axisMapping in Map.Axis)
            {
                try { axisMapping.Handle(buttonEvent); }
                catch (Exception e) { LoggingManager.Instance.Exception(e); }
            }
        }

        public void Update(float time)
        {
            foreach (var axisMapping in Map.Axis) axisMapping.Update(time);
        }

        public void AddPositionDelta(Vector3 delta)
        {
            foreach (var mapping in Map.Axis) mapping.AddDelta(delta);
        }

        public void Chirp()
        {
            if (_registry.TryGetMappedAction(MappableActionId.Chirp, out var mappedActions)) mappedActions.Invoke();
        }

        public void Add(ButtonEvent selectedEvent, MappableActionId selectedAction)
        {
            throw new NotImplementedException();
        }
    }

    public static class InputBindingExtensions
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

        public static void RemoveValue<T1, T2>(this Dictionary<T1, List<T2>> mapping, T1 key, T2 value)
        {
            if (!mapping.TryGetValue(key, out var actions)) return;
            actions.Remove(value);
            if (actions.Count == 0) mapping.Remove(key);
        }
    }
}
