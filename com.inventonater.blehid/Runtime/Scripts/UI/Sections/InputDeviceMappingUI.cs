using UnityEngine;

namespace Inventonater.BleHid
{
    public class InputDeviceMappingUI : SectionUI
    {
        private readonly InputDeviceMapping _mapping;
        public override string TabName { get; }
        private readonly ActionRegistry _registry;

        public InputDeviceMappingUI(InputDeviceMapping mapping)
        {
            TabName = mapping.Name;
            _mapping = mapping;

            _registry = BleHidManager.Instance.BleBridge.ActionRegistry;
        }

        public override void Update()
        {
        }

        public override void DrawUI()
        {
            foreach (var (inputEvent, actions) in _mapping.ButtonMapping)
            {
                GUILayout.Label(inputEvent.ToString());
                foreach (var action in actions)
                {
                    if (_registry.TryGetInfo(action, out var info))
                    {
                        GUILayout.Label($"{info.DisplayName} - {info.Description}");
                    }
                }
            }
        }
    }
}
