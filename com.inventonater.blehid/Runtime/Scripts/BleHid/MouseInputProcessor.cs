using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Handles processing of pointer input (touch, mouse, external) with motion filtering
    /// </summary>
    public class MouseInputProcessor
    {
        private readonly Action<int, int> _moveMouseDeltaCallback;
        private IInputFilter _inputFilter = new NoFilter();
        private Vector2? _lastFilteredPosition;
        public float GlobalScale = 1.0f;
        public float HorizontalSensitivity = 3.0f;
        public float VerticalSensitivity = 3.0f;

        public MouseInputProcessor(Action<int, int> moveMouseDeltaCallback) => _moveMouseDeltaCallback = moveMouseDeltaCallback;

        public void Reset()
        {
            _lastFilteredPosition = null;
            _inputFilter.Reset();
        }

        public void SetInputFilter(IInputFilter filter) => _inputFilter = filter;

        /// <summary>
        /// Handle pointer input from any source (touch, mouse, external devices)
        /// </summary>
        /// <param name="position">Screen position of the input</param>
        /// <param name="timestamp"></param>
        public void UpdatePosition(Vector2 position, float timestamp = 0)
        {
            if (timestamp == 0) timestamp = Time.time;
            if (!_lastFilteredPosition.HasValue) _lastFilteredPosition = position;

            Vector2 filteredPosition = _inputFilter.Filter(position, timestamp);
            var delta = filteredPosition - _lastFilteredPosition.Value;
            _lastFilteredPosition = filteredPosition;

            delta.x *= HorizontalSensitivity;
            delta.y *= VerticalSensitivity;
            delta *= GlobalScale;

            int finalX = Mathf.RoundToInt(delta.x);
            int finalY = Mathf.RoundToInt(delta.y);
            _moveMouseDeltaCallback(finalX, finalY);
        }
    }
}
