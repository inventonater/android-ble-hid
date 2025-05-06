using System;
using System.Collections.Generic;
using Newtonsoft.Json;

namespace Inventonater
{
    [Serializable]
    public readonly struct InputEvent : IEquatable<InputEvent>
    {
        public readonly Id id;
        public readonly Phase phase;
        public readonly Direction direction;

        public InputEvent(Id id = Id.Primary, Phase phase = Phase.None, Direction direction = Direction.None)
        {
            this.id = id;
            this.phase = phase;
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

        [JsonIgnore] public bool IsPress => phase == Phase.Press;
        [JsonIgnore] public bool IsRelease => phase == Phase.Release;
        [JsonIgnore] public bool IsHoldBegin => phase == Phase.HoldBegin;
        [JsonIgnore] public bool IsHoldEnd => phase == Phase.HoldEnd;
        [JsonIgnore] public bool IsTap => phase == Phase.Tap;
        [JsonIgnore] public bool IsDoubleTap => phase == Phase.DoubleTap;
        [JsonIgnore] public bool IsLongPress => phase == Phase.LongPress;

        [JsonIgnore] public bool IsLeft => direction == Direction.Left;
        [JsonIgnore] public bool IsRight => direction == Direction.Right;
        [JsonIgnore] public bool IsUp => direction == Direction.Up;
        [JsonIgnore] public bool IsDown => direction == Direction.Down;

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

        public static readonly InputEvent Up = new(direction:Direction.Up);
        public static readonly InputEvent Right = new(direction:Direction.Right);
        public static readonly InputEvent Down = new(direction:Direction.Down);
        public static readonly InputEvent Left = new(direction:Direction.Left);

        public static InputEvent Get(Id id, Phase phase) => new(id, phase);

        public static readonly InputEvent None = default;
        
        public static IEnumerable<InputEvent> GetAll()
        {
            // Return all predefined constants
            yield return None;
            
            // Button events
            yield return PrimaryPress;
            yield return PrimaryRelease;
            yield return PrimaryTap;
            yield return PrimaryDoubleTap;
            yield return PrimaryLongPress;
            
            yield return SecondaryPress;
            yield return SecondaryRelease;
            yield return SecondaryTap;
            yield return SecondaryDoubleTap;
            yield return SecondaryLongPress;
            
            yield return TertiaryPress;
            yield return TertiaryRelease;
            yield return TertiaryTap;
            yield return TertiaryDoubleTap;
            yield return TertiaryLongPress;
            
            // Direction events
            yield return Up;
            yield return Right;
            yield return Down;
            yield return Left;
        }
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
