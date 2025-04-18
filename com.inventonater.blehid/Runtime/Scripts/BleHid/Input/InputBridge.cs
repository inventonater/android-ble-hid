using System;
using System.Collections.Generic;
using Inventonater.BleHid.InputControllers;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class InputBridge
    {
        private readonly List<IBleHidInputBridge> _providers = new();
        public HashSet<IBleHidInputBridge> Providers => new(_providers);
        public event Action<IBleHidInputBridge> WhenProviderRegistered = delegate { };
        public event Action<IBleHidInputBridge> WhenProviderUnregistered = delegate { };
        public KeyboardBridge Keyboard { get; }
        public MouseBridge Mouse { get; }
        public MediaBridge Media { get; }

        private readonly BleHidManager manager;

        private readonly Dictionary<BleHidButtonEvent, Action> ButtonMapping = new();
        private readonly Dictionary<BleHidDirection, Action> DirectionMapping = new();

        public InputBridge(BleHidManager manager)
        {
            this.manager = manager;
            Keyboard = new KeyboardBridge(manager);
            Mouse = new MouseBridge(manager);
            Media = new MediaBridge(manager);

            ButtonMapping.Add(new BleHidButtonEvent(BleHidButtonEvent.Id.Primary, BleHidButtonEvent.Action.Press), () => Mouse.PressMouseButton(0));
            ButtonMapping.Add(new BleHidButtonEvent(BleHidButtonEvent.Id.Primary, BleHidButtonEvent.Action.Release), () => Mouse.ReleaseMouseButton(0));
            ButtonMapping.Add(new BleHidButtonEvent(BleHidButtonEvent.Id.Secondary, BleHidButtonEvent.Action.Press), () => Mouse.PressMouseButton(1));
            ButtonMapping.Add(new BleHidButtonEvent(BleHidButtonEvent.Id.Secondary, BleHidButtonEvent.Action.Release), () => Mouse.ReleaseMouseButton(1));

            DirectionMapping.Add(BleHidDirection.Up, () => Keyboard.SendKey(BleHidConstants.KEY_UP));
            DirectionMapping.Add(BleHidDirection.Right, () => Keyboard.SendKey(BleHidConstants.KEY_RIGHT));
            DirectionMapping.Add(BleHidDirection.Down, () => Keyboard.SendKey(BleHidConstants.KEY_DOWN));
            DirectionMapping.Add(BleHidDirection.Left, () => Keyboard.SendKey(BleHidConstants.KEY_LEFT));
        }

        public void RegisterProvider(IBleHidInputBridge bridge)
        {
            _providers.Add(bridge);
            Debug.Log($"Input provider registered: {bridge.Name}");
            bridge.WhenPositionEvent += HandlePositionEvent;
            bridge.WhenButtonEvent += HandleButtonEvent;
            bridge.WhenDirectionEvent += HandleDirectionEvent;
            WhenProviderRegistered?.Invoke(bridge);
        }

        public void UnregisterProvider(IBleHidInputBridge bridge)
        {
            _providers.Remove(bridge);
            Debug.Log($"Input provider unregistered: {bridge.Name}");
            bridge.WhenPositionEvent -= HandlePositionEvent;
            bridge.WhenButtonEvent -= HandleButtonEvent;
            bridge.WhenDirectionEvent -= HandleDirectionEvent;
            WhenProviderUnregistered?.Invoke(bridge);
        }

        private void HandlePositionEvent(Vector3 position)
        {
            if (!manager.ConfirmIsConnected()) return;

            Vector2 mousePosition = new Vector2(position.x, position.y);
            Mouse.UpdatePosition(mousePosition);

            // Handle volume (z)
            float volume = position.z;
            if (Math.Abs(volume) > 0.01f)
            {
                if (volume > 0) Media.VolumeUp();
                else Media.VolumeDown();
            }
        }

        private void HandleButtonEvent(BleHidButtonEvent buttonEvent)
        {
            if (!manager.ConfirmIsConnected()) return;
            if (ButtonMapping.TryGetValue(buttonEvent, out var action)) action();
        }

        private void HandleDirectionEvent(BleHidDirection bleHidDirection)
        {
            if (!manager.ConfirmIsConnected()) return;
            if (DirectionMapping.TryGetValue(bleHidDirection, out var action)) action();
        }
    }
}
