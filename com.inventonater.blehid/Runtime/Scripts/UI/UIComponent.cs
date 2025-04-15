using System;
using System.Collections;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Base class for UI components in the BLE HID system.
    /// Each component handles a specific section of the UI.
    /// </summary>
    public abstract class UIComponent
    {
        protected BleHidManager BleHidManager { get; private set; }
        protected LoggingManager Logger { get; private set; }
        protected bool isEditorMode => Application.isEditor;

        /// <summary>
        /// Initialize the UI component with required dependencies
        /// </summary>
        public virtual void Initialize(BleHidManager bleHidManager, LoggingManager logger)
        {
            BleHidManager = bleHidManager;
            Logger = logger;
        }
    }
}
