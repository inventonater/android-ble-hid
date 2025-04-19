using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Handles keyboard input functionality for BLE HID.
    /// </summary>
    public class KeyboardBridge
    {
        private BleHidManager manager;

        public KeyboardBridge(BleHidManager manager)
        {
            this.manager = manager;
        }

        /// <summary>
        /// Send a keyboard key press and release.
        /// </summary>
        /// <param name="keyCode">HID key code (see BleHidConstants)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool SendKey(byte keyCode)
        {
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.Call<bool>("sendKey", (int)keyCode); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        /// <summary>
        /// Send a keyboard key with modifier keys.
        /// </summary>
        /// <param name="keyCode">HID key code (see BleHidConstants)</param>
        /// <param name="modifiers">Modifier key bit flags (see BleHidConstants)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool SendKeyWithModifiers(byte keyCode, byte modifiers)
        {
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.Call<bool>("sendKeyWithModifiers", (int)keyCode, (int)modifiers); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        /// <summary>
        /// Type a string of text.
        /// </summary>
        /// <param name="text">The text to type</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool TypeText(string text)
        {
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.Call<bool>("typeText", text); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
    }
}
