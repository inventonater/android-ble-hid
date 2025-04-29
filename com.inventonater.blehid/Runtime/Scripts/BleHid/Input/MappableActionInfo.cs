using System;
using System.Reflection;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Contains information about a mappable action discovered through reflection.
    /// </summary>
    public class MappableActionInfo
    {
        /// <summary>
        /// Unique identifier for the action
        /// </summary>
        public string Id { get; }
        
        /// <summary>
        /// User-friendly name to display in the UI
        /// </summary>
        public string DisplayName { get; }
        
        /// <summary>
        /// Category for grouping related actions in the UI
        /// </summary>
        public string Category { get; }
        
        /// <summary>
        /// Description of what the action does
        /// </summary>
        public string Description { get; }
        
        /// <summary>
        /// Method info for the action method
        /// </summary>
        public MethodInfo Method { get; }
        
        /// <summary>
        /// Target object on which to invoke the method
        /// </summary>
        public object Target { get; }
        
        /// <summary>
        /// Creates a new MappableActionInfo
        /// </summary>
        /// <param name="id">Unique identifier for the action</param>
        /// <param name="displayName">User-friendly name to display in the UI</param>
        /// <param name="category">Category for grouping related actions in the UI</param>
        /// <param name="description">Description of what the action does</param>
        /// <param name="method">Method info for the action method</param>
        /// <param name="target">Target object on which to invoke the method</param>
        public MappableActionInfo(string id, string displayName, string category, string description, MethodInfo method, object target)
        {
            Id = id;
            DisplayName = displayName;
            Category = category;
            Description = description;
            Method = method;
            Target = target;
        }
        
        /// <summary>
        /// Invokes the action method on the target object
        /// </summary>
        /// <returns>True if the action was successful, false otherwise</returns>
        public bool Invoke()
        {
            try
            {
                return (bool)Method.Invoke(Target, null);
            }
            catch (Exception e)
            {
                LoggingManager.Instance.Exception(e);
                return false;
            }
        }
    }
}
