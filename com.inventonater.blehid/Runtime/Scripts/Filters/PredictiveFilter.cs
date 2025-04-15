using Inventonater.BleHid;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Predictive filter that attempts to compensate for latency by
    /// predicting where the pointer will be in the near future
    /// </summary>
    public class PredictiveFilter : IInputFilter
    {
        private LoggingManager Logger => LoggingManager.Instance;

        // Filter parameters
        private float _predictionTime;    // Time to predict ahead (seconds)
        private float _smoothingFactor;   // Smoothing factor for velocity (0-1)
        
        // Filter state
        private Vector2 _lastPosition;    // Last input position
        private Vector2 _velocity;        // Estimated velocity
        private float _lastTime;          // Last update timestamp
        private bool _initialized;        // Whether filter has been initialized
        
        /// <summary>
        /// Display name of the filter for UI
        /// </summary>
        public string Name => "Predictive";
        
        /// <summary>
        /// Brief description of how the filter works
        /// </summary>
        public string Description => "Attempts to reduce perceived latency by predicting future position";
        
        /// <summary>
        /// Creates a new instance of the Predictive Filter
        /// </summary>
        /// <param name="predictionTime">Time to predict ahead in seconds (default: 0.05)</param>
        /// <param name="smoothingFactor">Velocity smoothing factor (default: 0.5)</param>
        public PredictiveFilter(float predictionTime = 0.05f, float smoothingFactor = 0.5f)
        {
            _predictionTime = Mathf.Clamp(predictionTime, 0.01f, 0.2f);
            _smoothingFactor = Mathf.Clamp01(smoothingFactor);
            Reset();
        }
        
        /// <summary>
        /// Reset filter state
        /// </summary>
        public void Reset()
        {
            _initialized = false;
            _lastPosition = Vector2.zero;
            _velocity = Vector2.zero;
            _lastTime = 0;
        }

        /// <summary>
        /// Draw the filter's parameter controls in the current GUI layout
        /// </summary>
        public void DrawParameterControls()
        {
            // Draw prediction time slider
            GUILayout.Label("Prediction Time: How far ahead to predict (higher = more aggressive)");
            float newPredictionTime = UIHelper.SliderWithLabels(
                "Short", _predictionTime, 0.01f, 0.2f, "Long", 
                "Prediction: {0:F3}s", UIHelper.StandardSliderOptions);
                
            if (newPredictionTime != _predictionTime)
            {
                _predictionTime = newPredictionTime;
                Logger.AddLogEntry($"Changed prediction time to: {_predictionTime:F3}s");
            }
            
            // Draw smoothing factor slider
            GUILayout.Label("Smoothing: Controls velocity calculation smoothness");
            float newSmoothingFactor = UIHelper.SliderWithLabels(
                "Strong", _smoothingFactor, 0.1f, 0.9f, "Light", 
                "Smoothing: {0:F2}", UIHelper.StandardSliderOptions);
                
            if (newSmoothingFactor != _smoothingFactor)
            {
                _smoothingFactor = newSmoothingFactor;
                Logger.AddLogEntry($"Changed predictive filter smoothing to: {_smoothingFactor:F2}");
            }
        }
        
        /// <summary>
        /// Filter a 2D vector using predictive filtering
        /// </summary>
        /// <param name="point">Input vector</param>
        /// <param name="timestamp">Current timestamp (seconds)</param>
        /// <returns>Filtered (predicted) output vector</returns>
        public Vector2 Filter(Vector2 point, float timestamp)
        {
            // Initialize if needed
            if (!_initialized)
            {
                _lastPosition = point;
                _velocity = Vector2.zero;
                _lastTime = timestamp;
                _initialized = true;
                return point;
            }
            
            // Calculate time delta
            float dt = timestamp - _lastTime;
            if (dt <= 0.0f) dt = 0.001f; // Prevent division by zero
            _lastTime = timestamp;
            
            // Compute instantaneous velocity
            Vector2 instantVelocity = (point - _lastPosition) / dt;
            
            // Smooth velocity estimate with exponential moving average
            _velocity = _smoothingFactor * instantVelocity + (1 - _smoothingFactor) * _velocity;
            
            // Update last position for next frame
            _lastPosition = point;
            
            // Predict future position
            Vector2 prediction = point + (_velocity * _predictionTime);
            
            return prediction;
        }
    }
}
