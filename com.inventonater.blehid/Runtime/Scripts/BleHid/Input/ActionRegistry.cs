using System;
using System.Collections.Generic;
using System.Reflection;
using UnityEngine;

namespace Inventonater.BleHid
{
    public delegate void MouseMoveActionDelegate(Vector2 delta);

    public class ActionRegistry
    {
        private static readonly Action EmptyAction = () => { };

        public MouseMoveActionDelegate MouseMoveAction { get; }
        private readonly Dictionary<EInputAction, MappableActionInfo> _actionInfo = new();
        public bool TryGetInfo(EInputAction id, out MappableActionInfo info) => _actionInfo.TryGetValue(id, out info);
        public Action GetAction(EInputAction id) => TryGetInfo(id, out var info) ? info.Invoke : EmptyAction;

        public ActionRegistry(params object[] bridges) : this(_ => { }, bridges) { }
        public ActionRegistry(MouseMoveActionDelegate mouseMoveAction, params object[] bridges)
        {
            MouseMoveAction = mouseMoveAction;
            foreach (var bridge in bridges) DiscoverActions(bridge);
        }

        private void DiscoverActions(object target)
        {
            var type = target.GetType();
            LoggingManager.Instance.Log($"ActionRegistry: Discovering actions on {type.Name}");

            foreach (var method in type.GetMethods(BindingFlags.Public | BindingFlags.Instance))
            {
                var attribute = method.GetCustomAttribute<MappableActionAttribute>();
                if (attribute == null) continue;

                if (method.GetParameters().Length > 0)
                {
                    LoggingManager.Instance.Warning($"ActionRegistry: Method {method.Name} has MappableAction attribute but has parameters");
                    continue;
                }

                var actionInfo = new MappableActionInfo(attribute, method, target);

                _actionInfo[attribute.Id] = actionInfo;
                LoggingManager.Instance.Log($"ActionRegistry: Registered mappable action: {attribute.Id} ({attribute.DisplayName})");
            }
        }
    }
}
