using System;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Attribute to mark methods that can be remapped by users at runtime.
    /// Apply this to methods that should appear in the action remapping UI.
    /// </summary>
    [AttributeUsage(AttributeTargets.Method, AllowMultiple = false, Inherited = true)]
    public class MappableActionAttribute : Attribute
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
        /// Creates a new MappableAction attribute
        /// </summary>
        /// <param name="id">Unique identifier for the action</param>
        /// <param name="displayName">User-friendly name to display in the UI</param>
        /// <param name="category">Category for grouping related actions in the UI</param>
        /// <param name="description">Description of what the action does</param>
        public MappableActionAttribute(string id, string displayName, string category = "General", string description = "")
        {
            Id = id;
            DisplayName = displayName;
            Category = category;
            Description = description;
        }
    }
}
