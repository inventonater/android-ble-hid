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
        public void Add(IAxisMapping axisMapping) => _axisMappings.Add(axisMapping);
        public void Add(BleHidButtonEvent buttonEvent, Action action) => _buttonMapping.AppendValue(buttonEvent, action);
        public void Add(BleHidDirection dir, Action action) => _directionMapping.AppendValue(dir, action);
        public void AddPressRelease(BleHidButtonEvent.Id button, Action press, Action release)
        {
            Add(button, BleHidButtonEvent.Action.Press, press);
            Add(button, BleHidButtonEvent.Action.Release, release);
        }
        private void AddSingleAxisIncremental(Action increment, Action decrement, BleHidAxis bleHidAxis)
        {
            var axisMapping = new SingleIncrementalAxisMapping(bleHidAxis, increment, decrement);
            Add(BleHidButtonEvent.Id.Primary, BleHidButtonEvent.Action.Press, () => axisMapping.Active = true);
            Add(new BleHidButtonEvent(BleHidButtonEvent.Id.Primary, BleHidButtonEvent.Action.Release), () => axisMapping.Active = false);
            Add(axisMapping);
        }

        public static InputDeviceMapping Ble(BleBridge bridge)
        {
            var mouse = bridge.Mouse;
            var media = bridge.Media;
            var keyboard = bridge.Keyboard;

            var mapping = new InputDeviceMapping("BleMapping");
            mapping.AddPressRelease(BleHidButtonEvent.Id.Primary, () => mouse.PressMouseButton(0), () => mouse.ReleaseMouseButton(0));
            mapping.AddPressRelease(BleHidButtonEvent.Id.Secondary, () => mouse.PressMouseButton(1), () => mouse.ReleaseMouseButton(1));
            mapping.Add(BleHidDirection.Up, () => keyboard.SendKey(BleHidConstants.KEY_UP));
            mapping.Add(BleHidDirection.Right, () => keyboard.SendKey(BleHidConstants.KEY_RIGHT));
            mapping.Add(BleHidDirection.Down, () => keyboard.SendKey(BleHidConstants.KEY_DOWN));
            mapping.Add(BleHidDirection.Left, () => keyboard.SendKey(BleHidConstants.KEY_LEFT));
            mapping.Add(new MousePositionAxisMapping(mouse));

            mapping.AddSingleAxisIncremental(() => media.VolumeUp(), () => media.VolumeDown(), BleHidAxis.Z);
            return mapping;
        }

        public static InputDeviceMapping Phone(BleBridge bridge)
        {
            var serviceBridge = bridge.AccessibilityServiceBridge;

            var mapping = new InputDeviceMapping("PhoneMapping");

            mapping.Add(BleHidButtonEvent.Id.Secondary, BleHidButtonEvent.Action.Tap, () => serviceBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Back));
            mapping.Add(BleHidDirection.Up, () => serviceBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Up));
            mapping.Add(BleHidDirection.Right, () => serviceBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Right));
            mapping.Add(BleHidDirection.Down, () => serviceBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Down));
            mapping.Add(BleHidDirection.Left, () => serviceBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Left));

            Vector2 position = default;
            void DeltaMoveAction(Vector2 deltaMove)
            {
                serviceBridge.Swipe(position, position + deltaMove);
                position += deltaMove;
            }
            var mousePositionAxisMapping = new MousePositionAxisMapping(DeltaMoveAction);
            mapping.Add(BleHidButtonEvent.Id.Primary, BleHidButtonEvent.Action.Press, () =>
            {
                position = default;
                mousePositionAxisMapping.ResetPosition();
            });
            mapping.Add(mousePositionAxisMapping);

            mapping.AddSingleAxisIncremental(() => serviceBridge.VolumeUp(), () => serviceBridge.VolumeDown(), BleHidAxis.Z);
            return mapping;
        }

    }
}
