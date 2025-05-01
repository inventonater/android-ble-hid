using System;
using System.Collections.Generic;
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
        public AccessibilityServiceBridge AccessibilityService { get; private set; }

        public static BleHidManager Instance => FindFirstObjectByType<BleHidManager>();

        private void Awake()
        {
            Debug.Log("BleHidManager starting");

            Application.runInBackground = true;

            JavaBridge = new JavaBridge();
            BleBridge = new BleBridge(JavaBridge);
            AccessibilityService = new AccessibilityServiceBridge(JavaBridge);

            JavaBroadcaster = gameObject.AddComponent<JavaBroadcaster>();
            InputRouter = gameObject.AddComponent<InputRouter>();

            var accessibilityServiceRegistry = new ActionRegistry(AccessibilityService);
            _localMedia = InputDeviceMapping.LocalMedia(accessibilityServiceRegistry);
            _localDPad = InputDeviceMapping.LocalDPad(accessibilityServiceRegistry);

            var bleHidRegistry = new ActionRegistry(BleBridge.Mouse, BleBridge.Keyboard, BleBridge.Media);
            _bleMouse = InputDeviceMapping.BleMouse(bleHidRegistry);
            _bleMedia = InputDeviceMapping.BleMedia(bleHidRegistry);

            InputRouter.AddMapping(_localMedia);
            InputRouter.AddMapping(_localDPad);
            InputRouter.AddMapping(_bleMouse);
            InputRouter.AddMapping(_bleMedia);

            ConnectionBridge = new ConnectionBridge(JavaBridge);
            PipWorker = new PipBackgroundWorker();

            // JavaBroadcaster.OnPipModeChanged += HandlePipModeChanged;
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
            Application.runInBackground = true;
            try
            {
                await BleBridge.Permissions.Initialize();
                await AccessibilityService.Initialize();
                IsInitialized = Application.isEditor || JavaBridge.Call<bool>("initialize", gameObject.name);
                // manager.ForegroundServiceManager.Initialize(bridgeInstance);
                ConnectionBridge.InitializeIdentity();
            }
            catch (Exception e) { LoggingManager.Instance.Exception(e); }

            if (!IsInitialized) LoggingManager.Instance.Error("BleHidManager Initialization failed");
        }

        public void Close()
        {
            ConnectionBridge.IsAdvertising = false;
            ConnectionBridge.IsConnected = false;
            ConnectionBridge.ConnectedDeviceName = null;
            ConnectionBridge.ConnectedDeviceAddress = null;
            Debug.Log("BleHidManager closed");
        }

        // public void HandlePipModeChanged(bool isInPipMode)
        // {
        //     IsInPipMode = isInPipMode;
        //
        //     if (isInPipMode)
        //     {
        //         // Entering PiP mode - make sure our foreground service is running to keep the app alive
        //         Debug.Log("Entering PiP mode - ensuring foreground service is running");
        //         // ForegroundServiceManager.EnsureServiceRunning(true);
        //
        //         // Start the background worker thread to continue processing in PiP mode
        //         Debug.Log("Starting background worker for PiP mode");
        //         PipWorker.Start();
        //
        //         // Log the event to assist with debugging
        //         LoggingManager.Instance.Log($"PiP mode entered - background worker started at {DateTime.Now}");
        //     }
        //     else
        //     {
        //         // Exiting PiP mode - we can continue with normal operation
        //         Debug.Log("Exiting PiP mode");
        //
        //         // Get the background worker status before stopping (for diagnostics)
        //         // string workerStatus = PipWorker.GetStatus();
        //         // Debug.Log($"Background worker status before exiting PiP mode: {workerStatus}");
        //
        //         // We can stop the background worker since we're back to normal mode
        //         // Uncomment the line below if you want to stop the worker on exit
        //         // (for testing, we may want to keep it running to see if it continued during PiP)
        //         // PipWorker.Stop();
        //
        //         // Keep the service running to maintain functionality
        //         // The service can be stopped manually if needed
        //
        //         // Log the event to assist with debugging
        //         LoggingManager.Instance.Log($"PiP mode exited at {DateTime.Now}");
        //     }
        // }

        private void OnDestroy()
        {
            // Stop the background worker if it's running
            PipWorker.Stop();
            Close();
        }
    }
}
