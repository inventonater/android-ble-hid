using System;
using UnityEngine;

namespace Inventonater
{
    public interface IInputSourceDevice
    {
        string Name { get; }
        event Action<ButtonEvent> EmitInputEvent;
        event Action<Vector3> EmitPositionDelta;
        void InputDeviceEnabled();
        void InputDeviceDisabled();
    }
}
