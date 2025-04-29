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
        [SerializeField] private ConnectionBridge _connection;
        [SerializeField] private AccessibilityServiceBridge accessibilityService;
        [SerializeField] private PermissionsBridge permissions;

        public KeyboardBridge Keyboard => _keyboard;
        public MouseBridge Mouse => _mouse;
        public MediaBridge Media => _media;
        public AccessibilityServiceBridge AccessibilityService => accessibilityService;
        public PermissionsBridge Permissions => permissions;
        public ConnectionBridge Connection => _connection;

        public BleBridge(JavaBridge java)
        {
            _keyboard = new KeyboardBridge(java);
            _mouse = new MouseBridge(java);
            _media = new MediaBridge(java);
            accessibilityService = new AccessibilityServiceBridge(java);
            permissions = new PermissionsBridge();
            _connection = new ConnectionBridge(java);
        }
    }
}
