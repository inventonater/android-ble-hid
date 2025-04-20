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
        private readonly List<IAxisMapping> _axisMappings = new();

        public KeyboardBridge Keyboard { get; }
        public MouseBridge Mouse { get; }
        public MediaBridge Media { get; }

        private BleHidButtonEvent _pendingButtonEvent;
        private BleHidDirection _pendingDirection;

        public InputDeviceMapping(BleHidManager manager)
        {
            Keyboard = new KeyboardBridge(manager);
            Mouse = new MouseBridge(manager);
            Media = new MediaBridge(manager);

            AddPressRelease(BleHidButtonEvent.Id.Primary, 0);
            AddPressRelease(BleHidButtonEvent.Id.Secondary, 1);
            AddDirection(BleHidDirection.Up, BleHidConstants.KEY_UP);
            AddDirection(BleHidDirection.Right, BleHidConstants.KEY_RIGHT);
            AddDirection(BleHidDirection.Down, BleHidConstants.KEY_DOWN);
            AddDirection(BleHidDirection.Left, BleHidConstants.KEY_LEFT);

            _axisMappings.Add(MousePositionFilter = new MousePositionFilter(Mouse));
            _axisMappings.Add(new AxisMappingIncremental(BleHidAxis.Z, () => Media.VolumeUp(), () => Media.VolumeDown()));
        }

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

        public void SetDirection(BleHidDirection direction) => _pendingDirection = direction;
        public void SetButtonEvent(BleHidButtonEvent buttonEvent) => _pendingButtonEvent = buttonEvent;
        public void SetPosition(Vector3 absolutePosition)
        {
            foreach (var axisMapping in _axisMappings) axisMapping.SetValue(absolutePosition);
        }
        public void ResetPosition()
        {
            foreach (var axisMapping in _axisMappings) axisMapping.ResetPosition();
        }

        public void Update(float time)
        {
            if (_pendingButtonEvent != BleHidButtonEvent.None && ButtonMapping.TryGetValue(_pendingButtonEvent, out var buttonAction))
            {
                buttonAction();
                _pendingButtonEvent = BleHidButtonEvent.None;
            }

            if (_pendingDirection != BleHidDirection.None && DirectionMapping.TryGetValue(_pendingDirection, out var directionAction))
            {
                directionAction();
                _pendingDirection = BleHidDirection.None;
            }

            foreach (var axisMapping in _axisMappings) axisMapping.Update(time);
        }
    }
}
