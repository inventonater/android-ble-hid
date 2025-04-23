using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    [Serializable]
    public class MediaBridge
    {
        private BleHidManager _manager;
        public MediaBridge(BleHidManager manager) => _manager = manager;

        public bool PlayPause()
        {
            if (!_manager.ConfirmIsConnected()) return false;

            try { return _manager.Bridge.Call<bool>("playPause"); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        public bool NextTrack()
        {
            if (!_manager.ConfirmIsConnected()) return false;

            try { return _manager.Bridge.Call<bool>("nextTrack"); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        public bool PreviousTrack()
        {
            if (!_manager.ConfirmIsConnected()) return false;

            try { return _manager.Bridge.Call<bool>("previousTrack"); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        public bool VolumeUp()
        {
            if (!_manager.ConfirmIsConnected()) return false;

            try { return _manager.Bridge.Call<bool>("volumeUp"); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        public bool VolumeDown()
        {
            if (!_manager.ConfirmIsConnected()) return false;

            try { return _manager.Bridge.Call<bool>("volumeDown"); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }

        public bool Mute()
        {
            if (!_manager.ConfirmIsConnected()) return false;

            try { return _manager.Bridge.Call<bool>("mute"); }
            catch (Exception e)
            {
                Debug.LogException(e);
                return false;
            }
        }
    }
}
