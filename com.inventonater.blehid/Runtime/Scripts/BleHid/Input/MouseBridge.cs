using System;
using UnityEngine;

namespace Inventonater.BleHid.InputControllers
{
    public class MouseBridge
    {
        private BleHidManager manager;
        private readonly MousePositionFilter _filter;
        public MousePositionFilter PositionFilter => _filter;
        public void SetInputFilter(IInputFilter inputFilter) => _filter.SetInputFilter(inputFilter);
        public void Reset() => _filter.Reset();

        public MouseBridge(BleHidManager manager)
        {
            this.manager = manager;
            _filter = new MousePositionFilter();
        }

        public void UpdatePosition(Vector2 position, float timestamp = 0, bool flipY = true)
        {
            var (x, y) = _filter.UpdatePosition(position, timestamp, flipY);
            MoveMouse(x, y);
        }

        /// <summary>
        /// Send a mouse movement.
        /// </summary>
        /// <param name="deltaX">X-axis movement (-127 to 127)</param>
        /// <param name="deltaY">Y-axis movement (-127 to 127)</param>
        /// <returns>True if successful, false otherwise.</returns>
        private bool MoveMouse(int deltaX, int deltaY)
        {
            if (deltaX == 0 && deltaY == 0) return false;

            if (deltaX < -127 || deltaX > 127 || deltaY < -127 || deltaY > 127)
            {
                string message = $"Mouse movement values out of range: {deltaX}, {deltaY}";
                Debug.LogError(message);
                manager.LastErrorMessage = message;
                manager.LastErrorCode = BleHidConstants.ERROR_MOUSE_MOVEMENT_OUT_OF_RANGE;
                manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_MOUSE_MOVEMENT_OUT_OF_RANGE, message);
            }

            if (!manager.IsConnected) return false;

            try { return manager.BleInitializer.Call<bool>("moveMouse", deltaX, deltaY); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        /// <summary>
        /// Press a mouse button without releasing it.
        /// </summary>
        /// <param name="button">Button to press (0=left, 1=right, 2=middle)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool PressMouseButton(int button)
        {
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.Call<bool>("pressMouseButton", button); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        /// <summary>
        /// Release a specific mouse button.
        /// </summary>
        /// <param name="button">Button to release (0=left, 1=right, 2=middle)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool ReleaseMouseButton(int button)
        {
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.Call<bool>("releaseMouseButton", button); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        /// <summary>
        /// Click a mouse button.
        /// </summary>
        /// <param name="button">Button to click (0=left, 1=right, 2=middle)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool ClickMouseButton(int button)
        {
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.Call<bool>("clickMouseButton", button); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        public class MousePositionFilter
        {
            private IInputFilter _inputFilter = new NoFilter();
            private Vector2? _lastFilteredPosition;
            public float GlobalScale = 1.0f;
            public float HorizontalSensitivity = 3.0f;
            public float VerticalSensitivity = 3.0f;

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
            public (int x, int y) UpdatePosition(Vector2 position, float timestamp = 0, bool flipY = true)
            {
                if(flipY) position.y = -position.y;
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
                return (finalX, finalY);
            }
        }
    }
}
