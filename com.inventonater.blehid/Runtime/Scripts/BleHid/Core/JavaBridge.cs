using System;
using Unity.Profiling;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class JavaBridge
    {
        private AndroidJavaObject JavaObject { get; }

        public JavaBridge()
        {
            if (Application.platform == RuntimePlatform.Android)
            {
                AndroidJavaClass bridgeClass = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityBridge");
                JavaObject = bridgeClass.CallStatic<AndroidJavaObject>("getInstance");
            }
        }

        ~JavaBridge()
        {
            if (JavaObject == null) return;
            try { JavaObject.Call("close"); }
            catch (Exception e) { Debug.LogException(e); }
            JavaObject.Dispose();
        }

        static readonly ProfilerMarker _marker = new("BleHid.BleBridgeCaller.Call");
        private readonly bool _verbose = true;

        public void Call(string methodName, params object[] args)
        {
            using var profilerMarker = _marker.Auto();
            if (_verbose) LoggingManager.Instance.AddLogEntry($" -- {methodName} {string.Join(", ", args)}");
            JavaObject?.Call(methodName, args);
        }

        public T Call<T>(string methodName, params object[] args)
        {
            using var profilerMarker = _marker.Auto();
            if (_verbose) LoggingManager.Instance.AddLogEntry($" -- {methodName} {string.Join(", ", args)}");
            return JavaObject != null ? JavaObject.Call<T>(methodName, args) : default;
        }
    }
}
