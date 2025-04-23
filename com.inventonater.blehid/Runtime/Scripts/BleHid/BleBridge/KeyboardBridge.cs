using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    [Serializable]
    public class KeyboardBridge
    {
        private BleHidManager _manager;
        public KeyboardBridge(BleHidManager manager) => _manager = manager;

        public bool SendKey(byte keyCode)
        {
            if (!_manager.ConfirmIsConnected()) return false;

            try { return _manager.Bridge.Call<bool>("sendKey", (int)keyCode); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        public bool SendKeyWithModifiers(byte keyCode, byte modifiers)
        {
            if (!_manager.ConfirmIsConnected()) return false;

            try { return _manager.Bridge.Call<bool>("sendKeyWithModifiers", (int)keyCode, (int)modifiers); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        public bool TypeText(string text)
        {
            if (!_manager.ConfirmIsConnected()) return false;

            try { return _manager.Bridge.Call<bool>("typeText", text); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
    }
}
