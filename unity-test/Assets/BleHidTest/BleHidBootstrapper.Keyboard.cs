using System;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

namespace BleHid
{
    public partial class BleHidBootstrapper
    {
        // Keyboard UI elements
        private Dictionary<string, Button> keyButtons = new Dictionary<string, Button>();
        private Button sendTextButton;
        private TMP_InputField textInputField;
        
        private void CreateKeyboardPanelUI()
        {
            // Create text input field
            GameObject inputFieldObj = new GameObject("TextInputField", typeof(RectTransform));
            inputFieldObj.transform.SetParent(keyboardPanel.transform, false);
            RectTransform inputRect = inputFieldObj.GetComponent<RectTransform>();
            inputRect.anchoredPosition = new Vector2(0, 300);
            inputRect.sizeDelta = new Vector2(800, 80);
            inputRect.anchorMin = new Vector2(0.5f, 0.5f);
            inputRect.anchorMax = new Vector2(0.5f, 0.5f);
            
            textInputField = inputFieldObj.AddComponent<TMP_InputField>();
            
            // Create input field visuals
            GameObject textAreaObj = new GameObject("TextArea", typeof(RectTransform));
            textAreaObj.transform.SetParent(inputFieldObj.transform, false);
            RectTransform textAreaRect = textAreaObj.GetComponent<RectTransform>();
            textAreaRect.anchoredPosition = Vector2.zero;
            textAreaRect.sizeDelta = new Vector2(800, 80);
            textAreaRect.anchorMin = Vector2.zero;
            textAreaRect.anchorMax = Vector2.one;
            
            // Add background image
            Image bgImage = textAreaObj.AddComponent<Image>();
            bgImage.color = new Color(0.2f, 0.2f, 0.2f, 1);
            
            // Create placeholder
            GameObject placeholderObj = new GameObject("Placeholder", typeof(RectTransform));
            placeholderObj.transform.SetParent(textAreaObj.transform, false);
            RectTransform placeholderRect = placeholderObj.GetComponent<RectTransform>();
            placeholderRect.anchoredPosition = new Vector2(10, 0);
            placeholderRect.sizeDelta = new Vector2(-20, 0);
            placeholderRect.anchorMin = Vector2.zero;
            placeholderRect.anchorMax = Vector2.one;
            
            TextMeshProUGUI placeholderText = placeholderObj.AddComponent<TextMeshProUGUI>();
            placeholderText.text = "Enter text to type...";
            placeholderText.fontSize = 30;
            placeholderText.color = new Color(0.7f, 0.7f, 0.7f, 1);
            placeholderText.alignment = TextAlignmentOptions.Left;
            placeholderText.verticalAlignment = VerticalAlignmentOptions.Middle;
            
            // Create input text
            GameObject textObj = new GameObject("Text", typeof(RectTransform));
            textObj.transform.SetParent(textAreaObj.transform, false);
            RectTransform textRect = textObj.GetComponent<RectTransform>();
            textRect.anchoredPosition = new Vector2(10, 0);
            textRect.sizeDelta = new Vector2(-20, 0);
            textRect.anchorMin = Vector2.zero;
            textRect.anchorMax = Vector2.one;
            
            TextMeshProUGUI inputText = textObj.AddComponent<TextMeshProUGUI>();
            inputText.fontSize = 30;
            inputText.color = Color.white;
            inputText.alignment = TextAlignmentOptions.Left;
            inputText.verticalAlignment = VerticalAlignmentOptions.Middle;
            
            // Configure input field
            textInputField.targetGraphic = bgImage;
            textInputField.textComponent = inputText;
            textInputField.placeholder = placeholderText;
            textInputField.text = "";
            textInputField.caretBlinkRate = 0.85f;
            textInputField.caretWidth = 2;
            textInputField.selectionColor = new Color(0.2f, 0.6f, 1f, 0.4f);
            
            // Create send button
            sendTextButton = CreateButton(keyboardPanel.transform, "SendTextButton", new Vector2(0, 200), 
                new Vector2(300, 80), "Send Text", OnSendTextClicked);
            
            // Create common keyboard keys
            CreateKeyboardKeys();
        }

        private void CreateKeyboardKeys()
        {
            float keySize = 80;
            float keySpacing = 10;
            float startY = 0;
            
            // Create first row (Q-P)
            string[] row1 = { "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P" };
            float row1X = -(keySize + keySpacing) * (row1.Length / 2) + keySize / 2;
            
            for (int i = 0; i < row1.Length; i++)
            {
                Vector2 pos = new Vector2(row1X + i * (keySize + keySpacing), startY);
                Button keyButton = CreateButton(keyboardPanel.transform, "Key" + row1[i], pos, 
                    new Vector2(keySize, keySize), row1[i], null);
                
                // Store the key and attach click handler
                keyButtons[row1[i]] = keyButton;
                int keyIndex = i;
                keyButton.onClick.AddListener(() => OnKeyClicked(row1[keyIndex]));
            }
            
            // Create second row (A-L)
            string[] row2 = { "A", "S", "D", "F", "G", "H", "J", "K", "L" };
            float row2X = -(keySize + keySpacing) * (row2.Length / 2) + keySize / 2;
            float row2Y = startY - (keySize + keySpacing);
            
            for (int i = 0; i < row2.Length; i++)
            {
                Vector2 pos = new Vector2(row2X + i * (keySize + keySpacing), row2Y);
                Button keyButton = CreateButton(keyboardPanel.transform, "Key" + row2[i], pos, 
                    new Vector2(keySize, keySize), row2[i], null);
                
                // Store the key and attach click handler
                keyButtons[row2[i]] = keyButton;
                int keyIndex = i;
                keyButton.onClick.AddListener(() => OnKeyClicked(row2[keyIndex]));
            }
            
            // Create third row (Z-M)
            string[] row3 = { "Z", "X", "C", "V", "B", "N", "M" };
            float row3X = -(keySize + keySpacing) * (row3.Length / 2) + keySize / 2;
            float row3Y = row2Y - (keySize + keySpacing);
            
            for (int i = 0; i < row3.Length; i++)
            {
                Vector2 pos = new Vector2(row3X + i * (keySize + keySpacing), row3Y);
                Button keyButton = CreateButton(keyboardPanel.transform, "Key" + row3[i], pos, 
                    new Vector2(keySize, keySize), row3[i], null);
                
                // Store the key and attach click handler
                keyButtons[row3[i]] = keyButton;
                int keyIndex = i;
                keyButton.onClick.AddListener(() => OnKeyClicked(row3[keyIndex]));
            }
            
            // Create space bar
            float row4Y = row3Y - (keySize + keySpacing);
            Button spaceButton = CreateButton(keyboardPanel.transform, "KeySpace", new Vector2(0, row4Y), 
                new Vector2(keySize * 6, keySize), "Space", null);
            keyButtons["Space"] = spaceButton;
            spaceButton.onClick.AddListener(() => OnKeyClicked("Space"));
            
            // Create Enter key
            float row4X = (keySize + keySpacing) * 4;
            Button enterButton = CreateButton(keyboardPanel.transform, "KeyEnter", new Vector2(row4X, row4Y), 
                new Vector2(keySize * 2, keySize), "Enter", null);
            keyButtons["Enter"] = enterButton;
            enterButton.onClick.AddListener(() => OnKeyClicked("Enter"));
            
            // Create Backspace key
            Button backspaceButton = CreateButton(keyboardPanel.transform, "KeyBackspace", new Vector2(-row4X, row4Y), 
                new Vector2(keySize * 2, keySize), "⌫", null);
            keyButtons["Backspace"] = backspaceButton;
            backspaceButton.onClick.AddListener(() => OnKeyClicked("Backspace"));
        }
        
        // Keyboard event handlers
        private void OnKeyClicked(string key)
        {
            if (!bleHidManager.IsConnected)
            {
                AddLogEntry("Cannot send key " + key + ": No device connected");
                return;
            }
            
            byte keyCode = GetKeyCode(key);
            if (keyCode > 0)
            {
                bleHidManager.SendKey(keyCode);
            }
        }

        private void OnSendTextClicked()
        {
            if (!bleHidManager.IsConnected)
            {
                AddLogEntry("Cannot send text: No device connected");
                return;
            }
            
            string text = textInputField.text;
            if (string.IsNullOrEmpty(text))
            {
                AddLogEntry("Cannot send empty text");
                return;
            }
            
            bleHidManager.TypeText(text);
            
            // Clear the text field
            textInputField.text = "";
        }

        // Helper method to get key code for a key name
        private byte GetKeyCode(string key)
        {
            switch (key)
            {
                case "A": return BleHidConstants.KEY_A;
                case "B": return BleHidConstants.KEY_B;
                case "C": return BleHidConstants.KEY_C;
                case "D": return BleHidConstants.KEY_D;
                case "E": return BleHidConstants.KEY_E;
                case "F": return BleHidConstants.KEY_F;
                case "G": return BleHidConstants.KEY_G;
                case "H": return BleHidConstants.KEY_H;
                case "I": return BleHidConstants.KEY_I;
                case "J": return BleHidConstants.KEY_J;
                case "K": return BleHidConstants.KEY_K;
                case "L": return BleHidConstants.KEY_L;
                case "M": return BleHidConstants.KEY_M;
                case "N": return BleHidConstants.KEY_N;
                case "O": return BleHidConstants.KEY_O;
                case "P": return BleHidConstants.KEY_P;
                case "Q": return BleHidConstants.KEY_Q;
                case "R": return BleHidConstants.KEY_R;
                case "S": return BleHidConstants.KEY_S;
                case "T": return BleHidConstants.KEY_T;
                case "U": return BleHidConstants.KEY_U;
                case "V": return BleHidConstants.KEY_V;
                case "W": return BleHidConstants.KEY_W;
                case "X": return BleHidConstants.KEY_X;
                case "Y": return BleHidConstants.KEY_Y;
                case "Z": return BleHidConstants.KEY_Z;
                case "Space": return BleHidConstants.KEY_SPACE;
                case "Enter": return BleHidConstants.KEY_RETURN;
                case "Backspace": return BleHidConstants.KEY_BACKSPACE;
                default: return 0;
            }
        }
    }
}
