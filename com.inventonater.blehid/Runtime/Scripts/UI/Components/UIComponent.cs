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
        protected BleHidManager BleHidManager => BleHidManager.Instance;
        protected LoggingManager Logger => LoggingManager.Instance;
        protected bool IsEditorMode => Application.isEditor;
        
        /// <summary>
        /// The name of the tab this component is associated with
        /// </summary>
        public string TabName { get; set; }

        public abstract void Initialize();
        public abstract void Update();

        /// <summary>
        /// Called when the component's tab becomes active
        /// </summary>
        public virtual void OnActivate() { }

        /// <summary>
        /// Called when the component's tab becomes inactive
        /// </summary>
        public virtual void OnDeactivate() { }

    }
}
