using UnityEngine;

namespace Inventonater.BleHid
{
    public interface IAxisMapping
    {
        public void SetPositionDelta(Vector3 delta);
        void Update(float time);
        void Handle(BleHidButtonEvent pendingButtonEvent);
        void Handle(BleHidDirection pendingDirection);
    }
}
