using System;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class InputDeviceMapping
    {
        public enum BleHidAxisInputDestination
        {
            None,
            MouseX,
            MouseY,
            Volume,
            Zoom,
            Scroll
        }

        public MousePositionFilter MousePositionFilter { get; }
        public readonly Dictionary<BleHidButtonEvent, Action> ButtonMapping = new();
        public readonly Dictionary<BleHidDirection, Action> DirectionMapping = new();
        public readonly Dictionary<BleHidAxis, IContinuousValue> AxisMapping = new();

        public KeyboardBridge Keyboard { get; }
        public MouseBridge Mouse { get; }
        public MediaBridge Media { get; }

        public InputDeviceMapping(BleHidManager manager)
        {
            Keyboard = new KeyboardBridge(manager);
            Mouse = new MouseBridge(manager);
            Media = new MediaBridge(manager);

            MousePositionFilter = new MousePositionFilter();
            AddPressRelease(BleHidButtonEvent.Id.Primary, 0);
            AddPressRelease(BleHidButtonEvent.Id.Secondary, 1);
            AddDirection(BleHidDirection.Up, BleHidConstants.KEY_UP);
            AddDirection(BleHidDirection.Right, BleHidConstants.KEY_RIGHT);
            AddDirection(BleHidDirection.Down, BleHidConstants.KEY_DOWN);
            AddDirection(BleHidDirection.Left, BleHidConstants.KEY_LEFT);

            AddAxis(BleHidAxis.Z, new ContinuousValue(() => Media.VolumeUp(), () => Media.VolumeDown()));
        }

        private void AddAxis(BleHidAxis axis, IContinuousValue continuousValue) => AxisMapping.Add(axis, continuousValue);
        private void AddDirection(BleHidDirection dir, byte hidConstant) => DirectionMapping.Add(dir, () => Keyboard.SendKey(hidConstant));

        public void AddPressRelease(BleHidButtonEvent.Id button, int mouseButtonId)
        {
            ButtonMapping.Add(new BleHidButtonEvent(button, BleHidButtonEvent.Action.Press), () => Mouse.PressMouseButton(mouseButtonId));
            ButtonMapping.Add(new BleHidButtonEvent(button, BleHidButtonEvent.Action.Release), () => Mouse.ReleaseMouseButton(mouseButtonId));
        }

        public void AddTap(BleHidButtonEvent.Id button, byte hidConstant) =>
            ButtonMapping.Add(new BleHidButtonEvent(button, BleHidButtonEvent.Action.Tap), () => Keyboard.SendKey(hidConstant));

        public void AddDoubleTap(BleHidButtonEvent.Id button, byte hidConstant) =>
            ButtonMapping.Add(new BleHidButtonEvent(button, BleHidButtonEvent.Action.DoubleTap), () => Keyboard.SendKey(hidConstant));

        private class ContinuousValue : IContinuousValue
        {
            private readonly Action _increment;
            private readonly Action _decrement;
            private bool _initialized;
            private int _lastIntValue;
            private int _pendingDelta;

            public ContinuousValue(Action increment, Action decrement)
            {
                _increment = increment;
                _decrement = decrement;
            }

            public void Update(float absoluteValue)
            {
                int intValue = Mathf.RoundToInt(absoluteValue);
                if (!_initialized)
                {
                    _lastIntValue = intValue;
                    _pendingDelta = 0;
                    _initialized = true;
                }

                _pendingDelta += intValue - _lastIntValue;
                _lastIntValue = intValue;

                if (_pendingDelta > 0)
                {
                    _pendingDelta--;
                    _increment();
                }

                if (_pendingDelta < 0)
                {
                    _pendingDelta++;
                    _decrement();
                }
            }
        }
    }
}
