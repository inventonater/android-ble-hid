using System;
using Unity.Profiling;
using UnityEngine;
using UnityEngine.Profiling;

namespace Inventonater.BleHid
{
    public class JavaBridge
    {
        private AndroidJavaObject JavaObject { get; }
        private readonly bool _verbose = false;

        public JavaBridge()
        {
            if (Application.isEditor) return;
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
            var msg = $"JavaBridge.Call {methodName} {string.Join(", ", args)}";
            Log(msg);
            Profiler.BeginSample(msg);
            if (Application.isEditor) return;

            try { JavaObject?.Call(methodName, args); }
            catch (Exception e) { LoggingManager.Instance.AddLogException(e); }
            finally { Profiler.EndSample(); }
        }

        public T Call<T>(string methodName, params object[] args)
        {
            var msg = $"JavaBridge.Call {methodName} {string.Join(", ", args)}";
            Log(msg);
            Profiler.BeginSample(msg);
            if (Application.isEditor) return default;

            try
            {
                var result = JavaObject.Call<T>(methodName, args);
                Log($"JavaBridge.Call.Result: {result}");
                return result;
            }
            catch (Exception e) { LoggingManager.Instance.AddLogException(e); }
            finally { Profiler.EndSample(); }

            return default;
        }

        private void Log(string msg)
        {
            if (_verbose) LoggingManager.Instance.Log(msg);
            else Debug.Log(msg);
        }
    }
}
