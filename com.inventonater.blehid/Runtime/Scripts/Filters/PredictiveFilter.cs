using Inventonater.BleHid;
using UnityEngine;
using Newtonsoft.Json;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Predictive filter that attempts to compensate for latency
    /// </summary>
    public class PredictiveFilter : IInputFilter
    {
        [JsonProperty]
        private float _predictionTime; // Time to predict ahead in seconds
        
        [JsonProperty]
        private float _smoothingFactor; // Smoothing factor for velocity calculation
        
        [JsonIgnore]
        private Vector2 _lastPosition;
        
        [JsonIgnore]
        private Vector2 _velocity;
        
        [JsonIgnore]
        private float _lastTimestamp;
        
        [JsonIgnore]
        private bool _initialized;
        
        public string Name => "Predictive";
        public string Description => "Predicts future position to compensate for latency";
        
        public PredictiveFilter(float predictionTime = 0.05f, float smoothingFactor = 0.5f)
        {
            _predictionTime = Mathf.Max(0.01f, predictionTime);
            _smoothingFactor = Mathf.Clamp01(smoothingFactor);
            Reset();
        }
        
        public void Reset()
        {
            _lastPosition = Vector2.zero;
            _velocity = Vector2.zero;
            _lastTimestamp = 0;
            _initialized = false;
        }
        
        public void DrawParameterControls()
        {
            GUILayout.Label("Prediction Time: How far ahead to predict (seconds)");
            float newPredictionTime = UIHelper.SliderWithLabels(
                "Less", _predictionTime, 0.01f, 0.2f, "More", 
                "Prediction: {0:F3}s", UIHelper.StandardSliderOptions);
                
            if (newPredictionTime != _predictionTime)
            {
                _predictionTime = newPredictionTime;
                LoggingManager.Instance.AddLogEntry($"Changed prediction time to: {_predictionTime:F3}s");
            }
            
            GUILayout.Label("Smoothing: Velocity smoothing factor");
            float newSmoothingFactor = UIHelper.SliderWithLabels(
                "More Smooth", _smoothingFactor, 0.1f, 0.9f, "Less Smooth", 
                "Smoothing: {0:F2}", UIHelper.StandardSliderOptions);
                
            if (newSmoothingFactor != _smoothingFactor)
            {
                _smoothingFactor = newSmoothingFactor;
                LoggingManager.Instance.AddLogEntry($"Changed velocity smoothing to: {_smoothingFactor:F2}");
            }
        }
        
        public Vector2 Filter(Vector2 point, float timestamp)
        {
            if (!_initialized)
            {
                _lastPosition = point;
                _velocity = Vector2.zero;
                _lastTimestamp = timestamp;
                _initialized = true;
                return point;
            }
            
            // Calculate time delta
            float deltaTime = timestamp - _lastTimestamp;
            if (deltaTime <= 0.0001f) deltaTime = 0.016f; // Default to 60fps if delta is too small
            _lastTimestamp = timestamp;
            
            // Calculate instantaneous velocity
            Vector2 instantVelocity = (point - _lastPosition) / deltaTime;
            
            // Update smoothed velocity using exponential smoothing
            _velocity = _smoothingFactor * instantVelocity + (1 - _smoothingFactor) * _velocity;
            
            // Update last position
            _lastPosition = point;
            
            // Predict future position
            Vector2 predictedPosition = point + _velocity * _predictionTime;
            
            return predictedPosition;
        }
    }
}
