using System;
using Unity.Profiling;
using UnityEngine;

namespace Inventonater.BleHid
{
    [Serializable]
    public class MousePositionFilter : IAxisMapping
    {
        public IInputFilter Filter { get; private set; }
        public InputFilterFactory.FilterType CurrentFilterType { get; private set; }
        static readonly ProfilerMarker _profileMarker = new("BleHid.MousePositionFilter.Update");

        private Vector2? _lastFilteredPosition;
        public float GlobalScale = 1.0f;
        public float HorizontalSensitivity = 3.0f;
        public float VerticalSensitivity = 3.0f;
        [SerializeField] private Vector3 _pendingPosition;
        [SerializeField] private Vector3 _lastPosition;
        [SerializeField] private MouseBridge _mouse;
        [SerializeField] private bool _flipY;

        public MousePositionFilter(MouseBridge mouse, bool flipY = true)
        {
            _mouse = mouse;
            _flipY = flipY;
            CurrentFilterType = InputFilterFactory.FilterType.OneEuro;
            Filter = InputFilterFactory.CreateFilter(CurrentFilterType);
            Filter = Filter;
        }

        public void ResetPosition()
        {
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
            
            // Update the filter in the input router if available
            SetFilterToMapping();
            LoggingManager.Instance.Log($"Changed input filter to: {Filter.Name}");
        }

        private void SetFilterToMapping()
        {
            var mousePositionfilter = BleHidManager.Instance.InputRouter?.Mapping?.MousePositionFilter;
            if(mousePositionfilter != null) mousePositionfilter.Filter = Filter;
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
            
            // Update the filter in the input router if available
            SetFilterToMapping();
            
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
            if (filter is RawPassthrough) return InputFilterFactory.FilterType.None;
            
            return InputFilterFactory.FilterType.OneEuro; // Default
        }

        public void SetValue(Vector3 absolutePosition) => _pendingPosition = absolutePosition;

        public void Update(float time)
        {
            using var profilerMarker = _profileMarker.Auto();
            if (_flipY) _pendingPosition.y = -_pendingPosition.y;

            Vector2 filteredPosition = Filter.Filter(_pendingPosition, time);
            _lastFilteredPosition ??= filteredPosition;

            var delta = filteredPosition - _lastFilteredPosition.Value;
            _lastFilteredPosition = filteredPosition;

            delta.x *= HorizontalSensitivity;
            delta.y *= VerticalSensitivity;
            delta *= GlobalScale;

            _mouse.MoveMouse(delta.x, delta.y);
        }
    }
}
