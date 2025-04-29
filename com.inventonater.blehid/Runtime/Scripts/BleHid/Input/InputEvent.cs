using System;

namespace Inventonater.BleHid
{
    [Serializable]
    public readonly struct InputEvent : IEquatable<InputEvent>
    {
        public readonly Id id;
        public readonly Temporal temporal;
        public readonly Direction direction;

        public InputEvent(Id id, Temporal temporal)
        {
            this.id = id;
            this.temporal = temporal;
            this.direction = Direction.None;
        }

        public InputEvent(Direction direction)
        {
            this.id = Id.Primary;
            this.temporal = Temporal.None;
            this.direction = direction;
        }

        public enum Id
        {
            Primary = 0, // Left mouse button
            Secondary = 1, // Right mouse button
            Tertiary = 2 // Middle mouse button
        }

        public enum Temporal
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

        public enum Direction
        {
            None,
            Up,
            Right,
            Down,
            Left
        }

        public bool IsPress => temporal == Temporal.Press;
        public bool IsRelease => temporal == Temporal.Release;
        public bool IsHoldBegin => temporal == Temporal.HoldBegin;
        public bool IsHoldEnd => temporal == Temporal.HoldEnd;
        public bool IsTap => temporal == Temporal.Tap;
        public bool IsDoubleTap => temporal == Temporal.DoubleTap;
        public bool IsLongPress => temporal == Temporal.LongPress;

        public static readonly InputEvent PrimaryPress = new (Id.Primary, Temporal.Press);
        public static readonly InputEvent PrimaryRelease = new (Id.Primary, Temporal.Release);
        public static readonly InputEvent PrimaryTap = new (Id.Primary, Temporal.Tap);
        public static readonly InputEvent PrimaryDoubleTap = new (Id.Primary, Temporal.DoubleTap);
        public static readonly InputEvent PrimaryLongPress = new (Id.Primary, Temporal.LongPress);

        public static readonly InputEvent SecondaryPress = new (Id.Secondary, Temporal.Press);
        public static readonly InputEvent SecondaryRelease = new (Id.Secondary, Temporal.Release);
        public static readonly InputEvent SecondaryTap = new (Id.Secondary, Temporal.Tap);
        public static readonly InputEvent SecondaryDoubleTap = new (Id.Secondary, Temporal.DoubleTap);
        public static readonly InputEvent SecondaryLongPress = new (Id.Secondary, Temporal.LongPress);

        public static readonly InputEvent TertiaryPress = new (Id.Tertiary, Temporal.Press);
        public static readonly InputEvent TertiaryRelease = new (Id.Tertiary, Temporal.Release);
        public static readonly InputEvent TertiaryTap = new (Id.Tertiary, Temporal.Tap);
        public static readonly InputEvent TertiaryDoubleTap = new (Id.Tertiary, Temporal.DoubleTap);
        public static readonly InputEvent TertiaryLongPress = new (Id.Tertiary, Temporal.LongPress);

        public static readonly InputEvent Up = new(Direction.Up);
        public static readonly InputEvent Right = new(Direction.Right);
        public static readonly InputEvent Down = new(Direction.Down);
        public static readonly InputEvent Left = new(Direction.Left);

        public static InputEvent Get(Id id, Temporal temporal) => new(id, temporal);

        public static InputEvent None { get; } = default;
        public bool Equals(InputEvent other) => id == other.id && temporal == other.temporal && direction == other.direction;
        public override bool Equals(object obj) => obj is InputEvent other && Equals(other);
        public override int GetHashCode() => HashCode.Combine((int)id, (int)temporal, (int)direction);
        public static bool operator ==(InputEvent left, InputEvent right) => left.Equals(right);
        public static bool operator !=(InputEvent left, InputEvent right) => !left.Equals(right);
    }
}
