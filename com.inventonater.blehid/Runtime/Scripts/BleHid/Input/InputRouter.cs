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
            WhenMappingChanged(_mapping);
            LoggingManager.Instance.Log($"SetMapping: {mapping.Name}");
        }

        public void CycleMapping()
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
                prevSourceDevice.EmitPositionDelta -= HandlePositionDeltaEvent;
                prevSourceDevice.EmitButtonEvent -= HandleButtonEvent;
                prevSourceDevice.EmitDirection -= HandleDirection;

                prevSourceDevice.InputDeviceDisabled();
            }

            _sourceDevice = inputSourceDevice;
            if (_sourceDevice != null)
            {
                LoggingManager.Instance.Log($"registered: {_sourceDevice.Name}");
                _sourceDevice.EmitPositionDelta += HandlePositionDeltaEvent;
                _sourceDevice.EmitButtonEvent += HandleButtonEvent;
                _sourceDevice.EmitDirection += HandleDirection;
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
                CycleMapping();
            }
            _pendingButtonEvents.Add(buttonEvent);
        }

        private void HandleDirection(BleHidDirection direction) => _pendingDirection.Add(direction);

        private void HandlePositionDeltaEvent(Vector3 delta)
        {
            foreach (var mapping in _mapping.AxisMappings) mapping.SetPositionDelta(delta);
        }

        // ExecutionOrder Process
        private void Update()
        {
            if (!_active)
            {
                _pendingButtonEvents.Clear();
                _pendingDirection.Clear();
                return;
            }

            foreach (var pendingButtonEvent in _pendingButtonEvents) FireButtonEvent(pendingButtonEvent);
            _pendingButtonEvents.Clear();

            foreach (var pendingDirection in _pendingDirection) FireDirection(pendingDirection);
            _pendingDirection.Clear();

            foreach (var axisMapping in _mapping.AxisMappings) axisMapping.Update(Time.time);
        }

        private void FireDirection(BleHidDirection pendingDirection)
        {
            if (_mapping.DirectionMapping.TryGetValue(pendingDirection, out var directionActions))
            {
                foreach (var action in directionActions) TryFireAction(action);
            }

            foreach (var axisMapping in _mapping.AxisMappings) TryFireAction(() => axisMapping.Handle(pendingDirection));
        }

        private void FireButtonEvent(BleHidButtonEvent pendingButtonEvent)
        {
            if (_mapping.ButtonMapping.TryGetValue(pendingButtonEvent, out var buttonActions))
            {
                foreach (var action in buttonActions) TryFireAction(action);
            }

            foreach (var axisMapping in _mapping.AxisMappings) TryFireAction(() => axisMapping.Handle(pendingButtonEvent));
        }

        private static void TryFireAction(Action action)
        {
            try { action(); }
            catch (Exception e) { LoggingManager.Instance.Exception(e); }
        }

    }
}
