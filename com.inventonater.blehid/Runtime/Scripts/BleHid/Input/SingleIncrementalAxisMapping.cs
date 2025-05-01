using System;
using Unity.Profiling;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class SingleIncrementalAxisMapping : IAxisMapping
    {
        private readonly Axis _axis;
        private readonly Action _increment;
        private readonly Action _decrement;
        private bool _initialized;
        private int _lastIncrementValue;
        private int _pendingDelta;
        private readonly float _timeInterval;
        float _lastIncrement;
        static readonly ProfilerMarker _profileMarker = new("BleHid.AxisMappingIncremental.Update");
        private float _scale;
        private readonly InputEvent _start = new InputEvent(InputEvent.Id.Primary, InputEvent.Phase.Press);
        private readonly InputEvent _end = new InputEvent(InputEvent.Id.Primary, InputEvent.Phase.Release);

        public IInputFilter Filter => NoFilter.Instance;

        public SingleIncrementalAxisMapping(Axis axis, Action increment, Action decrement, float scale = 0.1f, float timeInterval = 0.04f)
        {
            _axis = axis;
            _increment = increment;
            _decrement = decrement;
            _scale = scale;
            _timeInterval = timeInterval;
        }

        public void Handle(InputEvent pendingButtonEvent)
        {
            if (pendingButtonEvent == _start) Active = true;
            if (pendingButtonEvent == _end) Active = false;
        }

        private bool _active;
        public bool Active
        {
            get => _active;
            private set
            {
                if (_active == value) return;
                ResetPosition();
                _active = value;
            }
        }

        public void ResetPosition()
        {
            _initialized = false;
            _pendingDelta = 0;
            _accumulatedDelta = 0;
        }

        float _accumulatedDelta;

        public void SetPositionDelta(Vector3 delta)
        {
            _accumulatedDelta += delta[(int)_axis] * _scale;

            int incrementValue = Mathf.RoundToInt(_accumulatedDelta);
            if (!_initialized)
            {
                _initialized = true;
                _lastIncrementValue = incrementValue;
                _pendingDelta = 0;
            }

            _pendingDelta += incrementValue - _lastIncrementValue;
            _lastIncrementValue = incrementValue;
        }

        public void Update(float time)
        {
            using var profile = _profileMarker.Auto();

            if (!_active) return;
            if (time < _lastIncrement + _timeInterval) return;
            _lastIncrement = time;

            if (_pendingDelta > 0)
            {
                _pendingDelta--;
                _increment();
            }

            if (_pendingDelta < 0)
            {
                _pendingDelta++;
                _decrement();
            }
        }
    }
}
