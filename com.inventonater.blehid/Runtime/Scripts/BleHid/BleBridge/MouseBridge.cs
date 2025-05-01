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
            _java.Call("moveMouse", deltaX, deltaY);
        }
        public void MoveMouse(Vector2 delta) => MoveMouse(Mathf.RoundToInt(delta.x), Mathf.RoundToInt(delta.y));

        public void PressMouseButton(int button) => _java.Call("pressMouseButton", button);
        public void ReleaseMouseButton(int button) => _java.Call("releaseMouseButton", button);
        public void ClickMouseButton(int button) => _java.Call("clickMouseButton", button);
        
        [MappableAction(id: EInputAction.Select, displayName: "Left Click", description: "Perform a left mouse button click")]
        public void LeftClick() => ClickMouseButton(0);
        public void RightClick() => ClickMouseButton(1);
        public void MiddleClick() => ClickMouseButton(2);

        [MappableAction(id: EInputAction.PrimaryPress, displayName: "Left Press", description: "Press the left mouse button")]
        public void LeftPress() => PressMouseButton(0);

        [MappableAction(id: EInputAction.PrimaryRelease, displayName: "Left Release", description: "Release the left mouse button")]
        public void LeftRelease() => ReleaseMouseButton(0);

        [MappableAction(id: EInputAction.SecondaryPress, displayName: "Right Press", description: "Press the right mouse button")]
        public void RightPress() => PressMouseButton(1);

        [MappableAction(id: EInputAction.SecondaryRelease, displayName: "Right Release", description: "Release the right mouse button")]
        public void RightRelease() => ReleaseMouseButton(1);

        [MappableAction(id: EInputAction.SecondaryPress, displayName: "Middle Press", description: "Press the middle mouse button")]
        public void MiddlePress() => PressMouseButton(2);

        [MappableAction(id: EInputAction.SecondaryRelease, displayName: "Middle Release", description: "Release the middle mouse button")]
        public void MiddleRelease() => ReleaseMouseButton(2);
    }
}
