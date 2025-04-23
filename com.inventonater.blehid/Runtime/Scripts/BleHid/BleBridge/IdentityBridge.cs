using System;
using System.Collections.Generic;
using UnityEngine;
using System.Runtime.InteropServices;

namespace Inventonater.BleHid
{
    [Serializable]
    public class IdentityBridge
    {
        private BleHidManager _manager;
        
        public IdentityBridge(BleHidManager manager)
        {
            _manager = manager;
        }
        
        /// <summary>
        /// Sets the BLE peripheral identity (UUID and device name) for consistent recognition across app restarts.
        /// </summary>
        /// <param name="identityUuid">The UUID string to use as the device's unique identifier</param>
        /// <param name="deviceName">Optional custom device name</param>
        /// <returns>True if identity was set successfully</returns>
        public bool SetBleIdentity(string identityUuid, string deviceName)
        {
            if (Application.isEditor)
            {
                Debug.Log($"[EDITOR] SetBleIdentity bond for identityUuid: {identityUuid}, deviceName: {deviceName}");
                return true;
            }

            if (!_manager.ConfirmIsInitialized()) return false;
            
            using (AndroidJavaClass unityBridge = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityBridge"))
            using (AndroidJavaObject bridgeInstance = unityBridge.CallStatic<AndroidJavaObject>("getInstance"))
            {
                return bridgeInstance.Call<bool>("setBleIdentity", identityUuid, deviceName);
            }
        }
        
        /// <summary>
        /// Gets detailed information about all devices currently bonded to this peripheral.
        /// </summary>
        /// <returns>List of dictionaries containing device information</returns>
        public List<Dictionary<string, string>> GetBondedDevicesInfo()
        {
            if (Application.isEditor) return MockBondedDevicesInfo();

            if (!_manager.ConfirmIsInitialized()) return new List<Dictionary<string, string>>();

            using (AndroidJavaClass unityBridge = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityBridge"))
            using (AndroidJavaObject bridgeInstance = unityBridge.CallStatic<AndroidJavaObject>("getInstance"))
            {
                AndroidJavaObject javaList = bridgeInstance.Call<AndroidJavaObject>("getBondedDevicesInfo");
                return ConvertJavaListToDeviceInfoList(javaList);
            }
        }
        
        /// <summary>
        /// Checks if a specific device is bonded to this peripheral.
        /// </summary>
        /// <param name="address">MAC address of the device to check</param>
        /// <returns>True if the device is bonded</returns>
        public bool IsDeviceBonded(string address)
        {
            if (Application.isEditor)
            {
                Debug.Log($"[EDITOR] IsDeviceBonded bond for device {address}");
                return true;
            }

            if (!_manager.ConfirmIsInitialized()) return false;

            using (AndroidJavaClass unityBridge = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityBridge"))
            using (AndroidJavaObject bridgeInstance = unityBridge.CallStatic<AndroidJavaObject>("getInstance"))
            {
                return bridgeInstance.Call<bool>("isDeviceBonded", address);
            }
        }
        
        /// <summary>
        /// Removes a bond with a specific device (forget pairing).
        /// </summary>
        /// <param name="address">MAC address of the device to forget</param>
        /// <returns>True if the bond was successfully removed</returns>
        public bool RemoveBond(string address)
        {
            if (Application.isEditor)
            {
                Debug.Log($"[EDITOR] Removing bond for device {address}");
                return true;
            }
            if (!_manager.ConfirmIsInitialized()) return false;
            
            using (AndroidJavaClass unityBridge = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityBridge"))
            using (AndroidJavaObject bridgeInstance = unityBridge.CallStatic<AndroidJavaObject>("getInstance"))
            {
                return bridgeInstance.Call<bool>("removeBond", address);
            }
        }
        
        /// <summary>
        /// Initiates pairing (bonding) with a remote device.
        /// </summary>
        /// <param name="address">MAC address of the device to pair with.</param>
        /// <returns>True if the bond request was successfully initiated.</returns>
        public bool PairDevice(string address)
        {
            if (Application.isEditor) return true;
            if (!_manager.ConfirmIsInitialized()) return false;
            using (AndroidJavaClass unityBridge = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityBridge"))
            using (AndroidJavaObject bridgeInstance = unityBridge.CallStatic<AndroidJavaObject>("getInstance"))
            {
                return bridgeInstance.Call<bool>("pairDevice", address);
            }
        }

        /// <summary>
        /// Gets the raw bond state of a remote device.
        /// </summary>
        /// <param name="address">MAC address of the device to query.</param>
        /// <returns>One of BluetoothDevice.BOND_NONE, BOND_BONDING, or BOND_BONDED.</returns>
        public int GetBondState(string address)
        {
            if (Application.isEditor) return 12; // BOND_BONDED
            if (!_manager.ConfirmIsInitialized()) return 10; // BOND_NONE
            using (AndroidJavaClass unityBridge = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityBridge"))
            using (AndroidJavaObject bridgeInstance = unityBridge.CallStatic<AndroidJavaObject>("getInstance"))
            {
                return bridgeInstance.Call<int>("getBondState", address);
            }
        }
        
        // Helper method to convert Java List<Map<String, String>> to C# List<Dictionary<string, string>>
        private List<Dictionary<string, string>> ConvertJavaListToDeviceInfoList(AndroidJavaObject javaList)
        {
            List<Dictionary<string, string>> result = new List<Dictionary<string, string>>();
            
            if (javaList == null)
                return result;
                
            int size = javaList.Call<int>("size");
            for (int i = 0; i < size; i++)
            {
                AndroidJavaObject javaMap = javaList.Call<AndroidJavaObject>("get", i);
                Dictionary<string, string> deviceInfo = new Dictionary<string, string>();
                
                // Get the key set
                AndroidJavaObject keySet = javaMap.Call<AndroidJavaObject>("keySet");
                AndroidJavaObject iterator = keySet.Call<AndroidJavaObject>("iterator");
                
                while (iterator.Call<bool>("hasNext"))
                {
                    string key = iterator.Call<string>("next");
                    string value = javaMap.Call<string>("get", key);
                    deviceInfo[key] = value;
                }
                
                result.Add(deviceInfo);
            }
            
            return result;
        }

        private static List<Dictionary<string, string>> MockBondedDevicesInfo()
        {
            // Return mock data for testing in the editor
            List<Dictionary<string, string>> mockDevices = new List<Dictionary<string, string>>();

            Dictionary<string, string> device1 = new Dictionary<string, string>
            {
                { "name", "Mock PC" },
                { "address", "00:11:22:33:44:55" },
                { "type", "LE" },
                { "bondState", "BONDED" },
                { "uuids", "None" }
            };

            Dictionary<string, string> device2 = new Dictionary<string, string>
            {
                { "name", "Mock Laptop" },
                { "address", "AA:BB:CC:DD:EE:FF" },
                { "type", "DUAL" },
                { "bondState", "BONDED" },
                { "uuids", "None" }
            };

            mockDevices.Add(device1);
            mockDevices.Add(device2);
            mockDevices.Add(device1);
            mockDevices.Add(device2);
            mockDevices.Add(device1);
            mockDevices.Add(device2);
            mockDevices.Add(device1);
            mockDevices.Add(device2);

            return mockDevices;
        }
    }
}
