using UnityEngine;

namespace Inventonater.BleHid.UI.Filters
{
    /// <summary>
    /// Simple Exponential Moving Average (EMA) filter for mouse input
    /// </summary>
    public class ExponentialMovingAverageFilter : IInputFilter
    {
        // Filter parameters
        private float alpha;        // Smoothing factor (0-1): higher = less smoothing
        private float minChange;    // Minimum change threshold
        
        // Filter state
        private Vector2 lastValue;  // Last filtered vector
        private bool initialized;   // Whether filter has been initialized
        
        /// <summary>
        /// Display name of the filter for UI
        /// </summary>
        public string Name => "EMA Filter";
        
        /// <summary>
        /// Brief description of how the filter works
        /// </summary>
        public string Description => "Simple low-pass filter with consistent smoothing";
        
        /// <summary>
        /// Creates a new instance of the EMA Filter
        /// </summary>
        /// <param name="alpha">Smoothing factor (0-1): higher = less smoothing, default: 0.5</param>
        /// <param name="minChange">Minimum change threshold to process, default: 0.0001</param>
        public ExponentialMovingAverageFilter(float alpha = 0.5f, float minChange = 0.0001f)
        {
            this.alpha = Mathf.Clamp01(alpha);
            this.minChange = minChange;
            Reset();
        }
        
        /// <summary>
        /// Reset filter state
        /// </summary>
        public void Reset()
        {
            initialized = false;
            lastValue = Vector2.zero;
        }
        
        /// <summary>
        /// Update filter parameters
        /// </summary>
        /// <param name="alpha">Smoothing factor (0-1)</param>
        /// <param name="minChange">Minimum change threshold</param>
        public void SetParameters(float alpha, float minChange)
        {
            this.alpha = Mathf.Clamp01(alpha);
            this.minChange = minChange;
        }
        
        /// <summary>
        /// Filter a 2D vector using exponential moving average
        /// </summary>
        /// <param name="point">Input vector</param>
        /// <param name="timestamp">Current timestamp (unused in this filter)</param>
        /// <returns>Filtered output vector</returns>
        public Vector2 Filter(Vector2 point, float timestamp)
        {
            // Initialize if needed
            if (!initialized)
            {
                lastValue = point;
                initialized = true;
                return point;
            }
            
            // Apply EMA formula to the entire vector: output = alpha * current + (1 - alpha) * lastOutput
            Vector2 filteredValue = (alpha * point) + ((1 - alpha) * lastValue);
            
            // Only update if change is significant
            if ((filteredValue - lastValue).sqrMagnitude >= minChange * minChange)
            {
                lastValue = filteredValue;
            }
            
            return lastValue;
        }
    }
}
