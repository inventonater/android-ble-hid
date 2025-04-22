using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    [Serializable]
    public class BleBridge
    {
        [SerializeField] private KeyboardBridge _keyboard;
        [SerializeField] private MouseBridge _mouse;
        [SerializeField] private MediaBridge _media;
        [SerializeField] private IdentityBridge _identity;

        public KeyboardBridge Keyboard => _keyboard;
        public MouseBridge Mouse => _mouse;
        public MediaBridge Media => _media;
        public IdentityBridge Identity => _identity;

        public BleBridge(BleHidManager manager)
        {
            _keyboard = new KeyboardBridge(manager);
            _mouse = new MouseBridge(manager);
            _media = new MediaBridge(manager);
            _identity = new IdentityBridge(manager);
        }
    }
}
