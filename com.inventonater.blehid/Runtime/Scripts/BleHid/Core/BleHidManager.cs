using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    [DefaultExecutionOrder(ExecutionOrder.Initialize)]
    public class BleHidManager : MonoBehaviour
    {
        public bool IsAdvertising { get; internal set; }
        public bool IsConnected { get; internal set; }
        public string ConnectedDeviceName { get; internal set; }
        public string ConnectedDeviceAddress { get; internal set; }
        public int ConnectionInterval { get; internal set; }
        public int SlaveLatency { get; internal set; }
        public int SupervisionTimeout { get; internal set; }
        public int MtuSize { get; internal set; }
        public int Rssi { get; internal set; }
        public int TxPowerLevel { get; internal set; }
        
        public bool IsInPipMode { get; internal set; }
        public PipBackgroundWorker PipWorker { get; private set; }

        public BleInitializer BleInitializer { get; private set; }
        public BleEventSystem BleEventSystem { get; private set; }
        public BleAdvertiser BleAdvertiser { get; private set; }
        public ConnectionManager ConnectionManager { get; private set; }
        public InputRouter InputRouter { get; private set; }
        public InputDeviceMapping Mapping { get; private set; }
        public BleIdentityManager IdentityManager { get; private set; }

        public BleBridge BleBridge { get; private set; }
        public static BleHidManager Instance => FindFirstObjectByType<BleHidManager>();

        private void Awake()
        {
            Debug.Log("BleHidManager starting");
            if (Application.isEditor) IsConnected = true;

            Application.runInBackground = true;

            BleBridge = new BleBridge(this);
            BleEventSystem = gameObject.AddComponent<BleEventSystem>();
            Mapping = gameObject.AddComponent<InputDeviceMapping>();
            InputRouter = gameObject.AddComponent<InputRouter>();
            InputRouter.SetMapping(Mapping);

            BleInitializer = new BleInitializer(this);
            BleAdvertiser = new BleAdvertiser(this);
            ConnectionManager = new ConnectionManager(this);
            IdentityManager = new BleIdentityManager(BleBridge.Identity);
            PipWorker = new PipBackgroundWorker();

            BleEventSystem.OnPipModeChanged += HandlePipModeChanged;
            Debug.Log("BleHidManager initialized");
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
                LoggingManager.Instance.AddLogEntry($"PiP mode entered - background worker started at {DateTime.Now}");
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
                LoggingManager.Instance.AddLogEntry($"PiP mode exited at {DateTime.Now}");
            }
        }

        private void OnDestroy()
        {
            // Stop the background worker if it's running
            PipWorker.Stop();
            BleInitializer.Close();
        }

        public bool ConfirmIsInitialized()
        {
            if (BleInitializer.IsInitialized) return true;

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
