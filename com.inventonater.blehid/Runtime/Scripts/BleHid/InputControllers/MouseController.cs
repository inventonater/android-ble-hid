using System;
using UnityEngine;

namespace Inventonater.BleHid.InputControllers
{
    /// <summary>
    /// Handles mouse input functionality for BLE HID.
    /// </summary>
    public class MouseController
    {
        private BleHidManager manager;
        public MouseInputProcessor MouseInputProcessor { get; }

        public MouseController(BleHidManager manager)
        {
            this.manager = manager;
            MouseInputProcessor = new MouseInputProcessor((x, y) => MoveMouse(x, y));
        }

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
                manager.LastErrorMessage = message;
                manager.LastErrorCode = BleHidConstants.ERROR_MOUSE_MOVEMENT_OUT_OF_RANGE;
                manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_MOUSE_MOVEMENT_OUT_OF_RANGE, message);
            }

            if (!manager.IsConnected)
            {
                LoggingManager.Instance.AddLogEntry($"[not connected] pointer position: ({deltaX}, {deltaY})");
                return false;
            }

            try { return manager.BleInitializer.BridgeInstance.Call<bool>("moveMouse", deltaX, deltaY); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        /// <summary>
        /// Press a mouse button without releasing it.
        /// </summary>
        /// <param name="button">Button to press (0=left, 1=right, 2=middle)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool PressMouseButton(int button)
        {
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.BridgeInstance.Call<bool>("pressMouseButton", button); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        /// <summary>
        /// Release a specific mouse button.
        /// </summary>
        /// <param name="button">Button to release (0=left, 1=right, 2=middle)</param>
        /// <returns>True if successful, false otherwise.</returns>
        public bool ReleaseMouseButton(int button)
        {
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.BridgeInstance.Call<bool>("releaseMouseButton", button); }
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
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.BridgeInstance.Call<bool>("clickMouseButton", button); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
    }
}
