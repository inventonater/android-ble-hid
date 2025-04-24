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

        private BleBridge BleBridge => BleHidManager.Instance.BleBridge;
        private MouseBridge Mouse => BleBridge.Mouse;
        private KeyboardBridge Keyboard => BleBridge.Keyboard;
        private MediaBridge Media => BleBridge.Media;

        public InputDeviceMapping()
        {

            LoggingManager.Instance.Log("Loading hardcoded configuration");
            AddPressRelease(BleHidButtonEvent.Id.Primary, () => Mouse.PressMouseButton(0), () => Mouse.ReleaseMouseButton(0));
            AddPressRelease(BleHidButtonEvent.Id.Secondary, () => Mouse.PressMouseButton(1), () => Mouse.ReleaseMouseButton(1));
            Add(BleHidDirection.Up, BleHidConstants.KEY_UP);
            Add(BleHidDirection.Right, BleHidConstants.KEY_RIGHT);
            Add(BleHidDirection.Down, BleHidConstants.KEY_DOWN);
            Add(BleHidDirection.Left, BleHidConstants.KEY_LEFT);
            Add(new MousePositionAxisMapping(Mouse));

            var volumeMapping = new SingleIncrementalAxisMapping(BleHidAxis.Z, () => Media.VolumeUp(), () => Media.VolumeDown());

            Add(BleHidButtonEvent.Id.Primary, BleHidButtonEvent.Action.Press, () => volumeMapping.Active = true);
            Add(new BleHidButtonEvent(BleHidButtonEvent.Id.Primary, BleHidButtonEvent.Action.Release), () => volumeMapping.Active = false);
            Add(volumeMapping);
        }

        private void Add(BleHidButtonEvent.Id id, BleHidButtonEvent.Action buttonAction, Action action) => Add(new BleHidButtonEvent(id, buttonAction), action);
        public void Add(IAxisMapping axisMapping) => _axisMappings.Add(axisMapping);
        public void Add(BleHidButtonEvent buttonEvent, Action action) => _buttonMapping.AppendValue(buttonEvent, action);

        public void Add(BleHidDirection dir, byte hidConstant) => Add(dir, () => Keyboard.SendKey(hidConstant));
        public void Add(BleHidDirection dir, Action action) => _directionMapping.AppendValue(dir, action);

        public void AddPressRelease(BleHidButtonEvent.Id button, Action press, Action release)
        {
            Add(button, BleHidButtonEvent.Action.Press, press);
            Add(button, BleHidButtonEvent.Action.Release, release);
        }

        public void AddKeyTap(BleHidButtonEvent.Id button, byte hidConstant) => _buttonMapping.AppendValue(new BleHidButtonEvent(button, BleHidButtonEvent.Action.Tap), () => Keyboard.SendKey(hidConstant));
        public void AddKeyDoubleTap(BleHidButtonEvent.Id button, byte hidConstant) => _buttonMapping.AppendValue(new BleHidButtonEvent(button, BleHidButtonEvent.Action.DoubleTap), () => Keyboard.SendKey(hidConstant));


    }
}
