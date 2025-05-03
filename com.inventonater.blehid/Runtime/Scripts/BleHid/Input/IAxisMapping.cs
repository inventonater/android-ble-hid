using UnityEngine;

namespace Inventonater.BleHid
{
    public interface IAxisMapping
    {
        public void AddDelta(Vector3 delta);
        void Update(float time);
        void Handle(InputEvent pendingButtonEvent);
    }
}
