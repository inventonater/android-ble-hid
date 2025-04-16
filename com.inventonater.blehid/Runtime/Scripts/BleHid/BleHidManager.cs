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
        private BleInitializer _bleInitializer;
        private BleEventSystem _bleEventSystem;
        private BleAdvertiser _bleAdvertiser;
        private ConnectionManager _connectionManager;
        private InputController _inputController;
        private ServiceManager _serviceManager;
        private BleUtils _bleUtils;

        // Public accessors for components
        public BleInitializer BleInitializer => _bleInitializer;
        public BleEventSystem BleEventSystem => _bleEventSystem;
        public BleAdvertiser BleAdvertiser => _bleAdvertiser;
        public ConnectionManager ConnectionManager => _connectionManager;
        public InputController InputController => _inputController;
        public ServiceManager ServiceManager => _serviceManager;
        public BleUtils BleUtils => _bleUtils;
        public static BleHidManager Instance { get; private set; }

        private void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }

            Instance = this;
            DontDestroyOnLoad(gameObject);

            // Initialize all components
            _bleUtils = new BleUtils(this);
            _bleEventSystem = gameObject.AddComponent<BleEventSystem>();
            _bleInitializer = new BleInitializer(this);
            _bleAdvertiser = new BleAdvertiser(this);
            _connectionManager = new ConnectionManager(this);
            _inputController = new InputController(this);
            _serviceManager = new ServiceManager(this);

            Debug.Log("BleHidManager initialized");
        }

        private void OnDestroy()
        {
            _bleInitializer.Close();
        }
    }
}
