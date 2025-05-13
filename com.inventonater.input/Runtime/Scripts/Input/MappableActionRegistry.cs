using System;
using System.Collections.Generic;
using System.Reflection;

namespace Inventonater
{
    public class MappedActions
    {
        private readonly List<MappableActionInfo> _mappableActionInfos = new();
        public IReadOnlyList<MappableActionInfo> ActionInfos => _mappableActionInfos;
        public void Add(MappableActionInfo info) => _mappableActionInfos.Add(info);

        public void Invoke()
        {
            foreach (var actionInfo in ActionInfos)
            {
                try
                {
                    actionInfo.Invoke();
                }
                catch (Exception e)
                {
                    LoggingManager.Instance.Exception(e);
                }
            }
        }
    }

    [Serializable]
    public class MappableActionRegistry
    {
        public MouseMoveActionDelegate MouseMoveAction { get; }
        
        private readonly Dictionary<MappableActionId, MappedActions> _actionInfo = new();
        public bool TryGetMappedAction(MappableActionId id, out MappedActions info) => _actionInfo.TryGetValue(id, out info);

        public MappableActionRegistry(params object[] bridges) : this(_ => { }, bridges) { }
        public MappableActionRegistry(MouseMoveActionDelegate mouseMoveAction, params object[] bridges)
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

                if(!_actionInfo.ContainsKey(actionInfo.Id)) _actionInfo.Add(actionInfo.Id, new MappedActions());
                
                _actionInfo[attribute.Id].Add(actionInfo);
                LoggingManager.Instance.Log($"ActionRegistry: Registered mappable action: {attribute.Id} ({attribute.DisplayName})");
            }
        }
    }
}
