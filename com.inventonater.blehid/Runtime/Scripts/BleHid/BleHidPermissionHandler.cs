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
    public class BleHidPermissionHandler
    {
    /// <summary>
    /// Represents an Android permission
    /// </summary>
    public class AndroidPermission
    {
        public string Name { get; set; }
        public string PermissionString { get; set; }
        public bool IsGranted { get; set; }
        public string Description { get; set; }
    }
    
    /// <summary>
    /// Constant for Camera permission
    /// </summary>
    public const string CAMERA_PERMISSION = "android.permission.CAMERA";
    
    /// <summary>
    /// Constant for Notification permission (Android 13+)
    /// </summary>
    public const string NOTIFICATION_PERMISSION = "android.permission.POST_NOTIFICATIONS";
    
    /// <summary>
    /// List of Bluetooth permissions required for Android 12+
    /// </summary>
    public static readonly AndroidPermission[] BluetoothPermissions = new AndroidPermission[]
    {
        new AndroidPermission
        {
            Name = "Bluetooth Connect",
            PermissionString = "android.permission.BLUETOOTH_CONNECT",
            Description = "Required to connect to and control Bluetooth devices"
        },
        new AndroidPermission
        {
            Name = "Bluetooth Scan",
            PermissionString = "android.permission.BLUETOOTH_SCAN",
            Description = "Required to scan for nearby Bluetooth devices"
        },
        new AndroidPermission
        {
            Name = "Bluetooth Advertise",
            PermissionString = "android.permission.BLUETOOTH_ADVERTISE",
            Description = "Required to advertise this device to other Bluetooth devices"
        }
    };
    
    /// <summary>
    /// List of other permissions required by the app
    /// </summary>
    public static readonly AndroidPermission[] OtherPermissions = new AndroidPermission[]
    {
        new AndroidPermission
        {
            Name = "Camera",
            PermissionString = CAMERA_PERMISSION,
            Description = "Required for using the camera features"
        },
        new AndroidPermission
        {
            Name = "Notifications",
            PermissionString = NOTIFICATION_PERMISSION,
            Description = "Required for displaying notifications when the app is in the background"
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
        /// Get a list of all missing permissions (both Bluetooth and other)
        /// </summary>
        /// <returns>List of permissions that are not currently granted</returns>
        public static List<AndroidPermission> GetMissingPermissions()
        {
            List<AndroidPermission> missingPermissions = new List<AndroidPermission>();
            
            // Skip permission check if not on Android
            if (Application.platform != RuntimePlatform.Android)
                return missingPermissions;
            
            // Check Bluetooth permissions (only for Android 12+)
            int sdkInt = GetAndroidSDKVersion();
            if (sdkInt >= 31)
            {
                // Check each required Bluetooth permission
                foreach (var permission in BluetoothPermissions)
                {
                    permission.IsGranted = HasUserAuthorizedPermission(permission.PermissionString);
                    
                    if (!permission.IsGranted)
                    {
                        missingPermissions.Add(permission);
                    }
                }
            }
            
            // Check other permissions (like Camera)
            foreach (var permission in OtherPermissions)
            {
                permission.IsGranted = HasUserAuthorizedPermission(permission.PermissionString);
                
                if (!permission.IsGranted)
                {
                    // Don't add camera if it's already in the list
                    bool alreadyInList = missingPermissions.Any(p => p.PermissionString == permission.PermissionString);
                    if (!alreadyInList)
                    {
                        missingPermissions.Add(permission);
                    }
                }
            }
            
            return missingPermissions;
        }
        
        /// <summary>
        /// Get a list of all missing Bluetooth permissions
        /// </summary>
        /// <returns>List of permissions that are not currently granted</returns>
        public static List<AndroidPermission> GetMissingBluetoothPermissions()
        {
            List<AndroidPermission> missingPermissions = new List<AndroidPermission>();
            
            // Skip permission check if not on Android
            if (Application.platform != RuntimePlatform.Android)
                return missingPermissions;
                
            // Skip if Android version is below 12 (API 31)
            int sdkInt = GetAndroidSDKVersion();
            if (sdkInt < 31)
                return missingPermissions;
            
            // Check each required permission
            foreach (var permission in BluetoothPermissions)
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
        /// Request camera permission
        /// </summary>
        public static IEnumerator RequestCameraPermission()
        {
            Debug.Log("Requesting Camera permission");
            
            yield return RequestAndroidPermission(CAMERA_PERMISSION);
            
            // Give a small delay to allow the permission request to complete
            yield return new WaitForSeconds(0.5f);
        }
        
        /// <summary>
        /// Check if camera permission is granted
        /// </summary>
        public static bool CheckCameraPermission()
        {
            return Application.platform != RuntimePlatform.Android || HasUserAuthorizedPermission(CAMERA_PERMISSION);
        }
        
        /// <summary>
        /// Check if notification permission is granted (only relevant for Android 13+)
        /// </summary>
        public static bool CheckNotificationPermission()
        {
            // Skip for non-Android platforms
            if (Application.platform != RuntimePlatform.Android)
                return true;
                
            // Only needed for Android 13+ (API 33+)
            int sdkInt = GetAndroidSDKVersion();
            if (sdkInt < 33)
                return true;
                
            return HasUserAuthorizedPermission(NOTIFICATION_PERMISSION);
        }
        
        /// <summary>
        /// Request notification permission (for Android 13+)
        /// </summary>
        public static IEnumerator RequestNotificationPermission()
        {
            Debug.Log("Requesting Notification permission");
            
            // Skip for non-Android platforms
            if (Application.platform != RuntimePlatform.Android)
                yield break;
                
            // Only needed for Android 13+ (API 33+)
            int sdkInt = GetAndroidSDKVersion();
            if (sdkInt < 33)
                yield break;
            
            yield return RequestAndroidPermission(NOTIFICATION_PERMISSION);
            
            // Give a small delay to allow the permission request to complete
            yield return new WaitForSeconds(0.5f);
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
                return GetMissingBluetoothPermissions().Count == 0;
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
