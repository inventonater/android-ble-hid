using System;
using System.Collections;
using UnityEngine;
using UnityEngine.UI;

#if UNITY_ANDROID
using UnityEngine.Android;
#endif

/// <summary>
/// Unity manager for BLE HID functionality.
/// This class provides a Unity-friendly interface to the Android BLE HID plugin.
/// </summary>
public class BleHidManager : MonoBehaviour
{
    // References to UI components
    public Button connectButton;
    public Button mouseButton;
    public Button keyboardButton;
    public Text statusText;
    
    // Private state
    private bool isInitialized = false;
    private bool isAdvertising = false;
    
    // Status strings
    private const string STATUS_NOT_INITIALIZED = "Status: Not initialized";
    private const string STATUS_READY = "Status: Ready";
    private const string STATUS_ADVERTISING = "Status: Advertising";
    private const string STATUS_CONNECTED = "Status: Connected";
    
#if UNITY_ANDROID
    // Connection listener implementation (for Android)
    private class ConnectionListener : AndroidJavaProxy
    {
        private BleHidManager manager;
        
        public ConnectionListener(BleHidManager manager) 
            : base("com.inventonater.hid.unity.UnityConnectionListener")
        {
            this.manager = manager;
        }
        
        public void onConnected(string deviceAddress)
        {
            // Use main thread for UI updates
            manager.RunOnMainThread(() =>
            {
                manager.statusText.text = STATUS_CONNECTED;
                manager.isAdvertising = false;
                manager.UpdateUI();
                Debug.Log($"Connected to device: {deviceAddress}");
            });
        }
        
        public void onDisconnected(string deviceAddress)
        {
            // Use main thread for UI updates
            manager.RunOnMainThread(() =>
            {
                manager.statusText.text = STATUS_READY;
                Debug.Log($"Disconnected from device: {deviceAddress}");
                
                // Restart advertising after disconnection
                if (manager.isInitialized && !manager.isAdvertising)
                {
                    manager.StartAdvertising();
                }
            });
        }
    }
    
    // Android plugin
    private AndroidJavaClass unityBleHid;
#endif
    
    // Start is called before the first frame update
    void Start()
    {
        // Set initial UI state
        statusText.text = STATUS_NOT_INITIALIZED;
        UpdateUI();
        
#if UNITY_ANDROID
        // Request required permissions
        RequestPermissions();
#endif
    }
    
    // Update UI state based on connection state
    private void UpdateUI()
    {
        connectButton.interactable = isInitialized;
        
        if (isAdvertising)
        {
            connectButton.GetComponentInChildren<Text>().text = "Stop Advertising";
        }
        else
        {
            connectButton.GetComponentInChildren<Text>().text = "Start Advertising";
        }
        
        bool isConnected = IsConnected();
        mouseButton.interactable = isConnected;
        keyboardButton.interactable = isConnected;
    }
    
#if UNITY_ANDROID
    // Check and request required permissions
    private void RequestPermissions()
    {
        // Android 12+ requires different permissions than older versions
        if (AndroidHelpers.IsAndroid12OrHigher())
        {
            string[] permissions = new string[] {
                "android.permission.BLUETOOTH_ADVERTISE",
                "android.permission.BLUETOOTH_CONNECT",
                "android.permission.BLUETOOTH_SCAN"
            };
            
            bool allGranted = true;
            foreach (string permission in permissions)
            {
                if (!Permission.HasUserAuthorizedPermission(permission))
                {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted)
            {
                Permission.RequestUserPermissions(permissions);
                return;
            }
        }
        else
        {
            // Pre-Android 12
            string[] permissions = new string[] {
                "android.permission.BLUETOOTH",
                "android.permission.BLUETOOTH_ADMIN",
                "android.permission.ACCESS_FINE_LOCATION"
            };
            
            bool allGranted = true;
            foreach (string permission in permissions)
            {
                if (!Permission.HasUserAuthorizedPermission(permission))
                {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted)
            {
                Permission.RequestUserPermissions(permissions);
                return;
            }
        }
        
        // Initialize BLE HID after permissions are granted
        Initialize();
    }
    
    // Execute action on main thread (for UI updates from callbacks)
    private void RunOnMainThread(Action action)
    {
        MainThreadDispatcher.Enqueue(action);
    }
#endif
    
    // Initialize the BLE HID functionality
    public void Initialize()
    {
#if UNITY_ANDROID
        try
        {
            // Create plugin classes
            unityBleHid = new AndroidJavaClass("com.inventonater.hid.unity.UnityBleHid");
            AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            AndroidJavaObject context = activity.Call<AndroidJavaObject>("getApplicationContext");
            
            // Initialize BLE HID
            bool success = unityBleHid.CallStatic<bool>("initialize", context);
            
            if (success)
            {
                // Set connection listener
                unityBleHid.CallStatic("setConnectionListener", new ConnectionListener(this));
                
                isInitialized = true;
                statusText.text = STATUS_READY;
                
                // Check if peripheral mode is supported
                if (!unityBleHid.CallStatic<bool>("isBlePeripheralSupported"))
                {
                    Debug.LogWarning("BLE peripheral mode not supported on this device");
                    statusText.text = "BLE peripheral not supported";
                }
            }
            else
            {
                Debug.LogError("Failed to initialize BLE HID");
                statusText.text = "Failed to initialize";
            }
        }
        catch (Exception e)
        {
            Debug.LogError($"Error initializing BLE HID: {e.Message}");
            statusText.text = "Error: " + e.Message;
        }
#else
        Debug.LogWarning("BLE HID is only supported on Android");
        statusText.text = "Not supported on this platform";
#endif
        
        UpdateUI();
    }
    
    // Toggle advertising
    public void ToggleAdvertising()
    {
#if UNITY_ANDROID
        if (!isInitialized)
        {
            Debug.LogWarning("BLE HID not initialized");
            return;
        }
        
        if (isAdvertising)
        {
            // Stop advertising
            unityBleHid.CallStatic("stopAdvertising");
            isAdvertising = false;
            statusText.text = STATUS_READY;
        }
        else
        {
            // Start advertising
            bool success = unityBleHid.CallStatic<bool>("startAdvertising");
            
            if (success)
            {
                isAdvertising = true;
                statusText.text = STATUS_ADVERTISING;
            }
            else
            {
                Debug.LogError("Failed to start advertising");
            }
        }
        
        UpdateUI();
#endif
    }
    
    // Check if device is connected
    private bool IsConnected()
    {
#if UNITY_ANDROID
        if (!isInitialized)
        {
            return false;
        }
        
        return unityBleHid.CallStatic<bool>("isConnected");
#else
        return false;
#endif
    }
    
    // Send a mouse movement
    public void MoveMouse(int x, int y)
    {
#if UNITY_ANDROID
        if (!IsConnected())
        {
            Debug.LogWarning("Not connected to a device");
            return;
        }
        
        unityBleHid.CallStatic<bool>("moveMouse", x, y);
#endif
    }
    
    // Click a mouse button
    public void ClickMouseButton(int button)
    {
#if UNITY_ANDROID
        if (!IsConnected())
        {
            Debug.LogWarning("Not connected to a device");
            return;
        }
        
        unityBleHid.CallStatic<bool>("clickMouseButton", button);
#endif
    }
    
    // Send a keyboard key
    public void SendKey(int keyCode)
    {
#if UNITY_ANDROID
        if (!IsConnected())
        {
            Debug.LogWarning("Not connected to a device");
            return;
        }
        
        unityBleHid.CallStatic<bool>("sendKey", keyCode);
#endif
    }
    
    // Open mouse control UI
    public void OpenMouseControl()
    {
        // Navigate to mouse control UI
        Debug.Log("Opening mouse control UI");
    }
    
    // Open keyboard control UI
    public void OpenKeyboardControl()
    {
        // Navigate to keyboard control UI
        Debug.Log("Opening keyboard control UI");
    }
    
    // Clean up
    private void OnDestroy()
    {
#if UNITY_ANDROID
        if (isInitialized)
        {
            unityBleHid.CallStatic("shutdown");
        }
#endif
    }
}

// Helper class for Unity main thread execution
public class MainThreadDispatcher : MonoBehaviour
{
    private static readonly Queue actions = new Queue();
    private static MainThreadDispatcher instance = null;
    
    // Singleton instance
    public static MainThreadDispatcher Instance
    {
        get
        {
            if (instance == null)
            {
                GameObject go = new GameObject("MainThreadDispatcher");
                DontDestroyOnLoad(go);
                instance = go.AddComponent<MainThreadDispatcher>();
            }
            return instance;
        }
    }
    
    // Enqueue action to run on main thread
    public static void Enqueue(Action action)
    {
        lock (actions)
        {
            actions.Enqueue(action);
        }
    }
    
    // Update is called once per frame
    void Update()
    {
        lock (actions)
        {
            while (actions.Count > 0)
            {
                Action action = actions.Dequeue() as Action;
                action.Invoke();
            }
        }
    }
}

// Helper for Android version checking
public static class AndroidHelpers
{
    public static bool IsAndroid12OrHigher()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        using (AndroidJavaClass buildVersionClass = new AndroidJavaClass("android.os.Build$VERSION"))
        {
            int sdkInt = buildVersionClass.GetStatic<int>("SDK_INT");
            return sdkInt >= 31; // Android 12 is API level 31
        }
#else
        return false;
#endif
    }
}
