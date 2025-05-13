using System;

namespace Inventonater
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

        [MappableAction(id: MappableActionId.Up, displayName: "Send Arrow Up", description: "Send Arrow Up key")]
        public void ArrowUpKey() => SendKey(HidKeyCode.UpArrow);

        [MappableAction(id: MappableActionId.Down, displayName: "Send Arrow Down", description: "Send Arrow Down key")]
        public void ArrowDownKey() => SendKey(HidKeyCode.DownArrow);

        [MappableAction(id: MappableActionId.Left, displayName: "Send Arrow Left", description: "Send Arrow Left key")]
        public void ArrowLeftKey() => SendKey(HidKeyCode.LeftArrow);

        [MappableAction(id: MappableActionId.Right, displayName: "Send Arrow Right", description: "Send Arrow Right key")]
        public void ArrowRightKey() => SendKey(HidKeyCode.RightArrow);

        public void EnterKey() => SendKey(HidKeyCode.Enter);
        public void EscapeKey() => SendKey(HidKeyCode.Escape);
        public void SpaceKey() => SendKey(HidKeyCode.Space);
        public void TabKey() => SendKey(HidKeyCode.Tab);
        public void BackspaceKey() => SendKey(HidKeyCode.Backspace);
        public void Copy() => SendKeyWithModifiers(HidKeyCode.C, HidKeyModifier.Control);
        public void Paste() => SendKeyWithModifiers(HidKeyCode.V, HidKeyModifier.Control);
        public void Cut() => SendKeyWithModifiers(HidKeyCode.X, HidKeyModifier.Control);
        public void SelectAll() => SendKeyWithModifiers(HidKeyCode.A, HidKeyModifier.Control);
    }
}
