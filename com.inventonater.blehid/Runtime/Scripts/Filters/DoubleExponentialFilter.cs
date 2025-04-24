using Inventonater.BleHid;
using UnityEngine;
using Newtonsoft.Json;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Double exponential smoothing filter (Holt's method)
    /// </summary>
    public class DoubleExponentialFilter : IInputFilter
    {
        [JsonProperty]
        private float _alpha; // Level smoothing factor
        
        [JsonProperty]
        private float _beta;  // Trend smoothing factor
        
        [JsonIgnore]
        private Vector2 _level;
        
        [JsonIgnore]
        private Vector2 _trend;
        
        [JsonIgnore]
        private bool _initialized;
        
        public string Name => "Double Exp";
        public string Description => "Double exponential smoothing filter (Holt's method)";
        
        public DoubleExponentialFilter(float alpha = 0.5f, float beta = 0.1f)
        {
            _alpha = Mathf.Clamp01(alpha);
            _beta = Mathf.Clamp01(beta);
            Reset();
        }
        
        public void Reset()
        {
            _level = Vector2.zero;
            _trend = Vector2.zero;
            _initialized = false;
        }
        
        public void DrawParameterControls()
        {
            GUILayout.Label("Alpha: Level smoothing factor");
            float newAlpha = UIHelper.SliderWithLabels(
                "More Smooth", _alpha, 0.1f, 0.9f, "Less Smooth", 
                "Alpha: {0:F2}", UIHelper.StandardSliderOptions);
                
            if (newAlpha != _alpha)
            {
                _alpha = newAlpha;
                LoggingManager.Instance.Log($"Changed double exp filter alpha to: {_alpha:F2}");
            }
            
            GUILayout.Label("Beta: Trend smoothing factor");
            float newBeta = UIHelper.SliderWithLabels(
                "More Stable", _beta, 0.01f, 0.5f, "More Responsive", 
                "Beta: {0:F2}", UIHelper.StandardSliderOptions);
                
            if (newBeta != _beta)
            {
                _beta = newBeta;
                LoggingManager.Instance.Log($"Changed double exp filter beta to: {_beta:F2}");
            }
        }
        
        public Vector2 Filter(Vector2 point, float timestamp)
        {
            if (!_initialized)
            {
                _level = point;
                _trend = Vector2.zero;
                _initialized = true;
                return point;
            }
            
            // Store previous level
            Vector2 prevLevel = _level;
            
            // Update level: level = α * observation + (1 - α) * (prevLevel + prevTrend)
            _level = _alpha * point + (1 - _alpha) * (prevLevel + _trend);
            
            // Update trend: trend = β * (level - prevLevel) + (1 - β) * prevTrend
            _trend = _beta * (_level - prevLevel) + (1 - _beta) * _trend;
            
            // Return level (could also return level + trend for one-step forecast)
            return _level;
        }
    }
}
