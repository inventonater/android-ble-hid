using UnityEngine;
using System;
using System.Collections.Generic;
using Inventonater;

namespace Inventonater
{
    /// <summary>
    /// UI component for keyboard controls
    /// </summary>
    public class KeyboardUI : SectionUI
    {
        public const string Name = "Keyboard";
        public override string TabName => Name;
        private KeyboardBridge Keyboard => BleHidClient.Instance.BleBridge.Keyboard;

        // Key mapping for characters to key codes
        private static readonly Dictionary<string, byte> keyMapping = new Dictionary<string, byte>()
        {
            { "A", BleHidConstants.KEY_A },
            { "B", BleHidConstants.KEY_B },
            { "C", BleHidConstants.KEY_C },
            { "D", BleHidConstants.KEY_D },
            { "E", BleHidConstants.KEY_E },
            { "F", BleHidConstants.KEY_F },
            { "G", BleHidConstants.KEY_G },
            { "H", BleHidConstants.KEY_H },
            { "I", BleHidConstants.KEY_I },
            { "J", BleHidConstants.KEY_J },
            { "K", BleHidConstants.KEY_K },
            { "L", BleHidConstants.KEY_L },
            { "M", BleHidConstants.KEY_M },
            { "N", BleHidConstants.KEY_N },
            { "O", BleHidConstants.KEY_O },
            { "P", BleHidConstants.KEY_P },
            { "Q", BleHidConstants.KEY_Q },
            { "R", BleHidConstants.KEY_R },
            { "S", BleHidConstants.KEY_S },
            { "T", BleHidConstants.KEY_T },
            { "U", BleHidConstants.KEY_U },
            { "V", BleHidConstants.KEY_V },
            { "W", BleHidConstants.KEY_W },
            { "X", BleHidConstants.KEY_X },
            { "Y", BleHidConstants.KEY_Y },
            { "Z", BleHidConstants.KEY_Z }
        };

        // Text input field
        private string textToSend = "";

        // QWERTY Keyboard layout
        private readonly string[] row1 = { "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P" };
        private readonly string[] row2 = { "A", "S", "D", "F", "G", "H", "J", "K", "L" };
        private readonly string[] row3 = { "Z", "X", "C", "V", "B", "N", "M" };

        public override void Update() { }

        public override void DrawUI()
        {
            DrawTextInputSection();
            DrawKeyboardSection();
            DrawNavigationKeysSection();
        }

        private void DrawTextInputSection()
        {
            UIHelper.BeginSection("Text Input");

            GUILayout.Label("Text to type:");
            textToSend = GUILayout.TextField(textToSend, UIHelper.StandardButtonOptions);

            // Send button using ActionButton to maintain consistency
            string editorMessage = GetTextActionMessage();
            if (UIHelper.Button("Send Text", SendTextMessage, editorMessage, UIHelper.StandardButtonOptions))
            {
                // ActionButton handles the action
            }

            UIHelper.EndSection();
        }

        private void DrawKeyboardSection()
        {
            UIHelper.BeginSection("Keyboard");

            // Row 1: Q-P
            UIHelper.ButtonGrid(row1, index => SendKey(row1[index]), row1.Length, UIHelper.StandardButtonHeight);

            // Row 2: A-L
            UIHelper.ButtonGrid(row2, index => SendKey(row2[index]), row2.Length, UIHelper.StandardButtonHeight);

            // Row 3: Z-M
            UIHelper.ButtonGrid(row3, index => SendKey(row3[index]), row3.Length, UIHelper.StandardButtonHeight);

            // Row 4: Special keys
            DrawSpecialKeysRow();

            UIHelper.EndSection();
        }

        private void DrawSpecialKeysRow()
        {
            string[] specialKeys = { "Enter", "Space", "Backspace" };
            Action[] specialActions =
            {
                () => SendSpecialKey(BleHidConstants.KEY_RETURN, "Enter"),
                () => SendSpecialKey(BleHidConstants.KEY_SPACE, "Space"),
                () => SendSpecialKey(BleHidConstants.KEY_BACKSPACE, "Backspace")
            };
            string[] specialMessages =
            {
                "Enter key pressed",
                "Space key pressed",
                "Backspace key pressed"
            };

            UIHelper.ActionButtonRow(
                specialKeys,
                specialActions,
                specialMessages,
                UIHelper.StandardButtonOptions);
        }

        private void DrawNavigationKeysSection()
        {
            UIHelper.BeginSection("Navigation Keys");

            string[] navKeys = { "Up", "Down", "Left", "Right" };
            Action[] navActions =
            {
                () => SendSpecialKey(BleHidConstants.KEY_UP, "Up"),
                () => SendSpecialKey(BleHidConstants.KEY_DOWN, "Down"),
                () => SendSpecialKey(BleHidConstants.KEY_LEFT, "Left"),
                () => SendSpecialKey(BleHidConstants.KEY_RIGHT, "Right")
            };
            string[] navMessages =
            {
                "Up key pressed",
                "Down key pressed",
                "Left key pressed",
                "Right key pressed"
            };

            UIHelper.ActionButtonRow(
                navKeys,
                navActions,
                navMessages,
                UIHelper.StandardButtonOptions);

            UIHelper.EndSection();
        }

        private void SendTextMessage()
        {
            if (string.IsNullOrEmpty(textToSend))
            {
                LoggingManager.Instance.Log("Cannot send empty text");
                return;
            }

            if (!IsEditorMode) Keyboard.TypeText(textToSend);
            LoggingManager.Instance.Log("Text sent: " + textToSend);
            textToSend = "";
        }

        private string GetTextActionMessage()
        {
            return !string.IsNullOrEmpty(textToSend) ? "Text sent: " + textToSend : "Cannot send empty text";
        }

        private void SendKey(string key)
        {
            byte keyCode = GetKeyCode(key);
            if (keyCode <= 0) return;

            if (IsEditorMode) LoggingManager.Instance.Log("Key pressed: " + key);
            else Keyboard.SendKey(keyCode);
        }

        private void SendSpecialKey(byte keyCode, string keyName)
        {
            if (IsEditorMode) LoggingManager.Instance.Log($"{keyName} key pressed");
            else Keyboard.SendKey(keyCode);
        }

        private byte GetKeyCode(string key)
        {
            return keyMapping.GetValueOrDefault(key, (byte)0);
        }
    }
}
