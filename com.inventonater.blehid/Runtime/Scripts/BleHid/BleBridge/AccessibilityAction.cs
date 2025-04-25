using System;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Enum for accessibility actions that can be performed on nodes.
    /// These values match the Android AccessibilityNodeInfo action constants.
    /// </summary>
    public enum AccessibilityAction
    {
        /// <summary>
        /// Action that gives input focus to the node.
        /// </summary>
        Focus = 1,
        
        /// <summary>
        /// Action that clears input focus from the node.
        /// </summary>
        ClearFocus = 2,
        
        /// <summary>
        /// Action that selects the node.
        /// </summary>
        Select = 4,
        
        /// <summary>
        /// Action that clears the selection from the node.
        /// </summary>
        ClearSelection = 8,
        
        /// <summary>
        /// Action that clicks on the node.
        /// </summary>
        Click = 16,
        
        /// <summary>
        /// Action that long clicks on the node.
        /// </summary>
        LongClick = 32,
        
        /// <summary>
        /// Action that gives accessibility focus to the node.
        /// </summary>
        AccessibilityFocus = 64,
        
        /// <summary>
        /// Action that clears accessibility focus from the node.
        /// </summary>
        ClearAccessibilityFocus = 128,
        
        /// <summary>
        /// Action that scrolls the node forward.
        /// </summary>
        ScrollForward = 4096,
        
        /// <summary>
        /// Action that scrolls the node backward.
        /// </summary>
        ScrollBackward = 8192,
        
        /// <summary>
        /// Action to copy the current selection to the clipboard.
        /// </summary>
        Copy = 16384,
        
        /// <summary>
        /// Action to paste the current clipboard content.
        /// </summary>
        Paste = 32768,
        
        /// <summary>
        /// Action to cut the current selection and place it to the clipboard.
        /// </summary>
        Cut = 65536,
        
        /// <summary>
        /// Action to set the selection. Performing this action with no arguments
        /// clears the selection.
        /// </summary>
        SetSelection = 131072,
        
        /// <summary>
        /// Action to expand an expandable node.
        /// </summary>
        Expand = 262144,
        
        /// <summary>
        /// Action to collapse an expandable node.
        /// </summary>
        Collapse = 524288,
        
        /// <summary>
        /// Action to dismiss a dismissable node.
        /// </summary>
        Dismiss = 1048576,
        
        /// <summary>
        /// Action that sets the text of the node. Performing this action without
        /// arguments, using null or empty CharSequence, will clear the text. This
        /// action will also put the cursor at the end of text.
        /// </summary>
        SetText = 2097152
    }
}
