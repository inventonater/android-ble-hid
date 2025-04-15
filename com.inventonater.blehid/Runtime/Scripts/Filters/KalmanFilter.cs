using Inventonater.BleHid;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Simple Kalman filter implementation for mouse input with velocity model
    /// </summary>
    public class KalmanFilter : IInputFilter
    {
        private LoggingManager logger => LoggingManager.Instance;
        // Filter parameters
        private float _processNoise;       // Process noise covariance
        private float _measurementNoise;   // Measurement noise covariance
        
        // Filter state
        private Vector2 _state;           // Current state estimate [x, y]
        private Vector2 _stateVelocity;   // Current velocity estimate [vx, vy]
        private Matrix4x4 _errorCovariance; // Error covariance matrix
        private float _lastTime;          // Last update timestamp
        private bool _initialized;        // Whether filter has been initialized
        
        /// <summary>
        /// Display name of the filter for UI
        /// </summary>
        public string Name => "Kalman Filter";
        
        /// <summary>
        /// Brief description of how the filter works
        /// </summary>
        public string Description => "Statistical filter that models uncertainty for optimal smoothing";
        
        /// <summary>
        /// Creates a new instance of the Kalman Filter
        /// </summary>
        /// <param name="processNoise">Process noise covariance (default: 0.001)</param>
        /// <param name="measurementNoise">Measurement noise covariance (default: 0.1)</param>
        public KalmanFilter(float processNoise = 0.001f, float measurementNoise = 0.1f)
        {
            _processNoise = Mathf.Max(0.0001f, processNoise);
            _measurementNoise = Mathf.Max(0.01f, measurementNoise);
            Reset();
        }
        
        /// <summary>
        /// Reset filter state
        /// </summary>
        public void Reset()
        {
            _initialized = false;
            _state = Vector2.zero;
            _stateVelocity = Vector2.zero;
            _lastTime = 0;
            
            // Initialize error covariance matrix with high uncertainty
            _errorCovariance = new Matrix4x4(
                new Vector4(1, 0, 0, 0),
                new Vector4(0, 1, 0, 0),
                new Vector4(0, 0, 1, 0),
                new Vector4(0, 0, 0, 1)
            );
        }

        /// <summary>
        /// Draw the filter's parameter controls in the current GUI layout
        /// </summary>
        public void DrawParameterControls()
        {
            // Draw process noise slider
            GUILayout.Label("Process Noise: Flexibility in motion model");
            float newProcessNoise = UIHelper.SliderWithLabels(
                "Low", _processNoise, 0.0001f, 0.01f, "High", 
                "Process Noise: {0:F5}", UIHelper.StandardSliderOptions);
                
            if (newProcessNoise != _processNoise)
            {
                _processNoise = newProcessNoise;
                logger.AddLogEntry($"Changed Kalman filter process noise to: {_processNoise:F5}");
            }
            
            // Draw measurement noise slider
            GUILayout.Label("Measurement Noise: Trust in input readings");
            float newMeasurementNoise = UIHelper.SliderWithLabels(
                "High Trust", _measurementNoise, 0.01f, 1.0f, "Low Trust", 
                "Measurement Noise: {0:F3}", UIHelper.StandardSliderOptions);
                
            if (newMeasurementNoise != _measurementNoise)
            {
                _measurementNoise = newMeasurementNoise;
                logger.AddLogEntry($"Changed Kalman filter measurement noise to: {_measurementNoise:F3}");
            }
        }
        
        /// <summary>
        /// Filter a 2D vector using Kalman filtering
        /// </summary>
        /// <param name="point">Input vector</param>
        /// <param name="timestamp">Current timestamp (seconds)</param>
        /// <returns>Filtered output vector</returns>
        public Vector2 Filter(Vector2 point, float timestamp)
        {
            // Initialize if needed
            if (!_initialized)
            {
                _state = point;
                _stateVelocity = Vector2.zero;
                _lastTime = timestamp;
                _initialized = true;
                return point;
            }
            
            // Calculate time delta
            float dt = timestamp - _lastTime;
            if (dt <= 0.0f) dt = 0.001f; // Prevent division by zero
            _lastTime = timestamp;
            
            // ----- Prediction Step -----
            
            // Predict state forward based on velocity model
            _state += _stateVelocity * dt;
            
            // Update error covariance matrix for prediction step
            // Simplified model that assumes process noise affects all states equally
            _errorCovariance[0, 0] += dt * dt * _processNoise; // x variance
            _errorCovariance[1, 1] += dt * dt * _processNoise; // y variance
            _errorCovariance[2, 2] += dt * _processNoise;      // vx variance
            _errorCovariance[3, 3] += dt * _processNoise;      // vy variance
            
            // ----- Update/Correction Step -----
            
            // Calculate Kalman gain - simplified for 2D position only
            float kGainX = _errorCovariance[0, 0] / (_errorCovariance[0, 0] + _measurementNoise);
            float kGainY = _errorCovariance[1, 1] / (_errorCovariance[1, 1] + _measurementNoise);
            float kGainVX = _errorCovariance[2, 0] / (_errorCovariance[0, 0] + _measurementNoise);
            float kGainVY = _errorCovariance[3, 1] / (_errorCovariance[1, 1] + _measurementNoise);
            
            // Update state based on measurement
            Vector2 residual = point - _state;
            _state.x += kGainX * residual.x;
            _state.y += kGainY * residual.y;
            _stateVelocity.x += kGainVX * residual.x / dt;
            _stateVelocity.y += kGainVY * residual.y / dt;
            
            // Update error covariance matrix
            _errorCovariance[0, 0] *= (1 - kGainX);
            _errorCovariance[1, 1] *= (1 - kGainY);
            
            return _state;
        }
    }
}
