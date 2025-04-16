using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Manages input functionality for the BLE HID service.
    /// Handles keyboard and mouse input operations.
    /// </summary>
    public class BleHidInputManager
    {
        /// <summary>
        /// Event that is triggered when there's an error with input operations
        /// </summary>
        public event BleHidCallbackHandler.ErrorHandler OnError;
        
        /// <summary>
        /// Reference to the native plugin bridge instance
        /// </summary>
        private AndroidJavaObject bridgeInstance;
        
        /// <summary>
        /// The mouse input processor that handles filtering and scaling
        /// </summary>
        private MouseInputProcessor mouseInputProcessor;
        
        /// <summary>
        /// Gets the mouse input processor
        /// </summary>
        public MouseInputProcessor MouseInputProcessor => mouseInputProcessor;
        
        /// <summary>
        /// Reference to the connection manager to check connection state
        /// </summary>
        private BleHidConnectionManager connectionManager;
        
        /// <summary>
        /// Stores the last error message
        /// </summary>
        public string LastErrorMessage { get; private set; }
        
        /// <summary>
        /// Stores the last error code
        /// </summary>
        public int LastErrorCode { get; private set; }
        
        /// <summary>
        /// Constructor requiring the bridge instance and connection manager
        /// </summary>
        /// <param name="bridgeInstance">The Java bridge instance for the native plugin</param>
        /// <param name="connectionManager">Reference to the connection manager for state checking</param>
        public BleHidInputManager(AndroidJavaObject bridgeInstance, BleHidConnectionManager connectionManager)
        {
            this.bridgeInstance = bridgeInstance;
            this.connectionManager = connectionManager;
            this.mouseInputProcessor = new MouseInputProcessor((x, y) => MoveMouse(x, y));
        }
        
        /// <summary>
        /// Ensure that a device is connected before performing an input operation
        /// </summary>
        private bool ConfirmIsConnected()
        {
            if (connectionManager.IsConnected) return true;

            string message = "No BLE device connected";
            Debug.LogError(message);
            LastErrorMessage = message;
            LastErrorCode = BleHidConstants.ERROR_NOT_CONNECTED;
            OnError?.Invoke(BleHidConstants.ERROR_NOT_CONNECTED, message);
            return false;
        }
        
        // ----- Keyboard Input Methods -----
        
        /// <summary>
        /// Send a keyboard key press and release.
        /// </summary>
        /// <param name="keyCode">HID key code (see BleHidConstants)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool SendKey(byte keyCode)
        {
            if (!ConfirmIsConnected()) return false;

            try { return bridgeInstance.Call<bool>("sendKey", (int)keyCode); }
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
            if (!ConfirmIsConnected()) return false;

            try { return bridgeInstance.Call<bool>("sendKeyWithModifiers", (int)keyCode, (int)modifiers); }
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
            if (!ConfirmIsConnected()) return false;

            try { return bridgeInstance.Call<bool>("typeText", text); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
        
        // ----- Mouse Input Methods -----
        
        /// <summary>
        /// Send a mouse movement.
        /// </summary>
        /// <param name="deltaX">X-axis movement (-127 to 127)</param>
        /// <param name="deltaY">Y-axis movement (-127 to 127)</param>
        /// <returns>True if successful, false otherwise.</returns>
        private bool MoveMouse(int deltaX, int deltaY)
        {
            if (deltaX == 0 && deltaY == 0) return false;

            if (deltaX < -127 || deltaX > 127 || deltaY < -127 || deltaY > 127)
            {
                string message = $"Mouse movement values out of range: {deltaX}, {deltaY}";
                Debug.LogError(message);
                LastErrorMessage = message;
                LastErrorCode = BleHidConstants.ERROR_MOUSE_MOVEMENT_OUT_OF_RANGE;
                OnError?.Invoke(BleHidConstants.ERROR_MOUSE_MOVEMENT_OUT_OF_RANGE, message);
            }

            if (!connectionManager.IsConnected)
            {
                LoggingManager.Instance.AddLogEntry($"[not connected] pointer position: ({deltaX}, {deltaY})");
                return false;
            }

            try { return bridgeInstance.Call<bool>("moveMouse", deltaX, deltaY); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        /// <summary>
        /// Click a mouse button.
        /// </summary>
        /// <param name="button">Button to click (0=left, 1=right, 2=middle)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool ClickMouseButton(int button)
        {
            if (!ConfirmIsConnected()) return false;

            try { return bridgeInstance.Call<bool>("clickMouseButton", button); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
    }
}
