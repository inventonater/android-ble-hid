using Inventonater.BleHid;
using UnityEngine;
using Newtonsoft.Json;

namespace Inventonater.BleHid
{
    [JsonObject(MemberSerialization.OptIn)]
    public class NoFilter : IInputFilter
    {
        public static readonly NoFilter Instance = new();
        public string Name => "No Filter";
        public string Description => "Direct input with no processing (lowest latency)";
        public void Reset() { }
        public void DrawParameterControls() { GUILayout.Label("This filter has no adjustable parameters."); }
        public Vector2 Filter(Vector2 point, float timestamp) => point;
    }
}
