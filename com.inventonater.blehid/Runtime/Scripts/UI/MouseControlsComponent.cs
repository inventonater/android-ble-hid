using UnityEngine;
using System;

namespace Inventonater.BleHid
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
        // Performance tracking
        private PerformanceTracker _performanceTracker;
        
        // Input processing
        private PointerInputProcessor _inputProcessor;
        
        private Rect touchpadRect;
        
        // Global scale applies to all mouse movement
        private float _globalScale = 1.0f;
        public float GlobalScale
        {
            get => _globalScale;
            set {
                _globalScale = value;
                if (_inputProcessor != null) {
                    _inputProcessor.SetSensitivity(_globalScale, _horizontalSensitivity, _verticalSensitivity);
                }
            }
        }
        
        // Mouse motion parameters with properties for automatic filter updates
        private float _horizontalSensitivity = 3.0f;
        public float HorizontalSensitivity 
        {
            get => _horizontalSensitivity;
            set {
                _horizontalSensitivity = value;
                if (_inputProcessor != null) {
                    _inputProcessor.SetSensitivity(_globalScale, _horizontalSensitivity, _verticalSensitivity);
                }
            }
        }
        
        private float _verticalSensitivity = 3.0f;
        public float VerticalSensitivity 
        {
            get => _verticalSensitivity;
            set {
                _verticalSensitivity = value;
                if (_inputProcessor != null) {
                    _inputProcessor.SetSensitivity(_globalScale, _horizontalSensitivity, _verticalSensitivity);
                }
            }
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
                    
                    // Update the input processor with the new filter
                    if (_inputProcessor != null)
                    {
                        _inputProcessor.SetInputFilter(inputFilter);
                    }
                    
                    Logger.AddLogEntry($"Changed input filter to: {inputFilter.Name}");
                }
            }
        }
        
        public override void Initialize(BleHidManager bleHidManager, LoggingManager logger)
        {
            base.Initialize(bleHidManager, logger);
            
            // Initialize filters with the default type
            _currentFilterType = InputFilterFactory.FilterType.OneEuro;
            
            // Make sure we have a valid filter right away
            inputFilter = InputFilterFactory.CreateFilter(_currentFilterType);
            
            // Initialize touchpad area (will be in the center of the mouse tab)
            touchpadRect = new Rect(Screen.width / 2 - 150, Screen.height / 2 - 100, 300, 200);
            
            // Initialize performance tracker
            _performanceTracker = new PerformanceTracker();
            
            // Initialize input processor
            _inputProcessor = new PointerInputProcessor(bleHidManager, logger, isEditorMode);
            _inputProcessor.SetInputFilter(inputFilter);
            _inputProcessor.SetSensitivity(_globalScale, _horizontalSensitivity, _verticalSensitivity);
            
            // Set initial touchpad rect
            UpdateTouchpadRect();
        }
        
        private void UpdateTouchpadRect()
        {
            if (_inputProcessor != null)
            {
                _inputProcessor.SetTouchpadRect(GetScreenTouchpadRect());
            }
        }
        
        public virtual void Update()
        {
            // Update performance metrics
            _performanceTracker.Update();
            
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
            else if (isEditorMode)
            {
                HandleMouseInput();
            }
            #endif
        }
        
        public virtual void DrawUI()
        {
            UIHelper.BeginSection("Mouse Touchpad");
            
            // Display mouse message metrics
            GUILayout.Label($"Mouse Messages: {_performanceTracker.MessagesPerSecond:F0}/sec", 
                           new GUIStyle(GUI.skin.label) { fontStyle = FontStyle.Bold });
            
            // Show touchpad instruction
            GUILayout.Label("Touchpad Area: Touch and drag to control mouse pointer");
            GUILayout.Label("Drag in touchpad area to send mouse movement to connected device");

            // Draw touchpad area using a Box with visual style to make it obvious
            GUIStyle touchpadStyle = new GUIStyle(GUI.skin.box);
            touchpadStyle.normal.background = Texture2D.grayTexture;

            // Add current drag status if in editor mode
            string touchpadLabel = "Click and drag (can drag outside)";
            #if UNITY_EDITOR
            if (isEditorMode && _inputProcessor.IsDragging())
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
                UpdateTouchpadRect();
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
            if (_inputProcessor.HandlePointerInput(position, state, inputSource))
            {
                // Message was sent, track for performance metrics
                _performanceTracker.TrackMessage();
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
            bool result = _inputProcessor.SendDirectMotion(motionDelta, source);
            if (result)
            {
                // Message was sent, track for performance metrics
                _performanceTracker.TrackMessage();
            }
            return result;
        }
        
        /// <summary>
        /// Checks if the component is ready to receive input
        /// </summary>
        private bool IsActive()
        {
            return BleHidManager != null && (BleHidManager.IsConnected || isEditorMode);
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
