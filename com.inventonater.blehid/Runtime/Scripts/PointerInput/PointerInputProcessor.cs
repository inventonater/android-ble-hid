using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Handles processing of pointer input (touch, mouse, external) with motion filtering
    /// </summary>
    public class PointerInputProcessor
    {
        private static LoggingManager Logger => LoggingManager.Instance;
        private static BleHidManager BleHidManager => BleHidManager.Instance;

        private bool isEditorMode;
        private Vector2 lastPosition;
        private bool isDragging = false;
        private Rect touchpadScreenRect;

        private IInputFilter _inputFilter;
        public float GlobalScale = 1.0f;
        public float HorizontalSensitivity = 3.0f;
        public float VerticalSensitivity = 3.0f;

        public void SetTouchpadRect(Rect screenRect) => touchpadScreenRect = screenRect;
        public void SetInputFilter(IInputFilter filter) => _inputFilter = filter;
        public bool IsDragging() => isDragging;

        /// <summary>
        /// Handle pointer input from any source (touch, mouse, external devices)
        /// </summary>
        /// <param name="position">Screen position of the input</param>
        /// <param name="state">Current state of the input (begin, move, end)</param>
        /// <param name="inputSource">Source name for logging</param>
        /// <returns>True if a move message was sent</returns>
        public bool HandlePointerInput(Vector2 position, PointerInputState state, string inputSource = "External")
        {
            // Skip if components are missing
            if (_inputFilter == null)
                return false;

            // Determine if we should process input (connected or in editor mode)
            bool shouldProcess = BleHidManager.IsConnected || isEditorMode;
            if (!shouldProcess)
                return false;

            // Log touchpad boundaries for debugging when input begins
            if (state == PointerInputState.Begin)
            {
                Logger?.AddLogEntry($"Touchpad screen rect: ({touchpadScreenRect.x}, {touchpadScreenRect.y}, w:{touchpadScreenRect.width}, h:{touchpadScreenRect.height})");
                Logger?.AddLogEntry($"{inputSource} input began at: ({position.x}, {position.y})");
            }

            bool didSendMove = false;

            if (state == PointerInputState.Begin)
            {
                if (touchpadScreenRect.Contains(position))
                {
                    lastPosition = position;
                    isDragging = true;
                    Logger.AddLogEntry($"{inputSource} drag started inside touchpad");
                }
            }
            else if (state == PointerInputState.Move)
            {
                if (!isDragging) return didSendMove;

                // Calculate raw delta since last position
                Vector2 rawDelta = position - lastPosition;

                float timestamp = Time.time;
                Vector2 filteredDelta = _inputFilter.Filter(rawDelta, timestamp);

                if (filteredDelta.sqrMagnitude > 0.0001f)
                {
                    Vector2 processedDelta = ProcessMotionVector(filteredDelta);
                    int finalDeltaX = Mathf.RoundToInt(processedDelta.x);
                    int finalDeltaY = Mathf.RoundToInt(processedDelta.y);

                    if (finalDeltaX != 0 || finalDeltaY != 0)
                    {
                        finalDeltaY = -finalDeltaY;
                        if (!isEditorMode) BleHidManager.MoveMouse(finalDeltaX, finalDeltaY);
                        else Logger?.AddLogEntry($"{inputSource} delta: ({finalDeltaX}, {finalDeltaY})");
                        lastPosition = position;
                        didSendMove = true;
                    }
                }
            }
            else if (state == PointerInputState.End)
            {
                if (isDragging)
                {
                    isDragging = false;
                    Logger.AddLogEntry($"{inputSource} drag ended");
                }
            }

            return didSendMove;
        }

        /// <summary>
        /// Processes a motion vector by applying sensitivities and scaling
        /// </summary>
        private Vector2 ProcessMotionVector(Vector2 motionVector)
        {
            // Get the direction and magnitude separately
            float magnitude = motionVector.magnitude;
            Vector2 direction = motionVector / magnitude;

            // Apply different sensitivities to each axis but keep as float
            direction.x *= HorizontalSensitivity;
            direction.y *= VerticalSensitivity;

            // Re-normalize after applying different axis sensitivities
            if (direction.sqrMagnitude > 0) { direction = direction.normalized; }

            // Apply global scale to the magnitude
            float scaledMagnitude = magnitude * GlobalScale;

            // Reconstruct the vector with proper direction and scaled magnitude
            return direction * scaledMagnitude;
        }
    }
}
