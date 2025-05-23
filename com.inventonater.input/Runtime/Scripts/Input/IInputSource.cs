using System;
using UnityEngine;

namespace Inventonater
{
    public interface IInputSource
    {
        string Name { get; }
        event Action<ButtonEvent> EmitInputEvent;
        event Action<Vector3> EmitPositionDelta;
        void InputDeviceEnabled();
        void InputDeviceDisabled();
    }
}
