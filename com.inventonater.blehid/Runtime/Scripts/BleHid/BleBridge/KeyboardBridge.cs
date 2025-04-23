using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    [Serializable]
    public class KeyboardBridge
    {
        private JavaBridge _java;
        public KeyboardBridge(JavaBridge java) => _java = java;
        public bool SendKey(byte keyCode) => _java.Call<bool>("sendKey", (int)keyCode);
        public bool SendKeyWithModifiers(byte keyCode, byte modifiers) => _java.Call<bool>("sendKeyWithModifiers", (int)keyCode, (int)modifiers);
        public bool TypeText(string text) => _java.Call<bool>("typeText", text);
    }
}
