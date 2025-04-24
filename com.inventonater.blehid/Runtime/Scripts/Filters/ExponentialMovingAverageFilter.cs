using Inventonater.BleHid;
using UnityEngine;
using Newtonsoft.Json;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Simple exponential moving average filter
    /// </summary>
    public class ExponentialMovingAverageFilter : IInputFilter
    {
        [JsonProperty]
        private float _alpha;
        
        [JsonProperty]
        private float _minChange;
        
        [JsonIgnore]
        private Vector2 _prevFiltered;
        
        [JsonIgnore]
        private bool _initialized;
        
        public string Name => "EMA Filter";
        public string Description => "Simple exponential moving average filter";
        
        public ExponentialMovingAverageFilter(float alpha = 0.5f, float minChange = 0.0001f)
        {
            _alpha = Mathf.Clamp01(alpha);
            _minChange = Mathf.Max(0, minChange);
            Reset();
        }
        
        public void Reset()
        {
            _prevFiltered = Vector2.zero;
            _initialized = false;
        }
        
        public void DrawParameterControls()
        {
            GUILayout.Label("Alpha: Smoothing factor (0-1)");
            float newAlpha = UIHelper.SliderWithLabels(
                "More Smooth", _alpha, 0.05f, 1.0f, "Less Smooth", 
                "Alpha: {0:F2}", UIHelper.StandardSliderOptions);
                
            if (newAlpha != _alpha)
            {
                _alpha = newAlpha;
                LoggingManager.Instance.Log($"Changed EMA filter alpha to: {_alpha:F2}");
            }
            
            GUILayout.Label("Min Change: Threshold for movement");
            float newMinChange = UIHelper.SliderWithLabels(
                "Low", _minChange, 0.0001f, 0.01f, "High", 
                "Min Change: {0:F4}", UIHelper.StandardSliderOptions);
                
            if (newMinChange != _minChange)
            {
                _minChange = newMinChange;
                LoggingManager.Instance.Log($"Changed EMA filter min change to: {_minChange:F4}");
            }
        }
        
        public Vector2 Filter(Vector2 point, float timestamp)
        {
            if (!_initialized)
            {
                _prevFiltered = point;
                _initialized = true;
                return point;
            }
            
            // Apply EMA formula: y[n] = α * x[n] + (1-α) * y[n-1]
            Vector2 filtered = _alpha * point + (1 - _alpha) * _prevFiltered;
            
            // Only update if change is significant
            if ((filtered - _prevFiltered).sqrMagnitude > _minChange * _minChange)
            {
                _prevFiltered = filtered;
            }
            
            return _prevFiltered;
        }
    }
}
