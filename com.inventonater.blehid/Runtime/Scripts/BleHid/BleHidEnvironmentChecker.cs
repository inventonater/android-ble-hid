using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Handles environment checks for BLE HID functionality.
    /// Responsible for validating the runtime environment, hardware capabilities,
    /// and plugin availability.
    /// </summary>
    public class BleHidEnvironmentChecker
    {
        /// <summary>
        /// Verifies that plugins with the necessary functionality are loaded.
        /// Only checks the inventonater namespace.
        /// </summary>
        public static bool VerifyPluginsLoaded(out string errorMsg)
        {
            errorMsg = "";
            
            if (Application.platform != RuntimePlatform.Android)
            {
                errorMsg = "BLE HID is only supported on Android";
                return false;
            }
            
            try
            {
                // Only use the new namespace
                AndroidJavaClass test = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityBridge");
                Debug.Log("Found plugin with namespace com.inventonater.blehid.unity");
                return true;
            }
            catch (Exception ex)
            {
                errorMsg = "BLE HID plugin not found in com.inventonater.blehid.unity namespace";
                Debug.LogError(errorMsg + ": " + ex.Message);
                return false;
            }
        }
        
        /// <summary>
        /// Verify that the bridge interface is valid and responsive.
        /// </summary>
        public static bool VerifyBridgeInterface(AndroidJavaObject bridge, out string errorMsg)
        {
            errorMsg = "";
            
            if (bridge == null)
            {
                errorMsg = "Bridge instance is null";
                return false;
            }
            
            try
            {
                // Call a method that shouldn't have side effects
                string result = bridge.Call<string>("toString");
                Debug.Log("Bridge interface verified: " + result);
                return true;
            }
            catch (Exception e)
            {
                errorMsg = "Bridge interface verification failed: " + e.Message;
                Debug.LogError(errorMsg);
                return false;
            }
        }
        
        /// <summary>
        /// Check if Bluetooth is enabled on the device.
        /// </summary>
        public static bool IsBluetoothEnabled(out string errorMsg)
        {
            errorMsg = "";
            
            if (Application.platform != RuntimePlatform.Android)
            {
                errorMsg = "Bluetooth check only works on Android";
                return false;
            }
            
            try
            {
                AndroidJavaClass bluetoothAdapter = new AndroidJavaClass("android.bluetooth.BluetoothAdapter");
                AndroidJavaObject defaultAdapter = bluetoothAdapter.CallStatic<AndroidJavaObject>("getDefaultAdapter");
                
                if (defaultAdapter == null)
                {
                    errorMsg = "Bluetooth not supported on this device";
                    return false;
                }
                
                bool isEnabled = defaultAdapter.Call<bool>("isEnabled");
                if (!isEnabled)
                {
                    errorMsg = "Bluetooth is turned off";
                }
                return isEnabled;
            }
            catch (Exception e)
            {
                errorMsg = "Failed to check Bluetooth state: " + e.Message;
                Debug.LogError(errorMsg);
                return false;
            }
        }
        
        /// <summary>
        /// Check if the device supports BLE peripheral/advertising functionality.
        /// Not all Android devices can act as a BLE peripheral.
        /// </summary>
        public static bool SupportsBleAdvertising(out string errorMsg)
        {
            errorMsg = "";
            
            if (Application.platform != RuntimePlatform.Android)
            {
                errorMsg = "BLE advertising check only works on Android";
                return false;
            }
            
            try
            {
                AndroidJavaClass bluetoothAdapter = new AndroidJavaClass("android.bluetooth.BluetoothAdapter");
                AndroidJavaObject defaultAdapter = bluetoothAdapter.CallStatic<AndroidJavaObject>("getDefaultAdapter");
                
                if (defaultAdapter == null)
                {
                    errorMsg = "Bluetooth not supported on this device";
                    return false;
                }
                
                // On some devices/Android versions this method might not exist, so we handle that case
                try 
                {
                    bool isSupported = defaultAdapter.Call<bool>("isMultipleAdvertisementSupported");
                    if (!isSupported)
                    {
                        errorMsg = "This device does not support BLE advertising";
                    }
                    return isSupported;
                }
                catch (Exception innerEx)
                {
                    // If the method doesn't exist, we can't be sure - use a different approach
                    Debug.LogWarning("Could not check BLE advertising support directly: " + innerEx.Message);
                    
                    // Check Android version as a fallback - M (23) and above generally support it
                    AndroidJavaClass buildVersion = new AndroidJavaClass("android.os.Build$VERSION");
                    int sdkInt = buildVersion.GetStatic<int>("SDK_INT");
                    
                    if (sdkInt < 23)
                    {
                        errorMsg = "BLE advertising likely not supported on Android " + sdkInt;
                        return false;
                    }
                    
                    // Can't be certain, but newer devices typically support it
                    Debug.Log("Could not definitively check BLE advertising support, but likely supported on Android " + sdkInt);
                    return true;
                }
            }
            catch (Exception e)
            {
                errorMsg = "Failed to check BLE advertising support: " + e.Message;
                Debug.LogError(errorMsg);
                return false;
            }
        }
        
        /// <summary>
        /// Check if the accessibility service is enabled.
        /// </summary>
        /// <param name="errorMsg">Output error message if check fails</param>
        /// <returns>True if the accessibility service is enabled, false otherwise</returns>
        public static bool CheckAccessibilityServiceEnabled(out string errorMsg)
        {
            errorMsg = "";
            
            if (Application.platform != RuntimePlatform.Android)
            {
                errorMsg = "Accessibility check only works on Android";
                return false;
            }
            
            try
            {
                // Try to get the local control instance
                BleHidLocalControl localControl = BleHidLocalControl.Instance;
                
                if (localControl == null)
                {
                    errorMsg = "BleHidLocalControl instance not available";
                    return false;
                }
                
                // Check if accessibility service is enabled
                bool isEnabled = localControl.IsAccessibilityServiceEnabled();
                
                if (!isEnabled)
                {
                    errorMsg = "Accessibility service is not enabled";
                }
                
                return isEnabled;
            }
            catch (Exception e)
            {
                errorMsg = "Failed to check accessibility service status: " + e.Message;
                Debug.LogError(errorMsg);
                return false;
            }
        }
        
        /// <summary>
        /// Run diagnostic checks and return a comprehensive report of the system state.
        /// </summary>
        /// <returns>A string containing the diagnostic information.</returns>
        public static string RunEnvironmentDiagnostics(BleHidManager manager)
        {
            System.Text.StringBuilder report = new System.Text.StringBuilder();
            
            report.AppendLine("===== BLE HID Environment Diagnostics =====");
            report.AppendLine("Date/Time: " + System.DateTime.Now.ToString());
            report.AppendLine("Platform: " + Application.platform);
            report.AppendLine("Unity Version: " + Application.unityVersion);
            
            if (Application.platform != RuntimePlatform.Android)
            {
                report.AppendLine("STATUS: UNSUPPORTED PLATFORM - Android required");
                return report.ToString();
            }
            
            // Android version check
            try
            {
                AndroidJavaClass versionClass = new AndroidJavaClass("android.os.Build$VERSION");
                int sdkInt = versionClass.GetStatic<int>("SDK_INT");
                string release = versionClass.GetStatic<string>("RELEASE");
                report.AppendLine($"Android Version: {release} (API {sdkInt})");
            }
            catch (Exception e)
            {
                report.AppendLine("Failed to get Android version: " + e.Message);
            }
            
            // Plugin load check
            string errorMsg;
            bool pluginsLoaded = VerifyPluginsLoaded(out errorMsg);
            report.AppendLine("Plugins Loaded: " + (pluginsLoaded ? "YES" : "NO"));
            if (!pluginsLoaded)
            {
                report.AppendLine("Plugin Error: " + errorMsg);
            }
            
            // Bluetooth enabled check
            bool bluetoothEnabled = IsBluetoothEnabled(out errorMsg);
            report.AppendLine("Bluetooth Enabled: " + (bluetoothEnabled ? "YES" : "NO"));
            if (!bluetoothEnabled)
            {
                report.AppendLine("Bluetooth Error: " + errorMsg);
            }
            
            // BLE advertising support check
            bool advertisingSupported = SupportsBleAdvertising(out errorMsg);
            report.AppendLine("BLE Advertising Support: " + (advertisingSupported ? "YES" : "NO"));
            if (!advertisingSupported)
            {
                report.AppendLine("Advertising Error: " + errorMsg);
            }
            
            // Permissions check
            if (Application.platform == RuntimePlatform.Android)
            {
                AndroidJavaClass versionClass = new AndroidJavaClass("android.os.Build$VERSION");
                int sdkInt = versionClass.GetStatic<int>("SDK_INT");
                
                if (sdkInt >= 31) // Android 12+
                {
                    bool hasConnectPermission = BleHidPermissionHandler.HasUserAuthorizedPermission("android.permission.BLUETOOTH_CONNECT");
                    bool hasScanPermission = BleHidPermissionHandler.HasUserAuthorizedPermission("android.permission.BLUETOOTH_SCAN");
                    bool hasAdvertisePermission = BleHidPermissionHandler.HasUserAuthorizedPermission("android.permission.BLUETOOTH_ADVERTISE");
                    
                    report.AppendLine("Permission BLUETOOTH_CONNECT: " + (hasConnectPermission ? "GRANTED" : "DENIED"));
                    report.AppendLine("Permission BLUETOOTH_SCAN: " + (hasScanPermission ? "GRANTED" : "DENIED"));
                    report.AppendLine("Permission BLUETOOTH_ADVERTISE: " + (hasAdvertisePermission ? "GRANTED" : "DENIED"));
                }
                else // Older Android
                {
                    report.AppendLine("Permissions: Not applicable for Android API " + sdkInt);
                }
            }
            
            // Accessibility service check
            bool accessibilityEnabled = CheckAccessibilityServiceEnabled(out errorMsg);
            report.AppendLine("Accessibility Service Enabled: " + (accessibilityEnabled ? "YES" : "NO"));
            if (!accessibilityEnabled)
            {
                report.AppendLine("Accessibility Error: " + errorMsg);
            }
            
            // Bridge instance check
            if (manager.BleInitializer.BridgeInstance != null)
            {
                report.AppendLine("Bridge Instance: PRESENT");
                
                bool bridgeValid = VerifyBridgeInterface(manager.BleInitializer.BridgeInstance, out errorMsg);
                report.AppendLine("Bridge Interface Valid: " + (bridgeValid ? "YES" : "NO"));
                if (!bridgeValid)
                {
                    report.AppendLine("Bridge Error: " + errorMsg);
                }
                
                report.AppendLine("Initialized: " + manager.IsInitialized);
                report.AppendLine("Advertising: " + manager.IsAdvertising);
                report.AppendLine("Connected: " + manager.IsConnected);
                
                if (manager.IsConnected)
                {
                    report.AppendLine("Connected Device: " + manager.ConnectedDeviceName + " (" + manager.ConnectedDeviceAddress + ")");
                }
            }
            else
            {
                report.AppendLine("Bridge Instance: NOT PRESENT");
            }
            
            report.AppendLine("Last Error Code: " + manager.LastErrorCode);
            report.AppendLine("Last Error Message: " + manager.LastErrorMessage);
            
            report.AppendLine("===== End of Diagnostics =====");
            
            return report.ToString();
        }
    }
}
