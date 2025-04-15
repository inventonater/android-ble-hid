using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Handles processing of pointer input (touch, mouse, external) with motion filtering
    /// </summary>
    public class PointerInputProcessor
    {
        private static LoggingManager logger => LoggingManager.Instance;

        private BleHidManager bleHidManager => BleHidManager.Instance;
        private bool isEditorMode;
        private Vector2 lastPosition;
        private bool isDragging = false;
        private Rect touchpadScreenRect;
        
        // Input filters
        private IInputFilter inputFilter;
        
        // Sensitivity settings
        private float globalScale = 1.0f;
        private float horizontalSensitivity = 3.0f;
        private float verticalSensitivity = 3.0f;

        /// <summary>
        /// Set the touchpad boundaries in screen coordinates
        /// </summary>
        public void SetTouchpadRect(Rect screenRect)
        {
            touchpadScreenRect = screenRect;
        }
        
        /// <summary>
        /// Set input filter to use for processing
        /// </summary>
        public void SetInputFilter(IInputFilter filter)
        {
            inputFilter = filter;
        }
        
        /// <summary>
        /// Set sensitivity parameters
        /// </summary>
        public void SetSensitivity(float globalScale, float horizontalSensitivity, float verticalSensitivity)
        {
            this.globalScale = globalScale;
            this.horizontalSensitivity = horizontalSensitivity;
            this.verticalSensitivity = verticalSensitivity;
        }
        
        /// <summary>
        /// Is the processor actively tracking a drag operation
        /// </summary>
        public bool IsDragging()
        {
            return isDragging;
        }
        
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
            if (bleHidManager == null || inputFilter == null)
                return false;
                
            // Determine if we should process input (connected or in editor mode)
            bool shouldProcess = bleHidManager.IsConnected || isEditorMode;
            if (!shouldProcess)
                return false;
                
            // Log touchpad boundaries for debugging when input begins
            if (state == PointerInputState.Begin)
            {
                logger?.AddLogEntry($"Touchpad screen rect: ({touchpadScreenRect.x}, {touchpadScreenRect.y}, w:{touchpadScreenRect.width}, h:{touchpadScreenRect.height})");
                logger?.AddLogEntry($"{inputSource} input began at: ({position.x}, {position.y})");
            }

            bool didSendMove = false;
            
            switch (state)
            {
                case PointerInputState.Begin:
                    // Start dragging if input begins inside touchpad
                    if (touchpadScreenRect.Contains(position))
                    {
                        lastPosition = position;
                        isDragging = true;
                        logger?.AddLogEntry($"{inputSource} drag started inside touchpad");
                    }
                    break;

                case PointerInputState.Move:
                    // Process movement if we're dragging
                    if (isDragging)
                    {
                        didSendMove = ProcessPointerMovement(position, inputSource);
                    }
                    break;

                case PointerInputState.End:
                    if (isDragging)
                    {
                        isDragging = false;
                        logger?.AddLogEntry($"{inputSource} drag ended");
                    }
                    break;
            }
            
            return didSendMove;
        }

        /// <summary>
        /// Processes pointer movement for both touch and mouse input
        /// </summary>
        private bool ProcessPointerMovement(Vector2 currentPosition, string source = "Pointer")
        {
            // Calculate raw delta since last position
            Vector2 rawDelta = currentPosition - lastPosition;
            
            // Apply unified vector filter
            float timestamp = Time.time;
            Vector2 filteredDelta = inputFilter.Filter(rawDelta, timestamp);
            
            // Don't process extremely small movements
            if (filteredDelta.sqrMagnitude < 0.0001f)
            {
                return false;
            }
            
            // Process the movement
            Vector2 processedDelta = ProcessMotionVector(filteredDelta);
            
            // Only convert to int at the final step
            int finalDeltaX = Mathf.RoundToInt(processedDelta.x);
            int finalDeltaY = Mathf.RoundToInt(processedDelta.y);
            
            // Only process if movement is significant
            if (finalDeltaX != 0 || finalDeltaY != 0)
            {
                // Invert Y direction for mouse movement (screen Y goes down, mouse Y goes up)
                finalDeltaY = -finalDeltaY;

                // Send movement or log in editor mode
                if (!isEditorMode)
                {
                    bleHidManager.MoveMouse(finalDeltaX, finalDeltaY);
                }
                else
                {
                    logger?.AddLogEntry($"{source} delta: ({finalDeltaX}, {finalDeltaY})");
                }
                
                // Update last position for next calculation
                lastPosition = currentPosition;
                
                return true;
            }
            
            return false;
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
            direction.x *= horizontalSensitivity;
            direction.y *= verticalSensitivity;
            
            // Re-normalize after applying different axis sensitivities
            if (direction.sqrMagnitude > 0)
            {
                direction = direction.normalized;
            }
            
            // Apply global scale to the magnitude
            float scaledMagnitude = magnitude * globalScale;
            
            // Reconstruct the vector with proper direction and scaled magnitude
            return direction * scaledMagnitude;
        }
    }
}
