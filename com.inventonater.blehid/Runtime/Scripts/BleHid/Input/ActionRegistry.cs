using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Registry for mappable actions discovered through reflection.
    /// </summary>
    public static class ActionRegistry
    {
        private static readonly Dictionary<string, MappableActionInfo> _actions = new Dictionary<string, MappableActionInfo>();
        
        /// <summary>
        /// All registered mappable actions
        /// </summary>
        public static IReadOnlyDictionary<string, MappableActionInfo> Actions => _actions;
        
        /// <summary>
        /// Discovers mappable actions on the target object using reflection.
        /// </summary>
        /// <param name="target">The object to scan for mappable actions</param>
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
                
                // Only support methods with no parameters that return bool
                if (method.GetParameters().Length > 0 || method.ReturnType != typeof(bool))
                {
                    LoggingManager.Instance.Warning($"ActionRegistry: Method {method.Name} has MappableAction attribute but has parameters or doesn't return bool");
                    continue;
                }
                
                var actionInfo = new MappableActionInfo(
                    attribute.Id,
                    attribute.DisplayName,
                    attribute.Category,
                    attribute.Description,
                    method,
                    target
                );
                
                _actions[attribute.Id] = actionInfo;
                LoggingManager.Instance.Log($"ActionRegistry: Registered mappable action: {attribute.Id} ({attribute.DisplayName})");
            }
        }
        
        /// <summary>
        /// Tries to get a mappable action by its ID.
        /// </summary>
        /// <param name="id">The ID of the action to get</param>
        /// <param name="action">The action, if found</param>
        /// <returns>True if the action was found, false otherwise</returns>
        public static bool TryGetAction(string id, out MappableActionInfo action)
        {
            return _actions.TryGetValue(id, out action);
        }
        
        /// <summary>
        /// Gets all mappable actions in a specific category.
        /// </summary>
        /// <param name="category">The category to filter by</param>
        /// <returns>A list of actions in the specified category</returns>
        public static List<MappableActionInfo> GetActionsByCategory(string category)
        {
            return _actions.Values.Where(a => a.Category == category).ToList();
        }
        
        /// <summary>
        /// Gets all unique categories of registered mappable actions.
        /// </summary>
        /// <returns>A list of unique category names</returns>
        public static List<string> GetCategories()
        {
            return _actions.Values.Select(a => a.Category).Distinct().ToList();
        }
        
        /// <summary>
        /// Clears all registered actions.
        /// </summary>
        public static void Clear()
        {
            _actions.Clear();
            LoggingManager.Instance.Log("ActionRegistry: Cleared all registered actions");
        }
    }
}
