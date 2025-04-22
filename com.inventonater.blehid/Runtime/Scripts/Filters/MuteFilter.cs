using Inventonater.BleHid;
using UnityEngine;
using Newtonsoft.Json;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Filter that mutes all input (returns zero)
    /// </summary>
    public class MuteFilter : IInputFilter
    {
        [JsonProperty]
        private float _muteTime;
        
        [JsonIgnore]
        private Vector2 _lastPosition;
        
        public string Name => "Mute";
        public string Description => "Mutes all input (returns zero)";
        
        public MuteFilter(float muteTime = 0.0f)
        {
            _muteTime = muteTime;
        }
        
        public void Reset()
        {
            _lastPosition = Vector2.zero;
        }
        
        public void DrawParameterControls()
        {
            GUILayout.Label("This filter mutes all input and returns zero.");
        }
        
        public Vector2 Filter(Vector2 point, float timestamp)
        {
            return Vector2.zero;
        }
    }
}
