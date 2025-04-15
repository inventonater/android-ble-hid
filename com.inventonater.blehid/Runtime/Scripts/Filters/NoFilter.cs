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
        /// Reset filter state - no effect for this filter type
        /// </summary>
        public void Reset() 
        {
            // No state to reset
        }
        
        /// <summary>
        /// Draw the filter's parameter controls in the current GUI layout
        /// </summary>
        /// <param name="logger">Logger for UI events</param>
        public void DrawParameterControls(LoggingManager logger)
        {
            // No parameters to display for this filter
            GUILayout.Label("This filter has no adjustable parameters.");
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
