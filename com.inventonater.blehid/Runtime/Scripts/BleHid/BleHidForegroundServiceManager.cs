using System;
using System.Collections;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Manages the foreground service for BLE HID functionality.
    /// Handles starting, stopping, and permissions related to the service.
    /// </summary>
    public class BleHidForegroundServiceManager
    {
        private LoggingManager Logger => LoggingManager.Instance;

        /// <summary>
        /// Event that is triggered when there's an error with the foreground service
        /// </summary>
        public event BleHidCallbackHandler.ErrorHandler OnError;
        
        /// <summary>
        /// Reference to the bridge instance for native method calls
        /// </summary>
        private AndroidJavaObject bridgeInstance;
        
        /// <summary>
        /// The Unity MonoBehaviour that owns this service (for coroutines)
        /// </summary>
        private MonoBehaviour ownerBehaviour;
        
        public BleHidForegroundServiceManager(AndroidJavaObject bridgeInstance, MonoBehaviour owner)
        {
            this.bridgeInstance = bridgeInstance;
            this.ownerBehaviour = owner;
        }
        
        /// <summary>
        /// Start the foreground service to keep accessibility service alive.
        /// This should be called when your app needs to ensure the service
        /// continues to run in the background. On Android 13+, this will check
        /// for notification permission which is required for foreground services.
        /// </summary>
        /// <returns>True if the service start request was sent successfully.</returns>
        public bool StartForegroundService()
        {
            if (Application.platform != RuntimePlatform.Android)
            {
                Debug.LogWarning("Foreground service is only available on Android.");
                return false;
            }
            
            // Check if we need notification permission (Android 13+ / API 33+)
            int sdkInt = BleHidPermissionHandler.GetAndroidSDKVersion();
            if (sdkInt >= 33 && !BleHidPermissionHandler.CheckNotificationPermission())
            {
                Debug.Log("Notification permission required for foreground service on Android 13+");
                LoggingManager.Instance.AddLogEntry("Notification permission needed for foreground service");
                
                // Find a MonoBehaviour to run the coroutine
                if (ownerBehaviour != null)
                {
                    Debug.Log("Requesting notification permission");
                    LoggingManager.Instance.AddLogEntry("Requesting notification permission");
                    ownerBehaviour.StartCoroutine(RequestNotificationAndStartService());
                    return true; // We're attempting to start the service after getting permission
                }
                else
                {
                    Debug.LogError("Failed to find MonoBehaviour to request notification permission");
                    LoggingManager.Instance.AddLogEntry("ERROR: Failed to find MonoBehaviour to request notification permission");
                }
            }

            // If no permission needed or already granted, start the service directly
            return StartForegroundServiceImpl();
        }
        
        /// <summary>
        /// Request notification permission and then start the service
        /// </summary>
        private IEnumerator RequestNotificationAndStartService()
        {
            yield return BleHidPermissionHandler.RequestNotificationPermission();
            
            // Check if permission was granted
            if (BleHidPermissionHandler.CheckNotificationPermission())
            {
                Debug.Log("Notification permission granted, starting foreground service");
                LoggingManager.Instance.AddLogEntry("Notification permission granted, starting foreground service");
                StartForegroundServiceImpl();
            }
            else
            {
                Debug.LogWarning("Notification permission denied, foreground service may not show notifications");
                LoggingManager.Instance.AddLogEntry("WARNING: Notification permission denied, foreground service may not show notifications");
                
                // Try to start service anyway, but it might not show a notification
                StartForegroundServiceImpl();
            }
        }
        
        /// <summary>
        /// Implementation of the actual foreground service start
        /// </summary>
        private bool StartForegroundServiceImpl()
        {
            try
            {
                using (AndroidJavaClass pluginClass = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityPlugin"))
                {
                    bool result = pluginClass.CallStatic<bool>("startForegroundService");
                    if (result) { 
                        Debug.Log("Foreground service start requested successfully");
                        LoggingManager.Instance.AddLogEntry("Foreground service start requested successfully");
                    }
                    else { 
                        Debug.LogError("Failed to start foreground service");
                        LoggingManager.Instance.AddLogEntry("ERROR: Failed to start foreground service");
                    }

                    return result;
                }
            }
            catch (Exception e)
            {
                string message = "Exception starting foreground service: " + e.Message;
                Debug.LogError(message);
                Debug.LogException(e);
                LoggingManager.Instance.AddLogEntry("ERROR: " + message);
                OnError?.Invoke(BleHidConstants.ERROR_GENERAL_ERROR, message);
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
                OnError?.Invoke(BleHidConstants.ERROR_GENERAL_ERROR, message);
                return false;
            }
        }
    }
}
