using UnityEngine;
using System;
using Inventonater.BleHid.UI.Filters;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// UI component for mouse controls including touchpad functionality
    /// </summary>
    public class MouseControlsComponent : UIComponent
    {
        
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
        
        private float _minCutoff = 1.0f;    // Higher = less smoothing
        public float MinCutoff 
        {
            get => _minCutoff;
            set 
            {
                _minCutoff = value;
                UpdateFilterParameters();
            }
        }
        
        private float _betaValue = 0.007f;  // Higher = less lag when moving fast
        public float BetaValue 
        {
            get => _betaValue;
            set 
            {
                _betaValue = value;
                UpdateFilterParameters();
            }
        }
        
        // Preset configuration index
        private int currentPresetIndex = 1; // Default to "Standard"
        private readonly string[] presetNames = { "Precision", "Standard", "Fast" };
        
        // Input filters for X and Y axis
        private IInputFilter xFilter;
        private IInputFilter yFilter;
        
        // Current filter type and parameters
        private InputFilterFactory.FilterType _currentFilterType = InputFilterFactory.FilterType.OneEuro;
        private float _filterParam1 = 1.0f;
        private float _filterParam2 = 0.007f;
        
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
                    _currentFilterType = value;
                    
                    // Get default parameters for this filter type
                    InputFilterFactory.GetParameterInfo(
                        _currentFilterType,
                        out _, out _, out _, out _filterParam1,
                        out _, out _, out _, out _filterParam2);
                    
                    // Create new filter instances with default parameters
                    xFilter = InputFilterFactory.CreateFilter(_currentFilterType, _filterParam1, _filterParam2);
                    yFilter = InputFilterFactory.CreateFilter(_currentFilterType, _filterParam1, _filterParam2);
                    
                    Logger.AddLogEntry($"Changed input filter to: {InputFilterFactory.GetFilterName(_currentFilterType)}");
                }
            }
        }
        
        // Updates current filter parameters
        private void UpdateFilterParameters()
        {
            if (xFilter != null && yFilter != null)
            {
                // Store current parameter values
                _filterParam1 = _minCutoff;
                _filterParam2 = _betaValue;
                
                // Update filter parameters
                xFilter.SetParameters(_filterParam1, _filterParam2);
                yFilter.SetParameters(_filterParam1, _filterParam2);
            }
        }
        
        // Apply a preset configuration
        private void ApplyPreset(int presetIndex)
        {
            currentPresetIndex = presetIndex;
            
            switch(presetIndex)
            {
                case 0: // Precision
                    HorizontalSensitivity = 2.0f;
                    VerticalSensitivity = 2.0f;
                    MinCutoff = 0.5f;  // More smoothing
                    BetaValue = 0.004f; // Less acceleration sensitivity
                    break;
                    
                case 1: // Standard
                    HorizontalSensitivity = 3.0f;
                    VerticalSensitivity = 3.0f;
                    MinCutoff = 1.0f;   // Default smoothing
                    BetaValue = 0.007f; // Default acceleration response
                    break;
                    
                case 2: // Fast
                    HorizontalSensitivity = 5.0f;
                    VerticalSensitivity = 5.0f;
                    MinCutoff = 2.0f;   // Less smoothing
                    BetaValue = 0.015f; // More acceleration sensitivity
                    break;
            }
            
            Logger.AddLogEntry($"Applied mouse preset: {presetNames[presetIndex]}");
        }
        
        public override void Initialize(BleHidManager bleHidManager, LoggingManager logger, bool isEditorMode)
        {
            base.Initialize(bleHidManager, logger, isEditorMode);
            
            // Initialize filters with the default type
            CurrentFilterType = InputFilterFactory.FilterType.OneEuro;
            
            // Initialize touchpad area (will be in the center of the mouse tab)
            touchpadRect = new Rect(Screen.width / 2 - 150, Screen.height / 2 - 100, 300, 200);
        }
        
        public override void Update()
        {
            // Only process touchpad input when:
            // 1. BLE is initialized or in editor mode
            // 2. Connected to a device or in editor mode
            if (BleHidManager == null || (!BleHidManager.IsConnected && !IsEditorMode))
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
            
            // Global scale slider
            GUILayout.Label("Global Speed: Adjusts overall mouse movement speed");
            float newGlobalScale = UIHelper.SliderWithLabels(
                "Slow", _globalScale, 0.25f, 10.0f, "Fast", 
                "Global Speed: {0:F2}×", UIHelper.StandardSliderOptions);
                
            if (newGlobalScale != _globalScale)
            {
                GlobalScale = newGlobalScale;
            }
            
            GUILayout.Space(10);
            
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
            if (xFilter != null)
            {
                GUILayout.Label(xFilter.Description, GUI.skin.box);
            }
            
            GUILayout.Space(10);
            
            // Preset selector
            GUILayout.BeginHorizontal();
            GUILayout.Label("Presets:", GUILayout.Width(80));
            for (int i = 0; i < presetNames.Length; i++)
            {
                GUI.enabled = currentPresetIndex != i;
                if (GUILayout.Button(presetNames[i], GUILayout.Height(40)))
                {
                    ApplyPreset(i);
                }
                GUI.enabled = true;
            }
            GUILayout.EndHorizontal();
            
            GUILayout.Space(10);
            
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
            
            // Only show filter parameters if we have a filter that uses them
            if (_currentFilterType != InputFilterFactory.FilterType.None)
            {
                // Get parameter info for current filter type
                InputFilterFactory.GetParameterInfo(
                    _currentFilterType,
                    out string param1Name, out float param1Min, out float param1Max, out _,
                    out string param2Name, out float param2Min, out float param2Max, out _);
                
                // First parameter slider (usually smoothing)
                GUILayout.Label($"{param1Name}: Adjusts filtering strength");
                float newParam1 = UIHelper.SliderWithLabels(
                    "Strong", _filterParam1, param1Min, param1Max, "Light", 
                    $"{param1Name}: {{0:F2}}", UIHelper.StandardSliderOptions);
                    
                if (newParam1 != _filterParam1)
                {
                    _filterParam1 = newParam1;
                    
                    // Update MinCutoff to maintain compatibility with the old code
                    if (_currentFilterType == InputFilterFactory.FilterType.OneEuro)
                    {
                        _minCutoff = _filterParam1;
                    }
                    
                    // Update filter parameters
                    if (xFilter != null && yFilter != null)
                    {
                        xFilter.SetParameters(_filterParam1, _filterParam2);
                        yFilter.SetParameters(_filterParam1, _filterParam2);
                    }
                }
                
                // Second parameter slider (usually response)
                GUILayout.Label($"{param2Name}: Fine-tunes filter behavior");
                float newParam2 = UIHelper.SliderWithLabels(
                    "Low", _filterParam2, param2Min, param2Max, "High", 
                    $"{param2Name}: {{0:F3}}", UIHelper.StandardSliderOptions);
                    
                if (newParam2 != _filterParam2)
                {
                    _filterParam2 = newParam2;
                    
                    // Update BetaValue to maintain compatibility with the old code
                    if (_currentFilterType == InputFilterFactory.FilterType.OneEuro)
                    {
                        _betaValue = _filterParam2;
                    }
                    
                    // Update filter parameters
                    if (xFilter != null && yFilter != null)
                    {
                        xFilter.SetParameters(_filterParam1, _filterParam2);
                        yFilter.SetParameters(_filterParam1, _filterParam2);
                    }
                }
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
        /// Handles touch input for the touchpad area
        /// </summary>
        private void HandleTouchInput(Touch touch)
        {
            // Always work in screen coordinates for input
            Vector2 touchScreenPos = touch.position;
            Rect screenTouchpadRect = GetScreenTouchpadRect();

            // Log touchpad boundaries and touch positions for debugging
            if (touch.phase == TouchPhase.Began)
            {
                Logger.AddLogEntry($"Touchpad screen rect: ({screenTouchpadRect.x}, {screenTouchpadRect.y}, w:{screenTouchpadRect.width}, h:{screenTouchpadRect.height})");
                Logger.AddLogEntry($"Touch began at: ({touchScreenPos.x}, {touchScreenPos.y})");
            }

            switch (touch.phase)
            {
                case TouchPhase.Began:
                    // Start dragging if touch begins inside touchpad
                    if (screenTouchpadRect.Contains(touchScreenPos))
                    {
                        lastTouchPosition = touchScreenPos;
                        isMouseDragging = true;
                        Logger.AddLogEntry("Touch drag started inside touchpad");
                    }
                    break;

                case TouchPhase.Moved:
                    // Process movement if we're dragging
                    if (isMouseDragging)
                    {
                        ProcessPointerMovement(touchScreenPos);
                    }
                    break;

                case TouchPhase.Ended:
                case TouchPhase.Canceled:
                    if (isMouseDragging)
                    {
                        isMouseDragging = false;
                        Logger.AddLogEntry("Touch drag ended");
                    }
                    break;
            }
        }

        #if UNITY_EDITOR
        /// <summary>
        /// Handles mouse input for the touchpad area (editor only)
        /// </summary>
        private void HandleMouseInput()
        {
            // Convert mouse position to screen coordinates (Unity mouse Y is inverted)
            Vector2 mouseScreenPos = new Vector2(
                Input.mousePosition.x,
                Input.mousePosition.y
            );

            Rect screenTouchpadRect = GetScreenTouchpadRect();

            // Start drag on mouse down inside touchpad
            if (Input.GetMouseButtonDown(0))
            {
                if (screenTouchpadRect.Contains(mouseScreenPos))
                {
                    lastTouchPosition = mouseScreenPos;
                    isMouseDragging = true;
                    Logger.AddLogEntry($"Mouse drag started at ({mouseScreenPos.x}, {mouseScreenPos.y})");
                }
            }
            // Continue drag during movement
            else if (Input.GetMouseButton(0) && isMouseDragging)
            {
                ProcessPointerMovement(mouseScreenPos);
            }
            // End drag on mouse up
            else if (Input.GetMouseButtonUp(0) && isMouseDragging)
            {
                isMouseDragging = false;
                Logger.AddLogEntry("Mouse drag ended");
            }
        }
        #endif

        /// <summary>
        /// Processes pointer movement for both touch and mouse input
        /// </summary>
        private void ProcessPointerMovement(Vector2 currentPosition)
        {
            // Calculate raw delta since last position
            Vector2 rawDelta = currentPosition - lastTouchPosition;
            
            // Apply 1€ filter to each axis
            float timestamp = Time.time;
            float filteredDeltaX = xFilter.Filter(rawDelta.x, timestamp);
            float filteredDeltaY = yFilter.Filter(rawDelta.y, timestamp);
            
            // Create filtered vector
            Vector2 filteredDelta = new Vector2(filteredDeltaX, filteredDeltaY);
            
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
                    Logger.AddLogEntry($"Sending mouse delta: ({finalDeltaX}, {finalDeltaY})");
                }
                else
                {
                    Logger.AddLogEntry($"Mouse delta: ({finalDeltaX}, {finalDeltaY})");
                }

                // Update last position for next calculation
                lastTouchPosition = currentPosition;
            }
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
