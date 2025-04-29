namespace Inventonater.BleHid
{
    public enum EInputEvent
    {
        None,

        PrimaryPress,
        PrimaryRelease,
        PrimaryTap,
        PrimaryDoubleTap,
        PrimaryLongPress,

        SecondaryPress,
        SecondaryRelease,
        SecondaryTap,
        SecondaryDoubleTap,
        SecondaryLongPress,

        TertiaryPress,
        TertiaryRelease,
        TertiaryTap,
        TertiaryDoubleTap,
        TertiaryLongPress,

        Up,
        Right,
        Down,
        Left,
    }

    public static class InputEventExtensions
    {
        public static InputEvent ToInputEvent(this EInputEvent inputEvent)
        {
            switch (inputEvent)
            {
                case EInputEvent.PrimaryPress: return InputEvent.PrimaryPress;
                case EInputEvent.PrimaryRelease: return InputEvent.PrimaryRelease;
                case EInputEvent.PrimaryTap: return InputEvent.PrimaryTap;
                case EInputEvent.PrimaryDoubleTap: return InputEvent.PrimaryDoubleTap;
                case EInputEvent.PrimaryLongPress: return InputEvent.PrimaryLongPress;
                case EInputEvent.SecondaryPress: return InputEvent.SecondaryPress;
                case EInputEvent.SecondaryRelease: return InputEvent.SecondaryRelease;
                case EInputEvent.SecondaryTap: return InputEvent.SecondaryTap;
                case EInputEvent.SecondaryDoubleTap: return InputEvent.SecondaryDoubleTap;
                case EInputEvent.SecondaryLongPress: return InputEvent.SecondaryLongPress;
                case EInputEvent.TertiaryPress: return InputEvent.TertiaryPress;
                case EInputEvent.TertiaryRelease: return InputEvent.TertiaryRelease;
                case EInputEvent.TertiaryTap: return InputEvent.TertiaryTap;
                case EInputEvent.TertiaryDoubleTap: return InputEvent.TertiaryDoubleTap;
                case EInputEvent.TertiaryLongPress: return InputEvent.TertiaryLongPress;
                case EInputEvent.Up: return InputEvent.Up;
                case EInputEvent.Right: return InputEvent.Right;
                case EInputEvent.Down: return InputEvent.Down;
                case EInputEvent.Left: return InputEvent.Left;
            }

            return InputEvent.None;
        }

        public static EInputEvent ToEInputEvent(this InputEvent inputEvent)
        {
            if (inputEvent.direction == InputEvent.Direction.Up) return EInputEvent.Up;
            if (inputEvent.direction == InputEvent.Direction.Down) return EInputEvent.Down;
            if (inputEvent.direction == InputEvent.Direction.Left) return EInputEvent.Left;
            if (inputEvent.direction == InputEvent.Direction.Right) return EInputEvent.Right;

            if (inputEvent.id == InputEvent.Id.Primary)
            {
                if (inputEvent.phase == InputEvent.Phase.Press) return EInputEvent.PrimaryPress;
                if (inputEvent.phase == InputEvent.Phase.Release) return EInputEvent.PrimaryRelease;
                if (inputEvent.phase == InputEvent.Phase.Tap) return EInputEvent.PrimaryTap;
                if (inputEvent.phase == InputEvent.Phase.DoubleTap) return EInputEvent.PrimaryDoubleTap;
                if (inputEvent.phase == InputEvent.Phase.LongPress) return EInputEvent.PrimaryLongPress;
            }

            if (inputEvent.id == InputEvent.Id.Secondary)
            {
                if (inputEvent.phase == InputEvent.Phase.Press) return EInputEvent.SecondaryPress;
                if (inputEvent.phase == InputEvent.Phase.Release) return EInputEvent.SecondaryRelease;
                if (inputEvent.phase == InputEvent.Phase.Tap) return EInputEvent.SecondaryTap;
                if (inputEvent.phase == InputEvent.Phase.DoubleTap) return EInputEvent.SecondaryDoubleTap;
                if (inputEvent.phase == InputEvent.Phase.LongPress) return EInputEvent.SecondaryLongPress;
            }

            if (inputEvent.id == InputEvent.Id.Tertiary)
            {
                if (inputEvent.phase == InputEvent.Phase.Press) return EInputEvent.TertiaryPress;
                if (inputEvent.phase == InputEvent.Phase.Release) return EInputEvent.TertiaryRelease;
                if (inputEvent.phase == InputEvent.Phase.Tap) return EInputEvent.TertiaryTap;
                if (inputEvent.phase == InputEvent.Phase.DoubleTap) return EInputEvent.TertiaryDoubleTap;
                if (inputEvent.phase == InputEvent.Phase.LongPress) return EInputEvent.TertiaryLongPress;
            }

            return EInputEvent.None;
        }
    }
}
