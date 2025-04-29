using System;
using System.Collections.Generic;
using UnityEngine;
using static Inventonater.BleHid.InputEvent.Direction;

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
        private readonly Dictionary<InputEvent, List<Action>> _buttonMapping = new();
        public IReadOnlyDictionary<InputEvent, List<Action>> ButtonMapping => _buttonMapping;

        private readonly List<IAxisMapping> _axisMappings = new();
        public IReadOnlyList<IAxisMapping> AxisMappings => _axisMappings;

        public InputDeviceMapping(string name) => Name = name;
        public string Name { get; }

        public void Add(InputEvent.Id id, InputEvent.Temporal buttonTemporal, Action action) => Add(new InputEvent(id, buttonTemporal), action);
        public void Add(InputEvent.Direction direction, Action action) => _buttonMapping.AppendValue(new InputEvent(direction), action);
        public void Add(InputEvent buttonEvent, Action action) => _buttonMapping.AppendValue(buttonEvent, action);

        public void AddPressRelease(InputEvent.Id button, Action press, Action release)
        {
            Add(button, InputEvent.Temporal.Press, press);
            Add(button, InputEvent.Temporal.Release, release);
        }

        public void Add(IAxisMapping axisMapping) => _axisMappings.Add(axisMapping);

        public static InputDeviceMapping BleMouse(BleBridge bridge)
        {
            var mouse = bridge.Mouse;
            var media = bridge.Media;
            var keyboard = bridge.Keyboard;

            var mapping = new InputDeviceMapping("BleMouse");
            mapping.AddPressRelease(InputEvent.Id.Primary, () => mouse.PressMouseButton(0), () => mouse.ReleaseMouseButton(0));
            mapping.AddPressRelease(InputEvent.Id.Secondary, () => mouse.PressMouseButton(1), () => mouse.ReleaseMouseButton(1));
            mapping.Add(Up, () => keyboard.SendKey(BleHidConstants.KEY_UP));
            mapping.Add(Right, () => keyboard.SendKey(BleHidConstants.KEY_RIGHT));
            mapping.Add(Down, () => keyboard.SendKey(BleHidConstants.KEY_DOWN));
            mapping.Add(Left, () => keyboard.SendKey(BleHidConstants.KEY_LEFT));
            mapping.Add(new MousePositionAxisMapping(mouse));
            mapping.Add(new SingleIncrementalAxisMapping(Axis.Z, () => media.VolumeUp(), () => media.VolumeDown()));
            return mapping;
        }

        public static InputDeviceMapping BleMedia(BleBridge bridge)
        {
            var media = bridge.Media;
            var mapping = new InputDeviceMapping("BleMedia");

            mapping.Add(InputEvent.Id.Primary, InputEvent.Temporal.DoubleTap, () => media.PlayPause());
            mapping.Add(Right, () => media.NextTrack());
            mapping.Add(Left, () => media.PreviousTrack());
            mapping.Add(Up, () => media.Mute());
            mapping.Add(Down, () => media.Mute());
            mapping.Add(new SingleIncrementalAxisMapping(Axis.Z, () => media.VolumeUp(), () => media.VolumeDown()));
            return mapping;
        }

        public static InputDeviceMapping LocalMedia(BleBridge bridge)
        {
            var serviceBridge = bridge.AccessibilityServiceBridge;
            var mapping = new InputDeviceMapping("LocalMediaMapping");

            mapping.Add(InputEvent.Id.Primary, InputEvent.Temporal.DoubleTap, () => serviceBridge.PlayPause());
            mapping.Add(Right, () => serviceBridge.NextTrack());
            mapping.Add(Left, () => serviceBridge.PreviousTrack());
            mapping.Add(Up, () => serviceBridge.Mute());
            mapping.Add(Down, () => serviceBridge.Mute());

            mapping.Add(new SingleIncrementalAxisMapping(Axis.Z, () => serviceBridge.VolumeUp(), () => serviceBridge.VolumeDown()));
            return mapping;
        }

        public static InputDeviceMapping LocalDPadNavigation(BleBridge bridge)
        {
            var serviceBridge = bridge.AccessibilityServiceBridge;
            var mapping = new InputDeviceMapping("LocalDPadNavigation");

            mapping.Add(InputEvent.Id.Primary, InputEvent.Temporal.Tap, () => serviceBridge.DPadCenter());
            mapping.Add(InputEvent.Id.Secondary, InputEvent.Temporal.Tap, () => serviceBridge.Back());
            mapping.Add(InputEvent.Id.Secondary, InputEvent.Temporal.DoubleTap, () => serviceBridge.Home());

            mapping.Add(Up, () => serviceBridge.DPadUp());
            mapping.Add(Right, () => serviceBridge.DPadRight());
            mapping.Add(Down, () => serviceBridge.DPadDown());
            mapping.Add(Left, () => serviceBridge.DPadLeft());
            mapping.Add(new SingleIncrementalAxisMapping(Axis.Z, () => serviceBridge.VolumeUp(), () => serviceBridge.VolumeDown()));
            return mapping;
        }

        private static readonly Vector2 SamsungResolution = new Vector2(1440, 3088);
        private static readonly Vector2 Pixel9XLResolution = new Vector2(1344, 2992);
        private static Vector2 Resolution => Pixel9XLResolution;
        private static Vector2 ClampToScreen(Vector2 vector2) => new(Mathf.Clamp(vector2.x, 0, Resolution.x), Mathf.Clamp(vector2.y, 0, Resolution.y));
        private static Vector2 ScreenCenter() => Resolution * 0.5f;

        public static InputDeviceMapping LocalDragNavigation(BleBridge bridge)
        {
            var serviceBridge = bridge.AccessibilityServiceBridge;
            var mapping = new InputDeviceMapping("LocalDragNavigation");

            var swipeMapping = new MousePositionAxisMapping(deltaMove => serviceBridge.SwipeExtend(deltaMove), requirePress: true);
            mapping.Add(InputEvent.Id.Primary, InputEvent.Temporal.Press, () => serviceBridge.SwipeBegin(ScreenCenter()));
            mapping.Add(InputEvent.Id.Primary, InputEvent.Temporal.Release, () => serviceBridge.SwipeEnd());
            mapping.Add(swipeMapping);

            return mapping;
        }
    }
}
