using UnityEngine;

namespace Inventonater.BleHid.UI.Filters
{
    /// <summary>
    /// Double Exponential Smoothing filter (Holt Filter) that models both
    /// level (position) and trend (velocity)
    /// </summary>
    public class DoubleExponentialFilter : IInputFilter
    {
        // Filter parameters
        private float _alpha;        // Level smoothing factor (0-1)
        private float _beta;         // Trend smoothing factor (0-1)
        
        // Filter state
        private float _level;        // Current smoothed value (position)
        private float _trend;        // Current trend (velocity)
        private bool _initialized;   // Whether filter has been initialized
        
        /// <summary>
        /// Display name of the filter for UI
        /// </summary>
        public string Name => "Double Exponential";
        
        /// <summary>
        /// Brief description of how the filter works
        /// </summary>
        public string Description => "Smoothing filter that models both position and trend (velocity)";
        
        /// <summary>
        /// Creates a new instance of the Double Exponential Filter
        /// </summary>
        /// <param name="alpha">Level smoothing factor (0-1): higher = less smoothing, default: 0.5</param>
        /// <param name="beta">Trend smoothing factor (0-1): higher = faster trend adaptation, default: 0.1</param>
        public DoubleExponentialFilter(float alpha = 0.5f, float beta = 0.1f)
        {
            _alpha = Mathf.Clamp01(alpha);
            _beta = Mathf.Clamp01(beta);
            Reset();
        }
        
        /// <summary>
        /// Reset filter state
        /// </summary>
        public void Reset()
        {
            _initialized = false;
            _level = 0f;
            _trend = 0f;
        }
        
        /// <summary>
        /// Update filter parameters
        /// </summary>
        /// <param name="alpha">Level smoothing factor (0-1)</param>
        /// <param name="beta">Trend smoothing factor (0-1)</param>
        public void SetParameters(float alpha, float beta)
        {
            _alpha = Mathf.Clamp01(alpha);
            _beta = Mathf.Clamp01(beta);
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
            if (!_initialized)
            {
                _level = value;
                _trend = 0f;
                _initialized = true;
                return value;
            }
            
            // Store old level for trend calculation
            float oldLevel = _level;
            
            // Update level (position estimate)
            _level = _alpha * value + (1 - _alpha) * (_level + _trend);
            
            // Update trend (velocity estimate)
            _trend = _beta * (_level - oldLevel) + (1 - _beta) * _trend;
            
            // Return smoothed value with trend component
            return _level;
        }
        
        /// <summary>
        /// Filter a 2D vector by applying the filter separately to each component
        /// </summary>
        /// <param name="point">Input vector</param>
        /// <param name="timestamp">Current timestamp</param>
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
