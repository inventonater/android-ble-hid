using System;
using System.Collections.Generic;
using UnityEngine;
using static Inventonater.BleHid.EInputEvent;

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
        private readonly ActionRegistry _actionRegistry;
        private readonly Dictionary<InputEvent, List<Action>> _buttonMapping = new();
        public IReadOnlyDictionary<InputEvent, List<Action>> ButtonMapping => _buttonMapping;

        private readonly List<IAxisMapping> _axisMappings = new();
        public IReadOnlyList<IAxisMapping> AxisMappings => _axisMappings;

        public InputDeviceMapping(string name, ActionRegistry actionRegistry)
        {
            Name = name;
            _actionRegistry = actionRegistry;
        }

        public string Name { get; }

        public void Add(InputEvent.Id id, InputEvent.Phase buttonPhase, Action action) => Add(new InputEvent(id, buttonPhase), action);
        public void Add(InputEvent buttonEvent, Action action) => _buttonMapping.AppendValue(buttonEvent, action);
        public void Add(EInputEvent buttonEvent, Action action) => _buttonMapping.AppendValue(buttonEvent.ToInputEvent(), action);
        public void Add(EInputEvent e, EInputAction a) => Add(e, _actionRegistry.GetAction(a));
        public void AddPressRelease(InputEvent.Id button, EInputAction press, EInputAction release)
        {
            Add(button, InputEvent.Phase.Press, _actionRegistry.GetAction(press));
            Add(button, InputEvent.Phase.Release, _actionRegistry.GetAction(release));
        }
        public void Add(IAxisMapping axisMapping) => _axisMappings.Add(axisMapping);

        public static InputDeviceMapping BleMouse(ActionRegistry registry)
        {
            var mapping = new InputDeviceMapping("BleMouse", registry);
            mapping.AddPressRelease(InputEvent.Id.Primary, EInputAction.MouseLeftPress, EInputAction.MouseLeftRelease);
            mapping.AddPressRelease(InputEvent.Id.Secondary, EInputAction.MouseRightPress, EInputAction.MouseRightRelease);
            mapping.Add(Up, EInputAction.KeyboardArrowUp);
            mapping.Add(Right, EInputAction.KeyboardArrowRight);
            mapping.Add(Down, EInputAction.KeyboardArrowDown);
            mapping.Add(Left, EInputAction.KeyboardArrowLeft);

            mapping.Add(new MousePositionAxisMapping(registry.GetMouseMoveAction()));
            mapping.Add(new SingleIncrementalAxisMapping(Axis.Z, registry.GetAction(EInputAction.MediaVolumeUp), registry.GetAction(EInputAction.MediaVolumeDown)));
            return mapping;
        }

        public static InputDeviceMapping BleMedia(ActionRegistry registry)
        {
            var mapping = new InputDeviceMapping("BleMedia", registry);

            mapping.Add(PrimaryDoubleTap, EInputAction.MediaPlayPause);
            mapping.Add(Right, EInputAction.MediaNextTrack);
            mapping.Add(Left, EInputAction.MediaPreviousTrack);
            mapping.Add(Up, EInputAction.MediaMute);
            mapping.Add(Down, EInputAction.MediaMute);

            mapping.Add(new SingleIncrementalAxisMapping(Axis.Z, registry.GetAction(EInputAction.MediaVolumeUp), registry.GetAction(EInputAction.MediaVolumeDown)));
            return mapping;
        }

        public static InputDeviceMapping LocalMedia(ActionRegistry registry)
        {
            var mapping = new InputDeviceMapping("LocalMedia", registry);

            mapping.Add(PrimaryDoubleTap, EInputAction.LocalPlayPause);
            mapping.Add(Right, EInputAction.LocalNextTrack);
            mapping.Add(Left, EInputAction.LocalPreviousTrack);
            mapping.Add(Up, EInputAction.LocalMute);
            mapping.Add(Down, EInputAction.LocalMute);

            mapping.Add(new SingleIncrementalAxisMapping(Axis.Z, registry.GetAction(EInputAction.LocalVolumeUp), registry.GetAction(EInputAction.LocalVolumeDown)));
            return mapping;
        }

        public static InputDeviceMapping LocalDPad(ActionRegistry registry)
        {
            var mapping = new InputDeviceMapping("LocalDPad", registry);

            mapping.Add(PrimaryTap, EInputAction.LocalDPadCenter);
            mapping.Add(SecondaryTap, EInputAction.LocalBack);
            mapping.Add(SecondaryDoubleTap, EInputAction.LocalHome);
            mapping.Add(Up, EInputAction.LocalDPadUp);
            mapping.Add(Right, EInputAction.LocalDPadRight);
            mapping.Add(Down, EInputAction.LocalDPadDown);
            mapping.Add(Left, EInputAction.LocalDPadLeft);

            mapping.Add(new SingleIncrementalAxisMapping(Axis.Z, registry.GetAction(EInputAction.LocalVolumeUp), registry.GetAction(EInputAction.LocalVolumeDown)));
            return mapping;
        }

        private static readonly Vector2 SamsungResolution = new Vector2(1440, 3088);
        private static readonly Vector2 Pixel9XLResolution = new Vector2(1344, 2992);
        private static Vector2 Resolution => Pixel9XLResolution;
        private static Vector2 ClampToScreen(Vector2 vector2) => new(Mathf.Clamp(vector2.x, 0, Resolution.x), Mathf.Clamp(vector2.y, 0, Resolution.y));
        private static Vector2 ScreenCenter() => Resolution * 0.5f;
        public static InputDeviceMapping LocalDrag(BleBridge bridge)
        {
            var serviceBridge = bridge.AccessibilityService;
            var mapping = new InputDeviceMapping("LocalDrag", bridge.ActionRegistry);

            var swipeMapping = new MousePositionAxisMapping(serviceBridge.SwipeExtend, requirePress: true);
            mapping.Add(PrimaryPress, () => serviceBridge.SwipeBegin(ScreenCenter()));
            mapping.Add(PrimaryRelease, () => serviceBridge.SwipeEnd());
            mapping.Add(swipeMapping);

            return mapping;
        }
    }
}
