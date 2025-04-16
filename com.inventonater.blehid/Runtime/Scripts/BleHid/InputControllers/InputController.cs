using UnityEngine;

namespace Inventonater.BleHid.InputControllers
{
    /// <summary>
    /// Main controller class that manages all input functionality for BLE HID.
    /// Acts as a facade for keyboard, mouse, and media controllers.
    /// </summary>
    public class InputController
    {
        public InputController(BleHidManager manager)
        {
            Keyboard = new KeyboardController(manager);
            Mouse = new MouseController(manager);
            Media = new MediaController(manager);
        }

        public KeyboardController Keyboard { get; }
        public MouseController Mouse { get; }
        public MediaController Media { get; }
    }
}
