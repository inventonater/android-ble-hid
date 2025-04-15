using Inventonater.BleHid;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Simple Exponential Moving Average (EMA) filter for mouse input
    /// </summary>
    public class ExponentialMovingAverageFilter : IInputFilter
    {
        private static LoggingManager Logger => LoggingManager.Instance;

        // Filter parameters
        private float _alpha;        // Smoothing factor (0-1): higher = less smoothing
        private float _minChange;    // Minimum change threshold
        
        // Filter state
        private Vector2 _lastValue;  // Last filtered vector
        private bool _initialized;   // Whether filter has been initialized
        
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
            _alpha = Mathf.Clamp01(alpha);
            _minChange = minChange;
            Reset();
        }
        
        /// <summary>
        /// Reset filter state
        /// </summary>
        public void Reset()
        {
            _initialized = false;
            _lastValue = Vector2.zero;
        }
        
        /// <summary>
        /// Draw the filter's parameter controls in the current GUI layout
        /// </summary>
        /// <param name="logger">Logger for UI events</param>
        public void DrawParameterControls()
        {
            // Draw alpha slider (smoothing amount)
            GUILayout.Label("Smoothing Factor: Adjusts filtering strength");
            float newAlpha = UIHelper.SliderWithLabels(
                "Strong", _alpha, 0.05f, 1.0f, "Light", 
                "Smoothing: {0:F2}", UIHelper.StandardSliderOptions);
                
            if (newAlpha != _alpha)
            {
                _alpha = newAlpha;
                Logger.AddLogEntry($"Changed EMA filter smoothing to: {_alpha:F2}");
            }
            
            // Draw min change threshold
            GUILayout.Label("Minimum Change: Threshold for movement detection");
            float newMinChange = UIHelper.SliderWithLabels(
                "Low", _minChange, 0.0001f, 0.01f, "High", 
                "Min Change: {0:F4}", UIHelper.StandardSliderOptions);
                
            if (newMinChange != _minChange)
            {
                _minChange = newMinChange;
                Logger.AddLogEntry($"Changed EMA filter minimum change to: {_minChange:F4}");
            }
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
            if (!_initialized)
            {
                _lastValue = point;
                _initialized = true;
                return point;
            }
            
            // Apply EMA formula to the entire vector: output = alpha * current + (1 - alpha) * lastOutput
            Vector2 filteredValue = (_alpha * point) + ((1 - _alpha) * _lastValue);
            
            // Only update if change is significant
            if ((filteredValue - _lastValue).sqrMagnitude >= _minChange * _minChange)
            {
                _lastValue = filteredValue;
            }
            
            return _lastValue;
        }
    }
}
