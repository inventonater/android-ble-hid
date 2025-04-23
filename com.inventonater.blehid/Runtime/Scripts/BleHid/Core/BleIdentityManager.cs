using System;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class BleIdentityManager
    {
        private const string IDENTITY_UUID_KEY = "ble_peripheral_uuid";
        private const string DEVICE_NAME_KEY = "ble_peripheral_name";
        private const string IDENTITY_CREATED_KEY = "ble_identity_created_time";
        private const string DEFAULT_DEVICE_NAME = "BLE HID Device";

        private readonly ConnectionBridge _connectionBridge;
        public BleIdentityManager(ConnectionBridge connectionBridge) => _connectionBridge = connectionBridge;

        public string GetDeviceName() => PlayerPrefs.GetString(DEVICE_NAME_KEY, DEFAULT_DEVICE_NAME);
        public string GetIdentityCreationDate() => PlayerPrefs.GetString(IDENTITY_CREATED_KEY, "Unknown");
        public List<Dictionary<string, string>> GetBondedDevices() => _connectionBridge.GetBondedDevicesInfo();
        public bool IsDeviceBonded(string address) => _connectionBridge.IsDeviceBonded(address);
        public bool ForgetDevice(string address) => _connectionBridge.RemoveBond(address);

        public bool InitializeIdentity()
        {
            string identityUuid = GetOrCreateDeviceUuid();
            string deviceName = GetDeviceName();

            Debug.Log($"Initializing BLE identity: {identityUuid}, Name: {deviceName}");
            return _connectionBridge.SetBleIdentity(identityUuid, deviceName);
        }

        public string GetOrCreateDeviceUuid()
        {
            if (PlayerPrefs.HasKey(IDENTITY_UUID_KEY)) return PlayerPrefs.GetString(IDENTITY_UUID_KEY);
            string newUuid = Guid.NewGuid().ToString();
            PlayerPrefs.SetString(IDENTITY_UUID_KEY, newUuid);

            PlayerPrefs.SetString(IDENTITY_CREATED_KEY, DateTime.Now.ToString("o"));
            PlayerPrefs.Save();

            Debug.Log($"Created new BLE device identity: {newUuid}");
            return newUuid;
        }

        public bool SetDeviceName(string name)
        {
            if (string.IsNullOrEmpty(name)) name = DEFAULT_DEVICE_NAME;
            PlayerPrefs.SetString(DEVICE_NAME_KEY, name);
            PlayerPrefs.Save();
            return _connectionBridge.SetBleIdentity(GetOrCreateDeviceUuid(), name);
        }

        public bool ResetIdentity()
        {
            string newUuid = Guid.NewGuid().ToString();
            PlayerPrefs.SetString(IDENTITY_UUID_KEY, newUuid);
            PlayerPrefs.SetString(IDENTITY_CREATED_KEY, DateTime.Now.ToString("o"));
            PlayerPrefs.Save();
            Debug.Log($"Reset BLE device identity to: {newUuid}");
            return _connectionBridge.SetBleIdentity(newUuid, GetDeviceName());
        }
    }
}
