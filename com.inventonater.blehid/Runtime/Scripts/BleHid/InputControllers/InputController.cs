using UnityEngine;

namespace Inventonater.BleHid.InputControllers
{
    /// <summary>
    /// Main controller class that manages all input functionality for BLE HID.
    /// Acts as a facade for keyboard, mouse, and media controllers.
    /// </summary>
    public class InputController
    {
        private BleHidManager manager;
        private KeyboardController keyboardController;
        private MouseController mouseController;
        private MediaController mediaController;

        public InputController(BleHidManager manager)
        {
            this.manager = manager;
            keyboardController = new KeyboardController(manager);
            mouseController = new MouseController(manager);
            mediaController = new MediaController(manager);
        }

        /// <summary>
        /// Access the keyboard controller.
        /// </summary>
        public KeyboardController Keyboard => keyboardController;

        /// <summary>
        /// Access the mouse controller.
        /// </summary>
        public MouseController Mouse => mouseController;

        /// <summary>
        /// Access the media controller.
        /// </summary>
        public MediaController Media => mediaController;

        /// <summary>
        /// Direct access to the mouse input processor.
        /// </summary>
        public MouseInputProcessor MouseInputProcessor => mouseController.MouseInputProcessor;
    }
}
