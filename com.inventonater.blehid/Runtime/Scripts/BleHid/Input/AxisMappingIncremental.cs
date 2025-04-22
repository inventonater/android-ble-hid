using System;
using Unity.Profiling;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class AxisMappingIncremental : IAxisMapping
    {
        private readonly BleHidAxis _axis;
        private readonly Action _increment;
        private readonly Action _decrement;
        private bool _initialized;
        private int _lastIntValue;
        private int _pendingDelta;
        private readonly float _interval;
        float _lastIncrement;
        static readonly ProfilerMarker _profileMarker = new("BleHid.AxisMappingIncremental.Update");

        public AxisMappingIncremental(BleHidAxis axis, Action increment, Action decrement, float interval = 0.02f)
        {
            _axis = axis;
            _increment = increment;
            _decrement = decrement;
            _interval = interval;
        }

        public void ResetPosition()
        {
            _initialized = false;
            _pendingDelta = 0;
        }

        public void SetValue(Vector3 absolutePosition)
        {
            var absoluteValue = absolutePosition[(int)_axis];
            int intValue = Mathf.RoundToInt(absoluteValue);
            if (!_initialized)
            {
                _lastIntValue = intValue;
                _pendingDelta = 0;
                _initialized = true;
            }

            _pendingDelta += intValue - _lastIntValue;
            _lastIntValue = intValue;
        }

        public void Update(float time)
        {
            using var profile = _profileMarker.Auto();

            if (time < _lastIncrement + _interval) return;
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
