using UnityEngine;

namespace Inventonater
{
    public interface IAxisMapping
    {
        public void AddDelta(Vector3 delta);
        void Update(float time);
        void Handle(InputEvent pendingButtonEvent);
    }
}
