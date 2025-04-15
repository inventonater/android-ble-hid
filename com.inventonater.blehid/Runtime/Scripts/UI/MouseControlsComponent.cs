using UnityEngine;
using System;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// UI component for mouse controls including touchpad functionality
    /// </summary>
    public class MouseControlsComponent : UIComponent
    {
        // One Euro Filter implementation for mouse pointer smoothing
        private class OneEuroFilter
        {
            private float mincutoff;    // Minimum cutoff frequency
            private float beta;         // Cutoff slope (speed coefficient)
            private float dcutoff;      // Derivative cutoff frequency
            
            private float x_prev;       // Previous filtered value
            private float dx_prev;      // Previous derivative
            private float lastTime;     // Last update timestamp
            private bool initialized;   // Whether filter has been initialized
            
            public OneEuroFilter(float mincutoff = 1.0f, float beta = 0.007f, float dcutoff = 1.0f)
            {
                this.mincutoff = mincutoff;
                this.beta = beta;
                this.dcutoff = dcutoff;
                initialized = false;
            }
            
            private float LowPassFilter(float x, float alpha, float y_prev)
            {
                return alpha * x + (1.0f - alpha) * y_prev;
            }
            
            private float ComputeAlpha(float cutoff, float deltaTime)
            {
                float tau = 1.0f / (2.0f * Mathf.PI * cutoff);
                return 1.0f / (1.0f + tau / deltaTime);
            }
            
            public float Filter(float x, float timestamp)
            {
                if (!initialized)
                {
                    x_prev = x;
                    dx_prev = 0.0f;
                    lastTime = timestamp;
                    initialized = true;
                    return x;
                }
                
                float deltaTime = timestamp - lastTime;
                if (deltaTime <= 0.0f) deltaTime = 0.001f; // Prevent division by zero
                lastTime = timestamp;
                
                // Estimate derivative
                float dx = (x - x_prev) / deltaTime;
                
                // Filter derivative
                float edx = LowPassFilter(dx, ComputeAlpha(dcutoff, deltaTime), dx_prev);
                dx_prev = edx;
                
                // Adjust cutoff based on derivative
                float cutoff = mincutoff + beta * Mathf.Abs(edx);
                
                // Filter signal
                float ex = LowPassFilter(x, ComputeAlpha(cutoff, deltaTime), x_prev);
                x_prev = ex;
                
                return ex;
            }
            
            // Method to filter Unity's Vector2 directly
            public Vector2 Filter(Vector2 point, float timestamp)
            {
                return new Vector2(
                    Filter(point.x, timestamp),
                    Filter(point.y, timestamp)
                );
            }
            
            // Set new parameters
            public void SetParameters(float mincutoff, float beta)
            {
                this.mincutoff = mincutoff;
                this.beta = beta;
            }
        }
        
        private Rect touchpadRect;
        private Vector2 lastTouchPosition;
        private bool isMouseDragging = false;
        
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
        
        // One Euro Filters (one for each axis)
        private OneEuroFilter xFilter;
        private OneEuroFilter yFilter;
        
        // Updates both filters with current parameters
        private void UpdateFilterParameters()
        {
            if (xFilter != null && yFilter != null)
            {
                xFilter.SetParameters(_minCutoff, _betaValue);
                yFilter.SetParameters(_minCutoff, _betaValue);
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
            
            // Initialize filters
            xFilter = new OneEuroFilter(_minCutoff, _betaValue);
            yFilter = new OneEuroFilter(_minCutoff, _betaValue);
            
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
            GUILayout.Label("Touch & Drag in the area below to move mouse");

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
            GUILayout.Box(touchpadLabel, touchpadStyle, GUILayout.Width(300), GUILayout.Height(200));

            // Update touchpad rect after layout to get the actual position
            if (Event.current.type == EventType.Repaint)
            {
                Rect lastRect = GUILayoutUtility.GetLastRect();
                touchpadRect = new Rect(lastRect.x, lastRect.y, lastRect.width, lastRect.height);
            }
            
            UIHelper.EndSection();
            
            // Mouse motion tuning controls
            UIHelper.BeginSection("Mouse Tuning");
            
            // Preset selector
            GUILayout.BeginHorizontal();
            GUILayout.Label("Presets:", GUILayout.Width(60));
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
            float newHorizontalSensitivity = UIHelper.SliderWithLabels(
                "Low", _horizontalSensitivity, 1.0f, 10.0f, "High", 
                "Horizontal: {0:F1}", UIHelper.StandardSliderOptions);
                
            if (newHorizontalSensitivity != _horizontalSensitivity)
            {
                HorizontalSensitivity = newHorizontalSensitivity;
            }
            
            // Vertical sensitivity slider
            float newVerticalSensitivity = UIHelper.SliderWithLabels(
                "Low", _verticalSensitivity, 1.0f, 10.0f, "High", 
                "Vertical: {0:F1}", UIHelper.StandardSliderOptions);
                
            if (newVerticalSensitivity != _verticalSensitivity)
            {
                VerticalSensitivity = newVerticalSensitivity;
            }
            
            // Smoothing parameter (Min Cutoff)
            float newMinCutoff = UIHelper.SliderWithLabels(
                "High", _minCutoff, 0.1f, 5f, "Low", 
                "Smoothing: {0:F1}", UIHelper.StandardSliderOptions);
                
            if (newMinCutoff != _minCutoff)
            {
                MinCutoff = newMinCutoff;
            }
            
            // Responsiveness parameter (Beta)
            float newBeta = UIHelper.SliderWithLabels(
                "Smooth", _betaValue, 0.001f, 0.5f, "Responsive", 
                "Responsiveness: {0:F3}", UIHelper.StandardSliderOptions);
                
            if (newBeta != _betaValue)
            {
                BetaValue = newBeta;
            }
            
            UIHelper.EndSection();

            // Mouse button controls
            UIHelper.BeginSection("Mouse Buttons");
            
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
            
            // Apply 1€ filter
            float timestamp = Time.time;
            float filteredDeltaX = xFilter.Filter(rawDelta.x, timestamp);
            float filteredDeltaY = yFilter.Filter(rawDelta.y, timestamp);
            
            // Apply sensitivity
            int scaledDeltaX = (int)(filteredDeltaX * _horizontalSensitivity);
            int scaledDeltaY = (int)(filteredDeltaY * _verticalSensitivity);

            // Only process if movement is significant
            if (Mathf.Abs(scaledDeltaX) > 0 || Mathf.Abs(scaledDeltaY) > 0)
            {
                // Invert Y direction for mouse movement (screen Y goes down, mouse Y goes up)
                scaledDeltaY = -scaledDeltaY;

                // Send movement or log in editor mode
                if (!IsEditorMode)
                {
                    BleHidManager.MoveMouse(scaledDeltaX, scaledDeltaY);
                    Logger.AddLogEntry($"Sending mouse delta: ({scaledDeltaX}, {scaledDeltaY})");
                }
                else
                {
                    Logger.AddLogEntry($"Mouse delta: ({scaledDeltaX}, {scaledDeltaY})");
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
