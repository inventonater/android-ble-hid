using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Manages foreground service functionality for BLE HID.
    /// </summary>
    public class ServiceManager
    {
        private BleHidManager manager;

        public ServiceManager(BleHidManager manager)
        {
            this.manager = manager;
        }

        /// <summary>
        /// Start the foreground service to keep accessibility service alive.
        /// This should be called when your app needs to ensure the service
        /// continues to run in the background.
        /// </summary>
        /// <returns>True if the service start request was sent successfully.</returns>
        public bool StartForegroundService()
        {
            if (Application.platform != RuntimePlatform.Android)
            {
                Debug.LogWarning("Foreground service is only available on Android.");
                return false;
            }

            try
            {
                using (AndroidJavaClass pluginClass = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityPlugin"))
                {
                    bool result = pluginClass.CallStatic<bool>("startForegroundService");
                    if (result) { Debug.Log("Foreground service start requested successfully"); }
                    else { Debug.LogError("Failed to start foreground service"); }

                    return result;
                }
            }
            catch (Exception e)
            {
                string message = "Exception starting foreground service: " + e.Message;
                Debug.LogError(message);
                Debug.LogException(e);
                manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_GENERAL_ERROR, message);
                return false;
            }
        }

        /// <summary>
        /// Stop the foreground service when it's no longer needed.
        /// </summary>
        /// <returns>True if the service stop request was sent successfully.</returns>
        public bool StopForegroundService()
        {
            if (Application.platform != RuntimePlatform.Android)
            {
                Debug.LogWarning("Foreground service is only available on Android.");
                return false;
            }

            try
            {
                using (AndroidJavaClass pluginClass = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityPlugin"))
                {
                    bool result = pluginClass.CallStatic<bool>("stopForegroundService");
                    if (result) { Debug.Log("Foreground service stop requested successfully"); }
                    else { Debug.LogError("Failed to stop foreground service"); }

                    return result;
                }
            }
            catch (Exception e)
            {
                string message = "Exception stopping foreground service: " + e.Message;
                Debug.LogError(message);
                Debug.LogException(e);
                manager.BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_GENERAL_ERROR, message);
                return false;
            }
        }
    }
}
