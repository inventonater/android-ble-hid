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
        public abstract string TabName { get; }
        public abstract void Update();
        public abstract void DrawUI();
        public virtual void OnActivate() { }
        public virtual void OnDeactivate() { }

    }
}
