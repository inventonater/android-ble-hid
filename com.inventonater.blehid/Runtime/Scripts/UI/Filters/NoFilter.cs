using UnityEngine;

namespace Inventonater.BleHid.UI.Filters
{
    /// <summary>
    /// Passthrough filter that applies no smoothing or processing to input
    /// </summary>
    public class NoFilter : IInputFilter
    {
        /// <summary>
        /// Display name of the filter for UI
        /// </summary>
        public string Name => "No Filter";
        
        /// <summary>
        /// Brief description of how the filter works
        /// </summary>
        public string Description => "Direct input with no processing (lowest latency)";
        
        /// <summary>
        /// Creates a new NoFilter instance
        /// </summary>
        public NoFilter() { }
        
        /// <summary>
        /// Reset filter state - no effect for this filter type
        /// </summary>
        public void Reset() 
        {
            // No state to reset
        }
        
        /// <summary>
        /// Update filter parameters - no effect for this filter type
        /// </summary>
        public void SetParameters(float param1, float param2)
        {
            // No parameters to update
        }
        
        /// <summary>
        /// Filter a single float value - returns value unchanged
        /// </summary>
        /// <param name="value">Input value</param>
        /// <param name="timestamp">Current timestamp (unused)</param>
        /// <returns>Original input value</returns>
        public float Filter(float value, float timestamp)
        {
            return value;
        }
        
        /// <summary>
        /// Filter a 2D vector - returns vector unchanged
        /// </summary>
        /// <param name="point">Input vector</param>
        /// <param name="timestamp">Current timestamp (unused)</param>
        /// <returns>Original input vector</returns>
        public Vector2 Filter(Vector2 point, float timestamp)
        {
            return point;
        }
    }
}
