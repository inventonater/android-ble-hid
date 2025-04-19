using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    public enum BleHidAxis
    {
        X,
        Y,
        Z
    }

    public enum BleHidDirection
    {
        None,
        Up,
        Right,
        Down,
        Left
    }

    public readonly struct BleHidButtonEvent : IEquatable<BleHidButtonEvent>
    {
        public readonly Id id;
        public readonly Action action;

        public BleHidButtonEvent(Id id, Action action)
        {
            this.id = id;
            this.action = action;
        }

        public enum Id
        {
            Primary = 0, // Left mouse button
            Secondary = 1, // Right mouse button
            Tertiary = 2 // Middle mouse button
        }

        public enum Action
        {
            None,
            Press,
            Release,
            HoldBegin,
            HoldEnd,
            Tap,
            DoubleTap,
            LongPress,
        }

        public bool Equals(BleHidButtonEvent other) => id == other.id && action == other.action;
        public override bool Equals(object obj) => obj is BleHidButtonEvent other && Equals(other);
        public override int GetHashCode() => HashCode.Combine((int)id, (int)action);
        public static bool operator ==(BleHidButtonEvent left, BleHidButtonEvent right) => left.Equals(right);
        public static bool operator !=(BleHidButtonEvent left, BleHidButtonEvent right) => !left.Equals(right);
    }

    public interface IInputSourceDevice
    {
        string Name { get; }
        event Action<BleHidButtonEvent> WhenButtonEvent;
        event Action<Vector3> WhenPositionEvent;
        event Action<BleHidDirection> WhenDirectionEvent;
        void InputDeviceEnabled();
        void InputDeviceDisabled();
    }

    public static class BleHidButtonExtensions
    {
        public static bool IsPress(this BleHidButtonEvent button) => button.action == BleHidButtonEvent.Action.Press;
        public static bool IsRelease(this BleHidButtonEvent button) => button.action == BleHidButtonEvent.Action.Release;
    }
}
