using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using System.Text;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Demo script for testing BLE HID functionality.
    /// Attach this to a GameObject in your scene to test basic BLE HID capabilities.
    /// </summary>
    public class BleHidDemo : MonoBehaviour
    {
        [Header("References")]
        [SerializeField] private Button initButton;
        [SerializeField] private Button advertiseButton;
        [SerializeField] private Button stopAdvertiseButton;
        [SerializeField] private Button keyboardButton;
        [SerializeField] private Button mouseButton;
        [SerializeField] private Button mediaButton;
        [SerializeField] private Text statusText;
        [SerializeField] private Text deviceInfoText;
        [SerializeField] private InputField textToTypeField;
        
        [Header("Settings")]
        [SerializeField] private string deviceName = "Inventonater HID";
        
        // BLE HID Manager instance
        private BleHidManager bleHidManager;
        
        // Status tracking
        private bool isInitialized = false;
        private bool isAdvertising = false;
        private bool isConnected = false;
        
        private void Awake()
        {
            // Create BleHidManager component if not already present
            bleHidManager = GetComponent<BleHidManager>();
            if (bleHidManager == null)
            {
                bleHidManager = gameObject.AddComponent<BleHidManager>();
            }
            
            // Set up UI button listeners
            if (initButton != null) 
                initButton.onClick.AddListener(InitializeHID);
            
            if (advertiseButton != null) 
                advertiseButton.onClick.AddListener(StartAdvertising);
            
            if (stopAdvertiseButton != null) 
                stopAdvertiseButton.onClick.AddListener(StopAdvertising);
            
            if (keyboardButton != null) 
                keyboardButton.onClick.AddListener(TestKeyboard);
            
            if (mouseButton != null) 
                mouseButton.onClick.AddListener(TestMouse);
            
            if (mediaButton != null) 
                mediaButton.onClick.AddListener(TestMedia);
            
            // Initialize UI state
            UpdateUI();
        }
        
        private void OnEnable()
        {
            // Register for BleHidManager events
            if (bleHidManager != null)
            {
                bleHidManager.OnInitializeComplete += HandleInitializeComplete;
                bleHidManager.OnAdvertisingStateChanged += HandleAdvertisingStateChanged;
                bleHidManager.OnConnectionStateChanged += HandleConnectionStateChanged;
                bleHidManager.OnError += HandleError;
                bleHidManager.OnDebugLog += HandleDebugLog;
            }
        }
        
        private void OnDisable()
        {
            // Unregister from BleHidManager events
            if (bleHidManager != null)
            {
                bleHidManager.OnInitializeComplete -= HandleInitializeComplete;
                bleHidManager.OnAdvertisingStateChanged -= HandleAdvertisingStateChanged;
                bleHidManager.OnConnectionStateChanged -= HandleConnectionStateChanged;
                bleHidManager.OnError -= HandleError;
                bleHidManager.OnDebugLog -= HandleDebugLog;
            }
        }
        
        public void InitializeHID()
        {
            if (statusText != null)
                statusText.text = "Initializing...";
            
            StartCoroutine(bleHidManager.Initialize());
        }
        
        public void StartAdvertising()
        {
            if (!isInitialized)
            {
                LogMessage("Not initialized. Initialize first.");
                return;
            }
            
            if (bleHidManager.StartAdvertising())
            {
                LogMessage("Started advertising");
            }
            else
            {
                LogMessage("Failed to start advertising");
            }
        }
        
        public void StopAdvertising()
        {
            if (!isInitialized)
            {
                LogMessage("Not initialized.");
                return;
            }
            
            bleHidManager.StopAdvertising();
            LogMessage("Stopped advertising");
        }
        
        public void TestKeyboard()
        {
            if (!isConnected)
            {
                LogMessage("No device connected");
                return;
            }
            
            string textToType = textToTypeField != null ? textToTypeField.text : "Hello from Inventonater BLE HID!";
            
            if (bleHidManager.TypeText(textToType))
            {
                LogMessage("Typed: " + textToType);
            }
            else
            {
                LogMessage("Failed to type text");
            }
        }
        
        public void TestMouse()
        {
            if (!isConnected)
            {
                LogMessage("No device connected");
                return;
            }
            
            // Move mouse in a small circle pattern
            StartCoroutine(MouseCirclePattern());
        }
        
        private IEnumerator MouseCirclePattern()
        {
            LogMessage("Moving mouse in circle pattern...");
            
            for (float angle = 0; angle < Mathf.PI * 2; angle += 0.2f)
            {
                int x = Mathf.RoundToInt(Mathf.Cos(angle) * 20);
                int y = Mathf.RoundToInt(Mathf.Sin(angle) * 20);
                
                bleHidManager.MoveMouse(x, y);
                yield return new WaitForSeconds(0.1f);
            }
            
            // Click left button
            bleHidManager.ClickMouseButton(BleHidConstants.BUTTON_LEFT);
            
            LogMessage("Mouse test completed");
        }
        
        public void TestMedia()
        {
            if (!isConnected)
            {
                LogMessage("No device connected");
                return;
            }
            
            StartCoroutine(MediaSequence());
        }
        
        private IEnumerator MediaSequence()
        {
            LogMessage("Testing media controls...");
            
            // Play/Pause
            bleHidManager.PlayPause();
            yield return new WaitForSeconds(1.0f);
            
            // Volume Up
            for (int i = 0; i < 3; i++)
            {
                bleHidManager.VolumeUp();
                yield return new WaitForSeconds(0.3f);
            }
            
            // Next Track
            bleHidManager.NextTrack();
            yield return new WaitForSeconds(1.0f);
            
            // Previous Track
            bleHidManager.PreviousTrack();
            yield return new WaitForSeconds(1.0f);
            
            // Volume Down
            for (int i = 0; i < 3; i++)
            {
                bleHidManager.VolumeDown();
                yield return new WaitForSeconds(0.3f);
            }
            
            // Play/Pause again
            bleHidManager.PlayPause();
            
            LogMessage("Media test completed");
        }
        
        #region Event Handlers
        
        private void HandleInitializeComplete(bool success, string message)
        {
            isInitialized = success;
            LogMessage("Initialize " + (success ? "succeeded" : "failed") + ": " + message);
            UpdateUI();
        }
        
        private void HandleAdvertisingStateChanged(bool advertising, string message)
        {
            isAdvertising = advertising;
            LogMessage("Advertising " + (advertising ? "started" : "stopped") + ": " + message);
            UpdateUI();
        }
        
        private void HandleConnectionStateChanged(bool connected, string deviceName, string deviceAddress)
        {
            isConnected = connected;
            
            if (connected)
            {
                LogMessage("Connected to: " + deviceName + " (" + deviceAddress + ")");
                if (deviceInfoText != null)
                {
                    deviceInfoText.text = "Connected\nDevice: " + deviceName + "\nAddress: " + deviceAddress;
                }
            }
            else
            {
                LogMessage("Disconnected");
                if (deviceInfoText != null)
                {
                    deviceInfoText.text = "Not Connected";
                }
            }
            
            UpdateUI();
        }
        
        private void HandleError(int errorCode, string errorMessage)
        {
            LogMessage("Error " + errorCode + ": " + errorMessage);
        }
        
        private void HandleDebugLog(string message)
        {
            LogMessage("Debug: " + message);
        }
        
        #endregion
        
        private void LogMessage(string message)
        {
            Debug.Log("[BleHidDemo] " + message);
            
            if (statusText != null)
            {
                StringBuilder sb = new StringBuilder(statusText.text);
                
                // Keep last 10 lines
                string[] lines = statusText.text.Split('\n');
                if (lines.Length >= 10)
                {
                    sb.Clear();
                    for (int i = 1; i < lines.Length; i++)
                    {
                        sb.AppendLine(lines[i]);
                    }
                }
                
                sb.AppendLine(message);
                statusText.text = sb.ToString().TrimStart();
            }
        }
        
        private void UpdateUI()
        {
            // Update buttons based on state
            if (initButton != null)
                initButton.interactable = !isInitialized;
            
            if (advertiseButton != null)
                advertiseButton.interactable = isInitialized && !isAdvertising;
            
            if (stopAdvertiseButton != null)
                stopAdvertiseButton.interactable = isInitialized && isAdvertising;
            
            if (keyboardButton != null)
                keyboardButton.interactable = isConnected;
            
            if (mouseButton != null)
                mouseButton.interactable = isConnected;
            
            if (mediaButton != null)
                mediaButton.interactable = isConnected;
        }
    }
}
