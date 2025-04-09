using UnityEngine;
using System;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// UI component for keyboard controls
    /// </summary>
    public class KeyboardControlsComponent : UIComponent
    {
        private string textToSend = "";
        
        public override void DrawUI()
        {
            // Text input section
            UIHelper.BeginSection("Text Input");
            GUILayout.Label("Text to type:");
            textToSend = GUILayout.TextField(textToSend, UIHelper.StandardButtonOptions);

            // Send button
            UIHelper.LoggingButton(
                "Send Text", 
                () => {
                    if (!string.IsNullOrEmpty(textToSend)) {
                        BleHidManager.TypeText(textToSend);
                        textToSend = "";
                    } else {
                        Logger.AddLogEntry("Cannot send empty text");
                    }
                },
                !string.IsNullOrEmpty(textToSend) ? "Text sent: " + textToSend : "Cannot send empty text",
                IsEditorMode,
                Logger,
                UIHelper.StandardButtonOptions);
            UIHelper.EndSection();

            // QWERTY Keyboard section
            UIHelper.BeginSection("Keyboard");
            
            // Row 1: Q-P
            string[] row1 = { "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P" };
            UIHelper.ButtonGrid(row1, index => SendKey(row1[index]), row1.Length, UIHelper.StandardButtonHeight);

            // Row 2: A-L
            string[] row2 = { "A", "S", "D", "F", "G", "H", "J", "K", "L" };
            UIHelper.ButtonGrid(row2, index => SendKey(row2[index]), row2.Length, UIHelper.StandardButtonHeight);

            // Row 3: Z-M
            string[] row3 = { "Z", "X", "C", "V", "B", "N", "M" };
            UIHelper.ButtonGrid(row3, index => SendKey(row3[index]), row3.Length, UIHelper.StandardButtonHeight);

            // Row 4: Special keys
            string[] specialKeys = { "Enter", "Space", "Backspace" };
            Action[] specialActions = {
                () => BleHidManager.SendKey(BleHidConstants.KEY_RETURN),
                () => BleHidManager.SendKey(BleHidConstants.KEY_SPACE),
                () => BleHidManager.SendKey(BleHidConstants.KEY_BACKSPACE)
            };
            string[] specialMessages = {
                "Enter key pressed",
                "Space key pressed",
                "Backspace key pressed"
            };
            
            UIHelper.ActionButtonRow(
                specialKeys,
                specialActions,
                IsEditorMode,
                Logger,
                specialMessages,
                UIHelper.StandardButtonOptions);
            UIHelper.EndSection();
            
            // Navigation keys
            UIHelper.BeginSection("Navigation Keys");
            string[] navKeys = { "Up", "Down", "Left", "Right" };
            Action[] navActions = {
                () => BleHidManager.SendKey(BleHidConstants.KEY_UP),
                () => BleHidManager.SendKey(BleHidConstants.KEY_DOWN),
                () => BleHidManager.SendKey(BleHidConstants.KEY_LEFT),
                () => BleHidManager.SendKey(BleHidConstants.KEY_RIGHT)
            };
            string[] navMessages = {
                "Up key pressed",
                "Down key pressed",
                "Left key pressed",
                "Right key pressed"
            };
            
            UIHelper.ActionButtonRow(
                navKeys,
                navActions,
                IsEditorMode,
                Logger,
                navMessages,
                UIHelper.StandardButtonOptions);
            UIHelper.EndSection();
        }

        private void SendKey(string key)
        {
            byte keyCode = GetKeyCode(key);
            if (keyCode > 0)
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Key pressed: " + key);
                else
                    BleHidManager.SendKey(keyCode);
            }
        }

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
                default: return 0;
            }
        }
    }
}
