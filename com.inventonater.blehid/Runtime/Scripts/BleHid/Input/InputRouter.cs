using System;
using System.Collections.Generic;
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
        private InputDeviceMapping _mapping;
        private List<InputDeviceMapping> _mappings = new();

        public bool HasMapping => _mapping != null;
        public bool HasDevice => _sourceDevice != null;
        [CanBeNull] public InputDeviceMapping Mapping => _mapping;

        [SerializeField] private List<BleHidButtonEvent> _pendingButtonEvents = new();
        [SerializeField] private List<BleHidDirection> _pendingDirection = new();
        [SerializeField] private bool _active;

        public void AddMapping(InputDeviceMapping mapping)
        {
            _mappings.Add(mapping);
            if (_mapping == null) SetMapping(mapping);
        }

        public void SetMapping(InputDeviceMapping mapping)
        {
            if (!_mappings.Contains(mapping)) AddMapping(mapping);
            if (_mapping == mapping) return;

            _mapping = mapping;
            HandleResetPosition();
            WhenMappingChanged(_mapping);
            LoggingManager.Instance.Log($"SetMapping: {mapping.Name}");
        }

        private void SwitchMapping()
        {
            if (_mappings.Count <= 1) return;
            int currentIndex = _mappings.IndexOf(_mapping);
            int nextIndex = (currentIndex + 1) % _mappings.Count;
            SetMapping(_mappings[nextIndex]);
        }


        private void ToggleActive()
        {
            _active = !_active;
            LoggingManager.Instance.Log($"Toggle active: {_active}");
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

        private void HandleButtonEvent(BleHidButtonEvent buttonEvent)
        {
            if (buttonEvent == new BleHidButtonEvent(BleHidButtonEvent.Id.Secondary, BleHidButtonEvent.Action.DoubleTap))
            {
                ToggleActive();
            }
            if (buttonEvent == new BleHidButtonEvent(BleHidButtonEvent.Id.Tertiary, BleHidButtonEvent.Action.DoubleTap))
            {
                SwitchMapping();
            }
            _pendingButtonEvents.Add(buttonEvent);
        }

        private void HandleDirection(BleHidDirection direction) => _pendingDirection.Add(direction);

        private void HandlePositionEvent(Vector3 absolutePosition)
        {
            foreach (var mapping in _mapping.AxisMappings) mapping.SetValue(absolutePosition);
        }

        private void HandleResetPosition()
        {
            foreach (var axisMapping in _mapping.AxisMappings) axisMapping.ResetPosition();
        }

        // ExecutionOrder Process
        private void Update()
        {
            if (!_active) return;

            using (_profileMarkerButtonEvent.Auto())
            {
                foreach (var pendingButtonEvent in _pendingButtonEvents)
                {
                    if (_mapping.ButtonMapping.TryGetValue(pendingButtonEvent, out var buttonActions))
                    {
                        foreach (var action in buttonActions) TryFireAction(action);
                    }
                }
                _pendingButtonEvents.Clear();
            }

            using (_profileMarkerDirection.Auto())
            {
                foreach (var pendingDirection in _pendingDirection)
                {
                    if (_mapping.DirectionMapping.TryGetValue(pendingDirection, out var directionActions))
                    {
                        foreach (var action in directionActions) TryFireAction(action);
                    }
                }
                _pendingDirection.Clear();
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
