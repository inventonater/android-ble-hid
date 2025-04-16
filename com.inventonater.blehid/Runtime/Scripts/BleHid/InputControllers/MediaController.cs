using System;
using UnityEngine;

namespace Inventonater.BleHid.InputControllers
{
    /// <summary>
    /// Handles media control functionality for BLE HID.
    /// </summary>
    public class MediaController
    {
        private BleHidManager manager;

        public MediaController(BleHidManager manager)
        {
            this.manager = manager;
        }

        /// <summary>
        /// Send media play/pause command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool PlayPause()
        {
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.BridgeInstance.Call<bool>("playPause"); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        /// <summary>
        /// Send media next track command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool NextTrack()
        {
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.BridgeInstance.Call<bool>("nextTrack"); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        /// <summary>
        /// Send media previous track command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool PreviousTrack()
        {
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.BridgeInstance.Call<bool>("previousTrack"); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        /// <summary>
        /// Send media volume up command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool VolumeUp()
        {
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.BridgeInstance.Call<bool>("volumeUp"); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        /// <summary>
        /// Send media volume down command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool VolumeDown()
        {
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.BridgeInstance.Call<bool>("volumeDown"); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        /// <summary>
        /// Send media mute command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool Mute()
        {
            if (!manager.ConfirmIsConnected()) return false;

            try { return manager.BleInitializer.BridgeInstance.Call<bool>("mute"); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
    }
}
