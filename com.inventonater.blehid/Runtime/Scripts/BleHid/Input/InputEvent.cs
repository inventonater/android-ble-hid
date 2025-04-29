using System;

namespace Inventonater.BleHid
{
    [Serializable]
    public readonly struct InputEvent : IEquatable<InputEvent>
    {
        public readonly Id id;
        public readonly Phase phase;
        public readonly Direction direction;

        public InputEvent(Id id, Phase phase)
        {
            this.id = id;
            this.phase = phase;
            this.direction = Direction.None;
        }

        public InputEvent(Direction direction)
        {
            this.id = Id.Primary;
            this.phase = Phase.None;
            this.direction = direction;
        }

        public enum Id
        {
            Primary = 0, // Left mouse button
            Secondary = 1, // Right mouse button
            Tertiary = 2 // Middle mouse button
        }

        public enum Phase
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

        public bool IsPress => phase == Phase.Press;
        public bool IsRelease => phase == Phase.Release;
        public bool IsHoldBegin => phase == Phase.HoldBegin;
        public bool IsHoldEnd => phase == Phase.HoldEnd;
        public bool IsTap => phase == Phase.Tap;
        public bool IsDoubleTap => phase == Phase.DoubleTap;
        public bool IsLongPress => phase == Phase.LongPress;

        public static readonly InputEvent PrimaryPress = new(Id.Primary, Phase.Press);
        public static readonly InputEvent PrimaryRelease = new(Id.Primary, Phase.Release);
        public static readonly InputEvent PrimaryTap = new(Id.Primary, Phase.Tap);
        public static readonly InputEvent PrimaryDoubleTap = new(Id.Primary, Phase.DoubleTap);
        public static readonly InputEvent PrimaryLongPress = new(Id.Primary, Phase.LongPress);

        public static readonly InputEvent SecondaryPress = new(Id.Secondary, Phase.Press);
        public static readonly InputEvent SecondaryRelease = new(Id.Secondary, Phase.Release);
        public static readonly InputEvent SecondaryTap = new(Id.Secondary, Phase.Tap);
        public static readonly InputEvent SecondaryDoubleTap = new(Id.Secondary, Phase.DoubleTap);
        public static readonly InputEvent SecondaryLongPress = new(Id.Secondary, Phase.LongPress);

        public static readonly InputEvent TertiaryPress = new(Id.Tertiary, Phase.Press);
        public static readonly InputEvent TertiaryRelease = new(Id.Tertiary, Phase.Release);
        public static readonly InputEvent TertiaryTap = new(Id.Tertiary, Phase.Tap);
        public static readonly InputEvent TertiaryDoubleTap = new(Id.Tertiary, Phase.DoubleTap);
        public static readonly InputEvent TertiaryLongPress = new(Id.Tertiary, Phase.LongPress);

        public static readonly InputEvent Up = new(Direction.Up);
        public static readonly InputEvent Right = new(Direction.Right);
        public static readonly InputEvent Down = new(Direction.Down);
        public static readonly InputEvent Left = new(Direction.Left);

        public static InputEvent Get(Id id, Phase phase) => new(id, phase);

        public static readonly InputEvent None = default;
        public bool Equals(InputEvent other) => id == other.id && phase == other.phase && direction == other.direction;
        public override bool Equals(object obj) => obj is InputEvent other && Equals(other);
        public override int GetHashCode() => HashCode.Combine((int)id, (int)phase, (int)direction);
        public static bool operator ==(InputEvent left, InputEvent right) => left.Equals(right);
        public static bool operator !=(InputEvent left, InputEvent right) => !left.Equals(right);
        
        public override string ToString()
        {
            if (direction != Direction.None)
            {
                return $"Direction.{direction}";
            }
            
            if (phase == Phase.None)
            {
                return "None";
            }
            
            return $"{id}.{phase}";
        }
    }
}
