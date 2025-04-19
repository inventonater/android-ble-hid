using UnityEngine;

namespace Inventonater.BleHid
{
    public class InputRouter
    {
        public delegate void InputDeviceChangedEvent(IInputSourceDevice prev, IInputSourceDevice next);
        public event InputDeviceChangedEvent WhenDeviceChanged = delegate { };

        private IInputSourceDevice _sourceDevice;
        private InputDeviceMapping _mapping;
        public InputDeviceMapping Mapping => _mapping;
        public bool HasMapping => _mapping != null;

        public bool HasDevice => _sourceDevice != null;
        private bool IsActive => HasMapping && HasDevice;

        public void SetMapping(InputDeviceMapping mapping)
        {
            _mapping = mapping;
            _mapping.MousePositionFilter.Reset();
        }

        public void SetSourceDevice(IInputSourceDevice inputSourceDevice)
        {
            IInputSourceDevice prevSourceDevice = _sourceDevice;
            if (prevSourceDevice != null)
            {
                LoggingManager.Instance.AddLogEntry($"unregistered: {prevSourceDevice.Name}");
                prevSourceDevice.WhenPositionEvent -= HandlePositionEvent;
                prevSourceDevice.WhenButtonEvent -= HandleButtonEvent;
                prevSourceDevice.WhenDirectionEvent -= HandleDirectionEvent;
                prevSourceDevice.InputDeviceDisabled();
            }
            _sourceDevice = inputSourceDevice;
            if (_sourceDevice != null)
            {
                LoggingManager.Instance.AddLogEntry($"registered: {_sourceDevice.Name}");
                _sourceDevice.WhenPositionEvent += HandlePositionEvent;
                _sourceDevice.WhenButtonEvent += HandleButtonEvent;
                _sourceDevice.WhenDirectionEvent += HandleDirectionEvent;
                _sourceDevice.InputDeviceEnabled();
            }

            WhenDeviceChanged(prevSourceDevice, _sourceDevice);
        }

        private void HandlePositionEvent(Vector3 position)
        {
            if (!IsActive) return;

            var (deltaX, deltaY) = _mapping.MousePositionFilter.CalculateDelta(position, Time.time);
            _mapping.Mouse.MoveMouse(deltaX, deltaY);
            if (_mapping.AxisMapping.TryGetValue(BleHidAxis.X, out var xAction)) xAction.Update(position.x);
            if (_mapping.AxisMapping.TryGetValue(BleHidAxis.Y, out var yAction)) yAction.Update(position.y);
            if (_mapping.AxisMapping.TryGetValue(BleHidAxis.Y, out var zAction)) zAction.Update(position.z);
        }

        private void HandleButtonEvent(BleHidButtonEvent buttonEvent)
        {
            if (!IsActive) return;
            if (_mapping.ButtonMapping.TryGetValue(buttonEvent, out var action)) action();
        }

        private void HandleDirectionEvent(BleHidDirection bleHidDirection)
        {
            if (!IsActive) return;
            if (_mapping.DirectionMapping.TryGetValue(bleHidDirection, out var action)) action();
        }
    }
}
