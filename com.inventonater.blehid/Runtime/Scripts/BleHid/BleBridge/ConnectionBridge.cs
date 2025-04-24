using System;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Manages BLE connection parameters and connection-related functionality.
    /// </summary>
    public class ConnectionBridge
    {
        private const string IDENTITY_UUID_KEY = "ble_peripheral_uuid";
        private const string DEVICE_NAME_KEY = "ble_peripheral_name";
        private const string IDENTITY_CREATED_KEY = "ble_identity_created_time";
        private const string DEFAULT_DEVICE_NAME = "BLE HID Device";

        private readonly JavaBridge _java;
        public ConnectionBridge(JavaBridge java) => _java = java;

        private bool _isConnected;
        private int _rssi;

        public bool IsConnected
        {
            get => _isConnected || Application.isEditor;
            internal set => _isConnected = value;
        }

        public string ConnectedDeviceName { get; internal set; }
        public string ConnectedDeviceAddress { get; internal set; }

        public bool IsAdvertising { get; internal set; }
        public int ConnectionInterval { get; private set; }
        public int SlaveLatency { get; private set; }
        public int SupervisionTimeout { get; private set; }
        public int MtuSize { get; private set; }

        public int Rssi
        {
            get => _rssi;
            internal set => _rssi = value;
        }

        public Color RssiColor =>
            _rssi switch
            {
                > -60 => Color.green,
                > -80 => Color.yellow,
                _ => Color.red
            };

        public void DrawRssiLabel()
        {
            GUIStyle rssiStyle = new GUIStyle(GUI.skin.label);
            rssiStyle.normal.textColor = RssiColor;

            string signalStrength = "";
            var rssi = Rssi.ToString();
            if (rssi != "--")
            {
                int rssiValue = int.Parse(rssi);
                if (rssiValue > -60) signalStrength = " (Excellent)";
                else if (rssiValue > -70) signalStrength = " (Good)";
                else if (rssiValue > -80) signalStrength = " (Fair)";
                else signalStrength = " (Poor)";
            }

            var rssiString = "RSSI: " + rssi + " dBm" + signalStrength;

            GUILayout.Label(rssiString, rssiStyle);
        }

        public int TxPowerLevel { get; internal set; }

        public bool StartAdvertising() => _java.Call<bool>("startAdvertising");
        public void StopAdvertising() => _java.Call("stopAdvertising");
        public bool GetAdvertisingState() => _java.Call<bool>("isAdvertising");

        public string GetDeviceName() => PlayerPrefs.GetString(DEVICE_NAME_KEY, DEFAULT_DEVICE_NAME);
        public string GetIdentityCreationDate() => PlayerPrefs.GetString(IDENTITY_CREATED_KEY, "Unknown");

        public void SetConnectionState(bool connected, string deviceName, string deviceAddress)
        {
            IsConnected = connected;
            ConnectedDeviceName = deviceName;
            ConnectedDeviceAddress = deviceAddress;
        }

        public void SetConnectionParameters(int interval, int latency, int timeout, int mtu)
        {
            ConnectionInterval = interval;
            SlaveLatency = latency;
            SupervisionTimeout = timeout;
            MtuSize = mtu;
        }

        public bool InitializeIdentity()
        {
            string identityUuid = GetOrCreateDeviceUuid();
            string deviceName = GetDeviceName();

            LoggingManager.Instance.Log($"Initializing BLE identity: {identityUuid}, Name: {deviceName}");
            return SetBleIdentity(identityUuid, deviceName);
        }

        public string GetOrCreateDeviceUuid()
        {
            if (PlayerPrefs.HasKey(IDENTITY_UUID_KEY)) return PlayerPrefs.GetString(IDENTITY_UUID_KEY);
            string newUuid = Guid.NewGuid().ToString();
            PlayerPrefs.SetString(IDENTITY_UUID_KEY, newUuid);

            PlayerPrefs.SetString(IDENTITY_CREATED_KEY, DateTime.Now.ToString("o"));
            PlayerPrefs.Save();

            LoggingManager.Instance.Log($"Created new BLE device identity: {newUuid}");
            return newUuid;
        }

        public bool SetDeviceName(string name)
        {
            if (string.IsNullOrEmpty(name)) name = DEFAULT_DEVICE_NAME;
            PlayerPrefs.SetString(DEVICE_NAME_KEY, name);
            PlayerPrefs.Save();
            return SetBleIdentity(GetOrCreateDeviceUuid(), name);
        }

        public bool ResetIdentity()
        {
            string newUuid = Guid.NewGuid().ToString();
            PlayerPrefs.SetString(IDENTITY_UUID_KEY, newUuid);
            PlayerPrefs.SetString(IDENTITY_CREATED_KEY, DateTime.Now.ToString("o"));
            PlayerPrefs.Save();
            LoggingManager.Instance.Log($"Reset BLE device identity to: {newUuid}");
            return SetBleIdentity(newUuid, GetDeviceName());
        }

        public bool SetTransmitPowerLevel(int level)
        {
            if (level is >= 0 and <= 2) return _java.Call<bool>("setTransmitPowerLevel", level);

            LoggingManager.Instance.Error("Invalid TX power level: " + level + ". Must be between 0 and 2.");
            return false;
        }

        /// <summary>
        /// Request a change in connection priority.
        /// Connection priority affects latency and power consumption.
        /// </summary>
        /// <param name="priority">The priority to request (0=HIGH, 1=BALANCED, 2=LOW_POWER)</param>
        /// <returns>True if the request was sent, false otherwise.</returns>
        public bool RequestConnectionPriority(int priority) => _java.Call<bool>("requestConnectionPriority", priority);

        /// <summary>
        /// Request a change in MTU (Maximum Transmission Unit) size.
        /// Larger MTU sizes can improve throughput.
        /// </summary>
        /// <param name="mtu">The MTU size to request (23-517 bytes)</param>
        /// <returns>True if the request was sent, false otherwise.</returns>
        public bool RequestMtu(int mtu)
        {
            if (mtu is >= 23 and <= 517) return _java.Call<bool>("requestMtu", mtu);
            LoggingManager.Instance.Error("Invalid MTU size: " + mtu + ". Must be between 23 and 517.");
            return false;
        }

        /// <summary>
        /// Reads the current RSSI (signal strength) value.
        /// </summary>
        /// <returns>True if the read request was sent, false otherwise.</returns>
        public bool ReadRssi() => _java.Call<bool>("readRssi");

        /// <summary>
        /// Gets all connection parameters as a dictionary.
        /// </summary>
        /// <returns>Dictionary of parameter names to values, or null if not connected.</returns>
        public Dictionary<string, string> GetConnectionParameters()
        {
            try
            {
                AndroidJavaObject parametersMap = _java.Call<AndroidJavaObject>("getConnectionParameters");
                if (parametersMap == null) { return null; }

                Dictionary<string, string> result = new Dictionary<string, string>();

                // Convert Java Map to C# Dictionary
                using AndroidJavaObject entrySet = parametersMap.Call<AndroidJavaObject>("entrySet");
                using AndroidJavaObject iterator = entrySet.Call<AndroidJavaObject>("iterator");

                while (iterator.Call<bool>("hasNext"))
                {
                    using AndroidJavaObject entry = iterator.Call<AndroidJavaObject>("next");
                    string key = entry.Call<AndroidJavaObject>("getKey").Call<string>("toString");
                    string value = entry.Call<AndroidJavaObject>("getValue").Call<string>("toString");
                    result[key] = value;
                }

                return result;
            }
            catch (Exception e)
            {
                LoggingManager.Instance.Error("Exception getting connection parameters: " + e.Message);
                return null;
            }
        }

        /// <summary>
        /// Disconnects from the currently connected device.
        /// </summary>
        /// <returns>True if the disconnect command was sent successfully, false otherwise.</returns>
        public bool Disconnect() => _java.Call<bool>("disconnect");

        /// <summary>
        /// Sets the BLE peripheral identity (UUID and device name) for consistent recognition across app restarts.
        /// </summary>
        /// <param name="identityUuid">The UUID string to use as the device's unique identifier</param>
        /// <param name="deviceName">Optional custom device name</param>
        /// <returns>True if identity was set successfully</returns>
        public bool SetBleIdentity(string identityUuid, string deviceName) => _java.Call<bool>("setBleIdentity", identityUuid, deviceName);

        /// <summary>
        /// Gets detailed information about all devices currently bonded to this peripheral.
        /// </summary>
        /// <returns>List of dictionaries containing device information</returns>
        public List<Dictionary<string, string>> GetBondedDevices()
        {
            if (Application.isEditor) return MockBondedDevicesInfo();
            AndroidJavaObject javaList = _java.Call<AndroidJavaObject>("getBondedDevicesInfo");
            return ConvertJavaListToDeviceInfoList(javaList);
        }

        /// <summary>
        /// Checks if a specific device is bonded to this peripheral.
        /// </summary>
        /// <param name="address">MAC address of the device to check</param>
        /// <returns>True if the device is bonded</returns>
        public bool IsDeviceBonded(string address)
        {
            if (Application.isEditor) return true;
            return _java.Call<bool>("isDeviceBonded", address);
        }

        /// <summary>
        /// Removes a bond with a specific device (forget pairing).
        /// </summary>
        /// <param name="address">MAC address of the device to forget</param>
        /// <returns>True if the bond was successfully removed</returns>
        public bool RemoveBond(string address)
        {
            if (Application.isEditor) return true;
            return _java.Call<bool>("removeBond", address);
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
