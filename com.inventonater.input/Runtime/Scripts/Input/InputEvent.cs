using System;
using System.Collections.Generic;
using System.ComponentModel;
using Newtonsoft.Json;
using UnityEngine;

namespace Inventonater
{
    public enum GestureState
    {
        None, // For non-holding events (taps, presses, etc.)
        Begin, // For the start of a hold gesture
        End // For the end of a hold gesture
    }

    [Serializable]
    public readonly struct InputEvent : IEquatable<InputEvent>
    {
        [JsonProperty(DefaultValueHandling = DefaultValueHandling.Ignore)] [DefaultValue(Button.Primary)]
        public readonly Button button;

        [JsonProperty(DefaultValueHandling = DefaultValueHandling.Ignore)] [DefaultValue(Phase.None)]
        public readonly Phase phase;

        [JsonProperty(DefaultValueHandling = DefaultValueHandling.Ignore)] [DefaultValue(Direction.None)]
        public readonly Direction direction;

        [JsonProperty(DefaultValueHandling = DefaultValueHandling.Ignore)] [DefaultValue(GestureState.None)]
        public readonly GestureState state; // None for non-holding events, Begin/End for hold-type phases

        public InputEvent(Direction direction) : this(Button.Primary, Phase.None, direction, GestureState.None) { }
        public InputEvent(Button button, Phase phase) : this(button, phase, Direction.None, GestureState.None) { }
        public InputEvent(Button button, Phase phase, bool isHolding) : this(button, phase, Direction.None, isHolding ? GestureState.Begin : GestureState.End) { }
        public InputEvent(Button button, Phase phase, GestureState state) : this(button, phase, Direction.None, state) { }
        private InputEvent(Button button, Phase phase, Direction direction, GestureState state)
        {
            this.button = button;
            this.phase = phase;
            this.direction = direction;
            this.state = state;
            if (state == GestureState.None && IsAnyHold(phase)) LoggingManager.Instance.Error($"Malformed InputEvent {ToString()}");
        }

        private static bool IsAnyHold(Phase phase) => phase is Phase.Hold or Phase.DoubleTap or Phase.TripleTap;

        public enum Button
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
            TapHold, // New: press-release-presshold sequence
            DoubleTapHold, // New: press-release-press-release-presshold sequence
        }

        public enum Direction
        {
            None,
            Up,
            Right,
            Down,
            Left
        }

        [JsonIgnore] public bool IsPrimary => button == Button.Primary;
        [JsonIgnore] public bool IsSecondary => button == Button.Secondary;
        [JsonIgnore] public bool IsTertiary => button == Button.Tertiary;

        [JsonIgnore] public bool IsPress => phase == Phase.Press;
        [JsonIgnore] public bool IsRelease => phase == Phase.Release;
        [JsonIgnore] public bool IsSingleTap => phase == Phase.SingleTap;
        [JsonIgnore] public bool IsDoubleTap => phase == Phase.DoubleTap;
        [JsonIgnore] public bool IsTripleTap => phase == Phase.TripleTap;

        // Hold events with begin/end state
        [JsonIgnore] public bool IsHold => phase == Phase.Hold;
        [JsonIgnore] public bool IsHoldBegin => phase == Phase.Hold && state == GestureState.Begin;
        [JsonIgnore] public bool IsHoldEnd => phase == Phase.Hold && state == GestureState.End;

        // Complex sequence events with begin/end state
        [JsonIgnore] public bool IsTapHold => phase == Phase.TapHold;
        [JsonIgnore] public bool IsTapHoldBegin => phase == Phase.TapHold && state == GestureState.Begin;
        [JsonIgnore] public bool IsTapHoldEnd => phase == Phase.TapHold && state == GestureState.End;

        [JsonIgnore] public bool IsDoubleTapHold => phase == Phase.DoubleTapHold;
        [JsonIgnore] public bool IsDoubleTapHoldBegin => phase == Phase.DoubleTapHold && state == GestureState.Begin;
        [JsonIgnore] public bool IsDoubleTapHoldEnd => phase == Phase.DoubleTapHold && state == GestureState.End;

        // Backward compatibility
        [JsonIgnore] public bool IsTap => IsSingleTap;
        [JsonIgnore] public bool IsLongPress => IsHold;

        [JsonIgnore] public bool IsLeft => direction == Direction.Left;
        [JsonIgnore] public bool IsRight => direction == Direction.Right;
        [JsonIgnore] public bool IsUp => direction == Direction.Up;
        [JsonIgnore] public bool IsDown => direction == Direction.Down;

        // Primary button events
        public static readonly InputEvent PrimaryPress = new(Button.Primary, Phase.Press);
        public static readonly InputEvent PrimaryRelease = new(Button.Primary, Phase.Release);
        public static readonly InputEvent PrimarySingleTap = new(Button.Primary, Phase.SingleTap);
        public static readonly InputEvent PrimaryDoubleTap = new(Button.Primary, Phase.DoubleTap);
        public static readonly InputEvent PrimaryTripleTap = new(Button.Primary, Phase.TripleTap);

        // Primary hold events with begin/end variants
        public static readonly InputEvent PrimaryHoldBegin = new(Button.Primary, Phase.Hold, state: GestureState.Begin);
        public static readonly InputEvent PrimaryHoldEnd = new(Button.Primary, Phase.Hold, state: GestureState.End);

        // Primary complex sequence events
        public static readonly InputEvent PrimaryTapHoldBegin = new(Button.Primary, Phase.TapHold, state: GestureState.Begin);
        public static readonly InputEvent PrimaryTapHoldEnd = new(Button.Primary, Phase.TapHold, state: GestureState.End);
        public static readonly InputEvent PrimaryDoubleTapHoldBegin = new(Button.Primary, Phase.DoubleTapHold, state: GestureState.Begin);
        public static readonly InputEvent PrimaryDoubleTapHoldEnd = new(Button.Primary, Phase.DoubleTapHold, state: GestureState.End);

        // Backward compatibility
        public static readonly InputEvent PrimaryTap = PrimarySingleTap;
        public static readonly InputEvent PrimaryLongPress = PrimaryHoldBegin;

        // Secondary button events
        public static readonly InputEvent SecondaryPress = new(Button.Secondary, Phase.Press);
        public static readonly InputEvent SecondaryRelease = new(Button.Secondary, Phase.Release);
        public static readonly InputEvent SecondarySingleTap = new(Button.Secondary, Phase.SingleTap);
        public static readonly InputEvent SecondaryDoubleTap = new(Button.Secondary, Phase.DoubleTap);
        public static readonly InputEvent SecondaryTripleTap = new(Button.Secondary, Phase.TripleTap);

        // Secondary hold events with begin/end variants
        public static readonly InputEvent SecondaryHoldBegin = new(Button.Secondary, Phase.Hold, state: GestureState.Begin);
        public static readonly InputEvent SecondaryHoldEnd = new(Button.Secondary, Phase.Hold, state: GestureState.End);

        // Secondary complex sequence events
        public static readonly InputEvent SecondaryTapHoldBegin = new(Button.Secondary, Phase.TapHold, state: GestureState.Begin);
        public static readonly InputEvent SecondaryTapHoldEnd = new(Button.Secondary, Phase.TapHold, state: GestureState.End);
        public static readonly InputEvent SecondaryDoubleTapHoldBegin = new(Button.Secondary, Phase.DoubleTapHold, state: GestureState.Begin);
        public static readonly InputEvent SecondaryDoubleTapHoldEnd = new(Button.Secondary, Phase.DoubleTapHold, state: GestureState.End);

        // Backward compatibility
        public static readonly InputEvent SecondaryTap = SecondarySingleTap;
        public static readonly InputEvent SecondaryLongPress = SecondaryHoldBegin;

        // Tertiary button events
        public static readonly InputEvent TertiaryPress = new(Button.Tertiary, Phase.Press);
        public static readonly InputEvent TertiaryRelease = new(Button.Tertiary, Phase.Release);
        public static readonly InputEvent TertiarySingleTap = new(Button.Tertiary, Phase.SingleTap);
        public static readonly InputEvent TertiaryDoubleTap = new(Button.Tertiary, Phase.DoubleTap);
        public static readonly InputEvent TertiaryTripleTap = new(Button.Tertiary, Phase.TripleTap);

        // Tertiary hold events with begin/end variants
        public static readonly InputEvent TertiaryHoldBegin = new(Button.Tertiary, Phase.Hold, state: GestureState.Begin);
        public static readonly InputEvent TertiaryHoldEnd = new(Button.Tertiary, Phase.Hold, state: GestureState.End);

        // Tertiary complex sequence events
        public static readonly InputEvent TertiaryTapHoldBegin = new(Button.Tertiary, Phase.TapHold, state: GestureState.Begin);
        public static readonly InputEvent TertiaryTapHoldEnd = new(Button.Tertiary, Phase.TapHold, state: GestureState.End);
        public static readonly InputEvent TertiaryDoubleTapHoldBegin = new(Button.Tertiary, Phase.DoubleTapHold, state: GestureState.Begin);
        public static readonly InputEvent TertiaryDoubleTapHoldEnd = new(Button.Tertiary, Phase.DoubleTapHold, state: GestureState.End);

        // Backward compatibility
        public static readonly InputEvent TertiaryTap = TertiarySingleTap;
        public static readonly InputEvent TertiaryLongPress = TertiaryHoldBegin;

        public static readonly InputEvent Up = new(direction: Direction.Up);
        public static readonly InputEvent Right = new(direction: Direction.Right);
        public static readonly InputEvent Down = new(direction: Direction.Down);
        public static readonly InputEvent Left = new(direction: Direction.Left);

        public static InputEvent Get(Button button, Phase phase) => new(button, phase);

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

        public bool Equals(InputEvent other) => button == other.button && phase == other.phase && direction == other.direction && state == other.state;
        public override bool Equals(object obj) => obj is InputEvent other && Equals(other);
        public override int GetHashCode() => HashCode.Combine((int)button, (int)phase, (int)direction, (int)state);
        public static bool operator ==(InputEvent left, InputEvent right) => left.Equals(right);
        public static bool operator !=(InputEvent left, InputEvent right) => !left.Equals(right);

        public override string ToString()
        {
            if (direction != Direction.None) return $"Direction.{direction}";
            if (phase == Phase.None) return "None";
            return state != GestureState.None ? $"{button}.{phase}.{state}" : $"{button}.{phase}";
        }
    }
}
