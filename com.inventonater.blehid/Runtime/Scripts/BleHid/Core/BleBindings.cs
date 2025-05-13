using System;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater
{
    [Serializable]
    public class BleBindings
    {
        public const string BleMediaName = "BleMedia";
        public const string BleMouseName = "BleMouse";
        public const string LocalMediaName = "LocalMedia";
        public const string LocalDPadName = "LocalDPad";

        private readonly InputBinding _localMedia;
        private readonly InputBinding _localDPad;
        private readonly InputBinding _bleMouse;
        private readonly InputBinding _bleMedia;

        public InputBinding LocalMedia => _localMedia;
        public InputBinding LocalDPad => _localDPad;
        public InputBinding BleMouse => _bleMouse;
        public InputBinding BleMedia => _bleMedia;

        public BleBindings(BleBridge bleBridge, AccessibilityServiceBridge accessibilityServiceBridge)
        {
            var accessibilityServiceRegistry = new MappableActionRegistry(accessibilityServiceBridge);
            var bleHidRegistry = new MappableActionRegistry(bleBridge.Mouse, bleBridge.Keyboard, bleBridge.Media);
            _localMedia = CreateLocalMedia(accessibilityServiceRegistry);
            _localDPad = CreateLocalDPad(accessibilityServiceRegistry);
            _bleMouse = CreateBleMouse(bleHidRegistry);
            _bleMedia = CreateBleMedia(bleHidRegistry);
        }

        public InputBinding CreateBleMouse(MappableActionRegistry registry)
        {
            var buttons = new List<ButtonMapEntry>
            {
                new (ButtonEvent.PrimaryPress, MappableActionId.PrimaryPress),
                new (ButtonEvent.PrimaryRelease, MappableActionId.PrimaryRelease),
                new (ButtonEvent.SecondaryPress, MappableActionId.SecondaryPress),
                new (ButtonEvent.SecondaryRelease, MappableActionId.SecondaryRelease),
                new (ButtonEvent.Up, MappableActionId.Up),
                new (ButtonEvent.Right, MappableActionId.Right),
                new (ButtonEvent.Down, MappableActionId.Down),
                new (ButtonEvent.Left, MappableActionId.Left),
            };
            var axisMappings = new List<IAxisMapping>
            {
                new MousePositionAxisMapping(registry.MouseMoveAction),
                new SingleAxisMappingVolumeIncremental(Axis.Z, registry),
            };

            var map = new InputMap(buttons, axisMappings);
            return new InputBinding(BleMouseName, registry, map);
        }

        public InputBinding CreateBleMedia(MappableActionRegistry registry)
        {
            var buttons = new List<ButtonMapEntry>
            {
                new (ButtonEvent.PrimaryDoubleTap, MappableActionId.PlayToggle),
                new (ButtonEvent.Right, MappableActionId.NextTrack),
                new (ButtonEvent.Left, MappableActionId.PreviousTrack),
                new (ButtonEvent.Up, MappableActionId.MuteToggle),
                new (ButtonEvent.Down, MappableActionId.MuteToggle),
            };
            var axisMappings = new List<IAxisMapping> { new SingleAxisMappingVolumeIncremental(Axis.Z, registry) };
            var map = new InputMap(buttons, axisMappings);
            return new InputBinding(BleMediaName, registry, map);
        }

        public InputBinding CreateLocalMedia(MappableActionRegistry registry)
        {
            var buttons = new List<ButtonMapEntry>
            {
                new (ButtonEvent.PrimaryDoubleTap, MappableActionId.PlayToggle),
                new (ButtonEvent.Right, MappableActionId.NextTrack),
                new (ButtonEvent.Left, MappableActionId.PreviousTrack),
                new (ButtonEvent.Up, MappableActionId.MuteToggle),
                new (ButtonEvent.Down, MappableActionId.MuteToggle),
            };
            var axisMappings = new List<IAxisMapping> { new SingleAxisMappingVolumeIncremental(Axis.Z, registry) };
            var map = new InputMap(buttons, axisMappings);
            return new InputBinding(LocalMediaName, registry, map);
        }

        public InputBinding CreateLocalDPad(MappableActionRegistry registry)
        {
            var buttons = new List<ButtonMapEntry>
            {
                new (ButtonEvent.PrimaryTap, MappableActionId.Select),
                new (ButtonEvent.SecondaryTap, MappableActionId.Back),
                new (ButtonEvent.SecondaryDoubleTap, MappableActionId.Home),
                new (ButtonEvent.Up, MappableActionId.Up),
                new (ButtonEvent.Right, MappableActionId.Right),
                new (ButtonEvent.Down, MappableActionId.Down),
                new (ButtonEvent.Left, MappableActionId.Left),
            };
            var axisMappings = new List<IAxisMapping>
                { new SingleAxisMappingVolumeIncremental(Axis.Z, registry) };
            var map = new InputMap(buttons, axisMappings);
            return new InputBinding(LocalDPadName, registry, map);
        }

        private static readonly Vector2 SamsungResolution = new Vector2(1440, 3088);
        private static readonly Vector2 Pixel9XLResolution = new Vector2(1344, 2992);
        private static Vector2 Resolution => Pixel9XLResolution;
        private static Vector2 ClampToScreen(Vector2 vector2) => new(Mathf.Clamp(vector2.x, 0, Resolution.x), Mathf.Clamp(vector2.y, 0, Resolution.y));

        private static Vector2 ScreenCenter() => Resolution * 0.5f;
        // public static InputDeviceMapping LocalDrag(BleBridge bridge)
        // {
        //     var serviceBridge = bridge.AccessibilityService;
        //     var mapping = new InputDeviceMapping("LocalDrag", bridge.ActionRegistry);
        //     var swipeMapping = new MousePositionAxisMapping(serviceBridge.SwipeExtend, requirePress: true);
        //     mapping.Add(PrimaryPress, () => serviceBridge.SwipeBegin(ScreenCenter()));
        //     mapping.Add(PrimaryRelease, () => serviceBridge.SwipeEnd());
        //     mapping.Add(swipeMapping);
        //     return mapping;
        // }
    }
}
