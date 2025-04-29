using System;
using Cysharp.Threading.Tasks;
using UnityEngine;

namespace Inventonater.BleHid
{
    [DefaultExecutionOrder(ExecutionOrder.Initialize)]
    public class BleHidManager : MonoBehaviour
    {
        private InputDeviceMapping _localDPad;
        private InputDeviceMapping _localMedia;
        private InputDeviceMapping _bleMouse;
        private InputDeviceMapping _bleMedia;
        private InputDeviceMapping _localDragNavigation;

        public bool IsInitialized { get; private set; }
        public bool IsInPipMode { get; internal set; }
        public PipBackgroundWorker PipWorker { get; private set; }
        public JavaBridge JavaBridge { get; private set; }
        public JavaBroadcaster JavaBroadcaster { get; private set; }
        public ConnectionBridge ConnectionBridge { get; private set; }
        public InputRouter InputRouter { get; private set; }
        public BleBridge BleBridge { get; private set; }
        public static BleHidManager Instance => FindFirstObjectByType<BleHidManager>();

        private void Awake()
        {
            Debug.Log("BleHidManager starting");

            Application.runInBackground = true;

            JavaBridge = new JavaBridge();
            BleBridge = new BleBridge(JavaBridge);

            JavaBroadcaster = gameObject.AddComponent<JavaBroadcaster>();
            InputRouter = gameObject.AddComponent<InputRouter>();

            var actionRegistry = BleBridge.ActionRegistry;
            _localMedia = InputDeviceMapping.LocalMedia(actionRegistry);
            _localDPad = InputDeviceMapping.LocalDPad(actionRegistry);
            _bleMouse = InputDeviceMapping.BleMouse(actionRegistry);
            _bleMedia = InputDeviceMapping.BleMedia(actionRegistry);

            InputRouter.AddMapping(_localMedia);
            InputRouter.AddMapping(_localDPad);
            InputRouter.AddMapping(_bleMouse);
            InputRouter.AddMapping(_bleMedia);

            ConnectionBridge = new ConnectionBridge(JavaBridge);
            PipWorker = new PipBackgroundWorker();

            JavaBroadcaster.OnPipModeChanged += HandlePipModeChanged;
            JavaBroadcaster.OnAdvertisingStateChanged += (advertising, message) => ConnectionBridge.IsAdvertising = advertising;
            JavaBroadcaster.OnConnectionStateChanged += (connected, deviceName, address) => ConnectionBridge.SetConnectionState(connected, deviceName, address);
            JavaBroadcaster.OnConnectionParametersChanged += (interval, latency, timeout, mtu) => ConnectionBridge.SetConnectionParameters(interval, latency, timeout, mtu);
            JavaBroadcaster.OnRssiRead += rssi => ConnectionBridge.Rssi = rssi;

            Debug.Log("BleHidManager initialized");
        }

        private async void Start()
        {
            await Initialize();
        }

        private async UniTask Initialize()
        {
            if (IsInitialized) return;

            Application.runInBackground = true;
            try
            {
                await BleBridge.Permissions.Initialize();
                await BleBridge.AccessibilityService.Initialize();

                IsInitialized = StartInitialize();

                // Initialize the foreground service manager
                // manager.ForegroundServiceManager.Initialize(bridgeInstance);

                if (!IsInitialized)
                {
                    string message = "Failed to initialize BLE HID plugin";
                    LoggingManager.Instance.Error(message);
                    JavaBroadcaster.OnError?.Invoke(BleHidConstants.ERROR_INITIALIZATION_FAILED, message);
                    JavaBroadcaster.OnInitializeComplete?.Invoke(false, message);
                    return;
                }

                ConnectionBridge.InitializeIdentity();
            }

            catch (Exception e)
            {
                string message = "Exception during initialization: " + e.Message;
                LoggingManager.Instance.Error(message);
                JavaBroadcaster.OnError?.Invoke(BleHidConstants.ERROR_INITIALIZATION_FAILED, message);
                JavaBroadcaster.OnInitializeComplete?.Invoke(false, message);
            }
        }

        private bool StartInitialize()
        {
            if(Application.isEditor) return true;
            return JavaBridge.Call<bool>("initialize", gameObject.name);
        }

        public void Close()
        {
            ConnectionBridge.IsAdvertising = false;
            ConnectionBridge.IsConnected = false;
            ConnectionBridge.ConnectedDeviceName = null;
            ConnectionBridge.ConnectedDeviceAddress = null;
            Debug.Log("BleHidManager closed");
        }

        public void HandlePipModeChanged(bool isInPipMode)
        {
            IsInPipMode = isInPipMode;

            if (isInPipMode)
            {
                // Entering PiP mode - make sure our foreground service is running to keep the app alive
                Debug.Log("Entering PiP mode - ensuring foreground service is running");
                // ForegroundServiceManager.EnsureServiceRunning(true);
                
                // Start the background worker thread to continue processing in PiP mode
                Debug.Log("Starting background worker for PiP mode");
                PipWorker.Start();
                
                // Log the event to assist with debugging
                LoggingManager.Instance.Log($"PiP mode entered - background worker started at {DateTime.Now}");
            }
            else
            {
                // Exiting PiP mode - we can continue with normal operation
                Debug.Log("Exiting PiP mode");
                
                // Get the background worker status before stopping (for diagnostics)
                // string workerStatus = PipWorker.GetStatus();
                // Debug.Log($"Background worker status before exiting PiP mode: {workerStatus}");
                
                // We can stop the background worker since we're back to normal mode
                // Uncomment the line below if you want to stop the worker on exit
                // (for testing, we may want to keep it running to see if it continued during PiP)
                // PipWorker.Stop();
                
                // Keep the service running to maintain functionality
                // The service can be stopped manually if needed
                
                // Log the event to assist with debugging
                LoggingManager.Instance.Log($"PiP mode exited at {DateTime.Now}");
            }
        }

        private void OnDestroy()
        {
            // Stop the background worker if it's running
            PipWorker.Stop();
            Close();
        }
    }
}
