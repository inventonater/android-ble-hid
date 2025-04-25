using System;
using Unity.Profiling;
using UnityEngine;

namespace Inventonater.BleHid
{
    [Serializable]
    public class MouseBridge
    {
        private JavaBridge _java;
        public MouseBridge(JavaBridge java) => _java = java;

        static readonly ProfilerMarker _marker = new("BleHid.MouseBridge.MoveMouse");

        public void MoveMouse(int deltaX, int deltaY)
        {
            using var profilerMarker = _marker.Auto();
            if (deltaX == 0 && deltaY == 0) return;
            if (deltaX < -127 || deltaX > 127 || deltaY < -127 || deltaY > 127) LoggingManager.Instance.Error($"Mouse movement values out of range: {deltaX}, {deltaY}");

            _java.Call<bool>("moveMouse", deltaX, deltaY);
        }

        public void MoveMouse(Vector2 delta) => MoveMouse(Mathf.RoundToInt(delta.x), Mathf.RoundToInt(delta.y));
        public bool PressMouseButton(int button) => _java.Call<bool>("pressMouseButton", button);
        public bool ReleaseMouseButton(int button) => _java.Call<bool>("releaseMouseButton", button);
        public bool ClickMouseButton(int button) => _java.Call<bool>("clickMouseButton", button);
    }
}
