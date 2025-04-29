using System;
using System.Collections.Generic;
using JetBrains.Annotations;
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

        [SerializeField] private List<InputEvent> _pendingButtonEvents = new();

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

        public void SetSourceDevice(IInputSourceDevice inputSourceDevice)
        {
            IInputSourceDevice prevSourceDevice = _sourceDevice;
            if (prevSourceDevice != null)
            {
                LoggingManager.Instance.Log($"unregistered: {prevSourceDevice.Name}");
                prevSourceDevice.EmitPositionDelta -= HandlePositionDeltaEvent;
                prevSourceDevice.EmitInputEvent -= HandleInputEvent;

                prevSourceDevice.InputDeviceDisabled();
            }

            _sourceDevice = inputSourceDevice;
            if (_sourceDevice != null)
            {
                LoggingManager.Instance.Log($"registered: {_sourceDevice.Name}");
                _sourceDevice.EmitPositionDelta += HandlePositionDeltaEvent;
                _sourceDevice.EmitInputEvent += HandleInputEvent;
                _sourceDevice.InputDeviceEnabled();
            }

            WhenDeviceChanged(prevSourceDevice, _sourceDevice);
        }

        private void HandleInputEvent(InputEvent buttonEvent)
        {
            if (buttonEvent == new InputEvent(InputEvent.Id.Tertiary, InputEvent.Phase.DoubleTap)) CycleMapping();
            _pendingButtonEvents.Add(buttonEvent);
        }

        private void HandlePositionDeltaEvent(Vector3 delta)
        {
            foreach (var mapping in _mapping.AxisMappings) mapping.SetPositionDelta(delta);
        }

        // ExecutionOrder Process
        private void Update()
        {
            foreach (var pendingButtonEvent in _pendingButtonEvents)
            {
                if (_mapping.ButtonMapping.TryGetValue(pendingButtonEvent, out var buttonActions))
                {
                    foreach (var action in buttonActions)
                    {
                        try { action(); }
                        catch (Exception e) { LoggingManager.Instance.Exception(e); }
                    }
                }

                foreach (var axisMapping in _mapping.AxisMappings)
                {
                    try { axisMapping.Handle(pendingButtonEvent); }
                    catch (Exception e) { LoggingManager.Instance.Exception(e); }
                }
            }
            _pendingButtonEvents.Clear();

            foreach (var axisMapping in _mapping.AxisMappings)
            {
                axisMapping.Update(Time.time);
            }
        }
    }
}
