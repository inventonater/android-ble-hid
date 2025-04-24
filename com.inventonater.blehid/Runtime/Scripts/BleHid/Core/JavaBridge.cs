using System;
using Unity.Profiling;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class JavaBridge
    {
        private AndroidJavaObject JavaObject { get; }
        static readonly ProfilerMarker _marker = new("BleHid.BleBridgeCaller.Call");
        private readonly bool _verbose = false;

        public JavaBridge()
        {
            if (Application.platform != RuntimePlatform.Android) return;
            AndroidJavaClass bridgeClass = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityBridge");
            JavaObject = bridgeClass.CallStatic<AndroidJavaObject>("getInstance");
        }

        ~JavaBridge()
        {
            if (JavaObject == null) return;
            try { JavaObject.Call("close"); }
            catch (Exception e) { Debug.LogException(e); }
            JavaObject.Dispose();
        }
        public void Call(string methodName, params object[] args)
        {
            using var profilerMarker = _marker.Auto();
            Log($"JavaBridge.Call {methodName} {string.Join(", ", args)}");

            if (Application.platform != RuntimePlatform.Android) return;

            try { JavaObject?.Call(methodName, args); }
            catch (Exception e) { LoggingManager.Instance.AddLogException(e); }
        }

        public T Call<T>(string methodName, params object[] args)
        {
            using var profilerMarker = _marker.Auto();
            Log($"JavaBridge.Call {methodName} {string.Join(", ", args)}");

            if (Application.platform != RuntimePlatform.Android) return default;

            try
            {
                var result = JavaObject.Call<T>(methodName, args);
                Log($"JavaBridge.Call.Result: {result}");
                return result;
            }
            catch (Exception e) { LoggingManager.Instance.AddLogException(e); }
            return default;
        }

        private void Log(string msg)
        {
            if (_verbose) LoggingManager.Instance.Log(msg);
            else Debug.Log(msg);
        }
    }
}
