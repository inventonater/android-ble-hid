using System;
using Unity.Profiling;
using UnityEngine;

namespace Inventonater.BleHid
{
    [Serializable]
    public class MouseBridge
    {
        private BleHidManager _manager;
        public MouseBridge(BleHidManager manager) => _manager = manager;

        static readonly ProfilerMarker _marker = new("BleHid.MouseBridge.MoveMouse");

        public bool MoveMouse(int deltaX, int deltaY)
        {
            using var profilerMarker = _marker.Auto();

            if (deltaX == 0 && deltaY == 0) return false;

            if (deltaX < -127 || deltaX > 127 || deltaY < -127 || deltaY > 127)
            {
                string message = $"Mouse movement values out of range: {deltaX}, {deltaY}";
                Debug.LogError(message);
                _manager.LastErrorMessage = message;
                _manager.LastErrorCode = BleHidConstants.ERROR_MOUSE_MOVEMENT_OUT_OF_RANGE;
                _manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_MOUSE_MOVEMENT_OUT_OF_RANGE, message);
            }

            if (!_manager.IsConnected) return false;

            try { return _manager.BleInitializer.Call<bool>("moveMouse", deltaX, deltaY); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
        public bool MoveMouse(float deltaX, float deltaY) => MoveMouse(Mathf.RoundToInt(deltaX), Mathf.RoundToInt(deltaY));

        public bool PressMouseButton(int button)
        {
            if (!_manager.ConfirmIsConnected()) return false;

            try { return _manager.BleInitializer.Call<bool>("pressMouseButton", button); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        public bool ReleaseMouseButton(int button)
        {
            if (!_manager.ConfirmIsConnected()) return false;

            try { return _manager.BleInitializer.Call<bool>("releaseMouseButton", button); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        public bool ClickMouseButton(int button)
        {
            if (!_manager.ConfirmIsConnected()) return false;

            try { return _manager.BleInitializer.Call<bool>("clickMouseButton", button); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
    }
}
