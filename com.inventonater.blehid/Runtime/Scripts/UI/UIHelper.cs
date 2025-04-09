using System;
using UnityEngine;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// Helper class for common UI operations to reduce code duplication
    /// </summary>
    public static class UIHelper
    {
        /// <summary>
        /// Create a standard button that handles both editor mode and runtime functionality
        /// </summary>
        public static bool Button(string label, Action runtimeAction, Action editorAction, bool isEditorMode, 
            LoggingManager logger, GUILayoutOption[] options = null)
        {
            bool buttonPressed = options != null ? GUILayout.Button(label, options) : GUILayout.Button(label);
            
            if (buttonPressed)
            {
                if (isEditorMode)
                {
                    editorAction?.Invoke();
                }
                else
                {
                    runtimeAction?.Invoke();
                }
            }
            
            return buttonPressed;
        }
        
        /// <summary>
        /// Create a standard button that logs a message in editor mode and performs an action in runtime
        /// </summary>
        public static bool LoggingButton(string label, Action runtimeAction, string editorLogMessage, 
            bool isEditorMode, LoggingManager logger, GUILayoutOption[] options = null)
        {
            return Button(label, runtimeAction, () => logger.AddLogEntry(editorLogMessage), isEditorMode, logger, options);
        }
        
        /// <summary>
        /// Create a standard styled box for error displays
        /// </summary>
        public static GUIStyle CreateErrorStyle(Color backgroundColor)
        {
            GUIStyle errorStyle = new GUIStyle(GUI.skin.box);
            errorStyle.normal.background = MakeColorTexture(backgroundColor);
            errorStyle.normal.textColor = Color.white;
            errorStyle.fontSize = 22;
            errorStyle.fontStyle = FontStyle.Bold;
            errorStyle.padding = new RectOffset(20, 20, 20, 20);
            return errorStyle;
        }
        
        /// <summary>
        /// Create a texture with a solid color
        /// </summary>
        public static Texture2D MakeColorTexture(Color color)
        {
            Texture2D texture = new Texture2D(1, 1);
            texture.SetPixel(0, 0, color);
            texture.Apply();
            return texture;
        }
        
        /// <summary>
        /// Create a standard button row with evenly spaced buttons
        /// </summary>
        public static void ButtonRow(string[] buttonLabels, Action<int> buttonCallback, 
            float height, GUILayoutOption widthOption = null)
        {
            GUILayout.BeginHorizontal();
            for (int i = 0; i < buttonLabels.Length; i++)
            {
                int index = i; // Capture for lambda
                GUILayoutOption[] options = widthOption != null 
                    ? new GUILayoutOption[] { GUILayout.Height(height), widthOption }
                    : new GUILayoutOption[] { GUILayout.Height(height) };
                    
                if (GUILayout.Button(buttonLabels[i], options))
                {
                    buttonCallback(index);
                }
            }
            GUILayout.EndHorizontal();
        }
    }
}
