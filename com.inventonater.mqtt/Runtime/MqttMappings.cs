using System;
using System.Collections.Generic;

namespace Inventonater
{
    [Serializable]
    public class MqttMappings
    {
        private InputDeviceMapping _mqttSpeaker;
        private InputDeviceMapping _mqttLights;
        private InputDeviceMapping _mqttChromecast;
        private InputDeviceMapping _shell;

        public InputDeviceMapping Speaker => _mqttSpeaker;
        public InputDeviceMapping Lights => _mqttLights;
        public InputDeviceMapping Chromecast => _mqttChromecast;
        public InputDeviceMapping Shell => _shell;

        public MqttMappings(InputRouter inputRouter, MqttBridge bridge)
        {
            _mqttSpeaker = CreateSpeakerMapping(bridge.SpotifyBridge);
            inputRouter.AddMapping(_mqttSpeaker);

            _mqttLights = CreateLightMapping(bridge.LightsBridge);
            inputRouter.AddMapping(_mqttLights);

            _mqttChromecast = CreateChromecastMapping(bridge.ChromecastBridge);
            inputRouter.AddMapping(_mqttChromecast);

            _shell = CreateShellMapping(bridge.ShellBridge);
            inputRouter.AddMapping(_shell);
        }

        public InputDeviceMapping CreateSpeakerMapping(MqttSpotifyBridge bridge)
        {
            ActionRegistry registry = new ActionRegistry(bridge);

            var buttons = new List<(InputEvent, EInputAction)>
            {
                (InputEvent.PrimaryDoubleTap, EInputAction.PlayToggle),
                (InputEvent.Right, EInputAction.NextTrack),
                (InputEvent.Left, EInputAction.PreviousTrack),
                (InputEvent.Up, EInputAction.VolumeUp),
                (InputEvent.Down, EInputAction.VolumeDown),
            };
            var axisMappings = new List<IAxisMapping> { new SingleAxisMappingIncremental(Axis.Z, registry.GetAction(EInputAction.VolumeUp), registry.GetAction(EInputAction.VolumeDown), scale: 0.4f, timeInterval: 0.04f) };

            return new InputDeviceMapping("Speaker", registry, buttons, axisMappings);
        }

        public InputDeviceMapping CreateLightMapping(MqttLightsBridge bridge)
        {
            ActionRegistry registry = new ActionRegistry(bridge);

            var buttons = new List<(InputEvent, EInputAction)>
            {
                (InputEvent.SecondaryPress, EInputAction.Back),
                (InputEvent.Right, EInputAction.Right),
                (InputEvent.Left, EInputAction.Left),
                (InputEvent.Up, EInputAction.Up),
                (InputEvent.Down, EInputAction.Down),
            };
            
            var axisMapping = new SingleAxisMappingDelta(Axis.Z, bridge.IncrementBrightness, scale: 1.4f, timeInterval: 0.4f);
            return new InputDeviceMapping("Lights", registry, buttons, new List<IAxisMapping> { axisMapping });
        }

        public InputDeviceMapping CreateChromecastMapping(MqttChromecastBridge bridge)
        {
            ActionRegistry registry = new ActionRegistry(bridge);

            var buttons = new List<(InputEvent, EInputAction)>
            {
                (InputEvent.PrimaryPress, EInputAction.Select),
                (InputEvent.SecondaryPress, EInputAction.Back),
                (InputEvent.Up, EInputAction.Up),
                (InputEvent.Down, EInputAction.Down),
                (InputEvent.Left, EInputAction.Left),
                (InputEvent.Right, EInputAction.Right),
            };
            var axisMapping = new SingleAxisMappingIncremental(Axis.Z, registry.GetAction(EInputAction.VolumeUp), registry.GetAction(EInputAction.VolumeDown), timeInterval: 0.04f);

            return new InputDeviceMapping("Chromecast", registry, buttons, new List<IAxisMapping> { axisMapping });
        }

        public InputDeviceMapping CreateShellMapping(MqttShellBridge bridge)
        {
            ActionRegistry registry = new ActionRegistry(bridge);

            var buttons = new List<(InputEvent, EInputAction)>
            {
                (InputEvent.PrimaryPress, EInputAction.PrimaryPress),
                (InputEvent.PrimaryRelease, EInputAction.PrimaryRelease),
                (InputEvent.PrimaryTap, EInputAction.Select),
                (InputEvent.SecondaryTap, EInputAction.Back),

                (InputEvent.SecondaryPress, EInputAction.SecondaryPress),
                (InputEvent.SecondaryRelease, EInputAction.SecondaryRelease),
                (InputEvent.TertiaryPress, EInputAction.TertiaryPress),
                (InputEvent.TertiaryRelease, EInputAction.TertiaryRelease),

                (InputEvent.Up, EInputAction.Up),
                (InputEvent.Down, EInputAction.Down),
                (InputEvent.Left, EInputAction.Left),
                (InputEvent.Right, EInputAction.Right),
            };
            var axisMapping = new SingleAxisMappingIncremental(Axis.Z, registry.GetAction(EInputAction.VolumeUp), registry.GetAction(EInputAction.VolumeDown), timeInterval: 0.04f);

            return new InputDeviceMapping("MQTT Shell", registry, buttons, new List<IAxisMapping> { axisMapping });
        }
    }
}
