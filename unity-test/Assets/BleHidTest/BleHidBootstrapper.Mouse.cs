using System;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using TMPro;

namespace BleHid
{
    public partial class BleHidBootstrapper
    {
        // Mouse control elements
        private RectTransform touchpadArea;
        private Button leftButton;
        private Button middleButton;
        private Button rightButton;
        
        // Mouse tracking fields
        private Vector2 lastTouchPosition;
        private bool isMouseDragging = false;

        private void CreateMousePanelUI()
        {
            // Create touchpad area
            GameObject touchpadObj = CreatePanel(mousePanel.transform, "TouchpadArea", new Vector2(0, 100), 
                new Vector2(800, 500), new Vector2(0.5f, 0.5f));
            
            touchpadArea = touchpadObj.GetComponent<RectTransform>();
            
            // Add an image to make the touchpad visible
            Image touchpadImage = touchpadObj.AddComponent<Image>();
            touchpadImage.color = new Color(0.2f, 0.2f, 0.2f, 0.5f);
            
            // Add touchpad event trigger
            EventTrigger trigger = touchpadObj.AddComponent<EventTrigger>();
            
            // Add pointer down event
            EventTrigger.Entry pointerDownEntry = new EventTrigger.Entry();
            pointerDownEntry.eventID = EventTriggerType.PointerDown;
            pointerDownEntry.callback.AddListener((data) => { OnTouchpadPointerDown((PointerEventData)data); });
            trigger.triggers.Add(pointerDownEntry);
            
            // Add drag event
            EventTrigger.Entry dragEntry = new EventTrigger.Entry();
            dragEntry.eventID = EventTriggerType.Drag;
            dragEntry.callback.AddListener((data) => { OnTouchpadDrag((PointerEventData)data); });
            trigger.triggers.Add(dragEntry);
            
            // Add pointer up event
            EventTrigger.Entry pointerUpEntry = new EventTrigger.Entry();
            pointerUpEntry.eventID = EventTriggerType.PointerUp;
            pointerUpEntry.callback.AddListener((data) => { OnTouchpadPointerUp((PointerEventData)data); });
            trigger.triggers.Add(pointerUpEntry);
            
            // Create mouse buttons
            float buttonWidth = 200;
            float buttonHeight = 100;
            float buttonSpacing = 20;
            
            leftButton = CreateButton(mousePanel.transform, "LeftButton", new Vector2(-buttonWidth - buttonSpacing, -200), 
                new Vector2(buttonWidth, buttonHeight), "Left Click", OnLeftMouseClicked);
            
            middleButton = CreateButton(mousePanel.transform, "MiddleButton", new Vector2(0, -200), 
                new Vector2(buttonWidth, buttonHeight), "Middle Click", OnMiddleMouseClicked);
            
            rightButton = CreateButton(mousePanel.transform, "RightButton", new Vector2(buttonWidth + buttonSpacing, -200), 
                new Vector2(buttonWidth, buttonHeight), "Right Click", OnRightMouseClicked);
            
            // Add label above touchpad
            TextMeshProUGUI touchpadLabel = CreateText(mousePanel.transform, "TouchpadLabel", new Vector2(0, 400), 
                new Vector2(800, 50), "Touch & Drag to Move Mouse");
        }
        
        // Mouse button handlers
        private void OnLeftMouseClicked()
        {
            if (!bleHidManager.IsConnected)
            {
                AddLogEntry("Cannot send left click: No device connected");
                return;
            }
            
            bleHidManager.ClickMouseButton(BleHidConstants.BUTTON_LEFT);
        }

        private void OnMiddleMouseClicked()
        {
            if (!bleHidManager.IsConnected)
            {
                AddLogEntry("Cannot send middle click: No device connected");
                return;
            }
            
            bleHidManager.ClickMouseButton(BleHidConstants.BUTTON_MIDDLE);
        }

        private void OnRightMouseClicked()
        {
            if (!bleHidManager.IsConnected)
            {
                AddLogEntry("Cannot send right click: No device connected");
                return;
            }
            
            bleHidManager.ClickMouseButton(BleHidConstants.BUTTON_RIGHT);
        }

        // Touchpad event handlers
        private void OnTouchpadPointerDown(PointerEventData data)
        {
            lastTouchPosition = data.position;
            isMouseDragging = true;
        }

        private void OnTouchpadDrag(PointerEventData data)
        {
            if (!isMouseDragging) return;
            
            if (!bleHidManager.IsConnected)
            {
                AddLogEntry("Cannot send mouse movement: No device connected");
                isMouseDragging = false;
                return;
            }
            
            // Calculate delta movement
            Vector2 currentPosition = data.position;
            Vector2 delta = currentPosition - lastTouchPosition;
            
            // Scale the movement (adjust sensitivity as needed)
            int scaledDeltaX = (int)(delta.x * 0.5f);
            int scaledDeltaY = (int)(delta.y * 0.5f);
            
            // Only send if there's significant movement
            if (Mathf.Abs(scaledDeltaX) > 0 || Mathf.Abs(scaledDeltaY) > 0)
            {
                // Send the mouse movement
                bleHidManager.MoveMouse(scaledDeltaX, scaledDeltaY);
                
                // Update last position
                lastTouchPosition = currentPosition;
            }
        }

        private void OnTouchpadPointerUp(PointerEventData data)
        {
            isMouseDragging = false;
        }
    }
}
