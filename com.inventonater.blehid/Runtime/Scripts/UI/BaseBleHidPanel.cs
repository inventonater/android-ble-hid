using UnityEngine;
using System.Collections.Generic;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// Base class for all BLE HID UI panels.
    /// </summary>
    public abstract class BaseBleHidPanel : IBleHidPanel
    {
        protected BleHidManager bleHidManager;
        protected BleHidLogger logger;
        
        // List of subtabs for this panel
        protected List<string> subtabNames = new List<string>();
        protected int currentSubtab = 0;
        
        // Standard UI elements
        protected GUIStyle titleStyle;
        protected GUIStyle subtitleStyle;
        protected GUIStyle errorStyle;
        protected GUIStyle warningStyle;
        protected GUIStyle successStyle;
        
        /// <summary>
        /// Initialize the panel with necessary references.
        /// </summary>
        public virtual void Initialize(BleHidManager manager)
        {
            bleHidManager = manager;
            logger = BleHidLogger.Instance;
            
            // Initialize styles
            InitializeStyles();
        }
        
        /// <summary>
        /// Draw the panel content.
        /// </summary>
        public virtual void DrawPanel()
        {
            // Draw subtabs if we have more than one
            if (subtabNames.Count > 1)
            {
                currentSubtab = GUILayout.Toolbar(currentSubtab, subtabNames.ToArray(), GUILayout.Height(50));
                GUILayout.Space(10);
                
                // Draw the current subtab content
                DrawSubtab(currentSubtab);
            }
            else
            {
                // If no subtabs, just draw the content directly
                DrawPanelContent();
            }
        }
        
        /// <summary>
        /// Whether this panel requires a connected device to function.
        /// </summary>
        public abstract bool RequiresConnectedDevice { get; }
        
        /// <summary>
        /// Called when the panel becomes active.
        /// </summary>
        public virtual void OnActivate() { }
        
        /// <summary>
        /// Called when the panel becomes inactive.
        /// </summary>
        public virtual void OnDeactivate() { }
        
        /// <summary>
        /// Process Update calls when this panel is active.
        /// </summary>
        public virtual void Update() { }
        
        /// <summary>
        /// Initialize common GUI styles.
        /// </summary>
        protected virtual void InitializeStyles()
        {
            // Title style
            titleStyle = new GUIStyle(GUI.skin.label);
            titleStyle.fontSize = 24;
            titleStyle.fontStyle = FontStyle.Bold;
            titleStyle.alignment = TextAnchor.MiddleCenter;
            
            // Subtitle style
            subtitleStyle = new GUIStyle(GUI.skin.label);
            subtitleStyle.fontSize = 18;
            subtitleStyle.fontStyle = FontStyle.Bold;
            
            // Error style
            errorStyle = new GUIStyle(GUI.skin.box);
            errorStyle.normal.textColor = Color.white;
            errorStyle.normal.background = MakeColorTexture(new Color(0.8f, 0.2f, 0.2f, 1.0f));
            errorStyle.fontSize = 16;
            errorStyle.padding = new RectOffset(10, 10, 10, 10);
            
            // Warning style
            warningStyle = new GUIStyle(GUI.skin.box);
            warningStyle.normal.textColor = Color.white;
            warningStyle.normal.background = MakeColorTexture(new Color(0.8f, 0.6f, 0.0f, 1.0f));
            warningStyle.fontSize = 16;
            warningStyle.padding = new RectOffset(10, 10, 10, 10);
            
            // Success style
            successStyle = new GUIStyle(GUI.skin.box);
            successStyle.normal.textColor = Color.white;
            successStyle.normal.background = MakeColorTexture(new Color(0.0f, 0.6f, 0.2f, 1.0f));
            successStyle.fontSize = 16;
            successStyle.padding = new RectOffset(10, 10, 10, 10);
        }
        
        /// <summary>
        /// Draw a specific subtab's content.
        /// </summary>
        protected virtual void DrawSubtab(int subtabIndex)
        {
            // Override in derived classes to implement specific subtab drawing
            DrawPanelContent();
        }
        
        /// <summary>
        /// Draw the main panel content.
        /// </summary>
        protected abstract void DrawPanelContent();
        
        /// <summary>
        /// Create a solid-colored texture for UI elements.
        /// </summary>
        protected Texture2D MakeColorTexture(Color color)
        {
            Texture2D texture = new Texture2D(1, 1);
            texture.SetPixel(0, 0, color);
            texture.Apply();
            return texture;
        }
        
        /// <summary>
        /// Draw a standard button with configurable height.
        /// </summary>
        protected bool DrawButton(string text, float height = 60)
        {
            return GUILayout.Button(text, GUILayout.Height(height));
        }
        
        /// <summary>
        /// Draw a standard error message box.
        /// </summary>
        protected void DrawErrorBox(string title, string message)
        {
            GUILayout.BeginVertical(errorStyle);
            GUILayout.Label(title, titleStyle);
            GUILayout.Space(5);
            GUILayout.Label(message);
            GUILayout.EndVertical();
        }
        
        /// <summary>
        /// Draw a standard warning message box.
        /// </summary>
        protected void DrawWarningBox(string title, string message)
        {
            GUILayout.BeginVertical(warningStyle);
            GUILayout.Label(title, titleStyle);
            GUILayout.Space(5);
            GUILayout.Label(message);
            GUILayout.EndVertical();
        }
        
        /// <summary>
        /// Draw a standard success message box.
        /// </summary>
        protected void DrawSuccessBox(string title, string message)
        {
            GUILayout.BeginVertical(successStyle);
            GUILayout.Label(title, titleStyle);
            GUILayout.Space(5);
            GUILayout.Label(message);
            GUILayout.EndVertical();
        }
    }
}
