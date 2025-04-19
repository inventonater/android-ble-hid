using UnityEngine;

namespace Inventonater.BleHid
{
    public class MousePositionFilter
    {
        public IInputFilter Filter { get; private set; }
        public InputFilterFactory.FilterType CurrentFilterType { get; private set; }

        private Vector2? _lastFilteredPosition;
        public float GlobalScale = 1.0f;
        public float HorizontalSensitivity = 3.0f;
        public float VerticalSensitivity = 3.0f;

        public MousePositionFilter()
        {
            CurrentFilterType = InputFilterFactory.FilterType.OneEuro;
            Filter = InputFilterFactory.CreateFilter(CurrentFilterType);
            Filter = Filter;
        }

        public void Reset()
        {
            _lastFilteredPosition = null;
            Filter.Reset();
        }

        public void SetInputFilter(InputFilterFactory.FilterType value)
        {
            if (CurrentFilterType == value) return;
            CurrentFilterType = value;
            Filter = InputFilterFactory.CreateFilter(CurrentFilterType);
            Filter.Reset();
            BleHidManager.Instance.InputRouter.Mapping.MousePositionFilter.Filter = Filter;
            LoggingManager.Instance.AddLogEntry($"Changed input filter to: {Filter.Name}");
        }

        /// <summary>
        /// Handle pointer input from any source (touch, mouse, external devices)
        /// </summary>
        /// <param name="position">Screen position of the input</param>
        /// <param name="timestamp"></param>
        /// <param name="flipY">Inverse the Y value</param>
        public (float deltaX, float deltaY) CalculateDelta(Vector3 position, float timestamp, bool flipY = true)
        {
            if (flipY) position.y = -position.y;
            if (!_lastFilteredPosition.HasValue) _lastFilteredPosition = position;

            Vector2 filteredPosition = Filter.Filter(position, timestamp);
            var delta = filteredPosition - _lastFilteredPosition.Value;
            _lastFilteredPosition = filteredPosition;

            delta.x *= HorizontalSensitivity;
            delta.y *= VerticalSensitivity;
            delta *= GlobalScale;
            return (delta.x, delta.y);
        }
    }
}
