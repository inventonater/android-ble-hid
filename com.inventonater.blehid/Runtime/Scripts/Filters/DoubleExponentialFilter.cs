using Inventonater.BleHid;
using UnityEngine;

namespace Inventonater.BleHid
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
        private Vector2 _level;      // Current smoothed position vector
        private Vector2 _trend;      // Current trend (velocity) vector
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
            _level = Vector2.zero;
            _trend = Vector2.zero;
        }
        
        /// <summary>
        /// Draw the filter's parameter controls in the current GUI layout
        /// </summary>
        /// <param name="logger">Logger for UI events</param>
        public void DrawParameterControls(LoggingManager logger)
        {
            // Draw alpha slider (level smoothing)
            GUILayout.Label("Level Smoothing: Smoothing applied to position");
            float newAlpha = UIHelper.SliderWithLabels(
                "Strong", _alpha, 0.1f, 0.9f, "Light", 
                "Level Smoothing: {0:F2}", UIHelper.StandardSliderOptions);
                
            if (newAlpha != _alpha)
            {
                _alpha = newAlpha;
                logger.AddLogEntry($"Changed double exp. level smoothing to: {_alpha:F2}");
            }
            
            // Draw beta slider (trend smoothing)
            GUILayout.Label("Trend Smoothing: Smoothing applied to velocity");
            float newBeta = UIHelper.SliderWithLabels(
                "Strong", _beta, 0.01f, 0.5f, "Light", 
                "Trend Smoothing: {0:F3}", UIHelper.StandardSliderOptions);
                
            if (newBeta != _beta)
            {
                _beta = newBeta;
                logger.AddLogEntry($"Changed double exp. trend smoothing to: {_beta:F3}");
            }
        }
        
        /// <summary>
        /// Filter a 2D vector using double exponential smoothing
        /// </summary>
        /// <param name="point">Input vector</param>
        /// <param name="timestamp">Current timestamp (unused in this filter)</param>
        /// <returns>Filtered output vector</returns>
        public Vector2 Filter(Vector2 point, float timestamp)
        {
            // Initialize if needed
            if (!_initialized)
            {
                _level = point;
                _trend = Vector2.zero;
                _initialized = true;
                return point;
            }
            
            // Store old level for trend calculation
            Vector2 oldLevel = _level;
            
            // Update level (position estimate) treating the vector as a whole
            _level = _alpha * point + (1 - _alpha) * (_level + _trend);
            
            // Update trend (velocity estimate) treating the vector as a whole
            _trend = _beta * (_level - oldLevel) + (1 - _beta) * _trend;
            
            // Return smoothed value with trend component
            return _level;
        }
    }
}
