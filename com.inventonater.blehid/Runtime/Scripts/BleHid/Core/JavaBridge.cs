using System;
using Unity.Profiling;
using UnityEngine;
using UnityEngine.Profiling;

namespace Inventonater
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

        public void Call(string methodName, params object[] args) => Call<bool>(methodName, args);
        public T Call<T>(string methodName, params object[] args)
        {
            var msg = $"JavaBridge.Call {methodName} {string.Join(", ", args)}";
            Log(msg);
            Profiler.BeginSample(msg);
            if (Application.isEditor) return default;

            try { return JavaObject.Call<T>(methodName, args); }
            catch (Exception e) { LoggingManager.Instance.Exception(e); }
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
