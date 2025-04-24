using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using Cysharp.Threading.Tasks;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Handles Android permission requests and checks for BLE HID functionality.
    /// Specifically manages Bluetooth permissions needed for Android 12+ devices 
    /// and other runtime permissions required by the plugin.
    /// </summary>
    public class PermissionsBridge
    {
        public class AndroidPermission
        {
            public string Name { get; set; }
            public string PermissionString { get; set; }
            public string Description { get; set; }
        }

        /// <summary>
        /// List of Bluetooth permissions required for Android 12+
        /// </summary>
        public readonly AndroidPermission[] Permissions =
        {
            new() { Name = "Bluetooth Connect", PermissionString = "android.permission.BLUETOOTH_CONNECT", Description = "Required to connect to and control Bluetooth devices" },
            new() { Name = "Bluetooth Scan", PermissionString = "android.permission.BLUETOOTH_SCAN", Description = "Required to scan for nearby Bluetooth devices" },
            new()
            {
                Name = "Bluetooth Advertise", PermissionString = "android.permission.BLUETOOTH_ADVERTISE",
                Description = "Required to advertise this device to other Bluetooth devices"
            },
            new() { Name = "Camera", PermissionString = "android.permission.CAMERA", Description = "Required for using the camera features" },
            new() { Name = "Notifications", PermissionString = "android.permission.POST_NOTIFICATIONS", Description = "Required for sending notifications" }
        };

        private bool _isInitialized;

        public IEnumerable<AndroidPermission> MissingPermissions => Permissions.Where(p => !HasUserAuthorizedPermission(p));
        public bool IsInitialized => _isInitialized;

        /// <summary>
        /// Request Bluetooth permissions required for Android 12+ (API level 31+)
        /// </summary>
        public async UniTask Initialize()
        {
            if (!MissingPermissions.Any())
            {
                LoggingManager.Instance.Log("No missing permissions.");
                _isInitialized = true;
                return;
            }

            try
            {
                var missingPermissions = MissingPermissions;
                foreach (var permission in missingPermissions)
                {
                    LoggingManager.Instance.Log($"RequestMissingPermissions: {permission.PermissionString}");

                    var activity = new AndroidJavaClass("com.unity3d.player.UnityPlayer").GetStatic<AndroidJavaObject>("currentActivity");
                    activity.Call("requestPermissions", new[] { permission.PermissionString }, 0);
                    while (!HasUserAuthorizedPermission(permission))
                    {
                        await UniTask.Delay(500);
                        LoggingManager.Instance.Log($"Waiting for permissions...");
                    }
                }
            }
            catch (Exception e)
            {
                var missingPermissions = MissingPermissions.Select(p => p.PermissionString);
                LoggingManager.Instance.Error($"Error requesting permissions {string.Join(", ", missingPermissions)}: {e.Message}");
                return;
            }

            _isInitialized = true;
            LoggingManager.Instance.Log($"RequestPermission finished");
        }

        public void OpenAppSettings()
        {
            try
            {
                AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
                AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
                AndroidJavaObject context = currentActivity.Call<AndroidJavaObject>("getApplicationContext");

                AndroidJavaClass intentClass = new AndroidJavaClass("android.content.Intent");
                AndroidJavaObject intent = new AndroidJavaObject(
                    "android.content.Intent",
                    "android.settings.APPLICATION_DETAILS_SETTINGS");

                AndroidJavaClass uriClass = new AndroidJavaClass("android.net.Uri");
                AndroidJavaObject uriBuilder = new AndroidJavaObject("java.lang.StringBuilder");
                uriBuilder.Call<AndroidJavaObject>("append", "package:");
                uriBuilder.Call<AndroidJavaObject>("append", context.Call<string>("getPackageName"));
                AndroidJavaObject uri = uriClass.CallStatic<AndroidJavaObject>("parse", uriBuilder.Call<string>("toString"));

                intent.Call<AndroidJavaObject>("setData", uri);
                intent.Call<AndroidJavaObject>("addFlags", intentClass.GetStatic<int>("FLAG_ACTIVITY_NEW_TASK"));

                currentActivity.Call("startActivity", intent);
            }
            catch (Exception e) { LoggingManager.Instance.Log("Failed to open app settings: " + e.Message); }
        }

        public bool HasUserAuthorizedPermission(AndroidPermission permission)
        {
            if (Application.platform != RuntimePlatform.Android) return true;

            try
            {
                var activity = new AndroidJavaClass("com.unity3d.player.UnityPlayer").GetStatic<AndroidJavaObject>("currentActivity");
                const int GRANTED = 0;
                int result = activity.Call<int>("checkSelfPermission", permission.PermissionString);
                return result == GRANTED;
            }
            catch (Exception e)
            {
                Debug.LogError($"Error checking permission {permission.PermissionString}: {e.Message}");
                return false;
            }
        }
    }
}
