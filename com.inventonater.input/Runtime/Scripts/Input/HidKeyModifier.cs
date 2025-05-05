using System;

namespace Inventonater
{
    [Flags]
    public enum HidKeyModifier : byte
    {
        None = 0x00,
        LeftControl = 0x01,
        LeftShift = 0x02,
        LeftAlt = 0x04,
        LeftGui = 0x08,
        RightControl = 0x10,
        RightShift = 0x20,
        RightAlt = 0x40,
        RightGui = 0x80,
        
        // Convenience combinations
        Control = LeftControl | RightControl,
        Shift = LeftShift | RightShift,
        Alt = LeftAlt | RightAlt,
        Gui = LeftGui | RightGui
    }
}
