using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Manages media control functionality for the BLE HID service.
    /// Handles media playback and volume control.
    /// </summary>
    public class BleHidMediaManager
    {
        /// <summary>
        /// Event that is triggered when there's an error with media operations
        /// </summary>
        public event BleHidCallbackHandler.ErrorHandler OnError;
        
        /// <summary>
        /// Reference to the native plugin bridge instance
        /// </summary>
        private AndroidJavaObject bridgeInstance;
        
        /// <summary>
        /// Reference to the connection manager to check connection state
        /// </summary>
        private BleHidConnectionManager connectionManager;
        
        /// <summary>
        /// Constructor requiring the bridge instance and connection manager
        /// </summary>
        /// <param name="bridgeInstance">The Java bridge instance for the native plugin</param>
        /// <param name="connectionManager">Reference to the connection manager for state checking</param>
        public BleHidMediaManager(AndroidJavaObject bridgeInstance, BleHidConnectionManager connectionManager)
        {
            this.bridgeInstance = bridgeInstance;
            this.connectionManager = connectionManager;
        }
        
        /// <summary>
        /// Ensure that a device is connected before performing a media operation
        /// </summary>
        private bool ConfirmIsConnected()
        {
            if (connectionManager.IsConnected) return true;

            string message = "No BLE device connected";
            Debug.LogError(message);
            OnError?.Invoke(BleHidConstants.ERROR_NOT_CONNECTED, message);
            return false;
        }

        /// <summary>
        /// Send media play/pause command.
        /// </summary>
        /// <returns>True if successful, false otherwise.</returns>
        public bool PlayPause()
        {
            if (!ConfirmIsConnected()) return false;

            try { return bridgeInstance.Call<bool>("playPause"); }
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
            if (!ConfirmIsConnected()) return false;

            try { return bridgeInstance.Call<bool>("nextTrack"); }
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
            if (!ConfirmIsConnected()) return false;

            try { return bridgeInstance.Call<bool>("previousTrack"); }
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
            if (!ConfirmIsConnected()) return false;

            try { return bridgeInstance.Call<bool>("volumeUp"); }
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
            if (!ConfirmIsConnected()) return false;

            try { return bridgeInstance.Call<bool>("volumeDown"); }
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
            if (!ConfirmIsConnected()) return false;

            try { return bridgeInstance.Call<bool>("mute"); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
    }
}
