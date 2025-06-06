using System;
using UnityEngine;

namespace Inventonater
{
    /// <summary>
    /// Helper class for common UI operations to reduce code duplication
    /// </summary>
    public static class UIHelper
    {
        // Standard UI sizes
        public static readonly float StandardButtonHeight = 60f;
        public static readonly float LargeButtonHeight = 80f;
        public static readonly float StandardSliderHeight = 40f; // Taller slider for better touch interaction
        public static readonly float LargeSliderHeight = 60f;
        
        public static readonly GUILayoutOption[] StandardButtonOptions = new GUILayoutOption[] { GUILayout.Height(StandardButtonHeight) };
        public static readonly GUILayoutOption[] LargeButtonOptions = new GUILayoutOption[] { GUILayout.Height(LargeButtonHeight) };
        public static readonly GUILayoutOption[] StandardSliderOptions = new GUILayoutOption[] { GUILayout.Height(StandardSliderHeight) };
        public static readonly GUILayoutOption[] LargeSliderOptions = new GUILayoutOption[] { GUILayout.Height(LargeSliderHeight) };
        public static GUIStyle BoldStyle => new(GUI.skin.label) { fontStyle = FontStyle.Bold };

        public static void SetupGUIStyles()
        {
            // Set up GUI style for better touch targets
            GUI.skin.button.fontSize = 24;
            GUI.skin.label.fontSize = 20;
            GUI.skin.textField.fontSize = 20;
            GUI.skin.box.fontSize = 20;
        }

        public static bool Button(string label, Action runtimeAction, string logMessage = null, GUILayoutOption[] options = null)
        {

            bool buttonPressed = options != null ? GUILayout.Button(label, options) : GUILayout.Button(label);
            if (buttonPressed)
            {
                if (!IsEditorMode) runtimeAction?.Invoke();
                LoggingManager.Instance.Log(logMessage);
            }

            return buttonPressed;
        }

        private static bool IsEditorMode => Application.isEditor;

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
        public static void ActionButtonRow(string[] buttonLabels, Action[] actions, string[] editorMessages, GUILayoutOption[] options = null)
        {
            if (options == null) options = StandardButtonOptions;
                
            GUILayout.BeginHorizontal();
            for (int i = 0; i < buttonLabels.Length; i++)
            {
                string message = (editorMessages != null && i < editorMessages.Length) ? editorMessages[i] : buttonLabels[i] + " pressed";
                Button(buttonLabels[i], actions[i], message, options);
            }
            GUILayout.EndHorizontal();
        }
        
        /// <summary>
        /// Begin a standard UI section with a title
        /// </summary>
        public static void BeginSection(string title = null)
        {
            GUILayout.BeginVertical(GUI.skin.box);
            if (!string.IsNullOrEmpty(title)) GUILayout.Label(title);
        }
        
        /// <summary>
        /// End a UI section
        /// </summary>
        public static void EndSection()
        {
            GUILayout.EndVertical();
        }
        
        /// <summary>
        /// Enhanced slider with touch-friendly height and better visual appearance (float version)
        /// </summary>
        public static float EnhancedSlider(float currentValue, float minValue, float maxValue, string labelFormat = "{0}", GUILayoutOption[] options = null)
        {
            // Use standard slider options if none provided
            options = options ?? StandardSliderOptions;
            
            // Create a custom slider style with larger thumb
            GUIStyle sliderStyle = new GUIStyle(GUI.skin.horizontalSlider);
            sliderStyle.fixedHeight = 0; // Allow height to be controlled by layout options
            
            GUIStyle thumbStyle = new GUIStyle(GUI.skin.horizontalSliderThumb);
            thumbStyle.fixedHeight = 0; // Allow thumb height to adapt
            thumbStyle.fixedWidth = 60; // Wider thumb for easier touch
            
            // Create the slider
            return GUILayout.HorizontalSlider(currentValue, minValue, maxValue, sliderStyle, thumbStyle, options);
        }
        
        /// <summary>
        /// Enhanced slider with touch-friendly height and better visual appearance (int version)
        /// </summary>
        public static int EnhancedSliderInt(int currentValue, int minValue, int maxValue, 
            string labelFormat = "{0}", GUILayoutOption[] options = null)
        {
            float result = EnhancedSlider((float)currentValue, (float)minValue, (float)maxValue, labelFormat, options);
            return Mathf.RoundToInt(result);
        }
        
        /// <summary>
        /// Slider with min/max labels and current value display
        /// </summary>
        public static float SliderWithLabels(string leftLabel, float currentValue, float minValue, float maxValue, 
            string rightLabel, string valueFormat = "{0}", GUILayoutOption[] options = null)
        {
            GUILayout.BeginVertical();
            
            // Top row with current value display
            GUILayout.BeginHorizontal();
            GUILayout.FlexibleSpace();
            GUILayout.Label(string.Format(valueFormat, currentValue), GUILayout.MinWidth(200));
            GUILayout.FlexibleSpace();
            GUILayout.EndHorizontal();
            
            // Slider row with min/max labels
            GUILayout.BeginHorizontal();
            GUILayout.Label(leftLabel, GUILayout.Width(100));
            float newValue = EnhancedSlider(currentValue, minValue, maxValue, valueFormat, options);
            GUILayout.Label(rightLabel, GUILayout.Width(100));
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            return newValue;
        }
        
        /// <summary>
        /// Slider with min/max labels and current value display (int version)
        /// </summary>
        public static int SliderWithLabels(string leftLabel, int currentValue, int minValue, int maxValue, 
            string rightLabel, string valueFormat = "{0}", GUILayoutOption[] options = null)
        {
            float result = SliderWithLabels(leftLabel, (float)currentValue, (float)minValue, (float)maxValue, 
                rightLabel, valueFormat, options);
            return Mathf.RoundToInt(result);
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
        
        /// <summary>
        /// Draw a dropdown control with proper styling and consistent behavior
        /// </summary>
        /// <param name="label">Label to display beside the dropdown</param>
        /// <param name="currentValue">The currently selected value</param>
        /// <param name="options">Array of all available options</param>
        /// <param name="isExpanded">Reference to a bool tracking the expanded state</param>
        /// <param name="onSelectionChanged">Action to call when selection changes (receives index)</param>
        /// <returns>True if a new option was selected</returns>
        public static bool DrawDropdownControl(string label, string currentValue, string[] options, 
            ref bool isExpanded, Action<int> onSelectionChanged)
        {
            bool selectionChanged = false;
            GUILayoutOption dropDownLabel = GUILayout.Width(120);
            
            // Label and current selection display
            GUILayout.BeginHorizontal();
            GUILayout.Label(label, dropDownLabel);
            
            // Create a custom dropdown style
            GUIStyle dropdownStyle = new GUIStyle(GUI.skin.button);
            dropdownStyle.alignment = TextAnchor.MiddleLeft;
            dropdownStyle.padding = new RectOffset(10, 10, 5, 5);
            
            // Make dropdown button that toggles the expanded state
            if (GUILayout.Button(currentValue, dropdownStyle, GUILayout.MinWidth(180)))
            {
                isExpanded = !isExpanded;
            }
            
            // Dropdown arrow indicator
            GUILayout.Label(isExpanded ? "▲" : "▼", GUILayout.Width(20));
            GUILayout.EndHorizontal();
            
            // Show dropdown options if expanded
            if (isExpanded)
            {
                GUILayout.BeginVertical(GUI.skin.box);
                for (int i = 0; i < options.Length; i++)
                {
                    // Determine if this is the currently selected option
                    bool isSelected = (options[i] == currentValue);
                    
                    // Style to highlight selected option
                    GUIStyle optionStyle = new GUIStyle(GUI.skin.button);
                    if (isSelected)
                    {
                        optionStyle.normal.textColor = Color.green;
                        optionStyle.fontStyle = FontStyle.Bold;
                    }
                    
                    // Option button
                    if (GUILayout.Button(options[i], optionStyle))
                    {
                        onSelectionChanged?.Invoke(i);
                        isExpanded = false;
                        selectionChanged = true;
                    }
                }
                GUILayout.EndVertical();
            }
            
            return selectionChanged;
        }
        
        /// <summary>
        /// Create a toggle button that switches between two states
        /// </summary>
        /// <param name="label">Label to display</param>
        /// <param name="isActive">Current state</param>
        /// <param name="activeText">Text to show when active</param>
        /// <param name="inactiveText">Text to show when inactive</param>
        /// <returns>The new state after the function completes</returns>
        public static bool ToggleButton(string label, bool isActive, string activeText, string inactiveText, 
            GUILayoutOption[] options = null)
        {
            GUILayout.BeginHorizontal();
            
            if (!string.IsNullOrEmpty(label))
            {
                GUILayout.Label(label, GUILayout.Width(120));
            }
            
            // Create button style based on current state
            GUIStyle toggleStyle = new GUIStyle(GUI.skin.button);
            if (isActive)
            {
                toggleStyle.normal.textColor = Color.green;
                toggleStyle.fontStyle = FontStyle.Bold;
            }
            
            // Use provided options or standard button options
            options = options ?? StandardButtonOptions;
            
            // Button text changes based on state
            string buttonText = isActive ? activeText : inactiveText;
            
            // Toggle button
            if (GUILayout.Button(buttonText, toggleStyle, options))
            {
                isActive = !isActive;
            }
            
            GUILayout.EndHorizontal();
            
            return isActive;
        }
        
        /// <summary>
        /// Format a value with the appropriate unit and precision
        /// </summary>
        public static string FormatValue(float value, string unit, int precision = 1)
        {
            string format = "{0:F" + precision + "} " + unit;
            return string.Format(format, value);
        }
        
        /// <summary>
        /// Create a simple info dialog with title, message and close button
        /// </summary>
        public static void DrawInfoDialog(string title, string message, Action onClose)
        {
            // Semi-transparent background overlay
            GUIStyle overlayStyle = new GUIStyle();
            overlayStyle.normal.background = MakeColorTexture(new Color(0, 0, 0, 0.7f));
            
            // Dialog style
            GUIStyle dialogStyle = new GUIStyle(GUI.skin.box);
            dialogStyle.padding = new RectOffset(20, 20, 20, 20);
            
            // Title style
            GUIStyle titleStyle = new GUIStyle(GUI.skin.label);
            titleStyle.fontSize = GUI.skin.label.fontSize + 4;
            titleStyle.fontStyle = FontStyle.Bold;
            titleStyle.alignment = TextAnchor.MiddleCenter;
            
            // Draw overlay across entire screen
            GUI.Box(new Rect(0, 0, Screen.width, Screen.height), "", overlayStyle);
            
            // Dialog dimensions
            float dialogWidth = Mathf.Min(Screen.width * 0.8f, 500);
            float dialogHeight = 300;
            float dialogX = (Screen.width - dialogWidth) / 2;
            float dialogY = (Screen.height - dialogHeight) / 2;
            
            // Draw dialog box
            GUILayout.BeginArea(new Rect(dialogX, dialogY, dialogWidth, dialogHeight), dialogStyle);
            
            // Dialog content
            GUILayout.Label(title, titleStyle);
            GUILayout.Space(10);
            
            // Message with word wrap
            GUIStyle messageStyle = new GUIStyle(GUI.skin.label);
            messageStyle.wordWrap = true;
            GUILayout.Label(message, messageStyle);
            
            GUILayout.FlexibleSpace();
            
            // Close button
            if (GUILayout.Button("Close", StandardButtonOptions))
            {
                onClose?.Invoke();
            }
            
            GUILayout.EndArea();
        }
        
        /// <summary>
        /// Handle a slider being dragged with proper tracking
        /// </summary>
        /// <param name="value">The current slider value</param>
        /// <param name="previousValue">The previous slider value for comparison</param>
        /// <param name="isBeingDragged">Reference to a bool tracking dragging state</param>
        /// <param name="onValueChanged">Action to call when value changes and drag ends</param>
        /// <returns>True if value was applied</returns>
        public static bool HandleSliderDragging(float value, float previousValue, ref bool isBeingDragged, Action onValueChanged)
        {
            bool valueApplied = false;
            
            // Track whether mouse button is pressed to detect drag state
            if (Event.current.type == EventType.MouseDown && Event.current.button == 0)
            {
                isBeingDragged = true;
            }
            else if (Event.current.type == EventType.MouseUp && Event.current.button == 0)
            {
                if (isBeingDragged)
                {
                    isBeingDragged = false;
                    // Apply on mouse up if value changed
                    if (!Mathf.Approximately(value, previousValue))
                    {
                        onValueChanged?.Invoke();
                        valueApplied = true;
                    }
                }
            }
            
            return valueApplied;
        }
    }
}
