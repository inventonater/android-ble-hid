using System;
using System.Collections.Generic;
using System.ComponentModel;
using Newtonsoft.Json;

namespace Inventonater
{
    [Serializable]
    public readonly struct InputEvent : IEquatable<InputEvent>
    {
        [JsonProperty(DefaultValueHandling = DefaultValueHandling.Ignore)]
        [DefaultValue(Id.Primary)]
        public readonly Id id;
        
        [JsonProperty(DefaultValueHandling = DefaultValueHandling.Ignore)]
        [DefaultValue(Phase.None)]
        public readonly Phase phase;
        
        [JsonProperty(DefaultValueHandling = DefaultValueHandling.Ignore)]
        [DefaultValue(Direction.None)]
        public readonly Direction direction;
        
        [JsonProperty(DefaultValueHandling = DefaultValueHandling.Ignore)]
        [DefaultValue(true)]
        public readonly bool isBegin;  // true for begin, false = end (for hold-type phases)

        public InputEvent(Id id = Id.Primary, Phase phase = Phase.None, Direction direction = Direction.None, bool isBegin = true)
        {
            this.id = id;
            this.phase = phase;
            this.direction = direction;
            this.isBegin = isBegin;
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
            SingleTap,
            DoubleTap,
            TripleTap,
            Hold,
            TapHold,        // New: press-release-presshold sequence
            DoubleTapHold,  // New: press-release-press-release-presshold sequence
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
        [JsonIgnore] public bool IsSingleTap => phase == Phase.SingleTap;
        [JsonIgnore] public bool IsDoubleTap => phase == Phase.DoubleTap;
        [JsonIgnore] public bool IsTripleTap => phase == Phase.TripleTap;
        
        // Hold events with begin/end state
        [JsonIgnore] public bool IsHold => phase == Phase.Hold;
        [JsonIgnore] public bool IsHoldBegin => phase == Phase.Hold && isBegin;
        [JsonIgnore] public bool IsHoldEnd => phase == Phase.Hold && !isBegin;
        
        // Complex sequence events with begin/end state
        [JsonIgnore] public bool IsTapHold => phase == Phase.TapHold;
        [JsonIgnore] public bool IsTapHoldBegin => phase == Phase.TapHold && isBegin;
        [JsonIgnore] public bool IsTapHoldEnd => phase == Phase.TapHold && !isBegin;
        
        [JsonIgnore] public bool IsDoubleTapHold => phase == Phase.DoubleTapHold;
        [JsonIgnore] public bool IsDoubleTapHoldBegin => phase == Phase.DoubleTapHold && isBegin;
        [JsonIgnore] public bool IsDoubleTapHoldEnd => phase == Phase.DoubleTapHold && !isBegin;
        
        // Backward compatibility
        [JsonIgnore] public bool IsTap => IsSingleTap;
        [JsonIgnore] public bool IsLongPress => IsHold;

        [JsonIgnore] public bool IsLeft => direction == Direction.Left;
        [JsonIgnore] public bool IsRight => direction == Direction.Right;
        [JsonIgnore] public bool IsUp => direction == Direction.Up;
        [JsonIgnore] public bool IsDown => direction == Direction.Down;

        // Primary button events
        public static readonly InputEvent PrimaryPress = new(Id.Primary, Phase.Press);
        public static readonly InputEvent PrimaryRelease = new(Id.Primary, Phase.Release);
        public static readonly InputEvent PrimarySingleTap = new(Id.Primary, Phase.SingleTap);
        public static readonly InputEvent PrimaryDoubleTap = new(Id.Primary, Phase.DoubleTap);
        public static readonly InputEvent PrimaryTripleTap = new(Id.Primary, Phase.TripleTap);
        
        // Primary hold events with begin/end variants
        public static readonly InputEvent PrimaryHoldBegin = new(Id.Primary, Phase.Hold, isBegin: true);
        public static readonly InputEvent PrimaryHoldEnd = new(Id.Primary, Phase.Hold, isBegin: false);
        
        // Primary complex sequence events
        public static readonly InputEvent PrimaryTapHoldBegin = new(Id.Primary, Phase.TapHold, isBegin: true);
        public static readonly InputEvent PrimaryTapHoldEnd = new(Id.Primary, Phase.TapHold, isBegin: false);
        public static readonly InputEvent PrimaryDoubleTapHoldBegin = new(Id.Primary, Phase.DoubleTapHold, isBegin: true);
        public static readonly InputEvent PrimaryDoubleTapHoldEnd = new(Id.Primary, Phase.DoubleTapHold, isBegin: false);
        
        // Backward compatibility
        public static readonly InputEvent PrimaryTap = PrimarySingleTap;
        public static readonly InputEvent PrimaryLongPress = PrimaryHoldBegin;

        // Secondary button events
        public static readonly InputEvent SecondaryPress = new(Id.Secondary, Phase.Press);
        public static readonly InputEvent SecondaryRelease = new(Id.Secondary, Phase.Release);
        public static readonly InputEvent SecondarySingleTap = new(Id.Secondary, Phase.SingleTap);
        public static readonly InputEvent SecondaryDoubleTap = new(Id.Secondary, Phase.DoubleTap);
        public static readonly InputEvent SecondaryTripleTap = new(Id.Secondary, Phase.TripleTap);
        
        // Secondary hold events with begin/end variants
        public static readonly InputEvent SecondaryHoldBegin = new(Id.Secondary, Phase.Hold, isBegin: true);
        public static readonly InputEvent SecondaryHoldEnd = new(Id.Secondary, Phase.Hold, isBegin: false);
        
        // Secondary complex sequence events
        public static readonly InputEvent SecondaryTapHoldBegin = new(Id.Secondary, Phase.TapHold, isBegin: true);
        public static readonly InputEvent SecondaryTapHoldEnd = new(Id.Secondary, Phase.TapHold, isBegin: false);
        public static readonly InputEvent SecondaryDoubleTapHoldBegin = new(Id.Secondary, Phase.DoubleTapHold, isBegin: true);
        public static readonly InputEvent SecondaryDoubleTapHoldEnd = new(Id.Secondary, Phase.DoubleTapHold, isBegin: false);
        
        // Backward compatibility
        public static readonly InputEvent SecondaryTap = SecondarySingleTap;
        public static readonly InputEvent SecondaryLongPress = SecondaryHoldBegin;

        // Tertiary button events
        public static readonly InputEvent TertiaryPress = new(Id.Tertiary, Phase.Press);
        public static readonly InputEvent TertiaryRelease = new(Id.Tertiary, Phase.Release);
        public static readonly InputEvent TertiarySingleTap = new(Id.Tertiary, Phase.SingleTap);
        public static readonly InputEvent TertiaryDoubleTap = new(Id.Tertiary, Phase.DoubleTap);
        public static readonly InputEvent TertiaryTripleTap = new(Id.Tertiary, Phase.TripleTap);
        
        // Tertiary hold events with begin/end variants
        public static readonly InputEvent TertiaryHoldBegin = new(Id.Tertiary, Phase.Hold, isBegin: true);
        public static readonly InputEvent TertiaryHoldEnd = new(Id.Tertiary, Phase.Hold, isBegin: false);
        
        // Tertiary complex sequence events
        public static readonly InputEvent TertiaryTapHoldBegin = new(Id.Tertiary, Phase.TapHold, isBegin: true);
        public static readonly InputEvent TertiaryTapHoldEnd = new(Id.Tertiary, Phase.TapHold, isBegin: false);
        public static readonly InputEvent TertiaryDoubleTapHoldBegin = new(Id.Tertiary, Phase.DoubleTapHold, isBegin: true);
        public static readonly InputEvent TertiaryDoubleTapHoldEnd = new(Id.Tertiary, Phase.DoubleTapHold, isBegin: false);
        
        // Backward compatibility
        public static readonly InputEvent TertiaryTap = TertiarySingleTap;
        public static readonly InputEvent TertiaryLongPress = TertiaryHoldBegin;

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
            
            // Primary button events
            yield return PrimaryPress;
            yield return PrimaryRelease;
            yield return PrimarySingleTap;
            yield return PrimaryDoubleTap;
            yield return PrimaryTripleTap;
            yield return PrimaryHoldBegin;
            yield return PrimaryHoldEnd;
            yield return PrimaryTapHoldBegin;
            yield return PrimaryTapHoldEnd;
            yield return PrimaryDoubleTapHoldBegin;
            yield return PrimaryDoubleTapHoldEnd;
            
            // Secondary button events
            yield return SecondaryPress;
            yield return SecondaryRelease;
            yield return SecondarySingleTap;
            yield return SecondaryDoubleTap;
            yield return SecondaryTripleTap;
            yield return SecondaryHoldBegin;
            yield return SecondaryHoldEnd;
            yield return SecondaryTapHoldBegin;
            yield return SecondaryTapHoldEnd;
            yield return SecondaryDoubleTapHoldBegin;
            yield return SecondaryDoubleTapHoldEnd;
            
            // Tertiary button events
            yield return TertiaryPress;
            yield return TertiaryRelease;
            yield return TertiarySingleTap;
            yield return TertiaryDoubleTap;
            yield return TertiaryTripleTap;
            yield return TertiaryHoldBegin;
            yield return TertiaryHoldEnd;
            yield return TertiaryTapHoldBegin;
            yield return TertiaryTapHoldEnd;
            yield return TertiaryDoubleTapHoldBegin;
            yield return TertiaryDoubleTapHoldEnd;
            
            // Direction events
            yield return Up;
            yield return Right;
            yield return Down;
            yield return Left;
            
            // Backward compatibility
            yield return PrimaryTap;
            yield return PrimaryLongPress;
            yield return SecondaryTap;
            yield return SecondaryLongPress;
            yield return TertiaryTap;
            yield return TertiaryLongPress;
        }
        public bool Equals(InputEvent other) => id == other.id && phase == other.phase && direction == other.direction && isBegin == other.isBegin;
        public override bool Equals(object obj) => obj is InputEvent other && Equals(other);
        public override int GetHashCode() => HashCode.Combine((int)id, (int)phase, (int)direction, isBegin);
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
            
            // Add begin/end suffix for hold-type events
            if ((phase == Phase.Hold || phase == Phase.TapHold || phase == Phase.DoubleTapHold))
            {
                string state = isBegin ? "Begin" : "End";
                return $"{id}.{phase}.{state}";
            }
            
            return $"{id}.{phase}";
        }
    }
}
