using UnityEngine;
using System;
using System.Collections.Generic;
using Inventonater.BleHid.UI.Filters;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// Different input states for any pointer device
    /// </summary>
    public enum PointerInputState
    {
        Begin,   // Input started (mouse down, touch began)
        Move,    // Input moved (mouse moved, touch moved)
        End      // Input ended (mouse up, touch ended/canceled)
    }
    
    /// <summary>
    /// UI component for mouse controls including touchpad functionality
    /// with support for external input sources
    /// </summary>
    public class MouseControlsComponent : UIComponent
    {
        // Performance metrics
        private float _fpsUpdateInterval = 0.5f; // How often to update FPS (in seconds)
        private float _lastFpsUpdateTime;
        private int _frameCount = 0;
        private float _currentFps = 0;
        
        // Target framerate control
        private int _targetFrameRate = 60; // Default to 60 FPS
        private const int MIN_FRAMERATE = 30;
        private const int MAX_FRAMERATE = 90;
        
        // Mouse message metrics
        private int _mouseMessageCount = 0;
        private float _lastMouseMessageCountUpdateTime;
        private float _currentMouseMessagesPerSecond = 0;
        private Queue<float> _messageTimestamps = new Queue<float>();
        
        private Rect touchpadRect;
        private Vector2 lastTouchPosition;
        private bool isMouseDragging = false;
        
        // Global scale applies to all mouse movement
        private float _globalScale = 1.0f;
        public float GlobalScale
        {
            get => _globalScale;
            set => _globalScale = value;
        }
        
        // Mouse motion parameters with properties for automatic filter updates
        private float _horizontalSensitivity = 3.0f;
        public float HorizontalSensitivity 
        {
            get => _horizontalSensitivity;
            set => _horizontalSensitivity = value;
        }
        
        private float _verticalSensitivity = 3.0f;
        public float VerticalSensitivity 
        {
            get => _verticalSensitivity;
            set => _verticalSensitivity = value;
        }
        
        // Unified input filter for mouse movement
        private IInputFilter inputFilter;
        
        // Current filter type
        private InputFilterFactory.FilterType _currentFilterType = InputFilterFactory.FilterType.OneEuro;
        
        /// <summary>
        /// Get or set the current filter type
        /// </summary>
        public InputFilterFactory.FilterType CurrentFilterType
        {
            get => _currentFilterType;
            set
            {
                if (_currentFilterType != value)
                {
                    // Store old filter to check if we're changing types
                    var oldFilter = inputFilter;
                    
                    _currentFilterType = value;
                    
                    // Create a new filter or reuse existing one
                    inputFilter = InputFilterFactory.CreateFilter(_currentFilterType);
                    
                    // Reset the filter to clear any state
                    inputFilter.Reset();
                    
                    Logger.AddLogEntry($"Changed input filter to: {inputFilter.Name}");
                }
            }
        }
        
        public override void Initialize(BleHidManager bleHidManager, LoggingManager logger, bool isEditorMode)
        {
            base.Initialize(bleHidManager, logger, isEditorMode);
            
            // Initialize filters with the default type
            _currentFilterType = InputFilterFactory.FilterType.OneEuro;
            
            // Make sure we have a valid filter right away
            inputFilter = InputFilterFactory.CreateFilter(_currentFilterType);
            
            // Initialize touchpad area (will be in the center of the mouse tab)
            touchpadRect = new Rect(Screen.width / 2 - 150, Screen.height / 2 - 100, 300, 200);
            
            // Initialize performance metrics
            _lastFpsUpdateTime = Time.time;
            _lastMouseMessageCountUpdateTime = Time.time;
            _currentFps = 0;
            _currentMouseMessagesPerSecond = 0;
            
            // Set the default target framerate
            _targetFrameRate = 60;
            Application.targetFrameRate = _targetFrameRate;
            Logger.AddLogEntry($"Initial target framerate set to: {_targetFrameRate}");
        }
        
        public override void Update()
        {
            // Update FPS counter
            _frameCount++;
            float currentTime = Time.time;
            
            // Calculate FPS and reset counter if update interval has elapsed
            if (currentTime - _lastFpsUpdateTime > _fpsUpdateInterval)
            {
                _currentFps = _frameCount / (currentTime - _lastFpsUpdateTime);
                _frameCount = 0;
                _lastFpsUpdateTime = currentTime;
                
                // Calculate messages per second
                // Remove timestamps older than 1 second
                while (_messageTimestamps.Count > 0 && _messageTimestamps.Peek() < currentTime - 1.0f)
                {
                    _messageTimestamps.Dequeue();
                }
                
                _currentMouseMessagesPerSecond = _messageTimestamps.Count;
            }
            
            // Only process touchpad input when active
            if (!IsActive())
            {
                return;
            }

            // Process direct touch input (mobile)
            if (Input.touchCount > 0)
            {
                HandleTouchInput(Input.GetTouch(0));
            }
            // Process mouse input (editor/desktop)
            #if UNITY_EDITOR
            else if (IsEditorMode)
            {
                HandleMouseInput();
            }
            #endif
        }
        
        public override void DrawUI()
        {
            UIHelper.BeginSection("Mouse Touchpad");
            
            // Display performance metrics
            GUILayout.Label($"FPS: {_currentFps:F1} | Mouse Messages: {_currentMouseMessagesPerSecond:F0}/sec", 
                           new GUIStyle(GUI.skin.label) { fontStyle = FontStyle.Bold });
            
            // Target framerate slider
            GUILayout.Label("Target FPS: Limits maximum frame rate");
            float newFrameRate = UIHelper.SliderWithLabels(
                "Low", (float)_targetFrameRate, MIN_FRAMERATE, MAX_FRAMERATE, "High", 
                "Target FPS: {0:F0}", UIHelper.StandardSliderOptions);
                
            int roundedFrameRate = Mathf.RoundToInt(newFrameRate);
            if (roundedFrameRate != _targetFrameRate)
            {
                _targetFrameRate = roundedFrameRate;
                Application.targetFrameRate = _targetFrameRate;
                Logger.AddLogEntry($"Target framerate set to: {_targetFrameRate}");
            }
            
            GUILayout.Space(5);
            
            // Show touchpad instruction
            GUILayout.Label("Touchpad Area: Touch and drag to control mouse pointer");
            GUILayout.Label("Drag in touchpad area to send mouse movement to connected device");

            // Draw touchpad area using a Box with visual style to make it obvious
            GUIStyle touchpadStyle = new GUIStyle(GUI.skin.box);
            touchpadStyle.normal.background = Texture2D.grayTexture;

            // Add current drag status if in editor mode
            string touchpadLabel = "Click and drag (can drag outside)";
            #if UNITY_EDITOR
            if (IsEditorMode && isMouseDragging)
            {
                touchpadLabel = "DRAGGING - Move mouse to control";
            }
            #endif

            // Draw the touchpad with clear visual feedback
            GUILayout.Box(touchpadLabel, touchpadStyle, GUILayout.Height(200));

            // Update touchpad rect after layout to get the actual position
            if (Event.current.type == EventType.Repaint)
            {
                Rect lastRect = GUILayoutUtility.GetLastRect();
                touchpadRect = new Rect(lastRect.x, lastRect.y, lastRect.width, lastRect.height);
            }
            
            UIHelper.EndSection();
            
            // Mouse motion tuning controls
            UIHelper.BeginSection("Mouse Tuning");
            
            // --- GLOBAL SETTINGS SECTION ---
            
            // Global scale slider
            GUILayout.Label("Global Speed: Adjusts overall mouse movement speed");
            float newGlobalScale = UIHelper.SliderWithLabels(
                "Slow", _globalScale, 0.25f, 10.0f, "Fast", 
                "Global Speed: {0:F2}×", UIHelper.StandardSliderOptions);
                
            if (newGlobalScale != _globalScale)
            {
                GlobalScale = newGlobalScale;
            }
            
            // Horizontal sensitivity slider
            GUILayout.Label("Horizontal Speed: Adjusts left-right sensitivity");
            float newHorizontalSensitivity = UIHelper.SliderWithLabels(
                "Low", _horizontalSensitivity, 1.0f, 10.0f, "High", 
                "Horizontal Speed: {0:F1}", UIHelper.StandardSliderOptions);
                
            if (newHorizontalSensitivity != _horizontalSensitivity)
            {
                HorizontalSensitivity = newHorizontalSensitivity;
            }
            
            // Vertical sensitivity slider
            GUILayout.Label("Vertical Speed: Adjusts up-down sensitivity");
            float newVerticalSensitivity = UIHelper.SliderWithLabels(
                "Low", _verticalSensitivity, 1.0f, 10.0f, "High", 
                "Vertical Speed: {0:F1}", UIHelper.StandardSliderOptions);
                
            if (newVerticalSensitivity != _verticalSensitivity)
            {
                VerticalSensitivity = newVerticalSensitivity;
            }
            
            GUILayout.Space(10);
            
            // --- FILTER SELECTION SECTION ---
            
            // Filter type selection
            GUILayout.Label("Input Filter: Determines how mouse movement is processed");
            GUILayout.BeginHorizontal();
            foreach (var filterType in InputFilterFactory.GetAvailableFilterTypes())
            {
                bool isSelected = filterType == _currentFilterType;
                GUI.enabled = !isSelected;
                
                if (GUILayout.Button(InputFilterFactory.GetFilterName(filterType), 
                                     isSelected ? GUI.skin.box : GUI.skin.button, 
                                     GUILayout.Height(30)))
                {
                    CurrentFilterType = filterType;
                }
                
                GUI.enabled = true;
            }
            GUILayout.EndHorizontal();
            
            // Show filter description if available
            if (inputFilter != null)
            {
                GUILayout.Label(inputFilter.Description, GUI.skin.box);
            }
            
            GUILayout.Space(10);
            
            // --- FILTER-SPECIFIC PARAMETERS SECTION ---
            
            // Let the filter draw its own parameter controls
            if (inputFilter != null)
            {
                inputFilter.DrawParameterControls(Logger);
            }
            
            UIHelper.EndSection();

            // Mouse button controls
            UIHelper.BeginSection("Mouse Buttons");
            
            GUILayout.Label("Click buttons to send mouse button actions to the connected device");
            
            string[] buttonLabels = { "Left Click", "Middle Click", "Right Click" };
            Action[] buttonActions = {
                () => BleHidManager.ClickMouseButton(BleHidConstants.BUTTON_LEFT),
                () => BleHidManager.ClickMouseButton(BleHidConstants.BUTTON_MIDDLE),
                () => BleHidManager.ClickMouseButton(BleHidConstants.BUTTON_RIGHT)
            };
            string[] buttonMessages = {
                "Left click pressed",
                "Middle click pressed",
                "Right click pressed"
            };
            
            UIHelper.ActionButtonRow(
                buttonLabels, 
                buttonActions, 
                IsEditorMode, 
                Logger, 
                buttonMessages,
                UIHelper.LargeButtonOptions);
            
            UIHelper.EndSection();
        }
        
        /// <summary>
        /// Handle pointer input from any source (touch, mouse, external devices)
        /// </summary>
        /// <param name="position">Screen position of the input</param>
        /// <param name="state">Current state of the input (begin, move, end)</param>
        /// <param name="inputSource">Source name for logging</param>
        public void HandlePointerInput(Vector2 position, PointerInputState state, string inputSource = "External")
        {
            // Skip if component is not active
            if (!IsActive())
                return;
                
            // Get touchpad area in screen coordinates
            Rect screenTouchpadRect = GetScreenTouchpadRect();
            
            // Log touchpad boundaries for debugging when input begins
            if (state == PointerInputState.Begin)
            {
                Logger.AddLogEntry($"Touchpad screen rect: ({screenTouchpadRect.x}, {screenTouchpadRect.y}, w:{screenTouchpadRect.width}, h:{screenTouchpadRect.height})");
                Logger.AddLogEntry($"{inputSource} input began at: ({position.x}, {position.y})");
            }

            switch (state)
            {
                case PointerInputState.Begin:
                    // Start dragging if input begins inside touchpad
                    if (screenTouchpadRect.Contains(position))
                    {
                        lastTouchPosition = position;
                        isMouseDragging = true;
                        Logger.AddLogEntry($"{inputSource} drag started inside touchpad");
                    }
                    break;

                case PointerInputState.Move:
                    // Process movement if we're dragging
                    if (isMouseDragging)
                    {
                        ProcessPointerMovement(position);
                    }
                    break;

                case PointerInputState.End:
                    if (isMouseDragging)
                    {
                        isMouseDragging = false;
                        Logger.AddLogEntry($"{inputSource} drag ended");
                    }
                    break;
            }
        }
        
        /// <summary>
        /// Handles touch input for the touchpad area
        /// </summary>
        private void HandleTouchInput(Touch touch)
        {
            // Convert Touch input state to our generic PointerInputState
            PointerInputState state;
            
            switch (touch.phase)
            {
                case TouchPhase.Began:
                    state = PointerInputState.Begin;
                    break;
                case TouchPhase.Moved:
                    state = PointerInputState.Move;
                    break;
                case TouchPhase.Ended:
                case TouchPhase.Canceled:
                    state = PointerInputState.End;
                    break;
                default:
                    return; // Ignore other phases
            }
            
            // Use the common input handler
            HandlePointerInput(touch.position, state, "Touch");
        }

        #if UNITY_EDITOR
        /// <summary>
        /// Handles mouse input for the touchpad area (editor only)
        /// </summary>
        private void HandleMouseInput()
        {
            // Convert mouse position to screen coordinates
            Vector2 mouseScreenPos = new Vector2(
                Input.mousePosition.x,
                Input.mousePosition.y
            );

            // Map mouse button states to our pointer states
            if (Input.GetMouseButtonDown(0))
            {
                HandlePointerInput(mouseScreenPos, PointerInputState.Begin, "Mouse");
            }
            else if (Input.GetMouseButton(0))
            {
                HandlePointerInput(mouseScreenPos, PointerInputState.Move, "Mouse");
            }
            else if (Input.GetMouseButtonUp(0))
            {
                HandlePointerInput(mouseScreenPos, PointerInputState.End, "Mouse");
            }
        }
        #endif
        
        /// <summary>
        /// Directly sends motion deltas to the connected device, bypassing touchpad constraints
        /// </summary>
        /// <param name="motionDelta">Raw motion vector to apply</param>
        /// <param name="source">Source name for logging</param>
        /// <returns>True if the motion was processed and sent</returns>
        public bool SendDirectMotion(Vector2 motionDelta, string source = "External")
        {
            if (!IsActive() || inputFilter == null)
                return false;
                
            // Apply unified vector filter
            float timestamp = Time.time;
            Vector2 filteredDelta = inputFilter.Filter(motionDelta, timestamp);
            
            // Don't process extremely small movements
            if (filteredDelta.sqrMagnitude < 0.0001f)
                return false;
            
            // Get the direction and magnitude separately
            float magnitude = filteredDelta.magnitude;
            Vector2 direction = filteredDelta / magnitude;
            
            // Apply different sensitivities to each axis but keep as float
            direction.x *= _horizontalSensitivity;
            direction.y *= _verticalSensitivity;
            
            // Re-normalize after applying different axis sensitivities
            if (direction.sqrMagnitude > 0)
            {
                direction = direction.normalized;
            }
            
            // Apply global scale to the magnitude
            float scaledMagnitude = magnitude * _globalScale;
            
            // Reconstruct the vector with proper direction and scaled magnitude
            Vector2 scaledDelta = direction * scaledMagnitude;
            
            // Only convert to int at the final step
            int finalDeltaX = Mathf.RoundToInt(scaledDelta.x);
            int finalDeltaY = Mathf.RoundToInt(scaledDelta.y);
            
            // Only process if movement is significant
            if (finalDeltaX != 0 || finalDeltaY != 0)
            {
                // Invert Y direction for mouse movement (screen Y goes down, mouse Y goes up)
                finalDeltaY = -finalDeltaY;

                // Send movement or log in editor mode
                if (!IsEditorMode)
                {
                    BleHidManager.MoveMouse(finalDeltaX, finalDeltaY);
                    Logger.AddLogEntry($"{source} direct motion: ({finalDeltaX}, {finalDeltaY})");
                }
                else
                {
                    Logger.AddLogEntry($"{source} direct motion: ({finalDeltaX}, {finalDeltaY})");
                }
                
                // Track this mouse message
                _messageTimestamps.Enqueue(Time.time);
                
                return true;
            }
            
            return false;
        }

        /// <summary>
        /// Processes pointer movement for both touch and mouse input
        /// </summary>
        private void ProcessPointerMovement(Vector2 currentPosition)
        {
            // Calculate raw delta since last position
            Vector2 rawDelta = currentPosition - lastTouchPosition;
            
            // Apply unified vector filter
            float timestamp = Time.time;
            Vector2 filteredDelta = inputFilter.Filter(rawDelta, timestamp);
            
            // Don't process extremely small movements
            if (filteredDelta.sqrMagnitude < 0.0001f)
            {
                return;
            }
            
            // Get the direction and magnitude separately
            float magnitude = filteredDelta.magnitude;
            Vector2 direction = filteredDelta / magnitude;
            
            // Apply different sensitivities to each axis but keep as float
            direction.x *= _horizontalSensitivity;
            direction.y *= _verticalSensitivity;
            
            // Re-normalize after applying different axis sensitivities
            if (direction.sqrMagnitude > 0)
            {
                direction = direction.normalized;
            }
            
            // Apply global scale to the magnitude
            float scaledMagnitude = magnitude * _globalScale;
            
            // Reconstruct the vector with proper direction and scaled magnitude
            Vector2 scaledDelta = direction * scaledMagnitude;
            
            // Only convert to int at the final step
            int finalDeltaX = Mathf.RoundToInt(scaledDelta.x);
            int finalDeltaY = Mathf.RoundToInt(scaledDelta.y);
            
            // Only process if movement is significant
            if (finalDeltaX != 0 || finalDeltaY != 0)
            {
                // Invert Y direction for mouse movement (screen Y goes down, mouse Y goes up)
                finalDeltaY = -finalDeltaY;

                // Send movement or log in editor mode
                if (!IsEditorMode)
                {
                    BleHidManager.MoveMouse(finalDeltaX, finalDeltaY);
                }
                else
                {
                    Logger.AddLogEntry($"Mouse delta: ({finalDeltaX}, {finalDeltaY})");
                }

                // Track this mouse message
                _messageTimestamps.Enqueue(Time.time);
                
                // Update last position for next calculation
                lastTouchPosition = currentPosition;
            }
        }
        
        /// <summary>
        /// Checks if the component is ready to receive input
        /// </summary>
        private bool IsActive()
        {
            return BleHidManager != null && (BleHidManager.IsConnected || IsEditorMode);
        }

        /// <summary>
        /// Converts GUI touchpad rect to screen coordinates
        /// </summary>
        private Rect GetScreenTouchpadRect()
        {
            // Convert GUI coordinates to screen coordinates
            return new Rect(
                touchpadRect.x,               // X stays the same
                Screen.height - touchpadRect.y - touchpadRect.height, // Convert GUI Y to screen Y
                touchpadRect.width,           // Width stays the same
                touchpadRect.height           // Height stays the same
            );
        }
    }
}
