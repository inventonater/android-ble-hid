using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Annotations;
using UnityEngine;

namespace Inventonater
{
    [DefaultExecutionOrder(ExecutionOrder.InputRouting)]
    public class InputRouter : MonoBehaviour
    {
        public delegate void InputDeviceChangedEvent(IInputSourceDevice prev, IInputSourceDevice next);

        public event InputDeviceChangedEvent WhenDeviceChanged = delegate { };
        public event Action<InputDeviceMapping> WhenMappingAdded = delegate { };
        public event Action<InputDeviceMapping> WhenMappingChanged = delegate { };

        private IInputSourceDevice _sourceDevice;
        private InputDeviceMapping _mapping;
        public List<InputDeviceMapping> Mappings { get; } = new();

        public bool HasMapping => _mapping != null;
        public bool HasDevice => _sourceDevice != null;
        [CanBeNull] public InputDeviceMapping Mapping => _mapping;

        [SerializeField] private List<InputEvent> pendingButtonEvents = new();

        public void AddMapping(InputDeviceMapping mapping)
        {
            if (Mappings.Any(m => m.Name == mapping.Name)) return;
            Mappings.Add(mapping);
            WhenMappingAdded(mapping);
            if (_mapping == null) SetMapping(mapping);
        }

        public void SetMapping(string mappingName) => SetMapping(Mappings.FirstOrDefault(m => m.Name == mappingName));

        public void SetMapping(InputDeviceMapping mapping)
        {
            if (mapping == null) return;
            if (Mappings.All(m => m.Name != mapping.Name)) AddMapping(mapping);
            if (_mapping != null && _mapping.Name == mapping.Name) return;

            _mapping = mapping;
            WhenMappingChanged(_mapping);
            LoggingManager.Instance.Log($"SetMapping: {mapping.Name}");
            Action chirp = _mapping.GetAction(EInputAction.Chirp);
            chirp?.Invoke();
        }

        public void CycleMapping()
        {
            if (Mappings.Count <= 1) return;
            int currentIndex = Mappings.IndexOf(_mapping);
            int nextIndex = (currentIndex + 1) % Mappings.Count;
            SetMapping(Mappings[nextIndex]);
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
            pendingButtonEvents.Add(buttonEvent);
        }

        private void HandlePositionDeltaEvent(Vector3 delta)
        {
            foreach (var mapping in _mapping.AxisMappings) mapping.AddDelta(delta);
        }

        // ExecutionOrder Process
        private void Update()
        {
            foreach (var pendingButtonEvent in pendingButtonEvents)
            {
                if (_mapping.ButtonMapping.TryGetValue(pendingButtonEvent, out var buttonActions))
                {
                    foreach (var inputAction in buttonActions)
                    {
                        try
                        {
                            var action = _mapping.GetAction(inputAction);
                            action();
                        }
                        catch (Exception e) { LoggingManager.Instance.Exception(e); }
                    }
                }

                foreach (var axisMapping in _mapping.AxisMappings)
                {
                    try { axisMapping.Handle(pendingButtonEvent); }
                    catch (Exception e) { LoggingManager.Instance.Exception(e); }
                }
            }
            pendingButtonEvents.Clear();

            foreach (var axisMapping in _mapping.AxisMappings)
            {
                axisMapping.Update(Time.time);
            }
        }
    }
}
