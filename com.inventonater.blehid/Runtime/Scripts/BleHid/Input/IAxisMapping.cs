using UnityEngine;

namespace Inventonater.BleHid
{
    public interface IAxisMapping
    {
        public void SetValue(Vector3 absolutePosition);
        void Update(float time);
        void ResetPosition();
    }
}
