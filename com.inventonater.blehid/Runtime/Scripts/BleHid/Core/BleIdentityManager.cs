using System;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Manages the persistent identity of the BLE HID peripheral device.
    /// This ensures the device maintains a consistent identity across app restarts,
    /// allowing paired devices to recognize it without requiring re-pairing.
    /// </summary>
    public class BleIdentityManager
    {
        private const string IDENTITY_UUID_KEY = "ble_peripheral_uuid";
        private const string DEVICE_NAME_KEY = "ble_peripheral_name";
        private const string IDENTITY_CREATED_KEY = "ble_identity_created_time";
        private const string DEFAULT_DEVICE_NAME = "BLE HID Device";

        private BleHidManager _bleHidManager;

        public BleIdentityManager(BleHidManager bleHidManager)
        {
            _bleHidManager = bleHidManager;
        }

        /// <summary>
        /// Initializes the device identity, either loading an existing one or creating a new one.
        /// Call this when the BLE service is starting.
        /// </summary>
        /// <returns>True if identity was successfully set</returns>
        public bool InitializeIdentity()
        {
            string identityUuid = GetOrCreateDeviceUuid();
            string deviceName = GetDeviceName();
            
            Debug.Log($"Initializing BLE identity: {identityUuid}, Name: {deviceName}");
            return _bleHidManager.BleBridge.Identity.SetBleIdentity(identityUuid, deviceName);
        }

        /// <summary>
        /// Gets the current device UUID from PlayerPrefs or creates a new one if it doesn't exist.
        /// </summary>
        /// <returns>The device UUID as a string</returns>
        public string GetOrCreateDeviceUuid()
        {
            if (PlayerPrefs.HasKey(IDENTITY_UUID_KEY))
            {
                return PlayerPrefs.GetString(IDENTITY_UUID_KEY);
            }

            // Create a new UUID
            string newUuid = Guid.NewGuid().ToString();
            PlayerPrefs.SetString(IDENTITY_UUID_KEY, newUuid);
            
            // Store creation time
            PlayerPrefs.SetString(IDENTITY_CREATED_KEY, DateTime.Now.ToString("o"));
            PlayerPrefs.Save();
            
            Debug.Log($"Created new BLE device identity: {newUuid}");
            return newUuid;
        }

        /// <summary>
        /// Gets the current device name from PlayerPrefs or uses the default if not set.
        /// </summary>
        /// <returns>The device name</returns>
        public string GetDeviceName()
        {
            return PlayerPrefs.HasKey(DEVICE_NAME_KEY) 
                ? PlayerPrefs.GetString(DEVICE_NAME_KEY) 
                : DEFAULT_DEVICE_NAME;
        }

        /// <summary>
        /// Sets a custom device name.
        /// </summary>
        /// <param name="name">The new device name</param>
        /// <returns>True if the name was successfully set</returns>
        public bool SetDeviceName(string name)
        {
            if (string.IsNullOrEmpty(name))
            {
                name = DEFAULT_DEVICE_NAME;
            }

            PlayerPrefs.SetString(DEVICE_NAME_KEY, name);
            PlayerPrefs.Save();
            
            // Update the name in the BLE peripheral
            return _bleHidManager.BleBridge.Identity.SetBleIdentity(GetOrCreateDeviceUuid(), name);
        }

        /// <summary>
        /// Resets the device identity, generating a new UUID.
        /// This will cause all paired devices to see this as a completely new device.
        /// </summary>
        /// <returns>True if identity reset was successful</returns>
        public bool ResetIdentity()
        {
            // Generate a new UUID
            string newUuid = Guid.NewGuid().ToString();
            PlayerPrefs.SetString(IDENTITY_UUID_KEY, newUuid);
            
            // Store creation time
            PlayerPrefs.SetString(IDENTITY_CREATED_KEY, DateTime.Now.ToString("o"));
            PlayerPrefs.Save();
            
            Debug.Log($"Reset BLE device identity to: {newUuid}");
            
            // Update the identity in the BLE peripheral
            return _bleHidManager.BleBridge.Identity.SetBleIdentity(newUuid, GetDeviceName());
        }

        /// <summary>
        /// Gets when the current identity was created.
        /// </summary>
        /// <returns>Creation date string, or "Unknown" if not available</returns>
        public string GetIdentityCreationDate()
        {
            if (PlayerPrefs.HasKey(IDENTITY_CREATED_KEY))
            {
                return PlayerPrefs.GetString(IDENTITY_CREATED_KEY);
            }
            return "Unknown";
        }

        /// <summary>
        /// Gets a list of all devices currently bonded (paired) with this peripheral.
        /// </summary>
        /// <returns>List of bonded device information</returns>
        public List<Dictionary<string, string>> GetBondedDevices()
        {
            return _bleHidManager.BleBridge.Identity.GetBondedDevicesInfo();
        }

        /// <summary>
        /// Checks if a specific device is currently bonded to this peripheral.
        /// </summary>
        /// <param name="address">The Bluetooth MAC address to check</param>
        /// <returns>True if the device is bonded</returns>
        public bool IsDeviceBonded(string address)
        {
            return _bleHidManager.BleBridge.Identity.IsDeviceBonded(address);
        }

        /// <summary>
        /// Removes a bond with a specific device (forget the device).
        /// </summary>
        /// <param name="address">The Bluetooth MAC address of the device to forget</param>
        /// <returns>True if the device was successfully forgotten</returns>
        public bool ForgetDevice(string address)
        {
            return _bleHidManager.BleBridge.Identity.RemoveBond(address);
        }
        /// <summary>
        /// Initiates pairing (bonding) with a remote device by MAC address.
        /// </summary>
        /// <param name="address">The MAC address of the device to pair with.</param>
        /// <returns>True if the bond request was successfully initiated.</returns>
        public bool PairDevice(string address)
        {
            return _bleHidManager.BleBridge.Identity.PairDevice(address);
        }

        /// <summary>
        /// Gets the raw bond state of a remote device.
        /// </summary>
        /// <param name="address">The MAC address of the device to query.</param>
        /// <returns>One of BluetoothDevice.BOND_NONE, BOND_BONDING, or BOND_BONDED.</returns>
        public int GetBondState(string address)
        {
            return _bleHidManager.BleBridge.Identity.GetBondState(address);
        }
    }
}
