using Inventonater;
using UnityEngine;

namespace Examples
{
    public class DefaultMappingExample : MonoBehaviour
    {
        private void Start()
        {
            var mappingFactory = new InputDeviceMappingFactory();

            var accessibilityServiceRegistry = new ActionRegistry(BleHidManager.Instance.AccessibilityServiceBridge);
            AddMapping(mappingFactory.LocalMedia(accessibilityServiceRegistry));
            AddMapping(mappingFactory.LocalDPad(accessibilityServiceRegistry));

            var bleBridge = BleHidManager.Instance.BleBridge;
            var bleHidRegistry = new ActionRegistry(bleBridge.Mouse, bleBridge.Keyboard, bleBridge.Media);
            AddMapping(mappingFactory.BleMouse(bleHidRegistry));
            AddMapping(mappingFactory.BleMedia(bleHidRegistry));
        }

        private void AddMapping(InputDeviceMapping mapping) => BleHidManager.Instance.AddMapping(mapping);
    }
}
