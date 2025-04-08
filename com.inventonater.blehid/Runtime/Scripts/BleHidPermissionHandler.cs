using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Handles Android permission requests and checks for BLE HID functionality.
    /// Specifically manages Bluetooth permissions needed for Android 12+ devices.
    /// </summary>
    public class BleHidPermissionHandler
    {
        /// <summary>
        /// Represents a Bluetooth permission
        /// </summary>
        public class BluetoothPermission
        {
            public string Name { get; set; }
            public string PermissionString { get; set; }
            public bool IsGranted { get; set; }
            public string Description { get; set; }
        }
        
        /// <summary>
        /// List of Bluetooth permissions required for Android 12+
        /// </summary>
        public static readonly BluetoothPermission[] RequiredPermissions = new BluetoothPermission[]
        {
            new BluetoothPermission
            {
                Name = "Bluetooth Connect",
                PermissionString = "android.permission.BLUETOOTH_CONNECT",
                Description = "Required to connect to and control Bluetooth devices"
            },
            new BluetoothPermission
            {
                Name = "Bluetooth Scan",
                PermissionString = "android.permission.BLUETOOTH_SCAN",
                Description = "Required to scan for nearby Bluetooth devices"
            },
            new BluetoothPermission
            {
                Name = "Bluetooth Advertise",
                PermissionString = "android.permission.BLUETOOTH_ADVERTISE",
                Description = "Required to advertise this device to other Bluetooth devices"
            }
        };
        /// <summary>
        /// Request Bluetooth permissions required for Android 12+ (API level 31+)
        /// </summary>
        public static IEnumerator RequestBluetoothPermissions()
        {
            Debug.Log("Requesting Android 12+ Bluetooth permissions");
            
            // Request BLUETOOTH_CONNECT permission
            yield return RequestAndroidPermission("android.permission.BLUETOOTH_CONNECT");
            
            // Request BLUETOOTH_SCAN permission
            yield return RequestAndroidPermission("android.permission.BLUETOOTH_SCAN");
            
            // Request BLUETOOTH_ADVERTISE permission
            yield return RequestAndroidPermission("android.permission.BLUETOOTH_ADVERTISE");
            
            // Give a small delay to allow the permission requests to complete
            yield return new WaitForSeconds(0.5f);
        }
        
        /// <summary>
        /// Get a list of all missing Bluetooth permissions
        /// </summary>
        /// <returns>List of permissions that are not currently granted</returns>
        public static List<BluetoothPermission> GetMissingPermissions()
        {
            List<BluetoothPermission> missingPermissions = new List<BluetoothPermission>();
            
            // Skip permission check if not on Android
            if (Application.platform != RuntimePlatform.Android)
                return missingPermissions;
                
            // Skip if Android version is below 12 (API 31)
            int sdkInt = GetAndroidSDKVersion();
            if (sdkInt < 31)
                return missingPermissions;
            
            // Check each required permission
            foreach (var permission in RequiredPermissions)
            {
                permission.IsGranted = HasUserAuthorizedPermission(permission.PermissionString);
                
                if (!permission.IsGranted)
                {
                    missingPermissions.Add(permission);
                }
            }
            
            return missingPermissions;
        }
        
        /// <summary>
        /// Open the Android app settings page for this application
        /// </summary>
        public static void OpenAppSettings()
        {
            if (Application.platform != RuntimePlatform.Android)
                return;
                
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
            catch (Exception e)
            {
                Debug.LogError("Failed to open app settings: " + e.Message);
            }
        }
        
        /// <summary>
        /// Request a specific Android permission using Unity's Permission API
        /// </summary>
        public static IEnumerator RequestAndroidPermission(string permission)
        {
            if (Application.platform != RuntimePlatform.Android)
                yield break;
                
            Debug.Log($"Requesting permission: {permission}");
            
            // Use Unity's permission system to check/request
            if (!HasUserAuthorizedPermission(permission))
            {
                Debug.Log($"Requesting permission: {permission}");
                RequestUserPermission(permission);
                
                // Wait briefly to allow the permission dialog to show and be handled
                yield return new WaitForSeconds(0.5f);
            }
            else
            {
                Debug.Log($"Permission already granted: {permission}");
            }
        }
        
        /// <summary>
        /// Check if the user has authorized the specified permission
        /// </summary>
        public static bool HasUserAuthorizedPermission(string permission)
        {
            if (Application.platform != RuntimePlatform.Android)
                return true;
                
            AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            AndroidJavaClass compatClass = new AndroidJavaClass("androidx.core.content.ContextCompat");
            AndroidJavaClass permissionClass = new AndroidJavaClass("android.content.pm.PackageManager");
            int granted = permissionClass.GetStatic<int>("PERMISSION_GRANTED");
            
            int result = compatClass.CallStatic<int>("checkSelfPermission", currentActivity, permission);
            return result == granted;
        }
        
        /// <summary>
        /// Request the specified permission from the user
        /// </summary>
        public static void RequestUserPermission(string permission)
        {
            if (Application.platform != RuntimePlatform.Android)
                return;
                
            try
            {
                AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
                AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
                AndroidJavaClass compatClass = new AndroidJavaClass("androidx.core.app.ActivityCompat");
                
                // Request permission - this will show the permission dialog
                compatClass.CallStatic("requestPermissions", currentActivity, new string[] { permission }, 0);
            }
            catch (Exception e)
            {
                Debug.LogError($"Error requesting permission {permission}: {e.Message}");
            }
        }
        
        /// <summary>
        /// Check if all required Bluetooth permissions are granted
        /// </summary>
        public static bool CheckBluetoothPermissions()
        {
            if (Application.platform != RuntimePlatform.Android)
                return true;
                
            AndroidJavaClass versionClass = new AndroidJavaClass("android.os.Build$VERSION");
            int sdkInt = versionClass.GetStatic<int>("SDK_INT");
                
            // For Android 12+ (API 31+) we need these permissions
            if (sdkInt >= 31)
            {
                return GetMissingPermissions().Count == 0;
            }
            
            // For older Android versions we check the legacy permissions
            return true; // These should be granted at install time pre-Android 12
        }
        
        /// <summary>
        /// Get the Android SDK version
        /// </summary>
        /// <returns>SDK version number, or -1 if not on Android</returns>
        public static int GetAndroidSDKVersion()
        {
            if (Application.platform != RuntimePlatform.Android)
                return -1;
                
            try
            {
                AndroidJavaClass versionClass = new AndroidJavaClass("android.os.Build$VERSION");
                return versionClass.GetStatic<int>("SDK_INT");
            }
            catch (Exception e)
            {
                Debug.LogError("Failed to get Android SDK version: " + e.Message);
                return -1;
            }
        }
    }
}
