using System;
using Unity.Profiling;
using UnityEngine;

namespace Inventonater
{
    /// <summary>
    /// Abstract base class for axis mappings that throttle updates based on a time interval
    /// </summary>
    public abstract class SingleAxisMappingThrottled : IAxisMapping
    {
        protected const float DefaultScale = 0.1f;
        protected const float DefaultTimeInterval = 0.080f;
        private readonly Axis _axis;
        private readonly float _timeInterval;
        private float _lastUpdate;
        private float _scale;
        private readonly InputEvent _start = new(InputEvent.Id.Primary, InputEvent.Phase.Press);
        private readonly InputEvent _end = new(InputEvent.Id.Primary, InputEvent.Phase.Release);
        private static readonly ProfilerMarker _profileMarker = new("BleHid.ThrottledAxisMapping.Update");
        private int _lastIncrementValue;
        private float _accumulatedDelta;

        public IInputFilter Filter => NoFilter.Instance;

        protected SingleAxisMappingThrottled(Axis axis, float scale = DefaultScale, float timeInterval = DefaultTimeInterval)
        {
            _axis = axis;
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

        private void ResetPosition()
        {
            _accumulatedDelta = 0;
            _lastIncrementValue = 0;
        }

        public void AddDelta(Vector3 delta3)
        {
            var delta = delta3[(int)_axis] * _scale;

            _accumulatedDelta += delta;
            int incrementValue = Mathf.RoundToInt(_accumulatedDelta);
            AddSingleAxisPendingDelta(incrementValue - _lastIncrementValue);
            _lastIncrementValue = incrementValue;
        }

        public void Update(float time)
        {
            using var profile = _profileMarker.Auto();

            if (!_active) return;
            if (time < _lastUpdate + _timeInterval) return;
            _lastUpdate = time;

            ThrottledUpdate();
        }

        protected abstract void AddSingleAxisPendingDelta(int pendingDelta);
        protected abstract void ThrottledUpdate();
        protected abstract void ProcessReset();

    }
}
