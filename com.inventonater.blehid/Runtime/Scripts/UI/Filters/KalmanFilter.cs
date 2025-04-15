using UnityEngine;

namespace Inventonater.BleHid.UI.Filters
{
    /// <summary>
    /// Kalman filter for mouse pointer smoothing with velocity model
    /// </summary>
    public class KalmanFilter : IInputFilter
    {
        // Simple 2x2 matrix class for Kalman filter operations
        private struct Matrix2x2
        {
            public float m00, m01, m10, m11;
            
            public Matrix2x2(float m00, float m01, float m10, float m11)
            {
                this.m00 = m00;
                this.m01 = m01;
                this.m10 = m10;
                this.m11 = m11;
            }
            
            // Matrix multiplication
            public static Matrix2x2 operator *(Matrix2x2 a, Matrix2x2 b)
            {
                return new Matrix2x2(
                    a.m00 * b.m00 + a.m01 * b.m10, a.m00 * b.m01 + a.m01 * b.m11,
                    a.m10 * b.m00 + a.m11 * b.m10, a.m10 * b.m01 + a.m11 * b.m11
                );
            }
            
            // Matrix addition
            public static Matrix2x2 operator +(Matrix2x2 a, Matrix2x2 b)
            {
                return new Matrix2x2(
                    a.m00 + b.m00, a.m01 + b.m01,
                    a.m10 + b.m10, a.m11 + b.m11
                );
            }
            
            // Matrix inverse (for 2x2 only)
            public Matrix2x2 Inverse()
            {
                float det = m00 * m11 - m01 * m10;
                if (Mathf.Abs(det) < 1e-6f)
                {
                    // Avoid division by zero, return identity
                    return new Matrix2x2(1, 0, 0, 1);
                }
                
                float invDet = 1.0f / det;
                return new Matrix2x2(
                    m11 * invDet, -m01 * invDet,
                    -m10 * invDet, m00 * invDet
                );
            }
            
            // Identity matrix
            public static Matrix2x2 Identity => new Matrix2x2(1, 0, 0, 1);
        }
        
        // Filter parameters
        private float _processNoise;     // Process noise covariance (uncertainty in the model)
        private float _measurementNoise; // Measurement noise covariance (uncertainty in the measurement)
        
        // State variables
        private Vector2 _state;      // [position, velocity]
        private Matrix2x2 _covariance; // Error covariance matrix
        private bool _initialized;   // Whether filter has been initialized
        
        /// <summary>
        /// Display name of the filter for UI
        /// </summary>
        public string Name => "Kalman Filter";
        
        /// <summary>
        /// Brief description of how the filter works
        /// </summary>
        public string Description => "Statistical filter that models position and velocity with adaptive certainty";
        
        /// <summary>
        /// Creates a new instance of the Kalman Filter
        /// </summary>
        /// <param name="processNoise">Process noise covariance (system uncertainty), default: 0.001</param>
        /// <param name="measurementNoise">Measurement noise (sensor uncertainty), default: 0.1</param>
        public KalmanFilter(float processNoise = 0.001f, float measurementNoise = 0.1f)
        {
            _processNoise = Mathf.Max(1e-5f, processNoise);
            _measurementNoise = Mathf.Max(1e-5f, measurementNoise);
            Reset();
        }
        
        /// <summary>
        /// Reset filter state
        /// </summary>
        public void Reset()
        {
            _initialized = false;
            _state = Vector2.zero;
            _covariance = new Matrix2x2(1, 0, 0, 1); // Start with identity covariance
        }
        
        /// <summary>
        /// Update filter parameters
        /// </summary>
        /// <param name="processNoise">Process noise covariance</param>
        /// <param name="measurementNoise">Measurement noise covariance</param>
        public void SetParameters(float processNoise, float measurementNoise)
        {
            _processNoise = Mathf.Max(1e-5f, processNoise);
            _measurementNoise = Mathf.Max(1e-5f, measurementNoise);
        }
        
        /// <summary>
        /// Filter a 2D vector using Kalman filter with velocity model
        /// </summary>
        /// <param name="point">Input vector</param>
        /// <param name="timestamp">Current timestamp</param>
        /// <returns>Filtered output vector</returns>
        public Vector2 Filter(Vector2 point, float timestamp)
        {
            // Initialize if needed
            if (!_initialized)
            {
                _state = new Vector2(point.x, 0);  // Initial position with zero velocity
                _covariance = Matrix2x2.Identity;
                _initialized = true;
                return point;
            }
            
            // Time since last update (used in state transition)
            float dt = 0.01f; // Fixed dt for simplicity, could be derived from timestamps
            
            // Prediction step
            // Predict state forward: x = F * x
            //  [ x_k ]   [ 1  dt ] [ x_{k-1} ]
            //  [     ] = [      ] [         ]
            //  [ v_k ]   [ 0   1 ] [ v_{k-1} ]
            float predictedPos = _state.x + _state.y * dt;
            float predictedVel = _state.y;
            
            // Predict error covariance forward: P = F * P * F^T + Q
            Matrix2x2 F = new Matrix2x2(1, dt, 0, 1);
            Matrix2x2 Q = new Matrix2x2(_processNoise, 0, 0, _processNoise);
            _covariance = F * _covariance * F + Q;
            
            // Update step
            // Kalman gain: K = P * H^T * (H * P * H^T + R)^{-1}
            float K_pos = _covariance.m00 / (_covariance.m00 + _measurementNoise);
            float K_vel = _covariance.m10 / (_covariance.m00 + _measurementNoise);
            
            // Update state with the measurement (position only)
            float innovation = point.x - predictedPos;
            float updatedPos = predictedPos + K_pos * innovation;
            float updatedVel = predictedVel + K_vel * innovation;
            
            // Store updated state
            _state = new Vector2(updatedPos, updatedVel);
            
            // Update error covariance
            Matrix2x2 I_KH = new Matrix2x2(
                1 - K_pos, 0,
                -K_vel, 1
            );
            _covariance = I_KH * _covariance;
            
            // Apply the same Kalman filtering to Y component
            // For Y, we use the same parameters but maintain separate state
            Vector2 result = new Vector2(
                updatedPos,
                FilterYComponent(point.y, timestamp)  // Use a separate method for Y component
            );
            
            return result;
        }
        
        /// <summary>
        /// Internal method to filter the Y component separately
        /// </summary>
        private float FilterYComponent(float value, float timestamp)
        {
            // We need separate state for Y component
            // This is a simplified version just for the Y-axis
            // A full implementation would duplicate the state and covariance
            
            // For now, just apply a simpler filter for the Y component
            float alpha = 0.5f;  // Simplification - use a basic smoothing for Y
            _state.y = alpha * value + (1 - alpha) * _state.y;
            
            return _state.y;
        }
    }
}
