using System;
using JetBrains.Annotations;
using UnityEngine;

namespace Inventonater.BleHid
{
    [DefaultExecutionOrder(ExecutionOrder.Preprocess)]
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

        public void SetMapping(InputDeviceMapping mapping)
        {
            _mapping = mapping;
            _mapping.ResetPosition();
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

        private void HandleButtonEvent(BleHidButtonEvent buttonEvent) => _mapping.SetButtonEvent(buttonEvent);
        private void HandleDirection(BleHidDirection direction) => _mapping.SetDirection(direction);
        private void HandlePositionEvent(Vector3 position) => _mapping.SetPosition(position);
        private void HandleResetPosition() => _mapping.ResetPosition();
    }
}
