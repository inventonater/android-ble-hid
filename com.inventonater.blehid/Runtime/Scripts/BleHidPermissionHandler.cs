using System;
using System.Collections;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Handles Android permission requests and checks for BLE HID functionality.
    /// Manages Bluetooth permissions needed for Android 12+ devices and Accessibility permissions.
    /// </summary>
    public class BleHidPermissionHandler
    {
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
                bool hasConnectPermission = HasUserAuthorizedPermission("android.permission.BLUETOOTH_CONNECT");
                bool hasScanPermission = HasUserAuthorizedPermission("android.permission.BLUETOOTH_SCAN");
                bool hasAdvertisePermission = HasUserAuthorizedPermission("android.permission.BLUETOOTH_ADVERTISE");
                
                Debug.Log($"Permissions: BLUETOOTH_CONNECT={hasConnectPermission}, BLUETOOTH_SCAN={hasScanPermission}, BLUETOOTH_ADVERTISE={hasAdvertisePermission}");
                
                return hasConnectPermission && hasScanPermission && hasAdvertisePermission;
            }
            
            // For older Android versions we check the legacy permissions
            return true; // These should be granted at install time pre-Android 12
        }
        
        /// <summary>
        /// Check if the Accessibility Service is enabled for this app
        /// </summary>
        public static bool IsAccessibilityServiceEnabled()
        {
            if (Application.platform != RuntimePlatform.Android)
                return true;
                
            try
            {
                // Use BleHidLocalControl to check accessibility status
                BleHidLocalControl localControl = BleHidLocalControl.Instance;
                return localControl.IsAccessibilityServiceEnabled();
            }
            catch (Exception e)
            {
                Debug.LogError("Failed to check accessibility service: " + e.Message);
                return false;
            }
        }
        
        /// <summary>
        /// Open the Android Accessibility Settings screen
        /// </summary>
        public static void OpenAccessibilitySettings()
        {
            if (Application.platform != RuntimePlatform.Android)
                return;
                
            try
            {
                // Use BleHidLocalControl to open accessibility settings
                BleHidLocalControl localControl = BleHidLocalControl.Instance;
                localControl.OpenAccessibilitySettings();
                Debug.Log("Opening Accessibility Settings...");
            }
            catch (Exception e)
            {
                Debug.LogError("Failed to open accessibility settings: " + e.Message);
            }
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
