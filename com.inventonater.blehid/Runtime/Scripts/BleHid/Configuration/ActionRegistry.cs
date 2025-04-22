using System;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class ActionRegistry
    {
        private readonly Dictionary<string, Func<Dictionary<string, object>, Action>> _actionFactories = new Dictionary<string, Func<Dictionary<string, object>, Action>>();
        private readonly BleBridge _bleBridge;
        
        public ActionRegistry(BleBridge bleBridge)
        {
            _bleBridge = bleBridge;
            RegisterDefaultActions();
        }
        
        private void RegisterDefaultActions()
        {
            // Mouse actions
            RegisterAction("Mouse.LeftButton.Press", _ => () => _bleBridge.Mouse.PressMouseButton(0));
            RegisterAction("Mouse.LeftButton.Release", _ => () => _bleBridge.Mouse.ReleaseMouseButton(0));
            RegisterAction("Mouse.RightButton.Press", _ => () => _bleBridge.Mouse.PressMouseButton(1));
            RegisterAction("Mouse.RightButton.Release", _ => () => _bleBridge.Mouse.ReleaseMouseButton(1));
            RegisterAction("Mouse.MiddleButton.Press", _ => () => _bleBridge.Mouse.PressMouseButton(2));
            RegisterAction("Mouse.MiddleButton.Release", _ => () => _bleBridge.Mouse.ReleaseMouseButton(2));
            
            // Keyboard actions
            RegisterAction("Keyboard.Key", parameters => {
                if (parameters.TryGetValue("keyCode", out var keyCodeObj) && keyCodeObj is string keyCodeStr)
                {
                    byte keyCode = KeyCodeFromString(keyCodeStr);
                    return () => _bleBridge.Keyboard.SendKey(keyCode);
                }
                return () => {};
            });
            
            // Media actions
            RegisterAction("Media.VolumeUp", _ => () => _bleBridge.Media.VolumeUp());
            RegisterAction("Media.VolumeDown", _ => () => _bleBridge.Media.VolumeDown());
            RegisterAction("Media.PlayPause", _ => () => _bleBridge.Media.PlayPause());
            RegisterAction("Media.NextTrack", _ => () => _bleBridge.Media.NextTrack());
            RegisterAction("Media.PreviousTrack", _ => () => _bleBridge.Media.PreviousTrack());
            RegisterAction("Media.Mute", _ => () => _bleBridge.Media.Mute());
        }
        
        public void RegisterAction(string path, Func<Dictionary<string, object>, Action> factory)
        {
            _actionFactories[path] = factory;
        }
        
        public Action GetAction(string path, Dictionary<string, object> parameters = null)
        {
            parameters ??= new Dictionary<string, object>();
            
            if (_actionFactories.TryGetValue(path, out var factory))
            {
                return factory(parameters);
            }
            
            Debug.LogWarning($"Action not found: {path}");
            return () => {};
        }
        
        private byte KeyCodeFromString(string keyCodeStr)
        {
            // Map string key names to HID key codes
            Dictionary<string, byte> keyCodes = new Dictionary<string, byte>
            {
                ["UpArrow"] = BleHidConstants.KEY_UP,
                ["DownArrow"] = BleHidConstants.KEY_DOWN,
                ["LeftArrow"] = BleHidConstants.KEY_LEFT,
                ["RightArrow"] = BleHidConstants.KEY_RIGHT,
                ["Enter"] = BleHidConstants.KEY_RETURN,
                ["Escape"] = BleHidConstants.KEY_ESCAPE,
                ["Space"] = BleHidConstants.KEY_SPACE,
                ["Tab"] = BleHidConstants.KEY_TAB,
                ["Backspace"] = BleHidConstants.KEY_BACKSPACE
                // Add more key mappings as needed
            };
            
            if (keyCodes.TryGetValue(keyCodeStr, out byte keyCode))
            {
                return keyCode;
            }
            
            Debug.LogWarning($"Unknown key code: {keyCodeStr}");
            return 0;
        }
    }
}
