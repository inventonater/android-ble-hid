using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Handles Android permission requests and checks for BLE HID functionality.
    /// Specifically manages Bluetooth permissions needed for Android 12+ devices 
    /// and other runtime permissions required by the plugin.
    /// </summary>
    public abstract class BleHidPermissionHandler
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
        public static readonly AndroidPermission[] Permissions =
        {
            new() { Name = "Bluetooth Connect", PermissionString = "android.permission.BLUETOOTH_CONNECT", Description = "Required to connect to and control Bluetooth devices" },
            new() { Name = "Bluetooth Scan", PermissionString = "android.permission.BLUETOOTH_SCAN", Description = "Required to scan for nearby Bluetooth devices" },
            new() { Name = "Bluetooth Advertise", PermissionString = "android.permission.BLUETOOTH_ADVERTISE", Description = "Required to advertise this device to other Bluetooth devices" },
            new() { Name = "Camera", PermissionString = "android.permission.CAMERA", Description = "Required for using the camera features" },
            new() { Name = "Notifications", PermissionString = "android.permission.POST_NOTIFICATIONS", Description = "Required for sending notifications" }
        };

        public static IEnumerable<AndroidPermission> GetMissingPermissions() => Permissions.Where(p => !HasUserAuthorizedPermission(p));
        public static bool CheckBluetoothPermissions() => GetMissingPermissions().Any();

        /// <summary>
        /// Request Bluetooth permissions required for Android 12+ (API level 31+)
        /// </summary>
        public static IEnumerator RequestPermissionsAndWait()
        {
            Debug.Log("Requesting Android Bluetooth permissions");
            foreach (AndroidPermission permission in Permissions)
            {
                yield return RequestAndroidPermissionAndWait(permission);
                yield return new WaitForSeconds(0.5f);
            }
        }

        public static void OpenAppSettings()
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
            catch (Exception e) { LoggingManager.Instance.AddLogEntry("Failed to open app settings: " + e.Message); }
        }

        public static IEnumerator RequestAndroidPermissionAndWait(AndroidPermission permission)
        {
            if (!HasUserAuthorizedPermission(permission))
            {
                Debug.Log($"Requesting permission: {permission}");
                RequestUserPermission(permission);

                // Wait briefly to allow the permission dialog to show and be handled
                yield return new WaitForSeconds(0.5f);
            }
        }

        public static bool HasUserAuthorizedPermission(AndroidPermission permission)
        {
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

        public static void RequestUserPermission(AndroidPermission permission)
        {
            if (Application.platform != RuntimePlatform.Android) return;

            try
            {
                var activity = new AndroidJavaClass("com.unity3d.player.UnityPlayer").GetStatic<AndroidJavaObject>("currentActivity");
                activity.Call("requestPermissions", new[] { permission.PermissionString }, 0);
            }
            catch (Exception e) { Debug.LogError($"Error requesting permission {permission.PermissionString}: {e.Message}"); }
        }
    }
}
