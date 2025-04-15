using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Extension methods for input filters
    /// </summary>
    public static class InputFilterExtensions
    {
        /// <summary>
        /// Default implementation to filter a float by converting to Vector2 and back
        /// This can be used by filters that want this default behavior
        /// </summary>
        public static float FilterAsVector(this IInputFilter filter, float value, float timestamp)
        {
            return filter.Filter(new Vector2(value, 0), timestamp).x;
        }
    }
}
