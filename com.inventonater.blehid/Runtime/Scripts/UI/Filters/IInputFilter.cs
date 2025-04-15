using UnityEngine;

namespace Inventonater.BleHid.UI.Filters
{
    /// <summary>
    /// Interface for all input filtering implementations used for mouse movement smoothing
    /// </summary>
    public interface IInputFilter
    {
        /// <summary>
        /// Display name of the filter for UI
        /// </summary>
        string Name { get; }
        
        /// <summary>
        /// Brief description of how the filter works
        /// </summary>
        string Description { get; }
        
        /// <summary>
        /// Filter a 2D vector - primary method for unified vector filtering
        /// </summary>
        /// <param name="point">Input vector</param>
        /// <param name="timestamp">Current timestamp (seconds)</param>
        /// <returns>Filtered output vector</returns>
        Vector2 Filter(Vector2 point, float timestamp);

        /// <summary>
        /// Update filter parameters
        /// </summary>
        /// <param name="param1">First parameter (meaning depends on filter type)</param>
        /// <param name="param2">Second parameter (meaning depends on filter type)</param>
        void SetParameters(float param1, float param2);
        
        /// <summary>
        /// Reset filter state
        /// </summary>
        void Reset();
    }
}
