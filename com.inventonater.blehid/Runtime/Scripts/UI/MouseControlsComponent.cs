using UnityEngine;

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
        private float mouseScale = 3;
        
        public override void Initialize(BleHidManager bleHidManager, LoggingManager logger, bool isEditorMode)
        {
            base.Initialize(bleHidManager, logger, isEditorMode);
            
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

            // Mouse button controls
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Left Click", GUILayout.Height(80)))
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Left click pressed");
                else
                    BleHidManager.ClickMouseButton(BleHidConstants.BUTTON_LEFT);
            }

            if (GUILayout.Button("Middle Click", GUILayout.Height(80)))
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Middle click pressed");
                else
                    BleHidManager.ClickMouseButton(BleHidConstants.BUTTON_MIDDLE);
            }

            if (GUILayout.Button("Right Click", GUILayout.Height(80)))
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Right click pressed");
                else
                    BleHidManager.ClickMouseButton(BleHidConstants.BUTTON_RIGHT);
            }
            GUILayout.EndHorizontal();
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
            // Calculate delta since last position
            Vector2 delta = currentPosition - lastTouchPosition;

            // Scale the movement (adjusted for sensitivity)
            int scaledDeltaX = (int)(delta.x * mouseScale);
            int scaledDeltaY = (int)(delta.y * mouseScale);

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
