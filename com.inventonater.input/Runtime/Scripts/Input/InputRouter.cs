using System;
using System.Collections.Generic;
using JetBrains.Annotations;
using UnityEngine;

namespace Inventonater
{
    [DefaultExecutionOrder(ExecutionOrder.InputRouting)]
    public class InputRouter : MonoBehaviour
    {
        public delegate void InputDeviceChangedEvent(IInputSourceDevice prev, IInputSourceDevice next);
        public event InputDeviceChangedEvent WhenInputDeviceChanged = delegate { };
        public event Action<InputBinding> WhenBindingChanged = delegate { };

        private IInputSourceDevice _sourceDevice;
        private InputBinding _binding;
        private InputBinding _shell;

        public bool HasMapping => _binding != null;
        public bool HasDevice => _sourceDevice != null;
        [CanBeNull] public InputBinding Binding => _binding;
        [SerializeField] private List<ButtonEvent> pendingButtonEvents = new();
        [SerializeField] private bool _verbose = true;

        public void SetBinding(InputBinding binding)
        {
            if (binding == null) return;
            if (_binding != null && _binding.Name == binding.Name) return;

            _binding = binding;
            WhenBindingChanged(_binding);
            LoggingManager.Instance.Log($"SetMapping: {binding.Name}");
            _binding.Chirp();
        }

        public void SetShellBinding(InputBinding shell) => _shell = shell;

        public void SetSource(IInputSourceDevice inputSourceDevice)
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

            WhenInputDeviceChanged(prevSourceDevice, _sourceDevice);
        }

        public void HandleInputEvent(ButtonEvent buttonEvent) => pendingButtonEvents.Add(buttonEvent);

        public void HandlePositionDeltaEvent(Vector3 delta) => _binding?.AddPositionDelta(delta);

        // ExecutionOrder Process
        public void Update()
        {
            foreach (var pendingButtonEvent in pendingButtonEvents) _binding?.Invoke(pendingButtonEvent);
            pendingButtonEvents.Clear();

            _binding?.Update(Time.time);
        }
    }
}
