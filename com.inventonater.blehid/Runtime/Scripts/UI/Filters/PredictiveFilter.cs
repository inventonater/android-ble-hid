using UnityEngine;
using System.Collections.Generic;

namespace Inventonater.BleHid.UI.Filters
{
    /// <summary>
    /// Predictive filter that extrapolates movement to compensate for perceived latency
    /// </summary>
    public class PredictiveFilter : IInputFilter
    {
        // Filter parameters
        private float _predictionTime;  // How far ahead to predict (seconds)
        private float _smoothingFactor; // Smoothing applied to velocity estimation
        
        // Filter state
        private readonly List<Vector2> _positions;   // History of recent positions
        private readonly List<float> _timestamps;    // Timestamps for positions
        private readonly int _historySize = 5;       // How many samples to keep
        private Vector2 _lastOutput;                 // Last filtered output
        private Vector2 _estimatedVelocity;          // Current velocity estimate
        private bool _initialized;                   // Whether filter has been initialized
        
        /// <summary>
        /// Display name of the filter for UI
        /// </summary>
        public string Name => "Predictive Filter";
        
        /// <summary>
        /// Brief description of how the filter works
        /// </summary>
        public string Description => "Reduces perceived lag by predicting future position";
        
        /// <summary>
        /// Creates a new instance of the Predictive Filter
        /// </summary>
        /// <param name="predictionTime">Time to look ahead (seconds), default: 0.05</param>
        /// <param name="smoothingFactor">Smoothing applied to velocity estimation (0-1), default: 0.5</param>
        public PredictiveFilter(float predictionTime = 0.05f, float smoothingFactor = 0.5f)
        {
            _predictionTime = Mathf.Max(0f, predictionTime);
            _smoothingFactor = Mathf.Clamp01(smoothingFactor);
            
            _positions = new List<Vector2>(_historySize);
            _timestamps = new List<float>(_historySize);
            Reset();
        }
        
        /// <summary>
        /// Reset filter state
        /// </summary>
        public void Reset()
        {
            _initialized = false;
            _positions.Clear();
            _timestamps.Clear();
            _lastOutput = Vector2.zero;
            _estimatedVelocity = Vector2.zero;
        }
        
        /// <summary>
        /// Update filter parameters
        /// </summary>
        /// <param name="predictionTime">Time to look ahead (seconds)</param>
        /// <param name="smoothingFactor">Smoothing applied to velocity estimation (0-1)</param>
        public void SetParameters(float predictionTime, float smoothingFactor)
        {
            _predictionTime = Mathf.Max(0f, predictionTime);
            _smoothingFactor = Mathf.Clamp01(smoothingFactor);
        }
        
        /// <summary>
        /// Add a new sample to the history buffer
        /// </summary>
        private void AddSample(float value, float timestamp)
        {
            // Add as Vector2 with Y=0 for consistency
            AddSample(new Vector2(value, 0), timestamp);
        }
        
        /// <summary>
        /// Add a new sample to the history buffer
        /// </summary>
        private void AddSample(Vector2 position, float timestamp)
        {
            // Add to history
            _positions.Add(position);
            _timestamps.Add(timestamp);
            
            // Maintain fixed history size
            if (_positions.Count > _historySize)
            {
                _positions.RemoveAt(0);
                _timestamps.RemoveAt(0);
            }
        }
        
        /// <summary>
        /// Estimate velocity based on recent history
        /// </summary>
        private Vector2 EstimateVelocity()
        {
            // Need at least 2 samples to estimate velocity
            if (_positions.Count < 2)
                return Vector2.zero;
            
            int lastIdx = _positions.Count - 1;
            int firstIdx = 0;
            
            // Calculate time span
            float deltaTime = _timestamps[lastIdx] - _timestamps[firstIdx];
            if (deltaTime <= 0.001f)
                return Vector2.zero;
            
            // Calculate position change
            Vector2 deltaPos = _positions[lastIdx] - _positions[firstIdx];
            
            // Calculate instantaneous velocity
            Vector2 instantVelocity = deltaPos / deltaTime;
            
            // Apply smoothing to velocity
            _estimatedVelocity = Vector2.Lerp(_estimatedVelocity, instantVelocity, _smoothingFactor);
            
            return _estimatedVelocity;
        }
        
        /// <summary>
        /// Filter a single float value
        /// </summary>
        /// <param name="value">Input value</param>
        /// <param name="timestamp">Current timestamp</param>
        /// <returns>Filtered (predicted) output value</returns>
        public float Filter(float value, float timestamp)
        {
            // Initialize if needed
            if (!_initialized)
            {
                AddSample(value, timestamp);
                _lastOutput = new Vector2(value, 0);
                _initialized = true;
                return value;
            }
            
            // Add to history
            AddSample(value, timestamp);
            
            // Estimate velocity
            Vector2 velocity = EstimateVelocity();
            
            // Calculate predicted position
            float predictedValue = value + (velocity.x * _predictionTime);
            
            // Update last output
            _lastOutput = new Vector2(predictedValue, 0);
            
            return predictedValue;
        }
        
        /// <summary>
        /// Filter a 2D vector
        /// </summary>
        /// <param name="point">Input vector</param>
        /// <param name="timestamp">Current timestamp</param>
        /// <returns>Filtered (predicted) output vector</returns>
        public Vector2 Filter(Vector2 point, float timestamp)
        {
            // Initialize if needed
            if (!_initialized)
            {
                AddSample(point, timestamp);
                _lastOutput = point;
                _initialized = true;
                return point;
            }
            
            // Add to history
            AddSample(point, timestamp);
            
            // Estimate velocity
            Vector2 velocity = EstimateVelocity();
            
            // Calculate predicted position
            Vector2 predictedPosition = point + (velocity * _predictionTime);
            
            // Update last output
            _lastOutput = predictedPosition;
            
            return predictedPosition;
        }
    }
}
