using UnityEngine;

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
            // Text input field
            GUILayout.Label("Text to type:");
            textToSend = GUILayout.TextField(textToSend, GUILayout.Height(60));

            // Send button
            if (GUILayout.Button("Send Text", GUILayout.Height(60)))
            {
                if (!string.IsNullOrEmpty(textToSend))
                {
                    if (IsEditorMode)
                        Logger.AddLogEntry("Text sent: " + textToSend);
                    else
                        BleHidManager.TypeText(textToSend);

                    textToSend = "";
                }
                else
                {
                    Logger.AddLogEntry("Cannot send empty text");
                }
            }

            // Common keys
            GUILayout.Label("Common Keys:");

            // Row 1: Q-P
            DrawKeyboardRow(new string[] { "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P" });

            // Row 2: A-L
            DrawKeyboardRow(new string[] { "A", "S", "D", "F", "G", "H", "J", "K", "L" });

            // Row 3: Z-M
            DrawKeyboardRow(new string[] { "Z", "X", "C", "V", "B", "N", "M" });

            // Row 4: Special keys
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Enter", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Enter key pressed");
                else
                    BleHidManager.SendKey(BleHidConstants.KEY_RETURN);
            }

            if (GUILayout.Button("Space", GUILayout.Height(60), GUILayout.Width(Screen.width * 0.3f)))
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Space key pressed");
                else
                    BleHidManager.SendKey(BleHidConstants.KEY_SPACE);
            }

            if (GUILayout.Button("Backspace", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Backspace key pressed");
                else
                    BleHidManager.SendKey(BleHidConstants.KEY_BACKSPACE);
            }
            GUILayout.EndHorizontal();
            
            // Navigation keys
            GUILayout.Label("Navigation Keys:");
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Up", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Up key pressed");
                else
                    BleHidManager.SendKey(BleHidConstants.KEY_UP);
            }
            if (GUILayout.Button("Down", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Down key pressed");
                else
                    BleHidManager.SendKey(BleHidConstants.KEY_DOWN);
            }
            if (GUILayout.Button("Left", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Left key pressed");
                else
                    BleHidManager.SendKey(BleHidConstants.KEY_LEFT);
            }
            if (GUILayout.Button("Right", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                    Logger.AddLogEntry("Right key pressed");
                else
                    BleHidManager.SendKey(BleHidConstants.KEY_RIGHT);
            }
            GUILayout.EndHorizontal();
        }

        private void DrawKeyboardRow(string[] keys)
        {
            GUILayout.BeginHorizontal();
            foreach (string key in keys)
            {
                if (GUILayout.Button(key, GUILayout.Height(60), GUILayout.Width(Screen.width / (keys.Length + 2))))
                {
                    SendKey(key);
                }
            }
            GUILayout.EndHorizontal();
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
