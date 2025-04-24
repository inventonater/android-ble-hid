using System;
using JetBrains.Annotations;
using Unity.Profiling;
using UnityEngine;

namespace Inventonater.BleHid
{
    [DefaultExecutionOrder(ExecutionOrder.InputRouting)]
    public class InputRouter : MonoBehaviour
    {

        public delegate void InputDeviceChangedEvent(IInputSourceDevice prev, IInputSourceDevice next);
        public event InputDeviceChangedEvent WhenDeviceChanged = delegate { };
        public event Action<InputDeviceMapping> WhenMappingChanged = delegate { };

        private IInputSourceDevice _sourceDevice;
        [SerializeField] private InputDeviceMapping _mapping;

        public bool HasMapping => _mapping != null;
        public bool HasDevice => _sourceDevice != null;
        [CanBeNull] public InputDeviceMapping Mapping => _mapping;

        [SerializeField] private BleHidButtonEvent _pendingButtonEvent;
        [SerializeField] private BleHidDirection _pendingDirection;

        public void SetMapping(InputDeviceMapping mapping)
        {
            _mapping = mapping;
            HandleResetPosition();
            WhenMappingChanged(_mapping);
        }

        public void SetSourceDevice(IInputSourceDevice inputSourceDevice)
        {
            IInputSourceDevice prevSourceDevice = _sourceDevice;
            if (prevSourceDevice != null)
            {
                LoggingManager.Instance.Log($"unregistered: {prevSourceDevice.Name}");
                prevSourceDevice.NotifyPosition -= HandlePositionEvent;
                prevSourceDevice.NotifyButtonEvent -= HandleButtonEvent;
                prevSourceDevice.NotifyDirection -= HandleDirection;
                prevSourceDevice.NotifyResetPosition -= HandleResetPosition;

                prevSourceDevice.InputDeviceDisabled();
            }
            _sourceDevice = inputSourceDevice;
            if (_sourceDevice != null)
            {
                LoggingManager.Instance.Log($"registered: {_sourceDevice.Name}");
                _sourceDevice.NotifyPosition += HandlePositionEvent;
                _sourceDevice.NotifyButtonEvent += HandleButtonEvent;
                _sourceDevice.NotifyDirection += HandleDirection;
                _sourceDevice.NotifyResetPosition += HandleResetPosition;

                _sourceDevice.InputDeviceEnabled();
            }

            WhenDeviceChanged(prevSourceDevice, _sourceDevice);
        }

        private void HandleButtonEvent(BleHidButtonEvent buttonEvent) => _pendingButtonEvent = buttonEvent;
        private void HandleDirection(BleHidDirection direction) => _pendingDirection = direction;
        private void HandlePositionEvent(Vector3 absolutePosition)
        {
            foreach(var mapping in _mapping.AxisMappings) mapping.SetValue(absolutePosition);
        }

        private void HandleResetPosition()
        {
            foreach (var axisMapping in _mapping.AxisMappings) axisMapping.ResetPosition();
        }

        // ExecutionOrder Process
        private void Update()
        {
            using (_profileMarkerButtonEvent.Auto())
            {
                if (_pendingButtonEvent != BleHidButtonEvent.None && _mapping.ButtonMapping.TryGetValue(_pendingButtonEvent, out var buttonActions))
                {
                    foreach (var action in buttonActions) TryFireAction(action);
                    _pendingButtonEvent = BleHidButtonEvent.None;
                }
            }

            using (_profileMarkerDirection.Auto())
            {
                if (_pendingDirection != BleHidDirection.None && _mapping.DirectionMapping.TryGetValue(_pendingDirection, out var directionActions))
                {
                    foreach (var action in directionActions) TryFireAction(action);
                    _pendingDirection = BleHidDirection.None;
                }
            }

            using (_profileMarkerAxis.Auto())
            {
                foreach (var axisMapping in _mapping.AxisMappings) axisMapping.Update(Time.time);
            }
        }

        private static void TryFireAction(Action action)
        {
            try { action(); }
            catch (Exception e) { LoggingManager.Instance.AddLogException(e); }
        }

        static readonly ProfilerMarker _profileMarkerButtonEvent = new("BleHid.InputDeviceMapping.Update.ButtonEvent");
        static readonly ProfilerMarker _profileMarkerDirection = new("BleHid.InputDeviceMapping.Update.DirectionMapping");
        static readonly ProfilerMarker _profileMarkerAxis = new("BleHid.InputDeviceMapping.Update.AxisMapping");
    }
}
