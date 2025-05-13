using System;
using System.Collections.Generic;

namespace Inventonater
{
    [Serializable]
    public class MqttInputBindings
    {
        private InputBinding _mqttSpeaker;
        private InputBinding _mqttLights;
        private InputBinding _mqttChromecast;
        private InputBinding _shell;

        public InputBinding Speaker => _mqttSpeaker;
        public InputBinding Lights => _mqttLights;
        public InputBinding Chromecast => _mqttChromecast;
        public InputBinding Shell => _shell;

        public MqttInputBindings(MqttBridge bridge)
        {
            _mqttSpeaker = CreateSpeaker(bridge.SpotifyBridge);
            _mqttLights = CreateLight(bridge.LightsBridge);
            _mqttChromecast = CreateChromecast(bridge.ChromecastBridge);
            _shell = CreateShell(bridge.ShellBridge);
        }

        public InputBinding CreateSpeaker(MqttSpotifyBridge bridge)
        {
            MappableActionRegistry registry = new MappableActionRegistry(bridge);

            var map = new InputMap();
            map.Add(ButtonEvent.PrimaryDoubleTap, MappableActionId.PlayToggle);
            map.Add(ButtonEvent.Right, MappableActionId.NextTrack);
            map.Add(ButtonEvent.Left, MappableActionId.PreviousTrack);
            map.Add(ButtonEvent.Up, MappableActionId.VolumeUp);
            map.Add(ButtonEvent.Down, MappableActionId.VolumeDown);
            map.Add(new SingleAxisMappingVolumeIncremental(Axis.Z, registry, scale: 0.4f, timeInterval: 0.04f));

            return new InputBinding("Speaker", registry, map);
        }

        public InputBinding CreateLight(MqttLightsBridge bridge)
        {
            MappableActionRegistry registry = new MappableActionRegistry(bridge);

            var buttons = new List<ButtonMapEntry>
            {
                new(ButtonEvent.SecondaryPress, MappableActionId.Back),
                new(ButtonEvent.Right, MappableActionId.Right),
                new(ButtonEvent.Left, MappableActionId.Left),
                new(ButtonEvent.Up, MappableActionId.Up),
                new(ButtonEvent.Down, MappableActionId.Down),
            };

            var axisMapping = new SingleAxisMappingDelta(Axis.Z, bridge.IncrementBrightness, scale: 1.4f, timeInterval: 0.4f);
            var map = new InputMap(buttons, new List<IAxisMapping> { axisMapping });
            return new InputBinding("Lights", registry, map);
        }

        public InputBinding CreateChromecast(MqttChromecastBridge bridge)
        {
            MappableActionRegistry registry = new MappableActionRegistry(bridge);

            var buttons = new List<ButtonMapEntry>
            {
                new(ButtonEvent.PrimaryPress, MappableActionId.Select),
                new(ButtonEvent.SecondaryPress, MappableActionId.Back),
                new(ButtonEvent.Up, MappableActionId.Up),
                new(ButtonEvent.Down, MappableActionId.Down),
                new(ButtonEvent.Left, MappableActionId.Left),
                new(ButtonEvent.Right, MappableActionId.Right),
            };
            var axisMapping = new SingleAxisMappingVolumeIncremental(Axis.Z, registry, timeInterval: 0.04f);

            var map = new InputMap(buttons, new List<IAxisMapping> { axisMapping });
            return new InputBinding("Chromecast", registry, map);
        }

        public InputBinding CreateShell(MqttShellBridge bridge)
        {
            MappableActionRegistry registry = new MappableActionRegistry(bridge);

            var buttons = new List<ButtonMapEntry>
            {
                new (ButtonEvent.PrimaryPress, MappableActionId.PrimaryPress),
                new (ButtonEvent.PrimaryRelease, MappableActionId.PrimaryRelease),
                new (ButtonEvent.PrimaryTap, MappableActionId.Select),
                new (ButtonEvent.SecondaryTap, MappableActionId.Back),

                new (ButtonEvent.SecondaryPress, MappableActionId.SecondaryPress),
                new (ButtonEvent.SecondaryRelease, MappableActionId.SecondaryRelease),
                new (ButtonEvent.TertiaryPress, MappableActionId.TertiaryPress),
                new (ButtonEvent.TertiaryRelease, MappableActionId.TertiaryRelease),

                new (ButtonEvent.Up, MappableActionId.Up),
                new (ButtonEvent.Down, MappableActionId.Down),
                new (ButtonEvent.Left, MappableActionId.Left),
                new (ButtonEvent.Right, MappableActionId.Right),
            };
            var axisMapping = new SingleAxisMappingVolumeIncremental(Axis.Z, registry, timeInterval: 0.04f);

            var map = new InputMap(buttons, new List<IAxisMapping> { axisMapping });
            return new InputBinding("MQTT Shell", registry, map);
        }
    }
}
