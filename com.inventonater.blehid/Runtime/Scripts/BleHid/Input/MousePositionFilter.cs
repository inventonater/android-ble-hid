using UnityEngine;

namespace Inventonater.BleHid
{
    public class MousePositionFilter : IAxisMapping
    {
        public IInputFilter Filter { get; private set; }
        public InputFilterFactory.FilterType CurrentFilterType { get; private set; }

        private Vector2? _lastFilteredPosition;
        public float GlobalScale = 1.0f;
        public float HorizontalSensitivity = 3.0f;
        public float VerticalSensitivity = 3.0f;
        private Vector3 _pendingPosition;
        private Vector3 _lastPosition;
        private readonly MouseBridge _mouse;
        private readonly bool _flipY;

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

        public void SetInputFilter(InputFilterFactory.FilterType value)
        {
            if (CurrentFilterType == value) return;
            CurrentFilterType = value;
            Filter = InputFilterFactory.CreateFilter(CurrentFilterType);
            Filter.Reset();
            BleHidManager.Instance.InputRouter.Mapping.MousePositionFilter.Filter = Filter;
            LoggingManager.Instance.AddLogEntry($"Changed input filter to: {Filter.Name}");
        }

        public void SetValue(Vector3 absolutePosition) => _pendingPosition = absolutePosition;

        public void Update(float time)
        {
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
