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
        private readonly Dictionary<BleHidButtonEvent, List<Action>> _buttonMapping = new();
        public IReadOnlyDictionary<BleHidButtonEvent, List<Action>> ButtonMapping => _buttonMapping;

        private readonly Dictionary<BleHidDirection, List<Action>> _directionMapping = new();
        public IReadOnlyDictionary<BleHidDirection, List<Action>> DirectionMapping => _directionMapping;

        private readonly List<IAxisMapping> _axisMappings = new();
        public IReadOnlyList<IAxisMapping> AxisMappings => _axisMappings;

        public InputDeviceMapping(string name) => Name = name;
        public string Name { get; }

        private void Add(BleHidButtonEvent.Id id, BleHidButtonEvent.Action buttonAction, Action action) => Add(new BleHidButtonEvent(id, buttonAction), action);
        public void Add(BleHidButtonEvent buttonEvent, Action action) => _buttonMapping.AppendValue(buttonEvent, action);
        public void Add(BleHidDirection dir, Action action) => _directionMapping.AppendValue(dir, action);

        public void AddPressRelease(BleHidButtonEvent.Id button, Action press, Action release)
        {
            Add(button, BleHidButtonEvent.Action.Press, press);
            Add(button, BleHidButtonEvent.Action.Release, release);
        }

        public void Add(IAxisMapping axisMapping) => _axisMappings.Add(axisMapping);

        public static InputDeviceMapping BleMouse(BleBridge bridge)
        {
            var mouse = bridge.Mouse;
            var media = bridge.Media;
            var keyboard = bridge.Keyboard;

            var mapping = new InputDeviceMapping("BleMouse");
            mapping.AddPressRelease(BleHidButtonEvent.Id.Primary, () => mouse.PressMouseButton(0), () => mouse.ReleaseMouseButton(0));
            mapping.AddPressRelease(BleHidButtonEvent.Id.Secondary, () => mouse.PressMouseButton(1), () => mouse.ReleaseMouseButton(1));
            mapping.Add(BleHidDirection.Up, () => keyboard.SendKey(BleHidConstants.KEY_UP));
            mapping.Add(BleHidDirection.Right, () => keyboard.SendKey(BleHidConstants.KEY_RIGHT));
            mapping.Add(BleHidDirection.Down, () => keyboard.SendKey(BleHidConstants.KEY_DOWN));
            mapping.Add(BleHidDirection.Left, () => keyboard.SendKey(BleHidConstants.KEY_LEFT));
            mapping.Add(new MousePositionAxisMapping(mouse));
            mapping.Add(new SingleIncrementalAxisMapping(BleHidAxis.Z, () => media.VolumeUp(), () => media.VolumeDown()));
            return mapping;
        }

        public static InputDeviceMapping BleMedia(BleBridge bridge)
        {
            var media = bridge.Media;
            var mapping = new InputDeviceMapping("BleMedia");

            mapping.Add(BleHidButtonEvent.Id.Primary, BleHidButtonEvent.Action.DoubleTap, () => media.PlayPause());
            mapping.Add(BleHidDirection.Right, () => media.NextTrack());
            mapping.Add(BleHidDirection.Left, () => media.PreviousTrack());
            mapping.Add(BleHidDirection.Up, () => media.Mute());
            mapping.Add(BleHidDirection.Down, () => media.Mute());
            mapping.Add(new SingleIncrementalAxisMapping(BleHidAxis.Z, () => media.VolumeUp(), () => media.VolumeDown()));
            return mapping;
        }

        public static InputDeviceMapping LocalMedia(BleBridge bridge)
        {
            var serviceBridge = bridge.AccessibilityServiceBridge;

            var mapping = new InputDeviceMapping("LocalMediaMapping");

            mapping.Add(BleHidButtonEvent.Id.Primary, BleHidButtonEvent.Action.DoubleTap, () => serviceBridge.PlayPause());
            mapping.Add(BleHidDirection.Right, () => serviceBridge.NextTrack());
            mapping.Add(BleHidDirection.Left, () => serviceBridge.PreviousTrack());
            mapping.Add(BleHidDirection.Up, () => serviceBridge.Mute());
            mapping.Add(BleHidDirection.Down, () => serviceBridge.Mute());

            mapping.Add(new SingleIncrementalAxisMapping(BleHidAxis.Z, () => serviceBridge.VolumeUp(), () => serviceBridge.VolumeDown()));
            return mapping;
        }

        private static readonly Vector2 SamsungResolution = new Vector2(1440, 3088);
        private static readonly Vector2 Pixel9XLResolution = new Vector2(1344, 2992);

        private static Vector2 Resolution => Pixel9XLResolution;
        private static Vector2 ClampToScreen(Vector2 vector2) => new(Mathf.Clamp(vector2.x, 0, Resolution.x), Mathf.Clamp(vector2.y, 0, Resolution.y));
        private static Vector2 ScreenCenter() => Resolution * 0.5f;

        public static InputDeviceMapping LocalDPadNavigation(BleBridge bridge)
        {
            var serviceBridge = bridge.AccessibilityServiceBridge;
            var mapping = new InputDeviceMapping("LocalDPadNavigation");

            mapping.Add(BleHidButtonEvent.Id.Primary, BleHidButtonEvent.Action.Tap, () => serviceBridge.DPadCenter());
            mapping.Add(BleHidButtonEvent.Id.Secondary, BleHidButtonEvent.Action.Tap, () => serviceBridge.Back());
            mapping.Add(BleHidButtonEvent.Id.Secondary, BleHidButtonEvent.Action.DoubleTap, () => serviceBridge.Home());

            mapping.Add(BleHidDirection.Up, () => serviceBridge.DPadUp());
            mapping.Add(BleHidDirection.Right, () => serviceBridge.DPadRight());
            mapping.Add(BleHidDirection.Down, () => serviceBridge.DPadDown());
            mapping.Add(BleHidDirection.Left, () => serviceBridge.DPadLeft());
            mapping.Add(new SingleIncrementalAxisMapping(BleHidAxis.Z, () => serviceBridge.VolumeUp(), () => serviceBridge.VolumeDown()));
            return mapping;
        }

        public static InputDeviceMapping LocalDragNavigation(BleBridge bridge)
        {
            var serviceBridge = bridge.AccessibilityServiceBridge;
            var mapping = new InputDeviceMapping("LocalDragNavigation");

            var swipeMapping = new MousePositionAxisMapping(deltaMove => serviceBridge.SwipeExtend(deltaMove), requirePress: true);
            mapping.Add(BleHidButtonEvent.Id.Primary, BleHidButtonEvent.Action.Press, () => serviceBridge.SwipeBegin(ScreenCenter()));
            mapping.Add(BleHidButtonEvent.Id.Primary, BleHidButtonEvent.Action.Release, () => serviceBridge.SwipeEnd());
            mapping.Add(swipeMapping);

            return mapping;
        }

    }
}
