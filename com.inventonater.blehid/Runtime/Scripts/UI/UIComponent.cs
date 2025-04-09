using System;
using System.Collections;
using UnityEngine;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// Base class for UI components in the BLE HID system.
    /// Each component handles a specific section of the UI.
    /// </summary>
    public abstract class UIComponent
    {
        protected BleHidManager BleHidManager { get; private set; }
        protected LoggingManager Logger { get; private set; }
        protected bool IsEditorMode { get; private set; }
        
        /// <summary>
        /// Initialize the UI component with required dependencies
        /// </summary>
        public virtual void Initialize(BleHidManager bleHidManager, LoggingManager logger, bool isEditorMode)
        {
            BleHidManager = bleHidManager;
            Logger = logger;
            IsEditorMode = isEditorMode;
        }
        
        /// <summary>
        /// Draw the component's UI
        /// </summary>
        public abstract void DrawUI();
        
        /// <summary>
        /// Update logic for the component
        /// </summary>
        public virtual void Update() { }
        
        /// <summary>
        /// Helper method to create a colored texture for UI elements
        /// </summary>
        protected Texture2D MakeColorTexture(Color color)
        {
            Texture2D texture = new Texture2D(1, 1);
            texture.SetPixel(0, 0, color);
            texture.Apply();
            return texture;
        }
    }
}
