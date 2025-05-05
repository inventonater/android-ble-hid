using System.Collections.Generic;
using UnityEngine;

namespace Inventonater
{
    public class InputDeviceMappingFactory
    {
        public InputDeviceMapping BleMouse(ActionRegistry registry)
        {
            var buttons = new List<(InputEvent, EInputAction)>
            {
                (InputEvent.PrimaryPress, EInputAction.PrimaryPress),
                (InputEvent.PrimaryRelease, EInputAction.PrimaryRelease),
                (InputEvent.SecondaryPress, EInputAction.SecondaryPress),
                (InputEvent.SecondaryRelease, EInputAction.SecondaryRelease),
                (InputEvent.Up, EInputAction.Up),
                (InputEvent.Right, EInputAction.Right),
                (InputEvent.Down, EInputAction.Down),
                (InputEvent.Left, EInputAction.Left),
            };
            var axisMappings = new List<IAxisMapping>
            {
                new MousePositionAxisMapping(registry.MouseMoveAction),
                new SingleAxisMappingIncremental(Axis.Z, registry.GetAction(EInputAction.VolumeUp), registry.GetAction(EInputAction.VolumeDown)),
            };

            return new InputDeviceMapping("BleMouse", registry, buttons, axisMappings);
        }

        public InputDeviceMapping BleMedia(ActionRegistry registry)
        {
            var buttons = new List<(InputEvent, EInputAction)>
            {
                (InputEvent.PrimaryDoubleTap, EInputAction.PlayPause),
                (InputEvent.Right, EInputAction.NextTrack),
                (InputEvent.Left, EInputAction.PreviousTrack),
                (InputEvent.Up, EInputAction.Mute),
                (InputEvent.Down, EInputAction.Mute),
            };
            var axisMappings = new List<IAxisMapping>
                { new SingleAxisMappingIncremental(Axis.Z, registry.GetAction(EInputAction.VolumeUp), registry.GetAction(EInputAction.VolumeDown)) };

            return new InputDeviceMapping("BleMedia", registry, buttons, axisMappings);
        }

        public InputDeviceMapping LocalMedia(ActionRegistry registry)
        {
            var buttons = new List<(InputEvent, EInputAction)>
            {
                (InputEvent.PrimaryDoubleTap, EInputAction.PlayPause),
                (InputEvent.Right, EInputAction.NextTrack),
                (InputEvent.Left, EInputAction.PreviousTrack),
                (InputEvent.Up, EInputAction.Mute),
                (InputEvent.Down, EInputAction.Mute),
            };
            var axisMappings = new List<IAxisMapping>
                { new SingleAxisMappingIncremental(Axis.Z, registry.GetAction(EInputAction.VolumeUp), registry.GetAction(EInputAction.VolumeDown)) };

            return new InputDeviceMapping("LocalMedia", registry, buttons, axisMappings);
        }

        public InputDeviceMapping LocalDPad(ActionRegistry registry)
        {
            var buttons = new List<(InputEvent, EInputAction)>
            {
                (InputEvent.PrimaryTap, EInputAction.Select),
                (InputEvent.SecondaryTap, EInputAction.Back),
                (InputEvent.SecondaryDoubleTap, EInputAction.Home),
                (InputEvent.Up, EInputAction.Up),
                (InputEvent.Right, EInputAction.Right),
                (InputEvent.Down, EInputAction.Down),
                (InputEvent.Left, EInputAction.Left),
            };
            var axisMappings = new List<IAxisMapping>
                { new SingleAxisMappingIncremental(Axis.Z, registry.GetAction(EInputAction.VolumeUp), registry.GetAction(EInputAction.VolumeDown)) };
            return new InputDeviceMapping("LocalDPad", registry, buttons, axisMappings);
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
