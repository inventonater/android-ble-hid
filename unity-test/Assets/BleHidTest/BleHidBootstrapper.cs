using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using TMPro;

namespace BleHid
{
    /// <summary>
    /// Bootstrapper for Android BLE HID functionality.
    /// This script automatically creates all the necessary UI components
    /// to operate the BLE HID device functionality.
    /// </summary>
    public partial class BleHidBootstrapper : MonoBehaviour
    {
        #region Private Fields
        private BleHidManager bleHidManager;
        private bool uiInitialized = false;
        #endregion

        #region Unity Lifecycle
        private void Awake()
        {
            // Create and configure BleHidManager
            GameObject managerObj = new GameObject("BleHidManager");
            bleHidManager = managerObj.AddComponent<BleHidManager>();
            
            // Register for events
            bleHidManager.OnInitializeComplete += OnInitializeComplete;
            bleHidManager.OnAdvertisingStateChanged += OnAdvertisingStateChanged;
            bleHidManager.OnConnectionStateChanged += OnConnectionStateChanged;
            bleHidManager.OnPairingStateChanged += OnPairingStateChanged;
            bleHidManager.OnError += OnError;
            bleHidManager.OnDebugLog += OnDebugLog;
        }

        private void Start()
        {
            // Create UI
            CreateUserInterface();
            
            // Initialize BLE HID
            StartCoroutine(bleHidManager.Initialize());
        }

        private void Update()
        {
            if (advertisingButton != null)
            {
                // Update button text based on advertising state
                advertisingButton.GetComponentInChildren<TextMeshProUGUI>().text = 
                    bleHidManager.IsAdvertising ? "Stop Advertising" : "Start Advertising";
            }
        }

        private void OnDestroy()
        {
            // Unregister events
            if (bleHidManager != null)
            {
                bleHidManager.OnInitializeComplete -= OnInitializeComplete;
                bleHidManager.OnAdvertisingStateChanged -= OnAdvertisingStateChanged;
                bleHidManager.OnConnectionStateChanged -= OnConnectionStateChanged;
                bleHidManager.OnPairingStateChanged -= OnPairingStateChanged;
                bleHidManager.OnError -= OnError;
                bleHidManager.OnDebugLog -= OnDebugLog;
            }
        }
        #endregion

        #region Core UI Functions
        // Helper method to create a panel
        private GameObject CreatePanel(Transform parent, string name, Vector2 position, Vector2 size, Vector2 pivot)
        {
            GameObject panel = new GameObject(name, typeof(RectTransform));
            panel.transform.SetParent(parent, false);
            
            RectTransform rectTransform = panel.GetComponent<RectTransform>();
            rectTransform.anchoredPosition = position;
            rectTransform.sizeDelta = size;
            rectTransform.anchorMin = new Vector2(0.5f, 0.5f);
            rectTransform.anchorMax = new Vector2(0.5f, 0.5f);
            rectTransform.pivot = pivot;
            
            return panel;
        }

        // Helper method to create a button
        private Button CreateButton(Transform parent, string name, Vector2 position, Vector2 size, string text, UnityEngine.Events.UnityAction onClick)
        {
            GameObject buttonObj = new GameObject(name, typeof(RectTransform));
            buttonObj.transform.SetParent(parent, false);
            
            RectTransform rectTransform = buttonObj.GetComponent<RectTransform>();
            rectTransform.anchoredPosition = position;
            rectTransform.sizeDelta = size;
            rectTransform.anchorMin = new Vector2(0.5f, 0.5f);
            rectTransform.anchorMax = new Vector2(0.5f, 0.5f);
            
            // Add image (background)
            Image image = buttonObj.AddComponent<Image>();
            image.color = new Color(0.2f, 0.2f, 0.2f, 1);
            
            // Add button component
            Button button = buttonObj.AddComponent<Button>();
            button.targetGraphic = image;
            
            // Set normal color
            ColorBlock colors = button.colors;
            colors.normalColor = new Color(0.2f, 0.2f, 0.2f, 1);
            colors.highlightedColor = new Color(0.3f, 0.3f, 0.3f, 1);
            colors.pressedColor = new Color(0.1f, 0.1f, 0.1f, 1);
            colors.selectedColor = new Color(0.3f, 0.3f, 0.3f, 1);
            colors.disabledColor = new Color(0.2f, 0.2f, 0.2f, 0.5f);
            button.colors = colors;
            
            // Add text
            GameObject textObj = new GameObject("Text", typeof(RectTransform));
            textObj.transform.SetParent(buttonObj.transform, false);
            
            RectTransform textRectTransform = textObj.GetComponent<RectTransform>();
            textRectTransform.anchoredPosition = Vector2.zero;
            textRectTransform.sizeDelta = new Vector2(size.x - 20, size.y - 20);
            textRectTransform.anchorMin = Vector2.zero;
            textRectTransform.anchorMax = Vector2.one;
            
            TextMeshProUGUI buttonText = textObj.AddComponent<TextMeshProUGUI>();
            buttonText.text = text;
            buttonText.fontSize = 24;
            buttonText.alignment = TextAlignmentOptions.Center;
            buttonText.color = Color.white;
            
            // Add click handler
            if (onClick != null)
            {
                button.onClick.AddListener(onClick);
            }
            
            return button;
        }

        // Helper method to create text
        private TextMeshProUGUI CreateText(Transform parent, string name, Vector2 position, Vector2 size, string text)
        {
            GameObject textObj = new GameObject(name, typeof(RectTransform));
            textObj.transform.SetParent(parent, false);
            
            RectTransform rectTransform = textObj.GetComponent<RectTransform>();
            rectTransform.anchoredPosition = position;
            rectTransform.sizeDelta = size;
            rectTransform.anchorMin = new Vector2(0.5f, 0.5f);
            rectTransform.anchorMax = new Vector2(0.5f, 0.5f);
            
            TextMeshProUGUI textComponent = textObj.AddComponent<TextMeshProUGUI>();
            textComponent.text = text;
            textComponent.fontSize = 24;
            textComponent.alignment = TextAlignmentOptions.Center;
            textComponent.color = Color.white;
            
            return textComponent;
        }
        
        private void OnAdvertisingButtonClicked()
        {
            if (bleHidManager.IsAdvertising)
            {
                bleHidManager.StopAdvertising();
            }
            else
            {
                bleHidManager.StartAdvertising();
            }
        }
        #endregion
    }
}
