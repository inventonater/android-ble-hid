using System.Collections.Generic;
using System.Reflection;

namespace Inventonater.BleHid
{
    public class ActionRegistry
    {
        public ActionRegistry(BleBridge bleBridge)
        {
            DiscoverActions(bleBridge.Mouse);
            DiscoverActions(bleBridge.Keyboard);
            DiscoverActions(bleBridge.Media);
            DiscoverActions(bleBridge.AccessibilityService);
        }

        private static readonly Dictionary<EInputAction, MappableActionInfo> _actions = new();
        public static IReadOnlyDictionary<EInputAction, MappableActionInfo> Actions => _actions;
        
        public static void DiscoverActions(object target)
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
                
                _actions[attribute.Id] = actionInfo;
                LoggingManager.Instance.Log($"ActionRegistry: Registered mappable action: {attribute.Id} ({attribute.DisplayName})");
            }
        }
        
        public static bool TryGetAction(EInputAction id, out MappableActionInfo action) => _actions.TryGetValue(id, out action);
    }
}
