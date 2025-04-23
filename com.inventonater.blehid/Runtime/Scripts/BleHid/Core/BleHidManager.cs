using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    [DefaultExecutionOrder(ExecutionOrder.Initialize)]
    public class BleHidManager : MonoBehaviour
    {
        public bool IsInitialized { get; private set; }
        public bool IsInPipMode { get; internal set; }
        public PipBackgroundWorker PipWorker { get; private set; }

        public JavaBridge JavaBridge { get; private set; }
        public BleEventSystem BleEventSystem { get; private set; }
        public ConnectionBridge ConnectionBridge { get; private set; }
        public InputRouter InputRouter { get; private set; }
        public InputDeviceMapping Mapping { get; private set; }
        public BleIdentityManager IdentityManager { get; private set; }
        public BleBridge BleBridge { get; private set; }

        public static BleHidManager Instance => FindFirstObjectByType<BleHidManager>();

        private void Awake()
        {
            Debug.Log("BleHidManager starting");

            Application.runInBackground = true;

            JavaBridge = new JavaBridge();
            BleBridge = new BleBridge(JavaBridge);
            BleEventSystem = gameObject.AddComponent<BleEventSystem>();
            Mapping = gameObject.AddComponent<InputDeviceMapping>();
            InputRouter = gameObject.AddComponent<InputRouter>();
            InputRouter.SetMapping(Mapping);

            ConnectionBridge = new ConnectionBridge(JavaBridge);
            IdentityManager = new BleIdentityManager(ConnectionBridge);
            PipWorker = new PipBackgroundWorker();

            BleEventSystem.OnPipModeChanged += HandlePipModeChanged;
            Debug.Log("BleHidManager initialized");
        }

        private void Start()
        {
            Initialize();
        }

        private void Initialize()
        {
            if (IsInitialized) return;
            Application.runInBackground = true;
            if (Application.isEditor)
            {
                LoggingManager.Instance.AddLogEntry("BLE HID is only supported on Android");
                return;
            }

            try
            {
                BleHidPermissionHandler.RequestAllPermissions();

                IsInitialized = JavaBridge.Call<bool>("initialize", gameObject.name);

                // Initialize the foreground service manager
                // manager.ForegroundServiceManager.Initialize(bridgeInstance);

                if (!IsInitialized)
                {
                    string message = "Failed to initialize BLE HID plugin";
                    LoggingManager.Instance.AddLogError(message);
                    BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_INITIALIZATION_FAILED, message);
                    BleEventSystem.OnInitializeComplete?.Invoke(false, message);
                    return;
                }

                IdentityManager.InitializeIdentity();
            }
            catch (Exception e)
            {
                string message = "Exception during initialization: " + e.Message;
                LoggingManager.Instance.AddLogError(message);
                BleEventSystem.OnError?.Invoke(BleHidConstants.ERROR_INITIALIZATION_FAILED, message);
                BleEventSystem.OnInitializeComplete?.Invoke(false, message);
            }
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
            Close();
        }
    }
}
