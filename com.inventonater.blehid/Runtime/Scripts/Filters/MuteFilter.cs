using UnityEngine;

namespace Inventonater.BleHid
{
    public class MuteFilter : IInputFilter
    {
        public string Name => "Block All Motion";
        public string Description => "Preventing Mouse Motion";
        public void Reset() { }
        public void DrawParameterControls() { GUILayout.Label("This filter has no adjustable parameters."); }
        public Vector2 Filter(Vector2 point, float timestamp) => Vector2.zero;
    }
}
