using System;
using System.Collections.Generic;
using System.Reflection;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class ActionRegistry
    {
        private readonly BleBridge _bleBridge;
        public ActionRegistry(BleBridge bleBridge)
        {
            _bleBridge = bleBridge;
            DiscoverActions(bleBridge.Mouse);
            DiscoverActions(bleBridge.Keyboard);
            DiscoverActions(bleBridge.Media);
            DiscoverActions(bleBridge.AccessibilityService);
        }

        private readonly Dictionary<EInputAction, MappableActionInfo> _actionInfo = new();
        public Action<Vector2> GetMouseMoveAction() => _bleBridge.Mouse.MoveMouse;

        public bool TryGetInfo(EInputAction id, out MappableActionInfo info) => _actionInfo.TryGetValue(id, out info);
        public Action GetAction(EInputAction id) => TryGetInfo(id, out var info) ? info.Invoke : EmptyAction;

        private static Action EmptyAction { get; } = () => { };

        public void DiscoverActions(object target)
        {
            if (target == null)
            {
                LoggingManager.Instance.Warning("ActionRegistry: Cannot discover actions on null target");
                return;
            }

            var type = target.GetType();
            LoggingManager.Instance.Log($"ActionRegistry: Discovering actions on {type.Name}");

            foreach (var method in type.GetMethods(BindingFlags.Public | BindingFlags.Instance))
            {
                var attribute = method.GetCustomAttribute<MappableActionAttribute>();
                if (attribute == null) continue;

                // Only support methods with no parameters
                if (method.GetParameters().Length > 0)
                {
                    LoggingManager.Instance.Warning($"ActionRegistry: Method {method.Name} has MappableAction attribute but has parameters");
                    continue;
                }

                var actionInfo = new MappableActionInfo(
                    attribute.Id,
                    attribute.DisplayName,
                    attribute.Description,
                    method,
                    target
                );

                _actionInfo[attribute.Id] = actionInfo;
                LoggingManager.Instance.Log($"ActionRegistry: Registered mappable action: {attribute.Id} ({attribute.DisplayName})");
            }
        }

    }
}
