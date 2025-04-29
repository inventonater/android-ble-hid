using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    public interface IInputSourceDevice
    {
        string Name { get; }
        event Action<InputEvent> EmitInputEvent;
        event Action<Vector3> EmitPositionDelta;
        void InputDeviceEnabled();
        void InputDeviceDisabled();
    }
}
