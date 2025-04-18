using UnityEngine;

namespace Inventonater.BleHid
{
    [DefaultExecutionOrder(-1000)]
    class Bootstrap : MonoBehaviour
    {
        void Awake()
        {
            // Make the engine tick even when the Activity isn’t focused
            Application.runInBackground = true;
        }
    }
}