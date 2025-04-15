using Inventonater.BleHid;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Implementation of the 1€ Filter for mouse pointer smoothing
    /// Original algorithm: https://cristal.univ-lille.fr/~casiez/1euro/
    /// </summary>
    public class OneEuroFilter : IInputFilter
    {
        // Filter parameters
        private float _minCutoff;    // Minimum cutoff frequency
        private float _beta;         // Cutoff slope (speed coefficient)
        private float _dcutoff;      // Derivative cutoff frequency
        
        // Filter state
        private Vector2 _xPrev;     // Previous filtered position vector
        private Vector2 _dxPrev;    // Previous derivative vector
        private float _lastTime;     // Last update timestamp
        private bool _initialized;   // Whether filter has been initialized
        
        /// <summary>
        /// Display name of the filter for UI
        /// </summary>
        public string Name => "1€ Filter";
        
        /// <summary>
        /// Brief description of how the filter works
        /// </summary>
        public string Description => "Adaptive filter that adjusts smoothing based on movement speed";
        
        /// <summary>
        /// Creates a new instance of the 1€ Filter
        /// </summary>
        /// <param name="minCutoff">Minimum cutoff frequency (default: 1.0)</param>
        /// <param name="beta">Beta parameter for speed coefficient (default: 0.007)</param>
        /// <param name="dcutoff">Derivative cutoff frequency (default: 1.0)</param>
        public OneEuroFilter(float minCutoff = 1.0f, float beta = 0.007f, float dcutoff = 1.0f)
        {
            _minCutoff = minCutoff;
            _beta = beta;
            _dcutoff = dcutoff;
            Reset();
        }
        
        /// <summary>
        /// Reset filter state
        /// </summary>
        public void Reset()
        {
            _initialized = false;
            _xPrev = Vector2.zero;
            _dxPrev = Vector2.zero;
            _lastTime = 0;
        }
        
        /// <summary>
        /// Configure filter parameters with meaningful names
        /// </summary>
        /// <param name="minCutoff">Minimum cutoff frequency (smoothing amount)</param>
        /// <param name="beta">Beta parameter (speed sensitivity)</param>
        public void Configure(float minCutoff, float beta)
        {
            _minCutoff = minCutoff;
            _beta = beta;
        }
        
        /// <summary>
        /// Draw the filter's parameter controls in the current GUI layout
        /// </summary>
        /// <param name="logger">Logger for UI events</param>
        public void DrawParameterControls(LoggingManager logger)
        {
            // Draw min cutoff slider (smoothing strength)
            GUILayout.Label("Smoothing: Adjusts filtering strength");
            float newMinCutoff = UIHelper.SliderWithLabels(
                "Strong", _minCutoff, 0.1f, 5.0f, "Light", 
                "Smoothing: {0:F2}", UIHelper.StandardSliderOptions);
                
            if (newMinCutoff != _minCutoff)
            {
                _minCutoff = newMinCutoff;
                logger.AddLogEntry($"Changed 1€ filter smoothing to: {_minCutoff:F2}");
            }
            
            // Draw beta slider (speed response)
            GUILayout.Label("Response: Adjusts sensitivity to rapid movement");
            float newBeta = UIHelper.SliderWithLabels(
                "Low", _beta, 0.001f, 0.1f, "High", 
                "Response: {0:F3}", UIHelper.StandardSliderOptions);
                
            if (newBeta != _beta)
            {
                _beta = newBeta;
                logger.AddLogEntry($"Changed 1€ filter response to: {_beta:F3}");
            }
        }
        
        /// <summary>
        /// Low-pass filter
        /// </summary>
        private float LowPassFilter(float x, float alpha, float y_prev)
        {
            return alpha * x + (1.0f - alpha) * y_prev;
        }
        
        /// <summary>
        /// Compute alpha value for filter
        /// </summary>
        private float ComputeAlpha(float cutoff, float deltaTime)
        {
            float tau = 1.0f / (2.0f * Mathf.PI * cutoff);
            return 1.0f / (1.0f + tau / deltaTime);
        }
        
        /// <summary>
        /// Filter a 2D vector using the 1€ algorithm
        /// </summary>
        /// <param name="point">Input vector</param>
        /// <param name="timestamp">Current timestamp (seconds)</param>
        /// <returns>Filtered output vector</returns>
        public Vector2 Filter(Vector2 point, float timestamp)
        {
            if (!_initialized)
            {
                _xPrev = point;
                _dxPrev = Vector2.zero;
                _lastTime = timestamp;
                _initialized = true;
                return point;
            }
            
            float deltaTime = timestamp - _lastTime;
            if (deltaTime <= 0.0f) deltaTime = 0.001f; // Prevent division by zero
            _lastTime = timestamp;
            
            // Estimate derivative as a vector
            Vector2 dx = (point - _xPrev) / deltaTime;
            
            // Filter derivative components
            float alpha_d = ComputeAlpha(_dcutoff, deltaTime);
            Vector2 edx = new Vector2(
                LowPassFilter(dx.x, alpha_d, _dxPrev.x),
                LowPassFilter(dx.y, alpha_d, _dxPrev.y)
            );
            _dxPrev = edx;
            
            // Adjust cutoff based on derivative magnitude
            float cutoff = _minCutoff + _beta * edx.magnitude;
            
            // Filter signal components
            float alpha_c = ComputeAlpha(cutoff, deltaTime);
            Vector2 ex = new Vector2(
                LowPassFilter(point.x, alpha_c, _xPrev.x),
                LowPassFilter(point.y, alpha_c, _xPrev.y)
            );
            _xPrev = ex;
            
            return ex;
        }
    }
}
