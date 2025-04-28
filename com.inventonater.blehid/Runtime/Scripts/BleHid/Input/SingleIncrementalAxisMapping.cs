using System;
using Unity.Profiling;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class SingleIncrementalAxisMapping : IAxisMapping
    {
        private readonly BleHidAxis _axis;
        private readonly Action _increment;
        private readonly Action _decrement;
        private bool _initialized;
        private int _lastIncrementValue;
        private int _pendingDelta;
        private readonly float _timeInterval;
        float _lastIncrement;
        static readonly ProfilerMarker _profileMarker = new("BleHid.AxisMappingIncremental.Update");
        private bool _active;
        private float _scale;

        public IInputFilter Filter => NoFilter.Instance;

        public SingleIncrementalAxisMapping(BleHidAxis axis, Action increment, Action decrement, float scale = 0.1f, float timeInterval = 0.02f)
        {
            _axis = axis;
            _increment = increment;
            _decrement = decrement;
            _scale = scale;
            _timeInterval = timeInterval;
        }

        public bool Active
        {
            get => _active;
            set
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
        }

        public void SetValue(Vector3 absolutePosition)
        {
            var absoluteValue = absolutePosition[(int)_axis];
            absoluteValue *= _scale;

            int incrementValue = Mathf.RoundToInt(absoluteValue);
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
