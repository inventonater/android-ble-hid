using System;
using UnityEngine;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// Helper class for common UI operations to reduce code duplication
    /// </summary>
    public static class UIHelper
    {
        // Standard UI sizes
        public static readonly float StandardButtonHeight = 60f;
        public static readonly float LargeButtonHeight = 80f;
        
        // Predefined layout options
        public static readonly GUILayoutOption[] StandardButtonOptions = new GUILayoutOption[] { GUILayout.Height(StandardButtonHeight) };
        public static readonly GUILayoutOption[] LargeButtonOptions = new GUILayoutOption[] { GUILayout.Height(LargeButtonHeight) };
        
        /// <summary>
        /// Create a standard button that handles both editor mode and runtime functionality
        /// </summary>
        public static bool Button(string label, Action runtimeAction, Action editorAction, bool isEditorMode, 
            LoggingManager logger, GUILayoutOption[] options = null)
        {
            bool buttonPressed = options != null ? GUILayout.Button(label, options) : GUILayout.Button(label);
            
            if (buttonPressed)
            {
                ExecuteWithEditorFallback(runtimeAction, editorAction, isEditorMode);
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
        /// Execute the appropriate action based on editor mode
        /// </summary>
        public static void ExecuteWithEditorFallback(Action runtimeAction, Action editorAction, bool isEditorMode)
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
        
        /// <summary>
        /// Create a row of action buttons with consistent styling
        /// </summary>
        public static void ActionButtonRow(string[] buttonLabels, Action[] actions, bool isEditorMode, 
            LoggingManager logger, string[] editorMessages, GUILayoutOption[] options = null)
        {
            if (options == null)
                options = StandardButtonOptions;
                
            GUILayout.BeginHorizontal();
            for (int i = 0; i < buttonLabels.Length; i++)
            {
                int index = i; // Capture for lambda
                string message = (editorMessages != null && i < editorMessages.Length) 
                    ? editorMessages[i] 
                    : buttonLabels[i] + " pressed";
                    
                LoggingButton(
                    buttonLabels[i], 
                    actions[i], 
                    message, 
                    isEditorMode, 
                    logger, 
                    options);
            }
            GUILayout.EndHorizontal();
        }
        
        /// <summary>
        /// Begin a standard UI section with a title
        /// </summary>
        public static void BeginSection(string title = null)
        {
            GUILayout.BeginVertical(GUI.skin.box);
            if (!string.IsNullOrEmpty(title))
            {
                GUILayout.Label(title);
            }
        }
        
        /// <summary>
        /// End a UI section
        /// </summary>
        public static void EndSection()
        {
            GUILayout.EndVertical();
        }
        
        /// <summary>
        /// Create a grid of buttons with consistent styling
        /// </summary>
        public static void ButtonGrid(string[] buttonLabels, Action<int> buttonCallback, 
            int columns, float buttonHeight, GUILayoutOption buttonWidth = null)
        {
            int rows = (buttonLabels.Length + columns - 1) / columns; // Round up division
            
            for (int row = 0; row < rows; row++)
            {
                GUILayout.BeginHorizontal();
                for (int col = 0; col < columns; col++)
                {
                    int index = row * columns + col;
                    if (index < buttonLabels.Length)
                    {
                        int capturedIndex = index; // Capture for lambda
                        GUILayoutOption[] options = buttonWidth != null 
                            ? new GUILayoutOption[] { GUILayout.Height(buttonHeight), buttonWidth } 
                            : new GUILayoutOption[] { GUILayout.Height(buttonHeight) };
                            
                        if (GUILayout.Button(buttonLabels[index], options))
                        {
                            buttonCallback(capturedIndex);
                        }
                    }
                    else
                    {
                        // Empty cell for grid alignment
                        GUILayout.FlexibleSpace();
                    }
                }
                GUILayout.EndHorizontal();
            }
        }
    }
}
