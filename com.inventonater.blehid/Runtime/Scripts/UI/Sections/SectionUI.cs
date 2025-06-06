using System;
using System.Collections;
using UnityEngine;

namespace Inventonater
{
    /// <summary>
    /// Base class for UI components in the BLE HID system.
    /// Each component handles a specific section of the UI.
    /// </summary>
    public abstract class SectionUI
    {
        protected bool IsEditorMode => Application.isEditor;
        public abstract string TabName { get; }
        public abstract void Update();
        public abstract void DrawUI();
    }
}
