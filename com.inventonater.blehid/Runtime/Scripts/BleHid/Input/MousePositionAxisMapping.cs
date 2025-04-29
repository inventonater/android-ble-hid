using System;
using Unity.Profiling;
using UnityEngine;

namespace Inventonater.BleHid
{
    [Serializable]
    public class MousePositionAxisMapping : IAxisMapping
    {
        public IInputFilter Filter { get; private set; }
        public InputFilterFactory.FilterType CurrentFilterType { get; private set; }
        static readonly ProfilerMarker _profileMarker = new("BleHid.MousePositionFilter.Update");

        private Vector3 _accumulatedDeltas;
        private Vector2? _lastFilteredPosition;
        public float GlobalScale = 1.0f;
        public float HorizontalSensitivity = 3.0f;
        public float VerticalSensitivity = 3.0f;
        float _lastIncrement;

        [SerializeField] private bool _flipY;
        private readonly Action<Vector2> _deltaMoveAction;

        public MousePositionAxisMapping(Action<Vector2> deltaMoveAction, bool requirePress = false, bool flipY = true, float timeInterval = 0.04f)
        {
            _timeInterval = timeInterval;
            _requirePress = requirePress;
            _deltaMoveAction = deltaMoveAction;

            _flipY = flipY;
            CurrentFilterType = InputFilterFactory.FilterType.OneEuro;
            Filter = InputFilterFactory.CreateFilter(CurrentFilterType);
            Filter = Filter;

            Active = !_requirePress;
        }

        public void Handle(InputEvent pendingButtonEvent)
        {
            ResetPosition();

            if (!_requirePress) return;
            if (pendingButtonEvent.id != InputEvent.Id.Primary) return;
            if(pendingButtonEvent.phase == InputEvent.Phase.Press) Active = true;
            if(pendingButtonEvent.phase == InputEvent.Phase.Release) Active = false;
        }

        private bool _active;
        private bool _requirePress;
        private float _timeInterval;

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
            _accumulatedDeltas = Vector2.zero;
            _lastFilteredPosition = null;
            Filter.Reset();
        }

        /// <summary>
        /// Set the input filter by type
        /// </summary>
        public void SetInputFilter(InputFilterFactory.FilterType value)
        {
            if (CurrentFilterType == value) return;
            CurrentFilterType = value;
            Filter = InputFilterFactory.CreateFilter(CurrentFilterType);
            Filter.Reset();
            
            LoggingManager.Instance.Log($"Changed input filter to: {Filter.Name}");
        }

        /// <summary>
        /// Set the input filter directly with a pre-configured filter instance
        /// </summary>
        public void SetInputFilter(IInputFilter filter)
        {
            if (filter == null) return;
            
            // Try to determine the filter type
            CurrentFilterType = DetermineFilterType(filter);
            
            // Set the filter
            Filter = filter;
            Filter.Reset();
            
            LoggingManager.Instance.Log($"Applied custom filter: {Filter.Name}");
        }
        
        /// <summary>
        /// Determine the filter type from a filter instance
        /// </summary>
        private InputFilterFactory.FilterType DetermineFilterType(IInputFilter filter)
        {
            if (filter is OneEuroFilter) return InputFilterFactory.FilterType.OneEuro;
            if (filter is KalmanFilter) return InputFilterFactory.FilterType.Kalman;
            if (filter is ExponentialMovingAverageFilter) return InputFilterFactory.FilterType.ExponentialMA;
            if (filter is DoubleExponentialFilter) return InputFilterFactory.FilterType.DoubleExponential;
            if (filter is PredictiveFilter) return InputFilterFactory.FilterType.Predictive;
            if (filter is MuteFilter) return InputFilterFactory.FilterType.Mute;
            if (filter is NoFilter) return InputFilterFactory.FilterType.None;
            
            return InputFilterFactory.FilterType.OneEuro; // Default
        }

        public void SetPositionDelta(Vector3 delta)
        {
            if (_flipY) delta.y = -delta.y;
            delta.x *= HorizontalSensitivity;
            delta.y *= VerticalSensitivity;
            delta *= GlobalScale;
            _accumulatedDeltas += delta;
        }

        public void Update(float time)
        {
            using var profilerMarker = _profileMarker.Auto();

            if (!_active) return;
            if (time < _lastIncrement + _timeInterval) return;
            _lastIncrement = time;

            Vector2 filteredPositionAbsolute = Filter.Filter(_accumulatedDeltas, time);

            _lastFilteredPosition ??= filteredPositionAbsolute;
            var filteredPositionDelta = filteredPositionAbsolute - _lastFilteredPosition.Value;
            _deltaMoveAction(filteredPositionDelta);

            _lastFilteredPosition = filteredPositionAbsolute;
        }
    }
}
