using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

namespace BleHid
{
    public partial class BleHidBootstrapper
    {
        #region UI References
        private Canvas mainCanvas;
        private GameObject tabButtonPanel;
        private GameObject mediaPanel;
        private GameObject mousePanel;
        private GameObject keyboardPanel;
        private GameObject statusPanel;
        private Button advertisingButton;
        private TextMeshProUGUI statusText;
        private TextMeshProUGUI connectionText;
        private TextMeshProUGUI deviceInfoText;
        private TextMeshProUGUI logText;
        private ScrollRect logScrollRect;
        
        // Tab buttons
        private Button mediaTabButton;
        private Button mouseTabButton;
        private Button keyboardTabButton;
        #endregion

        #region UI Creation
        private void CreateUserInterface()
        {
            // Check if UI is already created
            if (uiInitialized) return;
            
            // Create canvas
            GameObject canvasObj = new GameObject("BleHidCanvas");
            mainCanvas = canvasObj.AddComponent<Canvas>();
            mainCanvas.renderMode = RenderMode.ScreenSpaceOverlay;
            
            // Add canvas scaler
            CanvasScaler scaler = canvasObj.AddComponent<CanvasScaler>();
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            scaler.referenceResolution = new Vector2(1080, 1920);
            scaler.matchWidthOrHeight = 0.5f;
            
            // Add graphic raycaster for button interactions
            canvasObj.AddComponent<GraphicRaycaster>();
            
            // Create main panel that fills the screen
            GameObject mainPanel = CreatePanel(mainCanvas.transform, "MainPanel", Vector2.zero, 
                new Vector2(1080, 1920), new Vector2(0.5f, 0.5f));
            
            // Create tab button panel at the top
            tabButtonPanel = CreatePanel(mainPanel.transform, "TabButtonPanel", new Vector2(0, 860), 
                new Vector2(1080, 200), new Vector2(0.5f, 1f));
            
            // Create tab buttons
            float tabWidth = 360;
            float tabHeight = 120;
            
            mediaTabButton = CreateButton(tabButtonPanel.transform, "MediaTabButton", new Vector2(-tabWidth, 0), 
                new Vector2(tabWidth, tabHeight), "Media", OnMediaTabClicked);
            
            mouseTabButton = CreateButton(tabButtonPanel.transform, "MouseTabButton", Vector2.zero, 
                new Vector2(tabWidth, tabHeight), "Mouse", OnMouseTabClicked);
            
            keyboardTabButton = CreateButton(tabButtonPanel.transform, "KeyboardTabButton", new Vector2(tabWidth, 0), 
                new Vector2(tabWidth, tabHeight), "Keyboard", OnKeyboardTabClicked);
            
            // Create content panels
            mediaPanel = CreatePanel(mainPanel.transform, "MediaPanel", Vector2.zero, 
                new Vector2(1080, 1000), new Vector2(0.5f, 0.5f));
            
            mousePanel = CreatePanel(mainPanel.transform, "MousePanel", Vector2.zero, 
                new Vector2(1080, 1000), new Vector2(0.5f, 0.5f));
            mousePanel.SetActive(false);
            
            keyboardPanel = CreatePanel(mainPanel.transform, "KeyboardPanel", Vector2.zero, 
                new Vector2(1080, 1000), new Vector2(0.5f, 0.5f));
            keyboardPanel.SetActive(false);
            
            // Create status panel at the bottom
            statusPanel = CreatePanel(mainPanel.transform, "StatusPanel", new Vector2(0, -860), 
                new Vector2(1080, 200), new Vector2(0.5f, 0f));
            
            // Create status controls
            advertisingButton = CreateButton(statusPanel.transform, "AdvertisingButton", new Vector2(0, 60), 
                new Vector2(500, 80), "Start Advertising", OnAdvertisingButtonClicked);
            
            statusText = CreateText(statusPanel.transform, "StatusText", new Vector2(0, 0), 
                new Vector2(1000, 40), "Status: Initializing...");
            
            connectionText = CreateText(statusPanel.transform, "ConnectionText", new Vector2(0, -40), 
                new Vector2(1000, 40), "Not connected");
            
            deviceInfoText = CreateText(statusPanel.transform, "DeviceInfoText", new Vector2(0, -80), 
                new Vector2(1000, 40), "Device info: N/A");
            
            // Create log panel
            GameObject logPanelObj = CreatePanel(mainPanel.transform, "LogPanel", new Vector2(0, -620), 
                new Vector2(1000, 300), new Vector2(0.5f, 0f));
            
            // Create log scroll view
            GameObject scrollViewObj = new GameObject("LogScrollView", typeof(RectTransform));
            scrollViewObj.transform.SetParent(logPanelObj.transform, false);
            logScrollRect = scrollViewObj.AddComponent<ScrollRect>();
            RectTransform scrollRectTransform = scrollViewObj.GetComponent<RectTransform>();
            scrollRectTransform.anchoredPosition = Vector2.zero;
            scrollRectTransform.sizeDelta = new Vector2(980, 280);
            scrollRectTransform.anchorMin = new Vector2(0.5f, 0.5f);
            scrollRectTransform.anchorMax = new Vector2(0.5f, 0.5f);
            
            // Create viewport
            GameObject viewportObj = new GameObject("Viewport", typeof(RectTransform));
            viewportObj.transform.SetParent(scrollViewObj.transform, false);
            RectTransform viewportRectTransform = viewportObj.GetComponent<RectTransform>();
            viewportRectTransform.anchoredPosition = Vector2.zero;
            viewportRectTransform.sizeDelta = new Vector2(980, 280);
            viewportRectTransform.anchorMin = Vector2.zero;
            viewportRectTransform.anchorMax = Vector2.one;
            viewportRectTransform.pivot = new Vector2(0.5f, 0.5f);
            
            // Add mask
            viewportObj.AddComponent<RectMask2D>();
            
            // Create content
            GameObject contentObj = new GameObject("Content", typeof(RectTransform));
            contentObj.transform.SetParent(viewportObj.transform, false);
            RectTransform contentRectTransform = contentObj.GetComponent<RectTransform>();
            contentRectTransform.anchoredPosition = Vector2.zero;
            contentRectTransform.sizeDelta = new Vector2(980, 280);
            contentRectTransform.anchorMin = new Vector2(0, 1);
            contentRectTransform.anchorMax = new Vector2(1, 1);
            contentRectTransform.pivot = new Vector2(0.5f, 1);
            
            // Create log text
            logText = CreateText(contentObj.transform, "LogText", Vector2.zero, 
                new Vector2(980, 280), "Log output will appear here.");
            logText.alignment = TextAlignmentOptions.TopLeft;
            logText.enableWordWrapping = true;
            
            // Configure scroll view
            logScrollRect.content = contentRectTransform;
            logScrollRect.viewport = viewportRectTransform;
            logScrollRect.horizontal = false;
            logScrollRect.vertical = true;
            logScrollRect.scrollSensitivity = 15;
            logScrollRect.movementType = ScrollRect.MovementType.Elastic;
            logScrollRect.elasticity = 0.1f;
            
            // Create UI for each panel
            CreateMediaPanelUI();
            CreateMousePanelUI();
            CreateKeyboardPanelUI();
            
            // Set initial tab to Media
            OnMediaTabClicked();
            
            uiInitialized = true;
        }

        #region Tab UI Handlers
        private void OnMediaTabClicked()
        {
            // Show media panel, hide others
            mediaPanel.SetActive(true);
            mousePanel.SetActive(false);
            keyboardPanel.SetActive(false);
            
            // Update button colors
            ColorBlock mediaColors = mediaTabButton.colors;
            mediaColors.normalColor = new Color(0.2f, 0.4f, 0.8f, 1);
            mediaTabButton.colors = mediaColors;
            
            ColorBlock mouseColors = mouseTabButton.colors;
            mouseColors.normalColor = new Color(0.2f, 0.2f, 0.2f, 1);
            mouseTabButton.colors = mouseColors;
            
            ColorBlock keyboardColors = keyboardTabButton.colors;
            keyboardColors.normalColor = new Color(0.2f, 0.2f, 0.2f, 1);
            keyboardTabButton.colors = keyboardColors;
        }

        private void OnMouseTabClicked()
        {
            // Show mouse panel, hide others
            mediaPanel.SetActive(false);
            mousePanel.SetActive(true);
            keyboardPanel.SetActive(false);
            
            // Update button colors
            ColorBlock mediaColors = mediaTabButton.colors;
            mediaColors.normalColor = new Color(0.2f, 0.2f, 0.2f, 1);
            mediaTabButton.colors = mediaColors;
            
            ColorBlock mouseColors = mouseTabButton.colors;
            mouseColors.normalColor = new Color(0.2f, 0.4f, 0.8f, 1);
            mouseTabButton.colors = mouseColors;
            
            ColorBlock keyboardColors = keyboardTabButton.colors;
            keyboardColors.normalColor = new Color(0.2f, 0.2f, 0.2f, 1);
            keyboardTabButton.colors = keyboardColors;
        }

        private void OnKeyboardTabClicked()
        {
            // Show keyboard panel, hide others
            mediaPanel.SetActive(false);
            mousePanel.SetActive(false);
            keyboardPanel.SetActive(true);
            
            // Update button colors
            ColorBlock mediaColors = mediaTabButton.colors;
            mediaColors.normalColor = new Color(0.2f, 0.2f, 0.2f, 1);
            mediaTabButton.colors = mediaColors;
            
            ColorBlock mouseColors = mouseTabButton.colors;
            mouseColors.normalColor = new Color(0.2f, 0.2f, 0.2f, 1);
            mouseTabButton.colors = mouseColors;
            
            ColorBlock keyboardColors = keyboardTabButton.colors;
            keyboardColors.normalColor = new Color(0.2f, 0.4f, 0.8f, 1);
            keyboardTabButton.colors = keyboardColors;
        }
        #endregion
        #endregion
    }
}
