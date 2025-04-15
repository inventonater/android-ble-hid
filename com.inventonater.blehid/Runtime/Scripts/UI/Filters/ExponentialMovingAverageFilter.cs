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
        private float lastValue;    // Last filtered value
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
            lastValue = 0;
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
        /// Filter a single float value
        /// </summary>
        /// <param name="value">Input value</param>
        /// <param name="timestamp">Current timestamp (unused in this filter)</param>
        /// <returns>Filtered output value</returns>
        public float Filter(float value, float timestamp)
        {
            // Initialize if needed
            if (!initialized)
            {
                lastValue = value;
                initialized = true;
                return value;
            }
            
            // Apply EMA formula: output = alpha * current + (1 - alpha) * lastOutput
            float filteredValue = (alpha * value) + ((1 - alpha) * lastValue);
            
            // Only update if change is significant
            if (Mathf.Abs(filteredValue - lastValue) >= minChange)
            {
                lastValue = filteredValue;
            }
            
            return lastValue;
        }
        
        /// <summary>
        /// Filter a 2D vector by applying the filter separately to each component
        /// </summary>
        /// <param name="point">Input vector</param>
        /// <param name="timestamp">Current timestamp (unused in this filter)</param>
        /// <returns>Filtered output vector</returns>
        public Vector2 Filter(Vector2 point, float timestamp)
        {
            return new Vector2(
                Filter(point.x, timestamp),
                Filter(point.y, timestamp)
            );
        }
    }
}
