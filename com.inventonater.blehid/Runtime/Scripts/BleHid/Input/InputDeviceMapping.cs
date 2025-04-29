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

        public InputDeviceMapping(string name) => Name = name;

        public string Name { get; }

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

        public static InputDeviceMapping Create(string name, List<(InputEvent, EInputAction)> map, List<IAxisMapping> axisMappings)
        {
            var inputDeviceMapping = new InputDeviceMapping(name);
            foreach (var entry in map) inputDeviceMapping.Add(entry.Item1, entry.Item2);
            foreach (var entry in axisMappings) inputDeviceMapping.Add(entry);
            return inputDeviceMapping;
        }

        public static InputDeviceMapping BleMouse(ActionRegistry registry)
        {
            var buttons = new List<(InputEvent, EInputAction)>
            {
                (InputEvent.PrimaryPress, EInputAction.MouseLeftPress),
                (InputEvent.PrimaryRelease, EInputAction.MouseLeftRelease),
                (InputEvent.SecondaryPress, EInputAction.MouseRightPress),
                (InputEvent.SecondaryRelease, EInputAction.MouseRightRelease),
                (InputEvent.Up, EInputAction.KeyboardArrowUp),
                (InputEvent.Right, EInputAction.KeyboardArrowRight),
                (InputEvent.Down, EInputAction.KeyboardArrowDown),
                (InputEvent.Left, EInputAction.KeyboardArrowLeft),
            };
            var axisMappings = new List<IAxisMapping>
            {
                new MousePositionAxisMapping(registry.GetMouseMoveAction()),
                new SingleIncrementalAxisMapping(Axis.Z, registry.GetAction(EInputAction.MediaVolumeUp), registry.GetAction(EInputAction.MediaVolumeDown)),
            };

            return Create("BleMouse", buttons, axisMappings);
        }

        public static InputDeviceMapping BleMedia(ActionRegistry registry)
        {
            var buttons = new List<(InputEvent, EInputAction)>
            {
                (InputEvent.PrimaryDoubleTap, EInputAction.MediaPlayPause),
                (InputEvent.Right, EInputAction.MediaNextTrack),
                (InputEvent.Left, EInputAction.MediaPreviousTrack),
                (InputEvent.Up, EInputAction.MediaMute),
                (InputEvent.Down, EInputAction.MediaMute),
            };
            var axisMappings = new List<IAxisMapping> { new SingleIncrementalAxisMapping(Axis.Z, registry.GetAction(EInputAction.MediaVolumeUp), registry.GetAction(EInputAction.MediaVolumeDown)) };

            return Create("BleMedia", buttons, axisMappings);
        }

        public static InputDeviceMapping LocalMedia(ActionRegistry registry)
        {
            var buttons = new List<(InputEvent, EInputAction)>
            {
                (InputEvent.PrimaryDoubleTap, EInputAction.LocalPlayPause),
                (InputEvent.Right, EInputAction.LocalNextTrack),
                (InputEvent.Left, EInputAction.LocalPreviousTrack),
                (InputEvent.Up, EInputAction.LocalMute),
                (InputEvent.Down, EInputAction.LocalMute),
            };
            var axisMappings = new List<IAxisMapping> { new SingleIncrementalAxisMapping(Axis.Z, registry.GetAction(EInputAction.LocalVolumeUp), registry.GetAction(EInputAction.LocalVolumeDown)) };

            return Create("LocalMedia", buttons, axisMappings);
        }

        public static InputDeviceMapping LocalDPad(ActionRegistry registry)
        {
            var buttons = new List<(InputEvent, EInputAction)>
            {
                (InputEvent.PrimaryTap, EInputAction.LocalDPadCenter),
                (InputEvent.SecondaryTap, EInputAction.LocalBack),
                (InputEvent.SecondaryDoubleTap, EInputAction.LocalHome),
                (InputEvent.Up, EInputAction.LocalDPadUp),
                (InputEvent.Right, EInputAction.LocalDPadRight),
                (InputEvent.Down, EInputAction.LocalDPadDown),
                (InputEvent.Left, EInputAction.LocalDPadLeft),
            };
            var axisMappings = new List<IAxisMapping> { new SingleIncrementalAxisMapping(Axis.Z, registry.GetAction(EInputAction.LocalVolumeUp), registry.GetAction(EInputAction.LocalVolumeDown)) };
            return Create("LocalDPad", buttons, axisMappings);
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
