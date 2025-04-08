using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid.UI.Panels.Remote
{
    /// <summary>
    /// Panel for remote keyboard control functionality over BLE HID.
    /// </summary>
    public class RemoteKeyboardPanel : BaseBleHidPanel
    {
        private string textInput = "";
        private Vector2 specialKeysScrollPosition;
        
        // Keyboard state
        private bool shiftDown = false;
        private bool ctrlDown = false;
        private bool altDown = false;
        
        // Key layouts
        private string[] row1Keys = { "`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=" };
        private string[] row2Keys = { "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]", "\\" };
        private string[] row3Keys = { "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'" };
        private string[] row4Keys = { "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/" };
        
        // Direction keys
        private Dictionary<string, byte> directionKeys = new Dictionary<string, byte>()
        {
            { "Up", BleHidConstants.KEY_UP },
            { "Down", BleHidConstants.KEY_DOWN },
            { "Left", BleHidConstants.KEY_LEFT },
            { "Right", BleHidConstants.KEY_RIGHT }
        };
        
        // Special keys
        private Dictionary<string, byte> specialKeys = new Dictionary<string, byte>()
        {
            { "Esc", BleHidConstants.KEY_ESC },
            { "Tab", BleHidConstants.KEY_TAB },
            { "Caps", BleHidConstants.KEY_CAPS_LOCK },
            { "Enter", BleHidConstants.KEY_RETURN },
            { "Backspace", BleHidConstants.KEY_BACKSPACE },
            { "Delete", BleHidConstants.KEY_DELETE },
            { "Insert", BleHidConstants.KEY_INSERT },
            { "Home", BleHidConstants.KEY_HOME },
            { "End", BleHidConstants.KEY_END },
            { "Page Up", BleHidConstants.KEY_PAGE_UP },
            { "Page Down", BleHidConstants.KEY_PAGE_DOWN },
            { "Print", BleHidConstants.KEY_PRINT },
            { "Pause", BleHidConstants.KEY_PAUSE }
        };
        
        public override bool RequiresConnectedDevice => true;
        
        protected override void DrawPanelContent()
        {
            GUILayout.Label("Keyboard Controls", titleStyle);
            
            #if UNITY_EDITOR
            bool isEditorMode = true;
            #else
            bool isEditorMode = false;
            #endif
            
            // Text input area
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Text Input", subtitleStyle);
            
            GUILayout.BeginHorizontal();
            textInput = GUILayout.TextField(textInput, GUILayout.Height(40));
            
            if (GUILayout.Button("Clear", GUILayout.Width(80), GUILayout.Height(40)))
            {
                textInput = "";
            }
            
            if (GUILayout.Button("Send", GUILayout.Width(80), GUILayout.Height(40)))
            {
                if (isEditorMode)
                {
                    logger.Log($"Would send text: {textInput}");
                }
                else if (bleHidManager != null && !string.IsNullOrEmpty(textInput))
                {
                    bleHidManager.SendText(textInput);
                    logger.Log($"Sent text: {textInput}");
                }
            }
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            // Arrow keys section
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Directional Keys", subtitleStyle);
            
            // Up arrow key
            GUILayout.BeginHorizontal();
            GUILayout.FlexibleSpace();
            if (GUILayout.Button("Up", GUILayout.Height(50), GUILayout.Width(100)))
            {
                SendSpecialKey("Up");
            }
            GUILayout.FlexibleSpace();
            GUILayout.EndHorizontal();
            
            // Left, Down, Right arrow keys
            GUILayout.BeginHorizontal();
            GUILayout.FlexibleSpace();
            if (GUILayout.Button("Left", GUILayout.Height(50), GUILayout.Width(100)))
            {
                SendSpecialKey("Left");
            }
            
            if (GUILayout.Button("Down", GUILayout.Height(50), GUILayout.Width(100)))
            {
                SendSpecialKey("Down");
            }
            
            if (GUILayout.Button("Right", GUILayout.Height(50), GUILayout.Width(100)))
            {
                SendSpecialKey("Right");
            }
            GUILayout.FlexibleSpace();
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            // Modifier keys
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Modifier Keys", subtitleStyle);
            
            GUILayout.BeginHorizontal();
            DrawToggleButton("Shift", ref shiftDown, GUILayout.Height(50));
            DrawToggleButton("Ctrl", ref ctrlDown, GUILayout.Height(50));
            DrawToggleButton("Alt", ref altDown, GUILayout.Height(50));
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            // Special Keys (scrollable)
            GUILayout.BeginVertical(GUI.skin.box, GUILayout.Height(150));
            GUILayout.Label("Special Keys", subtitleStyle);
            
            specialKeysScrollPosition = GUILayout.BeginScrollView(specialKeysScrollPosition);
            
            // Create rows of special keys (4 per row)
            int keysPerRow = 4;
            int rowCount = (specialKeys.Count + keysPerRow - 1) / keysPerRow;
            
            int keyIndex = 0;
            string[] keyNames = new string[specialKeys.Count];
            
            foreach (string key in specialKeys.Keys)
            {
                keyNames[keyIndex] = key;
                keyIndex++;
            }
            
            for (int row = 0; row < rowCount; row++)
            {
                GUILayout.BeginHorizontal();
                
                for (int col = 0; col < keysPerRow; col++)
                {
                    int index = row * keysPerRow + col;
                    
                    if (index < keyNames.Length)
                    {
                        if (GUILayout.Button(keyNames[index], GUILayout.Height(40)))
                        {
                            SendSpecialKey(keyNames[index]);
                        }
                    }
                    else
                    {
                        // Empty placeholder to maintain grid layout
                        GUILayout.Space(0);
                    }
                }
                
                GUILayout.EndHorizontal();
            }
            
            GUILayout.EndScrollView();
            GUILayout.EndVertical();
            
            // Virtual keyboard
            DrawVirtualKeyboard();
        }
        
        /// <summary>
        /// Draw the virtual keyboard layout.
        /// </summary>
        private void DrawVirtualKeyboard()
        {
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Virtual Keyboard", subtitleStyle);
            
            // Row 1: Numbers and symbols
            DrawKeyboardRow(row1Keys);
            
            // Row 2: QWERTYUIOP and brackets
            DrawKeyboardRow(row2Keys);
            
            // Row 3: ASDFGHJKL and symbols
            GUILayout.BeginHorizontal();
            // Add a little spacing to offset for the staggered keyboard layout
            GUILayout.Space(20);
            DrawKeyboardRow(row3Keys);
            GUILayout.EndHorizontal();
            
            // Row 4: ZXCVBNM and symbols
            GUILayout.BeginHorizontal();
            // Add more spacing for the last row
            GUILayout.Space(40);
            DrawKeyboardRow(row4Keys);
            GUILayout.EndHorizontal();
            
            // Space bar row
            GUILayout.BeginHorizontal();
            
            if (GUILayout.Button("Space", GUILayout.Height(40), GUILayout.Width(Screen.width * 0.6f)))
            {
                SendKey(' ');
            }
            
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
        }
        
        /// <summary>
        /// Draw a row of keyboard keys.
        /// </summary>
        private void DrawKeyboardRow(string[] keys)
        {
            GUILayout.BeginHorizontal();
            
            foreach (string key in keys)
            {
                if (GUILayout.Button(key, GUILayout.Height(40), GUILayout.Width(40)))
                {
                    // Convert key string to character
                    if (key.Length == 1)
                    {
                        char keyChar = key[0];
                        SendKey(keyChar);
                    }
                    else
                    {
                        // Special key handling could be added here
                        logger.Log($"Special key pressed: {key}");
                    }
                }
            }
            
            GUILayout.EndHorizontal();
        }
        
        /// <summary>
        /// Send a keyboard key.
        /// </summary>
        private void SendKey(char key)
        {
            #if UNITY_EDITOR
            logger.Log($"Key pressed: {key}");
            #else
            if (bleHidManager != null)
            {
                // Apply modifiers if needed
                if (shiftDown)
                {
                    bleHidManager.KeyDown(BleHidConstants.KEY_LEFT_SHIFT);
                }
                
                if (ctrlDown)
                {
                    bleHidManager.KeyDown(BleHidConstants.KEY_LEFT_CTRL);
                }
                
                if (altDown)
                {
                    bleHidManager.KeyDown(BleHidConstants.KEY_LEFT_ALT);
                }
                
                // Send the key
                bleHidManager.SendKey(key);
                
                // Release modifiers
                if (shiftDown)
                {
                    bleHidManager.KeyUp(BleHidConstants.KEY_LEFT_SHIFT);
                }
                
                if (ctrlDown)
                {
                    bleHidManager.KeyUp(BleHidConstants.KEY_LEFT_CTRL);
                }
                
                if (altDown)
                {
                    bleHidManager.KeyUp(BleHidConstants.KEY_LEFT_ALT);
                }
                
                logger.Log($"Sent key: {key}");
            }
            #endif
        }
        
        /// <summary>
        /// Send a special key.
        /// </summary>
        private void SendSpecialKey(string keyName)
        {
            byte keyCode = 0;
            
            // Check if it's a directional key
            if (directionKeys.TryGetValue(keyName, out keyCode))
            {
                #if UNITY_EDITOR
                logger.Log($"Direction key pressed: {keyName}");
                #else
                if (bleHidManager != null)
                {
                    bleHidManager.SendKey(keyCode);
                    logger.Log($"Sent direction key: {keyName}");
                }
                #endif
                return;
            }
            
            // Check if it's a special key
            if (specialKeys.TryGetValue(keyName, out keyCode))
            {
                #if UNITY_EDITOR
                logger.Log($"Special key pressed: {keyName}");
                #else
                if (bleHidManager != null)
                {
                    bleHidManager.SendKey(keyCode);
                    logger.Log($"Sent special key: {keyName}");
                }
                #endif
                return;
            }
            
            logger.LogWarning($"Unknown key: {keyName}");
        }
        
        /// <summary>
        /// Draw a toggle button that changes state when clicked.
        /// </summary>
        private void DrawToggleButton(string text, ref bool state, params GUILayoutOption[] options)
        {
            // Change button style based on state
            GUIStyle toggleStyle = new GUIStyle(GUI.skin.button);
            if (state)
            {
                toggleStyle.normal.background = MakeColorTexture(new Color(0.2f, 0.7f, 0.2f, 1.0f));
                toggleStyle.normal.textColor = Color.white;
            }
            
            if (GUILayout.Button(text, toggleStyle, options))
            {
                // Toggle state
                state = !state;
                logger.Log($"{text} toggled to {(state ? "ON" : "OFF")}");
            }
        }
    }
}
