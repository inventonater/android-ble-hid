using System;

namespace Inventonater.BleHid
{
    [Serializable]
    public class KeyboardBridge
    {
        private JavaBridge _java;
        public KeyboardBridge(JavaBridge java) => _java = java;

        public void SendKey(/* BleHidConstants */int keyCode) => _java.Call("sendKey", keyCode);
        public void SendKey(HidKeyCode keyCode) => SendKey((int)keyCode);
        public void SendKeyWithModifiers(HidKeyCode keyCode, HidKeyModifier modifiers) => _java.Call("sendKeyWithModifiers", (int)keyCode, (int)modifiers);
        public void TypeText(string text) => _java.Call("typeText", text);
        
        [MappableAction(id: EInputAction.KeyboardEnter, displayName: "Send Enter", description: "Send Enter key")]
        public void EnterKey() => SendKey(HidKeyCode.Enter);
        
        [MappableAction(id: EInputAction.KeyboardEscape, displayName: "Send Escape", description: "Send Escape key")]
        public void EscapeKey() => SendKey(HidKeyCode.Escape);
        
        [MappableAction(id: EInputAction.KeyboardSpace, displayName: "Send Space", description: "Send Space key")]
        public void SpaceKey() => SendKey(HidKeyCode.Space);
        
        [MappableAction(id: EInputAction.KeyboardTab, displayName: "Send Tab", description: "Send Tab key")]
        public void TabKey() => SendKey(HidKeyCode.Tab);
        
        [MappableAction(id: EInputAction.KeyboardBackspace, displayName: "Send Backspace", description: "Send Backspace key")]
        public void BackspaceKey() => SendKey(HidKeyCode.Backspace);
        
        [MappableAction(id: EInputAction.KeyboardArrowUp, displayName: "Send Arrow Up", description: "Send Arrow Up key")]
        public void ArrowUpKey() => SendKey(HidKeyCode.UpArrow);
        
        [MappableAction(id: EInputAction.KeyboardArrowDown, displayName: "Send Arrow Down", description: "Send Arrow Down key")]
        public void ArrowDownKey() => SendKey(HidKeyCode.DownArrow);
        
        [MappableAction(id: EInputAction.KeyboardArrowLeft, displayName: "Send Arrow Left", description: "Send Arrow Left key")]
        public void ArrowLeftKey() => SendKey(HidKeyCode.LeftArrow);
        
        [MappableAction(id: EInputAction.KeyboardArrowRight, displayName: "Send Arrow Right", description: "Send Arrow Right key")]
        public void ArrowRightKey() => SendKey(HidKeyCode.RightArrow);
        
        // Common key combinations
        [MappableAction(id: EInputAction.KeyboardCopy, displayName: "Copy", description: "Send Copy command (Ctrl+C)")]
        public void Copy() => SendKeyWithModifiers(HidKeyCode.C, HidKeyModifier.Control);
        
        [MappableAction(id: EInputAction.KeyboardPaste, displayName: "Paste", description: "Send Paste command (Ctrl+V)")]
        public void Paste() => SendKeyWithModifiers(HidKeyCode.V, HidKeyModifier.Control);
        
        [MappableAction(id: EInputAction.KeyboardCut, displayName: "Cut", description: "Send Cut command (Ctrl+X)")]
        public void Cut() => SendKeyWithModifiers(HidKeyCode.X, HidKeyModifier.Control);
        
        [MappableAction(id: EInputAction.KeyboardSelectAll, displayName: "Select All", description: "Send Select All command (Ctrl+A)")]
        public void SelectAll() => SendKeyWithModifiers(HidKeyCode.A, HidKeyModifier.Control);
    }
}
