using System;
using System.Collections.Generic;
using JetBrains.Annotations;
using UnityEngine;

namespace Inventonater
{
    [DefaultExecutionOrder(ExecutionOrder.InputRouting)]
    public class InputRouter : MonoBehaviour
    {
        public delegate void InputDeviceChangedEvent(IInputSource prev, IInputSource next);
        public event InputDeviceChangedEvent WhenInputDeviceChanged = delegate { };
        public event Action<InputBinding> WhenBindingChanged = delegate { };

        private IInputSource _source;
        private InputBinding _binding;
        private InputBinding _shell;

        public bool HasMapping => _binding != null;
        public bool HasDevice => _source != null;
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

        public void SetSource(IInputSource inputSource)
        {
            IInputSource prevSource = _source;
            if (prevSource != null)
            {
                LoggingManager.Instance.Log($"unregistered: {prevSource.Name}");
                prevSource.EmitPositionDelta -= HandlePositionDeltaEvent;
                prevSource.EmitInputEvent -= HandleInputEvent;
                prevSource.InputDeviceDisabled();
            }

            _source = inputSource;
            if (_source != null)
            {
                LoggingManager.Instance.Log($"registered: {_source.Name}");
                _source.EmitPositionDelta += HandlePositionDeltaEvent;
                _source.EmitInputEvent += HandleInputEvent;
                _source.InputDeviceEnabled();
            }

            WhenInputDeviceChanged(prevSource, _source);
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
