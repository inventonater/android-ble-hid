using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    [Serializable]
    public class KeyboardBridge
    {
        private JavaBridge _java;
        public KeyboardBridge(JavaBridge java) => _java = java;
        public void SendKey(byte keyCode) => _java.Call("sendKey", (int)keyCode);
        public void SendKeyWithModifiers(byte keyCode, byte modifiers) => _java.Call("sendKeyWithModifiers", (int)keyCode, (int)modifiers);
        public void TypeText(string text) => _java.Call("typeText", text);
    }
}
