using UnityEngine;

namespace Inventonater.BleHid.UI.Panels.Remote
{
    /// <summary>
    /// Panel for remote mouse control functionality over BLE HID.
    /// </summary>
    public class RemoteMousePanel : BaseBleHidPanel
    {
        // Virtual touchpad state
        private Rect touchpadRect;
        private Vector2 lastTouchPosition;
        private bool isMouseDragging = false;
        private float mouseScale = 3.0f;
        
        public override bool RequiresConnectedDevice => true;
        
        public override void Initialize(BleHidManager manager)
        {
            base.Initialize(manager);
            
            // Initialize touchpad area (will be adjusted in DrawPanelContent)
            touchpadRect = new Rect(0, 0, 300, 200);
        }
        
        protected override void DrawPanelContent()
        {
            GUILayout.Label("Mouse Controls", titleStyle);
            
            #if UNITY_EDITOR
            bool isEditorMode = true;
            #else
            bool isEditorMode = false;
            #endif
            
            // Precision touchpad
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Touchpad", subtitleStyle);
            
            GUILayout.Label("Touch & Drag in the area below to move mouse");
            
            // Draw touchpad area with visual style
            GUIStyle touchpadStyle = new GUIStyle(GUI.skin.box);
            touchpadStyle.normal.background = MakeColorTexture(new Color(0.2f, 0.2f, 0.2f, 1.0f));
            
            // Add current drag status if in editor mode
            string touchpadLabel = isMouseDragging ? "DRAGGING - Move mouse to control" : "Click and drag (can drag outside)";
            
            // Touchpad needs a fixed size
            GUILayout.Box(touchpadLabel, touchpadStyle, GUILayout.Width(300), GUILayout.Height(200));
            
            // Update touchpad rect after layout to get the actual position
            if (Event.current.type == EventType.Repaint)
            {
                Rect lastRect = GUILayoutUtility.GetLastRect();
                touchpadRect = new Rect(lastRect.x, lastRect.y, lastRect.width, lastRect.height);
            }
            
            // Sensitivity slider
            GUILayout.BeginHorizontal();
            GUILayout.Label("Sensitivity:", GUILayout.Width(80));
            mouseScale = GUILayout.HorizontalSlider(mouseScale, 0.5f, 10.0f, GUILayout.Height(30));
            GUILayout.Label(mouseScale.ToString("F1"), GUILayout.Width(40));
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            // Mouse button controls
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Mouse Buttons", subtitleStyle);
            
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Left Click", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Left click pressed");
                }
                else if (bleHidManager != null)
                {
                    bleHidManager.ClickMouseButton(BleHidConstants.BUTTON_LEFT);
                    logger.Log("Sent left click command");
                }
            }
            
            if (GUILayout.Button("Middle Click", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Middle click pressed");
                }
                else if (bleHidManager != null)
                {
                    bleHidManager.ClickMouseButton(BleHidConstants.BUTTON_MIDDLE);
                    logger.Log("Sent middle click command");
                }
            }
            
            if (GUILayout.Button("Right Click", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Right click pressed");
                }
                else if (bleHidManager != null)
                {
                    bleHidManager.ClickMouseButton(BleHidConstants.BUTTON_RIGHT);
                    logger.Log("Sent right click command");
                }
            }
            GUILayout.EndHorizontal();
            
            // Additional buttons like double click, drag, etc.
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Double Click", GUILayout.Height(50)))
            {
                if (isEditorMode)
                {
                    logger.Log("Double click pressed");
                }
                else if (bleHidManager != null)
                {
                    // Simulate double click
                    bleHidManager.ClickMouseButton(BleHidConstants.BUTTON_LEFT);
                    bleHidManager.ClickMouseButton(BleHidConstants.BUTTON_LEFT);
                    logger.Log("Sent double click command");
                }
            }
            
            if (GUILayout.Button("Scroll Up", GUILayout.Height(50)))
            {
                if (isEditorMode)
                {
                    logger.Log("Scroll up pressed");
                }
                else if (bleHidManager != null)
                {
                    // Scroll functionality would require additional implementation
                    logger.Log("Scroll not implemented yet");
                }
            }
            
            if (GUILayout.Button("Scroll Down", GUILayout.Height(50)))
            {
                if (isEditorMode)
                {
                    logger.Log("Scroll down pressed");
                }
                else if (bleHidManager != null)
                {
                    // Scroll functionality would require additional implementation
                    logger.Log("Scroll not implemented yet");
                }
            }
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            // Manual control (arrow keys for precise movement)
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Arrow Controls", subtitleStyle);
            
            // Up button
            GUILayout.BeginHorizontal();
            GUILayout.FlexibleSpace();
            if (GUILayout.Button("▲", GUILayout.Height(60), GUILayout.Width(100)))
            {
                SendMouseMovement(0, -10);
            }
            GUILayout.FlexibleSpace();
            GUILayout.EndHorizontal();
            
            // Left, Down, Right buttons
            GUILayout.BeginHorizontal();
            GUILayout.FlexibleSpace();
            if (GUILayout.Button("◄", GUILayout.Height(60), GUILayout.Width(100)))
            {
                SendMouseMovement(-10, 0);
            }
            if (GUILayout.Button("▼", GUILayout.Height(60), GUILayout.Width(100)))
            {
                SendMouseMovement(0, 10);
            }
            if (GUILayout.Button("►", GUILayout.Height(60), GUILayout.Width(100)))
            {
                SendMouseMovement(10, 0);
            }
            GUILayout.FlexibleSpace();
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            // Process touchpad input during OnGUI
            ProcessTouchpadInput();
        }
        
        /// <summary>
        /// Process input events for the touchpad.
        /// </summary>
        private void ProcessTouchpadInput()
        {
            // Handle mouse input for the touchpad in OnGUI
            Event currentEvent = Event.current;
            
            // Convert GUI coordinates to screen coordinates
            Rect screenTouchpadRect = new Rect(
                touchpadRect.x,
                touchpadRect.y,
                touchpadRect.width,
                touchpadRect.height
            );
            
            Vector2 mousePos = currentEvent.mousePosition;
            
            switch (currentEvent.type)
            {
                case EventType.MouseDown:
                    if (screenTouchpadRect.Contains(mousePos) && currentEvent.button == 0)
                    {
                        isMouseDragging = true;
                        lastTouchPosition = mousePos;
                        currentEvent.Use(); // Consume the event
                        logger.Log("Touchpad: Touch started");
                    }
                    break;
                
                case EventType.MouseDrag:
                    if (isMouseDragging)
                    {
                        // Handle the touch movement
                        Vector2 delta = mousePos - lastTouchPosition;
                        
                        // Scale the movement
                        int scaledDeltaX = (int)(delta.x * mouseScale);
                        int scaledDeltaY = (int)(delta.y * mouseScale);
                        
                        // Only process if movement is significant
                        if (Mathf.Abs(scaledDeltaX) > 0 || Mathf.Abs(scaledDeltaY) > 0)
                        {
                            // Invert Y direction for mouse movement (screen Y goes down, mouse Y goes up)
                            SendMouseMovement(scaledDeltaX, -scaledDeltaY);
                            
                            // Update last position
                            lastTouchPosition = mousePos;
                        }
                        
                        currentEvent.Use(); // Consume the event
                    }
                    break;
                
                case EventType.MouseUp:
                    if (isMouseDragging)
                    {
                        isMouseDragging = false;
                        logger.Log("Touchpad: Touch ended");
                        currentEvent.Use(); // Consume the event
                    }
                    break;
            }
        }
        
        /// <summary>
        /// Send a mouse movement command.
        /// </summary>
        private void SendMouseMovement(int deltaX, int deltaY)
        {
            #if UNITY_EDITOR
            logger.Log($"Mouse delta: ({deltaX}, {deltaY})");
            #else
            if (bleHidManager != null)
            {
                // Clamp values to valid range (-127 to 127)
                deltaX = Mathf.Clamp(deltaX, -127, 127);
                deltaY = Mathf.Clamp(deltaY, -127, 127);
                
                bleHidManager.MoveMouse(deltaX, deltaY);
                logger.Log($"Sent mouse movement: ({deltaX}, {deltaY})");
            }
            #endif
        }
        
        public override void Update()
        {
            base.Update();
            
            // Additional update logic if needed
        }
    }
}
