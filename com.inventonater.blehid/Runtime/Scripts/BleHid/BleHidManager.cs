using UnityEngine;
using Inventonater.BleHid.InputControllers;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Main Unity interface for BLE HID functionality.
    /// This class provides methods to control Bluetooth HID emulation for keyboard, mouse, and media control.
    /// </summary>
    public class BleHidManager : MonoBehaviour
    {
        // State properties
        public bool IsInitialized { get; internal set; }
        public bool IsAdvertising { get; internal set; }
        public bool IsConnected { get; internal set; }
        public string ConnectedDeviceName { get; internal set; }
        public string ConnectedDeviceAddress { get; internal set; }
        public string LastErrorMessage { get; internal set; }
        public int LastErrorCode { get; internal set; }
        public int ConnectionInterval { get; internal set; }
        public int SlaveLatency { get; internal set; }
        public int SupervisionTimeout { get; internal set; }
        public int MtuSize { get; internal set; }
        public int Rssi { get; internal set; }
        public int TxPowerLevel { get; internal set; }

        // Component references
        public BleInitializer BleInitializer { get; private set; }
        public BleEventSystem BleEventSystem { get; private set; }
        public BleAdvertiser BleAdvertiser { get; private set; }
        public ConnectionManager ConnectionManager { get; private set; }
        public ServiceManager ServiceManager { get; private set; }
        public static BleHidManager Instance { get; private set; }
        public KeyboardController Keyboard { get; private set; }
        public MouseController Mouse { get; private set; }
        public MediaController Media { get; private set; }

        private void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }

            Instance = this;
            DontDestroyOnLoad(gameObject);

            BleEventSystem = gameObject.AddComponent<BleEventSystem>();
            BleInitializer = new BleInitializer(this);
            BleAdvertiser = new BleAdvertiser(this);
            ConnectionManager = new ConnectionManager(this);
            Keyboard = new KeyboardController(this);
            Mouse = new MouseController(this);
            Media = new MediaController(this);
            ServiceManager = new ServiceManager(this);

            Debug.Log("BleHidManager initialized");
        }

        private void OnDestroy()
        {
            BleInitializer.Close();
        }

        public bool ConfirmIsInitialized()
        {
            if (IsInitialized && BleInitializer.BridgeInstance != null) return true;

            string message = "BLE HID plugin not initialized";
            Debug.LogError(message);
            BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_NOT_INITIALIZED, message);
            return false;
        }

        public bool ConfirmIsConnected()
        {
            if (!ConfirmIsInitialized()) return false;
            if (IsConnected) return true;

            string message = "No BLE device connected";
            Debug.LogError(message);
            BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_NOT_CONNECTED, message);
            return false;
        }
    }
}
