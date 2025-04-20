using System;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class InputDeviceMapping : MonoBehaviour
    {
        public MousePositionFilter MousePositionFilter => _mousePositionFilter;

        public readonly Dictionary<BleHidButtonEvent, Action> ButtonMapping = new();
        public readonly Dictionary<BleHidDirection, Action> DirectionMapping = new();
        private readonly List<IAxisMapping> _axisMappings = new();

        public KeyboardBridge Keyboard => _keyboard;
        public MouseBridge Mouse => _mouse;
        public MediaBridge Media => _media;

        [SerializeField] private BleHidButtonEvent _pendingButtonEvent;
        [SerializeField] private BleHidDirection _pendingDirection;
        [SerializeField] private KeyboardBridge _keyboard;
        [SerializeField] private MouseBridge _mouse;
        [SerializeField] private MediaBridge _media;
        [SerializeField] private MousePositionFilter _mousePositionFilter;

        private void Awake()
        {
            var manager = BleHidManager.Instance;
            _keyboard = new KeyboardBridge(manager);
            _mouse = new MouseBridge(manager);
            _media = new MediaBridge(manager);

            AddPressRelease(BleHidButtonEvent.Id.Primary, 0);
            AddPressRelease(BleHidButtonEvent.Id.Secondary, 1);
            AddDirection(BleHidDirection.Up, BleHidConstants.KEY_UP);
            AddDirection(BleHidDirection.Right, BleHidConstants.KEY_RIGHT);
            AddDirection(BleHidDirection.Down, BleHidConstants.KEY_DOWN);
            AddDirection(BleHidDirection.Left, BleHidConstants.KEY_LEFT);

            _axisMappings.Add(_mousePositionFilter = new MousePositionFilter(Mouse));
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

        private void Update()
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

            foreach (var axisMapping in _axisMappings) axisMapping.Update(Time.time);
        }
    }
}
