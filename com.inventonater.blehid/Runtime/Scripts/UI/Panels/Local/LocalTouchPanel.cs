using System;
using UnityEngine;

namespace Inventonater.BleHid.UI.Panels.Local
{
    /// <summary>
    /// Panel for local touch input control functionality.
    /// Provides a virtual touchpad and swipe actions.
    /// </summary>
    public class LocalTouchPanel : BaseBleHidPanel
    {
        private BleHidLocalControl localControl;
        
        // Virtual touchpad state
        private Rect touchpadRect;
        private Vector2 lastTouchPosition;
        private bool isMouseDragging = false;
        private float touchScale =.75f;
        
        // Gesture types
        private string[] gestureTypes = new string[] { "Tap", "Swipe", "Long Press", "Pinch" };
        private int selectedGestureType = 0;
        
        public override bool RequiresConnectedDevice => false;
        
        public override void Initialize(BleHidManager manager)
        {
            base.Initialize(manager);
            
            // Get reference to the local control instance
            try
            {
                localControl = BleHidLocalControl.Instance;
            }
            catch (Exception e)
            {
                logger.LogError($"Failed to get LocalControl instance: {e.Message}");
            }
            
            // Initialize touchpad area (will be adjusted in DrawPanelContent)
            touchpadRect = new Rect(0, 0, 300, 200);
        }
        
        protected override void DrawPanelContent()
        {
            GUILayout.Label("Touch Controls", titleStyle);
            
            #if UNITY_EDITOR
            bool isEditorMode = true;
            #else
            bool isEditorMode = false;
            #endif
            
            // Precision touchpad
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Virtual Touchpad", subtitleStyle);
            
            GUILayout.Label("Touch and drag in the area below to control cursor");
            
            // Draw touchpad area with visual style
            GUIStyle touchpadStyle = new GUIStyle(GUI.skin.box);
            touchpadStyle.normal.background = MakeColorTexture(new Color(0.2f, 0.2f, 0.2f, 1.0f));
            
            string touchpadLabel = isMouseDragging ? "DRAGGING" : "Tap and drag here";
            
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
            touchScale = GUILayout.HorizontalSlider(touchScale, 0.1f, 2.0f, GUILayout.Height(30));
            GUILayout.Label(touchScale.ToString("F2"), GUILayout.Width(40));
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            // Gesture controls
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Gesture Controls", subtitleStyle);
            
            // Gesture type selection
            GUILayout.BeginHorizontal();
            GUILayout.Label("Gesture Type:", GUILayout.Width(100));
            selectedGestureType = GUILayout.Toolbar(selectedGestureType, gestureTypes, GUILayout.Height(40));
            GUILayout.EndHorizontal();
            
            // Draw controls for the selected gesture type
            DrawGestureControls(selectedGestureType);
            
            GUILayout.EndVertical();
            
            // Process touchpad input during OnGUI
            ProcessTouchpadInput();
        }
        
        /// <summary>
        /// Draw controls for the selected gesture type.
        /// </summary>
        private void DrawGestureControls(int gestureType)
        {
            switch (gestureType)
            {
                case 0: // Tap
                    DrawTapControls();
                    break;
                case 1: // Swipe
                    DrawSwipeControls();
                    break;
                case 2: // Long Press
                    DrawLongPressControls();
                    break;
                case 3: // Pinch
                    DrawPinchControls();
                    break;
            }
        }
        
        private void DrawTapControls()
        {
            GUILayout.Label("Tap Controls", GUILayout.Height(30));
            
            // Single tap actions
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Single Tap", GUILayout.Height(50)))
            {
                logger.Log("Single tap at center (not implemented)");
            }
            
            if (GUILayout.Button("Double Tap", GUILayout.Height(50)))
            {
                logger.Log("Double tap at center (not implemented)");
            }
            
            if (GUILayout.Button("Triple Tap", GUILayout.Height(50)))
            {
                logger.Log("Triple tap at center (not implemented)");
            }
            GUILayout.EndHorizontal();
            
            // Multi-touch tap actions
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Two-Finger Tap", GUILayout.Height(50)))
            {
                logger.Log("Two-finger tap (not implemented)");
            }
            
            if (GUILayout.Button("Three-Finger Tap", GUILayout.Height(50)))
            {
                logger.Log("Three-finger tap (not implemented)");
            }
            GUILayout.EndHorizontal();
        }
        
        private void DrawSwipeControls()
        {
            GUILayout.Label("Swipe Controls", GUILayout.Height(30));
            
            // Direction buttons - Row 1
            GUILayout.BeginHorizontal();
            GUILayout.FlexibleSpace();
            if (GUILayout.Button("↑ Swipe Up", GUILayout.Height(50), GUILayout.Width(180)))
            {
                logger.Log("Swipe up (not implemented)");
            }
            GUILayout.FlexibleSpace();
            GUILayout.EndHorizontal();
            
            // Direction buttons - Row 2
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("← Swipe Left", GUILayout.Height(50)))
            {
                logger.Log("Swipe left (not implemented)");
            }
            
            if (GUILayout.Button("→ Swipe Right", GUILayout.Height(50)))
            {
                logger.Log("Swipe right (not implemented)");
            }
            GUILayout.EndHorizontal();
            
            // Direction buttons - Row 3
            GUILayout.BeginHorizontal();
            GUILayout.FlexibleSpace();
            if (GUILayout.Button("↓ Swipe Down", GUILayout.Height(50), GUILayout.Width(180)))
            {
                logger.Log("Swipe down (not implemented)");
            }
            GUILayout.FlexibleSpace();
            GUILayout.EndHorizontal();
            
            // Multi-finger swipes
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Two-Finger Swipe", GUILayout.Height(50)))
            {
                logger.Log("Two-finger swipe (not implemented)");
            }
            
            if (GUILayout.Button("Three-Finger Swipe", GUILayout.Height(50)))
            {
                logger.Log("Three-finger swipe (not implemented)");
            }
            GUILayout.EndHorizontal();
        }
        
        private void DrawLongPressControls()
        {
            GUILayout.Label("Long Press Controls", GUILayout.Height(30));
            
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Long Press", GUILayout.Height(50)))
            {
                logger.Log("Long press at center (not implemented)");
            }
            
            if (GUILayout.Button("Long Press + Drag", GUILayout.Height(50)))
            {
                logger.Log("Long press and drag (not implemented)");
            }
            GUILayout.EndHorizontal();
            
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Select Text", GUILayout.Height(50)))
            {
                logger.Log("Text selection mode (not implemented)");
            }
            
            if (GUILayout.Button("Context Menu", GUILayout.Height(50)))
            {
                logger.Log("Show context menu (not implemented)");
            }
            GUILayout.EndHorizontal();
        }
        
        private void DrawPinchControls()
        {
            GUILayout.Label("Pinch Controls", GUILayout.Height(30));
            
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Pinch In (Zoom Out)", GUILayout.Height(50)))
            {
                logger.Log("Pinch in zoom (not implemented)");
            }
            
            if (GUILayout.Button("Pinch Out (Zoom In)", GUILayout.Height(50)))
            {
                logger.Log("Pinch out zoom (not implemented)");
            }
            GUILayout.EndHorizontal();
            
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Rotate Left", GUILayout.Height(50)))
            {
                logger.Log("Rotate gesture left (not implemented)");
            }
            
            if (GUILayout.Button("Rotate Right", GUILayout.Height(50)))
            {
                logger.Log("Rotate gesture right (not implemented)");
            }
            GUILayout.EndHorizontal();
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
                        int scaledDeltaX = (int)(delta.x * touchScale);
                        int scaledDeltaY = (int)(delta.y * touchScale);
                        
                        // Only process if movement is significant
                        if (Mathf.Abs(scaledDeltaX) > 0 || Mathf.Abs(scaledDeltaY) > 0)
                        {
                            #if !UNITY_EDITOR
                            if (localControl != null)
                            {
                                // This would be implemented in a future update
                                // localControl.MoveTouchPosition(scaledDeltaX, scaledDeltaY);
                            }
                            #endif
                            
                            logger.Log($"Touchpad: Move ({scaledDeltaX}, {scaledDeltaY}) (not implemented)");
                            
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
        
        public override void Update()
        {
            base.Update();
            
            // Additional update logic if needed
        }
    }
}
