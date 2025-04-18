using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Sample implementation of an input provider to demonstrate the input provider system.
    /// This provider allows keyboard input to control mouse movement and buttons.
    /// </summary>
    public class DesktopBleHidInputBridge : MonoBehaviour, IBleHidInputBridge
    {
        [SerializeField] private float _movementSpeed = 10f;
        [SerializeField] private bool _isActive = true;
        
        // IInputProvider implementation
        public string Name => "Keyboard Input Provider";
        public bool IsActive => _isActive;
        public event Action<BleHidButtonEvent> WhenButtonEvent = delegate { };
        public event Action<Vector3> WhenPositionEvent = delegate { };
        public event Action<BleHidDirection> WhenDirectionEvent = delegate { };

        private void Start()
        {
            BleHidManager.Instance.InputBridge.RegisterProvider(this);
        }

        private void OnDestroy()
        {
            BleHidManager.Instance.InputBridge.UnregisterProvider(this);
        }
        
        private void Update()
        {
            if (!_isActive) return;

        }
    }
}
