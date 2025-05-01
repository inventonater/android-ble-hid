using System;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
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

        public InputDeviceMapping(string name, ActionRegistry actionRegistry)
        {
            Name = name;
            Registry = actionRegistry;
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

        public static InputDeviceMapping Create(string name, List<(InputEvent, EInputAction)> map, List<IAxisMapping> axisMappings, ActionRegistry actionRegistry)
        {
            var inputDeviceMapping = new InputDeviceMapping(name, actionRegistry);
            foreach (var entry in map) inputDeviceMapping.Add(entry.Item1, entry.Item2);
            foreach (var entry in axisMappings) inputDeviceMapping.Add(entry);
            return inputDeviceMapping;
        }

        public static InputDeviceMapping BleMouse(ActionRegistry registry)
        {
            var buttons = new List<(InputEvent, EInputAction)>
            {
                (InputEvent.PrimaryPress, EInputAction.PrimaryPress),
                (InputEvent.PrimaryRelease, EInputAction.PrimaryRelease),
                (InputEvent.SecondaryPress, EInputAction.SecondaryPress),
                (InputEvent.SecondaryRelease, EInputAction.SecondaryRelease),
                (InputEvent.Up, EInputAction.Up),
                (InputEvent.Right, EInputAction.Right),
                (InputEvent.Down, EInputAction.Down),
                (InputEvent.Left, EInputAction.Left),
            };
            var axisMappings = new List<IAxisMapping>
            {
                new MousePositionAxisMapping(registry.MouseMoveAction),
                new SingleIncrementalAxisMapping(Axis.Z, registry.GetAction(EInputAction.VolumeUp), registry.GetAction(EInputAction.VolumeDown)),
            };

            return Create("BleMouse", buttons, axisMappings, registry);
        }

        public static InputDeviceMapping BleMedia(ActionRegistry registry)
        {
            var buttons = new List<(InputEvent, EInputAction)>
            {
                (InputEvent.PrimaryDoubleTap, EInputAction.PlayPause),
                (InputEvent.Right, EInputAction.NextTrack),
                (InputEvent.Left, EInputAction.PreviousTrack),
                (InputEvent.Up, EInputAction.Mute),
                (InputEvent.Down, EInputAction.Mute),
            };
            var axisMappings = new List<IAxisMapping> { new SingleIncrementalAxisMapping(Axis.Z, registry.GetAction(EInputAction.VolumeUp), registry.GetAction(EInputAction.VolumeDown)) };

            return Create("BleMedia", buttons, axisMappings, registry);
        }

        public static InputDeviceMapping LocalMedia(ActionRegistry registry)
        {
            var buttons = new List<(InputEvent, EInputAction)>
            {
                (InputEvent.PrimaryDoubleTap, EInputAction.PlayPause),
                (InputEvent.Right, EInputAction.NextTrack),
                (InputEvent.Left, EInputAction.PreviousTrack),
                (InputEvent.Up, EInputAction.Mute),
                (InputEvent.Down, EInputAction.Mute),
            };
            var axisMappings = new List<IAxisMapping> { new SingleIncrementalAxisMapping(Axis.Z, registry.GetAction(EInputAction.VolumeUp), registry.GetAction(EInputAction.VolumeDown)) };

            return Create("LocalMedia", buttons, axisMappings, registry);
        }

        public static InputDeviceMapping LocalDPad(ActionRegistry registry)
        {
            var buttons = new List<(InputEvent, EInputAction)>
            {
                (InputEvent.PrimaryTap, EInputAction.Select),
                (InputEvent.SecondaryTap, EInputAction.Back),
                (InputEvent.SecondaryDoubleTap, EInputAction.Home),
                (InputEvent.Up, EInputAction.Up),
                (InputEvent.Right, EInputAction.Right),
                (InputEvent.Down, EInputAction.Down),
                (InputEvent.Left, EInputAction.Left),
            };
            var axisMappings = new List<IAxisMapping> { new SingleIncrementalAxisMapping(Axis.Z, registry.GetAction(EInputAction.VolumeUp), registry.GetAction(EInputAction.VolumeDown)) };
            return Create("LocalDPad", buttons, axisMappings, registry);
        }

        private static readonly Vector2 SamsungResolution = new Vector2(1440, 3088);
        private static readonly Vector2 Pixel9XLResolution = new Vector2(1344, 2992);
        private static Vector2 Resolution => Pixel9XLResolution;
        private static Vector2 ClampToScreen(Vector2 vector2) => new(Mathf.Clamp(vector2.x, 0, Resolution.x), Mathf.Clamp(vector2.y, 0, Resolution.y));

        private static Vector2 ScreenCenter() => Resolution * 0.5f;
        // public static InputDeviceMapping LocalDrag(BleBridge bridge)
        // {
        //     var serviceBridge = bridge.AccessibilityService;
        //     var mapping = new InputDeviceMapping("LocalDrag", bridge.ActionRegistry);
        //     var swipeMapping = new MousePositionAxisMapping(serviceBridge.SwipeExtend, requirePress: true);
        //     mapping.Add(PrimaryPress, () => serviceBridge.SwipeBegin(ScreenCenter()));
        //     mapping.Add(PrimaryRelease, () => serviceBridge.SwipeEnd());
        //     mapping.Add(swipeMapping);
        //     return mapping;
        // }
    }
}
